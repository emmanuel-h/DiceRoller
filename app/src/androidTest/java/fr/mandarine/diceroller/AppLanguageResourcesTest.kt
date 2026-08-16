// app/src/androidTest/java/fr/mandarine/diceroller/AppLanguageResourcesTest.kt
package fr.mandarine.diceroller

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.mandarine.diceroller.presentation.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * That every language the picker offers has strings behind it, and that a device language the app
 * does not ship still lands somewhere sensible.
 *
 * [AppLanguage] and the `values-<lang>/` folders are two lists that have to agree, and nothing but
 * this test makes them: adding `AppLanguage.German` without a `values-de/` compiles, ships, and
 * silently shows English to anyone who picks it. That is invisible to every other test, because
 * missing resources fall back rather than fail.
 *
 * Resolution is exercised the way `ProvideAppLanguage` does it — a configuration context per
 * language — so this measures what the screen will actually read.
 */
@RunWith(AndroidJUnit4::class)
class AppLanguageResourcesTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    /** Strings as read by a context pinned to [tag], regardless of the device's own locale. */
    private fun stringsIn(tag: String): (Int) -> String {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocales(LocaleList.forLanguageTags(tag))
        val localized = context.createConfigurationContext(configuration)
        return { id -> localized.getString(id) }
    }

    /**
     * The default resources — `values/`, i.e. English — which is what any unshipped locale falls
     * back to and therefore what "default to English" means concretely.
     */
    private fun defaultStrings(): (Int) -> String = stringsIn("en")

    /**
     * A translated, distinctive sentence. Deliberately not one of the short labels: a language
     * could coincidentally share the word "Contact" with English, but not a whole caption.
     */
    private val probe = R.string.result_empty_pool_caption

    @Test
    fun givenEveryOfferedLanguage_whenResolved_thenItHasItsOwnStrings() {
        val english = defaultStrings()(probe)

        AppLanguage.entries.mapNotNull { it.tag }.filter { it != "en" }.forEach { tag ->
            assertNotEquals(
                "AppLanguage offers '$tag' but res/values-$tag/ has no translation for it — " +
                    "the picker would silently show English",
                english,
                stringsIn(tag)(probe),
            )
        }
    }

    @Test
    fun givenEveryOfferedLanguage_whenResolved_thenNothingComesBackBlank() {
        AppLanguage.entries.mapNotNull { it.tag }.forEach { tag ->
            val strings = stringsIn(tag)
            listOf(probe, R.string.settings_title, R.string.roll_button_empty).forEach { id ->
                assertNotEquals("Blank string for '$tag'", "", strings(id).trim())
            }
        }
    }

    /**
     * The first-launch rule: with no choice stored the app follows the device, and a device set to
     * a language this build does not ship reads English rather than nothing.
     */
    @Test
    fun givenADeviceLanguageTheAppDoesNotShip_whenResolved_thenEnglishIsUsed() {
        val english = defaultStrings()

        listOf("de", "es", "ja", "ar").forEach { tag ->
            val strings = stringsIn(tag)
            assertEquals(
                "An unshipped device language must fall back to English, not to '$tag'",
                english(probe),
                strings(probe),
            )
        }
    }

    /** And a device set to one the app *does* ship reads that one, with no choice stored. */
    @Test
    fun givenADeviceLanguageTheAppShips_whenResolved_thenThatLanguageIsUsed() {
        assertEquals("Ajoutez des dés ci-dessus pour composer votre réserve.", stringsIn("fr")(probe))
        assertEquals("Add dice above to build your pool.", stringsIn("en")(probe))
    }

    /** What Android hands back for a region-qualified device locale, which is the common case. */
    @Test
    fun givenARegionQualifiedDeviceLanguage_whenResolved_thenTheLanguageStillMatches() {
        assertEquals(stringsIn("fr")(probe), stringsIn("fr-CA")(probe))
        assertEquals(stringsIn("en")(probe), stringsIn("en-GB")(probe))
    }
}
