package com.bsit1c.numbersystemconverter

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** What the user has typed and selected. */
@Immutable
data class FormState(
    val input: String = "",
    val from: NumberBase = NumberBase.DECIMAL,
    val to: NumberBase = NumberBase.BINARY,
    val fractionDigits: Int = NumberConverter.DEFAULT_FRACTION_DIGITS,
) {
    fun toRequest() = ConversionRequest(input, from, to, fractionDigits)

    /** The precision setting only matters for decimal fractions going to another base. */
    val showsFractionDigits: Boolean
        get() = from == NumberBase.DECIMAL && to != NumberBase.DECIMAL && input.contains('.')
}

@Immutable
data class ConversionRequest(
    val input: String,
    val from: NumberBase,
    val to: NumberBase,
    val fractionDigits: Int,
)

/** Everything below the input card. */
@Immutable
sealed interface SolutionState {
    data object Idle : SolutionState

    data class Failed(val request: ConversionRequest, val message: String) : SolutionState

    data class Ready(
        val request: ConversionRequest,
        val result: ConversionResult,
        val allRows: List<SolutionRow>,
        val collapsed: Set<Int> = emptySet(),
        /** Rows actually shown (collapsed steps folded). Pre-computed so composition stays cheap. */
        val rows: List<SolutionRow> = allRows,
    ) : SolutionState {
        val stepCount: Int get() = result.steps.size
    }
}

/**
 * Holds the screen state across rotation and runs conversions off the main thread.
 * The conversion engine itself ([NumberConverter]) is unchanged.
 */
class ConverterViewModel(private val saved: SavedStateHandle) : ViewModel() {

    var form by mutableStateOf(restoreForm())
        private set

    var solution by mutableStateOf<SolutionState>(SolutionState.Idle)
        private set

    /** True only when a conversion takes long enough to be worth showing a spinner. */
    var isWorking by mutableStateOf(false)
        private set

    /** Validation message shown under the input while typing (null = fine or empty). */
    var liveError by mutableStateOf<String?>(null)
        private set

    private val scrollChannel = Channel<Unit>(Channel.CONFLATED)

    /** One-shot events asking the UI to bring the answer into view. Not replayed after rotation. */
    val scrollToAnswer: Flow<Unit> = scrollChannel.receiveAsFlow()

    private var job: Job? = null

    init {
        liveError = validate(form)
        // Recreate the last result after the process was killed in the background.
        restoreRequest()?.let { run(it, scroll = false) }
    }

    // ---------------------------------------------------------------- form events

    fun onInputChange(value: String) = update(form.copy(input = value))

    fun onFromChange(base: NumberBase) = update(form.copy(from = base))

    fun onToChange(base: NumberBase) = update(form.copy(to = base))

    fun onFractionDigitsChange(digits: Int) = update(form.copy(fractionDigits = digits))

    /** Swap bases. If the current answer is exact and matches the form, it becomes the new input. */
    fun swap() {
        val ready = solution as? SolutionState.Ready
        val result = ready?.result
        val useAnswer = result != null && !result.isApproximate &&
            ready.request.from == form.from && ready.request.to == form.to
        update(
            form.copy(
                input = if (useAnswer) result!!.answer else form.input,
                from = form.to,
                to = form.from,
            )
        )
    }

    fun reset() {
        job?.cancel()
        isWorking = false
        update(form.copy(input = ""))
        solution = SolutionState.Idle
        saved[KEY_REQUEST_INPUT] = null
    }

    fun convert() {
        val request = form.toRequest()
        val current = solution
        val sameAsShown = (current is SolutionState.Ready && current.request == request) ||
            (current is SolutionState.Failed && current.request == request)
        if (sameAsShown) {
            // Nothing changed: don't redo the work, just bring the answer back into view.
            scrollChannel.trySend(Unit)
            return
        }
        run(request, scroll = true)
    }

    fun toggleStep(step: Int) {
        val ready = solution as? SolutionState.Ready ?: return
        val collapsed = if (step in ready.collapsed) ready.collapsed - step else ready.collapsed + step
        solution = ready.copy(collapsed = collapsed, rows = SolutionRows.visible(ready.allRows, collapsed))
    }

    fun setAllCollapsed(collapse: Boolean) {
        val ready = solution as? SolutionState.Ready ?: return
        // The final answer step always stays open.
        val collapsed = if (collapse) (0 until ready.stepCount - 1).toSet() else emptySet()
        solution = ready.copy(collapsed = collapsed, rows = SolutionRows.visible(ready.allRows, collapsed))
    }

    // ---------------------------------------------------------------- internals

    private fun update(newForm: FormState) {
        if (newForm == form) return
        val inputOrBaseChanged = newForm.input != form.input || newForm.from != form.from
        form = newForm
        if (inputOrBaseChanged) liveError = validate(newForm)
        saved[KEY_INPUT] = newForm.input
        saved[KEY_FROM] = newForm.from.name
        saved[KEY_TO] = newForm.to.name
        saved[KEY_DIGITS] = newForm.fractionDigits
    }

    private fun validate(f: FormState): String? =
        if (f.input.isBlank()) null
        else (InputValidator.validate(f.input, f.from) as? ParseResult.Invalid)?.message

    private fun run(request: ConversionRequest, scroll: Boolean) {
        job?.cancel()
        saveRequest(request)
        job = viewModelScope.launch {
            // Only show a spinner if the work is noticeably slow, so fast conversions don't flicker.
            val spinner = launch { delay(150); isWorking = true }
            val next = withContext(Dispatchers.Default) { compute(request) }
            spinner.cancel()
            isWorking = false
            solution = next
            if (scroll) scrollChannel.trySend(Unit)
        }
    }

    private fun compute(request: ConversionRequest): SolutionState =
        when (val outcome = NumberConverter(request.fractionDigits).convert(request.input, request.from, request.to)) {
            is ConversionOutcome.Error -> SolutionState.Failed(request, outcome.message)
            is ConversionOutcome.Success -> {
                val rows = SolutionRows.flatten(outcome.result.steps)
                SolutionState.Ready(request, outcome.result, rows)
            }
        }

    private fun restoreForm() = FormState(
        input = saved[KEY_INPUT] ?: "",
        from = saved.get<String>(KEY_FROM)?.let { NumberBase.valueOf(it) } ?: NumberBase.DECIMAL,
        to = saved.get<String>(KEY_TO)?.let { NumberBase.valueOf(it) } ?: NumberBase.BINARY,
        fractionDigits = saved[KEY_DIGITS] ?: NumberConverter.DEFAULT_FRACTION_DIGITS,
    )

    private fun saveRequest(r: ConversionRequest) {
        saved[KEY_REQUEST_INPUT] = r.input
        saved[KEY_REQUEST_FROM] = r.from.name
        saved[KEY_REQUEST_TO] = r.to.name
        saved[KEY_REQUEST_DIGITS] = r.fractionDigits
    }

    private fun restoreRequest(): ConversionRequest? {
        val input = saved.get<String>(KEY_REQUEST_INPUT) ?: return null
        return ConversionRequest(
            input = input,
            from = NumberBase.valueOf(saved[KEY_REQUEST_FROM] ?: return null),
            to = NumberBase.valueOf(saved[KEY_REQUEST_TO] ?: return null),
            fractionDigits = saved[KEY_REQUEST_DIGITS] ?: NumberConverter.DEFAULT_FRACTION_DIGITS,
        )
    }

    private companion object {
        const val KEY_INPUT = "input"
        const val KEY_FROM = "from"
        const val KEY_TO = "to"
        const val KEY_DIGITS = "digits"
        const val KEY_REQUEST_INPUT = "req_input"
        const val KEY_REQUEST_FROM = "req_from"
        const val KEY_REQUEST_TO = "req_to"
        const val KEY_REQUEST_DIGITS = "req_digits"
    }
}
