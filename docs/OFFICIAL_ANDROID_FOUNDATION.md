# Official Telegram Android foundation migration

## Current upstream pin

Glasgram now tracks the official Android client as a Git submodule at `upstream/Telegram`.

- Upstream: https://github.com/DrKLO/Telegram
- Pinned commit: `3de37c8cfb039452be7398909461da6288babf15`
- Upstream release commit message: `update to 13.1.1 (7237)`
- License reported by upstream repository: GPL-2.0
- Submodule path: `upstream/Telegram`

The Git submodule pointer preserves the upstream repository's own history and avoids copying or rewriting its large source tree into Glasgram's history. The pin is intentionally fixed to a reviewed commit; it does not silently follow upstream master.

## Fetch the complete source

Clone Glasgram with submodules:

```bash
git clone --recurse-submodules https://github.com/KayrootYT/Glasgram.git
cd Glasgram
git submodule status
```

If Glasgram was already cloned:

```bash
git submodule update --init --recursive
```

To inspect the exact source revision:

```bash
git -C upstream/Telegram rev-parse HEAD
```

## Migration plan

This is the first foundation step, not the completed app migration. The existing Kotlin/Compose + TDLib application remains in `app/` until a tested replacement is ready.

1. Build the pinned official source in an isolated CI job, using the upstream-required Android Studio/Gradle, NDK and SDK versions. Do not use Telegram's dummy release keystore or Firebase configuration for a Glasgram release.
2. Inventory upstream modules, native libraries, build variants, API credentials, Firebase requirements and licensing notices.
3. Create a Glasgram application identity and branding layer without using Telegram's name or standard logo as the app identity.
4. Port Glasgram features into isolated modules or minimal patches; avoid invasive edits to upstream messaging, crypto, authorization and storage code.
5. Preserve the existing `com.glasgram.app` application ID and release signing key only if technically compatible. Do not assume TDLib session/database data can be imported into Telegram Android's native storage.
6. Design a backup/rollback and account re-authentication path before replacing the current app. Never delete the existing TDLib data during migration tests.
7. Add CI for debug builds first. Verify ABI coverage, APK signature, package ID, install/upgrade behavior and real-device login before any production release.
8. Review every upstream update and advance the submodule pin only in a separate pull request after successful build and regression tests.

## Important compatibility warning

The current Glasgram app is a custom Jetpack Compose client using TDLib. The official Telegram Android source is a different Java-based application architecture with its own networking, storage, UI and build setup. Adding the official source as a submodule does not automatically make it the runtime foundation. A deliberate feature and data migration is required.

## Upstream build requirements

The upstream README currently specifies Android Studio 2025.1.4, Android NDK 27.2.12479018 and Android SDK 36, and requires cloning recursively to initialize submodules. Check the pinned upstream README before building because requirements can change with later commits.

## Licensing and branding

The upstream project identifies itself as GPL-2.0 and asks third-party developers to obtain their own Telegram API ID, make the unofficial nature clear, avoid the standard Telegram logo, and publish their own code to comply with applicable licenses. Preserve upstream copyright/license notices and review the exact license obligations before distribution.

References:
- https://github.com/DrKLO/Telegram
- https://github.com/DrKLO/Telegram/blob/master/README.md
- https://core.telegram.org/api/obtaining_api_id
