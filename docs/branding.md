# Motion Air branding

The product is **Motion Air**. The repository and command are `motion-air`:
[cplus2jules/motion-air](https://github.com/cplus2jules/motion-air).
The short description is “Wireless iPhone controller for Just Dance and motion games on Mac.”

Use Motion Air in both English and Spanish. The native app, Xcode workspace and
scheme, Finder launcher, web controller, pairing page, PWA manifests, Expo app,
terminal messages, and setup instructions use this name.

## Design direction

The existing controller interface is the build target. Preserve its red #FF5364
and blue #20A5EB grips, dark #202630 controls, adaptive canvas, and rounded system
headings. The name changes; the physical controller silhouette and air accents
carry the project's visual history forward. The mark is `public/motion-mark.svg`;
the layered native source is `assets/icon-composer/Motion Air.icon`.

The existing [reference lock](design/reference-lock.md) remains the design source:
[Playdate](https://play.date) informs the compact, bold hardware identity, and
[Apple](https://apple.com) informs system typography, neutral surfaces, and blue
actions. Keep those roles distinct. This rebrand adds attribution to the pairing
footer and native settings without changing control placement or motion behavior.

## Joypad Air credit

Use “Built with Joypad Air in mind, and based on Joypad Air by David García
(mindavidev).” Link to the original project when space permits. Full credit and
license information live in [ACKNOWLEDGEMENTS.md](../ACKNOWLEDGEMENTS.md).

## Compatibility identifiers

Some internal names intentionally retain Joypad Air so an app update can use
existing installations and saved settings:

- iOS bundle ID `com.juliansalas.joypadair.probe` and Keychain service
  `com.juliansalas.joypadair.pairing` preserve the installed app and paired Macs.
- Bonjour `_joypadair._tcp`, QR scheme `joypadair://`, and the bridge's
  `app: "joypad-air"` protocol marker preserve discovery, pairing, and launcher reuse.
  The status response also exposes `displayName: "Motion Air"`.
- `JOYPAD_*` environment variables and `joypad-air-language` storage keep existing
  launch configurations and language preferences working.
- The internal `JoypadCore` package, sensor queue, and local emulator bundle ID
  retain their existing names. Existing `Joypad Air Just Dance` profiles remain
  recognized; newly generated profiles use `Motion Air Just Dance`.

The current local checkout may still be named `joypad-air`; launchers locate the
project relative to their own files. Fresh clones use `motion-air`. Historical
verification reports and screenshots retain the name shown when they were captured.
