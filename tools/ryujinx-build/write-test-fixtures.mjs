import { writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { buildProfile } from '../../server/ryujinx.js';
import { encodeDataResponse } from '../../server/dsu/packets.js';
const dir = process.env.MOTION_AIR_BUILD;
if (!dir) throw new Error('MOTION_AIR_BUILD is required.');
const players = Array.from({ length: 6 }, (_, i) => buildProfile(i + 1, 'JoyconRight', true, { deadzone: 0, controllerInput: true }));
writeFileSync(join(dir, 'players.json'), JSON.stringify(players));
for (let i = 0; i < 6; i++) {
  const controls = { connected: true, buttons: [['a'], ['b'], ['x'], ['y'], ['sl'], ['sr']][i], sticks: { R: { x: (i + 1) / 6, y: -0.5 } } };
  const sample = { ax: (i + 1) / 10, ay: -1, az: 0, pitch: (i + 1) * 10, yaw: 0, roll: 0, tsUs: 2000000 };
  writeFileSync(join(dir, `warmup-${i}.bin`), encodeDataResponse(10 + Math.floor(i / 4), i % 4, 1, { ...sample, tsUs: 1983334 }, controls));
  writeFileSync(join(dir, `player-${i}.bin`), encodeDataResponse(10 + Math.floor(i / 4), i % 4, 2, sample, controls));
  writeFileSync(join(dir, `release-${i}.bin`), encodeDataResponse(10 + Math.floor(i / 4), i % 4, 3, sample, { ...controls, buttons: [], sticks: {} }));
}
writeFileSync(join(dir,'profile.json'),JSON.stringify(buildProfile(1,'JoyconRight',true,{deadzone:0})));
for(let i=0;i<2;i++)writeFileSync(join(dir,`motion-${i}.bin`),encodeDataResponse(1,0,i+1,{ax:0.25,ay:-0.5,az:0.75,pitch:90,yaw:-60,roll:-30,tsUs:1000000+16666*i}));
