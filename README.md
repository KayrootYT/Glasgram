# Glasgram

**A native Android Telegram client concept with a custom Liquid Glass interface.**

Glasgram is built with Kotlin, Jetpack Compose, and the official TDLib client library. It is an independent third-party client project, not an official Telegram app.

## Project status

- [x] Native Android / Jetpack Compose starter
- [x] Initial Liquid Glass-inspired chat-list UI
- [x] Gradle project and CI workflow
- [x] TDLib adapter boundary and setup documentation
- [ ] TDLib native runtime packaging and end-to-end authorization
- [ ] Real chat list, send/receive, media, notifications, calls, and advanced features

Features are not considered complete until they are implemented and tested against a real Telegram account.

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
