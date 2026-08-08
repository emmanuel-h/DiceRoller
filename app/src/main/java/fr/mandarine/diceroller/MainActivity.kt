// app/src/main/java/fr/mandarine/diceroller/MainActivity.kt
package fr.mandarine.diceroller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.presentation.DiceRollerUiState
import fr.mandarine.diceroller.presentation.DiceRollerViewModel
import fr.mandarine.diceroller.presentation.component.DiceColorSwatchRow
import fr.mandarine.diceroller.presentation.component.DiceImage
import fr.mandarine.diceroller.presentation.component.DiceImageSize
import fr.mandarine.diceroller.presentation.component.DiceResultDisplay
import fr.mandarine.diceroller.presentation.model.DiceColor
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
                    onSelectDice = viewModel::selectDice,
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
 * @param uiState the current UI state
 * @param onSelectDice callback when a die type is selected
 * @param onSelectColor callback when a color variant is selected
 * @param onRollDice callback when the roll button is pressed
 * @param modifier optional modifier
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DiceRollerScreen(
    uiState: DiceRollerUiState,
    onSelectDice: (Dice) -> Unit,
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Dice selection section
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Select a die",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Dice.entries.forEach { dice ->
                        DiceSelectorChip(
                            dice = dice,
                            color = uiState.selectedColor,
                            isSelected = uiState.selectedDice == dice,
                            onClick = { onSelectDice(dice) },
                        )
                    }
                }

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
                    text = ART_ATTRIBUTION,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Result display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                DiceResultDisplay(
                    selectedDice = uiState.selectedDice,
                    selectedColor = uiState.selectedColor,
                    result = uiState.result,
                )
            }

            // Roll button
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onRollDice,
            ) {
                Text("Roll D${uiState.selectedDice.faces}")
            }
        }
    }
}

/**
 * A single die in the selector row, rendered as the pack artwork.
 *
 * Selection is conveyed by the surrounding container — a filled background and
 * a `primary` border — rather than by tinting the artwork, which carries its
 * own color.
 */
@Composable
private fun DiceSelectorChip(
    dice: Dice,
    color: DiceColor,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    Color.Transparent
                },
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = shape,
            )
            .selectable(
                selected = isSelected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(6.dp),
    ) {
        DiceImage(
            dice = dice,
            color = color,
            sizeVariant = DiceImageSize.Small,
            contentDescription = "Select ${dice.name}",
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DiceRollerScreenPreview() {
    DiceRollerTheme {
        DiceRollerScreen(
            uiState = DiceRollerUiState(),
            onSelectDice = {},
            onSelectColor = {},
            onRollDice = {},
        )
    }
}

@Preview(name = "Rolled - D20 ruby", showBackground = true)
@Composable
private fun DiceRollerScreenRolledPreview() {
    DiceRollerTheme(dynamicColor = false) {
        DiceRollerScreen(
            uiState = DiceRollerUiState(
                selectedDice = Dice.D20,
                selectedColor = DiceColor.Ruby,
                result = 17,
            ),
            onSelectDice = {},
            onSelectColor = {},
            onRollDice = {},
        )
    }
}
