# Development

For installation without building the apps, start with the [main README](../README.md).

To report an issue or propose a change, read the [contribution guide](../CONTRIBUTING.md) ([español](../CONTRIBUTING.es.md)).

## Repository layout

| Folder | Purpose |
| --- | --- |
| `android/` | Native Kotlin Android controller |
| `native/` | Native Swift iPhone app and shared JoypadCore tests |
| `server/` | Computer bridge, secure pairing and DSU motion delivery |
| `public/` | Browser controller and computer setup dashboard |
| `app/` | Expo controller fallback |
| `tools/` | Launchers, emulator setup, regression tests and release scripts |

## Computer bridge

Install Node.js 22 or later, then run:

```bash
npm ci
npm run start:paired
```

`npm run play` also launches the selected local emulator. Keep that process open while testing. `npm start` runs the separate browser-controller bridge.

Run the bridge checks with:

```bash
npm test
node --test tools/bootstrap/install-test.mjs tools/windows-test.mjs tools/launcher-test.mjs tools/pairing-test.mjs tools/ryujinx-test.mjs tools/focus-test.mjs tools/i18n-test.mjs tools/motion-test.mjs
```

The tests use isolated ports and log-only keyboard output. `npm run ryujinx:check` separately checks your installed emulator configuration.

The downloaded [Mac and Windows launchers](computer-setup.md) install a private Node runtime and locked npm dependencies. Run `node tools/bootstrap/check-clean-install.mjs` on either system to test first-time setup in an empty folder, simultaneous launches, cached startup and dependency repair. It does not launch an emulator or change your saved pairing.

## Android

Open the **android folder**, not the repository root, in Android Studio. Use JDK 17 and Android SDK 35. See [Android build instructions](../android/README.md).

With an Android emulator already running:

```bash
node tools/android/emulator-check.mjs
```

This builds the test APK, pairs it to a disposable local bridge with certificate pinning, subscribes a UDP receiver, and tests motion, visible pause, background and reconnect. It needs `ANDROID_HOME` and Java on your path. It sends no real keyboard input. For a custom Gradle installation, set `MOTION_AIR_GRADLE` to its executable.

## iPhone

Open `native/MotionAir.xcworkspace` in Xcode and select **MotionAir**. Device signing is described in the [iPhone setup guide](local-device-setup.md).

```bash
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer xcrun swift test --package-path native/Packages/JoypadCore
python3 -m unittest discover -s tools/ios -p 'test_*.py' -v
python3 -m unittest discover -s tools/mobile -p 'test_*.py' -v
```

The [iPhone release script](../tools/ios/build-release.sh) tests JoypadCore and archives a device IPA without booting a simulator.

## Motion validation

Both native apps send device-axis acceleration including gravity in g, gyroscope readings in degrees per second, and monotonic microsecond timestamps. They reject sensor readings older than 100 ms. Android requires both sensors to be fresh. The iPhone includes sensor-queue delay in the writer's freshness budget.

A passing emulator test establishes transport and lifecycle behavior. Use a real phone to check grip, sensor axes during movement, haptics and Just Dance scoring. Follow the [sensor contract](motion-coordinate-contract.md) and [implementation status](motion-implementation-status.md). Capture live transport diagnostics with `node tools/capture-motion.mjs --seconds 30` while motion is enabled.

[Release both apps](mobile-releases.md).
