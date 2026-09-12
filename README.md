# Motion Air

Use your **Android phone or iPhone as a wireless controller** for motion games on a **Mac or Windows PC**.

[Download Motion Air](https://github.com/cplus2jules/motion-air/releases) · [Leer en español](README.es.md)

Motion Air is still being tested. Buttons and motion delivery have automated checks; reliable Just Dance scoring needs testing with a real phone and game.

## What you need

- An Android phone running Android 8 or later, or an iPhone running iOS 17 or later.
- A Mac or Windows PC, connected to the same Wi-Fi as your phone.
- [Node.js](https://nodejs.org/en/download), installed on the computer. Choose the **LTS** installer; version 22 or later works.
- Your emulator and game, already installed. **Just Dance needs the motion-capable Ryujinx build.** The regular keyboard controller setup alone will not enable motion. Follow the [Mac setup guide](docs/local-device-setup.md) or [Windows setup guide](docs/windows-setup.md) for that one-time setup.

Games, emulator downloads, firmware and game keys are not included.

## 1. Download the right files

Use these links to download the latest apps. You can also find them under **Assets** on the [release page](https://github.com/cplus2jules/motion-air/releases/latest).

| For | Download |
| --- | --- |
| Your Mac or Windows PC | [Download for your computer](https://github.com/cplus2jules/motion-air/releases/latest/download/MotionAir-Computer.zip) |
| Your Android phone | [Download Android APK](https://github.com/cplus2jules/motion-air/releases/latest/download/MotionAir-Android.apk) |
| Your iPhone | [Download iPhone IPA](https://github.com/cplus2jules/motion-air/releases/latest/download/MotionAir-iPhone-unsigned.ipa) |

You need the computer ZIP **and** one phone app. You can ignore the other files. Sign in to GitHub if the repository asks you to; downloads from this private repository require access.

## 2. Install the phone app

### Android

1. Download the APK on your phone, then open it from **Downloads**.
2. If Android asks, allow your browser or file manager to **install unknown apps** for this installation.
3. Tap **Install**, then open **Motion Air**.

### iPhone

The iPhone download needs **sideloading**: a computer tool signs the app with your Apple account and installs it on your phone. Tapping the IPA on the iPhone will not install it.

1. Set up your preferred sideloading tool, such as [AltStore Classic](https://faq.altstore.io/) or [Sideloadly](https://sideloadly.io/), using its own installation guide.
2. Import the downloaded IPA into that tool and follow its signing and installation steps.
3. Open **Motion Air** on your iPhone. Allow **Local Network** access when asked.

Your signing method determines when the app needs refreshing. See [iPhone installation details](docs/ios-releases.md#download-a-build).

## 3. Start Motion Air on your computer

1. Extract the computer ZIP into a folder you can find again.
2. Open that folder and double-click:
   - **Mac:** `Motion Air.command`
   - **Windows:** `Motion Air.cmd`
3. Wait for the first-time installation to finish. A browser page with a pairing QR code opens.
4. Keep the launcher's Terminal or command window open while playing.

On Mac, allow Terminal under **System Settings → Privacy & Security → Accessibility** so the phone's buttons can control the game. The [Mac guide](docs/local-device-setup.md) explains the emulator setup; the [Windows guide](docs/windows-setup.md) explains selecting your `Ryujinx.exe`.

Keep the launcher inside its extracted folder. You can make a shortcut or Finder alias if you want it on your Desktop.

## 4. Connect your phone

1. Keep your phone and computer on the same Wi-Fi. Avoid a guest network.
2. Open Motion Air on your phone. Follow the short welcome guide, then choose **Pair my computer** on Android or **Pair my Mac** on iPhone. If you skipped the guide, use the blue pairing button at the bottom.
3. Scan the QR shown on the computer. Allow camera access if asked.
4. Check the computer name, then confirm the connection.
5. Click the game window on your computer. Try the phone's buttons.

Both apps let you practice the stick, A and B before connecting. You can replay the welcome guide from Settings.

You only need to scan the QR the first time. Later, select your saved computer. If its address changes and reconnecting fails, scan a fresh QR.

## 5. Turn on movement

1. Open your game using the motion-capable emulator setup.
2. On your phone, tap **Enable Motion**.
3. Hold the phone securely in your right hand, upright with the top toward your fingertips.
4. Tap **Dance Lock** to avoid accidental button presses. Hold the unlock button to return to the controls.

Keep Motion Air open on your phone. Switching to another app or locking the screen stops the connection. After reconnecting, tap **Enable Motion** again.

## Something isn't working

| Problem | Try this |
| --- | --- |
| My phone cannot connect | Use the same Wi-Fi, keep the computer launcher open, and scan a fresh QR. On iPhone, check Motion Air's Local Network permission. Guest Wi-Fi may block connections between devices. |
| The buttons do nothing | Click the game window. On Mac, check Terminal's Accessibility permission. |
| Connected, but movement does nothing | Tap **Enable Motion**. Check that the motion-capable emulator is running. A connected phone still needs a game listening for motion. |
| Movement stopped after I left the app | Reopen Motion Air, reconnect, and enable motion again. |
| Android says the update cannot install | Use the APK from the newest release. An older development app may use a different signing key. See [Android updates](android/README.md#updating). |
| The iPhone app will not open | Check whether your sideloading tool needs to refresh or re-sign it. |
| The launcher says Node.js is missing | Install the Node.js LTS version, then open the launcher again. |

More help: [connection troubleshooting](docs/connection-reliability.md) · [motion testing status](docs/motion-implementation-status.md)

## For developers

[Build and test the apps](docs/development.md) · [Publish both apps](docs/mobile-releases.md) · [Other controller options](docs/controller-options.md)

## Credits

Motion Air is based on [Joypad Air by David García (mindavidev)](https://github.com/mindavidev/joypad-air). This independent continuation retains the original MIT copyright and license. See [acknowledgements](ACKNOWLEDGEMENTS.md) and [license](LICENSE).
