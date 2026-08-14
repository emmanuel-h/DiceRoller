// app/src/main/java/fr/mandarine/diceroller/presentation/component/CustomDieCreatorDialog.kt
package fr.mandarine.diceroller.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import fr.mandarine.diceroller.domain.CustomDie
import fr.mandarine.diceroller.domain.DieType
import fr.mandarine.diceroller.presentation.CustomFacesResult
import fr.mandarine.diceroller.presentation.validateCustomFaces
import fr.mandarine.diceroller.ui.theme.DiceRollerTheme

/** Test tag of the faces input field. */
const val CUSTOM_FACES_FIELD_TAG: String = "custom-faces-field"

/** Test tag of the dialog's confirm button. */
const val CUSTOM_DIE_ADD_BUTTON_TAG: String = "custom-die-add-button"

/** Test tag of the inline validation message under the field. */
const val CUSTOM_FACES_MESSAGE_TAG: String = "custom-faces-message"

/** Gap between the faces field and the message beneath it. */
private val MESSAGE_SPACING = 8.dp

/**
 * Digits the field accepts, derived from the range's own ceiling rather than hard-coded, so
 * widening [fr.mandarine.diceroller.domain.DieType.FACES_RANGE] widens the field with it.
 */
private val MAX_FACES_DIGITS: Int = DieType.FACES_RANGE.last.toString().length

/**
 * The custom-die creator: one number field and an Add button (issue #4).
 *
 * A die is nothing but a face count, so this asks for exactly that and nothing else. Removal is
 * *not* here — a custom die is deleted from the `×` badge on its own chip — which is what keeps
 * this dialog a one-field form rather than a management screen.
 *
 * The typed text is local [remember] state rather than ViewModel state: a half-finished number is
 * transient UI, and keeping it here means the ViewModel never has to model "the user has typed 1
 * on the way to 12". Every rule about what is acceptable lives in [validateCustomFaces], so the
 * Add button's enablement and the message under the field are two readings of one verdict and
 * cannot disagree.
 *
 * @param existing the dice already defined, so the validator can reject duplicates and a full set
 * @param onAdd invoked with the validated die when Add is pressed
 * @param onDismiss invoked when the dialog is cancelled or dismissed from outside
 */
@Composable
fun CustomDieCreatorDialog(
    existing: List<CustomDie>,
    onAdd: (CustomDie) -> Unit,
    onDismiss: () -> Unit,
) {
    var facesInput by remember { mutableStateOf("") }
    val verdict = validateCustomFaces(input = facesInput, existing = existing)
    val valid = verdict as? CustomFacesResult.Valid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a custom die") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MESSAGE_SPACING)) {
                OutlinedTextField(
                    value = facesInput,
                    onValueChange = { typed ->
                        // Filtered at the source, so the validator only ever explains real
                        // mistakes and never "that is not a number" for a stray letter the
                        // keyboard should not have offered in the first place.
                        facesInput = typed.filter { it.isDigit() }.take(MAX_FACES_DIGITS)
                    },
                    label = { Text("Faces") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { valid?.let { onAdd(it.die) } },
                    ),
                    isError = verdict is CustomFacesResult.Invalid && !verdict.isIncomplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(CUSTOM_FACES_FIELD_TAG),
                )
                Message(verdict = verdict)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { valid?.let { onAdd(it.die) } },
                enabled = valid != null,
                modifier = Modifier.testTag(CUSTOM_DIE_ADD_BUTTON_TAG),
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

/**
 * The line under the field: the range hint while the input is merely unfinished, the specific
 * complaint once it is actually wrong.
 *
 * Always occupies the same slot, so the dialog does not resize as the user types.
 */
@Composable
private fun Message(verdict: CustomFacesResult, modifier: Modifier = Modifier) {
    val isProblem = verdict is CustomFacesResult.Invalid && !verdict.isIncomplete
    val text = when (verdict) {
        is CustomFacesResult.Valid -> "Adds ${verdict.die.label} to your dice."
        is CustomFacesResult.Invalid -> verdict.message
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = if (isProblem) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = modifier.testTag(CUSTOM_FACES_MESSAGE_TAG),
    )
}

// -- Previews -----------------------------------------------------------------

@Preview(name = "Creator - empty", showBackground = true)
@Composable
private fun CustomDieCreatorDialogPreview() {
    DiceRollerTheme(dynamicColor = false) {
        CustomDieCreatorDialog(existing = emptyList(), onAdd = {}, onDismiss = {})
    }
}

@Preview(name = "Creator - one die defined", showBackground = true)
@Composable
private fun CustomDieCreatorDialogWithExistingPreview() {
    DiceRollerTheme(dynamicColor = false) {
        CustomDieCreatorDialog(
            existing = listOf(CustomDie(7)),
            onAdd = {},
            onDismiss = {},
        )
    }
}
