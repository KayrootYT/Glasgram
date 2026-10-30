# APK updates without uninstalling Glasgram

Android supports an in-place app update. Users do **not** need to uninstall the old app when all of these remain true:

1. The new APK has the exact same `applicationId`: `com.glasgram.app`.
2. The new APK is signed with the exact same release signing certificate/private key as the installed version.
3. The new APK has a strictly higher `versionCode`.
4. The app does not intentionally clear or migrate away the user's app data.

This project keeps `applicationId = "com.glasgram.app"` for production releases. The debug variant uses `com.glasgram.app.debug` and a debug certificate; debug APKs are for testing and are not production update packages.

## One-time GitHub setup (required before the first signed release)

Create one Android release keystore locally and keep a secure offline backup. Never commit it to GitHub or include it in the project ZIP. Example:

```bash
keytool -genkeypair -v \
  -keystore glasgram-release.jks \
  -alias glasgram \
  -keyalg RSA -keysize 4096 -validity 10000
```

Export the keystore as Base64:

```bash
base64 -w 0 glasgram-release.jks
```

Add these GitHub Actions repository secrets:

- `GLASGRAM_KEYSTORE_BASE64` — Base64 contents of the keystore
- `GLASGRAM_STORE_PASSWORD` — keystore password
- `GLASGRAM_KEY_ALIAS` — `glasgram` (or the alias you chose)
- `GLASGRAM_KEY_PASSWORD` — key password
- `TELEGRAM_API_ID` and `TELEGRAM_API_HASH` — Telegram app credentials

Use the same keystore and alias for every Glasgram release forever. If the signing key is lost or replaced, Android will reject an in-place update over installations signed by the old key. Users would otherwise need a migration/reinstall path.

## Publishing a release

1. Commit and push source to the repository.
2. Create and push a version tag, e.g. `git tag v1.0.1 && git push origin v1.0.1`.
3. GitHub Actions builds and signs `app-release.apk`.
4. The workflow verifies the APK signature and package name before publishing the GitHub Release.
5. Users download the new APK and tap Install/Update. Android's package installer replaces the old app while retaining app data when package name, signing key and versionCode rules are satisfied.

The workflow uses `100000 + GitHub Actions run_number` as the release `versionCode`, ensuring it is higher than the early development build numbers and increases for subsequent runs of this workflow.

## Important

- Do not distribute the `.debug` APK as an update for production.
- Do not change `applicationId` after public release.
- Do not generate a new signing keystore for each version.
- APK updates require user confirmation through Android's package installer unless a managed-device/system installer policy applies.
- An in-app “Check for updates” UI/download/install prompt is a separate feature. This workflow publishes signed APK releases; it does not silently install them.
- App data preservation does not guarantee every internal database schema is forward-compatible. Use explicit migrations and backup/restore tests for data model changes.
- For Google Play distribution, configure Play App Signing and follow its upload-key/app-signing-key model; do not mix arbitrary sideload signing keys with Play-delivered installs.
