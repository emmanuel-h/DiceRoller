// app/src/main/java/fr/mandarine/diceroller/MainActivity.kt
package fr.mandarine.diceroller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.mandarine.diceroller.domain.CustomDie
import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.domain.DiceGroupResult
import fr.mandarine.diceroller.domain.DicePool
import fr.mandarine.diceroller.domain.DicePoolResult
import fr.mandarine.diceroller.domain.DieType
import fr.mandarine.diceroller.domain.RollRecord
import fr.mandarine.diceroller.domain.ValueTally
import fr.mandarine.diceroller.presentation.DiceRollerUiState
import fr.mandarine.diceroller.presentation.DiceRollerViewModel
import fr.mandarine.diceroller.presentation.component.AboutIconButton
import fr.mandarine.diceroller.presentation.component.AboutSheet
import fr.mandarine.diceroller.presentation.component.AddDiceChip
import fr.mandarine.diceroller.presentation.component.CustomDieCreatorDialog
import fr.mandarine.diceroller.presentation.component.DiceColorSwatchRow
import fr.mandarine.diceroller.presentation.component.DiceResultDisplay
import fr.mandarine.diceroller.presentation.component.DiceStepperChip
import fr.mandarine.diceroller.presentation.component.RollHistoryBand
import fr.mandarine.diceroller.presentation.model.DiceColor
import fr.mandarine.diceroller.presentation.rollButtonLabel
import fr.mandarine.diceroller.ui.theme.DiceRollerTheme

/** Die-type chips per row in the pool selector, giving each chip an equal share of the width. */
private const val CHIPS_PER_ROW = 3

/** Action label on the snackbar that puts a just-removed custom die back. */
private const val UNDO_REMOVE_LABEL = "Undo"

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
                    onShowCustomDieCreator = viewModel::showCustomDieCreator,
                    onDismissCustomDieCreator = viewModel::dismissCustomDieCreator,
                    onAddCustomDie = viewModel::addCustomDie,
                    onRemoveCustomDie = viewModel::removeCustomDie,
                    onUndoRemoveCustomDie = viewModel::undoRemoveCustomDie,
                    onDismissRemovedCustomDie = viewModel::dismissRemovedCustomDie,
                    onShowAbout = viewModel::showAbout,
                    onDismissAbout = viewModel::dismissAbout,
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
 * selector's chip grid, the results, the roll history, and the Roll button. There is
 * deliberately no top app bar: on a single-screen app its title earned less than the ~64dp it
 * cost, and the swatch row takes that space instead.
 *
 * Only two bands flex ([Modifier.weight]), and they are the two that can genuinely overflow: the
 * results, at pool sizes the design treats as extreme, and the history list once expanded. They
 * split the free space evenly and each scrolls internally, so neither can grow at the other's
 * expense or push anything off screen. The Roll button sits alone in [Scaffold]'s `bottomBar`, so
 * it cannot be pushed off screen either; the artwork attribution that used to sit under it is
 * one tap away in [AboutSheet] since issue #66, and the band it vacated went to the results.
 *
 * The history band's cost is real and was measured, not assumed: at 360×640dp — the shortest
 * viewport the fit tests bound — issue #64's layout cleared its densest *typical* pool by about
 * 16dp, and a collapsed band costs about 45dp. So at that pool on that viewport the result band
 * now scrolls its total into reach instead of showing everything at once; from ~680dp up,
 * everything fits again. This is the vertical price the inline design was chosen with, and it is
 * paid only once there is history to show: before the first roll the band renders nothing at all
 * and the pre-history layout is intact.
 *
 * Custom dice (issue #4) spend that budget once more, and only once: the grid grew from two rows
 * to three to hold the add chip, and it stays at three however many custom dice are defined —
 * see [fr.mandarine.diceroller.presentation.MAX_CUSTOM_DICE]. The extra row is charged whether or
 * not the user ever defines a die, because the add chip is what makes the feature discoverable at
 * all; the result band absorbs it, scrolling a little sooner on the shortest viewports.
 *
 * @param uiState the current UI state
 * @param onIncrementCount callback when a die type's chip is tapped on its right half
 * @param onDecrementCount callback when a die type's chip is tapped on its left half
 * @param onSelectColor callback when a color variant is selected
 * @param onRollDice callback when the roll button is pressed
 * @param onToggleHistory callback when the history band's header is tapped
 * @param onShowCustomDieCreator callback when the grid's add chip is tapped
 * @param onDismissCustomDieCreator callback when the creator dialog is cancelled
 * @param onAddCustomDie callback with the validated die the creator produced
 * @param onRemoveCustomDie callback when a custom chip's `×` badge is tapped
 * @param onUndoRemoveCustomDie callback when the removal snackbar's Undo action is used
 * @param onDismissRemovedCustomDie callback when that snackbar goes away un-actioned
 * @param onShowAbout callback when the info button beside the swatch row is tapped
 * @param onDismissAbout callback when the About sheet is swiped away or its scrim tapped
 * @param modifier optional modifier
 */
@Composable
fun DiceRollerScreen(
    uiState: DiceRollerUiState,
    onIncrementCount: (DieType) -> Unit,
    onDecrementCount: (DieType) -> Unit,
    onSelectColor: (DiceColor) -> Unit,
    onRollDice: () -> Unit,
    onToggleHistory: () -> Unit,
    onShowCustomDieCreator: () -> Unit = {},
    onDismissCustomDieCreator: () -> Unit = {},
    onAddCustomDie: (CustomDie) -> Unit = {},
    onRemoveCustomDie: (CustomDie) -> Unit = {},
    onUndoRemoveCustomDie: () -> Unit = {},
    onDismissRemovedCustomDie: () -> Unit = {},
    onShowAbout: () -> Unit = {},
    onDismissAbout: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // Keyed on the die, so the snackbar is shown once per removal. A second removal of the same
    // die can only follow an undo, which nulls the field and disposes this effect in between.
    uiState.removedCustomDie?.let { removed ->
        LaunchedEffect(removed) {
            val outcome = snackbarHostState.showSnackbar(
                message = "${removed.label} removed",
                actionLabel = UNDO_REMOVE_LABEL,
                duration = SnackbarDuration.Short,
            )
            when (outcome) {
                SnackbarResult.ActionPerformed -> onUndoRemoveCustomDie()
                SnackbarResult.Dismissed -> onDismissRemovedCustomDie()
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
            // Color first, since it recolors every die on screen below it. The About button
            // rides along at the end of that band rather than in one of its own: the swatches'
            // 44dp touch targets already set the band's height, so the credit's entry point is
            // free vertically — which is the whole point of issue #66.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DiceColorSwatchRow(
                    selectedColor = uiState.selectedColor,
                    onSelectColor = onSelectColor,
                    modifier = Modifier.weight(1f),
                )
                AboutIconButton(onClick = onShowAbout)
            }

            DicePoolSelector(
                dieTypes = uiState.dieTypes,
                pool = uiState.pool,
                color = uiState.selectedColor,
                canAddCustomDie = uiState.canAddCustomDie,
                onIncrementCount = onIncrementCount,
                onDecrementCount = onDecrementCount,
                onRemoveCustomDie = onRemoveCustomDie,
                onShowCustomDieCreator = onShowCustomDieCreator,
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

    if (uiState.isCustomDieCreatorVisible) {
        CustomDieCreatorDialog(
            existing = uiState.customDice,
            onAdd = onAddCustomDie,
            onDismiss = onDismissCustomDieCreator,
        )
    }

    if (uiState.isAboutVisible) {
        AboutSheet(onDismiss = onDismissAbout)
    }
}

/**
 * One cell of the pool selector's grid.
 *
 * A sealed type rather than a nullable die: the add chip is a genuinely different kind of cell that
 * happens to share the grid's geometry, and modelling it as "a row slot with no die" would make
 * every `when` in the renderer read as a null check.
 */
private sealed interface PoolCell {

    /** A die's stepper chip. */
    data class Die(val dieType: DieType) : PoolCell

    /** The chip that opens the custom-die creator. */
    data object Add : PoolCell
}

/**
 * The die-type chips as an even grid, [CHIPS_PER_ROW] to a row, with the add chip last.
 *
 * Plain [Row]s with weighted children rather than a `FlowRow`: every chip must be exactly the
 * same width for its two tap halves to be predictable, which weights guarantee and content-driven
 * flow layout does not. Trailing blanks keep the last row's chips at that same width when the cell
 * count is not a multiple of [CHIPS_PER_ROW] — which, with custom dice, is now the common case
 * rather than a hypothetical.
 *
 * Each row is measured at [IntrinsicSize.Min] and its cells stretch to [Modifier.fillMaxHeight],
 * so a row's chips are all as tall as its tallest one. Without that, the add chip and a die chip
 * would each measure to their own content and the row's bottom edges would not line up.
 */
@Composable
private fun DicePoolSelector(
    dieTypes: List<DieType>,
    pool: Map<DieType, Int>,
    color: DiceColor,
    canAddCustomDie: Boolean,
    onIncrementCount: (DieType) -> Unit,
    onDecrementCount: (DieType) -> Unit,
    onRemoveCustomDie: (CustomDie) -> Unit,
    onShowCustomDieCreator: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cells: List<PoolCell> = dieTypes.map { PoolCell.Die(it) } +
        if (canAddCustomDie) listOf(PoolCell.Add) else emptyList()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        cells.chunked(CHIPS_PER_ROW).forEach { rowCells ->
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowCells.forEach { cell ->
                    val cellModifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                    when (cell) {
                        is PoolCell.Die -> DiceStepperChip(
                            dice = cell.dieType,
                            count = pool[cell.dieType] ?: 0,
                            color = color,
                            onIncrement = { onIncrementCount(cell.dieType) },
                            onDecrement = { onDecrementCount(cell.dieType) },
                            modifier = cellModifier,
                            // Only a custom die can be deleted; a preset passing null is what
                            // leaves the six of them without a badge at all.
                            onRemove = (cell.dieType as? CustomDie)?.let { die ->
                                { onRemoveCustomDie(die) }
                            },
                        )

                        PoolCell.Add -> AddDiceChip(
                            onClick = onShowCustomDieCreator,
                            modifier = cellModifier,
                        )
                    }
                }
                repeat(CHIPS_PER_ROW - rowCells.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * The pinned bottom band: the Roll button, alone.
 *
 * It held the CC BY attribution beneath the button until issue #66, where that line was judged to
 * be costing every screen a band for something read once, next to — and competing with — the one
 * primary action. The credit moved to [AboutSheet], one tap away behind the info button at the
 * end of the swatch row; the ~22dp it gave back goes to the weighted result band above.
 */
@Composable
private fun RollBar(
    pool: Map<DieType, Int>,
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
                bottom = 12.dp,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(
                onClick = onRollDice,
                enabled = canRoll,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(rollButtonLabel(DicePool(pool)))
            }
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

private val PREVIEW_POOL: Map<DieType, Int> = Dice.entries.associateWith { dice ->
    when (dice) {
        Dice.D6 -> 4
        Dice.D8 -> 2
        Dice.D20 -> 1
        else -> 0
    }
}

/** The two custom dice the custom-dice previews define, plus the counts they sit at. */
private val PREVIEW_CUSTOM_DICE = listOf(CustomDie(3), CustomDie(100))

private val PREVIEW_CUSTOM_POOL: Map<DieType, Int> =
    PREVIEW_POOL + mapOf(CustomDie(3) to 2, CustomDie(100) to 1)

/** A roll of the mixed pool including both custom dice, to show badges in the result ladder. */
private val PREVIEW_CUSTOM_RESULT = DicePoolResult(
    groups = listOf(
        DiceGroupResult(
            dice = CustomDie(3),
            poolCount = 2,
            tallies = listOf(ValueTally(value = 3, count = 1), ValueTally(value = 1, count = 1)),
        ),
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
            dice = CustomDie(100),
            poolCount = 1,
            tallies = listOf(ValueTally(value = 73, count = 1)),
        ),
    ),
    total = 94,
)

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

/** First launch: no roll yet, so no history band at all — but the add chip is already there. */
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

/** Two custom dice defined: the third grid row filled, badges in the ladder, one add slot left. */
@Preview(name = "Custom dice - D3 and D100", showBackground = true, heightDp = 720)
@Composable
private fun DiceRollerScreenCustomDicePreview() {
    DiceRollerScreenPreviewOf(
        DiceRollerUiState(
            pool = PREVIEW_CUSTOM_POOL,
            customDice = PREVIEW_CUSTOM_DICE,
            selectedColor = DiceColor.Sapphire,
            result = PREVIEW_CUSTOM_RESULT,
            nowMillis = PREVIEW_NOW,
        ),
    )
}

/** The cap reached: three custom dice, so the add chip is gone and the grid is still three rows. */
@Preview(name = "Custom dice - at the cap", showBackground = true, heightDp = 720)
@Composable
private fun DiceRollerScreenCustomDiceFullPreview() {
    DiceRollerScreenPreviewOf(
        DiceRollerUiState(
            pool = PREVIEW_CUSTOM_POOL + mapOf(CustomDie(7) to 0),
            customDice = listOf(CustomDie(3), CustomDie(7), CustomDie(100)),
            selectedColor = DiceColor.Sapphire,
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
