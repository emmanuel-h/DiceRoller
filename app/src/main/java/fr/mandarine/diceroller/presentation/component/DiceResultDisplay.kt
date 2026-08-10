// app/src/main/java/fr/mandarine/diceroller/presentation/component/DiceResultDisplay.kt
package fr.mandarine.diceroller.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.mandarine.diceroller.R
import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.domain.DiceGroupResult
import fr.mandarine.diceroller.domain.DicePoolResult
import fr.mandarine.diceroller.domain.ValueTally
import fr.mandarine.diceroller.presentation.model.DiceColor
import fr.mandarine.diceroller.ui.theme.DiceRollerTheme

/** Opacity applied to the empty-state icon. */
private const val EMPTY_STATE_ALPHA = 0.4f

/** Shown when the pool has no dice queued at all. */
private const val EMPTY_POOL_CAPTION = "Add dice above to build your pool."

/** Shown when the pool has dice queued but the roll button has not been pressed yet. */
private const val NOT_ROLLED_CAPTION = "Tap Roll to see results."

/** Gap between two face entries on the same wrapped line. */
private val ENTRY_HORIZONTAL_SPACING = 14.dp

/** Gap between two wrapped lines of face entries within one die-type group. */
private val ENTRY_VERTICAL_SPACING = 6.dp

/** Gap between the art, the value and the `×N` count inside a single entry. */
private val ENTRY_INTERNAL_SPACING = 4.dp

/** Gap between one die-type group and the next. */
private val GROUP_SPACING = 12.dp

/** Words for 0..[fr.mandarine.diceroller.domain.DicePool.MAX_DICE_PER_TYPE], used by the
 * accessibility summary so it reads naturally instead of as bare digits. */
private val NUMBER_WORDS = listOf(
    "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
    "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen",
    "nineteen", "twenty",
)

/**
 * Displays the outcome of rolling a mixed dice pool as a per-die-type face-ladder turned sideways:
 * one header per die type, then that type's distinct rolled values (highest first) as compact
 * inline `art + value + ×N` entries that wrap, followed by a demoted total-sum line.
 *
 * The sideways arrangement is what keeps the whole screen scroll-free for a realistic pool
 * (issues #63, #64): a die type costs roughly one line instead of one line per rolled value, which
 * is what pays for the artwork growing at the same time.
 *
 * - **Empty state** (`result == null`): a dimmed die icon with a caption that depends on
 *   [isPoolEmpty] — inviting the user to build a pool, or to press Roll.
 * - **Populated state**: [DiceGroupResult.tallies] and [DicePoolResult.groups] are consumed
 *   read-only, already ordered by the domain layer (groups smallest-to-largest die, entries
 *   descending by value) — this composable never re-sorts them. Entries therefore run in reading
 *   order: left to right along a line, then down.
 *
 * At the extreme (20 dice of one type, so up to 20 distinct values) the content scrolls
 * internally; at realistic sizes it does not scroll at all.
 *
 * @param result the tallied outcome of the last roll, or null if no roll has happened yet or the
 *   pool changed since
 * @param isPoolEmpty whether every die type in the pool currently has a count of zero; only
 *   consulted when [result] is null, to choose the empty-state caption
 * @param selectedColor the color variant applied to every die rendered, across every group
 * @param modifier optional [Modifier] applied to the root container
 */
@Composable
fun DiceResultDisplay(
    result: DicePoolResult?,
    isPoolEmpty: Boolean,
    selectedColor: DiceColor,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        // Results read as a continuation of the selector above them, so they start at the top of
        // the band; the empty state is a placeholder for the whole band and centres in it.
        contentAlignment = if (result == null) Alignment.Center else Alignment.TopStart,
    ) {
        if (result == null) {
            EmptyResultState(isPoolEmpty = isPoolEmpty)
        } else {
            PopulatedResultState(result = result, selectedColor = selectedColor)
        }
    }
}

@Composable
private fun EmptyResultState(isPoolEmpty: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_casino),
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = EMPTY_STATE_ALPHA),
        )
        Text(
            text = if (isPoolEmpty) EMPTY_POOL_CAPTION else NOT_ROLLED_CAPTION,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * The groups-and-total content, scrollable only when it genuinely overflows.
 *
 * A plain scrollable [Column] rather than a `LazyColumn`: the content is bounded and small (at
 * most six groups of at most twenty entries), and laziness would demand a bounded-height parent,
 * which is exactly the constraint that made this component awkward to place before.
 */
@Composable
private fun PopulatedResultState(
    result: DicePoolResult,
    selectedColor: DiceColor,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(GROUP_SPACING),
    ) {
        // Invisible node carrying one generated summary, so a roll is announced once as a whole
        // rather than entry by entry.
        Box(
            modifier = Modifier
                .size(0.dp)
                .clearAndSetSemantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = result.toAccessibilitySummary()
                },
        )
        result.groups.forEach { group ->
            DiceGroupBlock(group = group, color = selectedColor)
        }
        TotalLine(total = result.total)
    }
}

/** One die type's header plus its wrapping run of face entries. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiceGroupBlock(
    group: DiceGroupResult,
    color: DiceColor,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ENTRY_VERTICAL_SPACING),
    ) {
        GroupHeader(group = group)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ENTRY_HORIZONTAL_SPACING),
            verticalArrangement = Arrangement.spacedBy(ENTRY_VERTICAL_SPACING),
        ) {
            group.tallies.forEach { tally ->
                FaceEntry(dice = group.dice, tally = tally, color = color)
            }
        }
    }
}

@Composable
private fun GroupHeader(group: DiceGroupResult, modifier: Modifier = Modifier) {
    Text(
        text = "${group.poolCount}×D${group.dice.faces}",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "${group.poolCount} ${group.dice.name} dice" },
    )
}

/**
 * One rolled value as a compact inline entry: die art, the value, then its `×N` multiplier.
 *
 * The artwork is the largest element and the wording the smallest, inverting the previous row
 * layout where a `headlineSmall` numeral dominated a 56dp-tall row (issue #63).
 */
@Composable
private fun FaceEntry(
    dice: Dice,
    tally: ValueTally,
    color: DiceColor,
    modifier: Modifier = Modifier,
) {
    val timesWord = if (tally.count == 1) "time" else "times"
    Row(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "Value ${tally.value}, rolled ${tally.count} $timesWord"
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ENTRY_INTERNAL_SPACING),
    ) {
        DiceImage(
            dice = dice,
            color = color,
            sizeVariant = DiceImageSize.Inline,
            contentDescription = null,
            modifier = Modifier.testTag("dice-row-art-${dice.name}-${tally.value}-${color.name}"),
        )
        Text(
            text = tally.value.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "×${tally.count}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TotalLine(total: Int, modifier: Modifier = Modifier) {
    Text(
        text = "Total $total",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.End,
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Total $total" },
    )
}

/**
 * Builds the hidden live-region summary announced after each roll, e.g.
 * `"Rolled 4 D6 and 2 D8. D6: one 6, two 4s, one 3. D8: one 7, one 2. Total 26."` — following the
 * same smallest-to-largest group order and descending-by-value entry order as the visible ladder.
 */
private fun DicePoolResult.toAccessibilitySummary(): String {
    val poolSummary = groups.joinToString(separator = " and ") { group ->
        "${group.poolCount} ${group.dice.name}"
    }
    val groupSummaries = groups.joinToString(separator = " ") { group ->
        val valuesSummary = group.tallies.joinToString(separator = ", ") { tally ->
            val plural = if (tally.count > 1) "s" else ""
            "${numberWord(tally.count)} ${tally.value}$plural"
        }
        "${group.dice.name}: $valuesSummary."
    }
    return "Rolled $poolSummary. $groupSummaries Total $total."
}

private fun numberWord(count: Int): String = NUMBER_WORDS.getOrElse(count) { count.toString() }

// -- Previews -----------------------------------------------------------------

@Preview(name = "Empty pool", showBackground = true)
@Composable
private fun DiceResultDisplayEmptyPoolPreview() {
    DiceRollerTheme(dynamicColor = false) {
        DiceResultDisplay(
            result = null,
            isPoolEmpty = true,
            selectedColor = DiceColor.Amethyst,
        )
    }
}

@Preview(name = "Non-empty pool, not rolled yet", showBackground = true)
@Composable
private fun DiceResultDisplayNotRolledPreview() {
    DiceRollerTheme(dynamicColor = false) {
        DiceResultDisplay(
            result = null,
            isPoolEmpty = false,
            selectedColor = DiceColor.Amethyst,
        )
    }
}

@Preview(name = "Populated - 4D6 + 2D8 + 1D20 ruby", showBackground = true, heightDp = 260)
@Composable
private fun DiceResultDisplayPopulatedPreview() {
    DiceRollerTheme(dynamicColor = false) {
        DiceResultDisplay(
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
                    DiceGroupResult(
                        dice = Dice.D20,
                        poolCount = 1,
                        tallies = listOf(ValueTally(value = 14, count = 1)),
                    ),
                ),
                total = 40,
            ),
            isPoolEmpty = false,
            selectedColor = DiceColor.Ruby,
        )
    }
}

@Preview(name = "Extreme - 20×D20 jade", showBackground = true, heightDp = 260)
@Composable
private fun DiceResultDisplayExtremePreview() {
    DiceRollerTheme(dynamicColor = false) {
        DiceResultDisplay(
            result = DicePoolResult(
                groups = listOf(
                    DiceGroupResult(
                        dice = Dice.D20,
                        poolCount = 20,
                        tallies = (20 downTo 6).map { value ->
                            ValueTally(value = value, count = if (value % 4 == 0) 2 else 1)
                        },
                    ),
                ),
                total = 213,
            ),
            isPoolEmpty = false,
            selectedColor = DiceColor.Jade,
        )
    }
}
