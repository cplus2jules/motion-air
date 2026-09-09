# Motion Air icon

Open **Motion Air.icon** in Apple Icon Composer. It is already assembled and is the icon source used by the iPhone target. Default, Dark, and Mono appearances were inspected in Icon Composer on September 9, 2026.

The mark extends Joypad Air’s red/blue controller identity, continued in Motion Air. Every source PNG has a 1024 × 1024 canvas and the same origin. Do not crop individual layers, round the canvas corners, or add shadows or highlights before import.

| Back to front | File | Contents |
| --- | --- | --- |
| Background | `01-background.png` | Opaque #F5F5F7 plate |
| Left grip | `02-left-grip.png` | Red shell on transparency |
| Right grip | `03-right-grip.png` | Blue shell on transparency |
| Controls | `04-controls.png` | Sticks, D-pad and face buttons on transparency |
| Air accent | `05-air.png` | Two motion dashes on transparency |

The Composer document uses its native background fill, matched to the background PNG, plus four foreground image layers. This lets Apple generate the dark and tinted background treatments. The background PNG is supplied for other editors and flat compositions. Layers remain at 100% scale, x: 0, y: 0. Apple’s material creates the depth, lighting, and edge treatment.

To rebuild the document manually, set its background fill to #F5F5F7, then import foreground layers 02–05 in order. In the sidebar, 05 is at the top and 02 at the bottom. Keep Liquid Glass effects enabled. Preview Default, Dark, and Mono, including their small previews, before saving.

`motion-air-flat.png` is the opaque portable composite. `motion-air-mark.png` is the transparent mark. Matching SVGs retain editable shapes. The app uses `Motion Air.icon`; Xcode generates older-system icons from it. The asset catalog also retains a flat icon for separate consumers.

Regenerate the PNGs and app/web mark with `node tools/export-icon.mjs`. The exporter needs the `sharp` package; it can be resolved through NODE_PATH when using a shared tooling installation. It does not overwrite the Composer document. After changing source shapes, replace the corresponding images inside Composer.

[Apple’s Icon Composer workflow](https://developer.apple.com/documentation/Xcode/creating-your-app-icon-using-icon-composer)
