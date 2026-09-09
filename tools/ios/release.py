#!/usr/bin/env python3
"""Version, verify, and describe unsigned Motion Air iPhone releases."""

import argparse
import hashlib
import json
import os
from pathlib import Path
import plistlib
import re
import sys

BUNDLE_ID = "com.juliansalas.joypadair.probe"
VERSION_PATTERN = r"(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)"


def version(value):
    if not re.fullmatch(VERSION_PATTERN, value):
        raise ValueError("Version must be three integers, such as 0.2.0, without a prefix or prerelease suffix.")
    return value


def build_number(value):
    # Apple's CFBundleVersion limits: four digits, then up to two and two.
    if not re.fullmatch(r"[1-9][0-9]{0,3}(?:\.(?:0|[1-9][0-9]?)){0,2}", value):
        raise ValueError("Build number must be an Apple-compatible positive number (for example 12.1).")
    return value


def metadata(project, ref="", requested="", run_number="1", attempt="1"):
    configured = set(re.findall(r"MARKETING_VERSION\s*=\s*([^;]+);", Path(project).read_text()))
    if len(configured) != 1:
        raise ValueError("All Xcode configurations must declare the same MARKETING_VERSION.")
    configured_version = version(configured.pop().strip().strip('"'))
    if ref.startswith("refs/tags/"):
        tag = ref.removeprefix("refs/tags/")
        if not tag.startswith("ios/v"):
            raise ValueError("iPhone release tags must use ios/vMAJOR.MINOR.PATCH.")
        resolved = version(tag.removeprefix("ios/v"))
        if requested and requested != resolved:
            raise ValueError("Requested version does not match the release tag.")
    else:
        resolved = version(requested) if requested else configured_version
    if not re.fullmatch(r"[1-9][0-9]{0,3}", run_number) or not re.fullmatch(r"[1-9][0-9]?", attempt):
        raise ValueError("Run number must be 1–9999 and attempt 1–99 for CFBundleVersion.")
    build = build_number(f"{run_number}.{attempt}")
    return {"version": resolved, "build_number": build, "artifact_name": f"MotionAir-{resolved}-build-{build}-unsigned"}


def verify_app(app, expected_version, expected_build):
    version(expected_version)
    build_number(expected_build)
    app = Path(app)
    with (app / "Info.plist").open("rb") as source:
        info = plistlib.load(source)
    expected = {
        "CFBundleIdentifier": BUNDLE_ID,
        "CFBundleDisplayName": "Motion Air",
        "CFBundleShortVersionString": expected_version,
        "CFBundleVersion": expected_build,
        "CFBundleSupportedPlatforms": ["iPhoneOS"],
    }
    for key, value in expected.items():
        if info.get(key) != value:
            raise ValueError(f"Archived app has unexpected {key}: expected {value!r}.")
    executable = info.get("CFBundleExecutable", "")
    if not executable or Path(executable).name != executable or not (app / executable).is_file():
        raise ValueError("Archived app is missing its executable.")
    if not os.access(app / executable, os.X_OK):
        raise ValueError("Archived app binary is not executable.")
    if (app / "embedded.mobileprovision").exists() or (app / "_CodeSignature").exists():
        raise ValueError("Sideload release unexpectedly contains an existing signing identity/profile.")
    return info


def describe_release(directory, release_version, build, commit, xcode):
    version(release_version)
    build_number(build)
    if not re.fullmatch(r"[0-9a-f]{40}", commit):
        raise ValueError("Release source must be a full Git commit SHA.")
    directory = Path(directory)
    stem = f"MotionAir-{release_version}-build-{build}"
    assets = [directory / f"{stem}-unsigned.ipa", directory / f"{stem}.dSYMs.zip"]
    if any(not p.is_file() or not p.stat().st_size for p in assets):
        raise ValueError("Both the IPA and debug-symbol archive must exist and be nonempty.")
    info = {
        "product": "Motion Air", "version": release_version, "build_number": build,
        "bundle_id": BUNDLE_ID, "platform": "iPhoneOS", "architecture": "arm64",
        "signing": "unsigned-requires-sideload-signing", "commit": commit,
        "xcode": xcode.strip(), "minimum_ios": "17.0",
    }
    manifest = directory / "build-info.json"
    manifest.write_text(json.dumps(info, indent=2) + "\n")
    assets.append(manifest)
    notes = directory / "RELEASE_NOTES.md"
    notes.write_text(f"""## Motion Air {release_version}

iPhone build **{build}**, from commit `{commit}`. Requires iOS 17 or later.

Download **{stem}-unsigned.ipa** and import it into your preferred sideloading
tool, such as AltStore Classic or Sideloadly. The tool must sign it with your
own Apple account before installation; the IPA cannot be installed by opening
it directly on an iPhone. This is not a TestFlight or App Store build.

Use the same Apple account and bundle identity for updates when possible.
Your sideloading tool may change the bundle ID, which can create a separate app
and require pairing your Mac again. Certificate expiry and refresh behavior
depend on your signing method.

The Mac pairing bridge is still required. See the repository's iPhone setup
guide in `docs/local-device-setup.md`. Automated tests cover the app build and
shared controller core; they do not establish physical motion or game scoring.

`SHA256SUMS.txt` verifies the IPA, debug symbols, build metadata, and these notes.
The dSYM archive is for investigating crashes and is not needed to play.

Built with Joypad Air in mind, and based on
[Joypad Air by David García (mindavidev)](https://github.com/mindavidev/joypad-air).
Motion Air retains the original MIT license and acknowledgements.
""")
    assets.append(notes)
    checksums = []
    for asset in assets:
        digest = hashlib.sha256(asset.read_bytes()).hexdigest()
        checksums.append(f"{digest}  {asset.name}\n")
    (directory / "SHA256SUMS.txt").write_text("".join(checksums))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    meta = commands.add_parser("metadata")
    meta.add_argument("--project", required=True)
    meta.add_argument("--ref", default="")
    meta.add_argument("--version", default="")
    meta.add_argument("--run-number", default="1")
    meta.add_argument("--attempt", default="1")
    verify = commands.add_parser("verify-app")
    verify.add_argument("--app", required=True)
    finish = commands.add_parser("describe")
    finish.add_argument("--directory", required=True)
    finish.add_argument("--commit", required=True)
    finish.add_argument("--xcode", required=True)
    for command in [verify, finish]:
        command.add_argument("--version", required=True)
        command.add_argument("--build", required=True)
    args = parser.parse_args()
    try:
        if args.command == "metadata":
            values = metadata(args.project, args.ref, args.version, args.run_number, args.attempt)
            print(json.dumps(values))
            if os.environ.get("GITHUB_OUTPUT"):
                with open(os.environ["GITHUB_OUTPUT"], "a") as output:
                    output.writelines(f"{key}={value}\n" for key, value in values.items())
        elif args.command == "verify-app":
            verify_app(args.app, args.version, args.build)
            print("Verified unsigned iPhone app identity, version, build, and executable.")
        else:
            describe_release(args.directory, args.version, args.build, args.commit, args.xcode)
    except (ValueError, OSError, plistlib.InvalidFileException) as error:
        parser.exit(1, f"Release validation failed: {error}\n")


if __name__ == "__main__":
    main()
