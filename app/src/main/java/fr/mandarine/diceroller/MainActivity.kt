// app/src/main/java/fr/mandarine/diceroller/MainActivity.kt
package fr.mandarine.diceroller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.domain.DiceGroupResult
import fr.mandarine.diceroller.domain.DicePool
import fr.mandarine.diceroller.domain.DicePoolResult
import fr.mandarine.diceroller.domain.RollRecord
import fr.mandarine.diceroller.domain.ValueTally
import fr.mandarine.diceroller.presentation.DiceRollerUiState
import fr.mandarine.diceroller.presentation.DiceRollerViewModel
import fr.mandarine.diceroller.presentation.component.DiceColorSwatchRow
import fr.mandarine.diceroller.presentation.component.DiceResultDisplay
import fr.mandarine.diceroller.presentation.component.DiceStepperChip
import fr.mandarine.diceroller.presentation.component.RollHistoryBand
import fr.mandarine.diceroller.presentation.model.DiceColor
import fr.mandarine.diceroller.presentation.rollButtonLabel
import fr.mandarine.diceroller.ui.theme.DiceRollerTheme

/** Credit required by the CC BY 4.0 license covering the dice artwork. */
private const val ART_ATTRIBUTION = "Dice art by Aeynit · CC BY 4.0"

/** Die-type chips per row in the pool selector, giving each chip an equal share of the width. */
private const val CHIPS_PER_ROW = 3

/** Horizontal inset shared by every band of the screen, so they align down a common edge. */
private val SCREEN_HORIZONTAL_PADDING = 16.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DiceRollerTheme {
                val viewModel: DiceRollerViewModel = viewModel(
                    factory = DiceRollerViewModel.factory(this),
                )
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                DiceRollerScreen(
                    uiState = uiState,
                    onIncrementCount = viewModel::incrementCount,
                    onDecrementCount = viewModel::decrementCount,
                    onSelectColor = viewModel::selectColor,
                    onRollDice = viewModel::rollDice,
                    onToggleHistory = viewModel::toggleHistoryExpanded,
                )
            }
        }
    }
}

/**
 * Main dice roller screen composable.
 *
 * Laid out as fixed bands so a realistic pool — three die types of a few dice each — fits on a
 * phone in portrait without scrolling anywhere (issue #64): the color swatches, the pool
 * selector's 3×2 chip grid, the results, the roll history, and the Roll button. There is
 * deliberately no top app bar: on a single-screen app its title earned less than the ~64dp it
 * cost, and the swatch row takes that space instead.
 *
 * Only two bands flex ([Modifier.weight]), and they are the two that can genuinely overflow: the
 * results, at pool sizes the design treats as extreme, and the history list once expanded. They
 * split the free space evenly and each scrolls internally, so neither can grow at the other's
 * expense or push anything off screen. The Roll button and the artwork attribution sit in
 * [Scaffold]'s `bottomBar`, so neither can be pushed off screen either.
 *
 * The history band's cost is real and was measured, not assumed: at 360×640dp — the shortest
 * viewport the fit tests bound — issue #64's layout cleared its densest *typical* pool by about
 * 16dp, and a collapsed band costs about 45dp. So at that pool on that viewport the result band
 * now scrolls its total into reach instead of showing everything at once; from ~680dp up,
 * everything fits again. This is the vertical price the inline design was chosen with, and it is
 * paid only once there is history to show: before the first roll the band renders nothing at all
 * and the pre-history layout is intact.
 *
 * @param uiState the current UI state
 * @param onIncrementCount callback when a die type's chip is tapped on its right half
 * @param onDecrementCount callback when a die type's chip is tapped on its left half
 * @param onSelectColor callback when a color variant is selected
 * @param onRollDice callback when the roll button is pressed
 * @param onToggleHistory callback when the history band's header is tapped
 * @param modifier optional modifier
 */
@Composable
fun DiceRollerScreen(
    uiState: DiceRollerUiState,
    onIncrementCount: (Dice) -> Unit,
    onDecrementCount: (Dice) -> Unit,
    onSelectColor: (DiceColor) -> Unit,
    onRollDice: () -> Unit,
    onToggleHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        bottomBar = {
            RollBar(
                pool = uiState.pool,
                canRoll = uiState.canRoll,
                onRollDice = onRollDice,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = SCREEN_HORIZONTAL_PADDING),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Color first, since it recolors every die on screen below it.
            DiceColorSwatchRow(
                selectedColor = uiState.selectedColor,
                onSelectColor = onSelectColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )

            DicePoolSelector(
                pool = uiState.pool,
                color = uiState.selectedColor,
                onIncrementCount = onIncrementCount,
                onDecrementCount = onDecrementCount,
            )

            // Results and history share one flexible band and one gap between them, rather than
            // being two children of the outer Column: the roll log is a continuation of the
            // result, and at the shortest supported height every 8dp it does not take is 8dp
            // the result keeps.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    DiceResultDisplay(
                        result = uiState.result,
                        isPoolEmpty = !uiState.canRoll,
                        selectedColor = uiState.selectedColor,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                RollHistoryBand(
                    history = uiState.history,
                    isExpanded = uiState.isHistoryExpanded,
                    nowMillis = uiState.nowMillis,
                    selectedColor = uiState.selectedColor,
                    onToggleExpanded = onToggleHistory,
                    // Weighted only when open, so the band takes half the flexible space to
                    // scroll its entries in; collapsed it wraps to its header and gives the
                    // rest back to the result.
                    modifier = if (uiState.isHistoryExpanded) Modifier.weight(1f) else Modifier,
                )
            }
        }
    }
}

/**
 * The six die-type chips as an even grid, [CHIPS_PER_ROW] to a row.
 *
 * Plain [Row]s with weighted children rather than a `FlowRow`: every chip must be exactly the
 * same width for its two tap halves to be predictable, which weights guarantee and content-driven
 * flow layout does not. Trailing blanks keep the last row's chips at that same width if the die
 * count ever stops being a multiple of [CHIPS_PER_ROW].
 */
@Composable
private fun DicePoolSelector(
    pool: Map<Dice, Int>,
    color: DiceColor,
    onIncrementCount: (Dice) -> Unit,
    onDecrementCount: (Dice) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Dice.entries.chunked(CHIPS_PER_ROW).forEach { rowDice ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowDice.forEach { dice ->
                    DiceStepperChip(
                        dice = dice,
                        count = pool[dice] ?: 0,
                        color = color,
                        onIncrement = { onIncrementCount(dice) },
                        onDecrement = { onDecrementCount(dice) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(CHIPS_PER_ROW - rowDice.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * The pinned bottom band: the Roll button, with the CC BY attribution tucked beneath it.
 *
 * The attribution has to stay on screen for the license, but not at the cost of the layout, so it
 * sits below the button in the smallest style the theme offers.
 */
@Composable
private fun RollBar(
    pool: Map<Dice, Int>,
    canRoll: Boolean,
    onRollDice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier, tonalElevation = 3.dp) {
        Column(
            modifier = Modifier.padding(
                start = SCREEN_HORIZONTAL_PADDING,
                end = SCREEN_HORIZONTAL_PADDING,
                top = 12.dp,
                bottom = 8.dp,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Button(
                onClick = onRollDice,
                enabled = canRoll,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(rollButtonLabel(DicePool(pool)))
            }
            Text(
                text = ART_ATTRIBUTION,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** The pool, result and history a "4D6 + 2D8 + 1D20" roll leaves behind, shared by the previews. */
private val PREVIEW_RESULT = DicePoolResult(
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
)

private const val PREVIEW_NOW = 1_700_000_000_000L

private val PREVIEW_HISTORY = listOf(
    RollRecord(result = PREVIEW_RESULT, rolledAtMillis = PREVIEW_NOW - 10_000L),
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
        rolledAtMillis = PREVIEW_NOW - 120_000L,
    ),
)

private val PREVIEW_POOL = Dice.entries.associateWith { dice ->
    when (dice) {
        Dice.D6 -> 4
        Dice.D8 -> 2
        Dice.D20 -> 1
        else -> 0
    }
}

@Composable
private fun DiceRollerScreenPreviewOf(uiState: DiceRollerUiState) {
    DiceRollerTheme(dynamicColor = false) {
        DiceRollerScreen(
            uiState = uiState,
            onIncrementCount = {},
            onDecrementCount = {},
            onSelectColor = {},
            onRollDice = {},
            onToggleHistory = {},
        )
    }
}

/** First launch: no roll yet, so no history band at all. */
@Preview(showBackground = true, heightDp = 720)
@Composable
private fun DiceRollerScreenPreview() {
    DiceRollerTheme {
        DiceRollerScreen(
            uiState = DiceRollerUiState(),
            onIncrementCount = {},
            onDecrementCount = {},
            onSelectColor = {},
            onRollDice = {},
            onToggleHistory = {},
        )
    }
}

@Preview(name = "Rolled - 4D6 + 2D8 + 1D20 ruby", showBackground = true, heightDp = 720)
@Composable
private fun DiceRollerScreenRolledPreview() {
    DiceRollerScreenPreviewOf(
        DiceRollerUiState(
            pool = PREVIEW_POOL,
            selectedColor = DiceColor.Ruby,
            result = PREVIEW_RESULT,
            history = PREVIEW_HISTORY,
            nowMillis = PREVIEW_NOW,
        ),
    )
}

/** The band open: results and history splitting the free space, each scrolling its own content. */
@Preview(name = "History expanded", showBackground = true, heightDp = 720)
@Composable
private fun DiceRollerScreenHistoryExpandedPreview() {
    DiceRollerScreenPreviewOf(
        DiceRollerUiState(
            pool = PREVIEW_POOL,
            selectedColor = DiceColor.Ruby,
            result = PREVIEW_RESULT,
            history = PREVIEW_HISTORY,
            isHistoryExpanded = true,
            nowMillis = PREVIEW_NOW,
        ),
    )
}

/** Compact-height check: the same realistic pool on a much shorter viewport. */
@Preview(name = "Rolled - compact height", showBackground = true, widthDp = 360, heightDp = 560)
@Composable
private fun DiceRollerScreenCompactPreview() {
    DiceRollerScreenRolledPreview()
}

/** The worst case for height: a short viewport with the history band open on top of it. */
@Preview(name = "History expanded - compact", showBackground = true, widthDp = 360, heightDp = 560)
@Composable
private fun DiceRollerScreenHistoryExpandedCompactPreview() {
    DiceRollerScreenHistoryExpandedPreview()
}
