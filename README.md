# DataLoggerApp

Android Bluetooth data logger baseline, written in Kotlin with Jetpack Compose.

The default branch previously contained only the Android starter screen. Earlier Bluetooth work is preserved in Git history before commit `8f1ba49` (which deleted the Bluetooth branch contents). The repair branch implements a new, limited baseline using the same service UUID as that earlier work.

## Implemented

- Android 9+ support, with runtime Bluetooth Connect permission on Android 12+.
- System Bluetooth enable prompt and list of devices already paired in Android settings.
- One Classic Bluetooth RFCOMM connection: connect as master or listen as slave.
- Send ASCII commands terminated by LF; receive LF or CRLF terminated ASCII data.
- Handle fragmented incoming lines, end of stream, cancellation and connection failures.
- Append timestamped incoming lines to private `received.log`; display the latest 100 lines.
- Close sockets and workers when the activity is destroyed. Rotation closes the connection; reconnect afterwards.

This is a transport/logger baseline. Sensor acquisition, command interpretation, sensor channels, alarms, charts, CSV export and cloud upload are **not implemented**. Listening does not automatically generate sensor readings or reply to commands.

## Build

Open the **DataLogger** directory in Android Studio. Install Android SDK platform 35 and use JDK 17.

```bash
cd DataLogger
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

The GitHub Actions workflow runs these checks and uploads a debug APK.

## Two-phone smoke test

1. Install the same debug APK on two phones supporting Classic Bluetooth. Pair them through Android settings.
2. Open the app on each phone, grant the requested permission and enable Bluetooth.
3. On the slave, tap **Listen as slave**. On the master, select the paired slave. Both should show **Connected**.
4. Send `PING` from the master. The slave should display `PING`. Send a reply from the slave and verify it on the master.
5. Disconnect while listening, connecting, or receiving. Verify the UI returns to Disconnected and a new connection can be started.
6. Deny permission and retry using **Enable / refresh Bluetooth**. Also test a disabled adapter and an unavailable peer.

Service UUID: `c61467fb-8db7-4793-b5bb-42cfacb0184e`. Both endpoints must use this UUID. This does not implement BLE or general-purpose RS232/SPP compatibility. Commands use printable ASCII; incoming lines are capped at 8192 bytes. Unterminated partial lines are discarded on disconnect. Log files remain private to the app and are removed when app data is cleared.

Local validation in the repair environment: whitespace checks pass, but Gradle could not download its distribution due to restricted network access. An Android build and physical Bluetooth tests are required before calling this hardware-verified.

Bluetooth implementation inspiration: [Philipp Lackner](https://youtube.com/@philipplackner).
