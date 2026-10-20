# Android multi-Slave design and implementation roadmap

Status: agreed design, not implemented. The current app supports one connection and protocol v1.
Scope: Android Master and Android Slaves using Classic Bluetooth RFCOMM. Other platforms are deferred.

## Connection model

One Master manages multiple independent Slave connections. Each Slave accepts one Master.
Introduce a Master connection manager keyed by device identity; each entry owns its BluetoothSession,
handshake state, pending commands, sensor validator, readings and recording state.
Keep discovery and pairing at coordinator level; connect only after Android confirms bonding.
Adding a connection must not close existing connections. A failure, queue overflow or disconnect
affects only the corresponding Slave. Bound both the device count and each connection's buffers;
start with two Slaves, and establish practical limits through physical testing.

Use a persisted app-generated UUID as node identity, with a user-editable display name.
A Bluetooth address locates a connection; it is not the recording identity.
Clear live readings and pending requests when a connection ends or its generation changes.
STOP stops sensors while retaining Bluetooth. A role change closes all connections and capture.
Keep foreground-only capture initially; rotation/background behavior must be explicit in the UI.

## Protocol v2 proposal

Use UTF-8 newline-delimited JSON with an 8192-byte frame limit, version negotiation and strict
field validation. This is a breaking protocol change: v1 peers must receive a visible incompatibility
message, with no silent fallback. Both roles must complete the handshake before commands or samples.

Handshake contains type=hello, version=2, role, nodeId and name. Slave reports sensor capabilities
through GET_INFO. Bind the advertised node identity to the connection for the session; duplicate
active identities are rejected rather than mixing readings.

Example command:
```json
{"type":"command","id":"42","name":"START","args":{"sensors":["accelerometer"],"rateHz":10}}
```

Example success:
```json
{"type":"response","id":"42","ok":true,"result":{"streamId":"stream-uuid","sensors":["accelerometer"],"rateHz":10}}
```

Example failure:
```json
{"type":"response","id":"42","ok":false,"error":{"code":"UNSUPPORTED_SENSOR","message":"Gyroscope unavailable"}}
```

| Command | Behavior |
|---|---|
| GET_INFO | Return identity, protocol version, available sensors, units and supported rate range |
| START | Validate requested channels/rate, start capture, acknowledge actual configuration and stream ID |
| STOP | Stop capture, discard queued samples, acknowledge after any in-flight sample |
| GET_STATUS | Return idle/streaming state and active configuration |
| PING | Reply to confirm responsiveness |

Request IDs are unique within a connection generation. Track pending requests per Slave, with
a proposed five-second response timeout; a timeout means outcome unknown, not confirmed failure.
Use GET_STATUS to reconcile a timed-out START/STOP. Late responses must not complete a newer request.
Bound the pending-request table and duplicate-response cache. Repeating a request ID returns its
cached response without executing again; a changed payload with the same ID is rejected.
START while already streaming returns the existing stream for identical configuration; changing
configuration requires STOP then START. Initial supported rates remain capped at 10 Hz per channel.

Samples carry streamId, sequence, channel, units, monotonic timestampNs, slave wall-clock time and
values. Master attaches nodeId, connection generation, master receipt time and recording session ID.
Validate dimensions, finite values, active stream and strictly increasing sequence per stream.
Queue the START acknowledgement before samples. Reject old-stream samples after STOP or reconnect.
Device monotonic clocks are not comparable across phones; group START does not promise synchronized capture.

## Master controls

Show one card per Slave: identity, connection/handshake state, available sensors, recording state,
latest readings and last command outcome. Support targeting one Slave, a selected group or all ready
Slaves. Snapshot the targets when submitting a group action and send separate requests.
Show success, error or timeout for every target; never report universal success after partial failure.
Slaves connected later do not automatically join an earlier group command.
Pair devices before the initial multi-device streaming test.

## Milestones and acceptance checks

1. **Two simultaneous Slaves:** introduce the connection manager and per-device UI/state.
   Verify connecting B leaves A connected; disconnect/reconnect B while A continues streaming.
2. **Acknowledged commands:** implement protocol v2, capability discovery, sensor selection,
   bounded rate validation and per-device request tracking. Test invalid arguments, unsupported sensors,
   duplicate IDs, late replies, timeout reconciliation, incompatible versions and old-stream samples.
3. **Group control:** add selected/all targeting and independent outcomes. Verify one refusing or
   disconnected Slave does not prevent others from starting or stopping.
4. **Recording/export:** persist sessions with node/stream identity and export CSV without mixing
   devices. Add graphs after recording correctness is established.

Run Android build, unit tests and lint for implementation changes. Add simulated multi-peer tests
for state isolation and command routing, then test on three physical Android phones (one Master,
two Slaves). Record actual results; automated tests do not verify hardware Bluetooth behavior.
Keep PR #1 draft until the existing baseline and new multi-device hardware checks pass.
