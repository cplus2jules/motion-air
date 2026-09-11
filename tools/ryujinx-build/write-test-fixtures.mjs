import { writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { buildProfile } from '../../server/ryujinx.js';
import { encodeDataResponse } from '../../server/dsu/packets.js';
const directory = process.env.MOTION_AIR_BUILD;
if (!directory) throw new Error('MOTION_AIR_BUILD is required.');
writeFileSync(join(directory, 'profile.json'), JSON.stringify(buildProfile(1, 'JoyconRight', true, { deadzone: 0 })));
for (let i = 0; i < 2; i++) writeFileSync(join(directory, `motion-${i}.bin`), encodeDataResponse(1, 0, i + 1, {
  ax: 0.25, ay: -0.5, az: 0.75, pitch: 90, yaw: -60, roll: -30, tsUs: 1000000 + 16666 * i,
}));
