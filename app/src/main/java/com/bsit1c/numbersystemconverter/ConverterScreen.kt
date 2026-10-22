@file:OptIn(ExperimentalMaterial3Api::class)

package com.bsit1c.numbersystemconverter

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * The app's only screen. Phones get one scrolling column; wide windows (tablets, landscape)
 * get the input on the left and the solution on the right.
 *
 * Performance notes:
 *  - Conversion and solution building run in [ConverterViewModel] on a background thread.
 *  - The solution is a flat list of small rows with stable keys and content types.
 *  - Form state is read inside the form item only, so typing does not rebuild the solution list.
 */
@Composable
fun ConverterScreen(vm: ConverterViewModel, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing, // includes the keyboard, so nothing hides under it
    ) { innerPadding ->
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (maxWidth >= Spacing.twoPaneMinWidth) TwoPaneLayout(vm) else SinglePaneLayout(vm)
        }
    }
}

// ------------------------------------------------------------------------- layouts

@Composable
private fun SinglePaneLayout(vm: ConverterViewModel) {
    val listState = rememberLazyListState()
    LaunchedEffect(vm) {
        vm.scrollToAnswer.collect { listState.animateScrollToItem(RESULT_ITEM_INDEX) }
    }
    val solution = vm.solution
    val solutionTools = rememberSolutionTools(solution)

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .widthIn(max = Spacing.contentMaxWidth)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = Spacing.l, end = Spacing.l, top = Spacing.s, bottom = Spacing.xxl),
        ) {
            item(key = "title", contentType = "title") { TitleBlock(Modifier.padding(bottom = Spacing.l)) }
            item(key = "form", contentType = "form") { FormCard(vm) }
            item(key = "result", contentType = "result") {
                ResultSection(vm, Modifier.padding(top = Spacing.l))
            }
            solutionItems(solution, vm, solutionTools)
        }
    }
}

@Composable
private fun TwoPaneLayout(vm: ConverterViewModel) {
    val listState = rememberLazyListState()
    LaunchedEffect(vm) {
        vm.scrollToAnswer.collect { listState.animateScrollToItem(0) }
    }
    val solution = vm.solution
    val solutionTools = rememberSolutionTools(solution)

    Row(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .width(Spacing.inputPaneWidth)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.l),
        ) {
            TitleBlock(Modifier.padding(bottom = Spacing.l))
            FormCard(vm)
            ResultSection(vm, Modifier.padding(top = Spacing.l))
        }
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentPadding = PaddingValues(start = Spacing.s, end = Spacing.xl, top = Spacing.s, bottom = Spacing.xxl),
        ) {
            if (solution is SolutionState.Ready) {
                solutionItems(solution, vm, solutionTools)
            } else {
                item(key = "placeholder", contentType = "placeholder") { SolutionPlaceholder() }
            }
        }
    }
}

private const val RESULT_ITEM_INDEX = 2

/** Things every row needs that should be computed once, not per row. */
private class SolutionTools(val charWidth: Dp, private val scrolls: HashMap<String, ScrollState>) {
    fun scrollFor(layout: TableLayout): ScrollState = scrolls.getOrPut(layout.id) { ScrollState(0) }
}

@Composable
private fun rememberSolutionTools(solution: SolutionState): SolutionTools {
    val math = LocalMathStyles.current
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer(cacheSize = 1)
    // One measurement of the monospace digit width for the whole screen (respects font scale).
    val charWidth = remember(math.body, density) {
        with(density) { (measurer.measure("0000000000", math.body).size.width / 10f).toDp() }
    }
    // Fresh table scroll states for each new result.
    val resultKey = (solution as? SolutionState.Ready)?.result
    val scrolls = remember(resultKey) { HashMap<String, ScrollState>() }
    return remember(charWidth, scrolls) { SolutionTools(charWidth, scrolls) }
}

private fun LazyListScope.solutionItems(solution: SolutionState, vm: ConverterViewModel, tools: SolutionTools) {
    if (solution !is SolutionState.Ready) return
    item(key = "solution-header", contentType = "solution-header") {
        SolutionHeader(
            stepCount = solution.stepCount,
            method = solution.result.method,
            allExpanded = solution.collapsed.isEmpty(),
            onToggleAll = { vm.setAllCollapsed(solution.collapsed.isEmpty()) },
        )
    }
    items(solution.rows, key = { it.key }, contentType = { it.contentType }) { row ->
        SolutionRowItem(
            row = row,
            collapsed = row.step in solution.collapsed,
            charWidth = tools.charWidth,
            tableScroll = tools::scrollFor,
            onToggleStep = vm::toggleStep,
            modifier = Modifier.animateItem(
                fadeInSpec = tween(180),
                fadeOutSpec = tween(120),
            ),
        )
    }
}

// ------------------------------------------------------------------------- title

@Composable
private fun TitleBlock(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(top = Spacing.s)) {
        Text("Number System", style = MaterialTheme.typography.headlineSmall)
    }
}

// ------------------------------------------------------------------------- form

@Composable
private fun FormCard(vm: ConverterViewModel) {
    val form = vm.form // read here so only this card recomposes while typing
    val liveError = vm.liveError
    val math = LocalMathStyles.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current

    val submit = {
        keyboard?.hide()
        focus.clearFocus()
        vm.convert()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(Spacing.l + 4.dp), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
            OutlinedTextField(
                value = form.input,
                onValueChange = vm::onInputChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Number in ${form.from.lowerName}") },
                placeholder = { Text(form.from.example, style = math.input) },
                singleLine = true,
                isError = liveError != null,
                shape = MaterialTheme.shapes.medium,
                textStyle = math.input,
                supportingText = {
                    Text(liveError ?: "Allowed: ${form.from.allowedDigits}")
                },
                trailingIcon = if (form.input.isNotEmpty()) {
                    {
                        IconButton(onClick = { vm.onInputChange("") }) {
                            Icon(AppIcons.Close, contentDescription = "Clear input", modifier = Modifier.size(20.dp))
                        }
                    }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    autoCorrectEnabled = false,
                    keyboardType = if (form.from == NumberBase.HEXADECIMAL) KeyboardType.Ascii else KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
            )

            SectionLabel("From")
            BaseSelector(selected = form.from, onSelect = vm::onFromChange)
            SwapDivider(onSwap = vm::swap)
            SectionLabel("To")
            BaseSelector(selected = form.to, onSelect = vm::onToChange)

            AnimatedVisibility(
                visible = form.showsFractionDigits,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                FractionDigitsPicker(form.fractionDigits, vm::onFractionDigitsChange)
            }

            Row(
                Modifier.padding(top = Spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                Button(
                    onClick = submit,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    if (vm.isWorking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text("Convert", style = MaterialTheme.typography.titleMedium)
                    }
                }
                OutlinedButton(
                    onClick = {
                        focus.clearFocus()
                        vm.reset()
                    },
                    modifier = Modifier.height(52.dp),
                    shape = MaterialTheme.shapes.medium,
                ) { Text("Reset") }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun BaseSelector(selected: NumberBase, onSelect: (NumberBase) -> Unit) {
    val bases = NumberBase.entries
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        bases.forEachIndexed { index, base ->
            SegmentedButton(
                selected = base == selected,
                onClick = { onSelect(base) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = bases.size),
                icon = {},
                modifier = Modifier.height(56.dp),
                label = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(base.shortName, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                        Text("base ${base.radix}", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                },
            )
        }
    }
}

@Composable
private fun SwapDivider(onSwap: () -> Unit) {
    var turns by remember { mutableIntStateOf(0) }
    val rotation by animateFloatAsState(turns * 180f, animationSpec = tween(250), label = "swap")
    Row(verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
        FilledTonalIconButton(
            onClick = {
                turns++
                onSwap()
            },
            modifier = Modifier.padding(horizontal = Spacing.s),
        ) {
            Icon(
                AppIcons.Swap,
                contentDescription = "Swap From and To",
                modifier = Modifier.graphicsLayer { rotationZ = rotation },
            )
        }
        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun FractionDigitsPicker(selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        SectionLabel("Max digits after the point")
        Text(
            "Used only if the fraction repeats forever in the target base (e.g. 0.1 in binary).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            NumberConverter.FRACTION_DIGIT_CHOICES.forEach { n ->
                FilterChip(
                    selected = n == selected,
                    onClick = { onSelect(n) },
                    label = { Text("$n") },
                )
            }
        }
    }
}

// ------------------------------------------------------------------------- result

@Composable
private fun ResultSection(vm: ConverterViewModel, modifier: Modifier = Modifier) {
    val solution = vm.solution
    AnimatedContent(
        targetState = solution,
        modifier = modifier.fillMaxWidth(),
        // Collapsing steps creates a new Ready object; only animate when the conversion itself changes.
        contentKey = { state ->
            when (state) {
                SolutionState.Idle -> "idle"
                is SolutionState.Failed -> state.request
                is SolutionState.Ready -> state.request
            }
        },
        transitionSpec = {
            (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 16 }) togetherWith fadeOut(tween(90))
        },
        label = "result",
    ) { state ->
        when (state) {
            SolutionState.Idle -> IdleHint()
            is SolutionState.Failed -> ErrorCard(state.message)
            is SolutionState.Ready -> {
                // True when the form no longer matches what is shown. derivedStateOf keeps this from
                // recomposing the card on every keystroke; it only flips when the answer goes stale.
                val stale by remember(state.request) { derivedStateOf { vm.form.toRequest() != state.request } }
                AnswerCard(state.result, state.request.fractionDigits, stale)
            }
        }
    }
}

@Composable
private fun IdleHint() {

}

@Composable
private fun AnswerCard(result: ConversionResult, fractionDigits: Int, stale: Boolean) {
    val math = LocalMathStyles.current
    val answerText = MathFormat.withBase(result.answer, result.to)
    // Very long answers (e.g. 64 bits) get a smaller size so they wrap less.
    val answerStyle = if (answerText.length > 20) math.answer.copy(fontSize = 22.sp, lineHeight = 30.sp) else math.answer
    val contentAlpha by animateFloatAsState(if (stale) 0.6f else 1f, label = "stale")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(Modifier.padding(Spacing.l + 4.dp), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Answer · ${result.to.displayName}",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f),
                )
                if (result.isApproximate) Pill("Approximate")
            }
            Column(Modifier.graphicsLayer { alpha = contentAlpha }, verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                SelectionContainer { MathText(answerText, style = answerStyle) }
                MathText(result.equation, style = math.equation)
                Text("Method: ${result.method}", style = MaterialTheme.typography.bodySmall)
                if (result.isApproximate) {
                    Text(
                        "This fraction never ends in ${result.to.lowerName}, so it is cut off after $fractionDigits digits.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            AnimatedVisibility(visible = stale) {
                Text(
                    "You changed the input — tap Convert to update.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            CopyButton(result.answer, Modifier.padding(top = Spacing.xs))
        }
    }
}

@Composable
private fun Pill(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = RoundedCornerShape(50),
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

@Composable
private fun CopyButton(text: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var copied by remember(text) { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1600)
            copied = false
        }
    }
    Button(
        onClick = {
            context.getSystemService(ClipboardManager::class.java)
                ?.setPrimaryClip(ClipData.newPlainText("Converted number", text))
            copied = true
        },
        modifier = modifier.height(48.dp),
        shape = MaterialTheme.shapes.medium,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
    ) {
        Crossfade(targetState = copied, label = "copy") { done ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (done) AppIcons.Check else AppIcons.Copy,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(Spacing.s))
                Text(if (done) "Copied" else "Copy answer")
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Column(Modifier.padding(Spacing.l + 4.dp), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text("Can't convert this number", style = MaterialTheme.typography.titleMedium)
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
