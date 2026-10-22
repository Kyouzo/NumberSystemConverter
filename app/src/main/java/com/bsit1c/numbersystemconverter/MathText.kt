package com.bsit1c.numbersystemconverter

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em

/*
 * The engine writes bases and exponents with Unicode characters (348₁₀, 16⁻²). Many phone fonts —
 * especially monospace ones — don't contain most of those characters and show empty boxes instead.
 * So before display, every run of sub/superscript characters is turned back into ordinary digits
 * drawn smaller and shifted down/up. Ordinary digits exist in every font.
 */

private const val SUPERSCRIPTS = "⁰¹²³⁴⁵⁶⁷⁸⁹⁻"
private const val SUBSCRIPTS = "₀₁₂₃₄₅₆₇₈₉"
private const val PLAIN = "0123456789-"

private val SuperscriptStyle = SpanStyle(fontSize = 0.7.em, baselineShift = BaselineShift(0.45f))
private val SubscriptStyle = SpanStyle(fontSize = 0.7.em, baselineShift = BaselineShift(-0.2f))

/** Converts Unicode sub/superscript runs into styled ordinary digits. */
fun mathAnnotated(text: String): AnnotatedString {
    if (text.none { it in SUPERSCRIPTS || it in SUBSCRIPTS }) return AnnotatedString(text)
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            val c = text[i]
            val table = when (c) {
                in SUPERSCRIPTS -> SUPERSCRIPTS
                in SUBSCRIPTS -> SUBSCRIPTS
                else -> null
            }
            if (table == null) {
                append(c)
                i++
                continue
            }
            val start = i
            while (i < text.length && text[i] in table) i++
            val digits = text.substring(start, i).map { PLAIN[table.indexOf(it)] }.joinToString("")
            withStyle(if (table == SUPERSCRIPTS) SuperscriptStyle else SubscriptStyle) { append(digits) }
        }
    }
}

/** A Text that shows engine math strings correctly on every font. */
@Composable
fun MathText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
    maxLines: Int = Int.MAX_VALUE,
    softWrap: Boolean = true,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val annotated = remember(text) { mathAnnotated(text) }
    Text(
        annotated,
        modifier = modifier,
        style = style,
        color = color,
        maxLines = maxLines,
        softWrap = softWrap,
        overflow = overflow,
    )
}
