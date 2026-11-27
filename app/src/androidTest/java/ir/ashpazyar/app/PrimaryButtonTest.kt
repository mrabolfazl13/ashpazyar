package ir.ashpazyar.app

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import ir.ashpazyar.app.ui.components.PrimaryButton
import ir.ashpazyar.app.ui.theme.AshpazyarTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PrimaryButtonTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun buttonIsVisibleAndClickable() {
        var clicked = false
        compose.setContent {
            AshpazyarTheme {
                PrimaryButton(text = "شروع آشپزی", onClick = { clicked = true })
            }
        }
        compose.onNodeWithText("شروع آشپزی").assertIsDisplayed().performClick()
        assertTrue(clicked)
    }

    @Test
    fun secondaryLabelIsDisplayed() {
        compose.setContent {
            AshpazyarTheme {
                Text("آشپزیار")
            }
        }
        compose.onNodeWithText("آشپزیار").assertIsDisplayed()
    }
}
