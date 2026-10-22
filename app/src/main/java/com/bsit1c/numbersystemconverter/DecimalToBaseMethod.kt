package com.bsit1c.numbersystemconverter

import com.bsit1c.numbersystemconverter.MathFormat.digitChar
import com.bsit1c.numbersystemconverter.MathFormat.plain
import com.bsit1c.numbersystemconverter.MathFormat.valueWithLetter
import com.bsit1c.numbersystemconverter.MathFormat.withBase
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Decimal → binary / octal / hexadecimal.
 *
 * Whole part: repeated division by the target base, remainders read bottom to top.
 * Fraction part: repeated multiplication by the target base, whole parts read top to bottom.
 *
 * Every number is held as an exact BigInteger / BigDecimal, so no floating-point error can creep in.
 */
internal object DecimalToBaseMethod {

    fun convert(
        number: ParsedNumber,
        target: NumberBase,
        maxFractionDigits: Int,
        out: SolutionBuilder,
    ): MethodOutput {
        val digits = number.digits
        if (digits.hasFraction) {
            out.step("Split the number at the decimal point") {
                text(
                    "The whole-number part and the fractional part are converted with different " +
                        "methods: divide the whole part, multiply the fraction. Then join the results with a point."
                )
                math(
                    "Whole-number part: ${digits.integer}",
                    "Fractional part:   0.${digits.fraction}",
                )
            }
        }

        val whole = convertWholePart(BigInteger(digits.integer), target, out)
        if (!digits.hasFraction) return MethodOutput(Digits(whole))

        val fraction = convertFractionPart(BigDecimal("0.${digits.fraction}"), target, maxFractionDigits, out)

        out.step("Join the two parts") {
            text("Write the converted whole-number part, then the point, then the converted fraction digits.")
            math(
                "Whole-number part: ${withBase(digits.integer, NumberBase.DECIMAL)} = ${withBase(whole, target)}",
                "Fractional part:   ${withBase("0." + digits.fraction, NumberBase.DECIMAL)} " +
                    "${if (fraction.approximate) "≈" else "="} ${withBase("0." + fraction.digits, target)}",
                "Together:          ${withBase(whole + "." + fraction.digits, target)}",
            )
        }
        return MethodOutput(Digits(whole, fraction.digits), fraction.approximate)
    }

    // ---------------------------------------------------------------- whole part

    private fun convertWholePart(value: BigInteger, target: NumberBase, out: SolutionBuilder): String {
        val b = target.radix
        val isHex = target == NumberBase.HEXADECIMAL
        val title = "Divide the whole-number part repeatedly by $b"

        if (value.signum() == 0) {
            out.step(title) {
                text("The whole-number part is 0. Zero is written as 0 in every number system, so there is nothing to divide.")
                math("${withBase("0", NumberBase.DECIMAL)} = ${withBase("0", target)}")
            }
            return "0"
        }

        val bigB = BigInteger.valueOf(b.toLong())
        val decB = BigDecimal(b)
        val rows = mutableListOf<List<String>>()
        val remainders = mutableListOf<Int>()
        var v = value
        while (v.signum() > 0) {
            val qr = v.divideAndRemainder(bigB)
            val quotient = qr[0]
            val remainder = qr[1].toInt()
            val exact = BigDecimal(v).divide(decB) // always terminates for b = 2, 8, 16
            val decimalPart = exact.subtract(BigDecimal(quotient))
            val remainderCell =
                if (decimalPart.signum() == 0) "0" else "${plain(decimalPart)} × $b = $remainder"
            rows.add(buildList<String> {
                add("$v ÷ $b")
                add(plain(exact))
                add(quotient.toString())
                add(remainderCell)
                if (isHex) add(digitChar(remainder).toString())
            })
            remainders += remainder
            v = quotient
        }

        out.step(title) {
            text(
                "Divide the number by $b. The whole part of the result is the quotient (Q). " +
                    "To find the remainder (R), multiply the decimal part of the result by $b. " +
                    "Then divide the quotient by $b again, and stop when the quotient becomes 0."
            )
            if (isHex) {
                text("In hexadecimal, remainders 10–15 are written as letters: 10 = A, 11 = B, 12 = C, 13 = D, 14 = E, 15 = F.")
            }
            table(
                headers = buildList {
                    add("Division"); add("Result"); add("Quotient (Q)"); add("Remainder (R)")
                    if (isHex) add("Hex digit")
                },
                rows = rows,
            )
        }

        val answer = remainders.reversed().map { digitChar(it) }.joinToString("")
        out.step("Read the remainders from bottom to top") {
            text(
                "The first remainder is the least significant digit (rightmost). " +
                    "The last remainder is the most significant digit (leftmost). " +
                    "So write the remainders starting from the last one."
            )
            math(
                "Remainders (first → last): " + remainders.joinToString(", ") { valueWithLetter(it) },
                "Read last → first:         " + answer.toList().joinToString(" "),
                "${withBase(value.toString(), NumberBase.DECIMAL)} = ${withBase(answer, target)}",
            )
        }
        return answer
    }

    // ---------------------------------------------------------------- fraction part

    private data class FractionOutput(val digits: String, val approximate: Boolean)

    private fun convertFractionPart(
        start: BigDecimal,
        target: NumberBase,
        maxFractionDigits: Int,
        out: SolutionBuilder,
    ): FractionOutput {
        val b = target.radix
        val decB = BigDecimal(b)
        val rows = mutableListOf<List<String>>()
        val produced = mutableListOf<Int>()
        val seen = HashMap<String, Int>() // fraction value → index of the multiplication that used it
        var repeatFrom = -1
        var f = start

        while (produced.size < maxFractionDigits && f.signum() != 0) {
            val key = plain(f)
            val earlier = seen[key]
            if (earlier != null) {
                repeatFrom = earlier
                break
            }
            seen[key] = produced.size
            val product = f.multiply(decB)
            val digit = product.toBigInteger().toInt()
            val next = product.subtract(BigDecimal(digit))
            rows += listOf(
                "${plain(f)} × $b",
                plain(product),
                if (digit >= 10) "$digit → ${digitChar(digit)}" else "$digit",
                plain(next),
            )
            produced += digit
            f = next
        }

        val exact = f.signum() == 0
        val shown = produced.toMutableList()
        var period: List<Int> = emptyList()
        if (repeatFrom >= 0) {
            period = produced.subList(repeatFrom, produced.size).toList()
            var i = 0
            while (shown.size < maxFractionDigits) {
                shown += period[i % period.size]
                i++
            }
        }
        val shownText = shown.map { digitChar(it) }.joinToString("")
        val producedText = produced.map { digitChar(it) }.joinToString("")

        out.step("Multiply the fractional part repeatedly by $b") {
            text(
                "Multiply the fraction by $b. The whole part of the product is the next digit" +
                    (if (target == NumberBase.HEXADECIMAL) " (10–15 are written as A–F)" else "") +
                    ". Keep only the fractional part and multiply it by $b again. " +
                    "Stop when the fractional part becomes 0."
            )
            table(listOf("Multiplication", "Product", "Digit", "Fraction left"), rows)
            when {
                exact -> text("The fraction left is now 0, so the conversion is exact.")
                repeatFrom >= 0 -> text(
                    "The fraction ${plain(f)} already appeared in row ${repeatFrom + 1}, so from here the same " +
                        "digits (${period.joinToString("") { digitChar(it).toString() }}) repeat forever. " +
                        "The fraction never reaches 0, so the answer is cut off after $maxFractionDigits digits."
                )
                else -> text(
                    "The fraction is still not 0 after $maxFractionDigits digits, so the answer is cut off " +
                        "(truncated, not rounded) at $maxFractionDigits digits and is approximate."
                )
            }
        }

        out.step("Read the digits from top to bottom") {
            text("Unlike the division method, the first digit you get is the first digit after the point.")
            val lines = mutableListOf(
                "Digits (first → last): " + producedText.toList().joinToString(" ")
            )
            if (repeatFrom >= 0) {
                val head = producedText.substring(0, repeatFrom)
                val cycle = producedText.substring(repeatFrom)
                lines += "Repeating form: 0.$head($cycle)…  (the part in brackets repeats)"
                lines += "Shown to $maxFractionDigits digits: 0.$shownText"
            }
            lines += "${withBase("0." + start.toPlainStringSafe(), NumberBase.DECIMAL)} " +
                "${if (exact) "=" else "≈"} ${withBase("0.$shownText", target)}"
            math(lines)
        }
        return FractionOutput(shownText, approximate = !exact)
    }

    /** Digits after "0." of a fraction in [0, 1). */
    private fun BigDecimal.toPlainStringSafe(): String = plain(this).removePrefix("0.")
}
