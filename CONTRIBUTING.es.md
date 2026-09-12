# Cómo contribuir a Motion Air

[Read in English](CONTRIBUTING.md) · [Volver a la instalación](README.es.md)

Puedes ayudar probando la app en tu teléfono, señalando una pantalla confusa, mejorando las instrucciones, traduciendo o proponiendo un cambio de código. No necesitas programar para contribuir.

## Elige por dónde empezar

| Quiero... | Abre este enlace |
| --- | --- |
| Reportar algo que no funciona | [Reporte de error](https://github.com/cplus2jules/motion-air/issues/new?template=01-bug-report.yml) |
| Proponer una función | [Propuesta de función](https://github.com/cplus2jules/motion-air/issues/new?template=02-feature-request.yml) |
| Dar mi opinión o pedir ayuda | [Formulario de opiniones](https://github.com/cplus2jules/motion-air/issues/new?template=03-feedback.yml) |
| Ver si alguien ya lo reportó | [Issues existentes](https://github.com/cplus2jules/motion-air/issues) |
| Mejorar código o documentación | [Enviar un pull request](#enviar-un-pull-request) |

Puedes escribir en español o inglés. Inicia sesión en GitHub. Este repositorio es privado, así que necesitas acceso para verlo y usar sus formularios. Tu respuesta se publica como un issue con tu cuenta de GitHub y es visible para quienes tienen acceso al repositorio.

## Reportar un problema

1. Busca en los issues existentes. Si encuentras el mismo problema, añade allí el modelo de tu teléfono y tu experiencia.
2. Abre el formulario de errores y explica qué pasó, qué esperabas y qué pasos causaron el fallo.
3. Añade el modelo del teléfono, la versión de iOS o Android, el sistema del equipo y la versión de Motion Air si los conoces. Puedes escribir "No lo sé".
4. Adjunta una captura o el texto exacto del error si ayuda. Oculta antes códigos QR de conexión, códigos de conexión, contraseñas y datos privados.

Para problemas de movimiento, indica si funcionan los botones, si activaste **Enable Motion** o **Activar movimiento** y qué estado de conexión muestra la app. Menciona el juego y el emulador, la Wi-Fi o punto de acceso, y si el problema empezó al bloquear el teléfono, cambiar de app o reconectar. Puedes dejar en blanco lo que no sepas.

Un teléfono conectado, un mando que se mueve en el emulador y una buena puntuación en Just Dance son resultados distintos. Indica cuál comprobaste. Consulta el [estado de las pruebas de movimiento](docs/motion-implementation-status.md).

## Ayudar sin cambiar código

- Prueba la [versión más reciente](https://github.com/cplus2jules/motion-air/releases/latest) e indica qué teléfono y equipo usaste y qué funcionó.
- Señala el paso de instalación donde te atascaste o propone una explicación más clara.
- Comparte capturas de la app actual. Indica la plataforma, versión e idioma; usa nombres como `iphone-pairing.png`.
- Actualiza las instrucciones en inglés y español cuando puedas. Conserva los nombres reales de los botones; las capturas de iPhone muestran la interfaz en inglés.

Usa el formulario de opiniones si quieres proponer un cambio de texto sin editar los archivos.

## Enviar un pull request

Un pull request es una propuesta de cambio que el responsable del proyecto puede revisar antes de incorporarla.

1. Para una función grande o un rediseño, abre primero un issue con el problema y tu propuesta. Una corrección pequeña de texto puede ir directamente a un pull request.
2. Crea una rama para tu cambio. Si tienes permiso de escritura, créala en este repositorio. Si no, usa un fork si está permitido. Si ninguna opción está disponible, describe el cambio en un issue.
3. Haz un cambio concreto. Para documentación, el botón del lápiz de GitHub permite editar un archivo en el navegador y proponer el cambio.
4. Ejecuta las comprobaciones correspondientes de abajo. Revisa las capturas si cambias la interfaz y los enlaces si cambias documentación.
5. Abre **Pull requests → New pull request**, elige `main` como base y tu rama para comparar, y rellena la plantilla. Usa un borrador si todavía queda trabajo.
6. Explica qué cambió y por qué. Enlaza el issue relacionado, añade capturas para cambios de interfaz e indica qué probaste. Responde a los comentarios y sube las siguientes correcciones a la misma rama.

Para trabajar localmente, clona el repositorio o tu fork y crea una rama:

```bash
git clone https://github.com/cplus2jules/motion-air.git
cd motion-air
git switch -c nombre-del-cambio
```

Sustituye `nombre-del-cambio` por una descripción breve. Si usas un fork, usa su URL. La [guía de desarrollo](docs/development.md) explica la preparación y las carpetas del proyecto.

## Comprobar el cambio

| Área modificada | Comprobaciones útiles |
| --- | --- |
| README, instrucciones o capturas | Abre el archivo renderizado en GitHub, sigue los enlaces y comprueba que carguen las imágenes y coincidan los nombres de los botones. |
| Puente del equipo o lanzadores | Usa Node.js 22 o posterior, ejecuta `npm ci`, después `npm test` y los scripts `npm run test:*` correspondientes de `package.json`. |
| App de Android | En `android/`, ejecuta `./gradlew assembleDebug testDebugUnitTest lintDebug` con JDK 17 y SDK 35. En Windows usa `gradlew.bat`. Para cambios de conexión o ciclo de vida, ejecuta también la [prueba del emulador Android](docs/development.md#android). |
| App de iPhone o JoypadCore | Compila el esquema **MotionAir** en Xcode y ejecuta `swift test --package-path native/Packages/JoypadCore` desde la raíz con las herramientas de Xcode. Consulta [desarrollo de iPhone](docs/development.md#iphone). |
| Scripts o flujos de publicación | Ejecuta las pruebas de Python de la [guía de desarrollo](docs/development.md#iphone) y revisa la [guía de publicación](docs/mobile-releases.md). |

Elige comprobaciones que cubran el cambio. Si no puedes probar una plataforma, indícalo en el pull request. Los cambios de movimiento requieren pruebas con un teléfono real, además de las automáticas, antes de afirmar que funcionan en el juego. Puedes usar las compilaciones de desarrollo de Android y el flujo de iPhone sin firma; un pull request no necesita las claves de publicación.

## Colaborar

Describe el comportamiento que observaste y mantén una conversación respetuosa. Da detalles suficientes para que otra persona pueda repetir el resultado. Separa los cambios que no estén relacionados en distintos pull requests.

Conserva la atribución de Joypad Air y la licencia MIT. Mantén los identificadores de compatibilidad salvo que el cambio incluya un plan de migración. No subas `.local/`, datos de conexión, contraseñas, almacenes de claves, perfiles de aprovisionamiento ni archivos de juegos. Usa capturas y registros que muestren solo lo necesario.
