import importlib.util
import pathlib
import unittest
from unittest.mock import patch

ROOT = pathlib.Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location("upstream_watch", ROOT / "scripts" / "upstream_watch.py")
watch = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(watch)


class UpstreamWatchTests(unittest.TestCase):
    def test_markdown_uses_real_line_breaks(self):
        payload = {
            "checked_at_utc": "2026-10-30T00:00:00+00:00",
            "review_required": True,
            "baseline_setup_required": False,
            "sources": {
                "tdlib": {
                    "repository": "tdlib/td",
                    "status": "REVIEW_REQUIRED",
                    "latest": {
                        "short_sha": "abcdef123456", "url": "https://github.com/tdlib/td/commit/abcdef",
                        "date": "2026-10-30T00:00:00Z", "message": "Update API schema"
                    },
                    "baseline_commit": "1234567890abcdef",
                    "release": None,
                    "comparison": {
                        "ahead_by": 1,
                        "files": [{"name": "td/generate/scheme/td_api.tl", "status": "modified", "additions": 4, "deletions": 1}],
                        "commits": [{"sha": "abcdef123456", "url": "https://github.com/tdlib/td/commit/abcdef", "message": "Update API schema", "date": "2026-10-30T00:00:00Z"}]
                    }
                }
            }
        }
        rendered = watch.render_markdown(payload)
        self.assertIn("\n## Detected upstream heads\n", rendered)
        self.assertNotIn("\\n## Detected upstream heads", rendered)
        self.assertIn("td_api.tl", rendered)

    @patch.object(watch, "latest_release", return_value=None)
    @patch.object(watch, "compare_commits")
    @patch.object(watch, "latest_commit")
    def test_detects_new_upstream_commit(self, latest, compare, release):
        latest.return_value = {
            "sha": "b" * 40, "short_sha": "b" * 12,
            "url": "https://github.com/tdlib/td/commit/" + "b" * 40,
            "date": "2026-10-30T00:00:00Z", "message": "New schema"
        }
        compare.return_value = {"status": "ahead", "ahead_by": 2, "behind_by": 0,
                               "total_commits": 2, "commits": [], "files": []}
        item = watch.inspect_source("tdlib", {
            "repository": "tdlib/td", "baseline_commit": "a" * 40, "purpose": "TDLib"
        })
        self.assertEqual(item["status"], "REVIEW_REQUIRED")
        self.assertEqual(item["comparison"]["ahead_by"], 2)
        compare.assert_called_once()

    @patch.object(watch, "latest_release", return_value=None)
    @patch.object(watch, "latest_commit")
    def test_empty_baseline_requires_initialization(self, latest, release):
        latest.return_value = {
            "sha": "c" * 40, "short_sha": "c" * 12,
            "url": "https://github.com/DrKLO/Telegram/commit/" + "c" * 40,
            "date": "2026-10-30T00:00:00Z", "message": "Initial head"
        }
        item = watch.inspect_source("telegram_android", {
            "repository": "DrKLO/Telegram", "baseline_commit": "", "purpose": "Android"
        })
        self.assertEqual(item["status"], "BASELINE_REQUIRED")
        self.assertIsNone(item["baseline_commit"])


if __name__ == "__main__":
    unittest.main()
