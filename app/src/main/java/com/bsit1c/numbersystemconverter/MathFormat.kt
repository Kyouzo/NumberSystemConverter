package com.bsit1c.numbersystemconverter

import java.math.BigDecimal
import kotlin.math.abs

/**
 * Formatting helpers that write numbers the way a textbook does:
 * 348₁₀, 2³, 16⁻¹, and so on.
 */
object MathFormat {
    private const val SUPERSCRIPTS = "⁰¹²³⁴⁵⁶⁷⁸⁹"
    private const val SUBSCRIPTS = "₀₁₂₃₄₅₆₇₈₉"
    const val DIGITS = "0123456789ABCDEF"

    fun superscript(n: Int): String {
        val body = abs(n).toString().map { SUPERSCRIPTS[it - '0'] }.joinToString("")
        return if (n < 0) "⁻$body" else body
    }

    fun subscript(n: Int): String = n.toString().map { SUBSCRIPTS[it - '0'] }.joinToString("")

    /** "2³", "16⁻²" */
    fun power(base: Int, exponent: Int): String = "$base${superscript(exponent)}"

    /** "101₂" */
    fun withBase(number: String, base: NumberBase): String = number + subscript(base.radix)

    fun digitChar(value: Int): Char = DIGITS[value]

    /** Value of a digit character (0–15), or -1 if it is not a digit at all. */
    fun digitValue(c: Char): Int = DIGITS.indexOf(c.uppercaseChar())

    /** Exact decimal text without exponent notation or trailing zeros. */
    fun plain(value: BigDecimal): String =
        if (value.signum() == 0) "0" else value.stripTrailingZeros().toPlainString()

    /** "12 (C)" for hex remainders 10–15, plain "7" otherwise. */
    fun valueWithLetter(value: Int): String =
        if (value >= 10) "$value (${digitChar(value)})" else "$value"

    /** Place values inside a bit group, e.g. [4, 2, 1] for k = 3. */
    fun groupWeights(bits: Int): List<Int> = (bits - 1 downTo 0).map { 1 shl it }

    /** "4 + 1 = 5", "4", or "0" – how a bit group adds up. */
    fun bitSum(group: String): String {
        val weights = groupWeights(group.length)
        val ones = group.indices.filter { group[it] == '1' }.map { weights[it] }
        return when (ones.size) {
            0 -> "0"
            1 -> "${ones[0]}"
            else -> ones.joinToString(" + ") + " = " + ones.sum()
        }
    }
}
