#!/bin/bash
set -euo pipefail
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SOURCE_DIR="${1:?Pass the source directory used to build the emulator.}"
DOTNET_BIN="${DOTNET_BIN:-dotnet}"
FIXTURES="$(mktemp -d)"
cd "$PROJECT_DIR"
MOTION_AIR_BUILD="$FIXTURES" node tools/ryujinx-build/write-test-fixtures.mjs
"$DOTNET_BIN" run --project tools/ryujinx-build/tests/MotionContract.csproj -p:RyujinxSource="$SOURCE_DIR" -- "$FIXTURES/profile.json" "$FIXTURES/motion-0.bin" "$FIXTURES/motion-1.bin"
