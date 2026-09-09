# Joypad Air verification — 2026-09-07

> Historical verification captured under the Joypad Air name, before the Motion Air rebrand. See [Motion Air branding](branding.md).

## Visual reference

Nintendo's official front-view Neon Red/Neon Blue Joy-Con photo was inspected in the browser. The playable UI follows its narrow silhouettes, inner rails, curved outside edges, vertically arranged controls, and dark recessed buttons. The rejected stretched color panels have been replaced.

## Verified locally

- Browser: desktop plus 844×390 landscape and 390px portrait. At 844×390 the stick and D-pad surfaces are 112px, separated vertically with no intersecting bounds or horizontal overflow. Portrait provides a working return-to-player-selection action.
- Shared English/Spanish work: language switching, persisted preferences and player names, settings labels, and mobile layouts checked by the parallel localization task. Its logging-backend A-button test produced matching DOWN/UP Z events after each language switch without reconnecting.
- 51 controller/DSU smoke assertions pass, including foreign-origin WebSocket rejection and guarded setup endpoints.
- 6 Ryujinx fixture tests pass: defaults/reordering, real mapping mismatches, exact backup, preserving unrelated players/settings, refusing running-emulator writes, unsupported/corrupt configuration.
- 7 localization tests pass. Native iOS and Android bundles built successfully on Expo SDK 54 in the localization task after the Pad changes.
- `git diff --check` and JavaScript syntax checks pass.

## Installed Ryujinx

Ryujinx 1.3.3 uses configuration schema 70. Both players were changed from sideways Joy-Cons to Pro Controller profiles for the full web controller. The original configuration backup is `~/Library/Application Support/Ryujinx/Config.json.backup-1788788976839`.

The setup page applied profiles successfully while Ryujinx was closed (additional backup `Config.json.backup-1788789098837`), then correctly refused an apply while Ryujinx was running. Computer use confirmed both players load Pro Controller with the generated key assignments. `npm run ryujinx:check` passes after reopening Ryujinx.

## Remaining verification boundary

macOS reports Accessibility denied for the server's responsible application. User permission was requested before granting that OS access. No successful real game input or gyro gameplay is claimed. A phone over actual Wi-Fi and a native device gyro run are also not yet verified; the available browser checks used the local Mac. Stock Ryujinx still needs the existing motion patch for DSU with keyboard input.

## Follow-up — September 9, 2026

The existing live paired bridge now reports native keyboard input and Accessibility granted. Its isolated Just Dance profile matches one right Joy-Con, and the running Ryujinx Motion build has one DSU subscription. Computer use showed Just Dance 2021. Neither saved iPhone was connected during this check; no physical motion or gameplay result is claimed.

Fixed the web controller reporting input ready before its first focus check. Unknown focus now shows the existing localized checking message. The server clears cached focus after a failed lookup or idle period, rechecks when the first controller connects, ignores outdated pending results, and pauses/releases input when focus becomes unknown. Connecting another player does not interrupt an already active session.

Validation: 56 controller smoke checks, 6 focus lifecycle tests, 4 motion tests, 3 secure pairing tests, 8 Ryujinx profile tests and 10 localization tests passed (87 total). Browser fixtures with no keyboard backend verified unknown-focus messages in English/Spanish and ready status only with confirmed focus. Syntax and diff checks passed.

The existing live bridge was left running with its saved pairing identity and emulator settings. Server-side focus changes require restarting that bridge. Computer use could inspect the game but could not establish sustained foreground focus for a web-button test; Terminal access through computer use was unavailable. The iPhone connection, physical motion and game-response checks remain pending.
