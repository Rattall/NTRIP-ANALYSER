# NTRIP-ANALYSER

Android app (Kotlin + Compose) for connecting to an NTRIP caster and analysing RTCM3 streams.

## Current implementation status

- Android project scaffold with Compose UI.
- Secure credential persistence via `EncryptedSharedPreferences`.
- NTRIP source table fetch and mountpoint selection.
- NTRIP stream connection with Rev1/Rev2 and optional TLS socket.
- Automatic reconnect with bounded exponential backoff and retry attempt tracking.
- RTCM3 framing + CRC24Q validation.
- Live stats dashboard:
	- Connection state / status
	- Bytes per second + total bytes
	- Messages per second + total messages
	- CRC failure count
	- Session uptime
- Connection event log showing recent connect/reconnect/disconnect transitions.
- Live decoded message feed.

## RTCM decode coverage (in-progress)

Registered message groups:

- 1004, 1012
- 1005, 1006, 1033, 1230
- 1071-1077 (MSM GPS)
- 1081-1087 (MSM GLONASS)
- 1091-1097 (MSM Galileo)
- 1121-1127 (MSM BeiDou)

Current decoder depth:

- 1005/1006/1033/1230: core field extraction implemented.
- 1004/1012: full header and per-satellite observation fields implemented.
- MSM families: common header + satellite/signal/cell masks + satellite blocks + per-cell observation blocks implemented (MSM1-7 field sets).
- Unsupported message types still show type, payload length, CRC status, and raw payload hex.

## Next implementation steps

1. Expand parser tests from synthetic frames to captured real-world golden frames.
2. Add value scaling/normalization helpers for MSM observation fields (raw bit values are currently exposed).
3. Add parser golden tests for each message family.
4. Add reconnect strategy and error classification in stream controller.

## Tests

- Added unit tests for decoder paths:
	- 1004 per-satellite field parsing
	- 1012 per-satellite field parsing
	- MSM 1074 mask, satellite block, and cell block parsing
- Added unit tests for frame/CRC paths:
	- RTCM frame extraction for single, fragmented, and back-to-back frame buffers
	- CRC24Q validation and invalid-CRC detection behavior
- Added unit tests for network parsing paths:
	- Source-table `STR;` parsing and malformed-line filtering
	- HTTP/NTRIP status-line handling (`HTTP 200`/`ICY 200` accepted, non-200 rejected)
- Added unit tests for reconnect policy:
	- Exponential delay growth and max-delay capping
- Test file: `app/src/test/java/com/rattall/ntripanalyser/rtcm/RtcmDecoderTests.kt`
- Test file: `app/src/test/java/com/rattall/ntripanalyser/rtcm/RtcmFramerTests.kt`
- Test file: `app/src/test/java/com/rattall/ntripanalyser/network/NtripSourceTableParserTests.kt`
- Test file: `app/src/test/java/com/rattall/ntripanalyser/network/NtripHttpResponseParserTests.kt`
- Test file: `app/src/test/java/com/rattall/ntripanalyser/domain/ReconnectPolicyTests.kt`

## Build

Open this folder in Android Studio and run `app`.