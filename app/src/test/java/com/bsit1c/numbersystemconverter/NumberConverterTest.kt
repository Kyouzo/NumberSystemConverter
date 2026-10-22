package com.bsit1c.numbersystemconverter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Random

class NumberConverterTest {

    private val converter = NumberConverter()

    private fun ok(input: String, from: NumberBase, to: NumberBase, c: NumberConverter = converter): ConversionResult =
        when (val o = c.convert(input, from, to)) {
            is ConversionOutcome.Success -> o.result
            is ConversionOutcome.Error -> {
                fail("Expected success for '$input' $from→$to but got: ${o.message}")
                throw IllegalStateException()
            }
        }

    private fun err(input: String, from: NumberBase): String =
        when (val o = converter.convert(input, from, NumberBase.DECIMAL)) {
            is ConversionOutcome.Error -> o.message
            is ConversionOutcome.Success -> {
                fail("Expected an error for '$input' in $from")
                throw IllegalStateException()
            }
        }

    private fun answer(input: String, from: NumberBase, to: NumberBase) = ok(input, from, to).answer

    private fun allText(r: ConversionResult): String = r.steps.joinToString("\n") { s ->
        s.title + "\n" + s.content.joinToString("\n") {
            when (it) {
                is StepContent.Paragraph -> it.text
                is StepContent.Formula -> it.lines.joinToString("\n")
                is StepContent.Table -> (listOf(it.headers) + it.rows).joinToString("\n") { row -> row.joinToString(" | ") }
            }
        }
    }

    // ------------------------------------------------ examples from the IT 133 Chapter 3 slides

    @Test
    fun slideExamples() {
        val B = NumberBase.BINARY; val O = NumberBase.OCTAL; val D = NumberBase.DECIMAL; val H = NumberBase.HEXADECIMAL
        assertEquals("101011100", answer("348", D, B))
        assertEquals("612", answer("394", D, O))
        assertEquals("15C", answer("348", D, H))
        assertEquals("10", answer("1010", B, D))
        assertEquals("27", answer("11011", B, D))
        assertEquals("65", answer("110101", B, O))
        assertEquals("B7", answer("10110111", B, H))
        assertEquals("101110", answer("56", O, B))
        assertEquals("46", answer("56", O, D))
        assertEquals("8B", answer("213", O, H))
        assertEquals("7932", answer("1EFC", H, D))
        assertEquals("5.75", answer("101.110", B, D))
        assertEquals("25.34", answer("25.34", D, D))
        assertEquals("277.359375", answer("425.27", O, D))
        assertEquals("683.17431640625", answer("2AB.2CA", H, D))
        assertEquals("C", answer("12", D, H))
    }

    @Test
    fun divisionStepsMatchSlideStyle() {
        val text = allText(ok("394", NumberBase.DECIMAL, NumberBase.OCTAL))
        assertTrue(text.contains("394 ÷ 8 | 49.25 | 49 | 0.25 × 8 = 2"))
        assertTrue(text.contains("49 ÷ 8 | 6.125 | 6 | 0.125 × 8 = 1"))
        assertTrue(text.contains("6 ÷ 8 | 0.75 | 0 | 0.75 × 8 = 6"))
        val hex = allText(ok("348", NumberBase.DECIMAL, NumberBase.HEXADECIMAL))
        assertTrue(hex.contains("348 ÷ 16 | 21.75 | 21 | 0.75 × 16 = 12 | C"))
        assertTrue(hex.contains("348₁₀ = 15C₁₆"))
    }

    @Test
    fun hexToDecimalShowsEveryTerm() {
        val text = allText(ok("1EFC", NumberBase.HEXADECIMAL, NumberBase.DECIMAL))
        assertTrue(text.contains("14 × 16² = 14 × 256 = 3584"))
        assertTrue(text.contains("4096 + 3584 + 240 + 12 = 7932"))
    }

    @Test
    fun octalToHexGoesThroughBinaryWithStages() {
        val r = ok("213", NumberBase.OCTAL, NumberBase.HEXADECIMAL)
        assertTrue(r.steps.any { it.stage?.startsWith("Stage 1") == true })
        assertTrue(r.steps.any { it.stage?.startsWith("Stage 2") == true })
        val text = allText(r)
        assertTrue(text.contains("010 001 011  →  010001011"))
        assertTrue(text.contains("Both equal 139"))
    }

    // ------------------------------------------------ edge cases

    @Test
    fun zeroAndLeadingZeros() {
        for (from in NumberBase.entries) for (to in NumberBase.entries) {
            assertEquals("0", answer("0", from, to))
            assertEquals("0", answer("0000", from, to))
            assertEquals("0", answer("-0", from, to))
        }
        assertEquals("5", answer("000101", NumberBase.BINARY, NumberBase.DECIMAL))
        assertTrue(allText(ok("000101", NumberBase.BINARY, NumberBase.DECIMAL)).contains("Leading zeros were dropped"))
    }

    @Test
    fun negativeNumbers() {
        assertEquals("-101011100", answer("-348", NumberBase.DECIMAL, NumberBase.BINARY))
        assertEquals("-27", answer("-11011", NumberBase.BINARY, NumberBase.DECIMAL))
        assertEquals("-8B", answer("-213", NumberBase.OCTAL, NumberBase.HEXADECIMAL))
        assertEquals("-0.101", answer("-0.625", NumberBase.DECIMAL, NumberBase.BINARY))
    }

    @Test
    fun hexIsCaseInsensitiveAndUppercased() {
        assertEquals("7932", answer("1efc", NumberBase.HEXADECIMAL, NumberBase.DECIMAL))
        assertEquals("FF", answer("ff", NumberBase.HEXADECIMAL, NumberBase.HEXADECIMAL))
        assertEquals("FF", answer("255", NumberBase.DECIMAL, NumberBase.HEXADECIMAL))
    }

    @Test
    fun largeIntegers() {
        val big = "123456789012345678901234567890"
        val expected = BigInteger(big).toString(16).uppercase()
        assertEquals(expected, answer(big, NumberBase.DECIMAL, NumberBase.HEXADECIMAL))
        assertEquals(big, answer(expected, NumberBase.HEXADECIMAL, NumberBase.DECIMAL))
        val bits = "1".repeat(64)
        assertEquals("18446744073709551615", answer(bits, NumberBase.BINARY, NumberBase.DECIMAL))
    }

    @Test
    fun fractionsExact() {
        assertEquals("0.101", answer("0.625", NumberBase.DECIMAL, NumberBase.BINARY))
        assertEquals("15C.A", answer("348.625", NumberBase.DECIMAL, NumberBase.HEXADECIMAL))
        assertEquals("0.5", answer(".1", NumberBase.BINARY, NumberBase.DECIMAL))
        assertEquals("1100.101", answer("C.A", NumberBase.HEXADECIMAL, NumberBase.BINARY))
        assertEquals("14.5", answer("1100.101", NumberBase.BINARY, NumberBase.OCTAL))
        assertEquals("C.A", answer("14.5", NumberBase.OCTAL, NumberBase.HEXADECIMAL))
        assertFalse(ok("0.625", NumberBase.DECIMAL, NumberBase.BINARY).isApproximate)
    }

    @Test
    fun repeatingFractionsAreApproximate() {
        val r = ok("0.1", NumberBase.DECIMAL, NumberBase.BINARY)
        assertTrue(r.isApproximate)
        assertEquals("0.000110011001", r.answer) // 12 digits by default
        assertTrue(r.equation.contains("≈"))
        assertTrue(allText(r).contains("0.0(0011)"))

        val r8 = ok("0.1", NumberBase.DECIMAL, NumberBase.BINARY, NumberConverter(8))
        assertEquals("0.00011001", r8.answer)

        val third = ok("0.2", NumberBase.DECIMAL, NumberBase.HEXADECIMAL)
        assertEquals("0.333333333333", third.answer)
    }

    // ------------------------------------------------ validation

    @Test
    fun rejectsInvalidDigits() {
        assertTrue(err("102", NumberBase.BINARY).contains("'2' is not a valid binary digit"))
        assertTrue(err("78", NumberBase.OCTAL).contains("'8' is not a valid octal digit"))
        assertTrue(err("12A", NumberBase.DECIMAL).contains("'A' is not a valid decimal digit"))
        assertTrue(err("1G", NumberBase.HEXADECIMAL).contains("'G' is not a valid hexadecimal digit"))
        assertTrue(err("0x1F", NumberBase.HEXADECIMAL).contains("prefix"))
        assertTrue(err("", NumberBase.DECIMAL).isNotBlank())
        assertTrue(err("-", NumberBase.DECIMAL).isNotBlank())
        assertTrue(err(".", NumberBase.DECIMAL).isNotBlank())
        assertTrue(err("1.2.3", NumberBase.DECIMAL).contains("only one"))
        assertTrue(err("1-2", NumberBase.DECIMAL).contains("beginning"))
        assertTrue(err("1,5", NumberBase.DECIMAL).contains("comma"))
        assertTrue(err("1".repeat(65), NumberBase.BINARY).contains("too long"))
    }

    // ------------------------------------------------ randomized cross-checks for all 12 directions

    @Test
    fun randomIntegersAllDirections() {
        val rnd = Random(42)
        repeat(3000) {
            val v = BigInteger(1 + rnd.nextInt(64), rnd)
            for (from in NumberBase.entries) for (to in NumberBase.entries) {
                val input = v.toString(from.radix)
                val expected = v.toString(to.radix).uppercase()
                assertEquals("$input $from→$to", expected, answer(input, from, to))
            }
        }
    }

    @Test
    fun randomFractionsBetweenPowerOfTwoBasesAndToDecimal() {
        val rnd = Random(7)
        val pow2 = listOf(NumberBase.BINARY, NumberBase.OCTAL, NumberBase.HEXADECIMAL)
        repeat(1500) {
            val from = pow2[rnd.nextInt(3)]
            val intPart = BigInteger(rnd.nextInt(30), rnd).toString(from.radix)
            val fracLen = 1 + rnd.nextInt(8)
            val frac = (1..fracLen).map { MathFormat.digitChar(rnd.nextInt(from.radix)) }.joinToString("")
            val input = "$intPart.$frac"
            val exact = ParsedNumberValue.of(input, from)
            for (to in NumberBase.entries) {
                val r = ok(input, from, to)
                assertFalse("$input $from→$to should be exact", r.isApproximate)
                assertEquals("$input $from→$to", 0, exact.compareTo(ParsedNumberValue.of(r.answer, to)))
            }
        }
    }

    @Test
    fun randomDecimalFractionsAreTruncatedCorrectly() {
        val rnd = Random(99)
        val targets = listOf(NumberBase.BINARY, NumberBase.OCTAL, NumberBase.HEXADECIMAL)
        repeat(2000) {
            val input = "${rnd.nextInt(5000)}.${1 + rnd.nextInt(99999)}"
            val exact = BigDecimal(input)
            for (to in targets) {
                val r = ok(input, NumberBase.DECIMAL, to)
                val got = ParsedNumberValue.of(r.answer, to)
                val step = BigDecimal.ONE.divide(BigDecimal(to.radix).pow(12))
                if (r.isApproximate) {
                    // truncation: got <= exact < got + one unit in the last place
                    assertTrue("$input→$to ${r.answer}", got <= exact && exact < got + step)
                } else {
                    assertEquals("$input→$to", 0, exact.compareTo(got))
                }
            }
        }
    }

    @Test
    fun everySuccessfulConversionHasStepsAndFinalAnswer() {
        for (from in NumberBase.entries) for (to in NumberBase.entries) {
            val r = ok("1", from, to)
            assertTrue(r.steps.isNotEmpty())
            assertEquals("Final answer", r.steps.last().title)
        }
    }

    @Test
    fun checksAlwaysPass() {
        val rnd = Random(3)
        repeat(500) {
            val v = BigInteger(1 + rnd.nextInt(40), rnd)
            for (from in NumberBase.entries) for (to in NumberBase.entries) {
                val text = allText(ok(v.toString(from.radix), from, to))
                assertFalse("check failed for $v $from→$to", text.contains("✗"))
            }
        }
    }
}

/** Test helper: exact value of a signed number string in a base. */
private object ParsedNumberValue {
    fun of(text: String, base: NumberBase): BigDecimal {
        val neg = text.startsWith("-")
        val digits = Digits.fromPlain(text.removePrefix("-"))
        val v = digits.valueIn(base)
        return if (neg) v.negate() else v
    }
}
