# Contributing to APK Manager

## Welcome

Thank you for your interest in contributing to APK Manager! Contributions of all kinds are
welcome: bug reports, feature ideas, documentation, translations and code.

## Ways to contribute

- **Report bugs** using the bug report issue form.
- **Suggest features** using the feature request issue form.
- **Improve documentation** or fix typos.
- **Submit code** via pull requests. For larger changes, please open an issue first to discuss the approach.
- **Review pull requests** and help test changes on different devices and Android versions.

## Development setup

Requirements:

- JDK 21
- Android SDK (install via Android Studio or the command-line tools)

Steps:

```bash
git clone https://github.com/SilberGecko6917/APK-Manager.git
cd APK-Manager
./gradlew assembleDebug
```

## Branching and commits

- `main` is protected. All changes go through pull requests.
- Create a branch per change using one of these prefixes:
  `feat/`, `fix/`, `chore/`, `docs/`, `refactor/`, `test/`, `ci/`.
  Example: `feat/installer-signature-check`.
- Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/).

Common commit types:

| Type       | Use for                                  | Example                                             |
| ---------- | ---------------------------------------- | --------------------------------------------------- |
| `feat`     | New feature                              | `feat(installer): verify APK signing certificate`   |
| `fix`      | Bug fix                                  | `fix(sync): handle rate-limited GitHub API responses` |
| `docs`     | Documentation only                       | `docs(readme): describe Shizuku setup`              |
| `style`    | Formatting, no behavior change           | `style: apply Spotless formatting`                  |
| `refactor` | Code change without behavior change      | `refactor(repo): extract release parser`            |
| `perf`     | Performance improvement                  | `perf(list): reduce recomposition in app list`      |
| `test`     | Adding or fixing tests                   | `test(updater): cover prerelease selection`         |
| `build`    | Build system or dependencies             | `build: update Compose BOM`                         |
| `ci`       | CI configuration                         | `ci: cache Gradle dependencies`                     |
| `chore`    | Maintenance that fits no other type      | `chore: remove unused resources`                    |

## Pull requests

- Keep PRs small and focused on one change.
- Use a Conventional Commit style PR title.
- Fill in the pull request template.
- Make sure CI passes before requesting review.
- PRs are squash-merged, so the PR title becomes the commit message on `main`.

## Code style

- Formatting is enforced with Spotless (ktlint). Static analysis uses Detekt.
- Before opening a PR, run:

  ```bash
  ./gradlew spotlessApply detekt lint test
  ```

- Follow the Jetpack Compose guidelines:
  - Keep composables stateless where possible and hoist state to the caller.
  - Provide `@Preview` functions for new UI components.
  - Use Material 3 components and tokens consistently.

## Translations

Translation support is planned for a later stage. Details will follow.

## Reporting bugs and requesting features

Please use the issue forms so that we get the information we need:

- [Bug report](../../issues/new?template=bug_report.yml)
- [Feature request](../../issues/new?template=feature_request.yml)

Search existing issues before opening a new one.

## Security

Do not report security vulnerabilities in public issues. Follow the process described in
[SECURITY.md](SECURITY.md) and use GitHub's private vulnerability reporting.

## License

APK Manager is licensed under the GNU General Public License v3.0 or later (GPL-3.0-or-later).
By contributing, you agree that your contributions are licensed under the same terms.
