package me.geckotv.apkmanager.feature.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import me.geckotv.apkmanager.core.auth.AuthState
import me.geckotv.apkmanager.core.designsystem.theme.ApkManagerTheme
import me.geckotv.apkmanager.core.model.AuthMethod
import me.geckotv.apkmanager.core.model.GitHubAccessLevel
import me.geckotv.apkmanager.core.model.GitHubAccount
import me.geckotv.apkmanager.core.network.model.RateLimit
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class AccountScreenScreenshotTest {
    @Test
    fun signedOutLight() =
        capture("account_signed_out_light", darkTheme = false) {
            AccountScreenFor(state(AuthState.SignedOut))
        }

    @Test
    fun signedInDark() =
        capture("account_signed_in_dark", darkTheme = true) {
            val account =
                GitHubAccount(
                    login = "octocat",
                    name = "The Octocat",
                    avatarUrl = null,
                    authMethod = AuthMethod.OAuth,
                    accessLevel = GitHubAccessLevel.Private,
                )
            AccountScreenFor(
                state(
                    AuthState.SignedIn(account),
                    rateLimit = RateLimit("core", 5_000, 4_987, Instant.parse("2026-01-01T12:00:00Z")),
                ),
            )
        }

    @Test
    fun deviceCodeLight() =
        capture("account_device_code_light", darkTheme = false) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(modifier = Modifier.padding(16.dp)) {
                    DeviceCodeSheetContent(
                        userCode = "ABCD-1234",
                        onCopyCodeAndOpen = {},
                        onCancel = {},
                        showProgressIndicator = false,
                    )
                }
            }
        }

    private fun capture(
        name: String,
        darkTheme: Boolean,
        content: @Composable () -> Unit,
    ) {
        captureRoboImage(filePath = "build/outputs/roborazzi/$name.png") {
            ApkManagerTheme(darkTheme = darkTheme, dynamicColor = false, content = content)
        }
    }

    private fun state(
        authState: AuthState,
        rateLimit: RateLimit? = null,
    ) = AccountUiState.Ready(
        authState = authState,
        rateLimit = rateLimit,
        accessLevel = GitHubAccessLevel.Private,
        sheet = null,
        isWorking = false,
        error = null,
    )
}

@Composable
private fun AccountScreenFor(state: AccountUiState) {
    AccountScreen(
        uiState = state,
        onBack = {},
        onAccessLevelSelect = {},
        onSignInClick = {},
        onUseTokenClick = {},
        onSignInWithToken = {},
        onCopyCodeAndOpen = {},
        onCancelSignIn = {},
        onDismissSheet = {},
        onSignOut = {},
        onDismissError = {},
    )
}
