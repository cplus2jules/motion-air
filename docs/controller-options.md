# Other controller options

The [native Android and iPhone apps](../README.md) are the main setup for motion games. The original browser and Expo controllers remain available.

## Browser controller

From a local checkout with Node.js installed:

```bash
npm ci
npm start
```

Open the displayed setup page and scan its QR with your phone. On iPhone, use Safari's **Share → Add to Home Screen** for a fullscreen controller. The browser controller on an ordinary HTTP connection provides buttons; iPhone motion sensor access requires HTTPS.

Choose a player each time you open the controller. It remembers the previous choice and releases held inputs when hidden or when Settings opens.

## Expo controller

Run the computer bridge in one Terminal window:

```bash
npm ci
npm start
```

In another Terminal, from the repository folder:

```bash
cd app
npm install
npx expo start
```

Open it through Expo Go and enable its gyro control. See the files in `app/` for this separate client.

## Emulator and language settings

Dolphin, Cemu and Citra can use a DSU/cemuhook receiver at `127.0.0.1:26760`. A receiver on another machine needs an explicitly configured network binding. Ryujinx's keyboard controller requires the local motion patch; use the [Mac](local-device-setup.md) or [Windows](windows-setup.md) guide.

The browser controller and setup dashboard support English and Spanish. Choose a language in the interface; it is saved on that device. Terminal output uses `JOYPAD_LANG=en` or `JOYPAD_LANG=es` and changes on restart.

```bash
JOYPAD_LANG=es npm start
```

The browser controller also has player names, color themes, sensitivity and haptic settings. These options vary between clients. Some games require separate physical HID controllers for two players and will reject multiple keyboard-backed pads.

The computer setup dashboard shows player connections, keyboard output, Accessibility permission, emulator focus and profiles. Quit Ryujinx before applying a new profile. Setup keeps a timestamped backup and preserves unrelated configuration.
