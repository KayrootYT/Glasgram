# Official Android upstream build audit

## Audit target

- Repository: https://github.com/DrKLO/Telegram
- Pinned revision: `3de37c8cfb039452be7398909461da6288babf15`
- Upstream release label: `13.1.1 (7237)`
- Gradle wrapper: Gradle 8.13
- Android Studio: 2025.1.4
- Android SDK / compile SDK: 36
- Android NDK: 27.2.12479018
- CMake: 3.22.1
- Java source/target: 8 (run Gradle with a compatible JDK 17 environment)
- Minimum Android API: 23
- Native CMake target: `tmessages.49`

## Build graph

The upstream root settings include the main library `TMessagesProj`, application variants `TMessagesProj_App`, Huawei/HockeyApp/Standalone/Test application modules, jlatexmath, and Media3 modules. The ordinary app module applies the Android application plugin and Telegram's custom `org.telegram.build-app-plugin`, depends on `:TMessagesProj`, and applies Google Services.

The normal debug APK variant is configured as `afatDebug` (four ABIs: armeabi-v7a, arm64-v8a, x86, x86_64). The debug variant adds the `.beta` application ID suffix. This is not suitable as the final Glasgram package configuration without deliberate changes.

## Required upstream configuration

The pinned upstream README explicitly requires:

1. A project-owned Telegram API ID and API hash.
2. A project-owned Firebase `google-services.json` for the intended Android application IDs.
3. A project-owned release keystore and matching Gradle signing properties for release builds.
4. Review of every provider-specific integration and app identity value in `BuildVars.java`.

The upstream checkout currently contains reproducible-build placeholder credentials/configuration. They must not be shipped in Glasgram. In particular, the upstream `BuildVars.java` contains Telegram's own API ID/hash, SafetyNet key, Google OAuth client ID, Play Store URL and Huawei identifiers. Glasgram must use its own authorized API credentials and provider configuration; do not copy or expose upstream credentials.

## Glasgram compatibility decisions

- Keep the current `app/` Compose + TDLib client untouched while the upstream app is isolated.
- Preserve the existing `com.glasgram.app` package and release signing key; do not change either during this audit.
- Do not assume TDLib's account/session database can be imported into Telegram Android's native storage.
- Do not use the upstream dummy release keystore or upstream Firebase configuration.
- Disable or replace official-store-only integrations where appropriate; notably upstream source comments state passkeys work only with official app IDs.
- Keep Telegram's GPL notices and meet applicable source publication obligations for distributed modifications.
- Clearly identify Glasgram as an unofficial third-party client and use original branding.

## Next safe build sequence

1. Confirm required Glasgram API/Firebase configuration is available as repository secrets or protected files without printing secret values.
2. Create an isolated Glasgram application module/configuration on a dedicated migration branch. Do not edit the pinned upstream source in place until the minimal patch is documented.
3. First attempt a debug-only build in CI with a project-owned application ID and provider config. Never run a release/signing task in this phase.
4. If Google Services or provider integrations block the build, identify the exact failing task and disable/replace only the relevant integration in the Glasgram layer; do not insert dummy production credentials.
5. Verify APK package ID, ABI coverage, signature, install/upgrade behavior and login on a test device before considering any migration of user data.

## Status

This audit records the source build contract and migration hazards. It is not a successful APK build and does not claim that the official Telegram Android source is running as Glasgram yet.
