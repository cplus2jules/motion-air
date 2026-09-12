#!/bin/bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"
if [ "$(/usr/bin/uname -s)" != Darwin ]; then
  echo 'This launcher is for macOS. On Windows, open Motion Air.cmd.' >&2
  exit 1
fi
MACOS_VERSION="$(/usr/bin/sw_vers -productVersion)"
# The bundled libnut keyboard library targets macOS 14, even though Node itself
# can run on older macOS versions.
if [ "${MACOS_VERSION%%.*}" -lt 14 ]; then
  echo 'Motion Air needs macOS 14 or later for its computer control library.' >&2
  exit 1
fi
ARCH="$(/usr/bin/uname -m)"
if [ "$(/usr/sbin/sysctl -n hw.optional.arm64 2>/dev/null || true)" = 1 ]; then ARCH=arm64; fi
if [ "$ARCH" = x86_64 ]; then ARCH=x64; fi
VERSION='' ARCHIVE='' EXPECTED=''
while IFS=, read -r version platform arch digest filename; do
  if [ "$platform" = darwin ] && [ "$arch" = "$ARCH" ]; then
    VERSION="$version"; ARCHIVE="$filename"; EXPECTED="$digest"
  fi
done < "$ROOT/tools/bootstrap/node-runtimes.csv"
if [ -z "$VERSION" ]; then
  echo "Unsupported Mac processor: $ARCH. Motion Air supports Apple Silicon and Intel Macs." >&2
  exit 1
fi
RUNTIMES="$ROOT/.local/runtime"
RUNTIME="$RUNTIMES/${ARCHIVE%.tar.gz}"
NODE="$RUNTIME/bin/node"
NPM="$RUNTIME/lib/node_modules/npm/bin/npm-cli.js"
mkdir -p "$RUNTIMES"
export PATH="$RUNTIME/bin:$PATH"
export MOTION_AIR_NPM_CLI="$NPM"

# The OS releases this lock if setup crashes. Keep the file so waiting launches
# always lock the same inode. Release it before starting the long-lived bridge.
if [ "${1:-}" != --prepare-runtime ]; then
  echo 'Checking Motion Air setup. If another window is installing, this window will wait.'
  /usr/bin/lockf -k -t 720 "$RUNTIMES/setup.lock" /bin/bash "$0" --prepare-runtime
  if [ "${1:-}" = --install-only ]; then exit 0; fi
  exec "$NODE" "$ROOT/tools/start-pairing.mjs" --launch "$@"
fi
STAGE=''
cleanup() { if [ -n "$STAGE" ]; then rm -rf "$STAGE"; fi; }
trap cleanup EXIT
if [ ! -x "$NODE" ] || [ ! -f "$NPM" ] || [ "$(cat "$RUNTIME/.verified-sha256" 2>/dev/null || true)" != "$EXPECTED" ] || ! "$NODE" --version >/dev/null 2>&1; then
  echo "First-time setup: downloading Node.js for your Mac ($ARCH)."
  echo 'It stays inside the Motion Air folder. No separate Node.js installation is needed.'
  STAGE="$(mktemp -d "$RUNTIMES/.node-download.XXXXXX")"
  /usr/bin/curl --fail --location --show-error --proto '=https' --proto-redir '=https' --tlsv1.2 \
    --connect-timeout 20 --max-time 600 --retry 2 \
    "https://nodejs.org/dist/v$VERSION/$ARCHIVE" -o "$STAGE/$ARCHIVE"
  ACTUAL="$(/usr/bin/shasum -a 256 "$STAGE/$ARCHIVE")"
  ACTUAL="${ACTUAL%% *}"
  if [ "$ACTUAL" != "$EXPECTED" ]; then
    echo 'Node.js download verification failed. Open Motion Air again to retry.' >&2
    exit 1
  fi
  /usr/bin/tar -xzf "$STAGE/$ARCHIVE" -C "$STAGE"
  EXTRACTED="$STAGE/${ARCHIVE%.tar.gz}"
  [ -x "$EXTRACTED/bin/node" ] && [ -f "$EXTRACTED/lib/node_modules/npm/bin/npm-cli.js" ] || {
    echo 'The Node.js download is incomplete. Open Motion Air again to retry.' >&2; exit 1;
  }
  printf '%s\n' "$EXPECTED" > "$EXTRACTED/.verified-sha256"
  # Replace only this managed runtime after a verified download has succeeded.
  rm -rf "$RUNTIME"
  mv "$EXTRACTED" "$RUNTIME"
fi
if ! "$NODE" --version >/dev/null 2>&1; then
  echo 'Node.js could not run on this Mac. Check the supported macOS version in README.md.' >&2
  exit 1
fi
"$NODE" "$ROOT/tools/bootstrap/install.mjs"
