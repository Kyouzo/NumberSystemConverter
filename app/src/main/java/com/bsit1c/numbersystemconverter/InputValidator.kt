package com.bsit1c.numbersystemconverter

import java.math.BigDecimal
import java.math.BigInteger

/** Unsigned digits of a number, split at the radix point. */
data class Digits(val integer: String, val fraction: String = "") {
    val text: String get() = if (fraction.isEmpty()) integer else "$integer.$fraction"
    val hasFraction: Boolean get() = fraction.isNotEmpty()

    /** Exact value of these digits read in [base]. Always terminates for bases 2, 8, 10, 16. */
    fun valueIn(base: NumberBase): BigDecimal {
        val radix = base.radix
        val whole = BigDecimal(BigInteger(integer.ifEmpty { "0" }, radix))
        if (fraction.isEmpty()) return whole
        val numerator = BigDecimal(BigInteger(fraction, radix))
        val denominator = BigDecimal(BigInteger.valueOf(radix.toLong()).pow(fraction.length))
        return whole.add(numerator.divide(denominator))
    }

    companion object {
        /** Splits plain decimal text such as "277.359375". */
        fun fromPlain(text: String): Digits {
            val point = text.indexOf('.')
            return if (point < 0) Digits(text) else Digits(text.substring(0, point), text.substring(point + 1))
        }
    }
}

/**
 * A validated, normalised number: upper-case digits, no leading zeros in the whole part
 * (except a single "0") and no trailing zeros in the fraction.
 */
data class ParsedNumber(
    val base: NumberBase,
    val negative: Boolean,
    val digits: Digits,
) {
    val isZero: Boolean get() = digits.integer == "0" && digits.fraction.isEmpty()
    val hasFraction: Boolean get() = digits.hasFraction
    val magnitudeText: String get() = digits.text
    val signedText: String get() = (if (negative) "-" else "") + digits.text
}

sealed interface ParseResult {
    data class Valid(val number: ParsedNumber, val notes: List<String>) : ParseResult
    data class Invalid(val message: String) : ParseResult
}

/** Checks user input against the rules of the selected source base. */
object InputValidator {
    const val MAX_DIGITS = 64

    fun validate(raw: String, base: NumberBase): ParseResult {
        val notes = mutableListOf<String>()
        val trimmed = raw.trim().replace('−', '-')
        if (trimmed.isEmpty()) {
            return ParseResult.Invalid("Enter a ${base.lowerName} number to convert.")
        }

        val compact = trimmed.filterNot { it == ' ' || it == '_' }
        if (compact.length != trimmed.length) {
            notes += "Spaces were removed — they are only used to make long numbers easier to read."
        }
        if (compact.isEmpty()) {
            return ParseResult.Invalid("Enter at least one digit.")
        }

        var body = compact
        var negative = false
        if (body[0] == '-' || body[0] == '+') {
            negative = body[0] == '-'
            body = body.substring(1)
        }
        if (body.isEmpty()) {
            return ParseResult.Invalid("Enter at least one digit after the sign.")
        }

        var pointIndex = -1
        for ((i, c) in body.withIndex()) {
            when {
                c == '.' -> {
                    if (pointIndex >= 0) {
                        return ParseResult.Invalid("A number can have only one ${base.pointName} (.).")
                    }
                    pointIndex = i
                }
                c == ',' -> return ParseResult.Invalid(
                    "Use a period (.) for the ${base.pointName}, not a comma."
                )
                c == '-' || c == '+' -> return ParseResult.Invalid(
                    "A sign (- or +) is only allowed at the very beginning of the number."
                )
                else -> {
                    val value = MathFormat.digitValue(c)
                    if (value < 0 || value >= base.radix) {
                        return ParseResult.Invalid(invalidDigitMessage(c, i, body, base))
                    }
                }
            }
        }

        val intRaw = if (pointIndex >= 0) body.substring(0, pointIndex) else body
        val fracRaw = if (pointIndex >= 0) body.substring(pointIndex + 1) else ""
        if (intRaw.isEmpty() && fracRaw.isEmpty()) {
            return ParseResult.Invalid("Enter at least one digit, not just a point.")
        }
        if (intRaw.length + fracRaw.length > MAX_DIGITS) {
            return ParseResult.Invalid("That number is too long. Please enter at most $MAX_DIGITS digits.")
        }

        if (base == NumberBase.HEXADECIMAL && body.any { it.isLowerCase() }) {
            notes += "Lowercase letters were written in uppercase (a = A = 10, …, f = F = 15)."
        }
        val intUpper = intRaw.uppercase()
        val fracUpper = fracRaw.uppercase()
        val intDigits = intUpper.trimStart('0').ifEmpty { "0" }
        val fracDigits = fracUpper.trimEnd('0')
        if (intUpper.length > 1 && intUpper.startsWith('0')) {
            notes += "Leading zeros were dropped (${intUpper} → $intDigits) — they do not change the value."
        }
        if (fracDigits.length < fracUpper.length) {
            notes += "Zeros at the end of the fraction were dropped — they do not change the value."
        }

        val digits = Digits(intDigits, fracDigits)
        val isZero = intDigits == "0" && fracDigits.isEmpty()
        if (negative && isZero) {
            notes += "-0 is the same as 0, so the minus sign was dropped."
        }
        return ParseResult.Valid(ParsedNumber(base, negative && !isZero, digits), notes)
    }

    private fun invalidDigitMessage(c: Char, index: Int, body: String, base: NumberBase): String {
        val prefixHint =
            if (index == 1 && body[0] == '0' && c.lowercaseChar() in "xob") {
                " Leave out the 0${c.lowercaseChar()} prefix and enter only the digits."
            } else {
                ""
            }
        return "'$c' is not a valid ${base.lowerName} digit. ${base.displayName} uses only ${base.allowedDigits}.$prefixHint"
    }
}
