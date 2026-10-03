package fr.mandarine.diceroller.presentation.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RollRevealTest {

    private val progresses = (0..99).map { it / 100f }

    @Test
    fun givenALandedDie_whenReadingItsFace_thenItIsTheRolledValue() {
        assertEquals(7, tumblingFace(faces = 20, finalValue = 7, progress = 1f, seed = 3))
    }

    @Test
    fun givenATumblingDie_whenReadingItsFace_thenItIsAlwaysAFaceTheDieHas() {
        listOf(4, 6, 7, 20, 100).forEach { faces ->
            (0..10).forEach { seed ->
                progresses.forEach { progress ->
                    val face = tumblingFace(faces, finalValue = 1, progress = progress, seed = seed)
                    assertTrue("D$faces showed $face", face in 1..faces)
                }
            }
        }
    }

    @Test
    fun givenATumblingD20_whenReadingItsFaces_thenTheyActuallyChange() {
        val faces = progresses.map { tumblingFace(20, finalValue = 1, progress = it, seed = 0) }

        assertTrue(faces.distinct().size > 3)
    }

    /** Recomposing within one flicker step must not change the face, or it would flicker per frame. */
    @Test
    fun givenTheSameProgressAndSeed_whenReadTwice_thenTheFaceIsTheSame() {
        assertEquals(
            tumblingFace(20, finalValue = 1, progress = 0.3f, seed = 5),
            tumblingFace(20, finalValue = 1, progress = 0.3f, seed = 5),
        )
    }

    @Test
    fun givenALandedDie_whenReadingItsTilt_thenItIsLevel() {
        assertEquals(0f, tumblingTilt(progress = 1f, seed = 2), 0f)
    }

    @Test
    fun givenATumblingDie_whenReadingItsTilt_thenItStaysWithinTheMaximum() {
        progresses.forEach { progress ->
            assertTrue(kotlin.math.abs(tumblingTilt(progress, seed = 1)) <= 15f)
        }
    }

    @Test
    fun givenTheSettledReveal_whenRead_thenItIsLandedAndAtRest() {
        assertEquals(1f, RollReveal.Settled.progress, 0f)
        assertEquals(1f, RollReveal.Settled.landingScale, 0f)
    }
}
