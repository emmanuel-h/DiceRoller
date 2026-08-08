// app/src/main/java/fr/mandarine/diceroller/MainActivity.kt
package fr.mandarine.diceroller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.domain.DiceGroupResult
import fr.mandarine.diceroller.domain.DicePool
import fr.mandarine.diceroller.domain.DicePoolResult
import fr.mandarine.diceroller.domain.ValueTally
import fr.mandarine.diceroller.presentation.DiceRollerUiState
import fr.mandarine.diceroller.presentation.DiceRollerViewModel
import fr.mandarine.diceroller.presentation.component.DiceColorSwatchRow
import fr.mandarine.diceroller.presentation.component.DiceResultDisplay
import fr.mandarine.diceroller.presentation.component.DiceStepperChip
import fr.mandarine.diceroller.presentation.model.DiceColor
import fr.mandarine.diceroller.presentation.rollButtonLabel
import fr.mandarine.diceroller.ui.theme.DiceRollerTheme

/** Credit required by the CC BY 4.0 license covering the dice artwork. */
private const val ART_ATTRIBUTION = "Dice art by Aeynit · CC BY 4.0"

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
                )
            }
        }
    }
}

/**
 * Main dice roller screen composable.
 *
 * The top section (color picker, dice-pool stepper chips, art attribution) lives in a
 * `verticalScroll`-able column so all six always-visible [DiceStepperChip]s stay reachable on
 * compact-height phones. The result face-ladder below it is given the remaining space via
 * [Modifier.weight] on its containing [Box], which — since that `Box` sits in the outer,
 * non-scrolling [Column] rather than inside the scrollable one — gives the ladder's internal
 * `LazyColumn` the bounded height it requires; nesting it directly inside the scrollable column
 * would hand it unbounded height constraints and crash at runtime. The Roll button is promoted to
 * [Scaffold]'s `bottomBar` so it never scrolls out of view.
 *
 * @param uiState the current UI state
 * @param onIncrementCount callback when a die type's stepper `+` control is tapped
 * @param onDecrementCount callback when a die type's stepper `−` control is tapped
 * @param onSelectColor callback when a color variant is selected
 * @param onRollDice callback when the roll button is pressed
 * @param modifier optional modifier
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DiceRollerScreen(
    uiState: DiceRollerUiState,
    onIncrementCount: (Dice) -> Unit,
    onDecrementCount: (Dice) -> Unit,
    onSelectColor: (DiceColor) -> Unit,
    onRollDice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Dice Roller") },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = onRollDice,
                    enabled = uiState.canRoll,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                ) {
                    Text(rollButtonLabel(DicePool(uiState.pool)))
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Selection section: color first, since it recolors the die chips below it.
            // Scrollable on its own so the six always-visible stepper chips stay reachable
            // without shrinking the result region below it.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Color",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                DiceColorSwatchRow(
                    selectedColor = uiState.selectedColor,
                    onSelectColor = onSelectColor,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = "Dice pool",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Dice.entries.forEach { dice ->
                        DiceStepperChip(
                            dice = dice,
                            count = uiState.pool[dice] ?: 0,
                            color = uiState.selectedColor,
                            onIncrement = { onIncrementCount(dice) },
                            onDecrement = { onDecrementCount(dice) },
                        )
                    }
                }

                Text(
                    text = ART_ATTRIBUTION,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Result display — bounded-height parent for DiceResultDisplay's internal
            // LazyColumn; see the class-level KDoc above for why this Box must not be nested
            // inside the scrollable Column above.
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
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DiceRollerScreenPreview() {
    DiceRollerTheme {
        DiceRollerScreen(
            uiState = DiceRollerUiState(),
            onIncrementCount = {},
            onDecrementCount = {},
            onSelectColor = {},
            onRollDice = {},
        )
    }
}

@Preview(name = "Rolled - 4D6 + 2D8 ruby", showBackground = true, heightDp = 800)
@Composable
private fun DiceRollerScreenRolledPreview() {
    DiceRollerTheme(dynamicColor = false) {
        DiceRollerScreen(
            uiState = DiceRollerUiState(
                pool = Dice.entries.associateWith { dice ->
                    when (dice) {
                        Dice.D6 -> 4
                        Dice.D8 -> 2
                        else -> 0
                    }
                },
                selectedColor = DiceColor.Ruby,
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
            ),
            onIncrementCount = {},
            onDecrementCount = {},
            onSelectColor = {},
            onRollDice = {},
        )
    }
}
