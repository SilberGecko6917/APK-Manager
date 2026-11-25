# Releasing

Releases are built and published by the [Release workflow](../.github/workflows/release.yml) when a version tag is pushed.

## One-time setup

### 1. Create the release keystore

The release key signs every APK that users install. **If it is lost, existing installations can no longer be updated**; if it leaks, others can publish updates in your name. Create it once, on a trusted machine, and back up the keystore file and its passwords (for example in a password manager).

```bash
keytool -genkeypair -v \
  -keystore apk-manager-release.jks \
  -alias apk-manager \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -dname "CN=APK Manager"
```

### 2. Add the repository secrets

In **Settings → Secrets and variables → Actions → New repository secret**:

| Secret | Value |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | The keystore, Base64-encoded: `base64 -w0 apk-manager-release.jks` (macOS: `base64 -i apk-manager-release.jks`) |
| `RELEASE_KEYSTORE_PASSWORD` | The keystore password |
| `RELEASE_KEY_ALIAS` | `apk-manager` (or the alias you chose) |
| `RELEASE_KEY_PASSWORD` | The key password (the same as the keystore password if you were not asked separately) |
| `DEBUG_KEYSTORE_BASE64` | Optional: a shared debug keystore, so debug APKs from different CI runs can be installed over each other (see below) |

### Optional: shared debug keystore

Without it, every CI run signs debug APKs with a new random key, and a newer debug APK cannot be installed over an older one. The debug key is not used for releases.

```bash
keytool -genkeypair -keystore debug.keystore \
  -storepass android -alias androiddebugkey -keypass android \
  -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
base64 -w0 debug.keystore   # value for DEBUG_KEYSTORE_BASE64
```

## Publishing a release

1. Make sure `main` is green and the roadmap/changelog are up to date.
2. Tag the commit and push the tag:

   ```bash
   git tag v0.1.0
   git push origin v0.1.0
   ```

3. The workflow builds the signed APK and creates a GitHub release with:
   - `APK-Manager-<version>.apk` and its `.sha256` checksum
   - the APK checksum and the **signing certificate SHA-256 fingerprint** in the release notes
   - release notes generated from the merged pull requests

Pre-release tags (`v0.1.0-beta.1`, `v0.1.0-rc.1`) are published as pre-releases.

## Versioning

The version name and code come from the tag, not from the build files:

| Tag | `versionName` | `versionCode` |
|---|---|---|
| `v0.1.0-beta.1` | `0.1.0-beta.1` | 10001 |
| `v0.1.0-rc.2` | `0.1.0-rc.2` | 10002 |
| `v0.1.0` | `0.1.0` | 10099 |
| `v1.2.3` | `1.2.3` | 1020399 |

`versionCode = major × 1,000,000 + minor × 10,000 + patch × 100 + n`, where `n` is the pre-release number (1–98) or 99 for the final release. Minor and patch must be ≤ 99. Pre-release numbers are shared between `alpha`, `beta` and `rc`, so keep counting up (`beta.1`, `beta.2`, `rc.3`, …). See [`scripts/release-version.sh`](../scripts/release-version.sh).

## Local signed builds

The signing configuration reads environment variables, so nothing secret is stored in the repository:

```bash
export APKMANAGER_KEYSTORE_FILE=/path/to/apk-manager-release.jks
export APKMANAGER_KEYSTORE_PASSWORD=…
export APKMANAGER_KEY_ALIAS=apk-manager
export APKMANAGER_KEY_PASSWORD=…
./gradlew assembleRelease -Papkmanager.versionName=0.1.0 -Papkmanager.versionCode=10099
```

Without these variables, `assembleRelease` produces an unsigned APK.

## Android developer verification

Google is rolling out developer verification for apps installed outside Google Play (globally from 2027). Register the package name `me.geckotv.apkmanager` together with the release certificate fingerprint in the Android Developer Console before then. A stable, registered key also reduces Play Protect warnings over time.
