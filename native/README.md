# Motion Air native controller and motion probe

Open **MotionAir.xcworkspace** in Xcode and choose **MotionAir**. The iPhone app pairs with the Mac's local bridge, remembers its identity securely, provides a menu joystick and navigation buttons, and streams Core Motion after the bridge acknowledges the `just-dance` profile. Navigation and pairing were brought forward from the plan at the user's request; actual game scoring remains unverified.

- [Install, pair, and verify](../docs/local-device-setup.md)
- [Sensor, wire, and coordinate contract](../docs/motion-coordinate-contract.md)
- [Master implementation plan](../docs/swift-local-just-dance-plan.md)

`Packages/JoypadCore` contains sensor conversions, wire messages, safe endpoints, invitation parsing, session sequencing, bounded output buffering, and Swift Testing tests. `iOS/MotionAir` owns Core Motion, the ordered WebSocket sender, lifecycle cleanup, touch controls, Bonjour browsing, the QR scanner, pinned TLS, and Keychain persistence. The Mac pairing service is currently Node; no Swift Mac companion exists yet.

## UI and icon

The controller uses red and blue grips, dark tactile buttons, a device sheet, and separate settings. Motion and Dance Lock actions use native Liquid Glass on iOS 26, with solid controls when Reduce Transparency is enabled. Extra shoulders and SL/SR are under More buttons. All control input still uses the existing ordered sender and release timing.

The current [Refero reference lock](../docs/design/reference-lock.md) documents the design sources. The [verification report](../docs/design/ui-verification.md) records rendered checks and their limits.

The target compiles `assets/icon-composer/Motion Air.icon`. Open it in Icon Composer to edit lighting and appearances. The [icon package](../assets/icon-composer/README.md) includes five aligned PNG layers, editable SVGs, and a flat composite. Xcode generates older-system icon images from the Composer document.

Debug builds support `--ui-preview-connected`, `--ui-preview-motion`, and `--ui-preview-dance` for repeatable visual verification. These fixtures are labeled on screen and produce no game or sensor output. Release builds exclude them. Use normal pairing to verify actual transport.

The QR flow confirms a specific Mac before trust is saved. Discovery alone never authorizes a connection. A successful socket connection does not establish successful game scoring.
