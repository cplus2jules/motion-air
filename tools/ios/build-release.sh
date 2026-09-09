#!/bin/bash
# Build an unsigned device IPA for a sideloading tool to sign locally.
set -euo pipefail

root="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$root"
: "${VERSION:?Set VERSION to a release version such as 0.2.0}"
: "${BUILD_NUMBER:?Set BUILD_NUMBER to an Apple build number such as 1.1}"
output="${MOTION_AIR_OUTPUT_DIR:-$root/.local/ios-release}"
mkdir -p "$output/logs"
if [ -e "$output/dist" ]; then
  echo "Output already exists: $output/dist. Choose a new MOTION_AIR_OUTPUT_DIR." >&2
  exit 1
fi
mkdir "$output/dist"
build_root="$(mktemp -d "${RUNNER_TEMP:-${TMPDIR:-/tmp}}/motion-air-ios.XXXXXX")"
trap 'rm -rf "$build_root"' EXIT

# Validate before starting an expensive build or using a version in file paths.
python3 - "$VERSION" "$BUILD_NUMBER" <<'PY'
import sys
sys.path.insert(0, 'tools/ios')
from release import version, build_number
version(sys.argv[1])
build_number(sys.argv[2])
PY

xcodebuild -version | tee "$output/logs/toolchain.log"
xcrun swift --version | tee -a "$output/logs/toolchain.log"
export CLANG_MODULE_CACHE_PATH="$build_root/module-cache"
export SWIFTPM_MODULECACHE_OVERRIDE="$build_root/module-cache"
xcrun swift test --package-path native/Packages/JoypadCore \
  --scratch-path "$build_root/swift" --manifest-cache local --disable-sandbox \
  2>&1 | tee "$output/logs/swift-tests.log"

archive="$build_root/MotionAir.xcarchive"
xcodebuild archive -workspace native/MotionAir.xcworkspace -scheme MotionAir \
  -configuration Release -sdk iphoneos -destination 'generic/platform=iOS' \
  -derivedDataPath "$build_root/DerivedData" -archivePath "$archive" \
  -resultBundlePath "$build_root/Archive.xcresult" \
  MARKETING_VERSION="$VERSION" CURRENT_PROJECT_VERSION="$BUILD_NUMBER" \
  CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO CODE_SIGN_IDENTITY= \
  DEVELOPMENT_TEAM= SKIP_INSTALL=NO \
  2>&1 | tee "$output/logs/archive.log"

app="$archive/Products/Applications/MotionAir.app"
python3 tools/ios/release.py verify-app --app="$app" --version="$VERSION" --build="$BUILD_NUMBER"
xcrun lipo "$app/MotionAir" -verify_arch arm64
mkdir "$build_root/Payload"
ditto "$app" "$build_root/Payload/MotionAir.app"
stem="MotionAir-$VERSION-build-$BUILD_NUMBER"
ditto -c -k --keepParent "$build_root/Payload" "$output/dist/$stem-unsigned.ipa"
ditto -c -k --keepParent "$archive/dSYMs" "$output/dist/$stem.dSYMs.zip"
python3 tools/ios/release.py describe --directory="$output/dist" \
  --version="$VERSION" --build="$BUILD_NUMBER" --commit="$(git rev-parse HEAD)" \
  --xcode="$(xcodebuild -version)"

echo "Unsigned iPhone IPA and checksums: $output/dist"
if [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then
  cat "$output/dist/RELEASE_NOTES.md" >> "$GITHUB_STEP_SUMMARY"
fi
