package ir.ashpazyar.app.ui.screens.update

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import ir.ashpazyar.app.domain.model.UpdateManifest
import ir.ashpazyar.app.ui.theme.AshpazyarTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class UpdateOverlayTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val manifest = UpdateManifest(
        productId = "ashpazyar",
        platform = "android",
        latestVersion = "1.3.0",
        minimumSupportedVersion = "1.2.0",
        forceUpdate = false,
        updatePolicy = "optional",
        releaseChannel = "stable",
        downloadUrl = "https://cafebazaar.ir/app/ir.ashpazyar.app",
        sha256 = "a".repeat(64),
        releaseNotes = "تازه‌های قاشق",
        publishedAt = "2026-11-28T10:00:00Z",
        versionCode = 6
    )

    @Test
    fun optionalUpdateCanBePostponed() {
        composeRule.setContent {
            AshpazyarTheme {
                UpdateOverlay(
                    state = UpdateUiState(
                        phase = UpdatePhase.AVAILABLE,
                        manifest = manifest,
                        mandatory = false,
                        currentVersion = "1.2.2"
                    ),
                    onRetry = {},
                    onDismiss = {},
                    onOpenBazaar = {},
                    onCloseMessage = {}
                )
            }
        }
        composeRule.onNodeWithText("نسخه تازه قاشق آماده است").assertIsDisplayed()
        composeRule.onNodeWithText("بعداً").assertIsDisplayed()
        composeRule.onNodeWithText("رفتن به بازار").assertIsDisplayed()
    }

    @Test
    fun forcedUpdateHasNoPostponeAction() {
        composeRule.setContent {
            AshpazyarTheme {
                UpdateOverlay(
                    state = UpdateUiState(
                        phase = UpdatePhase.FORCED,
                        manifest = manifest.copy(updatePolicy = "forced", forceUpdate = true),
                        mandatory = true,
                        currentVersion = "1.2.2"
                    ),
                    onRetry = {},
                    onDismiss = {},
                    onOpenBazaar = {},
                    onCloseMessage = {}
                )
            }
        }
        composeRule.onNodeWithText("به‌روزرسانی الزامی").assertIsDisplayed()
        composeRule.onNodeWithText("رفتن به بازار").assertIsDisplayed()
        composeRule.onNodeWithText("بعداً").assertDoesNotExist()
    }

    @Test
    fun bazaarButtonInvokesOnlyStoreCallback() {
        var opened = false
        composeRule.setContent {
            AshpazyarTheme {
                UpdateOverlay(
                    state = UpdateUiState(
                        phase = UpdatePhase.AVAILABLE,
                        manifest = manifest,
                        mandatory = false,
                        currentVersion = "1.2.2"
                    ),
                    onRetry = {},
                    onDismiss = {},
                    onOpenBazaar = { opened = true },
                    onCloseMessage = {}
                )
            }
        }
        composeRule.onNodeWithText("رفتن به بازار").performClick()
        composeRule.runOnIdle { assertTrue(opened) }
    }
}
