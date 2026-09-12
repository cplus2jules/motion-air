# Automatic computer setup

Download [MotionAir-Computer.zip](https://github.com/cplus2jules/motion-air/releases/latest/download/MotionAir-Computer.zip), extract the whole folder, then open **Motion Air.command** on Mac or **Motion Air.cmd** on Windows. Keep the window open while setup runs and while playing.

The launcher installs Node.js and the packages used by Motion Air's computer bridge. You do not need Homebrew, Node.js, npm, Git or a compiler to run this setup. Use a folder you can write to, such as Documents. Internet access is needed on first use and when dependencies change.

The downloads stay inside the extracted folder. Setup does not install a system service, change your global Node.js installation or ask for an administrator account. Mac Accessibility and Windows private-network firewall permissions still apply when you start playing. Your emulator, games and phone app have their own installation steps in the [main guide](../README.md).

## Computers

| Computer | Runtime selected |
| --- | --- |
| Apple Silicon Mac, macOS 14+ | Native ARM64 |
| Intel Mac, macOS 14+ | x64 |
| Intel or AMD PC, 64-bit Windows 10/11 | x64 |
| ARM PC, Windows 11 | x64 through Windows app emulation |

The keyboard library includes an x64 Windows binary, so Windows ARM also uses x64 Node. [Microsoft explains Windows 11's x64 emulation](https://learn.microsoft.com/en-us/windows/arm/apps-on-arm-x86-emulation). The Mac minimum comes from the packaged keyboard library, which targets macOS 14. Node.js itself has [separate platform requirements](https://github.com/nodejs/node/blob/v22.23.2/BUILDING.md#platform-list). The emulator can require a newer system or different hardware. Windows ARM gameplay still needs testing on a physical PC.

## If setup stops

- Check your internet connection and reopen the same launcher. Failed downloads can be retried.
- Extract the whole ZIP before opening it. Keep the launcher beside `package.json` and the `tools` folder.
- If the folder is read-only, extract a fresh copy into Documents. Do not run the launcher as an administrator to work around this.
- If a company or school blocks downloads, ask its IT team about access to `nodejs.org` and `registry.npmjs.org`.
- If a second launcher window says it is waiting, let the first one finish. Setup installs one copy at a time.

Do not delete the whole `.local` folder to fix an install: it can also contain saved pairing and emulator settings. The launcher repairs missing dependencies itself.

## For maintainers

`tools/bootstrap/node-runtimes.csv` pins the Node version, archive and SHA-256 for each runtime. The launchers download from `https://nodejs.org/dist/` and verify the archive against this manifest before extraction. When updating it, verify hashes against the official release's `SHASUMS256.txt` and run the Mac and Windows setup checks.

Node lives in `.local/runtime`, the npm download cache in `.local/npm-cache`, and dependencies in `node_modules`. The installer uses `npm ci` with `package-lock.json`. Its completion stamp includes the package files, OS, CPU architecture and Node ABI. A changed stamp or failed module import triggers a fresh install. The platform file lock covers runtime and package installation, and is released before the bridge starts.

To install without launching a browser or emulator:

```sh
# Mac, from the extracted folder
bash "Motion Air.command" --install-only
```

```bat
rem Windows Command Prompt, from the extracted folder
"Motion Air.cmd" --install-only
```

With a development Node installation, run `node --test tools/bootstrap/install-test.mjs` and `node tools/bootstrap/check-clean-install.mjs`. The latter creates a temporary folder and exercises the actual launcher with Node removed from PATH. CI runs it on macOS and Windows. Build tools for the optional Ryujinx source build are separate from the bridge runtime.

## Español

Descarga el ZIP del equipo, extrae **toda la carpeta** en Documentos y abre **Motion Air.command** en Mac o **Motion Air.cmd** en Windows. El lanzador descarga Node.js e instala las dependencias del puente automáticamente. No necesitas instalar Node.js, npm ni un gestor de paquetes por separado, ni usar una cuenta de administrador.

Necesitas internet la primera vez y cuando cambien las dependencias. Después se reutilizan los archivos de esa carpeta. Si una descarga falla, revisa la conexión y vuelve a abrir el lanzador. Si abres dos ventanas a la vez, una espera a que termine la otra.

Se admiten Mac con macOS 14 o posterior y PC con Windows 10/11 de 64 bits. Los PC ARM necesitan Windows 11 y usan la emulación de aplicaciones x64. El emulador puede exigir un sistema más reciente; el juego en Windows ARM aún necesita pruebas en un PC físico.

El emulador, los juegos y la app del teléfono se preparan por separado. Sigue el [README en español](../README.es.md). Mantén abierta la ventana mientras juegas y concede los permisos de Accesibilidad en Mac o de red privada en Windows cuando correspondan.

Si algo falla, conserva la carpeta `.local`: también puede guardar conexiones y ajustes del emulador. Usa el [formulario de ayuda](https://github.com/cplus2jules/motion-air/issues/new?template=03-feedback.yml) si el segundo intento falla, e incluye el mensaje de error sin códigos de conexión ni contraseñas.
