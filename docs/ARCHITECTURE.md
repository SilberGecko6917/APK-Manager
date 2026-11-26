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
| Persistence | Room 3 (tracked apps, latest releases), DataStore (settings, encrypted token) |
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
:core:model           Domain models and pure logic (versions, asset selection, input parsing), plain Kotlin
:core:common          Coroutine dispatchers, application scope, clock
:core:network         GitHub REST + GraphQL clients, rate limits
:core:auth            GitHub sign-in (device flow, personal access tokens), encrypted token storage
:core:database        Room database, DAOs, entities
:core:datastore       Settings
:core:data            Repositories: tracked apps, installed apps, update checking
:core:work            Background update checks (WorkManager) and update notifications
:core:installer       Download, verification, installer backends
:core:testing         Test fakes and rules
:feature:apps         Apps list, "Add app" flow, app details (release notes, history)
:feature:onboarding   First-launch permissions
:feature:account      GitHub account and sign-in
:feature:settings     Settings, about
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

- The OAuth app's client ID is public and is set as a build config field from the Gradle property `apkmanager.github.clientId` (see `gradle.properties`). Forks can point it at their own OAuth app. Device flow must be enabled in the OAuth app settings.
- `:core:auth` runs the device flow (`DeviceFlowClient`), validates tokens against `GET /user` (the `X-OAuth-Scopes` header tells the access level of classic tokens) and implements `GitHubCredentialsProvider` for the network layer with an in-memory cache.
- The `repo` scope is broad because OAuth apps have no read-only scope for private repositories. Users who want least privilege can use a fine-grained PAT instead.
- Organizations with OAuth app access restrictions must approve the app before its tokens can read the organization's private repositories.
- Tokens are encrypted with an AES-GCM key from the Android Keystore and stored in a dedicated DataStore file (`credentials`) that is excluded from backups and device transfers. A token that cannot be decrypted counts as signed out. (`androidx.security:security-crypto` is deprecated and not used.)
- `401` responses are treated as "token expired or revoked", not as a rate limit.

## Update checking

- **Signed in:** a GraphQL query fetches the latest releases of up to ~50 repositories at once (about 1 point per query).
- **Signed out:** REST `GET /repos/{owner}/{repo}/releases/latest` with `If-None-Match`. A `304 Not Modified` response does not count against the primary rate limit for authenticated requests.
- Pre-releases use the release list endpoint and are filtered on the client.
- `x-ratelimit-remaining`, `x-ratelimit-reset` and `retry-after` are always parsed. Checks back off when the quota runs low.

### Networking (`:core:network`)

- `GitHubNetworkDataSource` wraps the REST endpoints (repository, latest release, releases, asset download) and the batched GraphQL query. Every call returns a `GitHubResult`: `Success` (with the `ETag`), `NotModified` or `Failure` with a `GitHubError` (`NotFound`, `Unauthorized`, `Forbidden`, `RateLimited`, `Server`, `Network`, `Unexpected`).
- All requests to `api.github.com` send `Accept: application/vnd.github+json`, `X-GitHub-Api-Version: 2022-11-28`, a `User-Agent` and, when signed in, the token. The token is supplied by a `GitHubCredentialsProvider`.
- `RateLimitTracker` exposes the latest `x-ratelimit-*` values per resource as a `StateFlow`.
- Asset downloads use the asset API URL with `Accept: application/octet-stream`. GitHub redirects to a storage host; the token is never sent there, which a test verifies.

### Tracked apps (`:core:data`)

- The database (`:core:database`, Room 3) stores each tracked app (repository, linked package name, pre-release setting, preferred asset name, ETag) with its **latest release** and that release's assets. Older releases are fetched on demand.
- `TrackedAppsRepository` combines the database with the installed apps: `TrackedApp.updateStatus` compares the installed version with the latest release (see below) and updates when apps are installed, updated or removed (package broadcasts).
- `ReleaseChecker` checks all apps with as few requests as possible:
  - **Signed in:** one GraphQL query per 50 apps returns the latest release IDs; only apps whose release changed are fetched again through REST for their assets and notes.
  - **Signed out:** `GET …/releases/latest` per app with the stored `ETag`.
  - **Pre-releases included:** `GET …/releases` per app (with `ETag`), because "latest" skips pre-releases.
  - It stops at the first rate limit error and reports the remaining apps as rate limited.
- Adding an app looks up the repository and its 20 newest releases once, so the "Add app" sheet can switch between stable and pre-releases and pick an asset without further requests.
- Installed apps are read through `PackageManager`, which needs `QUERY_ALL_PACKAGES` to see apps from other sources.

### Background checks (`:core:work`)

- `UpdateCheckScheduler` follows the settings: while background checks are on, a unique periodic WorkManager job runs every 6, 12 or 24 hours with a network constraint (any network, or unmetered only) and "battery not low"; turning checks off cancels it.
- `UpdateCheckWorker` (`@HiltWorker`) runs the same `checkForUpdates()` as the app and retries with WorkManager's backoff when no app could be checked (e.g. offline).
- `UpdateNotifier` posts one notification per app with an update, grouped under a summary, and only once per release (`tracked_apps.notified_release_id`). Notifications of apps that are up to date again are removed.
- Apps whose updates are installed automatically are not notified after a check; instead the check schedules the [automatic updates](#automatic-updates).
- WorkManager is initialized on demand with Hilt's worker factory (`ApkManagerApplication` implements `Configuration.Provider`; the default initializer is removed in the manifest).

### Version detection

`UpdateStatus.of` decides in this order:

1. **Skipped release:** if the user marked the latest release as skipped ("Skip this update"), the app is up to date until a newer release appears (`tracked_apps.skipped_release_id`).
2. **Installed through APK Manager:** after a successful install the release ID and the APK's `versionCode` are stored (`installed_release_id`, `installed_version_code`). As long as the installed `versionCode` is unchanged, the same release ID means "up to date", whatever the tag says. Installing a release clears the skipped release.
3. **Otherwise**, the version is taken from the tag (with the optional per-app regex: its first group, or the whole match) and compared with the installed `versionName` using a tolerant version parser (`Version`): it takes the first number sequence (`v1.2.3`, `release-2024.05.01`), understands pre-release labels (`dev` < `alpha` < `beta` < `pre` < `rc` < final) and ignores other suffixes (`-foss`, `(45)`, `+build`). Tags without a number (`nightly`) are compared as normalized text. If they still differ, the status is "unknown", or "update available" when APK Manager installed a different release.
4. After downloading, the APK's `versionCode` is authoritative (no downgrades).

### Asset selection

1. Only `.apk` assets (later also `.apks` / `.xapk`). Ignore checksums, signatures and debug builds.
2. Apply the per-app include/exclude regex (case-insensitive, matched anywhere in the file name; an invalid pattern is ignored). The details screen previews which assets each filter keeps.
3. Rank by device ABI (`Build.SUPPORTED_ABIS` order, ABI tokens such as `arm64`, `aarch64`, `armv7`, `x86_64` in the file name), then universal APKs, then debug builds. Incompatible ABIs are left out.
4. If the choice is still ambiguous, ask the user once and remember the asset name. Later releases match it with versions and numbers ignored (`app-v1.2.0-arm64.apk` matches `app-v1.3.1-arm64.apk`).

## Install pipeline

`:core:installer` exposes `AppInstaller`: installs run in the application scope, so they continue when the user leaves the screen, and their state (`Downloading`, `Verifying`, `Installing`, `WaitingForUser`, `Succeeded`, `Failed`) is observable per app.

```
pick APK → download → verify file → inspect APK → compare with installed app → install → clean up
```

1. **Pick the APK** with `ApkAssets.select` (asset filter, device ABI, remembered asset name).
2. **Download** through the asset API URL (`GET /repos/{owner}/{repo}/releases/assets/{id}` with `Accept: application/octet-stream`), for public and private repositories alike. The redirect to the storage host is followed **without** the `Authorization` header. The SHA-256 digest is computed while the file is written to the cache directory.
3. **Verify the file:** size and SHA-256 digest (from the release asset's `digest` field when GitHub provides it).
4. **Inspect the APK:** `PackageManager.getPackageArchiveInfo(…, GET_SIGNING_CERTIFICATES)` gives the package name, version code and signing certificates (SHA-256 of each certificate).
5. **Compare with the installed app** (`ApkVerifier`):
   - the package name must match the linked package of the tracked app,
   - the installed app's signer must be in the APK's signing lineage (APK Signature Scheme v3 key rotation); apps with several signers need the same signer set,
   - no downgrade (lower `versionCode`).
6. **Install** through a `PackageInstaller` session (`MODE_FULL_INSTALL`, `setRequireUserAction(USER_ACTION_NOT_REQUIRED)` on Android 12+, package source "downloaded file"). The session status arrives at a non-exported broadcast receiver through a mutable, explicit `PendingIntent`. `STATUS_PENDING_USER_ACTION` carries the system confirmation, which the UI launches while the app is in the foreground.
7. **Clean up** the downloaded file and remember the installed release. A tracked app without a linked package is linked to the installed package.

While installs run, `InstallNotifications` shows one notification per app (download progress with "Cancel", checking, installing, or "Tap to install" when the system confirmation is waiting) and runs `InstallService`, a `dataSync` foreground service, so Android does not stop a download when the user leaves the app.

Session failures are mapped to clear messages, including signature conflicts (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`) and Android developer verification failures (`EXTRA_DEVELOPER_VERIFICATION_FAILURE_REASON`, Android 16 QPR2+).

### Self-updates

Release builds add APK Manager's own repository (Gradle property `apkmanager.github.repository`) to the tracked apps once, linked to its own package, so it updates itself through the same pipeline. Pre-release builds follow pre-releases. Android kills the process while it replaces the app; the next start shows the new version.

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

Each tracked app has an update policy: **notify only** or **install automatically**. Apps without their own policy follow the default in the settings (notify only).

1. After a background check, `AutoUpdater` (`:core:work`) picks the apps to update: policy "install automatically", installed, an update available, and a release APK Manager has not tried before (`tracked_apps.auto_update_release_id`). These apps are not notified.
2. If there are any, `AutoUpdateScheduler` enqueues a unique one-time `AutoUpdateWorker` with the constraints from the settings: unmetered network (default on), charging (default off), and always "battery not low". The check itself stays cheap.
3. `AutoUpdateWorker` installs the updates one after another through `AppInstaller` (APK Manager itself last, because updating it ends the process), so the usual download, verification and install notifications apply. Each release is tried once:
   - **installed:** an "updated" notification in a low-importance channel,
   - **Android asks for a confirmation:** the install notification says "Tap to install",
   - **failed for good** (e.g. a different signing key): notified like any other update,
   - **failed for a reason that may pass** (offline, server error, rate limit, storage): retried with WorkManager's backoff and at the next check.

Without root or Shizuku, Android 12+ installs an update without asking (`setRequireUserAction(USER_ACTION_NOT_REQUIRED)`) only when:

- APK Manager holds `REQUEST_INSTALL_PACKAGES` and declares `UPDATE_PACKAGES_WITHOUT_USER_ACTION`,
- APK Manager is the installer of record (or update owner on Android 14+) of the app being updated, or the app is APK Manager itself, and
- the app being updated targets a recent enough API level: 29 on Android 12, 30 on 13, 31 on 14, and from Android 15 on two versions below the device's (33 on 15, 34 on 16).

`UnattendedUpdates.isPossible` mirrors these rules so the UI can tell the user up front; Android may still ask, which the install pipeline handles. In practice, the first update of an app installed by another source needs one confirmation. After that, updates can run unattended. With Shizuku or root (planned), all updates can be unattended.

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
| Navigation | `NavigationToolbar`: a floating toolbar whose selected destination expands to show its label, with an optional FAB. On large screens, Navigation 3's `ListDetailSceneStrategy` shows the apps list and the app details side by side |
| Top app bar | `ApkManagerTopAppBar`: large flexible top app bar with optional subtitle, collapses on scroll |
| Choices | `SingleChoiceButtonGroup`: connected button group, the selected button morphs into a pill |
| Sheets | `ApkManagerBottomSheet`: modal bottom sheets instead of dialogs |
| Icons | `ApkManagerIcons`: Material Symbols Rounded vector drawables, filled variants for selected states |
| Haptics | Components perform haptic feedback through `LocalHapticFeedback`; `ApkManagerTheme(hapticsEnabled = false)` turns it off everywhere |

`DesignSystemShowcase` combines all components on one screen. It is used for previews and screenshot tests
(`./gradlew :core:designsystem:recordRoborazziDebug`, output in `core/designsystem/build/outputs/roborazzi/`).
