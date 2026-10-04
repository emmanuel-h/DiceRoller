// app/src/main/java/fr/mandarine/diceroller/presentation/AppLanguage.kt
package fr.mandarine.diceroller.presentation

import androidx.annotation.StringRes
import fr.mandarine.diceroller.R

/**
 * A language the user can put the app into, independently of what the device is set to.
 *
 * The point of the feature: a phone in English can run DiceRoller in French, and the choice sticks
 * across launches. There is no "follow the system" entry: an install that has never picked starts
 * in the device's language, or [English] when the app does not ship it — see [forDevice].
 *
 * @property tag the BCP-47 tag this entry selects. Matched against the `values-<tag>/` folders.
 * @property labelRes what the option is called in the picker. Each language uses its own
 *   endonym — `English`, `Français` — and is `translatable="false"` on purpose: a list of
 *   languages is read by someone who does not yet read the language they are looking at, so
 *   translating "French" into "Französisch" would help exactly the people who do not need the row.
 */
enum class AppLanguage(val tag: String, @param:StringRes val labelRes: Int) {

    English(tag = "en", labelRes = R.string.language_en),

    French(tag = "fr", labelRes = R.string.language_fr);

    companion object {

        /**
         * The entry matching [tag], or null when it is null, blank or unrecognised.
         *
         * Returns null rather than throwing because the tag comes from storage, which on an upgrade
         * may name a locale this build no longer ships. Matching is on the language subtag alone,
         * so a stored `fr-FR` still resolves to [French].
         */
        fun ofTag(tag: String?): AppLanguage? {
            val language = tag?.substringBefore('-')?.takeIf { it.isNotBlank() } ?: return null
            return entries.firstOrNull { it.tag == language }
        }

        /**
         * The language a device whose locale is [tag] reads the app in: its own when the app ships
         * it, otherwise [English], the language Android's resource fallback lands on anyway.
         */
        fun forDevice(tag: String?): AppLanguage = ofTag(tag) ?: English
    }
}
