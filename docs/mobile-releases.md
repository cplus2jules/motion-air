# Publish Android and iPhone together

The [Android and iPhone releases workflow](../.github/workflows/mobile-releases.yml) publishes one GitHub Release containing:

- **MotionAir-Android.apk**, a signed Android APK that users can install and update.
- **MotionAir-iPhone-unsigned.ipa**, an unsigned iPhone IPA for users to sign with their sideloading tool.
- **MotionAir-Computer.zip**, a computer ZIP with the Mac and Windows launchers and bridge source.
- Build metadata, iPhone crash symbols, release notes and SHA-256 checksums.

The GitHub repository is private. Only people with repository access can download these builds. Publishing a release does not change that visibility.

## What runs

| Action | Result |
| --- | --- |
| Push relevant changes to `main` or open a pull request | Desktop checks on Windows and macOS; Android unit tests, lint, debug APK and emulator integration; iPhone core tests and device archive when native files change |
| Actions → Android and iPhone releases → Run workflow | Tests and complete release artifacts, without publishing a GitHub Release. Select `main` and enter a version. |
| Push `mobile/vMAJOR.MINOR.PATCH` from `main` | All checks and builds, then a GitHub Release with both apps |
| Push `ios/vMAJOR.MINOR.PATCH` | The existing independent iPhone-only release workflow |

The Android emulator test pairs through the pinned HTTPS/WebSocket bridge, receives sensor frames over UDP, checks monotonic timestamps, and exercises visible pause, background and reconnection. The fixture never injects real keyboard events. It does not test a physical phone's sensor accuracy or in-game scoring.

The publisher runs only after both app builds and the desktop checks pass. A draft release holds the uploads until the upload step succeeds. Existing releases are never overwritten automatically. Only publishing jobs receive repository write permission; ordinary builds use read access.

## Android signing setup

Android updates must retain the same release signing key. Keep a private backup of the keystore and passwords outside the repository. Losing the key prevents normal updates to installations signed with it.

Under **Settings → Secrets and variables → Actions**, configure these repository secrets:

| Secret | Value |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | The release keystore encoded as base64, on one line |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password |
| `ANDROID_KEY_ALIAS` | Alias of the release key inside the keystore |
| `ANDROID_KEY_PASSWORD` | Password for that key |

The workflow restores the keystore to the runner's temporary folder, signs the APK, verifies it against [the public release certificate](../android/release-signing-certificate.pem), and deletes the temporary keystore. It fails if any secret is missing. It never falls back to a debug key.

Export the public certificate when setting up the key with `keytool -exportcert -rfc`, and store only that public certificate in `android/release-signing-certificate.pem`. The certificate allows CI to reject an accidentally replaced signing identity. Do not rotate the key to fix an ordinary build failure.

Debug builds use `com.motionair.controller.debug`, so they can coexist with releases and cannot replace them. Release builds use `com.motionair.controller`.

The iPhone IPA is unsigned. This pipeline does not require Apple signing secrets and does not publish to the App Store, TestFlight or Google Play.

## Publish a new version

After the changes are tested and merged into `main`:

```bash
git switch main
git pull --ff-only origin main
git tag mobile/v0.2.0
git push origin mobile/v0.2.0
```

Use a new, higher version instead of the example if it already exists. Both apps use the tag's version. A release tag must point to a commit already on `main`.

Versions have three integer parts. Android's version code is `major × 1,000,000 + minor × 1,000 + patch`, so each part must stay in its documented range: major 0 to 2099, minor and patch 0 to 999. Version 0.0.0 and prerelease suffixes are rejected. iPhone build numbers use the workflow run number and attempt; see [iPhone build-number limits](ios-releases.md#publish-a-version).

Wait for **Android and iPhone releases** to finish in Actions, then open Releases and check the APK, IPA and computer ZIP. A successful local build alone does not establish that GitHub published anything.

## Local release verification

```bash
python3 -m unittest discover -s tools/mobile -p 'test_*.py' -v
python3 -m unittest discover -s tools/ios -p 'test_*.py' -v
```

For Android, use JDK 17 and Android SDK 35. Set these environment variables through your local secret manager or private shell environment:

- `MOTION_AIR_VERSION`, for example `0.2.0`.
- `ANDROID_KEYSTORE_FILE`, the absolute keystore path.
- `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` and `ANDROID_KEY_PASSWORD`.

Then run:

```bash
cd android
./gradlew assembleRelease testReleaseUnitTest lintRelease
python3 ../tools/mobile/release.py verify-apk --apk=app/build/outputs/apk/release/app-release.apk --certificate=release-signing-certificate.pem --apksigner="$ANDROID_HOME/build-tools/35.0.0/apksigner"
```

See [iPhone local verification](ios-releases.md#local-verification) for its device archive. The final bundle checks the iPhone checksums and commit, Android release identity, version and version code before packaging a computer ZIP from that same Git commit.

If a publish step fails after creating a draft, inspect the draft and its Actions log before retrying. The workflow refuses to replace an existing release, including a draft. Publish a new version after resolving the problem.

References: [Android signing and update identity](https://developer.android.com/studio/publish/app-signing), [GitHub encrypted secrets](https://docs.github.com/en/actions/security-for-github-actions/security-guides/using-secrets-in-github-actions), [GitHub release creation](https://cli.github.com/manual/gh_release_create).
