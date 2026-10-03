// app/src/androidTest/java/fr/mandarine/diceroller/RollRevealReducedMotionUiTest.kt
package fr.mandarine.diceroller

import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** What the system's "Remove animations" setting hands Compose: an animator duration scale of 0. */
private object AnimationsOff : MotionDurationScale {
    override val scaleFactor: Float = 0f
}

/**
 * With animations turned off system-wide, a roll shows its outcome straight away (issue #1).
 *
 * A class of its own because the scale is part of the rule's effect context, fixed per rule.
 */
@RunWith(AndroidJUnit4::class)
class RollRevealReducedMotionUiTest {

    @get:Rule
    val composeTestRule = createComposeRule(effectContext = AnimationsOff)

    @Test
    fun givenAnimationsOff_whenRolling_thenTheOutcomeIsAnnouncedWithinAFewFrames() {
        composeTestRule.launchPausedWithOneD6()

        composeTestRule.onNodeWithText(rollLabel()).performClick()
        repeat(3) { composeTestRule.mainClock.advanceTimeByFrame() }

        composeTestRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion))
            .assertExists()
    }
}
