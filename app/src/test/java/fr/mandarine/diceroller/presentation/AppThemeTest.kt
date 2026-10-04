// app/src/test/java/fr/mandarine/diceroller/presentation/AppThemeTest.kt
package fr.mandarine.diceroller.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Resolving a stored theme name, and the default for a user who never picked. */
class AppThemeTest {

    /** The picker is inline, so the order here is the order on screen. */
    @Test
    fun givenTheOptions_whenListed_thenLightThenDark() {
        assertEquals(listOf(AppTheme.Light, AppTheme.Dark), AppTheme.entries)
    }

    @Test
    fun givenEveryOptionsOwnName_whenResolved_thenItRoundTrips() {
        AppTheme.entries.forEach { theme -> assertEquals(theme, AppTheme.ofName(theme.name)) }
    }

    @Test
    fun givenNoOrAnUnknownName_whenResolved_thenNothingIsReturned() {
        assertNull(AppTheme.ofName(null))
        assertNull(AppTheme.ofName("Sepia"))
    }

    @Test
    fun givenTheDevicesDarkMode_whenResolvingTheDefault_thenTheMatchingThemeIsUsed() {
        assertEquals(AppTheme.Dark, AppTheme.forDevice(isDeviceDark = true))
        assertEquals(AppTheme.Light, AppTheme.forDevice(isDeviceDark = false))
    }
}
