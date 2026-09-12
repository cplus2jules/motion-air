#!/bin/bash
# Double-click in Finder. Terminal owns the bridge's Accessibility permission.
cd "$(dirname "$0")" || exit 1
/bin/bash tools/bootstrap/macos.sh "$@"
result=$?

if [ "$result" -ne 0 ] && [ -t 0 ]; then
  printf '\nPress Return to close this window. '
  read -r
fi
exit "$result"
