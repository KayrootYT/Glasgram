# TDLib integration setup

Glasgram uses the official TDLib API. The Android app needs both the Java binding and a matching native TDLib JNI library for each packaged ABI.

## 1. Telegram app credentials

- Register your client at https://my.telegram.org.
- Keep `api_id` and `api_hash` outside version control.
- For local builds, put values in your user-level Gradle properties or supply environment variables in CI secrets.
- Never put a user's phone login code, password, session database, or encryption key in Git.

## 2. Native runtime

The Java dependency alone does **not** guarantee that `libtdjni.so` is packaged. Build or obtain the official TDLib Android artifacts using the instructions in the official repository, matching the Java binding version and each target ABI:

- https://github.com/tdlib/td
- https://github.com/tdlib/td/tree/master/example/android
- https://core.telegram.org/tdlib

Place validated ABI-specific native libraries under `app/src/main/jniLibs/<abi>/` only after confirming their origin, version, and license. The repository ignores these binaries by default; decide on a reproducible, license-compliant artifact strategy before committing them.

## 3. Version matching

Keep the TDLib Java API and JNI binary on the same release. Do not mix arbitrary versions or copy a native library from an unrelated APK. Test startup on a physical device for every ABI you distribute.

## 4. Authentication and data safety

Implement the official TDLib authorization state machine (parameters, phone number, code, optional password, ready/logged-out states). Do not manually send login codes to Telegram endpoints. Configure database encryption and handle logout/database cleanup deliberately.

## Current implementation status

This repository currently contains a UI starter, not a completed TDLib runtime integration. Live Telegram sign-in and messaging must not be presented as working until the native library is packaged and the authorization/update flow is implemented and tested.
