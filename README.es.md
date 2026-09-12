# Motion Air

Usa tu **Android o iPhone como mando inalámbrico** para juegos con movimiento en **Mac o Windows**.

[Descargar Motion Air](https://github.com/cplus2jules/motion-air/releases) · [Read in English](README.md)

Motion Air sigue en pruebas. Hay comprobaciones automáticas de los botones y del envío de movimiento; la precisión de las puntuaciones de Just Dance necesita pruebas con un teléfono y un juego reales.

## Qué necesitas

- Un teléfono con Android 8 o posterior, o un iPhone con iOS 17 o posterior.
- Un Mac o PC con Windows, conectado a la misma Wi-Fi que el teléfono.
- [Node.js](https://nodejs.org/en/download) instalado en el equipo. Elige el instalador **LTS**, versión 22 o posterior.
- Tu emulador y juego instalados. **Just Dance necesita la versión de Ryujinx con soporte de movimiento.** Configurar solo los botones no activa el movimiento. Sigue la [guía de Mac](docs/local-device-setup.md) o la [guía de Windows](docs/windows-setup.md) para preparar el emulador.

No se incluyen juegos, emuladores, firmware ni claves de juegos.

## 1. Descarga los archivos

Usa estos enlaces para descargar las apps más recientes. También están en **Assets** de la [página de la versión](https://github.com/cplus2jules/motion-air/releases/latest).

| Para | Archivo |
| --- | --- |
| Tu Mac o PC con Windows | [Descargar para el equipo](https://github.com/cplus2jules/motion-air/releases/latest/download/MotionAir-Computer.zip) |
| Tu Android | [Descargar APK de Android](https://github.com/cplus2jules/motion-air/releases/latest/download/MotionAir-Android.apk) |
| Tu iPhone | [Descargar IPA de iPhone](https://github.com/cplus2jules/motion-air/releases/latest/download/MotionAir-iPhone-unsigned.ipa) |

Necesitas el ZIP del equipo **y** una app para el teléfono. Puedes ignorar los demás archivos. Inicia sesión en GitHub si te lo pide; este repositorio privado requiere acceso.

## 2. Instala la app del teléfono

### Android

1. Descarga el APK en tu teléfono y ábrelo desde **Descargas**.
2. Si Android lo pide, permite que el navegador o gestor de archivos **instale aplicaciones desconocidas** para esta instalación.
3. Pulsa **Instalar** y abre **Motion Air**.

### iPhone

El iPhone requiere una instalación externa, llamada **sideloading**. Una herramienta en el ordenador firma la app con tu cuenta de Apple y la instala. Abrir el IPA directamente en el teléfono no lo instala.

1. Configura tu herramienta, como [AltStore Classic](https://faq.altstore.io/) o [Sideloadly](https://sideloadly.io/), siguiendo su guía.
2. Importa el IPA descargado y sigue los pasos para firmarlo e instalarlo.
3. Abre **Motion Air** en el iPhone y permite el acceso a la **red local** cuando lo pida.

Tu método de firma determina cuándo debes renovar la app. Consulta los [detalles de instalación en iPhone](docs/ios-releases.md#download-a-build).

La app comienza con una guía breve. Estas capturas muestran Android; la app de iPhone sigue las mismas tres lecciones.

<img src="docs/images/android-welcome-es.png" alt="Bienvenida de Motion Air en Android, con los botones Omitir y Vamos a probar" width="260">

## 3. Abre Motion Air en el equipo

1. Extrae el ZIP del equipo en una carpeta que puedas encontrar después.
2. Dentro de esa carpeta, haz doble clic en:
   - **Mac:** `Motion Air.command`
   - **Windows:** `Motion Air.cmd`
3. Espera a que termine la primera instalación. Se abrirá una página con el QR de conexión.
4. Mantén abierta la ventana de Terminal o de comandos mientras juegas.

En Mac, permite Terminal en **Ajustes del Sistema → Privacidad y seguridad → Accesibilidad** para que los botones controlen el juego. La [guía de Mac](docs/local-device-setup.md) explica la preparación del emulador; la [guía de Windows](docs/windows-setup.md) explica cómo seleccionar tu `Ryujinx.exe`.

Deja el lanzador dentro de su carpeta. Puedes crear un acceso directo o alias para el Escritorio.

## 4. Conecta el teléfono

Al abrir la app, una guía breve te permite practicar el stick, A y B, y probar el bloqueo para bailar. Al terminar, elige **Conectar mi equipo** en Android o **Pair my Mac** en iPhone. Si omites la guía, usa el botón azul de conexión. Puedes volver a verla desde los ajustes.

1. Usa la misma Wi-Fi en ambos dispositivos. Evita una red de invitados.
2. En Motion Air, escanea el QR de tu ordenador. Permite la cámara si lo pide.
3. Comprueba el nombre del equipo y confirma la conexión.
4. Haz clic en la ventana del juego y prueba los botones del teléfono.

Elige **Escanear QR del equipo**, o **Pegar código de conexión** si no puedes usar la cámara.

<img src="docs/images/android-pairing-es.png" alt="Pantalla de conexión de Android con Escanear QR del equipo y Pegar código de conexión" width="260"> <img src="docs/images/android-controller-offline-es.png" alt="Mando Android antes de conectar: stick y botones atenuados, con Conectar un equipo en la parte inferior" width="260">

El stick y los botones del mando permanecen atenuados hasta conectar. Para probar el stick sin un equipo, abre **Ajustes → Ver guía de bienvenida → Vamos a probar** en Android.

Solo necesitas escanear el QR la primera vez. Después, selecciona tu equipo guardado. Si cambia su dirección y no puedes reconectar, escanea un QR nuevo.

## 5. Activa el movimiento

1. Abre el juego con el emulador preparado para movimiento.
2. Pulsa **Activar movimiento** o **Enable Motion** en el teléfono.
3. Sujeta bien el teléfono en la mano derecha, en vertical y con la parte superior hacia los dedos.
4. Pulsa **Bloqueo para bailar** o **Dance Lock** para evitar pulsaciones accidentales. Mantén pulsado el botón de desbloqueo para volver al mando.

Mantén Motion Air abierto. Cambiar de app o bloquear la pantalla detiene la conexión. Tras reconectar, activa el movimiento de nuevo.

El bloqueo para bailar oculta los controles. **Enviando movimiento** indica que el receptor del equipo recibe tus movimientos; no garantiza una puntuación en el juego. La captura siguiente muestra esta pantalla en inglés: **Hold to unlock** es el botón que debes mantener pulsado para desbloquear.

<img src="docs/images/android-dance-lock.png" alt="Bloqueo para bailar en Android, en inglés, con Sending motion y Hold to unlock" width="260">

## Si algo falla

| Problema | Qué probar |
| --- | --- |
| El teléfono no conecta | Usa la misma Wi-Fi, deja abierto el lanzador y escanea un QR nuevo. En iPhone, revisa el permiso de red local. Una red de invitados puede impedir la conexión. |
| Los botones no responden | Haz clic en la ventana del juego. En Mac, revisa el permiso de Accesibilidad de Terminal. |
| Conecta, pero no hay movimiento | Activa el movimiento y comprueba que el emulador compatible esté abierto. El juego también debe recibir los datos. |
| El movimiento se detuvo al salir de la app | Abre Motion Air, reconecta y activa el movimiento de nuevo. |
| Android no instala la actualización | Usa el APK de la versión más reciente. Una app antigua de desarrollo puede tener otra firma. Consulta [actualizaciones de Android](android/README.md#updating). |
| La app de iPhone no abre | Comprueba si tu herramienta necesita renovar o volver a firmar la app. |
| El lanzador no encuentra Node.js | Instala Node.js LTS y vuelve a abrir el lanzador. |

Más ayuda: [conexión](docs/connection-reliability.md) · [estado de las pruebas de movimiento](docs/motion-implementation-status.md).

## Desarrollo

[Compilar y probar](docs/development.md) · [Publicar las dos apps](docs/mobile-releases.md) · [Otras opciones de mando](docs/controller-options.md).

## Créditos

Motion Air está basado en [Joypad Air de David García (mindavidev)](https://github.com/mindavidev/joypad-air). Esta continuación independiente conserva el copyright y la licencia MIT originales. Consulta los [agradecimientos](ACKNOWLEDGEMENTS.md) y la [licencia](LICENSE).
