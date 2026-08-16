// app/src/main/java/fr/mandarine/diceroller/presentation/model/DiceColor.kt
package fr.mandarine.diceroller.presentation.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import fr.mandarine.diceroller.R
import fr.mandarine.diceroller.domain.Dice

/**
 * The twelve color variants shipped by the Fantasy Dices Pack.
 *
 * Each entry carries a [labelRes] naming it in the user's language, a [swatch] color for the
 * picker row (sampled from the variant's `d20` art), and — via [drawableFor] — the pack drawable
 * for every [Dice] type.
 *
 * The display name is a *resource*, deliberately split from the enum's own [name] by issue #68:
 * `name` is the stable identity — what [fr.mandarine.diceroller.data.DataStoreDiceColorStore]
 * persists, what artwork test tags are keyed by — and translating it in place would have made
 * yesterday's saved colour unreadable the moment the phone changed language. `name` never moves;
 * `labelRes` is free to.
 *
 * @property labelRes lowercase display name, e.g. `"amethyst"` — a colour is always named in the
 *   middle of a sentence ("amethyst dice", "D20, amethyst"), never on its own
 * @property swatch dominant color of the variant, used for the picker dot
 */
enum class DiceColor(@param:StringRes val labelRes: Int, val swatch: Color) {

    Amethyst(labelRes = R.string.color_amethyst, swatch = Color(0xFF875E95)),
    Amber(labelRes = R.string.color_amber, swatch = Color(0xFFE18744)),
    Sapphire(labelRes = R.string.color_sapphire, swatch = Color(0xFF4C86B3)),
    Ruby(labelRes = R.string.color_ruby, swatch = Color(0xFFB14B58)),
    Gold(labelRes = R.string.color_gold, swatch = Color(0xFFA59654)),
    Orchid(labelRes = R.string.color_orchid, swatch = Color(0xFFB34997)),
    Smoke(labelRes = R.string.color_smoke, swatch = Color(0xFF4D4851)),
    Jade(labelRes = R.string.color_jade, swatch = Color(0xFF599B89)),
    Moss(labelRes = R.string.color_moss, swatch = Color(0xFF859A59)),
    Bronze(labelRes = R.string.color_bronze, swatch = Color(0xFFAA7F51)),
    Rose(labelRes = R.string.color_rose, swatch = Color(0xFFA4565D)),
    Indigo(labelRes = R.string.color_indigo, swatch = Color(0xFF5F6095));

    /**
     * Returns the drawable resource for this color variant of the given [dice].
     *
     * This is the single mapping point from `(color, die)` to artwork — the
     * same convention the deleted `DiceShape.fromDice` followed. Both `when`
     * expressions are exhaustive, so adding a [Dice] or a [DiceColor] variant
     * is a compile error until the artwork is wired up here.
     */
    @DrawableRes
    fun drawableFor(dice: Dice): Int = when (this) {
        Amethyst -> when (dice) {
            Dice.D4 -> R.drawable.dice_amethyst_d4
            Dice.D6 -> R.drawable.dice_amethyst_d6
            Dice.D8 -> R.drawable.dice_amethyst_d8
            Dice.D10 -> R.drawable.dice_amethyst_d10
            Dice.D12 -> R.drawable.dice_amethyst_d12
            Dice.D20 -> R.drawable.dice_amethyst_d20
        }

        Amber -> when (dice) {
            Dice.D4 -> R.drawable.dice_amber_d4
            Dice.D6 -> R.drawable.dice_amber_d6
            Dice.D8 -> R.drawable.dice_amber_d8
            Dice.D10 -> R.drawable.dice_amber_d10
            Dice.D12 -> R.drawable.dice_amber_d12
            Dice.D20 -> R.drawable.dice_amber_d20
        }

        Sapphire -> when (dice) {
            Dice.D4 -> R.drawable.dice_sapphire_d4
            Dice.D6 -> R.drawable.dice_sapphire_d6
            Dice.D8 -> R.drawable.dice_sapphire_d8
            Dice.D10 -> R.drawable.dice_sapphire_d10
            Dice.D12 -> R.drawable.dice_sapphire_d12
            Dice.D20 -> R.drawable.dice_sapphire_d20
        }

        Ruby -> when (dice) {
            Dice.D4 -> R.drawable.dice_ruby_d4
            Dice.D6 -> R.drawable.dice_ruby_d6
            Dice.D8 -> R.drawable.dice_ruby_d8
            Dice.D10 -> R.drawable.dice_ruby_d10
            Dice.D12 -> R.drawable.dice_ruby_d12
            Dice.D20 -> R.drawable.dice_ruby_d20
        }

        Gold -> when (dice) {
            Dice.D4 -> R.drawable.dice_gold_d4
            Dice.D6 -> R.drawable.dice_gold_d6
            Dice.D8 -> R.drawable.dice_gold_d8
            Dice.D10 -> R.drawable.dice_gold_d10
            Dice.D12 -> R.drawable.dice_gold_d12
            Dice.D20 -> R.drawable.dice_gold_d20
        }

        Orchid -> when (dice) {
            Dice.D4 -> R.drawable.dice_orchid_d4
            Dice.D6 -> R.drawable.dice_orchid_d6
            Dice.D8 -> R.drawable.dice_orchid_d8
            Dice.D10 -> R.drawable.dice_orchid_d10
            Dice.D12 -> R.drawable.dice_orchid_d12
            Dice.D20 -> R.drawable.dice_orchid_d20
        }

        Smoke -> when (dice) {
            Dice.D4 -> R.drawable.dice_smoke_d4
            Dice.D6 -> R.drawable.dice_smoke_d6
            Dice.D8 -> R.drawable.dice_smoke_d8
            Dice.D10 -> R.drawable.dice_smoke_d10
            Dice.D12 -> R.drawable.dice_smoke_d12
            Dice.D20 -> R.drawable.dice_smoke_d20
        }

        Jade -> when (dice) {
            Dice.D4 -> R.drawable.dice_jade_d4
            Dice.D6 -> R.drawable.dice_jade_d6
            Dice.D8 -> R.drawable.dice_jade_d8
            Dice.D10 -> R.drawable.dice_jade_d10
            Dice.D12 -> R.drawable.dice_jade_d12
            Dice.D20 -> R.drawable.dice_jade_d20
        }

        Moss -> when (dice) {
            Dice.D4 -> R.drawable.dice_moss_d4
            Dice.D6 -> R.drawable.dice_moss_d6
            Dice.D8 -> R.drawable.dice_moss_d8
            Dice.D10 -> R.drawable.dice_moss_d10
            Dice.D12 -> R.drawable.dice_moss_d12
            Dice.D20 -> R.drawable.dice_moss_d20
        }

        Bronze -> when (dice) {
            Dice.D4 -> R.drawable.dice_bronze_d4
            Dice.D6 -> R.drawable.dice_bronze_d6
            Dice.D8 -> R.drawable.dice_bronze_d8
            Dice.D10 -> R.drawable.dice_bronze_d10
            Dice.D12 -> R.drawable.dice_bronze_d12
            Dice.D20 -> R.drawable.dice_bronze_d20
        }

        Rose -> when (dice) {
            Dice.D4 -> R.drawable.dice_rose_d4
            Dice.D6 -> R.drawable.dice_rose_d6
            Dice.D8 -> R.drawable.dice_rose_d8
            Dice.D10 -> R.drawable.dice_rose_d10
            Dice.D12 -> R.drawable.dice_rose_d12
            Dice.D20 -> R.drawable.dice_rose_d20
        }

        Indigo -> when (dice) {
            Dice.D4 -> R.drawable.dice_indigo_d4
            Dice.D6 -> R.drawable.dice_indigo_d6
            Dice.D8 -> R.drawable.dice_indigo_d8
            Dice.D10 -> R.drawable.dice_indigo_d10
            Dice.D12 -> R.drawable.dice_indigo_d12
            Dice.D20 -> R.drawable.dice_indigo_d20
        }
    }

    companion object {
        /** Variant used until the user picks one, and whenever a stored name is unreadable. */
        val Default: DiceColor = Amethyst

        /**
         * Returns the entry whose [name] matches [name], or [Default] when it
         * does not correspond to any known variant (e.g. a value persisted by
         * an older build whose color has since been renamed).
         */
        fun fromNameOrDefault(name: String?): DiceColor =
            entries.firstOrNull { it.name == name } ?: Default
    }
}
