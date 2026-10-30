# Glasgram

**A native Android Telegram client concept with a custom Liquid Glass interface.**

Glasgram is built with Kotlin, Jetpack Compose, and the official TDLib client library. It is an independent third-party client project, not an official Telegram app.

## Project status (v0.2.0)

**Working (written, not yet device-tested):**
- [x] Liquid-glass UI: translucent glass cards, specular rims, drifting light orbs, floating glass tab bar
- [x] Login (phone, code, 2-step password) and auto-login on next launch
- [x] Chat list: live updates, last-message preview, unread badges, pinned/muted, search, folder chips
- [x] Long-press chat actions: pin, mute, mark as read
- [x] Conversation screen: send, reply, edit, delete, copy, delivery ticks, date separators, history paging
- [x] **Glasgram Settings** (client-side mod features): Ghost Mode, Always Offline, keep deleted messages,
      keep edit history, accent colours, glass intensity, bubble roundness, text size, compact list,
      24h clock, show IDs, double-tap reply, haptics

**Not done yet:** photos/videos/voice/stickers (shown as labels only), avatars, calls, push notifications
when the app is closed, secret-chat specifics, Telegram folders, reactions, forwarding.

## Build

Requirements: Android Studio (current stable), JDK 17, Android SDK 35.

```bash
./gradlew assembleDebug
```

The debug APK is normally generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Telegram API setup

1. Create your own application credentials at https://my.telegram.org.
2. Supply `TELEGRAM_API_ID` and `TELEGRAM_API_HASH` as local Gradle properties or environment variables; never commit real credentials.
3. Follow [docs/TDLIB_SETUP.md](docs/TDLIB_SETUP.md) to package the matching TDLib Java/JNI runtime for Android.
4. Test authorization and messaging before enabling any feature flag.

The API credentials identify the client application; they are not a substitute for a user's login code or password. Do not collect login codes outside TDLib's authorization flow.

## Architecture

- `feature/`: Compose screens and UI state
- `data/telegram/`: TDLib adapter and update stream
- `core/design/`: reusable glass surfaces, colors, and typography
- `docs/`: integration and release guidance

## Security

- Do not commit API hashes, user sessions, TDLib database files, or signing keys.
- Keep TDLib's database encryption key in secure Android storage.
- Cloud chats and Secret Chats have different security and synchronization properties.
- Follow Telegram API terms and all applicable licenses.

## License

The project's own code is currently marked as **MIT** in `LICENSE`. Third-party dependencies and TDLib remain subject to their own licenses. This does not relicense Telegram's code or branding.


## Automatic Telegram upstream updates

Glasgram includes a scheduled GitHub Actions watcher for the official Telegram Android and TDLib repositories. It generates an upstream change report and opens/updates a review issue. See [docs/AUTO_UPDATE_ENGINE.md](docs/AUTO_UPDATE_ENGINE.md).

The watcher intentionally does not blindly merge upstream code or publish APKs. Telegram Android and TDLib releases can be independent, and the Java TDLib API must match its native JNI binaries and packaged ABIs. Review, integration, tests and release approval remain required.


## In-place APK updates (no uninstall)

Production releases keep the stable package ID `com.glasgram.app`. Android can update an existing installation without uninstalling when the APK is signed by the same release key and has a higher `versionCode`. Configure the one-time GitHub signing secrets and follow [docs/APK_UPDATES_AND_SIGNING.md](docs/APK_UPDATES_AND_SIGNING.md). Never use debug APKs as production updates, and never rotate the release keystore.
