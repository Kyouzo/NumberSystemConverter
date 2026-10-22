package com.bsit1c.numbersystemconverter

import com.bsit1c.numbersystemconverter.MathFormat.bitSum
import com.bsit1c.numbersystemconverter.MathFormat.digitChar
import com.bsit1c.numbersystemconverter.MathFormat.groupWeights
import com.bsit1c.numbersystemconverter.MathFormat.withBase

/**
 * Shortcuts between binary and its "power-of-two" relatives:
 *  - binary → octal / hex: group bits in 3s / 4s starting at the binary point.
 *  - octal / hex → binary: replace every digit with its 3-bit / 4-bit code.
 */
internal object BinaryGroupingMethod {

    /** Binary → octal (groups of 3) or hexadecimal (groups of 4). */
    fun groupBits(bits: Digits, target: NumberBase, out: SolutionBuilder): Digits {
        val k = target.bitsPerDigit
        val intPadded = bits.integer.padStart(roundUp(bits.integer.length, k), '0')
        val fracPadded = bits.fraction.padEnd(roundUp(bits.fraction.length, k), '0')
        val intGroups = intPadded.chunked(k)
        val fracGroups = if (fracPadded.isEmpty()) emptyList() else fracPadded.chunked(k)
        val leadingAdded = intPadded.length - bits.integer.length
        val trailingAdded = fracPadded.length - bits.fraction.length
        val hasFraction = fracGroups.isNotEmpty()

        out.step("Group the bits in sets of $k") {
            text(
                "Start at the binary point. Going left, split the whole-number bits into groups of $k." +
                    (if (hasFraction) " Going right, split the fraction bits into groups of $k." else "") +
                    " If a group is short, fill it with zeros on the outside" +
                    (if (hasFraction) " (in front for the whole part, at the end for the fraction)" else " (in front)") +
                    ". These zeros do not change the value."
            )
            val lines = mutableListOf("Bits:    ${bits.text}")
            lines += "Grouped: " + intGroups.joinToString(" ") +
                (if (hasFraction) " . " + fracGroups.joinToString(" ") else "")
            math(lines)
            if (leadingAdded > 0) text("Added $leadingAdded zero${plural(leadingAdded)} in front to complete the leftmost group.")
            if (trailingAdded > 0) text("Added $trailingAdded zero${plural(trailingAdded)} at the end to complete the last fraction group.")
        }

        val weights = groupWeights(k)
        val intResult = intGroups.map { digitChar(it.toInt(2)) }.joinToString("")
        val fracResult = fracGroups.map { digitChar(it.toInt(2)) }.joinToString("")

        out.step("Convert each group to one ${target.lowerName} digit") {
            text(
                "Inside every group the bits have the place values ${weights.joinToString(", ")}. " +
                    "Add the place values where the bit is 1." +
                    (if (target == NumberBase.HEXADECIMAL) " Values 10–15 become the letters A–F." else "")
            )
            table(
                headers = buildList {
                    if (hasFraction) add("Part")
                    add("Group"); add("Value"); add("${target.shortName} digit")
                },
                rows = intGroups.map { groupRow(it, "whole", hasFraction) } +
                    fracGroups.map { groupRow(it, "fraction", hasFraction) },
            )
        }

        val result = Digits(intResult.trimStart('0').ifEmpty { "0" }, fracResult.trimEnd('0'))
        out.step("Write the digits together") {
            text("Keep the digits in the same order as the groups" + (if (hasFraction) ", with the point in the same place." else "."))
            math("${withBase(bits.text, NumberBase.BINARY)} = ${withBase(result.text, target)}")
        }
        return result
    }

    /** Octal → binary (3 bits per digit) or hexadecimal → binary (4 bits per digit). */
    fun expandDigits(source: Digits, from: NumberBase, out: SolutionBuilder): Digits {
        val k = from.bitsPerDigit
        val weights = groupWeights(k)
        val hasFraction = source.hasFraction
        val code = { c: Char -> MathFormat.digitValue(c).toString(2).padStart(k, '0') }

        out.step("Replace each ${from.lowerName} digit with $k bits") {
            text(
                "Every ${from.lowerName} digit becomes exactly $k binary digits (bits). " +
                    "Use the place values ${weights.joinToString(", ")}: write 1 under the place values that add up " +
                    "to the digit, and 0 under the others."
            )
            math(referenceTable(from))
            table(
                headers = buildList {
                    if (hasFraction) add("Part")
                    add("${from.shortName} digit"); add("Value"); add("$k bits")
                },
                rows = source.integer.map { digitRow(it, from, "whole", hasFraction, code) } +
                    source.fraction.map { digitRow(it, from, "fraction", hasFraction, code) },
            )
        }

        val intGroups = source.integer.map(code)
        val fracGroups = source.fraction.map(code)
        val rawInt = intGroups.joinToString("")
        val rawFrac = fracGroups.joinToString("")
        val result = Digits(rawInt.trimStart('0').ifEmpty { "0" }, rawFrac.trimEnd('0'))

        out.step("Join the groups") {
            text("Write the groups side by side in the same order" + (if (hasFraction) ", keeping the point in place." else "."))
            val grouped = intGroups.joinToString(" ") + (if (hasFraction) " . " + fracGroups.joinToString(" ") else "")
            val raw = rawInt + (if (hasFraction) ".$rawFrac" else "")
            math("$grouped  →  $raw")
            if (result.integer != rawInt || result.fraction != rawFrac) {
                text(
                    "Remove the unnecessary zeros" +
                        (if (result.integer != rawInt) " in front of the number" else "") +
                        (if (result.integer != rawInt && result.fraction != rawFrac) " and" else "") +
                        (if (result.fraction != rawFrac) " at the end of the fraction" else "") +
                        " — they do not change the value."
                )
                math("$raw  →  ${result.text}")
            }
            math("${withBase(source.text, from)} = ${withBase(result.text, NumberBase.BINARY)}")
        }
        return result
    }

    private fun groupRow(group: String, part: String, showPart: Boolean): List<String> {
        val value = group.toInt(2)
        return buildList {
            if (showPart) add(part)
            add(group)
            add(bitSum(group))
            add(digitChar(value).toString())
        }
    }

    private fun digitRow(
        c: Char,
        from: NumberBase,
        part: String,
        showPart: Boolean,
        code: (Char) -> String,
    ): List<String> {
        val value = MathFormat.digitValue(c)
        val bits = code(c)
        val sum = bitSum(bits) // e.g. "4 + 1 = 5"
        val valueCell = when {
            value >= 10 -> "$value = ${sum.substringBefore(" =")}"
            sum.contains('=') -> "$value = ${sum.substringBefore(" =")}"
            else -> "$value"
        }
        return buildList {
            if (showPart) add(part)
            add(c.toString())
            add(valueCell)
            add(bits)
        }
    }

    private fun referenceTable(from: NumberBase): List<String> {
        val k = from.bitsPerDigit
        val entries = (0 until from.radix).map { "${digitChar(it)}→${it.toString(2).padStart(k, '0')}" }
        return entries.chunked(4).map { it.joinToString("  ") }
    }

    private fun roundUp(length: Int, k: Int): Int = if (length == 0) 0 else ((length + k - 1) / k) * k

    private fun plural(n: Int) = if (n == 1) "" else "s"
}
