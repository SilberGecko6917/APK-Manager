# Roadmap

This roadmap is a living document. Each milestone maps to a GitHub milestone, and each item to one or more issues and pull requests.
Design and technical decisions are described in [ARCHITECTURE.md](ARCHITECTURE.md).

## v0.1 — Foundation

The first usable version: track GitHub apps and install updates manually.

- [x] **Project setup** (`chore/project-setup`, #9)
  - Gradle version catalog, `build-logic` convention plugins, module skeleton
  - Spotless (ktlint), Detekt, Android Lint
  - CI: build, unit tests and static analysis on every pull request
  - Dependabot for Gradle and GitHub Actions
- [x] **Design system** (`feat/design-system`)
  - Theme: Material You dynamic color, custom seed color, light/dark/AMOLED black
  - Typography (rounded variable font), shapes and motion
  - Grouped segmented lists, flexible top app bars, floating toolbar navigation, button groups, bottom sheets
  - Screenshot tests (Roborazzi) for light, dark and pitch black themes
- [x] **App shell** (`feat/app-shell`)
  - Navigation 3 with predictive back and adaptive two-pane layouts
  - Onboarding: install-unknown-apps and notification permissions
  - Screen skeletons: Apps, Settings
- [x] **GitHub client** (`feat/github-client`)
  - REST and GraphQL clients, ETag caching, rate limit tracking
  - Release and asset models, tests against MockWebServer
- [x] **GitHub sign-in** (`feat/github-auth`)
  - OAuth device flow (public access, optional private repo access)
  - Fine-grained personal access token as an alternative
  - Token encrypted with an Android Keystore key, excluded from backups
  - Account screen with live rate limit
- [x] **App tracking** (`feat/app-tracking`, `feat/apps-ui`, #17, #18)
  - Room database for tracked apps, releases and install state
  - "Add app" sheet: URL / `owner/repo` parsing, repository preview, asset picker, package mapping
  - Apps list grouped by "Updates", "Up to date" and "Not installed"; app actions sheet
- [x] **APK installer** (`feat/apk-installer`)
  - Download with progress (public and private repos)
  - Verification: SHA-256 digest, package name, version code, signing certificate
  - Installation via `PackageInstaller` sessions
- [x] **Release pipeline** (`ci/release`, #15, #16)
  - Signed release APK attached to GitHub Releases on `v*` tags or a manual workflow run
  - APK Manager tracks its own repository to update itself
- [x] **App details** (`feat/app-details`)
  - Details screen with installed version, release history and Markdown changelog
  - Adaptive two-pane layout on large screens

## v0.2 — Background

- [x] Periodic background update checks (WorkManager) with configurable interval and constraints (`feat/background-checks`)
- [x] Update notifications (one per app, grouped)
- [x] Download progress notifications (`feat/download-notifications`)
- [x] Per-app asset filters (include/exclude regex), ABI ranking, pre-release toggle
- [x] Version detection improvements (release ID tracking, tag regex override, "mark as up to date")

## v0.3 — Automatic updates

- [ ] Unattended updates (`USER_ACTION_NOT_REQUIRED`) for apps installed through APK Manager
- [ ] Installer backends: Shizuku and root (Dhizuku later)
- [ ] Per-app update policy: notify only / download automatically / install automatically
- [ ] Global constraints: unmetered network only, while charging, battery not low
- [ ] Clear handling of Android developer verification failures

## v0.4 — Portability

- [ ] Import / export of tracked apps (JSON)
- [ ] Import from Obtainium exports
- [ ] Additional sources: Codeberg / Forgejo / Gitea, GitLab
- [ ] Split APK bundles (`.apks`, `.xapk`)

## v1.0 — Discover

- [ ] Community catalog: a separate public repository with app entries (repository, package name, signing certificate fingerprint, categories) contributed through pull requests
- [ ] Discover tab: browse the catalog and search GitHub by topic (`topic:android-app`)
- [ ] Translations (English, German first)
- [ ] Distribution through IzzyOnDroid and F-Droid
- [ ] Android developer verification registration for the APK Manager package

## Later

- GitHub App sign-in as a least-privilege alternative to the OAuth app
- F-Droid / IzzyOnDroid repositories as sources
- Home screen widget
- Multiple GitHub accounts
