# DataLoggerApp

Android Bluetooth data logger baseline, written in Kotlin with Jetpack Compose.

The default branch previously contained only the Android starter screen. Earlier Bluetooth work is preserved in Git history before commit `8f1ba49` (which deleted the Bluetooth branch contents). The repair branch implements a new, limited baseline using the same service UUID as that earlier work.

## Implemented

- Android 9+ support, with runtime Bluetooth Connect permission on Android 12+.
- System Bluetooth enable prompt, paired devices and in-app nearby Classic Bluetooth discovery.
- Explicit Master / Slave mode selection. Changing mode closes the old connection.
- Master can maintain up to two independent Classic Bluetooth RFCOMM Slave connections; Slave accepts one Master. Each peer has separate handshake, controls, readings and log file.
- In-app pairing before connection, bond-state tracking and a 60-second wait timeout. Android keeps control of pairing confirmations.
- Slave discoverability through a 120-second system prompt; Scan/Connect permissions on Android 12+, location for discovery on Android 9–11.
- Discovery stops before connection and when leaving the app; receivers and pending connection intents are cleaned up on destruction. System bonds are not removed.
- Send ASCII commands terminated by LF; receive LF or CRLF terminated ASCII data.
- Handle fragmented incoming lines, end of stream, cancellation and connection failures.
- Append timestamped incoming lines to private `received.log`; display the latest 100 lines.
- Close sockets and workers when the activity is destroyed. Rotation closes the connection; reconnect afterwards.

Master-controlled foreground sensor streaming is implemented. Alarms, charts, CSV export and cloud upload remain unimplemented.

## Slave sensor preview

Slave mode can preview real accelerometer (m/s2), gyroscope (rad/s) and light (lx) readings when the phone provides them. Missing sensors are listed as unavailable. Tap **Start sensor preview**; each channel updates at most 10 times per second. Samples contain a sequence number, monotonic sensor timestamp in nanoseconds, local wall-clock receipt timestamp, channel, units and copied values. Preview stops on role change or when leaving the foreground and must be started again explicitly. A connected Master can start and stop transmission using the controls below.

Check on a phone: rotate/move it and cover its light sensor; verify the readings respond. Stop preview and ensure sequence numbers freeze. Start again, switch modes, and background/reopen the app; capture should stay stopped until explicitly restarted. Test a device lacking gyroscope/light support.

## Build

Open the **DataLogger** directory in Android Studio. Install Android SDK platform 35 and use JDK 17.

```bash
cd DataLogger
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

The GitHub Actions workflow runs these checks and uploads a debug APK together with `SHA256SUMS`. After downloading the artifact, verify it with `sha256sum -c SHA256SUMS` before installing.

## Two-phone smoke test

1. Install the same debug APK on two phones supporting Classic Bluetooth. For a fresh-pairing test, remove any previous bond using Android settings.
2. Open the app on each phone, grant the requested permission and enable Bluetooth.
3. Select **SLAVE**, tap **Listen for master**, then **Make discoverable (120 seconds)** and accept the system prompt. Select **MASTER** on the other phone, tap **Find nearby devices**, grant discovery permission, then **Pair / connect** to the slave. Confirm the system pairing prompt on both phones. Connection starts only after the bond completes. Both should show **Connected**.
4. Both phones exchange `HELLO 1 MASTER/SLAVE`. On Master, tap LIST_SENSORS, START, move the Slave phone, and verify sample readings on Master; tap STOP and verify they stop. Unknown commands return ERROR UNKNOWN_COMMAND. Repeat START/STOP and reconnect.
5. Disconnect while listening, connecting, or receiving. Verify the UI returns to Disconnected and a new connection can be started.
6. Deny Connect/Scan/location permissions and retry. Test Bluetooth off, Android 9–11 Location off, a pairing refusal, a 60-second timeout, switching roles while pairing, leaving the app during discovery, and reconnecting to an already bonded peer. After a timeout or role change, a late bond must not automatically connect; tap the device again.
7. Rotate or close the app during pairing and verify the old activity does not connect later. Confirm discovery ends and a fresh scan works.

Service UUID: `c61467fb-8db7-4793-b5bb-42cfacb0184e`. Both endpoints must use this UUID. This does not implement BLE or general-purpose RS232/SPP compatibility. Commands use printable ASCII; incoming lines are capped at 8192 bytes. Unterminated partial lines are discarded on disconnect. Log files remain private to the app and are removed when app data is cleared.

Sensor preview and permission repair passed CI run 37936122498. Sensor transmission passed CI run 38032856580, and integration validation passed CI run 38119836745 (assembleDebug, unit tests and lintDebug). Both produced a debug APK artifact. The two-phone checks above remain unperformed and are required for hardware verification.

Bluetooth implementation inspiration: [Philipp Lackner](https://youtube.com/@philipplackner).

## Sensor protocol v1

Frames are printable ASCII terminated by LF (CRLF accepted), maximum 8192 bytes. Each endpoint sends `HELLO 1 MASTER` or `HELLO 1 SLAVE`; the opposite role must arrive within 10 seconds. Commands before handshake, duplicate handshakes and incompatible roles disconnect. Both phones must run this version.

Master commands: `LIST_SENSORS` returns `SENSORS` plus actual local availability; `START` returns `OK START` or `ERROR NO_SENSORS` / `ERROR BACKGROUND`; `STOP` returns `OK STOP`. Repeated START does not restart capture. Unknown commands return `ERROR UNKNOWN_COMMAND`.

Samples: `SAMPLE sequence channel units timestampNs receivedAtMs value[,value,value]`. Channels/units are accelerometer/m/s2, gyroscope/rad/s, light/lx. Master rejects invalid dimensions, non-finite values, negative timestamps and non-increasing sequence numbers; a successful START resets its sequence expectation. Raw incoming frames remain in private received.log, including rejected frames for diagnostics.

Capture is limited to 10 Hz per available sensor. The outgoing queue holds at most 64 frames; saturation disconnects rather than growing memory. STOP clears queued frames; a write already in flight may finish before OK STOP. Disconnect, role change and leaving the foreground stop capture. Backgrounding the streaming Slave sends `EVENT STOP BACKGROUND`; return to the app and explicitly START again. Rotation disconnects. Physical Bluetooth/sensor verification remains pending.

## Next: Android multi-Slave support

The agreed next milestone is one Android Master controlling two Android Slaves independently, followed by acknowledged commands, group control and recording/export. See [the architecture and command protocol roadmap](docs/android-multi-slave-plan.md) for the proposed protocol v2 and acceptance checks. The first implementation supports up to two Slave connections with protocol v1. Request IDs, sensor/rate selection, group commands and protocol v2 remain planned.

### Three-phone multi-Slave check (not yet performed)

Install the same APK on one Master and two Slaves. Pair both Slaves, listen on each, then connect both from Master. START each independently and verify the correct readings under each device. STOP/disconnect the second while the first continues streaming. Remove the disconnected device card and reconnect it; verify its sequence starts cleanly without affecting the first. Test Bluetooth off, role changes, a peer failing its handshake, and rotation. Rotation still closes connections. Each Master peer logs to its own private received-ADDRESS.log; these files are diagnostics, not recording sessions. The two-device cap is an initial software limit, not a verified hardware capacity. Build and physical verification of this change are pending.

## Role dashboards and sharing

Master has a fleet overview, independent station cards, live channel readings, Start/Stop controls,
capability lookup, pairing/discovery and disconnect controls. Slave has its own connection state,
sensor availability, formatted local readings, capture/transmission state and a Master instruction card.
Local preview controls are available when disconnected; a connected Slave can disconnect to end participation.
Light and dark themes use a consistent teal/slate palette. There are no charts or recording/export controls yet.

Slave receives no fleet roster or other-Slave telemetry. Master may explicitly send a per-device instruction
using `SHARE text` (1–160 printable ASCII characters); Slave acknowledges `OK SHARE`.
`CLEAR_SHARED` revokes it and receives `OK CLEAR_SHARED`. Invalid text returns
`ERROR INVALID_SHARED_TEXT`. These commands are accepted only after the existing Master role handshake.
Instruction display state clears on disconnect or role change; diagnostic logs can retain received commands.
Revocation updates the live card; it is not deletion from diagnostic history.
This is a narrow sharing feature, not a general permission system or cryptographic authorization.
Android pairing and the role handshake remain the connection boundary. Existing protocol-v1 builds without
this extension respond UNKNOWN_COMMAND, visible in the Master's device status.

UI/device checklist (pending):
- Check light/dark themes, large font sizes, scrolling and narrow screens on both roles.
- Connect two Slaves and verify each card's readings and Start/Stop target the correct device.
- Share different instructions to A and B; verify neither sees the other's instruction or sensor readings.
- Revoke A's instruction while B's remains; disconnect/reconnect A and verify its card is empty.
- Test empty/invalid instructions, unsupported older peers, missing sensors and background capture stops.
- Confirm Slave has no fleet roster, remote-device controls or raw command console.
