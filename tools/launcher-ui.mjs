const clean = value => String(value).replace(/[\x00-\x1f\x7f]/g, ' ');

export function createLauncherUI({ spanish = false, output = process.stdout, color = output.isTTY && !process.env.NO_COLOR } = {}) {
  const words = (en, es) => spanish ? es : en;
  const tint = (code, text) => color ? `\x1b[${code}m${text}\x1b[0m` : text;
  const line = text => output.write(`${text}\n`);
  return {
    start() {
      line('');
      line(`  ${tint('1', 'MOTION AIR')}   ${tint('36', '[ + ]')} ${tint('31', '[ o ]')}`);
      line(`  ${words('One more round.', 'Siempre hay otra partida.')}`);
      line(`  ${'─'.repeat(42)}`);
    },
    step(label, translation) { line(`  ${tint('36', '›')} ${words(label, translation)}`); },
    ready(url, reused = false) {
      line('');
      line(`  ${tint('32;1', words('READY FOR YOUR PHONE', 'LISTO PARA TU TELÉFONO'))}`);
      line(`  ${words('Pair your phone', 'Empareja tu teléfono')}  ${clean(url)}`);
      line('');
      line(`  ${words('1. Open Motion Air on your phone.', '1. Abre Motion Air en el teléfono.')}`);
      line(`  ${words('2. Scan the computer QR and confirm its name.', '2. Escanea el QR y confirma el nombre del equipo.')}`);
      line(`  ${words('3. Open your game. Enable Motion to dance.', '3. Abre el juego. Activa Enable Motion para bailar.')}`);
      line('');
      line(`  ${words(reused ? 'Keep the original launcher window open.' : 'Keep this launcher window open while playing.', reused ? 'Mantén abierta la ventana original del lanzador.' : 'Mantén esta ventana del lanzador abierta al jugar.')}`);
      line(`  ${words(reused ? 'Use Ctrl+C in the original launcher to stop the bridge.' : 'Ctrl+C stops the controller bridge.', reused ? 'Usa Ctrl+C en la lanzador original para detener el puente.' : 'Ctrl+C detiene el puente del mando.')}`);
      line(`  ${words('Quit Ryujinx from its own window.', 'Cierra Ryujinx desde su propia ventana.')}`);
      line(`  ${'─'.repeat(42)}`);
    },
    stopped() { line(`\n  ${tint('1', words('Controller stopped.', 'Mando detenido.'))} ${words('See you next round.', 'Hasta la próxima partida.')}`); },
    error(message) { line(`\n  ${tint('31;1', words('COULD NOT START', 'NO SE PUDO INICIAR'))}\n  ${clean(message)}\n`); },
  };
}
