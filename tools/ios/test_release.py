import hashlib
import json
import os
from pathlib import Path
import plistlib
import tempfile
import unittest

from release import BUNDLE_ID, build_number, describe_release, metadata, verify_app, version


class ReleaseTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.project = self.root / "project.pbxproj"
        self.project.write_text("MARKETING_VERSION = 0.1.0; MARKETING_VERSION = 0.1.0;")

    def app(self, **changes):
        app = self.root / "MotionAir.app"
        app.mkdir(exist_ok=True)
        info = {
            "CFBundleIdentifier": BUNDLE_ID, "CFBundleDisplayName": "Motion Air",
            "CFBundleShortVersionString": "0.2.0", "CFBundleVersion": "12.1",
            "CFBundleSupportedPlatforms": ["iPhoneOS"], "CFBundleExecutable": "MotionAir",
        }
        info.update(changes)
        (app / "Info.plist").write_bytes(plistlib.dumps(info))
        (app / "MotionAir").write_bytes(b"fixture")
        (app / "MotionAir").chmod(0o755)
        return app

    def test_branch_build_uses_project_version(self):
        result = metadata(self.project, "refs/heads/main", run_number="42", attempt="2")
        self.assertEqual(result["version"], "0.1.0")
        self.assertEqual(result["build_number"], "42.2")

    def test_tag_overrides_project_version(self):
        result = metadata(self.project, "refs/tags/ios/v0.2.0")
        self.assertEqual(result["version"], "0.2.0")
        self.assertEqual(result["artifact_name"], "MotionAir-0.2.0-build-1.1-unsigned")

    def test_manual_build_can_set_version(self):
        self.assertEqual(metadata(self.project, requested="1.2.3")["version"], "1.2.3")

    def test_versions_reject_paths_shell_text_and_unsupported_suffixes(self):
        for value in ["", "v1.2.3", "1.2", "01.2.3", "1.2.3-beta.1", "1.2.3\npublish=true", "$(id)", "../1.2.3"]:
            with self.subTest(value=value), self.assertRaises(ValueError):
                version(value)

    def test_wrong_and_mismatched_tags_fail(self):
        for ref in ["refs/tags/v1.2.3", "refs/tags/ios/v1.2", "refs/tags/ios/v1.2.3-beta.1"]:
            with self.subTest(ref=ref), self.assertRaises(ValueError):
                metadata(self.project, ref)
        with self.assertRaises(ValueError):
            metadata(self.project, "refs/tags/ios/v0.2.0", requested="0.3.0")

    def test_conflicting_project_versions_fail(self):
        self.project.write_text("MARKETING_VERSION = 0.1.0; MARKETING_VERSION = 0.2.0;")
        with self.assertRaises(ValueError):
            metadata(self.project)

    def test_build_counter_stays_within_apple_limits(self):
        self.assertEqual(build_number("9999.99.99"), "9999.99.99")
        for value in ["0", "10000", "1.100", "1.1.100", "01", "1.01", "1.2.3.4", "1\n2"]:
            with self.subTest(value=value), self.assertRaises(ValueError):
                build_number(value)
        for run, attempt in [("10000", "1"), ("1", "100"), ("0", "1"), ("1", "0")]:
            with self.subTest(run=run, attempt=attempt), self.assertRaises(ValueError):
                metadata(self.project, run_number=run, attempt=attempt)

    def test_valid_iphone_bundle_passes(self):
        self.assertEqual(verify_app(self.app(), "0.2.0", "12.1")["CFBundleIdentifier"], BUNDLE_ID)

    def test_simulator_or_wrong_version_bundle_cannot_be_released(self):
        for changes in [{"CFBundleSupportedPlatforms": ["iPhoneSimulator"]}, {"CFBundleVersion": "1"},
                        {"CFBundleShortVersionString": "0.1.0"}, {"CFBundleIdentifier": "wrong.app"}]:
            with self.subTest(changes=changes), self.assertRaises(ValueError):
                verify_app(self.app(**changes), "0.2.0", "12.1")

    def test_missing_or_non_executable_binary_fails(self):
        app = self.app()
        (app / "MotionAir").chmod(0o644)
        with self.assertRaises(ValueError):
            verify_app(app, "0.2.0", "12.1")
        (app / "MotionAir").unlink()
        with self.assertRaises(ValueError):
            verify_app(app, "0.2.0", "12.1")

    def test_embedded_signing_profile_cannot_leak_into_unsigned_release(self):
        app = self.app()
        (app / "embedded.mobileprovision").write_bytes(b"fixture profile")
        with self.assertRaises(ValueError):
            verify_app(app, "0.2.0", "12.1")

    def test_manifest_and_checksums_cover_release_assets(self):
        for suffix in ["-unsigned.ipa", ".dSYMs.zip"]:
            (self.root / f"MotionAir-0.2.0-build-12.1{suffix}").write_bytes(b"fixture asset")
        describe_release(self.root, "0.2.0", "12.1", "a" * 40, "Xcode 26.6")
        manifest = json.loads((self.root / "build-info.json").read_text())
        self.assertEqual(manifest["signing"], "unsigned-requires-sideload-signing")
        lines = (self.root / "SHA256SUMS.txt").read_text().splitlines()
        self.assertEqual(len(lines), 4)
        for line in lines:
            digest, filename = line.split("  ", 1)
            self.assertEqual(hashlib.sha256((self.root / filename).read_bytes()).hexdigest(), digest)

    def test_missing_assets_cannot_produce_release_manifest(self):
        with self.assertRaises(ValueError):
            describe_release(self.root, "0.2.0", "12.1", "a" * 40, "Xcode 26.6")
        self.assertFalse((self.root / "build-info.json").exists())


if __name__ == "__main__":
    unittest.main()
