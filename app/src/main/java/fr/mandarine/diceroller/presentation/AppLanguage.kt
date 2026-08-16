// app/src/main/java/fr/mandarine/diceroller/presentation/AppLanguage.kt
package fr.mandarine.diceroller.presentation

import androidx.annotation.StringRes
import fr.mandarine.diceroller.R

/**
 * A language the user can put the app into, independently of what the device is set to.
 *
 * The point of the feature: a phone in English can run DiceRoller in French, and the choice sticks
 * across launches. [System] is the way back — it drops the override and lets the app follow the
 * device again, which is where every install starts.
 *
 * @property tag the BCP-47 tag this entry selects, or null for [System], which selects nothing.
 *   Matched against the `values-<tag>/` folders and against `res/xml/locales_config.xml`.
 * @property labelRes what the option is called in the picker. The named languages use their own
 *   endonym — `English`, `Français` — and are `translatable="false"` on purpose: a list of
 *   languages is read by someone who does not yet read the language they are looking at, so
 *   translating "French" into "Französisch" would help exactly the people who do not need the row.
 *   Only [System] is translated, because it names a behaviour rather than a language.
 */
enum class AppLanguage(val tag: String?, @param:StringRes val labelRes: Int) {

    /** Follow the device. The default, and the only entry that clears the override. */
    System(tag = null, labelRes = R.string.language_system),

    English(tag = "en", labelRes = R.string.language_en),

    French(tag = "fr", labelRes = R.string.language_fr);

    companion object {

        /**
         * The entry matching [tag], or [System] when it is null, blank or unrecognised.
         *
         * Falls back rather than throwing because the tag comes from outside: `AppCompatDelegate`
         * hands back whatever was stored, which on an upgrade may name a locale this build no
         * longer ships. Matching is on the language subtag alone, so a stored `fr-FR` — which is
         * what Android 13's own picker writes — still resolves to [French].
         */
        fun ofTag(tag: String?): AppLanguage {
            val language = tag?.substringBefore('-')?.takeIf { it.isNotBlank() } ?: return System
            return entries.firstOrNull { it.tag == language } ?: System
        }
    }
}
