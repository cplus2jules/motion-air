// Exports the repo-native mark as aligned Icon Composer layers. No baked lighting.
// Usage: NODE_PATH=/path/to/sharp/node_modules node tools/export-icon.mjs
import { createRequire } from 'node:module';
import { mkdir, writeFile, copyFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
const require = createRequire(import.meta.url);
const sharp = require('sharp');
const root = fileURLToPath(new URL('../', import.meta.url));
const out = path.join(root, 'assets/icon-composer');
await mkdir(out, { recursive: true });
const svg = content => `<svg xmlns="http://www.w3.org/2000/svg" width="1024" height="1024" viewBox="0 0 1024 1024">${content}</svg>`;
const layers = [
  ['01-background', '<rect width="1024" height="1024" fill="#f5f5f7"/>'],
  ['02-left-grip', '<path fill="#ff5364" d="M338 272h136v524H338c-92 0-158-66-158-158V430c0-92 66-158 158-158Z"/>'],
  ['03-right-grip', '<path fill="#20a5eb" d="M550 208h136c92 0 158 66 158 158v208c0 92-66 158-158 158H550V208Z"/>'],
  ['04-controls', '<g fill="#202630"><circle cx="326" cy="433" r="55"/><rect x="303" y="583" width="46" height="128" rx="12"/><rect x="262" y="624" width="128" height="46" rx="12"/><circle cx="698" cy="582" r="55"/><circle cx="698" cy="324" r="26"/><circle cx="642" cy="380" r="26"/><circle cx="754" cy="380" r="26"/><circle cx="698" cy="436" r="26"/></g>'],
  ['05-air', '<g fill="#202630"><rect x="310" y="160" width="164" height="28" rx="14"/><rect x="384" y="104" width="90" height="28" rx="14"/></g>'],
];
for (const [name, drawing] of layers) {
  await writeFile(path.join(out, `${name}.svg`), svg(drawing));
  await sharp(Buffer.from(svg(drawing))).png().toFile(path.join(out, `${name}.png`));
}
const foreground = layers.slice(1).map(([, draw]) => draw).join('');
const combined = layers.map(([, draw]) => draw).join('');
await writeFile(path.join(out, 'motion-air.svg'), svg(combined));
await writeFile(path.join(root, 'public/motion-mark.svg'), svg(foreground));
await sharp(Buffer.from(svg(combined))).png().toFile(path.join(out, 'motion-air-flat.png'));
await sharp(Buffer.from(svg(foreground))).png().toFile(path.join(out, 'motion-air-mark.png'));
const catalog = path.join(root, 'native/iOS/MotionAir/Assets.xcassets');
for (const group of ['AppIcon.appiconset', 'MotionMark.imageset']) await mkdir(path.join(catalog, group), { recursive: true });
await writeFile(path.join(catalog, 'Contents.json'), JSON.stringify({ info: { author: 'xcode', version: 1 } }, null, 2));
await copyFile(path.join(out, 'motion-air-flat.png'), path.join(catalog, 'AppIcon.appiconset/AppIcon.png'));
await copyFile(path.join(out, 'motion-air-mark.png'), path.join(catalog, 'MotionMark.imageset/MotionMark.png'));
await writeFile(path.join(catalog, 'AppIcon.appiconset/Contents.json'), JSON.stringify({ images: [{ filename: 'AppIcon.png', idiom: 'universal', platform: 'ios', size: '1024x1024' }], info: { author: 'xcode', version: 1 } }, null, 2));
await writeFile(path.join(catalog, 'MotionMark.imageset/Contents.json'), JSON.stringify({ images: [{ filename: 'MotionMark.png', idiom: 'universal' }], info: { author: 'xcode', version: 1 } }, null, 2));
console.log(`Exported ${layers.length} aligned PNG layers, SVG sources, flat icon and transparent mark to ${out}`);
