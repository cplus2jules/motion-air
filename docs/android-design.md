# Android design decisions

The Android app follows the existing Motion Air iPhone product, using native Kotlin views. The target is a phone in portrait orientation, operated without a developer terminal.

| Decision | Source | Role |
| --- | --- | --- |
| Red left grip, blue right grip, dark circular controls | Existing `NavigationControls.swift` and Motion Air icon | Preserve the same controller identity and physical button positions |
| Single sans-serif family, direct color blocks, immediate press feedback | [Playdate reference](https://play.date), Refero style c91209ef-f7f3-4d2b-bf69-41b58e4e2cc2 | Borrow tactile hierarchy only; Motion Air retains its own colors |
| Quiet pairing form, dark primary action, secondary paste option | [imgs.so reference](https://imgs.so), Refero style b7df8424-4714-4bb8-a4e1-48760f76d909 | Keep setup focused on the next action |
| Scan with a manual code fallback and name confirmation | [Comet device pairing](https://refero.design/screens/86594ce3-46a5-4543-b522-83fdcd1dbebd) and existing iPhone pairing | Camera denial must not prevent pairing |
| Dark Dance Lock with a 1.5-second unlock hold | Existing `DanceLockView.swift` | Prevent accidental game input while preserving motion |

Touch targets are at least 48 dp. Standard buttons retain TalkBack activation; the stick has equivalent directional buttons. Connection, focus, and receiver states use text. Layout scrolls on small phones and at large font sizes. No sensor statistics trigger reconstruction of held controls. No downloaded imagery or generated mockup is needed for this code-native interface.
