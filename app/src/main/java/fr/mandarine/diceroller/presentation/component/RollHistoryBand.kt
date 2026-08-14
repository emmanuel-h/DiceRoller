// app/src/main/java/fr/mandarine/diceroller/presentation/component/RollHistoryBand.kt
package fr.mandarine.diceroller.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.mandarine.diceroller.R
import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.domain.DiceGroupResult
import fr.mandarine.diceroller.domain.DicePoolResult
import fr.mandarine.diceroller.domain.DieType
import fr.mandarine.diceroller.domain.RollRecord
import fr.mandarine.diceroller.domain.ValueTally
import fr.mandarine.diceroller.presentation.model.DiceColor
import fr.mandarine.diceroller.presentation.notation
import fr.mandarine.diceroller.presentation.relativeTimeLabel
import fr.mandarine.diceroller.ui.theme.DiceRollerTheme

/** Test tag on the header row, which is also the expand/collapse control. */
const val ROLL_HISTORY_HEADER_TAG: String = "roll-history-header"

/** Test tag on the scrollable list of entries, present only while the band is expanded. */
const val ROLL_HISTORY_LIST_TAG: String = "roll-history-list"

/** Rotation applied to [R.drawable.ic_expand_more] so one asset serves both states. */
private const val EXPANDED_CHEVRON_ROTATION = 180f

/** Separator drawn between two die-type runs on an entry's face line. */
private const val GROUP_SEPARATOR = "·"

private val CHEVRON_SIZE = 18.dp
private val HEADER_SPACING = 6.dp

/** Collapsed, the band is exactly this tall — small enough to give back to the result band,
 * large enough to stay a reachable touch target. */
private val HEADER_MIN_HEIGHT = 40.dp
private val ENTRY_VERTICAL_PADDING = 8.dp
private val ENTRY_LINE_SPACING = 4.dp
private val FACE_SPACING = 8.dp
private val FACE_INTERNAL_SPACING = 2.dp

/**
 * The roll log as a collapsible band sitting between the live result and the Roll button.
 *
 * Collapsed it costs a single header row, which is the price of keeping the screen's scroll-free
 * four-band layout (issue #64) intact: the band is *absent* entirely until the first roll, so a
 * first launch is pixel-identical to the screen before history existed. Callers are expected to
 * give it a bounded height when [isExpanded] — the entry list scrolls within whatever it is
 * given rather than pushing the Roll button off screen.
 *
 * The log is append-only: there is deliberately no way to clear it from the UI. The only thing
 * that ever removes an entry is the [fr.mandarine.diceroller.presentation.MAX_HISTORY_RECORDS]
 * cap pushing the oldest off the end.
 *
 * Each entry shows what was rolled in dice notation with its total, then the individual faces as
 * [DiceImageSize.Compact] artwork, then how long ago it happened. The faces are drawn in
 * [selectedColor] — the current colour, not one frozen per record — because colour is a
 * display-time choice throughout this app: picking one never even clears the live result.
 *
 * @param history past rolls, newest first; the band renders nothing when this is empty
 * @param isExpanded whether the entry list is shown below the header
 * @param nowMillis reference time the relative timestamps are measured against
 * @param selectedColor the colour variant every die in the log is rendered in
 * @param onToggleExpanded called when the header is tapped
 * @param modifier optional [Modifier] applied to the root container
 */
@Composable
fun RollHistoryBand(
    history: List<RollRecord>,
    isExpanded: Boolean,
    nowMillis: Long,
    selectedColor: DiceColor,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (history.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider()
        HistoryHeader(
            entryCount = history.size,
            isExpanded = isExpanded,
            onToggleExpanded = onToggleExpanded,
        )
        if (isExpanded) {
            // Lazy is safe — and worth it — only here: the parent bounds this band's height when
            // expanded, and the log can hold up to MAX_HISTORY_RECORDS entries of artwork.
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag(ROLL_HISTORY_LIST_TAG),
            ) {
                // No item key: two rolls a millisecond apart would otherwise collide on their
                // timestamp, and a duplicate key is a crash rather than a glitch.
                items(items = history) { record ->
                    RollHistoryEntry(
                        record = record,
                        nowMillis = nowMillis,
                        color = selectedColor,
                    )
                }
            }
        }
    }
}

/**
 * The always-visible row: the chevron and the entry count, and nothing else.
 *
 * The header carries no destructive action — the log cannot be cleared from the UI at all — so
 * the whole row is one control with one meaning: open or close the list.
 */
@Composable
private fun HistoryHeader(
    entryCount: Int,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stateWord = if (isExpanded) "expanded" else "collapsed"
    val entryWord = if (entryCount == 1) "roll" else "rolls"
    Row(
        modifier = modifier
            .fillMaxWidth()
            // The band's whole cost to the screen, so it is kept to the smallest row that is
            // still a comfortable touch target rather than trimmed further (issue #64).
            .heightIn(min = HEADER_MIN_HEIGHT)
            .clickable(onClick = onToggleExpanded)
            .testTag(ROLL_HISTORY_HEADER_TAG)
            .semantics(mergeDescendants = true) {
                contentDescription = "Recent rolls, $entryCount $entryWord, $stateWord"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HEADER_SPACING),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_expand_more),
            contentDescription = null,
            modifier = Modifier
                .size(CHEVRON_SIZE)
                .rotate(if (isExpanded) EXPANDED_CHEVRON_ROTATION else 0f),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Recent ($entryCount)",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** One past roll: notation and total, the faces that produced it, and how long ago it was. */
@Composable
private fun RollHistoryEntry(
    record: RollRecord,
    nowMillis: Long,
    color: DiceColor,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = ENTRY_VERTICAL_PADDING)
            .semantics(mergeDescendants = true) {
                contentDescription = record.toAccessibilityLabel(nowMillis)
            },
        verticalArrangement = Arrangement.spacedBy(ENTRY_LINE_SPACING),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = record.result.notation(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = record.result.total.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        FaceLine(result = record.result, color = color)
        Text(
            text = relativeTimeLabel(
                rolledAtMillis = record.rolledAtMillis,
                nowMillis = nowMillis,
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The faces of one past roll, in the same order the live result uses: groups smallest-to-largest
 * by die, values descending within a group, separated by a [GROUP_SEPARATOR] between die types.
 *
 * Repeats are shown as `×N` rather than as N copies of the artwork, so a 20-die group stays one
 * short run instead of twenty icons — the same compression the live face-ladder uses.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FaceLine(
    result: DicePoolResult,
    color: DiceColor,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(FACE_SPACING),
        verticalArrangement = Arrangement.spacedBy(ENTRY_LINE_SPACING),
        // Faces and the separator between them differ in height; centring keeps the separator on
        // the dice's midline instead of floating at the top of the line.
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        result.groups.forEachIndexed { index, group ->
            if (index > 0) {
                Text(
                    text = GROUP_SEPARATOR,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            group.tallies.forEach { tally ->
                HistoryFace(dice = group.dice, tally = tally, color = color)
            }
        }
    }
}

@Composable
private fun HistoryFace(
    dice: DieType,
    tally: ValueTally,
    color: DiceColor,
    modifier: Modifier = Modifier,
) {
    // No semantics of its own: the entry above merges its descendants and carries an explicit
    // contentDescription, which takes precedence over the numerals here — so a screen reader
    // hears the entry's one sentence, while the numerals stay in the tree for tests to find.
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FACE_INTERNAL_SPACING),
    ) {
        DiceImage(
            dice = dice,
            color = color,
            sizeVariant = DiceImageSize.Compact,
            contentDescription = null,
        )
        Text(
            text = tally.value.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (tally.count > 1) {
            Text(
                text = "×${tally.count}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * The whole entry as one spoken phrase, e.g.
 * `"4D6 + 2D8, total 40, just now. D6: 6, 4 twice, 3. D8: 7, 2."`
 *
 * Merged onto the entry rather than left to the individual faces, which are cleared from the
 * semantics tree — a screen reader stepping through 20 separate numerals to hear one past roll
 * would be far worse than one sentence.
 */
private fun RollRecord.toAccessibilityLabel(nowMillis: Long): String {
    val when_ = relativeTimeLabel(rolledAtMillis = rolledAtMillis, nowMillis = nowMillis)
    val faces = result.groups.joinToString(separator = " ") { group ->
        val values = group.tallies.joinToString(separator = ", ") { tally ->
            if (tally.count > 1) "${tally.value} ${tally.count} times" else "${tally.value}"
        }
        "${group.dice.label}: $values."
    }
    return "${result.notation()}, total ${result.total}, $when_. $faces"
}

// -- Previews -----------------------------------------------------------------

private val PREVIEW_NOW = 1_700_000_000_000L

private val PREVIEW_HISTORY = listOf(
    RollRecord(
        result = DicePoolResult(
            groups = listOf(
                DiceGroupResult(
                    dice = Dice.D6,
                    poolCount = 4,
                    tallies = listOf(
                        ValueTally(value = 6, count = 1),
                        ValueTally(value = 4, count = 2),
                        ValueTally(value = 3, count = 1),
                    ),
                ),
                DiceGroupResult(
                    dice = Dice.D8,
                    poolCount = 2,
                    tallies = listOf(
                        ValueTally(value = 7, count = 1),
                        ValueTally(value = 2, count = 1),
                    ),
                ),
            ),
            total = 40,
        ),
        rolledAtMillis = PREVIEW_NOW - 10_000L,
    ),
    RollRecord(
        result = DicePoolResult(
            groups = listOf(
                DiceGroupResult(
                    dice = Dice.D20,
                    poolCount = 1,
                    tallies = listOf(ValueTally(value = 14, count = 1)),
                ),
            ),
            total = 14,
        ),
        rolledAtMillis = PREVIEW_NOW - 2L * 60_000L,
    ),
    RollRecord(
        result = DicePoolResult(
            groups = listOf(
                DiceGroupResult(
                    dice = Dice.D6,
                    poolCount = 2,
                    tallies = listOf(
                        ValueTally(value = 5, count = 1),
                        ValueTally(value = 4, count = 1),
                    ),
                ),
            ),
            total = 9,
        ),
        rolledAtMillis = PREVIEW_NOW - 3L * 60L * 60_000L,
    ),
)

@Preview(name = "Collapsed", showBackground = true, widthDp = 360)
@Composable
private fun RollHistoryBandCollapsedPreview() {
    DiceRollerTheme(dynamicColor = false) {
        RollHistoryBand(
            history = PREVIEW_HISTORY,
            isExpanded = false,
            nowMillis = PREVIEW_NOW,
            selectedColor = DiceColor.Ruby,
            onToggleExpanded = {},
        )
    }
}

@Preview(name = "Expanded", showBackground = true, widthDp = 360, heightDp = 240)
@Composable
private fun RollHistoryBandExpandedPreview() {
    DiceRollerTheme(dynamicColor = false) {
        RollHistoryBand(
            history = PREVIEW_HISTORY,
            isExpanded = true,
            nowMillis = PREVIEW_NOW,
            selectedColor = DiceColor.Ruby,
            onToggleExpanded = {},
        )
    }
}
