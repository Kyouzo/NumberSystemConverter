package com.bsit1c.numbersystemconverter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SolutionRowsTest {

    private fun result(input: String, from: NumberBase, to: NumberBase): ConversionResult =
        (NumberConverter().convert(input, from, to) as ConversionOutcome.Success).result

    @Test
    fun keysAreUniqueForEveryDirection() {
        for (from in NumberBase.entries) for (to in NumberBase.entries) {
            val input = if (from == NumberBase.BINARY) "-1011.011" else "-123.4"
            val rows = SolutionRows.flatten(result(input, from, to).steps)
            assertEquals("$from→$to", rows.size, rows.map { it.key }.toSet().size)
        }
    }

    @Test
    fun everyStepHasHeaderAndEnd() {
        val r = result("213", NumberBase.OCTAL, NumberBase.HEXADECIMAL)
        val rows = SolutionRows.flatten(r.steps)
        assertEquals(r.steps.size, rows.count { it is SolutionRow.StepHeader })
        assertEquals(r.steps.size, rows.count { it is SolutionRow.StepEnd })
        assertTrue(rows.first() is SolutionRow.StepHeader)
        assertTrue(rows.last() is SolutionRow.StepEnd)
    }

    @Test
    fun tableRowsShareOneLayoutAndColumnsFitContent() {
        val rows = SolutionRows.flatten(result("348", NumberBase.DECIMAL, NumberBase.HEXADECIMAL).steps)
        val table = rows.filterIsInstance<SolutionRow.TableLine>()
        assertEquals(4, table.size) // header + 3 divisions
        assertTrue(table.all { it.layout === table[0].layout })
        assertTrue(table.first().isHeader)
        assertTrue(table.last().last)
        table.forEach { line ->
            line.cells.forEachIndexed { i, cell ->
                assertTrue(SolutionRows.textWidth(cell) <= line.layout.columnChars[i])
            }
        }
    }

    @Test
    fun longSolutionsAreSplitIntoSmallRows() {
        val bits = "1".repeat(64)
        val rows = SolutionRows.flatten(result(bits, NumberBase.BINARY, NumberBase.DECIMAL).steps)
        // 64 table rows + 64 multiplication lines must be separate items, not one giant block.
        assertTrue(rows.count { it is SolutionRow.TableLine } >= 65)
        assertTrue(rows.count { it is SolutionRow.FormulaLine } >= 64)
    }

    @Test
    fun collapsingHidesOnlyThatStepsBody() {
        val rows = SolutionRows.flatten(result("348", NumberBase.DECIMAL, NumberBase.BINARY).steps)
        val visible = SolutionRows.visible(rows, setOf(0))
        assertTrue(visible.none { it.step == 0 && it !is SolutionRow.StepHeader && it !is SolutionRow.StepEnd })
        assertEquals(rows.count { it.step != 0 }, visible.count { it.step != 0 })
        assertEquals(rows, SolutionRows.visible(rows, emptySet()))
    }
}
