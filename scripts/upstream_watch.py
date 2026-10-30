#!/usr/bin/env python3
"""Check official Telegram Android and TDLib upstream heads; produce a review report."""
import argparse
import datetime
import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request

API = "https://api.github.com"


def request_json(url):
    headers = {
        "Accept": "application/vnd.github+json",
        "User-Agent": "Glasgram-Upstream-Watch",
        "X-GitHub-Api-Version": "2022-11-28",
    }
    token = os.getenv("GITHUB_TOKEN")
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(req, timeout=30) as response:
        return json.load(response)


def latest_commit(repo):
    data = request_json(f"{API}/repos/{repo}/commits?per_page=1")
    if not data:
        raise RuntimeError(f"No commits returned for {repo}")
    commit = data[0]
    return {
        "sha": commit["sha"],
        "short_sha": commit["sha"][:12],
        "url": commit["html_url"],
        "date": commit["commit"]["committer"]["date"],
        "message": commit["commit"]["message"].splitlines()[0][:180],
    }


def compare_commits(repo, base, head):
    base_q = urllib.parse.quote(base, safe="")
    head_q = urllib.parse.quote(head, safe="")
    data = request_json(f"{API}/repos/{repo}/compare/{base_q}...{head_q}")
    commits = []
    for item in data.get("commits", [])[:100]:
        commits.append({
            "sha": item["sha"][:12],
            "url": item["html_url"],
            "message": item["commit"]["message"].splitlines()[0][:180],
            "date": item["commit"]["committer"]["date"],
        })
    files = [
        {"name": item.get("filename", ""), "status": item.get("status", "modified"),
         "additions": item.get("additions", 0), "deletions": item.get("deletions", 0)}
        for item in data.get("files", [])[:300]
    ]
    return {
        "status": data.get("status", "unknown"),
        "ahead_by": data.get("ahead_by", 0),
        "behind_by": data.get("behind_by", 0),
        "total_commits": data.get("total_commits", len(commits)),
        "commits": commits,
        "files": files,
    }


def latest_release(repo):
    try:
        release = request_json(f"{API}/repos/{repo}/releases/latest")
        return {"tag": release.get("tag_name"), "url": release.get("html_url")}
    except urllib.error.HTTPError as exc:
        if exc.code in (404, 403):
            return None
        raise


def inspect_source(key, source):
    repo = source["repository"]
    head = latest_commit(repo)
    baseline = source.get("baseline_commit", "").strip()
    item = {
        "repository": repo,
        "purpose": source.get("purpose", ""),
        "baseline_commit": baseline or None,
        "latest": head,
        "status": "BASELINE_REQUIRED" if not baseline else (
            "UNCHANGED" if baseline == head["sha"] else "REVIEW_REQUIRED"
        ),
    }
    if baseline and baseline != head["sha"]:
        try:
            item["comparison"] = compare_commits(repo, baseline, head["sha"])
        except urllib.error.HTTPError as exc:
            item["comparison_error"] = f"GitHub compare API returned HTTP {exc.code}"
    item["release"] = latest_release(repo)
    return item


def build_report(cfg):
    results = {key: inspect_source(key, source) for key, source in cfg["sources"].items()}
    return {
        "checked_at_utc": datetime.datetime.now(datetime.timezone.utc).isoformat(),
        "sources": results,
        "review_required": any(item["status"] == "REVIEW_REQUIRED" for item in results.values()),
        "baseline_setup_required": any(item["status"] == "BASELINE_REQUIRED" for item in results.values()),
    }


def render_markdown(payload):
    lines = [
        "# Glasgram upstream update report", "",
        f"- Checked (UTC): `{payload['checked_at_utc']}`",
        "- Sources: official Telegram Android (`DrKLO/Telegram`) and official TDLib (`tdlib/td`).", "",
        "## Detected upstream heads", "",
    ]
    for key, item in payload["sources"].items():
        head = item["latest"]
        lines.extend([
            f"### {key} — {item['status']}",
            f"- Repository: https://github.com/{item['repository']}",
            f"- Latest commit: [`{head['short_sha']}`]({head['url']})",
            f"- Commit date: `{head['date']}`",
            f"- Summary: {head['message']}",
        ])
        if item.get("release"):
            release = item["release"]
            lines.append(f"- Latest release: [{release['tag']}]({release['url']})")
        if item.get("baseline_commit"):
            lines.append(f"- Reviewed baseline: `{item['baseline_commit'][:12]}`")
        if item.get("comparison"):
            comparison = item["comparison"]
            lines.append(f"- Commits ahead: `{comparison['ahead_by']}`; files changed in API response: `{len(comparison['files'])}`")
            if comparison["commits"]:
                lines.append("- Recent commits:")
                lines.extend(f"  - [`{c['sha']}`]({c['url']}): {c['message']}" for c in comparison["commits"][-30:])
            if comparison["files"]:
                lines.append("- Changed files (first 100/300 returned):")
                lines.extend(f"  - `{f['name']}` ({f['status']}, +{f['additions']}/-{f['deletions']})" for f in comparison["files"][:100])
        if item.get("comparison_error"):
            lines.append(f"- Comparison warning: {item['comparison_error']}")
        lines.append("")

    lines.extend([
        "## Required engineering review", "",
        "1. Classify commits and changed files into features, API/schema, security, bug fixes and build changes.",
        "2. Check TDLib API/schema compatibility and native JNI ABI/version pairing.",
        "3. Map each change to Glasgram modules; preserve Glasgram UI and custom features.",
        "4. Implement only changes supported by public Telegram APIs/TDLib and permitted by applicable licenses.",
        "5. Run build, unit, UI, regression, login, media and device tests before release.",
        "6. Advance the reviewed baseline only after corresponding changes are integrated and verified.", "",
        "## Safety boundary", "",
        "This watcher detects and reports upstream changes. It does not blindly copy source files, bump an unverified TDLib binary, rewrite app code, or auto-publish an APK. Telegram Android and TDLib can change independently; JNI binaries must match the Java/API version and target ABIs.", "",
    ])
    return "\n".join(lines)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--config", default="config/upstream-lock.json")
    parser.add_argument("--output", default="upstream-report.md")
    parser.add_argument("--json-output", default="upstream-report.json")
    args = parser.parse_args()

    with open(args.config, encoding="utf-8") as source_file:
        config = json.load(source_file)
    payload = build_report(config)
    with open(args.json_output, "w", encoding="utf-8") as output_file:
        json.dump(payload, output_file, indent=2)
    with open(args.output, "w", encoding="utf-8") as output_file:
        output_file.write(render_markdown(payload))
    print(f"Wrote {args.output} and {args.json_output}")
    print("Review required:", payload["review_required"])
    print("Baseline setup required:", payload["baseline_setup_required"])


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        print(f"upstream watcher failed: {exc}", file=sys.stderr)
        sys.exit(1)
