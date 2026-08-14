// app/src/main/java/fr/mandarine/diceroller/domain/DieType.kt
package fr.mandarine.diceroller.domain

/**
 * Anything that can be rolled: a die identified purely by how many faces it has.
 *
 * Two implementations, and deliberately only two:
 * - [Dice], the six standard presets, which are the only ones with artwork; keeping them an
 *   `enum` is what lets
 *   [fr.mandarine.diceroller.presentation.model.DiceColor.drawableFor] stay a
 *   compiler-checked exhaustive `when`, so a new preset cannot ship without its 12 renders.
 * - [CustomDie], a user-defined face count (issue #4), which has no artwork and is drawn as a
 *   swatch-coloured badge instead.
 *
 * A die *is* its face count: [CustomDie] refuses to be constructed with a face count a [Dice]
 * already covers, so no two [DieType] values ever share a [faces] — which is what makes a
 * `Map<DieType, Int>` pool and the face-count-keyed
 * [fr.mandarine.diceroller.data.RollHistoryCodec] format unambiguous.
 *
 * @property faces the number of faces on the die; always within [FACES_RANGE]
 */
sealed interface DieType {

    val faces: Int

    /**
     * This die in standard dice notation, e.g. `"D6"` or `"D7"` — one die's worth of the
     * notation [fr.mandarine.diceroller.presentation.poolNotation] builds pools out of.
     *
     * For a [Dice] entry this is identical to its `name`, which is why it can replace `name` in
     * accessibility labels and test tags without changing a single string.
     */
    val label: String get() = "D$faces"

    companion object {

        /**
         * Face counts a die may have at all.
         *
         * The floor of 2 is what a die means — one face is not a random outcome. The ceiling is
         * a sanity bound rather than a rule of any game: it keeps a mistyped `999999` from
         * producing a pool whose result ladder is longer than the log it would be recorded in.
         */
        val FACES_RANGE: IntRange = 2..1000

        /**
         * The die with [faces] faces: the [Dice] preset if one has that face count, otherwise a
         * [CustomDie] — or null when [faces] is outside [FACES_RANGE].
         *
         * The single place that resolves a bare face count to a die, so "6 means the standard D6,
         * never a custom die that looks like one" is decided once. Both the custom-die creator
         * and [fr.mandarine.diceroller.data.RollHistoryCodec]'s decoder go through it, which is
         * why a history written before a custom die was deleted still reads back correctly.
         */
        fun ofFaces(faces: Int): DieType? =
            Dice.entries.firstOrNull { it.faces == faces }
                ?: if (faces in FACES_RANGE) CustomDie(faces) else null
    }
}
