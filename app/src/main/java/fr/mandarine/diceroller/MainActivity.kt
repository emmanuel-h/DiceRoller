// app/src/main/java/fr/mandarine/diceroller/MainActivity.kt
package fr.mandarine.diceroller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.presentation.DiceRollerUiState
import fr.mandarine.diceroller.presentation.DiceRollerViewModel
import fr.mandarine.diceroller.ui.theme.DiceRollerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DiceRollerTheme {
                val viewModel: DiceRollerViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                DiceRollerScreen(
                    uiState = uiState,
                    onSelectDice = viewModel::selectDice,
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
 * @param onRollDice callback when the roll button is pressed
 * @param modifier optional modifier
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiceRollerScreen(
    uiState: DiceRollerUiState,
    onSelectDice: (Dice) -> Unit,
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
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Dice.entries.forEach { dice ->
                        FilterChip(
                            selected = uiState.selectedDice == dice,
                            onClick = { onSelectDice(dice) },
                            label = { Text(dice.name) },
                        )
                    }
                }
            }

            // Result display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                contentAlignment = Alignment.Center,
            ) {
                if (uiState.result != null) {
                    Text(
                        text = uiState.result.toString(),
                        style = MaterialTheme.typography.displayLarge,
                    )
                } else {
                    Text(
                        text = "\u2013",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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

@Preview(showBackground = true)
@Composable
private fun DiceRollerScreenPreview() {
    DiceRollerTheme {
        DiceRollerScreen(
            uiState = DiceRollerUiState(),
            onSelectDice = {},
            onRollDice = {},
        )
    }
}
