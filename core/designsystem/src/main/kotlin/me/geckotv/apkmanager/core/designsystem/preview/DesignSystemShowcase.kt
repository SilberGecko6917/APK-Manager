package me.geckotv.apkmanager.core.designsystem.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.geckotv.apkmanager.core.designsystem.component.ApkManagerTopAppBar
import me.geckotv.apkmanager.core.designsystem.component.ButtonGroupOption
import me.geckotv.apkmanager.core.designsystem.component.NavigationToolbar
import me.geckotv.apkmanager.core.designsystem.component.NavigationToolbarItem
import me.geckotv.apkmanager.core.designsystem.component.Section
import me.geckotv.apkmanager.core.designsystem.component.SectionHeader
import me.geckotv.apkmanager.core.designsystem.component.SectionItem
import me.geckotv.apkmanager.core.designsystem.component.SectionItemIcon
import me.geckotv.apkmanager.core.designsystem.component.SingleChoiceButtonGroup
import me.geckotv.apkmanager.core.designsystem.component.SwitchSectionItem
import me.geckotv.apkmanager.core.designsystem.component.section
import me.geckotv.apkmanager.core.designsystem.icon.ApkManagerIcons
import me.geckotv.apkmanager.core.designsystem.theme.ApkManagerTheme

private data class SampleApp(
    val name: String,
    val versions: String,
)

private val sampleUpdates =
    listOf(
        SampleApp("Essentials", "21.3 → 21.4"),
        SampleApp("Obtainium", "1.2.1 → 1.2.3"),
        SampleApp("Private tool", "0.9.0 → 1.0.0"),
    )

/** Theme options shown in the [DesignSystemShowcase]. */
@Immutable
data class ShowcaseThemeSettings(
    val themeMode: ShowcaseThemeMode = ShowcaseThemeMode.System,
    val dynamicColor: Boolean = true,
    val amoledBlack: Boolean = false,
    val hapticsEnabled: Boolean = true,
)

enum class ShowcaseThemeMode { System, Light, Dark }

/**
 * A screen built only from design system components. Used for previews, screenshot tests and the
 * app's temporary home screen, so changes to the design system are easy to review.
 */
@Composable
fun DesignSystemShowcase(
    settings: ShowcaseThemeSettings,
    onSettingsChange: (ShowcaseThemeSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedDestination by remember { mutableIntStateOf(0) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                ApkManagerTopAppBar(
                    title = "APK Manager",
                    subtitle = "3 updates available",
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = innerPadding.calculateTopPadding(),
                        bottom = innerPadding.calculateBottomPadding() + 112.dp,
                    ),
            ) {
                section(items = sampleUpdates, title = "Updates", key = { it.name }) { app ->
                    SectionItem(
                        headline = app.name,
                        supportingText = app.versions,
                        leadingContent = { SectionItemIcon(ApkManagerIcons.Apps) },
                        trailingContent = { FilledTonalButton(onClick = {}) { Text("Update") } },
                        onClick = {},
                    )
                }
                item { ThemeSettings(settings = settings, onSettingsChange = onSettingsChange) }
                item { AboutSection() }
            }
        }
        ShowcaseNavigationToolbar(
            selectedIndex = selectedDestination,
            onItemSelect = { selectedDestination = it },
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
        )
    }
}

@Composable
private fun ThemeSettings(
    settings: ShowcaseThemeSettings,
    onSettingsChange: (ShowcaseThemeSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SectionHeader(text = "Theme")
        SingleChoiceButtonGroup(
            options =
                listOf(
                    ButtonGroupOption("System", ApkManagerIcons.BrightnessAuto),
                    ButtonGroupOption("Light", ApkManagerIcons.LightMode),
                    ButtonGroupOption("Dark", ApkManagerIcons.DarkMode),
                ),
            selectedIndex = settings.themeMode.ordinal,
            onOptionSelect = { onSettingsChange(settings.copy(themeMode = ShowcaseThemeMode.entries[it])) },
            modifier = Modifier.fillMaxWidth(),
        )
        Section(modifier = Modifier.padding(top = 8.dp)) {
            item {
                SwitchSectionItem(
                    headline = "Dynamic color",
                    supportingText = "Use colors from your wallpaper",
                    checked = settings.dynamicColor,
                    onCheckedChange = { onSettingsChange(settings.copy(dynamicColor = it)) },
                    leadingContent = { SectionItemIcon(ApkManagerIcons.Palette) },
                )
            }
            item {
                SwitchSectionItem(
                    headline = "Pitch black",
                    supportingText = "Black background in dark theme",
                    checked = settings.amoledBlack,
                    onCheckedChange = { onSettingsChange(settings.copy(amoledBlack = it)) },
                    leadingContent = { SectionItemIcon(ApkManagerIcons.Contrast) },
                )
            }
            item {
                SwitchSectionItem(
                    headline = "Haptic feedback",
                    checked = settings.hapticsEnabled,
                    onCheckedChange = { onSettingsChange(settings.copy(hapticsEnabled = it)) },
                    leadingContent = { SectionItemIcon(ApkManagerIcons.Vibration) },
                )
            }
        }
    }
}

@Composable
private fun AboutSection(modifier: Modifier = Modifier) {
    Section(modifier = modifier, title = "About") {
        item {
            SectionItem(
                headline = "Source code",
                supportingText = "github.com/SilberGecko6917/APK-Manager",
                leadingContent = { SectionItemIcon(ApkManagerIcons.Code) },
                trailingContent = {
                    Icon(
                        painter = painterResource(ApkManagerIcons.OpenInNew),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                onClick = {},
            )
        }
    }
}

@Composable
private fun ShowcaseNavigationToolbar(
    selectedIndex: Int,
    onItemSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationToolbar(
        items =
            listOf(
                NavigationToolbarItem("Apps", ApkManagerIcons.Apps, ApkManagerIcons.AppsFilled),
                NavigationToolbarItem("Discover", ApkManagerIcons.Explore, ApkManagerIcons.ExploreFilled),
                NavigationToolbarItem("Settings", ApkManagerIcons.Settings, ApkManagerIcons.SettingsFilled),
            ),
        selectedIndex = selectedIndex,
        onItemSelect = onItemSelect,
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(onClick = {}) {
                Icon(painter = painterResource(ApkManagerIcons.Add), contentDescription = "Add app")
            }
        },
    )
}

@Preview(name = "Light", showBackground = true, heightDp = 900)
@Composable
private fun DesignSystemShowcaseLightPreview() {
    ApkManagerTheme(darkTheme = false, dynamicColor = false) {
        DesignSystemShowcase(settings = ShowcaseThemeSettings(dynamicColor = false), onSettingsChange = {})
    }
}

@Preview(name = "Dark", showBackground = true, heightDp = 900)
@Composable
private fun DesignSystemShowcaseDarkPreview() {
    ApkManagerTheme(darkTheme = true, dynamicColor = false) {
        DesignSystemShowcase(settings = ShowcaseThemeSettings(dynamicColor = false), onSettingsChange = {})
    }
}
