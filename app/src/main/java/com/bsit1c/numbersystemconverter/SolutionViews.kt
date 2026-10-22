package com.bsit1c.numbersystemconverter

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * Composables for the flattened solution rows. Each row draws its own slice of the step "card"
 * (top, middle or bottom), so a whole step looks like one card while the LazyColumn only composes
 * the rows on screen. No shadows or blur: flat tonal surfaces are cheap to draw while scrolling.
 */

private val CardRadius = 20.dp
private val TopShape = RoundedCornerShape(topStart = CardRadius, topEnd = CardRadius)
private val BottomShape = RoundedCornerShape(bottomStart = CardRadius, bottomEnd = CardRadius)
private const val BLOCK_RADIUS = 10

@Composable
fun SolutionHeader(
    stepCount: Int,
    method: String,
    allExpanded: Boolean,
    onToggleAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = Spacing.l, bottom = Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Step-by-step solution", style = MaterialTheme.typography.titleLarge)
            Text(
                "$stepCount steps · $method",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onToggleAll) {
            Text(if (allExpanded) "Collapse all" else "Expand all")
        }
    }
}

/**
 * Draws one [SolutionRow].
 * @param charWidth width of one monospace character in the math style, measured once per screen.
 * @param tableScroll shared horizontal scroll state for each table, so all its rows scroll together.
 */
@Composable
fun SolutionRowItem(
    row: SolutionRow,
    collapsed: Boolean,
    charWidth: Dp,
    tableScroll: (TableLayout) -> ScrollState,
    onToggleStep: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardColor = MaterialTheme.colorScheme.surfaceContainerLow
    when (row) {
        is SolutionRow.StepHeader -> StepHeaderRow(row, collapsed, onToggleStep, modifier.background(cardColor, TopShape))
        is SolutionRow.Paragraph -> MathText(
            row.text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = modifier
                .fillMaxWidth()
                .background(cardColor)
                .padding(horizontal = Spacing.l, vertical = Spacing.xs),
        )
        is SolutionRow.FormulaLine -> FormulaLineRow(row, modifier.background(cardColor))
        is SolutionRow.TableLine -> TableLineRow(row, charWidth, tableScroll(row.layout), modifier.background(cardColor))
        is SolutionRow.StepEnd -> Column(modifier.fillMaxWidth()) {
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .height(Spacing.m)
                    .background(cardColor, BottomShape)
            )
            Spacer(Modifier.height(Spacing.m))
        }
    }
}

@Composable
private fun StepHeaderRow(
    row: SolutionRow.StepHeader,
    collapsed: Boolean,
    onToggleStep: (Int) -> Unit,
    modifier: Modifier,
) {
    val chevronRotation by animateFloatAsState(if (collapsed) -90f else 0f, label = "chevron")
    Column(
        modifier
            .fillMaxWidth()
            .clip(TopShape)
            .clickable(role = Role.Button, onClickLabel = if (collapsed) "Expand step" else "Collapse step") {
                onToggleStep(row.step)
            }
            .semantics { stateDescription = if (collapsed) "Collapsed" else "Expanded" }
            .padding(start = Spacing.l, end = Spacing.s, top = Spacing.l, bottom = Spacing.s),
    ) {
        row.stage?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(start = 40.dp, bottom = 2.dp),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(28.dp)
                    .background(
                        if (row.isFinal) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "${row.number}",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (row.isFinal) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onPrimary,
                )
            }
            Text(
                row.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = Spacing.m),
            )
            Icon(
                AppIcons.ChevronDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(Spacing.s)
                    .size(20.dp)
                    .graphicsLayer { rotationZ = chevronRotation },
            )
        }
    }
}

@Composable
private fun FormulaLineRow(row: SolutionRow.FormulaLine, modifier: Modifier) {
    val math = LocalMathStyles.current
    val shape: Shape = when {
        row.first && row.last -> RoundedCornerShape(BLOCK_RADIUS.dp)
        row.first -> RoundedCornerShape(topStart = BLOCK_RADIUS.dp, topEnd = BLOCK_RADIUS.dp)
        row.last -> RoundedCornerShape(bottomStart = BLOCK_RADIUS.dp, bottomEnd = BLOCK_RADIUS.dp)
        else -> RectangleShape
    }
    Box(
        modifier
            .fillMaxWidth()
            .padding(
                start = Spacing.l,
                end = Spacing.l,
                top = if (row.first) Spacing.xs else 0.dp,
                bottom = if (row.last) Spacing.xs else 0.dp,
            )
    ) {
        MathText(
            row.text,
            style = math.body,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, shape)
                .padding(
                    start = Spacing.m,
                    end = Spacing.m,
                    top = if (row.first) Spacing.m else 1.dp,
                    bottom = if (row.last) Spacing.m else 1.dp,
                ),
        )
    }
}

private val CellPadding = 10.dp

@Composable
private fun TableLineRow(row: SolutionRow.TableLine, charWidth: Dp, scroll: ScrollState, modifier: Modifier) {
    val math = LocalMathStyles.current
    val colors = MaterialTheme.colorScheme
    val style = if (row.isHeader) math.bodyBold else math.body
    Box(
        modifier
            .fillMaxWidth()
            .padding(
                start = Spacing.l,
                end = Spacing.l,
                top = if (row.isHeader) Spacing.s else 0.dp,
                bottom = if (row.last) Spacing.s else 0.dp,
            )
    ) {
        Row(Modifier.horizontalScroll(scroll)) {
            row.layout.columnChars.forEachIndexed { i, chars ->
                Box(
                    Modifier
                        .width(charWidth * (chars + 0.5f) + CellPadding * 2)
                        .background(if (row.isHeader) colors.secondaryContainer else colors.surfaceContainerLowest)
                        .border(0.5.dp, colors.outlineVariant)
                        .padding(horizontal = CellPadding, vertical = 7.dp),
                ) {
                    MathText(
                        row.cells.getOrElse(i) { "" },
                        style = style,
                        color = if (row.isHeader) colors.onSecondaryContainer else colors.onSurface,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                    )
                }
            }
        }
    }
}

/** Shown in the solution pane before anything has been converted. */
@Composable
fun SolutionPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        Text("No solution yet", style = MaterialTheme.typography.titleMedium)
        Text(
            "Convert a number and every step of the working will appear here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
