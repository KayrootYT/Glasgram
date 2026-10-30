# Glasgram Automatic Telegram Update Engine

## What it does

The `Telegram Upstream Watch` workflow runs on weekdays and on manual dispatch. It checks the official Telegram Android source repository (`DrKLO/Telegram`) and official TDLib repository (`tdlib/td`), then publishes a report artifact and creates/updates a review issue.

It is a safe update-detection and integration-assistance pipeline, not a blind source-code merger.

## Why it does not auto-merge or auto-release

Telegram Android and TDLib are separate codebases. A new Telegram Android commit does not mean a matching TDLib/JNI artifact exists. The current Glasgram project pins a third-party JitPack TDLib package; blindly replacing its commit can break Java/JNI compatibility or supported CPU architectures.

Feature changes can also require UI work, permissions, storage changes, migrations, server-side support, API-layer updates and license review. A commit detector cannot safely infer and implement all of that without engineering review.

## Workflow

1. Scheduled/manual check of upstream repositories.
2. Compare upstream heads with reviewed baselines in `config/upstream-lock.json`.
3. Generate Markdown and JSON reports.
4. Create/update a GitHub issue for review.
5. Developer classifies and maps changes to Glasgram.
6. Implement in a feature branch and run CI/device tests.
7. Update the reviewed baseline only after integration is complete.
8. Merge and publish after approval.

## Configure baselines

The first workflow run reports `BASELINE_REQUIRED` because the baseline commit fields are intentionally empty. Copy the reported upstream commit SHAs into `config/upstream-lock.json` as the initial reviewed baseline, then commit the file.

After a change is fully reviewed and integrated, advance only that source's `baseline_commit` to the commit you have actually processed. Do not advance it just because a newer upstream commit exists.

## Run manually

GitHub repository → Actions → Telegram Upstream Watch → Run workflow.

Local check (Python 3.9+; internet required):

```bash
python3 scripts/upstream_watch.py
```

## Next automation stages

- Parse release notes and `td_api.tl` schema diffs.
- Classify changes into feature, API, security, bug-fix and build categories.
- Generate a per-module implementation plan and regression checklist.
- Prepare draft pull requests for deterministic dependency/config changes only.
- Require human review for source merges, migrations, permissions, cryptography, login/session handling and APK publishing.

## Current project-specific constraints

- Glasgram is Kotlin + Jetpack Compose and uses TDLib.
- `docs/TDLIB_SETUP.md` states that the native TDLib JNI runtime is not yet verified/packaged.
- The app README lists media, calls, push notifications, secret-chat specifics, folders, reactions and forwarding as incomplete.
- Do not mark a feature complete until it is implemented and tested against a real Telegram account/device.
- Keep API credentials, signing keys, user sessions and TDLib database encryption keys out of source control.

## Official references

- https://github.com/DrKLO/Telegram
- https://github.com/tdlib/td
- https://core.telegram.org/tdlib/
- https://core.telegram.org/tdlib/getting-started
- https://telegram.org/apps
