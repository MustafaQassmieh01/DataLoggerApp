# DataLogger five-day implementation plan (October 8–12, 2026)

Work branch: `fix/bluetooth-baseline`, draft PR #1. Inspect current remote source and CI before each stage; keep the PR draft until build and physical-device verification are complete. Do not merge automatically.

## Day 1 — October 8: roles and build baseline

Add explicit Master / Slave UI selection, disconnect on mode changes, and a shortcut to system pairing. Master shows connection targets; slave shows listener controls. Fix SDK setup failure caused by requesting the removed `tools` package. Mode selection is a local UI choice; protocol role negotiation remains for Day 4.

## Day 2 — October 9: pairing before connection

Implement in-app Classic Bluetooth discovery, slave discoverability via the Android system prompt, and bond-state tracking. Request Scan/Connect on Android 12+ and location permission for discovery on Android 9–11. Use `createBond()` and connect only after BOND_BONDED; handle refusal, cancellation and timeout. Cancel discovery before RFCOMM connection, unregister receivers, and keep all system pairing confirmations visible.

Day 2 implementation: discovery, discoverability prompt, permissions, bond gating and timeout added. System pairing confirmation is preserved. Local Gradle downloads remain blocked; validate via PR CI. Physical checklist is in README. Day 1 CI run 37730441991 passed all Android checks.

## Day 3 — October 10: real sensor collection

Capture available accelerometer, gyroscope and light readings on the slave. Report unsupported sensors explicitly; do not invent temperature/humidity data. Add typed sensor samples with channel, units, values, timestamps and sequence numbers. Register/unregister sensor listeners with lifecycle and streaming state; limit sample rates. Add local slave previews and meaningful protocol/sensor-model tests.

Day 3 brought forward on October 9: local sensor preview, availability reporting, typed samples, per-channel 10 Hz cap and foreground/role cleanup implemented. Transmission remains Day 4. Also repaired Day 2 lint failure by declaring and requesting legacy coarse/fine location together. Device sensor validation remains pending.

## Day 4 — October 11: master-controlled transmission

Implement role handshake and documented LIST_SENSORS / START / STOP commands with replies and errors. Slave sends framed samples; master validates, displays and logs them. Rate-limit and bound outgoing buffers to avoid slow-peer memory growth. Stop sensor capture and queued transmission on disconnect or mode change. Test split frames, malformed input, unknown commands and reconnects. Use a documented protocol, not an unverified claim of RS232 compatibility.

Day 4 implemented early on October 10: role/version handshake, LIST_SENSORS/START/STOP, real sensor frames and Master validation/display, bounded outgoing queue and disconnect/background cleanup. Added protocol and frame tests. Local Gradle bootstrap is network-blocked; CI and two-phone verification remain required.

## Day 5 — October 12: integration and verification

Resolve build, unit-test and lint failures. Verify permission denial, Bluetooth off, pairing refusal, missing sensors, disconnects, repeated starts/stops and mode switches. Produce a debug APK when CI succeeds and a two-phone test checklist; clearly separate automated checks from hardware tests the user must perform. Update README and PR description to describe actual implemented behavior and remaining limitations.

Continue the earliest unfinished prerequisite before moving to a later stage. Each stage ends with focused commits on the work branch, a brief progress report, CI status and a concrete next check. Preserve existing user changes and inspect remote branch head before publishing.
