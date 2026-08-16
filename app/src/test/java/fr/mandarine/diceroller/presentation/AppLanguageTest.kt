// app/src/test/java/fr/mandarine/diceroller/presentation/AppLanguageTest.kt
package fr.mandarine.diceroller.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Resolving a stored language tag back to an option.
 *
 * The tag comes from `AppCompatDelegate`, i.e. from outside this build: it may have been written
 * by an older version, or by Android 13's own per-app language screen, which stores a full
 * `fr-FR` where this picker would have stored `fr`. Every one of those has to land somewhere
 * sensible rather than throw.
 */
class AppLanguageTest {

    // --- The set on offer ---

    @Test
    fun givenTheOptions_whenListed_thenSystemComesFirst() {
        assertEquals(AppLanguage.System, AppLanguage.entries.first())
    }

    @Test
    fun givenTheOptions_whenListed_thenOnlySystemHasNoTag() {
        val untagged = AppLanguage.entries.filter { it.tag == null }

        assertEquals(listOf(AppLanguage.System), untagged)
    }

    @Test
    fun givenTheOptions_whenListed_thenEveryTagAndLabelIsDistinct() {
        val tags = AppLanguage.entries.mapNotNull { it.tag }
        val labels = AppLanguage.entries.map { it.labelRes }

        assertEquals("Tags must be unique: $tags", tags.size, tags.toSet().size)
        assertEquals("Labels must be unique: $labels", labels.size, labels.toSet().size)
    }

    /** A tag naming a region would not match a `values-<lang>/` folder this app ships. */
    @Test
    fun givenTheOptions_whenListed_thenEveryTagIsABareLanguageSubtag() {
        AppLanguage.entries.mapNotNull { it.tag }.forEach { tag ->
            assertTrue("'$tag' must be a bare language subtag", !tag.contains('-'))
            assertEquals("'$tag' must be lowercase", tag.lowercase(), tag)
        }
    }

    // --- Resolving a stored tag ---

    @Test
    fun givenAKnownTag_whenResolved_thenItsOptionIsReturned() {
        assertEquals(AppLanguage.English, AppLanguage.ofTag("en"))
        assertEquals(AppLanguage.French, AppLanguage.ofTag("fr"))
    }

    /**
     * What Android 13's own language screen stores. It has to resolve to the same option this
     * picker would have written, or the sheet would show no selection after a change made there.
     */
    @Test
    fun givenARegionQualifiedTag_whenResolved_thenTheLanguageStillMatches() {
        assertEquals(AppLanguage.French, AppLanguage.ofTag("fr-FR"))
        assertEquals(AppLanguage.English, AppLanguage.ofTag("en-US"))
    }

    /** No override stored: the app is following the device, which is [AppLanguage.System]. */
    @Test
    fun givenNoTag_whenResolved_thenSystemIsReturned() {
        assertEquals(AppLanguage.System, AppLanguage.ofTag(null))
        assertEquals(AppLanguage.System, AppLanguage.ofTag(""))
        assertEquals(AppLanguage.System, AppLanguage.ofTag("   "))
    }

    /** A locale an older build shipped and this one does not; falling back beats crashing. */
    @Test
    fun givenAnUnshippedTag_whenResolved_thenSystemIsReturned() {
        assertEquals(AppLanguage.System, AppLanguage.ofTag("de"))
        assertEquals(AppLanguage.System, AppLanguage.ofTag("es-419"))
    }

    @Test
    fun givenEveryOptionsOwnTag_whenResolved_thenItRoundTrips() {
        AppLanguage.entries.forEach { language ->
            assertEquals(language, AppLanguage.ofTag(language.tag))
        }
    }

    /** `System` is the absence of a choice, so it must not be reachable by naming a language. */
    @Test
    fun givenANamedLanguage_whenResolved_thenItIsNotSystem() {
        AppLanguage.entries.filter { it.tag != null }.forEach { language ->
            assertNotEquals(AppLanguage.System, AppLanguage.ofTag(language.tag))
        }
    }
}
