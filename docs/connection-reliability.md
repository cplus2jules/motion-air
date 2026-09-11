# Connection and motion verification — 9 September 2026

The phone connection, sensor capture, and emulator motion subscription are separate signals. A connected phone can send buttons while the regular Ryujinx keyboard profile has no motion input.

## Changes

- Saved Macs use Bonjour to discover the current IPv4 address and port. The resolver uses address bytes, avoiding the `%en0` suffix in Network.framework debug descriptions.
- Address probes run concurrently. A discovered address is persisted only after the existing certificate fingerprint matches. QR claims remain sequential and single use; pairing tokens remain in Keychain.
- A temporary inactive scene, such as Control Center, releases touch controls without ending motion. Entering the background still ends the session.
- An eight-second heartbeat grace period lets brief stalls recover. Unsent motion still coalesces to the newest sample and expires after 100 ms; the bridge still neutralizes stale motion after 250 ms.
- The app shows temporary recovery, retains the last disconnect reason and session duration, and reports whether a motion receiver is listening. The diagnostics screen separately shows sensor delivery and the age of the latest motion at the Mac.
- The Mac-only setup API retains at most 24 connection-stage events: TCP arrival, TLS completion or failure, and pairing response status. It does not record invitation codes, tokens, or request bodies.

## Verified

- Debug simulator and signed Release device builds pass under Swift 6.
- 35 Swift tests and 17 Node tests pass. These cover pairing identity, local address validation and history bounds, cancellation, delayed and missing heartbeats, receiver diagnostics, stale-motion neutralization, ordered input, launcher behavior, and focus handling.
- The production Swift resolver, compiled in a temporary native harness, resolved a changed Bonjour address and port, authenticated the original certificate, rejected a different pin, and honored cancellation.
- The native pinned WebSocket remained open beyond the pairing URLSession's eight-second resource timeout. That timeout was not reproduced as the source of healthy-session disconnects.
- An iPhone simulator paired through actual HTTPS/WSS and reconnected from its saved Mac. Control Center left the session connected. A deliberately paused isolated bridge produced the recovery notice and recovered without ending the session. Backgrounding produced the explicit background disconnect and released held controls after the transition.
- The recovery and missing-receiver interface was inspected at 390 × 844 points. Screenshots and runtime logs are in `.local/ui-verification` and `.local/network-debug`.
- The signed update installed on the physical iPhone without changing its bundle ID or Keychain service.

## Still requires the physical phone

The user's screenshots show the same home 5 GHz SSID on both devices and a QR invitation containing the Mac's current private IPv4 address. The Mac listens on all IPv4 interfaces on TCP 3443, and its firewall is disabled. This rules out the stale hotspot address for that photographed attempt; it does not establish router isolation or a certificate problem.

Check the iPhone's Local Network permission and compare its IP address, subnet mask, and router with the Mac. Correlate a deliberate new pairing attempt with `networkEvents` from the local setup API; do not print the API's invitation or QR fields. No incoming attempt had been recorded immediately after these diagnostics were enabled, so that empty observation alone cannot diagnose the router.

Starting Just Dance in the patched Ryujinx produced a DSU subscription, but that game later returned to its library. Physical sensor delivery into an active song and scoring have not been verified by this change. Use `Motion Air.command` and the window whose title includes `joypad-motion`; the regular Ryujinx profile inspected in this session contains keyboard bindings without motion configuration.
