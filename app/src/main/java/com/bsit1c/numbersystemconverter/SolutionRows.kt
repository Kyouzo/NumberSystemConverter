package com.bsit1c.numbersystemconverter

/**
 * The step-by-step solution flattened into small list rows, so a LazyColumn only composes the rows
 * that are on screen. A single step with a 64-row table becomes ~66 cheap rows instead of one huge item.
 *
 * Plain Kotlin (no Compose / Android), built on a background thread, unit-testable.
 */
sealed interface SolutionRow {
    /** Stable, unique key for LazyColumn. */
    val key: String

    /** Index of the step this row belongs to. */
    val step: Int

    /** LazyColumn content type, so rows of the same kind reuse each other's layout. */
    val contentType: Int

    data class StepHeader(
        override val step: Int,
        val number: Int,
        val title: String,
        val stage: String?,
        val isFinal: Boolean,
    ) : SolutionRow {
        override val key get() = "h$step"
        override val contentType get() = TYPE_HEADER
    }

    data class Paragraph(override val step: Int, val index: Int, val text: String) : SolutionRow {
        override val key get() = "p$step.$index"
        override val contentType get() = TYPE_PARAGRAPH
    }

    /** One line of a formula block. [first]/[last] let the UI round the block's corners. */
    data class FormulaLine(
        override val step: Int,
        val block: Int,
        val line: Int,
        val text: String,
        val first: Boolean,
        val last: Boolean,
    ) : SolutionRow {
        override val key get() = "f$step.$block.$line"
        override val contentType get() = TYPE_FORMULA
    }

    /**
     * One row of a table. All rows of a table share the same [layout] instance, so the columns line
     * up even though each row is a separate list item.
     */
    data class TableLine(
        override val step: Int,
        val table: Int,
        val row: Int,
        val cells: List<String>,
        val isHeader: Boolean,
        val last: Boolean,
        val layout: TableLayout,
    ) : SolutionRow {
        override val key get() = "t$step.$table.$row"
        override val contentType get() = TYPE_TABLE
    }

    /** Closes the step's card (bottom corners and spacing). */
    data class StepEnd(override val step: Int) : SolutionRow {
        override val key get() = "e$step"
        override val contentType get() = TYPE_END
    }

    companion object {
        const val TYPE_HEADER = 1
        const val TYPE_PARAGRAPH = 2
        const val TYPE_FORMULA = 3
        const val TYPE_TABLE = 4
        const val TYPE_END = 5
    }
}

/**
 * Column widths of a table measured in "monospace character widths".
 * Computed once from the text, so the UI never has to measure text while scrolling.
 */
data class TableLayout(val id: String, val columnChars: List<Float>)

object SolutionRows {

    /** Flattens every step. Collapsing is applied later with [visible]. */
    fun flatten(steps: List<SolutionStep>): List<SolutionRow> {
        val rows = ArrayList<SolutionRow>(steps.size * 8)
        steps.forEachIndexed { s, step ->
            rows += SolutionRow.StepHeader(s, s + 1, step.title, step.stage, isFinal = s == steps.lastIndex)
            var paragraph = 0
            var block = 0
            var table = 0
            step.content.forEach { content ->
                when (content) {
                    is StepContent.Paragraph -> rows += SolutionRow.Paragraph(s, paragraph++, content.text)
                    is StepContent.Formula -> {
                        val b = block++
                        content.lines.forEachIndexed { i, line ->
                            rows += SolutionRow.FormulaLine(s, b, i, line, i == 0, i == content.lines.lastIndex)
                        }
                    }
                    is StepContent.Table -> {
                        val t = table++
                        val layout = TableLayout("$s.$t", columnWidths(content))
                        rows += SolutionRow.TableLine(s, t, 0, content.headers, true, content.rows.isEmpty(), layout)
                        content.rows.forEachIndexed { i, cells ->
                            rows += SolutionRow.TableLine(s, t, i + 1, cells, false, i == content.rows.lastIndex, layout)
                        }
                    }
                }
            }
            rows += SolutionRow.StepEnd(s)
        }
        return rows
    }

    /** Rows to show when the steps in [collapsed] are folded: only their header and end remain. */
    fun visible(rows: List<SolutionRow>, collapsed: Set<Int>): List<SolutionRow> =
        if (collapsed.isEmpty()) rows
        else rows.filter { it.step !in collapsed || it is SolutionRow.StepHeader || it is SolutionRow.StepEnd }

    private fun columnWidths(table: StepContent.Table): List<Float> =
        table.headers.indices.map { col ->
            val cells = table.rows.map { it.getOrElse(col) { "" } } + table.headers[col]
            cells.maxOf { textWidth(it) }
        }

    /**
     * Approximate width in monospace character cells. Superscripts and other symbols may come from a
     * fallback font that is a little wider, so they are counted as 1.25 cells.
     */
    internal fun textWidth(text: String): Float =
        text.sumOf { c -> if (c.code < 128) 1.0 else 1.25 }.toFloat()
}
