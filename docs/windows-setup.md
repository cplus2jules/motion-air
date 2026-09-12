# Motion Air on Windows

## First launch

1. Extract the whole computer ZIP into a folder you can write to, such as Documents. Use **Extract All**; keep the whole folder together.
2. Open your existing Ryujinx once, then quit it. Double-click **Motion Air.cmd**. First use downloads Node.js and installs the bridge dependencies automatically; later launches reuse them. No separate Node.js installation or administrator account is needed. See [automatic setup](computer-setup.md) for supported computers and retry instructions.
3. Choose **Ryujinx.exe** in the file picker. Motion Air detects `portable` beside that executable, or `%APPDATA%\Ryujinx`. If needed, choose **Config.json** from Ryujinx's **File → Open Ryujinx Folder**.
4. Motion Air saves a timestamped backup of Config.json and configures one right Joy-Con for Just Dance. Game paths and unrelated settings are preserved; keys, firmware, games and saves stay in your existing Ryujinx data folder. Ryujinx must be closed when applying the profile. Config versions other than 70 are refused without a rewrite.
5. Allow **Node.js** on your **private network** if Windows Firewall asks. The phone and PC must share a network that permits communication between devices.
6. Scan the browser's pairing QR with Motion Air on Android or iPhone. Confirm your PC's name. Keep the launcher window open and click the Ryujinx window to use buttons.

Next time, double-click **Motion Air.cmd** and connect to your saved computer on the phone. Motion starts off after reconnecting; turn on **Enable Motion** when ready. You can create a Windows shortcut to the `.cmd` file on your Desktop; keep the file itself in the project folder. Closing the launcher stops its bridge, while Ryujinx stays open.

The iPhone app may still label the action **Pair a Mac**; its QR pairing protocol also works with the Windows bridge. Android calls it **Scan computer QR**. Windows does not currently advertise Bonjour; scan a new QR if DHCP changes the PC address.

## Motion and your existing Ryujinx

Stock Ryujinx ignores CemuHook/DSU motion on its keyboard backend. Buttons can work with that build, but enabling motion in the phone cannot remove this emulator restriction. Select a compatible patched build, or build the repository's pinned motion variant:

1. Install [Git for Windows](https://git-scm.com/downloads/win), [.NET SDK 9](https://dotnet.microsoft.com/download/dotnet/9.0), and Node.js.
2. Quit Ryujinx. Double-click **Build Ryujinx Motion.cmd**. This downloads the pinned 1.3.3 source, checks its revision, applies the included patch, builds Windows x64, and runs the motion contract test. It creates a separate build under `.local/builds`.
3. Double-click **Motion Air.cmd**. It selects the new build and uses the settings location remembered from your existing Ryujinx. If you have never selected a settings location, choose the existing Config.json when prompted.

The build does not download games, keys, firmware or user data. It leaves the installed emulator executable intact. Build and physical Windows gameplay validation must be distinguished: this repository's Windows build helper has not yet been run on a Windows host.

## Change or restore setup

From Command Prompt in the project folder, run `"Motion Air.cmd" --setup` to select another emulator. Advanced overrides are `RYUJINX_EXE` and `RYUJINX_CONFIG_DIR`; `PAIRING_DSU_PORT` changes the DSU port. To restore your controller settings, quit Ryujinx and replace Config.json with its `Config.json.backup-...` copy. The selected executable and data directory are stored in `.local/windows-launcher.json`.

If first-time setup fails, check your internet connection and reopen the launcher from its extracted folder. If input does not reach the game, run both programs normally under the same Windows user; a normal launcher cannot inject input into an elevated Ryujinx window. Keep the game focused. The phone reports whether the emulator is consuming motion; zero receivers means the game has not subscribed to DSU.

## Español

Extrae la carpeta completa y abre **Motion Air.cmd**. Node.js y las dependencias se instalan automáticamente; necesitas internet la primera vez. Elige tu **Ryujinx.exe** con Ryujinx cerrado. El lanzador guarda la ubicación, crea una copia de Config.json, prepara un Joy-Con derecho y abre el QR de conexión. Si Windows lo solicita, permite Node.js en la red privada. Escanea el QR desde Android o iPhone, confirma el nombre del PC y mantén abierta la ventana del lanzador mientras juegas.

Ryujinx sin el parche solo recibe botones desde este puente. Para movimiento, usa una versión compatible o instala Git y .NET SDK 9 y abre **Build Ryujinx Motion.cmd**. Después, abre **Motion Air.cmd**. La compilación nueva usa los datos existentes del emulador. Activa el movimiento de nuevo tras cada reconexión. Para elegir otra versión, ejecuta `"Motion Air.cmd" --setup`.
