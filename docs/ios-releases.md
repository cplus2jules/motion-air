# iPhone builds and releases

For a release containing **both Android and iPhone downloads**, use the [shared mobile release guide](mobile-releases.md). The iPhone workflow below remains available for independent iPhone builds and `ios/v` tags.

The [iPhone CI and Releases workflow](../.github/workflows/ios.yml) builds the
native Swift **MotionAir** app as an **unsigned IPA for sideloading**. It uses
GitHub-hosted macOS 26 with Xcode 26.6. No Apple signing certificate, provisioning
profile, App Store Connect key, or paid developer membership is needed to build it.

## What runs

| Trigger | Result |
| --- | --- |
| Push to `main`, or pull request into `main`, affecting native code, icons, or release tooling | Release-tool tests, JoypadCore tests, a Release archive for a real iPhone, and downloadable artifacts |
| Actions → iPhone CI and Releases → Run workflow | Same checks and IPA; an optional version overrides the Xcode default for this build only |
| Push `ios/vMAJOR.MINOR.PATCH` | Same checks, followed by a GitHub Release with the tested IPA, dSYMs, metadata, release notes, and SHA-256 checksums |

Tag builds must point to a commit already merged into `main`. Invalid version
tags fail before the Xcode build. Only the publishing job receives permission
to write a GitHub Release; pull requests and ordinary builds have read access.
The workflow uses commit-pinned official GitHub Actions and the built-in
`GITHUB_TOKEN`. No repository secrets need to be added.

## Download a build

Open **Actions → iPhone CI and Releases**, select a successful run, and download
its `MotionAir-<version>-build-<number>-unsigned` artifact. Unzip that download
to find the `.ipa`. CI artifacts are retained for 30 days. Versioned release
assets are available from **Releases**. Sign in to GitHub to download from this
private repository.

The IPA contains the `arm64` iPhone app, not a Simulator build. Import it into
your chosen signing/sideloading tool, such as [AltStore Classic](https://faq.altstore.io/)
or [Sideloadly](https://sideloadly.io/), and follow that tool's installation steps.
The tool must sign the IPA with your Apple account. Opening the unsigned IPA
directly on an iPhone will not install it. This workflow does not upload to
TestFlight or the App Store.

Follow the phone setup and installation troubleshooting in the
[English guide](../README.md#iphone) or [Spanish guide](../README.es.md#iphone).
These cover Developer Mode, trusting the signing account, free-account renewal,
and the direct Xcode installation fallback when Sideloadly fails.

Keep the same signing account and bundle identity for updates where possible.
A sideloading tool may rewrite the bundle ID and create a separate installation,
so saved Mac pairing is not guaranteed across different signing methods. Refresh
and expiry depend on your signing method. The existing Mac bridge and local
Ryujinx setup are still required; see [iPhone setup](local-device-setup.md).

## Publish a version

After the desired app changes are merged into `main`:

```bash
git switch main
git pull --ff-only origin main
git tag ios/v0.2.0
git push origin ios/v0.2.0
```

Replace `0.2.0` with the version you want to release. The tag sets the app's
`CFBundleShortVersionString`; the workflow run number and attempt set
`CFBundleVersion`, for example `12.1`. Both are checked in the archived app
before packaging. iPhone tags use `ios/v` so they are independent of Node/npm
package versions. Ordinary builds use `MARKETING_VERSION` from the Xcode project.

Versions use three integers, such as `0.2.0` or `1.4.2`. Prerelease suffixes are
not supported. Existing releases are never overwritten automatically: publish
a new tag for an updated app. If a network failure leaves a draft release,
inspect it before retrying; the workflow stops when a release already exists.
The current build-number scheme supports 9,999 runs and 99 attempts per run;
update it before reaching that limit, retaining increasing Apple build numbers.

## Local verification

```bash
python3 -m unittest discover -s tools/ios -p 'test_*.py' -v
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer \
VERSION=0.2.0 BUILD_NUMBER=1.1 \
MOTION_AIR_OUTPUT_DIR="$(mktemp -d /tmp/motion-air-release.XXXXXX)" \
bash tools/ios/build-release.sh
```

The script prints its output directory. `dist/` contains the IPA, dSYM archive,
`build-info.json`, `RELEASE_NOTES.md`, and `SHA256SUMS.txt`. `logs/` contains Swift
test and archive logs. It refuses to reuse an existing `dist/` directory so a
failed build cannot accidentally publish stale files. Temporary archive and
build-cache directories are removed on exit.

These checks verify buildability, version metadata, packaging, and shared-core
behavior. Test the installed app on a physical iPhone for sensors, haptics,
reconnection, and in-game scoring before describing a version as gameplay-tested.

## References

- [GitHub macOS 26 runner software](https://github.com/actions/runner-images/blob/main/images/macos/macos-26-Readme.md)
- [GitHub workflow artifacts](https://docs.github.com/en/actions/concepts/workflows-and-actions/workflow-artifacts)
- [GitHub CLI release creation](https://cli.github.com/manual/gh_release_create)
- [Apple app version number](https://developer.apple.com/documentation/bundleresources/information-property-list/cfbundleshortversionstring)
- [Apple build number](https://developer.apple.com/documentation/bundleresources/information-property-list/cfbundleversion)
