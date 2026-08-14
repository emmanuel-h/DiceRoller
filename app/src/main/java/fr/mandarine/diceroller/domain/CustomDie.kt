// app/src/main/java/fr/mandarine/diceroller/domain/CustomDie.kt
package fr.mandarine.diceroller.domain

/**
 * A user-defined die with an arbitrary face count (issue #4).
 *
 * Unlike [Dice] there is no artwork for one of these — the Fantasy Dices Pack ships six shapes
 * and nothing else — so the UI draws it as a swatch-coloured badge carrying its face count. That
 * is the only way a custom die differs in behaviour: it rolls, sorts, notates, tallies and
 * persists exactly like a preset, because every one of those paths keys off [faces] alone.
 *
 * Construction is validated rather than clamped, so an out-of-range face count is a bug at the
 * call site and not a silently different die. Callers holding an unvalidated number — the
 * creator dialog, the history decoder — should go through [DieType.ofFaces], which returns null
 * instead of throwing.
 *
 * @property faces the number of faces, within [DieType.FACES_RANGE] and not one of [Dice]'s
 * @throws IllegalArgumentException if [faces] is out of range, or is a face count [Dice] already
 *   covers — a `CustomDie(6)` would be a second, distinct pool key rendering as `"D6"`, so the
 *   pool could hold `"1D6 + 1D6"` and mean two different things by it
 */
data class CustomDie(override val faces: Int) : DieType {

    init {
        require(faces in DieType.FACES_RANGE) {
            "Custom die faces must be within ${DieType.FACES_RANGE}, was $faces"
        }
        require(Dice.entries.none { it.faces == faces }) {
            "$faces faces is the standard ${Dice.entries.first { it.faces == faces }}, " +
                "which already exists as a preset"
        }
    }
}
