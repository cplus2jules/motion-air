# Multiplayer / Multijugador

## English

The local 0.3.0 phone apps and matching computer/emulator changes support **one to six phones in one Motion Air server**. Each phone is assigned a different player, and the Just Dance preset exposes **six separate right Joy-Cons**, not one paired Joy-Con controller. Players select their own characters using their own phone. Character availability and player limits still depend on the game and song.

Update every phone, the computer bridge, and the patched emulator together. Older iPhone builds accept only Player 1; older emulator builds do not consume the separate button/stick streams. These changes have not been published to the download links yet.

1. Finish/save the game and quit Ryujinx. Stop the previous Motion Air Terminal window with Ctrl+C.
2. Build the matching emulator with `npm run ryujinx:build` on Mac or `tools/ryujinx-build/build-windows.ps1` on Windows. The build selects its output for the launcher after contract checks pass.
3. For an existing Mac setup, run `node tools/ryujinx-setup.mjs --config-dir .local/ryujinx-motion-data --just-dance --patched --players 6 --controller-input`. This backs up the previous configuration. Fresh `npm run ryujinx:prepare` setups and Windows setup create six profiles.
4. Open **Motion Air.command** or **Motion Air.cmd**. Connect each phone to the same saved computer. New phones scan a fresh one-use QR invitation each; existing pairings remain valid.
5. Check each phone's **Connected · Player N** label and the player number on the computer pairing page. Open Just Dance in the selected emulator, join from each phone, and choose separate characters. Enable Motion separately on each phone after joining; reconnects start with Motion off.

A seventh phone sees a full-server message. Selecting an occupied slot does not disconnect its current player. A reconnect uses the phone's previous number when free, otherwise another free slot. A duplicate connection using the same saved phone credential is rejected until its previous session closes; a lost phone heartbeat expires after eight seconds.

### Routing and validation

| Players | Local DSU UDP port | DSU slots | Ryujinx profiles |
| --- | --- | --- | --- |
| 1–4 | 26760 | 0–3 | Player1–Player4, JoyconRight |
| 5–6 | 26761 | 0–1 | Player5–Player6, JoyconRight |

Both ports belong to the same Node bridge. Standard DSU has four slots per endpoint, so the second endpoint keeps each phone's sensor and input stream separate. A custom `PAIRING_DSU_PORT` also reserves its next port. Both remain loopback-only in paired mode.

The paired launcher enables `DSU_CONTROLS=1`. Full button and stick snapshots use the standard DSU data fields. The optional four-byte Motion Air extension at offsets 100–103 contains `4D 41 01 flags`, with SL in bit 0 and SR in bit 1; the standard 100-byte prefix and sensor offsets are preserved. The matching emulator's `use_controller_input: true` replaces desktop keyboard input with that player's snapshot. Control snapshots repeat every 20 ms without advancing sensor timestamps. Disconnect releases controls, stale controls expire in the emulator after 250 ms, and the existing 250 ms sensor watchdog remains active.

Verification for this change: authenticated six-phone WebSocket/HTTPS/UDP integration; seventh-phone rejection; occupied-slot protection; per-player buttons, sticks, motion and receiver counts; independent disconnect/rejoin; and the real emulator's game-facing input and six-axis contracts for six Joy-Cons. Bridge regressions, Swift tests, iPhone device builds, and Android build/unit/lint checks also pass. A live two-iPhone check in Just Dance 2022 showed two connected players with separate on-screen scoring feedback. This confirms the two-player path; a physical six-phone session and scoring accuracy remain untested. Windows runtime/gameplay has not been checked on a physical Windows computer.

## Español

Las apps locales **0.3.0**, junto con el puente y el emulador actualizados, admiten **de uno a seis teléfonos en un solo servidor**. Cada teléfono tiene su propio jugador y el perfil de Just Dance crea **seis Joy-Con derechos independientes**. Cada jugador puede elegir su personaje desde su teléfono; la disponibilidad depende del juego y de la canción.

Actualiza todos los teléfonos, el puente y el emulador juntos. Las versiones anteriores de iPhone solo aceptan Jugador 1, y los emuladores anteriores no leen los botones y sticks independientes. Estos cambios todavía no están publicados en los enlaces de descarga.

1. Guarda la partida y cierra Ryujinx. Detén la ventana anterior de Motion Air con Ctrl+C.
2. Compila el emulador compatible con `npm run ryujinx:build` en Mac o `tools/ryujinx-build/build-windows.ps1` en Windows.
3. Si ya tienes una instalación en Mac, ejecuta `node tools/ryujinx-setup.mjs --config-dir .local/ryujinx-motion-data --just-dance --patched --players 6 --controller-input`. Se guarda una copia de la configuración. La preparación inicial y la configuración de Windows crean seis perfiles.
4. Abre **Motion Air.command** o **Motion Air.cmd**. Conecta cada teléfono al mismo equipo guardado. Cada teléfono nuevo escanea un QR de un solo uso distinto; los emparejamientos guardados siguen siendo válidos.
5. Comprueba **Connected · Player N** en iPhone o **Conectado · Jugador N** en Android y el número mostrado en el equipo. Abre Just Dance, une cada jugador y elige personajes distintos. Activa Motion por separado en cada teléfono; al reconectar empieza desactivado.

Un séptimo teléfono recibe un aviso de servidor lleno, sin expulsar a nadie. Un puesto ocupado rechaza otra conexión. Al reconectar se intenta recuperar el número anterior si está libre; si no, se asigna otro libre. Una conexión duplicada del mismo teléfono espera a que termine la anterior. El plazo de respuesta del teléfono es de ocho segundos.

Los jugadores 1–4 usan los puestos DSU 0–3 del puerto local 26760; los jugadores 5–6 usan 0–1 del puerto 26761. Son dos salidas UDP del mismo servidor. Si cambias `PAIRING_DSU_PORT`, también se utiliza el puerto siguiente. Los botones y sticks se repiten cada 20 ms sin alterar el reloj del sensor; los controles sin datos actuales caducan a los 250 ms.

Las pruebas automáticas verifican seis teléfonos autenticados, flujos separados, rechazo del séptimo, reconexión y entradas independientes en el emulador. También pasan las pruebas del puente, Swift y compilación de iPhone, y compilación/pruebas/lint de Android. Una comprobación en vivo con dos iPhone en Just Dance 2022 mostró dos jugadores conectados con valoraciones independientes en pantalla. Quedan pendientes una sesión física con seis teléfonos y la precisión de la puntuación. No se ha validado el funcionamiento en un equipo Windows físico.
