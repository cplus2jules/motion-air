# Windows launcher and Android APK verification

Verified locally on September 12, 2026. The host was macOS; the Android device was an Android 15 ARM64 emulator.

## Downloads

- `dist/Motion-Air-Windows.zip`: extract the entire folder, install Node.js 22 if needed, then double-click **Motion Air.cmd**. The first launch installs bridge dependencies and asks for an existing Ryujinx executable. See [Windows setup](windows-setup.md).
- `dist/Motion-Air-Android-0.1.0-debug.apk`: native Kotlin app, Android 8.0 and later, signed with a development key for sideloading. This is not a Play Store release. See [Android setup](../android/README.md).
- `dist/SHA256SUMS.txt`: hashes for both downloads.

The Windows archive contains source, launchers and documentation. It excludes installed dependencies, local pairing identities, signing keys, emulator binaries, firmware and games. The optional **Build Ryujinx Motion.cmd** builds the pinned, patched emulator from source.

## Passed locally

- Bridge assertions: 56 passed.
- Windows setup regression tests: 6 passed. These exercise file selection, settings preservation, portable data precedence, process-check failure handling, argument passing, and remembering a manual selection after a motion build. The test that invokes the real Windows helper was skipped on macOS.
- Launcher, TLS pairing and motion integration tests: 11 passed. Ryujinx configuration, focus and localization tests also passed.
- Android build, six JVM tests and Android lint completed successfully. Lint reports warnings, with no errors. Tests cover invitation validation, exact profile acknowledgments, sensor units, saved hosts and TLS pinning against an actual HTTPS test server.
- Two Android emulator smoke tests passed: home-screen entry points and encrypted Android Keystore persistence.
- An Android emulator integration test paired through the real HTTPS bridge, received the controller acknowledgment, enabled motion, delivered emulator sensor data to a DSU receiver, entered Dance Lock, unlocked with a hold, and disconnected on backgrounding. One observed run delivered 494 motion frames with no reported drops. After backgrounding, the bridge reported no connected player, held buttons, stick directions or active DSU slot.
- The APK passed Android `apksigner` verification. Its manifest reports application ID `com.motionair.controller`, version `0.1.0`, minimum SDK 26 and target SDK 35.
- The controller and Dance Lock screens were inspected on the emulator. Git whitespace checks and the macOS launcher shell syntax checks passed.

The bridge integration test uses a disposable pairing identity and logging-only keyboard mode. No test typed into the user's applications or launched a game. The optional `BridgeIntegrationTest` accepts a fresh `pairingInvitation` instrumentation argument and expects an active DSU receiver; ordinary connected tests skip it when no invitation is supplied.

## Still requires device verification

The Windows file picker, PowerShell launcher, foreground-window checks, console shutdown and patched Windows emulator build have not run on a Windows computer. A Windows/macOS CI workflow is included, but was not pushed or executed remotely during this work.

Physical Android camera scanning, sensor orientation, haptics, LAN pairing and sustained gameplay need a real phone. Emulator transport results do not establish Just Dance scoring. Stock Ryujinx does not consume DSU motion for the keyboard controller; motion requires the compatible patched build described in the Windows setup guide.
