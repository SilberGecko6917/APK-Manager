#!/usr/bin/env bash
# Derives the app version from a release tag.
#
#   scripts/release-version.sh v1.2.3        -> name=1.2.3        code=1020399 prerelease=false
#   scripts/release-version.sh v1.2.3-beta.2 -> name=1.2.3-beta.2 code=1020302 prerelease=true
#
# versionCode = major * 1,000,000 + minor * 10,000 + patch * 100 + build, where build is the
# pre-release number (1-98) or 99 for the final release. Pre-releases therefore always sort before
# the final release of the same version, and every later version gets a higher code.
set -euo pipefail

tag="${1:?usage: release-version.sh <tag>}"
version="${tag#v}"
pattern='^([0-9]+)\.([0-9]+)\.([0-9]+)(-(alpha|beta|rc)\.?([0-9]+))?$'

if [[ ! "$version" =~ $pattern ]]; then
    echo "Tag '$tag' is not a version like v1.2.3 or v1.2.3-beta.1" >&2
    exit 1
fi

major="${BASH_REMATCH[1]}"
minor="${BASH_REMATCH[2]}"
patch="${BASH_REMATCH[3]}"
if [[ -n "${BASH_REMATCH[4]}" ]]; then
    build="${BASH_REMATCH[6]}"
    prerelease=true
else
    build=99
    prerelease=false
fi

if ((minor > 99 || patch > 99 || build < 1 || build > 99)); then
    echo "Tag '$tag' is out of range (minor and patch <= 99, pre-release number 1-98)" >&2
    exit 1
fi
if [[ "$prerelease" == true ]] && ((build > 98)); then
    echo "Tag '$tag': the pre-release number must be 98 or lower" >&2
    exit 1
fi

echo "name=$version"
echo "code=$((major * 1000000 + minor * 10000 + patch * 100 + build))"
echo "prerelease=$prerelease"
