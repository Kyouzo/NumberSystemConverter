package com.bsit1c.numbersystemconverter

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.unit.dp

/**
 * The handful of icons the app needs, drawn as vectors so no icon library is required.
 * Stroke style, 24×24 grid, tinted by the Icon composable.
 */
object AppIcons {

    private fun icon(name: String, block: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            addPath(
                pathData = PathBuilder().apply(block).nodes,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }.build()

    val Copy: ImageVector by lazy {
        icon("Copy") {
            // back sheet
            moveTo(15f, 5f); lineTo(15f, 4f)
            arcToRelative(1f, 1f, 0f, false, false, -1f, -1f)
            lineTo(5f, 3f)
            arcToRelative(1f, 1f, 0f, false, false, -1f, 1f)
            lineTo(4f, 14f)
            arcToRelative(1f, 1f, 0f, false, false, 1f, 1f)
            lineTo(6f, 15f)
            // front sheet
            moveTo(10f, 7f); lineTo(19f, 7f)
            arcToRelative(1f, 1f, 0f, false, true, 1f, 1f)
            lineTo(20f, 20f)
            arcToRelative(1f, 1f, 0f, false, true, -1f, 1f)
            lineTo(10f, 21f)
            arcToRelative(1f, 1f, 0f, false, true, -1f, -1f)
            lineTo(9f, 8f)
            arcToRelative(1f, 1f, 0f, false, true, 1f, -1f)
            close()
        }
    }

    val Check: ImageVector by lazy {
        icon("Check") { moveTo(5f, 12.5f); lineTo(10f, 17.5f); lineTo(19f, 7f) }
    }

    /** Up and down arrows: swap source and target. */
    val Swap: ImageVector by lazy {
        icon("Swap") {
            moveTo(8f, 20f); lineTo(8f, 4f)
            moveTo(4f, 8f); lineTo(8f, 4f); lineTo(12f, 8f)
            moveTo(16f, 4f); lineTo(16f, 20f)
            moveTo(12f, 16f); lineTo(16f, 20f); lineTo(20f, 16f)
        }
    }

    val ChevronDown: ImageVector by lazy {
        icon("ChevronDown") { moveTo(6f, 9f); lineTo(12f, 15f); lineTo(18f, 9f) }
    }

    val Close: ImageVector by lazy {
        icon("Close") { moveTo(6f, 6f); lineTo(18f, 18f); moveTo(18f, 6f); lineTo(6f, 18f) }
    }
}
