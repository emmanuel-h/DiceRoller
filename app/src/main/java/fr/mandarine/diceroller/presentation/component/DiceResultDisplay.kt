// app/src/main/java/fr/mandarine/diceroller/presentation/component/DiceResultDisplay.kt
package fr.mandarine.diceroller.presentation.component

import android.content.res.Resources
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
import fr.mandarine.diceroller.domain.DieType
import fr.mandarine.diceroller.domain.ValueTally
import fr.mandarine.diceroller.presentation.model.DiceColor
import fr.mandarine.diceroller.ui.theme.DiceRollerTheme

/** Opacity applied to the empty-state icon. */
private const val EMPTY_STATE_ALPHA = 0.4f

/** Gap between two face entries on the same wrapped line. */
private val ENTRY_HORIZONTAL_SPACING = 14.dp

/** Gap between two wrapped lines of face entries within one die-type group. */
private val ENTRY_VERTICAL_SPACING = 6.dp

/** Gap between the art, the value and the `×N` count inside a single entry. */
private val ENTRY_INTERNAL_SPACING = 4.dp

/** Gap between one die-type group and the next. */
private val GROUP_SPACING = 12.dp

/** Separator between two die-type groups within the spoken roll summary. */
private const val GROUP_SUMMARY_SEPARATOR = " "

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
            text = stringResource(
                if (isPoolEmpty) {
                    R.string.result_empty_pool_caption
                } else {
                    R.string.result_not_rolled_caption
                },
            ),
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
    val resources = LocalResources.current
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
                    contentDescription = result.toAccessibilitySummary(resources)
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
    val label = dieLabel(group.dice)
    val description = pluralStringResource(
        R.plurals.result_group_description,
        group.poolCount,
        group.poolCount,
        label,
    )
    Text(
        text = stringResource(R.string.result_group_header, group.poolCount, label),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = description },
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
    dice: DieType,
    tally: ValueTally,
    color: DiceColor,
    modifier: Modifier = Modifier,
) {
    val description = pluralStringResource(
        R.plurals.result_face_description,
        tally.count,
        tally.value,
        tally.count,
    )
    Row(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = description
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ENTRY_INTERNAL_SPACING),
    ) {
        DiceImage(
            dice = dice,
            color = color,
            sizeVariant = DiceImageSize.Inline,
            contentDescription = null,
            modifier = Modifier.testTag("dice-row-art-${dice.label}-${tally.value}-${color.name}"),
        )
        Text(
            text = stringResource(R.string.number, tally.value),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.multiplier, tally.count),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TotalLine(total: Int, modifier: Modifier = Modifier) {
    val text = stringResource(R.string.result_total, total)
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.End,
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = text },
    )
}

/**
 * Builds the hidden live-region summary announced after each roll, e.g.
 * `"Rolled 4 D6 and 2 D8. D6: one 6, 2 4s, one 3. D8: one 7, one 2. Total 26."` — following the
 * same smallest-to-largest group order and descending-by-value entry order as the visible ladder.
 *
 * Assembled from [Resources] rather than from `stringResource`, because it is one sentence built
 * out of a variable number of clauses: a composable could not loop over the groups without the
 * resource lookups themselves becoming conditional. The caller reads the resources once and hands
 * them down.
 *
 * Every clause is a resource, including the `" and "` joining the pool entries and the `s` that
 * used to be glued onto a repeated value — issue #68's rule is that no user-facing wording is
 * assembled in Kotlin. The English number words this used to spell out ("two 4s") went with it:
 * they were 21 hard-coded English strings, and a screen reader says "two" for `2` anyway.
 */
private fun DicePoolResult.toAccessibilitySummary(resources: Resources): String {
    val separator = resources.getString(R.string.a11y_pool_separator)
    val poolSummary = groups.joinToString(separator = separator) { group ->
        resources.getString(R.string.a11y_pool_entry, group.poolCount, group.dice.label)
    }
    val groupSummaries = groups.joinToString(separator = GROUP_SUMMARY_SEPARATOR) { group ->
        val valuesSummary = group.tallies.joinToString(separator = ", ") { tally ->
            resources.getQuantityString(
                R.plurals.a11y_value_tally,
                tally.count,
                tally.count,
                tally.value,
            )
        }
        resources.getString(R.string.a11y_group_summary, group.dice.label, valuesSummary)
    }
    return resources.getString(R.string.a11y_roll_summary, poolSummary, groupSummaries, total)
}

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
