package com.bsit1c.numbersystemconverter

/**
 * The four number systems supported by the converter.
 *
 * Part of the conversion engine: no Android dependencies.
 */
enum class NumberBase(
    val radix: Int,
    val displayName: String,
    val shortName: String,
) {
    BINARY(2, "Binary", "Binary"),
    OCTAL(8, "Octal", "Octal"),
    DECIMAL(10, "Decimal", "Decimal"),
    HEXADECIMAL(16, "Hexadecimal", "Hex");

    /** Lower-case name for use inside sentences ("a binary digit"). */
    val lowerName: String get() = displayName.lowercase()

    /** Number of bits one digit stands for (3 for octal, 4 for hex). 0 for decimal. */
    val bitsPerDigit: Int
        get() = when (this) {
            BINARY -> 1
            OCTAL -> 3
            HEXADECIMAL -> 4
            DECIMAL -> 0
        }

    /** Human description of the legal digits, e.g. "the digits 0–7". */
    val allowedDigits: String
        get() = when (this) {
            BINARY -> "the digits 0 and 1"
            OCTAL -> "the digits 0–7"
            DECIMAL -> "the digits 0–9"
            HEXADECIMAL -> "the digits 0–9 and the letters A–F"
        }

    /** Name of the radix point in this base ("binary point", "decimal point", …). */
    val pointName: String get() = "$lowerName point"

    /** A short example used as an input placeholder. */
    val example: String
        get() = when (this) {
            BINARY -> "e.g. 101011100"
            OCTAL -> "e.g. 612"
            DECIMAL -> "e.g. 348"
            HEXADECIMAL -> "e.g. 15C"
        }
}
