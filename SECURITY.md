# Security Policy

## Supported versions

Only the latest release of APK Manager receives security fixes.

| Version        | Supported |
| -------------- | --------- |
| Latest release | Yes       |
| Older releases | No        |

## Reporting a vulnerability

Please do not report security vulnerabilities through public GitHub issues.

Report them privately via GitHub's private vulnerability reporting:

1. Go to the repository's **Security** tab.
2. Click **Report a vulnerability**.

Direct link: https://github.com/SilberGecko6917/APK-Manager/security/advisories/new

## What to include

- A description of the vulnerability and its potential impact
- Steps to reproduce, or a proof of concept
- Affected app version and Android version
- Installer backend involved (system installer, Shizuku or root), if relevant
- Any suggested mitigation, if you have one

## Response expectations

This is a community project maintained on a best-effort basis. We aim to acknowledge
reports within about 7 days and will keep you informed about progress and the fix timeline.
Please allow reasonable time for a fix before any public disclosure.

## Scope

In scope, for example:

- Storage and handling of GitHub authentication tokens
- APK download, verification and signing certificate checks
- Installer backends (system installer, Shizuku, root) and the permissions they use
- Handling of data from tracked GitHub repositories and releases

Out of scope: vulnerabilities in third-party services or dependencies that are not
exploitable through APK Manager, and issues that require an already-compromised device.
