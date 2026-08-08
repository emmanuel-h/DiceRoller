// app/src/main/java/fr/mandarine/diceroller/presentation/component/DiceResultDisplay.kt
package fr.mandarine.diceroller.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
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

/** Fixed width of the trailing "×N" count column, so it aligns down a group. */
private val COUNT_COLUMN_WIDTH = 40.dp

/** Words for 0..[fr.mandarine.diceroller.domain.DicePool.MAX_DICE_PER_TYPE], used by the
 * accessibility summary so it reads naturally instead of as bare digits. */
private val NUMBER_WORDS = listOf(
    "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
    "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen",
    "nineteen", "twenty",
)

/**
 * Displays the outcome of rolling a mixed dice pool as a per-die-type face-ladder: one header
 * and one row per distinct rolled value (highest first), followed by a demoted total-sum line.
 *
 * Replaces the single die-plus-number layout with a scrollable list because a pool can contain
 * up to six die types and up to [fr.mandarine.diceroller.domain.DicePool.MAX_DICE_PER_TYPE] dice
 * each.
 *
 * - **Empty state** (`result == null`): a dimmed die icon with a caption that depends on
 *   [isPoolEmpty] — inviting the user to build a pool, or to press Roll.
 * - **Populated state**: [DiceGroupResult.tallies] and [DicePoolResult.groups] are consumed
 *   read-only, already ordered by the domain layer (groups smallest-to-largest die, rows
 *   descending by value) — this composable never re-sorts them.
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
        contentAlignment = Alignment.Center,
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

@Composable
private fun PopulatedResultState(
    result: DicePoolResult,
    selectedColor: DiceColor,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Invisible node carrying one generated summary, since per-row liveRegion is unreliable
        // inside a LazyColumn.
        Box(
            modifier = Modifier
                .size(0.dp)
                .clearAndSetSemantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = result.toAccessibilitySummary()
                },
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            result.groups.forEach { group ->
                item(key = "header-${group.dice.name}") {
                    GroupHeader(group = group)
                }
                itemsIndexed(
                    items = group.tallies,
                    key = { _, tally -> "row-${group.dice.name}-${tally.value}" },
                ) { index, tally ->
                    ResultRow(
                        dice = group.dice,
                        tally = tally,
                        color = selectedColor,
                        modifier = Modifier.padding(top = if (index == 0) 8.dp else 4.dp),
                    )
                }
                item(key = "spacer-${group.dice.name}") {
                    Spacer20()
                }
            }
            item(key = "divider") {
                HorizontalDivider(modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
            }
            item(key = "total") {
                TotalLine(total = result.total)
            }
        }
    }
}

@Composable
private fun Spacer20(modifier: Modifier = Modifier) {
    Box(modifier = modifier.height(20.dp))
}

@Composable
private fun GroupHeader(group: DiceGroupResult, modifier: Modifier = Modifier) {
    Text(
        text = "${group.poolCount}×D${group.dice.faces}",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "${group.poolCount} ${group.dice.name} dice" },
    )
}

/**
 * One rolled value's row: die art (decorative, [color]) — value — fixed-width count, so `×`
 * glyphs line up down the group whether the count is one or two digits.
 */
@Composable
private fun ResultRow(
    dice: Dice,
    tally: ValueTally,
    color: DiceColor,
    modifier: Modifier = Modifier,
) {
    val timesWord = if (tally.count == 1) "time" else "times"
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "Value ${tally.value}, rolled ${tally.count} $timesWord"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DiceImage(
            dice = dice,
            color = color,
            sizeVariant = DiceImageSize.Small,
            contentDescription = null,
            modifier = Modifier.testTag("dice-row-art-${dice.name}-${tally.value}-${color.name}"),
        )
        Text(
            text = tally.value.toString(),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "×${tally.count}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.width(COUNT_COLUMN_WIDTH),
        )
    }
}

@Composable
private fun TotalLine(total: Int, modifier: Modifier = Modifier) {
    Text(
        text = "Total $total",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Total $total" },
    )
}

/**
 * Builds the hidden live-region summary announced after each roll, e.g.
 * `"Rolled 4 D6 and 2 D8. D6: one 6, two 4s, one 3. D8: one 7, one 2. Total 26."` — following the
 * same smallest-to-largest group order and descending-by-value row order as the visible ladder.
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

@Preview(name = "Populated - 4D6 + 2D8 ruby", showBackground = true, heightDp = 500)
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
                ),
                total = 26,
            ),
            isPoolEmpty = false,
            selectedColor = DiceColor.Ruby,
        )
    }
}
