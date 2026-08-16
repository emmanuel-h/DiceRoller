// app/src/main/java/fr/mandarine/diceroller/presentation/component/DieLabel.kt
package fr.mandarine.diceroller.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import fr.mandarine.diceroller.R
import fr.mandarine.diceroller.domain.DieType

/**
 * How a single die is *named on screen*: `"D6"`, `"D7"`, `"D1000"`.
 *
 * Not a translation. `D6` is dice notation, the same in every language, and
 * [R.string.die_label] is marked `translatable="false"` to keep it that way. It goes through a
 * resource only so the face count lands in the string as a formatted argument rather than as a
 * concatenated `Int` — which is what lets the platform shape the digits for the active locale, and
 * lay the label out correctly under RTL.
 *
 * The domain's own [DieType.label] stays the value used wherever the string is *not* being shown
 * to a person: test tags, persisted keys, and the pool notation
 * [fr.mandarine.diceroller.presentation.poolNotation] builds, which is a notation rather than a
 * sentence and is deliberately assembled without touching resources.
 */
@Composable
fun dieLabel(die: DieType): String = stringResource(R.string.die_label, die.faces)
