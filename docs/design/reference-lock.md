# Motion Air UI reference lock

Designing the native iPhone controller and Finder launch experience for local Mac players. The main job is to connect, navigate a game, and protect the controls while dancing. Build directly from the existing red/blue split-controller identity and its secure pairing flow.

## Direction

The controller is the product: two colored grips and dark, physical controls, on a native adaptive canvas. The Mac presents a pairing station with the same mark, a large QR, clear next steps and remembered phones. Keep diagnostics one action away. Use native Liquid Glass for floating actions and navigation on iOS 26, with material/solid fallbacks on iOS 17–18 and Reduce Transparency.

| Decision | Source | Role to preserve | Reason |
| --- | --- | --- | --- |
| Red and blue grips; paired-controller icon | Existing `public/icon.svg`, user brief | Product identity | Keep Motion Air recognizable |
| Bold hardware silhouette and compact rounded display type | [Playdate](https://play.date), Refero style c91209ef-f7f3-4d2b-bf69-41b58e4e2cc2 | Hardware and brand only | Make the controls feel like a handheld object |
| Adaptive canvas, system text, blue primary actions | [Apple](https://apple.com), Refero style e02fcb85-b3eb-4b9e-985f-ab2044beb6cc | Native hierarchy and actions | Keep setup and settings readable |
| Dark key surfaces with fine inset highlights | [Raycast](https://raycast.com), Refero style e5a749d7-9f71-4b52-9f50-24b243396034 | Physical controls and terminal only | Separate pressable keys from content |
| Controller-first screen with a compact connection header | [YouTube remote](https://refero.design/screens/6ed651e2-5cb8-482e-b0cb-cdaebbf06550) | Control layout | Controls stay above troubleshooting |
| Device management sheet with names and explicit current state | [Spotify device sheet](https://refero.design/screens/bd947445-36f8-4231-b022-c85ec7b032fd) | Device selection | Make saved and active devices distinct |
| Short steps beside the pairing action | [Spotify setup](https://refero.design/screens/3886f0de-c454-4ce4-b187-178a66000ba8) | Instruction hierarchy | Give the next action at the point of use |
| Entry, identity review, device management | [Comet sync flow](https://refero.design/flows/10949) | Sequence only | Preserve explicit trust before saving a Mac |

Tokens: canvas #F5F5F7 (adaptive system grouped background on iPhone), ink #1D1D1F, red #FF5364 and blue #20A5EB for hardware, action #0071E3, key #202630. SF Rounded for short product headings, SF Pro for body, SF Mono for diagnostics. Main panels 28pt, inner surfaces 16pt with 12pt inset. Controls at least 44pt; game buttons use immediate feedback and no delayed animation.

Signature: split controller grips, echoed in the layered app mark. No generic feature grid, background blobs, moving live numbers, or decorative animation during play. Pairing and settings use native sheets. The web copy/check icon uses the transitions-dev icon-swap recipe; touch hover is gated and keyboard actions are instant.

## Icon construction

Extend the editable vector mark in the repository and export lossless, aligned 1024 × 1024 PNGs. Background opaque; foreground layers transparent; no rounded outer mask, baked shadows, bevels, or highlights. Keep both grips and controls on separate layers so Icon Composer owns depth and lighting. Compile the saved Icon Composer document into the app; Xcode generates older-system icons. Keep the flat composite for portable use and the transparent vector for the web brand mark.

Apple references: [Icon Composer workflow](https://developer.apple.com/documentation/Xcode/creating-your-app-icon-using-icon-composer), [app icon guidelines](https://developer.apple.com/design/human-interface-guidelines/app-icons), [Liquid Glass in SwiftUI](https://developer.apple.com/documentation/swiftui/glasseffectcontainer).

## Verification target

Inspect compact and large iPhones, light and dark appearance, accessibility Dynamic Type, Reduce Motion and Reduce Transparency. Exercise unpaired, pairing validation/recovery, connected, motion, Dance Lock, settings and disconnect. Verify simultaneous input and release on the wire separately from screenshots. Check Mac desktop/mobile widths, keyboard focus, language, loading/error/empty/saved/connected states, QR renewal and copy fallback. Physical sensor, haptic and in-game scoring evidence must remain separate from simulator results.

## Welcome tour

Extend the existing controller identity in three screens: welcome, practice, and motion. Retain the red/blue hardware, large rounded headings, adaptive system canvas and glass primary action. The first screen uses the existing mark with a native geometric Mac illustration. The practice controller responds locally to stick drags and A/B presses; it has no session or transport dependency. The final screen explains Motion and Dance Lock, with an explicitly labeled lock preview.

| Decision | Reference | Bounded role |
| --- | --- | --- |
| Large product illustration with short text and one primary action | [Ahead welcome](https://refero.design/screens/16f8e01d-d9c7-4bd7-9f7b-4cf91165fec1) | Composition; keep Joypad colors |
| Try a control before using it; visible progress and skip | [Apple News Quartiles tutorial](https://refero.design/screens/96c2c436-26ae-422e-880c-6d8b088723be) | Learning interaction and navigation |
| Short sequence with an exit on every step | [Swipe gesture onboarding](https://refero.design/flows/3422) | Flow only |
| Hardware as the main illustration | Playdate style already locked above | Media hierarchy only |

The tour appears once per app installation/preferences, can be skipped, and can be replayed from Controller settings. Finishing opens the existing Mac chooser and secure pairing flow. Practice never enables live motion, asks for permissions, or sends game input. Page changes fade and travel 12pt in / 8pt out over 240 ms using the shared (0.23, 1, 0.32, 1) ease-out curve; the lock demonstration uses a 0.5 s / 0.2 bounce spring for explanation. Reduce Motion removes those transitions and the tilted phone. Fixed primary controls remain outside a scrollable page for large text and compact screens.
