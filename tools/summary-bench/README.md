# Summary benchmark

Desktop tooling for comparing summary models. Not part of the app build. First used for Summarization 200M,
rejected 2026-11-17 (CLAUDE.md, Inference): on the Korean sets it answered in English and invented content.

- `run_200m.py`: configuration B (Summarization 200M alone, both modes) via `mediapipe`.

- `sets.json`: 4 three-memo sets (3 Korean, 1 English), 35 facts, 3 questions, every written date.
- `score.py`: scores result files: facts kept, questions still open, dates missing or added, output language.
- `test_score.py`: scorer tests, standard library only.

## Setup (Windows)
The MSYS2 `python` on PATH cannot load Windows wheels; use the python.org build (uninstalled after the
2026-11-17 run; reinstall with `winget install --id Python.Python.3.12 --exact --scope user`).
`score.py` and `test_score.py` need only the standard library; `run_200m.py` needs the venv.

```
%LOCALAPPDATA%\Programs\Python\Python312\python.exe -m venv .venv
.venv\Scripts\python.exe -m pip install mediapipe==1.2.3
```

The model stays outside the repo (`*.litertlm` is gitignored). Check a copy with SHA-256
`8b2d4ef09236adb9ead3127325526ba1aa5a59feb7c5de2d3f5958f27479de59`.

## Use
```
.venv\Scripts\python.exe test_score.py
.venv\Scripts\python.exe score.py --self-check
.venv\Scripts\python.exe score.py results\B-tldr.json results\A-gemma.json
```

Baseline A (Gemma, the app's synthesis path) runs on a device or emulator:

```
adb push gemma3-1b-it-int4.litertlm /data/local/tmp/
adb shell mkdir -p /data/local/tmp/summary-bench
adb push sets.json /data/local/tmp/summary-bench/
gradlew :app:installDebug :app:installDebugAndroidTest
adb shell am instrument -w -e config A -e class io.wit117.thoughtweave.RealModelSummaryBench io.wit117.thoughtweave.test/androidx.test.runner.AndroidJUnitRunner
adb pull /sdcard/Android/data/io.wit117.thoughtweave/files/summary-bench/A-gemma.json results/
```

`config`: `A` the app's synthesis path; `C` Gemma lists each memo's facts, then the app's prompt merges the lists
(the extraction prompts live in the test); `D` the code's sentence list per memo is merged by the app's prompt.
`E-gemma.json` is a retired config: the dated summary example, since adopted by the app
(`A-gemma-dated-example.json` is the app's path afterwards and matches it).
`runs` defaults to 1, since Gemma's runs were identical. Results for 2026-11-17 are in `results/`.

The scorer counts kept facts only; it does not notice a wrong fact that has no checked token, so read the
outputs too.

A result file is `{"config": "...", "runs": [{"set": "ko-1", "output": "...", "seconds": 0.4}]}`. Several runs
per set are allowed; totals average them, so run-to-run noise is visible.

## Scoring rules
- A fact is kept when each of its token groups has a variant in the output. Case and spaces are ignored;
  digits must not run on, so "12.1" does not match "12.15".
- A question is kept when some sentence has its tokens and still reads as a question ("?", "-까", "-ㄹ지",
  "여부", "미정", "whether", "not decided"). An answered question counts as lost.
- Dates are read as month-day from "12월 5일", "12.5", "12/5", "Jan 9", "9 January". Relative dates
  ("12일 후") are not read, so a date rewritten that way counts as missing. This approximates the app's
  `TextDateReader`, which is Kotlin; "3.14" reads as March 14 in both.
- Language: Korean sets need at least 60% Hangul among letters; English sets at most 5%.
