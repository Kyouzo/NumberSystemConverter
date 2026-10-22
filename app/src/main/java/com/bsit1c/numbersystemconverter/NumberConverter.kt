package com.bsit1c.numbersystemconverter

import com.bsit1c.numbersystemconverter.MathFormat.plain
import com.bsit1c.numbersystemconverter.MathFormat.withBase

/**
 * Entry point of the conversion engine. Pure Kotlin – no Android classes – so it can be unit-tested
 * on the JVM and reused by any UI.
 *
 * The solution steps are recorded *while* the answer is computed, so the steps shown are always the
 * calculation that actually produced the answer.
 *
 * @param maxFractionDigits how many digits after the point to produce when a fraction does not end
 *   in the target base (e.g. 0.1₁₀ in binary). Results cut off at this limit are marked approximate.
 */
class NumberConverter(val maxFractionDigits: Int = DEFAULT_FRACTION_DIGITS) {

    init {
        require(maxFractionDigits in 1..MAX_FRACTION_DIGITS) { "maxFractionDigits must be 1..$MAX_FRACTION_DIGITS" }
    }

    fun convert(input: String, from: NumberBase, to: NumberBase): ConversionOutcome {
        val parsed = when (val p = InputValidator.validate(input, from)) {
            is ParseResult.Invalid -> return ConversionOutcome.Error(p.message)
            is ParseResult.Valid -> p
        }
        val number = parsed.number
        val out = SolutionBuilder()

        if (parsed.notes.isNotEmpty() || number.negative) {
            out.step("Prepare the number") {
                parsed.notes.forEach { text(it) }
                if (number.negative) {
                    text(
                        "The number is negative. Convert its size (without the minus sign), " +
                            "then put the minus sign back in front of the answer."
                    )
                }
                math("Number to convert: ${withBase(number.signedText, from)}")
            }
        }

        val method: String
        val output: MethodOutput
        when {
            from == to -> {
                method = "Same number system"
                out.step("Nothing to convert") {
                    text(
                        "The number is already in ${from.lowerName}, so its digits stay the same. " +
                            "A number has the same value no matter how it is written."
                    )
                }
                output = MethodOutput(number.digits)
            }

            from == NumberBase.DECIMAL -> {
                method = if (number.hasFraction) {
                    "Repeated division and multiplication by ${to.radix}"
                } else {
                    "Repeated division by ${to.radix}"
                }
                output = DecimalToBaseMethod.convert(number, to, maxFractionDigits, out)
            }

            to == NumberBase.DECIMAL -> {
                method = "Positional notation (powers of ${from.radix})"
                output = PositionalMethod.convert(number, out)
            }

            from == NumberBase.BINARY -> {
                method = "Grouping bits in sets of ${to.bitsPerDigit}"
                output = MethodOutput(BinaryGroupingMethod.groupBits(number.digits, to, out))
            }

            to == NumberBase.BINARY -> {
                method = "Each ${from.lowerName} digit → ${from.bitsPerDigit} bits"
                output = MethodOutput(BinaryGroupingMethod.expandDigits(number.digits, from, out))
            }

            else -> { // octal ↔ hexadecimal
                method = "Through binary (${from.shortName} → Binary → ${to.shortName})"
                out.step("Plan: go through binary") {
                    text(
                        "There is no direct shortcut between octal and hexadecimal, but both are easy to convert " +
                            "to and from binary: 1 octal digit = 3 bits and 1 hex digit = 4 bits. " +
                            "So convert ${from.lowerName} → binary first, then binary → ${to.lowerName}."
                    )
                }
                out.stage = "Stage 1 · ${from.displayName} → Binary"
                val bits = BinaryGroupingMethod.expandDigits(number.digits, from, out)
                out.stage = "Stage 2 · Binary → ${to.displayName}"
                val digits = BinaryGroupingMethod.groupBits(bits, to, out)
                out.stage = null
                output = MethodOutput(digits)
            }
        }

        val answer = (if (number.negative) "-" else "") + output.digits.text
        val result = ConversionResult(
            from = from,
            to = to,
            input = number.signedText,
            answer = answer,
            isApproximate = output.approximate,
            method = method,
            steps = emptyList(),
        )

        addCheck(number, output, from, to, out)

        out.step("Final answer") {
            if (number.negative) text("Put the minus sign back in front of the answer.")
            math(result.equation)
            if (output.approximate) {
                text(
                    "≈ means \"approximately equal\": this fraction never ends in ${to.lowerName}, " +
                        "so it is shown cut off after $maxFractionDigits digits."
                )
            }
        }

        return ConversionOutcome.Success(result.copy(steps = out.build()))
    }

    /** Optional verification step, like the "check by converting to decimal" in class notes. */
    private fun addCheck(number: ParsedNumber, output: MethodOutput, from: NumberBase, to: NumberBase, out: SolutionBuilder) {
        if (from == to || to == NumberBase.DECIMAL || output.approximate) return
        val inputValue = number.digits.valueIn(from)
        val answerValue = output.digits.valueIn(to)
        val ok = inputValue.compareTo(answerValue) == 0
        out.step("Check (optional)") {
            if (from == NumberBase.DECIMAL) {
                text("Convert the answer back to decimal. You should get the number you started with.")
                math(
                    PositionalMethod.compactExpansion(output.digits, to),
                    if (ok) "${plain(answerValue)} = ${plain(inputValue)} ✓" else "${plain(answerValue)} ≠ ${plain(inputValue)} ✗",
                )
            } else {
                text("Convert both numbers to decimal. If the conversion is right, both give the same value.")
                math(
                    PositionalMethod.compactExpansion(number.digits, from),
                    PositionalMethod.compactExpansion(output.digits, to),
                    if (ok) "Both equal ${plain(inputValue)}, so the conversion is correct ✓" else "The values differ ✗",
                )
            }
        }
    }

    companion object {
        const val DEFAULT_FRACTION_DIGITS = 12
        const val MAX_FRACTION_DIGITS = 64
        val FRACTION_DIGIT_CHOICES = listOf(8, 12, 16, 24)
    }
}
