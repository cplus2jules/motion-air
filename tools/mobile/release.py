#!/usr/bin/env python3
"""Validate and assemble one GitHub release for both native phone apps."""
import argparse
import base64
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import ssl
import subprocess


def version(value):
    if not re.fullmatch(r"(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)", value):
        raise ValueError("Use a version such as 0.2.0, without a prefix or prerelease suffix.")
    major, minor, patch = map(int, value.split('.'))
    if not (0 <= major <= 2099 and 0 <= minor <= 999 and 0 <= patch <= 999):
        raise ValueError("Version exceeds Android versionCode limits.")
    if major * 1_000_000 + minor * 1000 + patch == 0:
        raise ValueError("Version 0.0.0 is not releasable.")
    return value


def metadata(ref, requested):
    if ref.startswith('refs/tags/'):
        if not ref.startswith('refs/tags/mobile/v'):
            raise ValueError("Shared phone releases use mobile/vMAJOR.MINOR.PATCH tags.")
        resolved = version(ref.removeprefix('refs/tags/mobile/v'))
        if requested and requested != resolved:
            raise ValueError("The requested version does not match the tag.")
        return resolved
    return version(requested)


def restore_key(output):
    names = ['ANDROID_KEYSTORE_BASE64', 'ANDROID_KEYSTORE_PASSWORD', 'ANDROID_KEY_ALIAS', 'ANDROID_KEY_PASSWORD']
    if any(not os.environ.get(name) for name in names):
        raise ValueError("Android signing secrets are missing. Follow docs/mobile-releases.md; debug keys are never used for releases.")
    key = base64.b64decode(os.environ['ANDROID_KEYSTORE_BASE64'], validate=True)
    if not key:
        raise ValueError("The Android keystore is empty.")
    with os.fdopen(os.open(output, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600), 'wb') as file:
        file.write(key)


def verify_apk(apk, certificate, apksigner):
    report = subprocess.check_output([apksigner, 'verify', '--verbose', '--print-certs', str(apk)], text=True)
    digests = re.findall(r'^Signer #\d+ certificate SHA-256 digest: ([a-fA-F0-9]+)$', report, re.MULTILINE)
    expected = hashlib.sha256(ssl.PEM_cert_to_DER_cert(certificate.read_text())).hexdigest()
    if [digest.lower() for digest in digests] != [expected]:
        raise ValueError("APK signing identity does not match the repository's public release certificate.")
    print(f'Android release signature verified: SHA-256 {expected}')


def verify_checksums(directory):
    verified = set()
    for line in (directory / 'SHA256SUMS.txt').read_text().splitlines():
        digest, name = line.split('  ', 1)
        if not re.fullmatch(r'[a-f0-9]{64}', digest) or Path(name).name != name or name in verified:
            raise ValueError("Invalid checksum manifest.")
        if hashlib.sha256((directory / name).read_bytes()).hexdigest() != digest:
            raise ValueError(f"Checksum mismatch: {name}")
        verified.add(name)
    return verified


def verify_inputs(iphone, android, release_version, commit):
    version(release_version)
    verified = verify_checksums(iphone)
    if 'build-info.json' not in verified:
        raise ValueError("iPhone metadata is missing its checksum.")
    info = json.loads((iphone / 'build-info.json').read_text())
    if (info.get('version'), info.get('commit'), info.get('platform'), info.get('signing')) != (
            release_version, commit, 'iPhoneOS', 'unsigned-requires-sideload-signing'):
        raise ValueError("iPhone build does not match this release source/version/platform.")
    ipa = list(iphone.glob('*.ipa'))
    if len(ipa) != 1 or ipa[0].name not in verified:
        raise ValueError("Expected exactly one verified iPhone IPA.")
    android_info = json.loads((android / 'android-build-info.json').read_text())
    major, minor, patch = map(int, release_version.split('.'))
    expected_code = major * 1_000_000 + minor * 1000 + patch
    elements = android_info.get('elements', [])
    if (android_info.get('applicationId') != 'com.motionair.controller' or
            android_info.get('variantName') != 'release' or len(elements) != 1 or
            elements[0].get('versionName') != release_version or
            elements[0].get('versionCode') != expected_code or elements[0].get('filters')):
        raise ValueError("Android artifact is not the matching universal release build.")
    apk = android / f'MotionAir-{release_version}-android.apk'
    if not apk.is_file() or not apk.stat().st_size:
        raise ValueError("The Android APK is missing or empty.")
    return info


def bundle(iphone, android, output, release_version):
    commit = subprocess.check_output(['git', 'rev-parse', 'HEAD'], text=True).strip()
    verify_inputs(iphone, android, release_version, commit)
    output.mkdir(parents=True, exist_ok=False)
    for source in iphone.iterdir():
        if source.name not in {'SHA256SUMS.txt', 'RELEASE_NOTES.md'} and source.is_file():
            name = 'iphone-build-info.json' if source.name == 'build-info.json' else source.name
            shutil.copyfile(source, output / name)
    for source in android.iterdir():
        if source.is_file():
            shutil.copyfile(source, output / source.name)
    computer_zip = output / f'MotionAir-{release_version}-computer.zip'
    subprocess.run(['git', 'archive', '--format=zip', f'--prefix=MotionAir-{release_version}/',
                    '-o', str(computer_zip), commit], check=True)
    (output / 'RELEASE_NOTES.md').write_text(f"""## Download Motion Air {release_version}

Use your Android phone or iPhone as a wireless controller on a Mac or Windows PC.

1. Download **MotionAir-{release_version}-computer.zip** on your computer and extract it.
2. Download **MotionAir-{release_version}-android.apk** for Android 8 or later, or the
   **unsigned.ipa** file for iPhone with iOS 17 or later.
3. Follow the [easy setup guide](https://github.com/cplus2jules/motion-air/blob/mobile/v{release_version}/README.md).

Android: open the APK on your phone to install it. Updates use the same release
signing key. Development builds have a separate app ID and do not replace it.

iPhone: import the IPA into your sideloading tool and sign it with your Apple
account. Opening the unsigned file on an iPhone does not install it.

Both phones need the computer launcher and a compatible emulator. Emulator
binaries, games, firmware and keys are not included. Just Dance motion requires
the local motion-capable Ryujinx build described in the setup guides.

Both apps reject old sensor frames. Android also preserves rapid taps
and keeps tracking through a visible pause, then stops on background.

Automated checks cover both app builds, Swift/Kotlin regressions, Android emulator
pairing and motion delivery, and the desktop bridge on macOS and Windows. Physical
phone motion and in-game scores still need real-device testing.

Source commit: `{commit}`. `SHA256SUMS.txt` verifies every download. The dSYM zip
and build-info files are for debugging; you do not need them to play.

Based on [Joypad Air by David García](https://github.com/mindavidev/joypad-air).
Motion Air retains the original MIT license and acknowledgements.
""")
    lines = [f"{hashlib.sha256(path.read_bytes()).hexdigest()}  {path.name}\n"
             for path in sorted(output.iterdir()) if path.is_file()]
    (output / 'SHA256SUMS.txt').write_text(''.join(lines))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest='command', required=True)
    meta = commands.add_parser('metadata')
    meta.add_argument('--ref', default='')
    meta.add_argument('--version', default='')
    key = commands.add_parser('restore-key')
    key.add_argument('--output', required=True)
    apk = commands.add_parser('verify-apk')
    apk.add_argument('--apk', type=Path, required=True)
    apk.add_argument('--certificate', type=Path, required=True)
    apk.add_argument('--apksigner', required=True)
    pack = commands.add_parser('bundle')
    pack.add_argument('--version', required=True)
    pack.add_argument('--iphone', type=Path, required=True)
    pack.add_argument('--android', type=Path, required=True)
    pack.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    try:
        if args.command == 'metadata':
            resolved = metadata(args.ref, args.version)
            print(f'version={resolved}')
            if os.environ.get('GITHUB_OUTPUT'):
                with open(os.environ['GITHUB_OUTPUT'], 'a') as output:
                    output.write(f'version={resolved}\n')
        elif args.command == 'restore-key':
            restore_key(args.output)
        elif args.command == 'verify-apk':
            verify_apk(args.apk, args.certificate, args.apksigner)
        else:
            bundle(args.iphone, args.android, args.output, args.version)
    except (ValueError, OSError) as error:
        parser.exit(1, f'{error}\n')


if __name__ == '__main__':
    main()
