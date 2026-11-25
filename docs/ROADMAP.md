# Roadmap

This roadmap is a living document. Each milestone maps to a GitHub milestone, and each item to one or more issues and pull requests.
Design and technical decisions are described in [ARCHITECTURE.md](ARCHITECTURE.md).

## v0.1 — Foundation

The first usable version: track GitHub apps and install updates manually.

- [ ] **Project setup** (`chore/project-setup`)
  - Gradle version catalog, `build-logic` convention plugins, module skeleton
  - Spotless (ktlint), Detekt, Android Lint
  - CI: build, unit tests and static analysis on every pull request
  - Dependabot for Gradle and GitHub Actions
- [ ] **Design system** (`feat/design-system`)
  - Theme: Material You dynamic color, custom seed color, light/dark/AMOLED black
  - Typography (rounded variable font), shapes and motion
  - Grouped rounded cards (24 dp outer / 4 dp inner corners), flexible top app bars, floating toolbar, bottom sheets
- [ ] **App shell** (`feat/app-shell`)
  - Navigation 3 with predictive back and adaptive two-pane layouts
  - Onboarding: install-unknown-apps and notification permissions
  - Screen skeletons: Apps, Settings
- [ ] **GitHub client** (`feat/github-client`)
  - REST and GraphQL clients, ETag caching, rate limit tracking
  - Release and asset models, tests against MockWebServer
- [ ] **GitHub sign-in** (`feat/github-auth`)
  - OAuth device flow (public access, optional private repo access)
  - Fine-grained personal access token as an alternative
  - Token encrypted with an Android Keystore key, excluded from backups
  - Account screen with live rate limit
- [ ] **App tracking** (`feat/app-tracking`)
  - Room database for tracked apps, releases and install state
  - "Add app" sheet: URL / `owner/repo` parsing, repository preview, asset picker, package mapping
  - Apps list grouped by "Updates", "Up to date" and "Not installed"; app details with Markdown changelog
- [ ] **APK installer** (`feat/apk-installer`)
  - Download with progress (public and private repos)
  - Verification: SHA-256 digest, package name, version code, signing certificate
  - Installation via `PackageInstaller` sessions
- [ ] **Release pipeline** (`ci/release`)
  - Signed release APK attached to GitHub Releases on `v*` tags
  - APK Manager can track its own repository to update itself

## v0.2 — Background

- [ ] Periodic background update checks (WorkManager) with configurable interval and constraints
- [ ] Update notifications (one per app, grouped), download progress notifications
- [ ] Per-app asset filters (include/exclude regex), ABI ranking, pre-release toggle
- [ ] Version detection improvements (release ID tracking, tag regex override, "mark as up to date")

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
