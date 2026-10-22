package com.bsit1c.numbersystemconverter

import com.bsit1c.numbersystemconverter.MathFormat.plain
import com.bsit1c.numbersystemconverter.MathFormat.power
import com.bsit1c.numbersystemconverter.MathFormat.withBase
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Binary / octal / hexadecimal → decimal, using positional notation:
 * multiply every digit by its place value (a power of the base) and add the results.
 * Negative powers are used for digits after the radix point. Always exact.
 */
internal object PositionalMethod {

    private data class Term(
        val symbol: Char,
        val value: Int,
        val position: Int,
        val placeValue: BigDecimal,
    ) {
        val product: BigDecimal get() = placeValue.multiply(BigDecimal(value))
    }

    fun convert(number: ParsedNumber, out: SolutionBuilder): MethodOutput {
        val base = number.base
        val b = base.radix
        val isHex = base == NumberBase.HEXADECIMAL
        val terms = terms(number.digits, base)

        out.step("Write the place value of each digit") {
            text(
                "Number the positions from the ${base.pointName}: the digit just left of it is position 0, " +
                    "and the positions go up by 1 as you move left." +
                    (if (number.hasFraction) " Digits after the point get negative positions: −1, −2, −3, …" else "") +
                    " Each place value is $b raised to the position."
            )
            if (isHex) {
                text("Remember the letter values: A = 10, B = 11, C = 12, D = 13, E = 14, F = 15.")
            }
            table(
                headers = buildList {
                    add("Digit")
                    if (isHex) add("Value")
                    add("Position")
                    add("Place value")
                },
                rows = terms.map { t ->
                    buildList {
                        add(t.symbol.toString())
                        if (isHex) add(t.value.toString())
                        add(t.position.toString().replace('-', '−'))
                        add(placeValueLabel(b, t))
                    }
                },
            )
        }

        out.step("Multiply each digit by its place value") {
            math(
                withBase(number.magnitudeText, base) + " = " +
                    terms.joinToString(" + ") { "${it.value} × ${power(b, it.position)}" }
            )
            math(terms.map { "${it.value} × ${power(b, it.position)} = ${it.value} × ${plain(it.placeValue)} = ${plain(it.product)}" })
            if (base == NumberBase.BINARY) {
                text("A bit that is 0 always gives 0, so only the 1 bits add anything to the total.")
            }
        }

        val total = terms.fold(BigDecimal.ZERO) { acc, t -> acc.add(t.product) }
        out.step("Add the results") {
            val sumLine = terms.joinToString(" + ") { plain(it.product) } + " = " + plain(total)
            math(
                sumLine,
                "${withBase(number.magnitudeText, base)} = ${withBase(plain(total), NumberBase.DECIMAL)}",
            )
        }
        return MethodOutput(Digits.fromPlain(plain(total)))
    }

    private fun terms(digits: Digits, base: NumberBase): List<Term> {
        val b = BigInteger.valueOf(base.radix.toLong())
        val list = mutableListOf<Term>()
        val n = digits.integer.length
        digits.integer.forEachIndexed { i, c ->
            val pos = n - 1 - i
            list += Term(c, MathFormat.digitValue(c), pos, BigDecimal(b.pow(pos)))
        }
        digits.fraction.forEachIndexed { j, c ->
            val pos = -(j + 1)
            list += Term(c, MathFormat.digitValue(c), pos, BigDecimal.ONE.divide(BigDecimal(b.pow(j + 1))))
        }
        return list
    }

    private fun placeValueLabel(b: Int, t: Term): String =
        if (t.position >= 0) {
            "${power(b, t.position)} = ${plain(t.placeValue)}"
        } else {
            val denominator = BigInteger.valueOf(b.toLong()).pow(-t.position)
            "${power(b, t.position)} = 1/$denominator = ${plain(t.placeValue)}"
        }

    /**
     * Compact "1×2⁵ + 1×2³ = 32 + 8 = 40" line used by the answer checks.
     * Zero digits are left out because they add nothing.
     */
    fun compactExpansion(digits: Digits, base: NumberBase): String {
        val b = base.radix
        val nonZero = terms(digits, base).filter { it.value != 0 }
        val total = nonZero.fold(BigDecimal.ZERO) { acc, t -> acc.add(t.product) }
        val head = withBase(digits.text, base)
        if (nonZero.isEmpty()) return "$head = 0"
        val expr = nonZero.joinToString(" + ") { "${it.value}×${power(b, it.position)}" }
        if (nonZero.size == 1 && nonZero[0].value == 1 && nonZero[0].position == 0) return "$head = 1"
        val sums = nonZero.joinToString(" + ") { plain(it.product) }
        return if (nonZero.size == 1) "$head = $expr = ${plain(total)}" else "$head = $expr = $sums = ${plain(total)}"
    }
}
