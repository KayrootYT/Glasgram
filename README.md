# Glasgram

**A native Android Telegram client concept with a custom Liquid Glass interface.**

Glasgram is an independent third-party Android client project, not an official Telegram app.

## Project status (v0.2.0)

**Architecture migration in progress:** the official Android client source is pinned as a Git submodule at `upstream/Telegram`. The current Compose + TDLib app project is stored in `Glasgram.zip` at the repository root. The upstream source remains separate and is not yet the running Glasgram app. See [docs/OFFICIAL_ANDROID_FOUNDATION.md](docs/OFFICIAL_ANDROID_FOUNDATION.md).

## Repository layout

- `Glasgram.zip` — complete current Android application project (app module and Gradle project files).
- `.github/workflows/` — CI, source audits and packaging automation; kept outside the ZIP.
- `.gitmodules` and `upstream/Telegram` — official Telegram Android source pin; kept outside the ZIP.
- `docs/`, `config/`, `scripts/` — project documentation and repository automation; kept outside the ZIP.
- `README.md`, `LICENSE`, `.gitignore` — repository-level files.

## Build

Requirements: Android Studio (current stable), JDK 17, Android SDK 36.

Extract the project archive into a working directory, then open that directory in Android Studio:

```bash
unzip Glasgram.zip -d Glasgram
cd Glasgram
gradle assembleDebug
```

The debug APK is normally generated at `app/build/outputs/apk/debug/app-debug.apk`. GitHub Actions also extracts `Glasgram.zip` before running the Android CI build.

## Telegram API setup

1. Create your own application credentials at https://my.telegram.org.
2. Supply `TELEGRAM_API_ID` and `TELEGRAM_API_HASH` as local Gradle properties or environment variables; never commit real credentials.
3. Follow [docs/TDLIB_SETUP.md](docs/TDLIB_SETUP.md) to package the matching TDLib Java/JNI runtime for Android.
4. Test authorization and messaging before enabling any feature flag.

The API credentials identify the client application; they are not a substitute for a user's login code or password. Do not collect login codes outside TDLib's authorization flow.

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
