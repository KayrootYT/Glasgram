# Glasgram implementation status and remaining work

This document is the verified scope boundary for the current source package. A checked box means source/configuration exists; it does not imply successful end-to-end operation on a physical device.

## Completed in this source revision

- [x] Native Android project remains Kotlin + Jetpack Compose.
- [x] Existing Liquid Glass UI screens preserved.
- [x] TDLib gateway and basic authorization states (phone, code, 2-step password) exist in source.
- [x] Basic chat list, text message send, reply, edit, delete, history paging and pin/mute actions exist in source.
- [x] User-defined local chat folders: create up to 12, assign chats from long-press actions, filter by folder, and remove folders; stored locally in SharedPreferences.
- [x] TDLib database encryption key is generated randomly and wrapped with an AES-256-GCM Android Keystore key before persistence.
- [x] Kotlin Compose compiler plugin version aligned with Kotlin plugin version.
- [x] Telegram upstream watcher compares official Telegram Android and TDLib repository heads and requests a commit/file diff when a reviewed baseline is configured.
- [x] Watcher creates JSON/Markdown reports and GitHub Actions updates one open review issue rather than creating a new issue every scheduled run.
- [x] Python watcher unit tests are included in Android CI.
- [x] CI no longer requires Telegram API credentials merely to compile; without credentials the app intentionally shows the missing-credentials state.
- [x] Production application ID remains `com.glasgram.app`; release version code/name can be supplied by CI.
- [x] Release signing is configured from protected environment secrets; no signing key is committed.
- [x] Signed release workflow verifies APK signature and package identity before publishing.
- [x] Upgrade instructions document same package ID + same signing certificate + higher versionCode requirements.

## Not verified in this environment

- [ ] Android Gradle build / APK: not run in this environment; CI must complete successfully.
- [ ] Release keystore secrets: repository owner must configure the one-time signing secrets before the release workflow can publish a signed APK.
- [ ] In-place upgrade: must be tested by installing a signed release, then installing a higher-version APK signed by the same key without uninstalling.
- [ ] TDLib Java/JNI artifact: dependency is pinned, but packaged native libraries and supported ABIs must be confirmed from a successful build and physical-device startup.
- [ ] Telegram login, session persistence, send/receive and reconnect against a real account/device.
- [ ] Keystore-backed database key behavior on supported Android devices, including restore/upgrade edge cases.

## Remaining product phases

### Phase 1 — Foundation

- Verify and pin a reproducible official TDLib Java/JNI artifact with ABI inventory and license notices.
- Add Gradle wrapper (requires the official wrapper JAR/distribution) and prove clean CI build.
- Test authorization including registration, email login, other-device confirmation and future TDLib authorization states.
- Add logout/session reset tests, lifecycle/reconnect handling, database migration and encrypted backup policy.
- Add runtime notification permission flow and secure notification behavior.

### Phase 2 — Telegram parity

- Media send/download and playback: photo, video, document, audio, voice note, video note and animation.
- Message entities, formatting, stickers, reactions, polls, forwarding, pinning, scheduling and drafts.
- Full chat folders, archive, topics, group/channel administration, contacts and global search.
- Calls, stories, bots, Mini Apps, inline mode and push notifications.
- Premium-gated functionality only where the official API and account entitlement allow it.
- Secret Chats require separate end-to-end encrypted-chat support and must not be represented as ordinary cloud chats.

### Phase 3 — Third-party client capabilities

- [x] Initial local custom chat folders (create, assign, filter, delete) implemented in the Glasgram feature branch; requires CI and device verification.
- Advanced server-backed folders/tabs, per-chat appearance, download manager, translation, bookmarks, gesture customization, privacy controls and multi-account support.
- Each feature requires a product specification, API support check, UI, persistence, accessibility and regression tests. Do not copy proprietary client code/assets.

### Phase 4 — AutoSync 2.0

- Baseline commits must be initialized by reviewing the current upstream heads.
- Add TDLib schema diff parsing and Telegram release-note ingestion.
- Generate module-specific implementation plans and regression test plans.
- Draft PRs only for deterministic safe changes; source merges, cryptography, session handling, migrations and release signing require human review.
- Add an in-app Update Center backed by a trusted release manifest and signed update metadata.

### Phase 5 — Release

- Configure the one-time release keystore secrets and verify first signed release.
- Test in-place upgrade from a previous signed build, including TDLib database/session preservation.
- Add an in-app Update Center that checks the trusted GitHub Releases manifest and hands the APK to Android Package Installer (user confirmation required).
- Test clean install, upgrade, login, logout, media, notifications, battery/background behavior and multiple Android versions/ABIs.
- Publish only after all critical tests pass and the release has explicit approval.

## Do not claim as complete

The current project is not yet feature-equivalent to Telegram, does not implement every third-party-client feature, and does not automatically merge or release upstream code. The watcher is update detection and review automation, not autonomous feature implementation.


## 2026-10-31 feature branch update

The `glasgram-feature-implementation` branch adds persistent local custom chat folders. Folder membership is client-side and does not create Telegram-native folders or sync to other devices. CI and physical-device verification are still required. This branch does not yet replace the TDLib Compose foundation with the official Telegram Android source.
