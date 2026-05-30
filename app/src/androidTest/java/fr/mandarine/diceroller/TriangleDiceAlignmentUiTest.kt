// app/src/androidTest/java/fr/mandarine/diceroller/TriangleDiceAlignmentUiTest.kt
package fr.mandarine.diceroller

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.presentation.component.DicePolygon
import fr.mandarine.diceroller.presentation.component.DicePolygonSize
import fr.mandarine.diceroller.ui.theme.DiceRollerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for the triangle dice alignment fix.
 *
 * Verifies that [DicePolygon] renders without layout errors for D4 and D20
 * (triangle polygons with a non-zero [fr.mandarine.diceroller.presentation.model.DiceShape.verticalOffsetFraction])
 * at both [DicePolygonSize.Small] and [DicePolygonSize.Large] size variants,
 * and that non-triangle dice (D6, D8, D12) are equally unaffected.
 *
 * Each test uses a known text label as the `content` slot so that
 * [assertIsDisplayed] can confirm the composable composed and laid out
 * successfully without throwing an exception.
 */
@RunWith(AndroidJUnit4::class)
class TriangleDiceAlignmentUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // -------------------------------------------------------------------------
    // D4 (Triangle) — both size variants
    // -------------------------------------------------------------------------

    @Test
    fun givenD4_whenRenderedAtSmallSize_thenContentLabelIsDisplayed() {
        composeTestRule.setContent {
            DiceRollerTheme(dynamicColor = false) {
                DicePolygon(dice = Dice.D4, sizeVariant = DicePolygonSize.Small) {
                    Text(text = "D4")
                }
            }
        }

        composeTestRule.onNodeWithText("D4").assertIsDisplayed()
    }

    @Test
    fun givenD4_whenRenderedAtLargeSize_thenContentLabelIsDisplayed() {
        composeTestRule.setContent {
            DiceRollerTheme(dynamicColor = false) {
                DicePolygon(dice = Dice.D4, sizeVariant = DicePolygonSize.Large) {
                    Text(text = "D4")
                }
            }
        }

        composeTestRule.onNodeWithText("D4").assertIsDisplayed()
    }

    // -------------------------------------------------------------------------
    // D20 (Icosahedron / Triangle) — both size variants
    // -------------------------------------------------------------------------

    @Test
    fun givenD20_whenRenderedAtSmallSize_thenContentLabelIsDisplayed() {
        composeTestRule.setContent {
            DiceRollerTheme(dynamicColor = false) {
                DicePolygon(dice = Dice.D20, sizeVariant = DicePolygonSize.Small) {
                    Text(text = "D20")
                }
            }
        }

        composeTestRule.onNodeWithText("D20").assertIsDisplayed()
    }

    @Test
    fun givenD20_whenRenderedAtLargeSize_thenContentLabelIsDisplayed() {
        composeTestRule.setContent {
            DiceRollerTheme(dynamicColor = false) {
                DicePolygon(dice = Dice.D20, sizeVariant = DicePolygonSize.Large) {
                    Text(text = "D20")
                }
            }
        }

        composeTestRule.onNodeWithText("D20").assertIsDisplayed()
    }

    // -------------------------------------------------------------------------
    // D4 / D20 — selected state (fills polygon background)
    // -------------------------------------------------------------------------

    @Test
    fun givenD4Selected_whenRenderedAtSmallSize_thenContentLabelIsDisplayed() {
        composeTestRule.setContent {
            DiceRollerTheme(dynamicColor = false) {
                DicePolygon(dice = Dice.D4, sizeVariant = DicePolygonSize.Small, isSelected = true) {
                    Text(text = "D4")
                }
            }
        }

        composeTestRule.onNodeWithText("D4").assertIsDisplayed()
    }

    @Test
    fun givenD20Selected_whenRenderedAtSmallSize_thenContentLabelIsDisplayed() {
        composeTestRule.setContent {
            DiceRollerTheme(dynamicColor = false) {
                DicePolygon(dice = Dice.D20, sizeVariant = DicePolygonSize.Small, isSelected = true) {
                    Text(text = "D20")
                }
            }
        }

        composeTestRule.onNodeWithText("D20").assertIsDisplayed()
    }

    // -------------------------------------------------------------------------
    // D4 / D20 — dark theme
    // -------------------------------------------------------------------------

    @Test
    fun givenD4DarkTheme_whenRenderedAtLargeSize_thenContentLabelIsDisplayed() {
        composeTestRule.setContent {
            DiceRollerTheme(darkTheme = true, dynamicColor = false) {
                DicePolygon(dice = Dice.D4, sizeVariant = DicePolygonSize.Large) {
                    Text(text = "D4")
                }
            }
        }

        composeTestRule.onNodeWithText("D4").assertIsDisplayed()
    }

    @Test
    fun givenD20DarkTheme_whenRenderedAtLargeSize_thenContentLabelIsDisplayed() {
        composeTestRule.setContent {
            DiceRollerTheme(darkTheme = true, dynamicColor = false) {
                DicePolygon(dice = Dice.D20, sizeVariant = DicePolygonSize.Large) {
                    Text(text = "D20")
                }
            }
        }

        composeTestRule.onNodeWithText("D20").assertIsDisplayed()
    }

    // -------------------------------------------------------------------------
    // Non-triangle polygons are unaffected — D6, D8, D12
    // -------------------------------------------------------------------------

    @Test
    fun givenD6_whenRenderedAtSmallSize_thenContentLabelIsDisplayed() {
        composeTestRule.setContent {
            DiceRollerTheme(dynamicColor = false) {
                DicePolygon(dice = Dice.D6, sizeVariant = DicePolygonSize.Small) {
                    Text(text = "D6")
                }
            }
        }

        composeTestRule.onNodeWithText("D6").assertIsDisplayed()
    }

    @Test
    fun givenD6_whenRenderedAtLargeSize_thenContentLabelIsDisplayed() {
        composeTestRule.setContent {
            DiceRollerTheme(dynamicColor = false) {
                DicePolygon(dice = Dice.D6, sizeVariant = DicePolygonSize.Large) {
                    Text(text = "D6")
                }
            }
        }

        composeTestRule.onNodeWithText("D6").assertIsDisplayed()
    }

    @Test
    fun givenD8_whenRenderedAtSmallSize_thenContentLabelIsDisplayed() {
        composeTestRule.setContent {
            DiceRollerTheme(dynamicColor = false) {
                DicePolygon(dice = Dice.D8, sizeVariant = DicePolygonSize.Small) {
                    Text(text = "D8")
                }
            }
        }

        composeTestRule.onNodeWithText("D8").assertIsDisplayed()
    }

    @Test
    fun givenD8_whenRenderedAtLargeSize_thenContentLabelIsDisplayed() {
        composeTestRule.setContent {
            DiceRollerTheme(dynamicColor = false) {
                DicePolygon(dice = Dice.D8, sizeVariant = DicePolygonSize.Large) {
                    Text(text = "D8")
                }
            }
        }

        composeTestRule.onNodeWithText("D8").assertIsDisplayed()
    }

    @Test
    fun givenD12_whenRenderedAtSmallSize_thenContentLabelIsDisplayed() {
        composeTestRule.setContent {
            DiceRollerTheme(dynamicColor = false) {
                DicePolygon(dice = Dice.D12, sizeVariant = DicePolygonSize.Small) {
                    Text(text = "D12")
                }
            }
        }

        composeTestRule.onNodeWithText("D12").assertIsDisplayed()
    }

    @Test
    fun givenD12_whenRenderedAtLargeSize_thenContentLabelIsDisplayed() {
        composeTestRule.setContent {
            DiceRollerTheme(dynamicColor = false) {
                DicePolygon(dice = Dice.D12, sizeVariant = DicePolygonSize.Large) {
                    Text(text = "D12")
                }
            }
        }

        composeTestRule.onNodeWithText("D12").assertIsDisplayed()
    }

    // -------------------------------------------------------------------------
    // Full screen smoke test — D4 and D20 in the selector row render correctly
    // -------------------------------------------------------------------------

    @Test
    fun givenScreenWithD4AsSelectedDice_whenScreenIsDisplayed_thenD4SelectorIsPresent() {
        composeTestRule.setContent {
            DiceRollerTheme(dynamicColor = false) {
                DiceRollerScreen(
                    uiState = fr.mandarine.diceroller.presentation.DiceRollerUiState(
                        selectedDice = Dice.D4,
                        result = null,
                    ),
                    onSelectDice = {},
                    onRollDice = {},
                )
            }
        }

        composeTestRule
            .onNodeWithText("Roll D4")
            .assertIsDisplayed()
    }

    @Test
    fun givenScreenWithD20AsSelectedDice_whenScreenIsDisplayed_thenD20SelectorIsPresent() {
        composeTestRule.setContent {
            DiceRollerTheme(dynamicColor = false) {
                DiceRollerScreen(
                    uiState = fr.mandarine.diceroller.presentation.DiceRollerUiState(
                        selectedDice = Dice.D20,
                        result = null,
                    ),
                    onSelectDice = {},
                    onRollDice = {},
                )
            }
        }

        composeTestRule
            .onNodeWithText("Roll D20")
            .assertIsDisplayed()
    }

    @Test
    fun givenScreenWithD4ResultDisplayed_whenScreenIsRendered_thenResultValueIsShown() {
        composeTestRule.setContent {
            DiceRollerTheme(dynamicColor = false) {
                DiceRollerScreen(
                    uiState = fr.mandarine.diceroller.presentation.DiceRollerUiState(
                        selectedDice = Dice.D4,
                        result = 3,
                    ),
                    onSelectDice = {},
                    onRollDice = {},
                )
            }
        }

        composeTestRule.onNodeWithText("3").assertIsDisplayed()
    }

    @Test
    fun givenScreenWithD20ResultDisplayed_whenScreenIsRendered_thenResultValueIsShown() {
        composeTestRule.setContent {
            DiceRollerTheme(dynamicColor = false) {
                DiceRollerScreen(
                    uiState = fr.mandarine.diceroller.presentation.DiceRollerUiState(
                        selectedDice = Dice.D20,
                        result = 17,
                    ),
                    onSelectDice = {},
                    onRollDice = {},
                )
            }
        }

        composeTestRule.onNodeWithText("17").assertIsDisplayed()
    }
}
