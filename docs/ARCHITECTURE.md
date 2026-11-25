# Architecture

This document describes the technical design of APK Manager. It is updated as the implementation evolves.

## Goals

1. Keep apps from GitHub Releases up to date, including apps from **private repositories**.
2. Stay well within GitHub's API rate limits, even with many tracked apps.
3. Never install an APK that is not what the user expects (wrong package, wrong signer, downgrade).
4. Make updates as automatic as Android allows.
5. A clean, modern Material 3 Expressive UI.
6. No backend server: everything runs on the device.

## Tech stack

| Area | Choice |
|---|---|
| Language / build | Kotlin, Gradle (Kotlin DSL) with version catalog and convention plugins, JDK 21 |
| SDK levels | `minSdk` 26, `targetSdk` 37, `compileSdk` 37.2 |
| UI | Jetpack Compose, Material 3 (Expressive components), Material Symbols icons, materialKolor (seed color schemes) |
| Navigation | Navigation 3 |
| Dependency injection | Hilt |
| Persistence | Room (tracked apps, releases), DataStore (settings, encrypted token) |
| Networking | OkHttp, Retrofit, kotlinx.serialization |
| Background work | WorkManager |
| Images | Coil |
| Privileged installs | Shizuku API (optional) |
| Quality | Spotless (ktlint), Detekt, Android Lint, JUnit, Turbine, MockWebServer, Roborazzi + Robolectric (screenshot tests) |

Exact versions live in `gradle/libs.versions.toml`.

## Modules

```
:app                  MainActivity, navigation shell, DI wiring
:core:designsystem    Theme, typography, shapes, shared components
:core:model           Domain models (plain Kotlin)
:core:common          Coroutine dispatchers, result types, utilities
:core:network         GitHub REST + GraphQL clients, authentication, rate limits
:core:database        Room database, DAOs, entities
:core:datastore       Settings and encrypted credentials
:core:data            Repositories, source providers, update checking
:core:installer       Download, verification, installer backends
:feature:apps         Apps list
:feature:details      App details, release and asset selection
:feature:add          "Add app" flow
:feature:settings     Settings, account, about
:feature:discover     Catalog and search (v1.0)
build-logic/          Gradle convention plugins
```

Modules are added when the feature that needs them is implemented.

### Patterns

- **Unidirectional data flow:** ViewModels expose a single `StateFlow<UiState>` and receive user actions as function calls or events.
- Repositories are the single source of truth and expose `Flow`s backed by Room.
- Composables are stateless where possible; screen-level composables collect state, components receive plain values and callbacks.

## Sources

Every place an app can be fetched from implements a `SourceProvider`:

```kotlin
interface SourceProvider {
    fun canHandle(url: String): Boolean
    suspend fun resolve(input: String): SourceApp
    suspend fun latestReleases(apps: List<TrackedApp>): Map<TrackedAppId, ReleaseResult>
    suspend fun openDownload(asset: ReleaseAsset): DownloadStream
}
```

GitHub comes first. Codeberg/Forgejo/Gitea and GitLab follow in v0.4 without changes to the rest of the app.

## GitHub authentication

APK Manager has no server, so it cannot keep a client secret. GitHub's **OAuth device flow** does not need one.

| Mode | How | Access | Rate limit |
|---|---|---|---|
| Signed out | — | Public repositories | 60 requests/hour per IP |
| Signed in | OAuth device flow, no scope | Public repositories | 5,000 requests/hour |
| Signed in + private repos | OAuth device flow, `repo` scope | All repositories the user can access | 5,000 requests/hour |
| Personal access token | Fine-grained PAT with "Contents: Read" on selected repositories | Selected repositories only | 5,000 requests/hour |

- The OAuth app's client ID is public and is set as a build config field. Device flow must be enabled in the OAuth app settings.
- The `repo` scope is broad because OAuth apps have no read-only scope for private repositories. Users who want least privilege can use a fine-grained PAT instead.
- Organizations with OAuth app access restrictions must approve the app before its tokens can read the organization's private repositories.
- Tokens are encrypted with an AES-GCM key from the Android Keystore and stored in DataStore. They are excluded from backups and exports. (`androidx.security:security-crypto` is deprecated and not used.)
- `401` responses are treated as "token expired or revoked", not as a rate limit.

## Update checking

- **Signed in:** a GraphQL query fetches the latest releases of up to ~50 repositories at once (about 1 point per query).
- **Signed out:** REST `GET /repos/{owner}/{repo}/releases/latest` with `If-None-Match`. A `304 Not Modified` response does not count against the primary rate limit for authenticated requests.
- Pre-releases use the release list endpoint and are filtered on the client.
- `x-ratelimit-remaining`, `x-ratelimit-reset` and `retry-after` are always parsed. Checks back off when the quota runs low.

### Version detection

1. If the app was installed through APK Manager, compare the stored release ID / tag with the latest release.
2. Otherwise, compare the release tag with the installed `versionName` using a tolerant semantic version parser (an optional per-app regex can extract the version from the tag).
3. After downloading, the APK's `versionCode` is authoritative.

### Asset selection

1. Only `.apk` assets (later also `.apks` / `.xapk`). Ignore checksums, signatures and debug builds.
2. Apply the per-app include/exclude regex.
3. Rank by device ABI (`Build.SUPPORTED_ABIS` order), then universal APKs.
4. If the choice is still ambiguous, ask the user once and remember the choice.

## Install pipeline

```
download → verify file → inspect APK → compare with installed app → install → clean up
```

1. **Download.** Public repositories: `browser_download_url`. Private repositories: `GET /repos/{owner}/{repo}/releases/assets/{id}` with `Accept: application/octet-stream` and the token; the redirect to the storage host is followed **without** the `Authorization` header.
2. **Verify the file:** size and SHA-256 digest (from the release asset's `digest` field when available).
3. **Inspect the APK:** `PackageManager.getPackageArchiveInfo(…, GET_SIGNING_CERTIFICATES)` gives the package name, version code and signing certificates.
4. **Compare with the installed app:** same package name as tracked, same signing certificate (including rotation history), no downgrade unless explicitly allowed.
5. **Install** through the selected installer backend.
6. **Clean up** the downloaded file.

### Installer backends

```kotlin
interface Installer {
    suspend fun install(session: InstallRequest): InstallResult
}
```

- **Default:** `PackageInstaller` sessions. Results arrive through a broadcast receiver; `STATUS_PENDING_USER_ACTION` shows the system confirmation (or a notification when in the background).
- **Shizuku:** privileged session through Shizuku, no confirmation needed for any app.
- **Root:** `pm install` through `su`.
- **Dhizuku:** later.

## Automatic updates

Without root or Shizuku, Android 12+ allows unattended updates through `setRequireUserAction(USER_ACTION_NOT_REQUIRED)` only when:

- APK Manager holds `REQUEST_INSTALL_PACKAGES` and declares `UPDATE_PACKAGES_WITHOUT_USER_ACTION`,
- APK Manager is the installer of record (or update owner) of the app being updated, and
- the app being updated targets a recent enough SDK (e.g. API 33+ on Android 15, API 34+ on Android 16).

In practice, the first update of an app installed by another source needs one confirmation. After that, updates can run unattended. With Shizuku or root, all updates can be unattended.

Background checks run as unique periodic WorkManager work with network and battery constraints. Downloads run as separate work, so a check stays cheap.

## Android developer verification

Google is rolling out developer verification for apps installed outside Google Play. It is enforced in a few countries since September 2026 and is planned globally for 2027. Installs and updates of apps from unregistered developers then only work if the user has enabled the "advanced flow".

- The APK Manager package and signing key will be registered before the global rollout.
- Install results with developer verification failure reasons are shown with a clear explanation and a link to the system setting.

## Security and privacy

- No analytics, no tracking, no backend.
- Tokens: Keystore-encrypted, never logged, excluded from backups and exports.
- APKs are verified before installation (see [install pipeline](#install-pipeline)).
- Network requests go only to the configured sources (GitHub API and its download hosts).

## Design

The visual language follows Material 3 Expressive and is inspired by [Essentials](https://github.com/sameerasw/essentials).
Everything lives in `:core:designsystem`:

| Piece | Implementation |
|---|---|
| Theme | `ApkManagerTheme`: dynamic color (Material You) on Android 12+, otherwise a scheme generated from a seed color (materialKolor, selectable `ColorStyle`), light / dark / pitch black, expressive motion scheme |
| Typography | Google Sans Flex with fully rounded terminals (`ROND` axis), subset to Latin scripts |
| Shapes | Slightly larger radii than the Material defaults (12 / 16 / 20 / 28 dp) |
| Grouped lists | `Section` / `LazyListScope.section` built on Material's `SegmentedListItem`: large outer corners, small inner corners, small gaps. `SectionItem`, `SwitchSectionItem`, `SectionItemIcon`, `SectionHeader` |
| Navigation | `NavigationToolbar`: a floating toolbar whose selected destination expands to show its label, with an optional FAB |
| Top app bar | `ApkManagerTopAppBar`: large flexible top app bar with optional subtitle, collapses on scroll |
| Choices | `SingleChoiceButtonGroup`: connected button group, the selected button morphs into a pill |
| Sheets | `ApkManagerBottomSheet`: modal bottom sheets instead of dialogs |
| Icons | `ApkManagerIcons`: Material Symbols Rounded vector drawables, filled variants for selected states |
| Haptics | Components perform haptic feedback through `LocalHapticFeedback`; `ApkManagerTheme(hapticsEnabled = false)` turns it off everywhere |

`DesignSystemShowcase` combines all components on one screen. It is used for previews and screenshot tests
(`./gradlew :core:designsystem:recordRoborazziDebug`, output in `core/designsystem/build/outputs/roborazzi/`).
