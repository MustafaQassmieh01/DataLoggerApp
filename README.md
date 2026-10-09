# DataLoggerApp

Android Bluetooth data logger baseline, written in Kotlin with Jetpack Compose.

The default branch previously contained only the Android starter screen. Earlier Bluetooth work is preserved in Git history before commit `8f1ba49` (which deleted the Bluetooth branch contents). The repair branch implements a new, limited baseline using the same service UUID as that earlier work.

## Implemented

- Android 9+ support, with runtime Bluetooth Connect permission on Android 12+.
- System Bluetooth enable prompt, paired devices and in-app nearby Classic Bluetooth discovery.
- Explicit Master / Slave mode selection. Changing mode closes the old connection.
- One Classic Bluetooth RFCOMM connection: connect as master or listen as slave.
- In-app pairing before connection, bond-state tracking and a 60-second wait timeout. Android keeps control of pairing confirmations.
- Slave discoverability through a 120-second system prompt; Scan/Connect permissions on Android 12+, location for discovery on Android 9–11.
- Discovery stops before connection and when leaving the app; receivers and pending connection intents are cleaned up on destruction. System bonds are not removed.
- Send ASCII commands terminated by LF; receive LF or CRLF terminated ASCII data.
- Handle fragmented incoming lines, end of stream, cancellation and connection failures.
- Append timestamped incoming lines to private `received.log`; display the latest 100 lines.
- Close sockets and workers when the activity is destroyed. Rotation closes the connection; reconnect afterwards.

This is a transport/logger baseline. Bluetooth sensor transmission, command interpretation, sensor channels, alarms, charts, CSV export and cloud upload are **not implemented**. Listening does not automatically generate sensor readings or reply to commands.

## Slave sensor preview

Slave mode can preview real accelerometer (m/s2), gyroscope (rad/s) and light (lx) readings when the phone provides them. Missing sensors are listed as unavailable. Tap **Start sensor preview**; each channel updates at most 10 times per second. Samples contain a sequence number, monotonic sensor timestamp in nanoseconds, local wall-clock receipt timestamp, channel, units and copied values. Preview stops on role change or when leaving the foreground and must be started again explicitly. These samples are not sent over Bluetooth yet.

Check on a phone: rotate/move it and cover its light sensor; verify the readings respond. Stop preview and ensure sequence numbers freeze. Start again, switch modes, and background/reopen the app; capture should stay stopped until explicitly restarted. Test a device lacking gyroscope/light support.

## Build

Open the **DataLogger** directory in Android Studio. Install Android SDK platform 35 and use JDK 17.

```bash
cd DataLogger
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

The GitHub Actions workflow runs these checks and uploads a debug APK.

## Two-phone smoke test

1. Install the same debug APK on two phones supporting Classic Bluetooth. For a fresh-pairing test, remove any previous bond using Android settings.
2. Open the app on each phone, grant the requested permission and enable Bluetooth.
3. Select **SLAVE**, tap **Listen for master**, then **Make discoverable (120 seconds)** and accept the system prompt. Select **MASTER** on the other phone, tap **Find nearby devices**, grant discovery permission, then **Pair / connect** to the slave. Confirm the system pairing prompt on both phones. Connection starts only after the bond completes. Both should show **Connected**.
4. Send `PING` from the master. The slave should display `PING`. Send a reply from the slave and verify it on the master.
5. Disconnect while listening, connecting, or receiving. Verify the UI returns to Disconnected and a new connection can be started.
6. Deny Connect/Scan/location permissions and retry. Test Bluetooth off, Android 9–11 Location off, a pairing refusal, a 60-second timeout, switching roles while pairing, leaving the app during discovery, and reconnecting to an already bonded peer. After a timeout or role change, a late bond must not automatically connect; tap the device again.
7. Rotate or close the app during pairing and verify the old activity does not connect later. Confirm discovery ends and a fresh scan works.

Service UUID: `c61467fb-8db7-4793-b5bb-42cfacb0184e`. Both endpoints must use this UUID. This does not implement BLE or general-purpose RS232/SPP compatibility. Commands use printable ASCII; incoming lines are capped at 8192 bytes. Unterminated partial lines are discarded on disconnect. Log files remain private to the app and are removed when app data is cleared.

Day 1 CI passed assembleDebug, testDebugUnitTest and lintDebug. Day 2 compiled and passed unit tests in CI, but lint failed on the missing coarse-location declaration. This follow-up adds the declaration and matching legacy permission request; the latest CI must validate it and sensor capture. The two-phone checks above remain unperformed and are required for hardware verification.

Bluetooth implementation inspiration: [Philipp Lackner](https://youtube.com/@philipplackner).
