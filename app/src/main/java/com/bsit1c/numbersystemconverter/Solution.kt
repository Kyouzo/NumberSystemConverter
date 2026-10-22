package com.bsit1c.numbersystemconverter

/** One piece of content inside a solution step. */
sealed interface StepContent {
    /** An explanation sentence. */
    data class Paragraph(val text: String) : StepContent

    /** One or more lines of math, shown in a monospaced box. */
    data class Formula(val lines: List<String>) : StepContent

    /** A worked table (divisions, groups, place values …). */
    data class Table(val headers: List<String>, val rows: List<List<String>>) : StepContent
}

/**
 * One numbered step of a solution.
 * [stage] is set when a conversion runs through an intermediate base (e.g. "Stage 1 · Octal → Binary").
 */
data class SolutionStep(
    val title: String,
    val content: List<StepContent>,
    val stage: String? = null,
)

/** Everything the UI needs to show a finished conversion. */
data class ConversionResult(
    val from: NumberBase,
    val to: NumberBase,
    /** Normalised input, with sign. */
    val input: String,
    /** Answer digits, with sign. */
    val answer: String,
    /** True when a fraction had to be cut off at the precision limit. */
    val isApproximate: Boolean,
    /** Short name of the method used. */
    val method: String,
    val steps: List<SolutionStep>,
) {
    val equation: String
        get() = "${MathFormat.withBase(input, from)} ${if (isApproximate) "≈" else "="} ${MathFormat.withBase(answer, to)}"
}

sealed interface ConversionOutcome {
    data class Success(val result: ConversionResult) : ConversionOutcome
    data class Error(val message: String) : ConversionOutcome
}

/** Small DSL used by the conversion methods to record their steps as they compute. */
class SolutionBuilder {
    private val steps = mutableListOf<SolutionStep>()

    /** Stage label attached to every step added while it is set. */
    var stage: String? = null

    fun step(title: String, block: StepBuilder.() -> Unit) {
        steps += SolutionStep(title, StepBuilder().apply(block).content.toList(), stage)
    }

    fun build(): List<SolutionStep> = steps.toList()
}

class StepBuilder {
    internal val content = mutableListOf<StepContent>()

    fun text(text: String) {
        content += StepContent.Paragraph(text)
    }

    fun math(vararg lines: String) {
        content += StepContent.Formula(lines.toList())
    }

    fun math(lines: List<String>) {
        content += StepContent.Formula(lines)
    }

    fun table(headers: List<String>, rows: List<List<String>>) {
        content += StepContent.Table(headers, rows)
    }
}

/** What a single conversion method hands back to [NumberConverter]. */
internal data class MethodOutput(val digits: Digits, val approximate: Boolean = false)
