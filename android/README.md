# Motion Air for Android

Use Android 8 or later. Motion needs both an accelerometer and a gyroscope; phones without them can still use buttons.

## Install and play

1. Download [**MotionAir-Android.apk**](https://github.com/cplus2jules/motion-air/releases/latest/download/MotionAir-Android.apk) from the latest release.
2. Open it on your phone. Allow installation from your browser or file manager if Android asks, then tap **Install**.
3. Follow the [easy setup guide](../README.md) to start the computer launcher and connect the phone.

The phone app works with the same secure local bridge on Mac and Windows. Keep both devices on the same Wi-Fi. Scan the computer QR or use **Paste pairing code**, check the computer name, then pair.

Tap **Enable Motion** when connected. **Dance Lock** prevents accidental buttons; hold its unlock control for 1.5 seconds. Motion continues through a brief pause while the app remains visible. Leaving the app or locking the screen disconnects it; reconnect and enable motion again when you return.

## Updating

Install the newer release APK over the existing release app. GitHub release APKs use one persistent signing key and increasing version codes, so Android can accept updates without deleting saved connections.

Development builds use `com.motionair.controller.debug`; release builds use `com.motionair.controller`. They install side by side. A development build from before this separation used the release app ID with a different key. If Android reports a conflicting package for that older build, uninstall that development app first and pair again after installing the release. Uninstalling removes its saved connections.

## Build in Android Studio

1. In Android Studio, choose **File → Open** and select this repository's **android** folder.
2. Install Android SDK 35 and choose JDK 17 for Gradle.
3. Wait for Gradle sync. Select a device in Device Manager, then run the **app** configuration.

The repo root contains the computer bridge and is not an Android Studio Gradle project.

For command-line builds, set `ANDROID_HOME` to your Android SDK and use JDK 17:

```bash
cd android
./gradlew assembleDebug testDebugUnitTest lintDebug
```

On Windows, use `gradlew.bat` instead of `./gradlew`. The debug APK is at `app/build/outputs/apk/debug/app-debug.apk`. Start an emulator before running `node tools/android/emulator-check.mjs` from the repository root.

## Release builds

The [shared release workflow](../docs/mobile-releases.md) signs the Android APK, builds the iPhone IPA and publishes both on one GitHub Release. It tests Kotlin logic, lint, Android emulator pairing and lifecycle, and both desktop operating systems first.

Release signing uses environment variables. Never commit a keystore or password. See the release guide for the four GitHub signing secrets and local signing instructions.

The wire format uses raw device axes, acceleration including gravity in g with the Core Motion sign convention, gyroscope readings in degrees per second, and monotonic microsecond timestamps. [Sensor contract](../docs/motion-coordinate-contract.md).
