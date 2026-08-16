// app/src/main/java/fr/mandarine/diceroller/presentation/component/ClearPoolButton.kt
package fr.mandarine.diceroller.presentation.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.mandarine.diceroller.R
import fr.mandarine.diceroller.ui.theme.DiceRollerTheme

/** Test tag of the button that empties the dice pool. */
const val CLEAR_POOL_BUTTON_TAG: String = "clear-pool-button"

/**
 * The control that puts every die type's count back to 0 in one tap (issue #67).
 *
 * Lives at the leading end of the roll bar, immediately before the Roll button, so the action that
 * empties the pool sits with the action that rolls it. That band is free vertically — the Roll
 * button already sets its height — which is what let this be added without spending any of the
 * slack issues #64 and #4 left.
 *
 * Everything about it is chosen to keep it from reading as a second Roll: an outline where Roll is
 * filled, a glyph where Roll is a sentence, and a touch target next to a button that takes all the
 * remaining width. Callers hide it entirely while the pool is empty rather than disabling it, so it
 * never offers an action that would do nothing — and the Roll button goes back to spanning the bar.
 *
 * No confirmation: rebuilding a pool costs a few taps on chips that are already on screen, which is
 * cheaper than a dialog on every use.
 *
 * @param onClick invoked when the button is tapped
 * @param modifier optional [Modifier] applied to the button
 */
@Composable
fun ClearPoolButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedIconButton(
        onClick = onClick,
        modifier = modifier.testTag(CLEAR_POOL_BUTTON_TAG),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_close),
            // The button carries a glyph, so this is its whole label.
            contentDescription = stringResource(R.string.clear_pool_description),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// -- Previews -----------------------------------------------------------------

@Preview(name = "Clear pool button", showBackground = true)
@Composable
private fun ClearPoolButtonPreview() {
    DiceRollerTheme(dynamicColor = false) {
        Surface {
            ClearPoolButton(onClick = {}, modifier = Modifier.padding(8.dp))
        }
    }
}
