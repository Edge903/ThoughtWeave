"""Configuration B: Summarization 200M alone, on the desktop CPU.

Usage: python run_200m.py MODEL [--sets ko-1,ko-2] [--runs 2]
Writes results/B-tldr.json and results/B-keypoints.json for score.py. Each set's memos are given as one text,
titles and contents joined as score.source_text does. Seconds cover summarize() only; load time is printed.
"""
from __future__ import annotations

import argparse
import json
import sys
import time
from pathlib import Path

from mediapipe.tasks.python.core.base_options import BaseOptions
from mediapipe.tasks.python.text import text_summarizer as ts

from score import load_sets, source_text

HERE = Path(__file__).parent


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("model")
    parser.add_argument("--sets", help="comma-separated set ids (default: all)")
    parser.add_argument("--runs", type=int, default=2)
    args = parser.parse_args()

    sets = load_sets()
    chosen = args.sets.split(",") if args.sets else list(sets)
    cache = HERE / ".cache"
    cache.mkdir(exist_ok=True)
    (HERE / "results").mkdir(exist_ok=True)

    for mode in (ts.Mode.TLDR, ts.Mode.KEYPOINTS):
        start = time.perf_counter()
        options = ts.TextSummarizerOptions(
            base_options=BaseOptions(model_asset_path=args.model), mode=mode, cache_dir=str(cache))
        runs = []
        with ts.TextSummarizer.create_from_options(options) as summarizer:
            print(f"{mode.name}: loaded in {time.perf_counter() - start:.1f}s")
            for set_id in chosen:
                for run in range(args.runs):
                    t0 = time.perf_counter()
                    output = summarizer.summarize(source_text(sets[set_id])).summary
                    runs.append({"set": set_id, "run": run + 1, "seconds": round(time.perf_counter() - t0, 2),
                                 "output": output})
        out = HERE / "results" / f"B-{mode.name.lower()}.json"
        out.write_text(json.dumps({"config": f"B-{mode.name.lower()} (200M alone, desktop CPU)", "runs": runs},
                                  ensure_ascii=False, indent=2), encoding="utf-8")
        print(f"wrote {out}")
    return 0


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    sys.exit(main())
