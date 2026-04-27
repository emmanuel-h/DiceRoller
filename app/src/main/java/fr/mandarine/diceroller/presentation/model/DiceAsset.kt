// app/src/main/java/fr/mandarine/diceroller/presentation/model/DiceAsset.kt
package fr.mandarine.diceroller.presentation.model

import androidx.annotation.DrawableRes
import fr.mandarine.diceroller.R
import fr.mandarine.diceroller.domain.Dice

/**
 * Maps each [Dice] enum value to its static drawable resource ID.
 *
 * This is the single source of truth for the Dice → drawable asset mapping.
 * All paths must use tintable single-color Vector Drawables so that
 * [DiceImage] can apply runtime tinting from the Material 3 color scheme.
 *
 * Adding a new die type requires updating [Dice] and adding a branch here —
 * the compiler enforces exhaustiveness.
 */
object DiceAsset {

    /**
     * Returns the [DrawableRes] resource ID for the given [dice] type.
     *
     * The `when` expression is exhaustive — the compiler will flag any
     * missing branch if a new [Dice] variant is added.
     */
    @DrawableRes
    fun fromDice(dice: Dice): Int = when (dice) {
        Dice.D4 -> R.drawable.ic_dice_d4
        Dice.D6 -> R.drawable.ic_dice_d6
        Dice.D8 -> R.drawable.ic_dice_d8
        Dice.D12 -> R.drawable.ic_dice_d12
        Dice.D20 -> R.drawable.ic_dice_d20
    }
}
