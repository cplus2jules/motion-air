import hashlib
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
from release import metadata, restore_key, verify_apk, verify_inputs, version


class MobileReleaseTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.iphone = self.root / 'iphone'; self.iphone.mkdir()
        self.android = self.root / 'android'; self.android.mkdir()
        (self.iphone / 'phone.ipa').write_bytes(b'fixture')
        (self.iphone / 'build-info.json').write_text(json.dumps({
            'version': '0.2.0', 'commit': 'a' * 40, 'platform': 'iPhoneOS',
            'signing': 'unsigned-requires-sideload-signing',
        }))
        (self.iphone / 'SHA256SUMS.txt').write_text(''.join(
            f'{hashlib.sha256(path.read_bytes()).hexdigest()}  {path.name}\n'
            for path in sorted(self.iphone.iterdir())))
        (self.android / 'MotionAir-0.2.0-android.apk').write_bytes(b'fixture')
        self.android_info = {'applicationId': 'com.motionair.controller', 'variantName': 'release',
                             'elements': [{'versionName': '0.2.0', 'versionCode': 2000, 'filters': []}]}
        self.write_android()

    def write_android(self):
        (self.android / 'android-build-info.json').write_text(json.dumps(self.android_info))

    def test_tag_and_manual_version(self):
        self.assertEqual(metadata('refs/tags/mobile/v0.2.0', ''), '0.2.0')
        self.assertEqual(metadata('refs/heads/main', '1.2.3'), '1.2.3')
        for ref, requested in [('refs/tags/ios/v0.2.0', ''), ('refs/tags/mobile/v0.2.0', '0.3.0')]:
            with self.assertRaises(ValueError): metadata(ref, requested)

    def test_invalid_versions_fail_before_build_or_path_creation(self):
        for value in ['', '0.0.0', '01.2.3', '../0.2.0', '0.2.0-beta', '1.1000.0', '2100.0.0', '1.2.3\nx=y']:
            with self.subTest(value=value), self.assertRaises(ValueError): version(value)
        self.assertEqual(version('2099.999.999'), '2099.999.999')

    def test_builds_must_share_source_version_and_release_identity(self):
        verify_inputs(self.iphone, self.android, '0.2.0', 'a' * 40)
        with self.assertRaises(ValueError): verify_inputs(self.iphone, self.android, '0.2.0', 'b' * 40)
        self.android_info['applicationId'] += '.debug'
        self.write_android()
        with self.assertRaises(ValueError): verify_inputs(self.iphone, self.android, '0.2.0', 'a' * 40)

    def test_wrong_android_version_code_cannot_be_released(self):
        self.android_info['elements'][0]['versionCode'] = 1
        self.write_android()
        with self.assertRaises(ValueError): verify_inputs(self.iphone, self.android, '0.2.0', 'a' * 40)

    def test_corrupt_ipa_is_rejected(self):
        (self.iphone / 'phone.ipa').write_bytes(b'tampered')
        with self.assertRaises(ValueError): verify_inputs(self.iphone, self.android, '0.2.0', 'a' * 40)

    def test_apk_signed_with_another_key_is_rejected(self):
        cert = self.root / 'public.pem'
        cert.write_text('-----BEGIN CERTIFICATE-----\na2V5\n-----END CERTIFICATE-----\n')
        with patch('subprocess.check_output', return_value='Signer #1 certificate SHA-256 digest: ' + '0' * 64):
            with self.assertRaises(ValueError): verify_apk(self.root / 'app.apk', cert, 'apksigner')

    def test_absent_signing_secrets_fail_without_a_debug_key_fallback(self):
        with patch.dict('os.environ', {}, clear=True):
            with self.assertRaises(ValueError): restore_key(self.root / 'release.jks')
        self.assertFalse((self.root / 'release.jks').exists())

    def test_key_is_private_and_never_overwrites_an_existing_key(self):
        env = {'ANDROID_KEYSTORE_BASE64': 'a2V5', 'ANDROID_KEYSTORE_PASSWORD': 'fixture',
               'ANDROID_KEY_ALIAS': 'release', 'ANDROID_KEY_PASSWORD': 'fixture'}
        target = self.root / 'release.jks'
        with patch.dict('os.environ', env, clear=True):
            restore_key(target)
            self.assertEqual(target.stat().st_mode & 0o777, 0o600)
            with self.assertRaises(FileExistsError): restore_key(target)
