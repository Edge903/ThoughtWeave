"""Scores summaries against sets.json: facts kept, dates missing or added, questions kept, output language.

Usage:
  python score.py results.json [more.json ...]   score result files
  python score.py --self-check                   score each set's own memos (must be full marks)

A result file: {"config": "B-tldr", "runs": [{"set": "ko-1", "output": "...", "seconds": 0.4}, ...]}.
A set may have several runs; totals are averaged per set so configurations with different run counts compare.

The date reader is a stand-in for the app's schedule date reader: it reads "12월 5일", "12.5", "12/5",
"2026.12.5", "Jan 9", "9 January" (month first for numeric dates, as both profiles use). Relative dates
("12일 후", "next Friday") are not read, so a date rewritten that way counts as missing, as in the app.
"""
from __future__ import annotations

import json
import re
import sys
from dataclasses import dataclass, field
from pathlib import Path

SETS_PATH = Path(__file__).with_name("sets.json")

_MONTH_NAMES = ["january", "february", "march", "april", "may", "june", "july", "august", "september",
                "october", "november", "december"]
MONTHS = {m[:3]: i + 1 for i, m in enumerate(_MONTH_NAMES)}
# Full names or three-letter abbreviations only, so "market" or "decide" never read as a month.
_MONTH = r"\b(" + "|".join(n for m in _MONTH_NAMES for n in dict.fromkeys([m, m[:3]])) + r")\b\.?"
_DATE_PATTERNS = [
    (re.compile(r"(\d{1,2})\s*월\s*(\d{1,2})\s*일"), "md"),
    # Not part of a longer number: "4,900", "1.5", "7:40" are skipped, but "12/7, then" still reads.
    (re.compile(r"(?<![\d.:/])(?<!\d,)(?:\d{4}\s*[./-]\s*)?(\d{1,2})\s*[./]\s*(\d{1,2})(?![\d])(?![.,:/]\d)"), "md"),
    (re.compile(_MONTH + r"\s*(\d{1,2})(?:st|nd|rd|th)?(?!\d)", re.I), "Md"),
    (re.compile(r"(?<!\d)(\d{1,2})(?:st|nd|rd|th)?\s+(?:of\s+)?" + _MONTH, re.I), "dM"),
]
# "까" but not "까지" (until).
_QUESTION_MARKERS = re.compile(r"\?|까(?!지)|는지|을지|할지|여부|미정|whether|undecided|not decided|\btbd\b|\bdecide", re.I)


def dates_in(text: str) -> set[str]:
    found = set()
    for pattern, order in _DATE_PATTERNS:
        for m in pattern.finditer(text):
            if order == "md":
                month, day = int(m.group(1)), int(m.group(2))
            elif order == "Md":
                month, day = MONTHS[m.group(1).lower()[:3]], int(m.group(2))
            else:
                month, day = MONTHS[m.group(2).lower()[:3]], int(m.group(1))
            if 1 <= month <= 12 and 1 <= day <= 31:
                found.add(f"{month:02d}-{day:02d}")
    return found


def _token_regex(token: str) -> re.Pattern:
    """Spaces in the text are optional between the token's characters; digits must not run on."""
    chars = [re.escape(c) for c in token if not c.isspace()]
    body = r"\s*".join(chars)
    if token[0].isdigit():
        body = r"(?<!\d)" + body
    if token[-1].isdigit():
        body = body + r"(?![\d])"
    return re.compile(body, re.I)


def has_fact(text: str, groups: list[list[str]]) -> bool:
    return all(any(_token_regex(v).search(text) for v in group) for group in groups)


def _segments(text: str) -> list[str]:
    """Sentences end at . ! ? followed by a space, or at a line break, so "12.5" stays whole."""
    return [s for s in re.split(r"(?<=[.!?])\s+|\n", text) if s.strip()]


def kept_as_question(text: str, groups: list[list[str]]) -> bool:
    """Some sentence or line has the fact's tokens and still reads as a question."""
    return any(has_fact(s, groups) and _QUESTION_MARKERS.search(s) for s in _segments(text))


def language_ok(text: str, language: str) -> bool:
    hangul = len(re.findall(r"[가-힣]", text))
    latin = len(re.findall(r"[A-Za-z]", text))
    if hangul + latin == 0:
        return False
    share = hangul / (hangul + latin)
    return share >= 0.6 if language == "ko" else share <= 0.05


@dataclass
class Score:
    set_id: str
    facts_kept: int
    facts_total: int
    lost: list[str]
    dates_missing: list[str]
    dates_added: list[str]
    questions_kept: int
    questions_total: int
    language_ok: bool
    seconds: float | None = None


def score(s: dict, output: str, seconds: float | None = None) -> Score:
    lost = [f["id"] for f in s["facts"] if not has_fact(output, f["all"])]
    questions = [f for f in s["facts"] if f.get("question")]
    found = dates_in(output)
    expected = set(s["dates"])
    return Score(
        set_id=s["id"],
        facts_kept=len(s["facts"]) - len(lost),
        facts_total=len(s["facts"]),
        lost=lost,
        dates_missing=sorted(expected - found),
        dates_added=sorted(found - expected),
        questions_kept=sum(kept_as_question(output, q["all"]) for q in questions),
        questions_total=len(questions),
        language_ok=language_ok(output, s["language"]),
        seconds=seconds,
    )


def source_text(s: dict) -> str:
    return "\n\n".join(m["title"] + "\n" + m["content"] for m in s["memos"])


def load_sets() -> dict[str, dict]:
    return {s["id"]: s for s in json.loads(SETS_PATH.read_text(encoding="utf-8"))["sets"]}


@dataclass
class Report:
    config: str
    scores: list[Score] = field(default_factory=list)

    def print(self, sets: dict[str, dict]) -> None:
        print(f"== {self.config}")
        print(f"{'set':6} {'facts':>7} {'q':>5} {'dates -/+':>10} {'lang':>5} {'sec':>6}  lost / date changes")
        per_set: dict[str, list[Score]] = {}
        for sc in self.scores:
            per_set.setdefault(sc.set_id, []).append(sc)
            sec = f"{sc.seconds:.1f}" if sc.seconds is not None else "-"
            changes = [f"-{d}" for d in sc.dates_missing] + [f"+{d}" for d in sc.dates_added]
            print(f"{sc.set_id:6} {sc.facts_kept:>3}/{sc.facts_total:<3} {sc.questions_kept:>2}/{sc.questions_total:<2}"
                  f" {len(sc.dates_missing):>4}/{len(sc.dates_added):<4} {'ok' if sc.language_ok else 'NO':>5} {sec:>6}"
                  f"  {', '.join(sc.lost) or '-'} | {' '.join(changes) or '-'}")
        mean = lambda xs: sum(xs) / len(xs)
        facts = sum(mean([x.facts_kept for x in v]) for v in per_set.values())
        total = sum(sets[k]["facts"].__len__() for k in per_set)
        dates = sum(mean([len(x.dates_missing) + len(x.dates_added) for x in v]) for v in per_set.values())
        qs = sum(mean([x.questions_kept for x in v]) for v in per_set.values())
        qt = sum(v[0].questions_total for v in per_set.values())
        print(f"TOTAL (mean per set) facts {facts:.1f}/{total}, questions {qs:.1f}/{qt}, date changes {dates:.1f},"
              f" runs {len(self.scores)}\n")


def main(argv: list[str]) -> int:
    sets = load_sets()
    if argv == ["--self-check"]:
        report = Report("self-check (memos scored as their own summary)",
                        [score(s, source_text(s)) for s in sets.values()])
        report.print(sets)
        bad = [sc for sc in report.scores if sc.lost or sc.dates_missing or sc.dates_added
               or sc.questions_kept != sc.questions_total or not sc.language_ok]
        return 1 if bad else 0
    if not argv:
        print(__doc__)
        return 2
    for path in argv:
        data = json.loads(Path(path).read_text(encoding="utf-8"))
        Report(data["config"], [score(sets[r["set"]], r["output"], r.get("seconds")) for r in data["runs"]]).print(sets)
    return 0


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    sys.exit(main(sys.argv[1:]))
