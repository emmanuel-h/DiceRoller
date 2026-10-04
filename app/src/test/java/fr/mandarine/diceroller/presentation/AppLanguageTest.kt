// app/src/test/java/fr/mandarine/diceroller/presentation/AppLanguageTest.kt
package fr.mandarine.diceroller.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Resolving a stored language tag back to an option.
 *
 * The tag comes from storage or from the device's locale, i.e. from outside this build: it may
 * have been written by an older version, or carry a region (`fr-FR`) where this picker stores
 * `fr`. Every one of those has to land somewhere sensible rather than throw.
 */
class AppLanguageTest {

    // --- The set on offer ---

    /** The picker is inline, so the order here is the order on screen. */
    @Test
    fun givenTheOptions_whenListed_thenEnglishThenFrench() {
        assertEquals(listOf(AppLanguage.English, AppLanguage.French), AppLanguage.entries)
    }

    @Test
    fun givenTheOptions_whenListed_thenEveryTagAndLabelIsDistinct() {
        val tags = AppLanguage.entries.map { it.tag }
        val labels = AppLanguage.entries.map { it.labelRes }

        assertEquals("Tags must be unique: $tags", tags.size, tags.toSet().size)
        assertEquals("Labels must be unique: $labels", labels.size, labels.toSet().size)
    }

    /** A tag naming a region would not match a `values-<lang>/` folder this app ships. */
    @Test
    fun givenTheOptions_whenListed_thenEveryTagIsABareLanguageSubtag() {
        AppLanguage.entries.map { it.tag }.forEach { tag ->
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

    @Test
    fun givenNoTag_whenResolved_thenNothingIsReturned() {
        assertNull(AppLanguage.ofTag(null))
        assertNull(AppLanguage.ofTag(""))
        assertNull(AppLanguage.ofTag("   "))
    }

    /** A locale an older build shipped and this one does not; falling back beats crashing. */
    @Test
    fun givenAnUnshippedTag_whenResolved_thenNothingIsReturned() {
        assertNull(AppLanguage.ofTag("de"))
        assertNull(AppLanguage.ofTag("es-419"))
    }

    // --- The default for a user who never picked ---

    @Test
    fun givenAShippedDeviceLanguage_whenResolvingTheDefault_thenItIsUsed() {
        assertEquals(AppLanguage.French, AppLanguage.forDevice("fr"))
        assertEquals(AppLanguage.English, AppLanguage.forDevice("en"))
    }

    /** What Android's resource fallback would show anyway, so the picker agrees with the screen. */
    @Test
    fun givenAnUnshippedDeviceLanguage_whenResolvingTheDefault_thenItIsEnglish() {
        assertEquals(AppLanguage.English, AppLanguage.forDevice("de"))
        assertEquals(AppLanguage.English, AppLanguage.forDevice(null))
    }

    @Test
    fun givenEveryOptionsOwnTag_whenResolved_thenItRoundTrips() {
        AppLanguage.entries.forEach { language ->
            assertEquals(language, AppLanguage.ofTag(language.tag))
        }
    }
}
