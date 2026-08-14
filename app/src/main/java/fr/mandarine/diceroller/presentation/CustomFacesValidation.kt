// app/src/main/java/fr/mandarine/diceroller/presentation/CustomFacesValidation.kt
package fr.mandarine.diceroller.presentation

import fr.mandarine.diceroller.domain.CustomDie
import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.domain.DieType

/** Hint shown under the faces field before the user has typed anything wrong. */
val CUSTOM_FACES_HINT: String =
    "Between ${DieType.FACES_RANGE.first} and ${DieType.FACES_RANGE.last} faces."

/**
 * The verdict on what the user typed into the custom-die creator's faces field.
 *
 * A sealed result rather than a nullable die plus a nullable message, so "valid" and "has an
 * error" cannot both be true, and the dialog's Add button is enabled by a type check instead of
 * by a second, separately-derived boolean.
 */
sealed interface CustomFacesResult {

    /** The input names a die that can be added: [die]. */
    data class Valid(val die: CustomDie) : CustomFacesResult

    /**
     * The input cannot be added, for the reason in [message].
     *
     * @property message user-facing, specific about *which* rule was broken, and shown inline
     *   under the field rather than as a toast
     * @property isIncomplete true while the field is simply not finished yet — empty, or `"1"` on
     *   the way to `"12"`. The dialog keeps such a message out of the way instead of scolding the
     *   user mid-type, but the Add button stays disabled either way.
     */
    data class Invalid(val message: String, val isIncomplete: Boolean = false) : CustomFacesResult
}

/**
 * Validates a typed face count against every rule that could stop it becoming a custom die, and
 * says which one it broke.
 *
 * Kept a pure function next to [poolNotation] and [rollButtonLabel] rather than pushed into
 * [DiceRollerViewModel]: the creator's text field is transient UI state, so the ViewModel never
 * sees a half-typed number, and every rule here is unit-testable without a coroutine or a store.
 *
 * The rules, in the order a user hits them:
 * 1. blank, or not a whole number — nothing to add yet
 * 2. outside [DieType.FACES_RANGE] — see the range's own docs for why it is bounded at all
 * 3. already a preset — `6` means the standard D6, which is on screen already
 * 4. already defined — the user has this die
 * 5. at [MAX_CUSTOM_DICE] — no room, and this is checked last so a duplicate still reports as a
 *    duplicate rather than as "full"
 *
 * @param input the raw field contents
 * @param existing the custom dice already defined
 */
fun validateCustomFaces(input: String, existing: List<CustomDie>): CustomFacesResult {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) {
        return CustomFacesResult.Invalid(CUSTOM_FACES_HINT, isIncomplete = true)
    }
    val faces = trimmed.toIntOrNull()
        ?: return CustomFacesResult.Invalid("Enter a whole number of faces.")

    val die = DieType.ofFaces(faces)
        ?: return CustomFacesResult.Invalid(
            "Faces must be between ${DieType.FACES_RANGE.first} and " +
                "${DieType.FACES_RANGE.last}.",
            // A lone "1" is on its way to "10" or "12" as often as it is a mistake.
            isIncomplete = faces < DieType.FACES_RANGE.first,
        )

    if (die is Dice) {
        return CustomFacesResult.Invalid("${die.label} is already one of the standard dice.")
    }
    if (die in existing) {
        return CustomFacesResult.Invalid("${die.label} is already in your custom dice.")
    }
    if (existing.size >= MAX_CUSTOM_DICE) {
        return CustomFacesResult.Invalid(
            "You can have $MAX_CUSTOM_DICE custom dice. Remove one to add another.",
        )
    }
    // ofFaces returns a CustomDie for every face count that is neither out of range nor a preset,
    // both of which returned above.
    return CustomFacesResult.Valid(die as CustomDie)
}
