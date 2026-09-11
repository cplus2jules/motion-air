# Motion Air for Android

Native Kotlin controller for the same Motion Air bridge used by iPhone. Requires Android 8.0 or newer. Buttons work without motion sensors; dancing requires both an accelerometer and gyroscope, plus a compatible Ryujinx motion build.

## Install and connect

Install the generated **Motion Air APK** on your phone. Android may ask you to allow installing apps from the browser or file manager used to open it. This development APK is signed for sideloading; it is not a Play Store release.

1. Open **Motion Air.cmd** on Windows or **Motion Air.command** on Mac. Leave the launcher open.
2. Put the phone and computer on the same Wi-Fi. Open Motion Air on Android and choose **Scan computer QR**, or **Paste pairing code**. Camera access is requested only by the scanner.
3. Confirm the computer name. Saved computers reconnect from the app's home screen.
4. Click the Ryujinx window on the computer, choose your game, and enable motion in the phone. **Dance Lock** prevents accidental presses; hold its unlock button for 1.5 seconds to return to controls. TalkBack can activate the unlock button directly.

The controller is portrait-oriented. App switching, screen locking, and disconnecting stop the sensors and close the connection; the bridge releases held input. Return to the app and tap **Reconnect**, then enable motion again. If the computer gets a new address, scan a new QR. English and Spanish are available from the home screen.

## Build

Install JDK 17, Android SDK Platform 35 and Build Tools 35.0.0, or open this folder in Android Studio and let it install the required SDK. Set `ANDROID_HOME`, or set `sdk.dir` in a local `local.properties`. The Gradle wrapper downloads its pinned distribution and validates its checksum.

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug
```

On Windows, run `gradlew.bat assembleDebug testDebugUnitTest lintDebug`. The APK is at `app/build/outputs/apk/debug/app-debug.apk`. With a device or emulator attached, run `./gradlew connectedDebugAndroidTest` to check app launch and Android Keystore persistence. No Expo server is needed.

The debug signing key is a development identity. Preserve a dedicated release keystore before distributing future release builds; release signing is intentionally not configured with a shared debug key.

## Protocol and privacy

- One-time `joypadair://pair` QR invitations validate version, expiry, host, certificate fingerprint and computer identity.
- HTTPS and WSS pin the exact certificate SHA-256 from the QR. Redirects are disabled. Hosts and DNS results must be local network addresses. There is no trust-all TLS mode or cleartext fallback.
- Per-computer bearer tokens are encrypted with an AES-GCM key in Android Keystore. App backup is disabled. Tokens and pairing codes are not written to logs.
- The app requires the `just-dance` profile advertisement and an exact configuration acknowledgment before streaming. Each connection has a fresh session ID, ordered sample numbers, and monotonic microsecond timestamps.
- Android accelerometer readings are negated and divided by 9.80665 to match the existing Core Motion convention. Gyroscope rates are converted from radians/second to degrees/second. Gravity stays present and the bridge performs the only device-to-DSU transform. See [Android motion sensor units](https://developer.android.com/develop/sensors-and-location/sensors/sensors_motion) and the [shared coordinate contract](../docs/motion-coordinate-contract.md).
- Motion requests approximately 60 Hz; old or congested frames are discarded. Buttons preserve down/up ordering and a minimum tap interval. A heartbeat timeout closes stale connections.

Automated transport, unit and emulator tests do not establish physical sensor orientation, Wi-Fi reliability, haptics or Just Dance scoring on an actual Android phone. Validate those on a real device and distinguish them from the APK build result.

## Español

Instala el APK, abre el lanzador de Motion Air en Windows o Mac y conecta ambos equipos a la misma Wi-Fi. En Android, pulsa **Escanear QR del equipo** o **Pegar código de conexión**, confirma el nombre del equipo y activa el movimiento cuando estés listo. **Bloqueo para bailar** evita pulsaciones accidentales. Mantén pulsado el botón de desbloqueo durante 1,5 segundos para volver al mando.

Para compilar, instala JDK 17 y Android SDK 35. Ejecuta `gradlew.bat assembleDebug` en Windows o `./gradlew assembleDebug` en Mac. El APK se genera en `app/build/outputs/apk/debug/app-debug.apk`. Al salir de la app se detienen los sensores; pulsa **Reconectar** al volver. El movimiento comienza desactivado tras cada reconexión.
