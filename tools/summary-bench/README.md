# Summary benchmark

Desktop tooling for the Summarization 200M evaluation (`docs/TODO.md`). Not part of the app build.

- `sets.json`: 4 three-memo sets (3 Korean, 1 English), 35 facts, 3 questions, every written date.
- `score.py`: scores result files: facts kept, questions still open, dates missing or added, output language.
- `test_score.py`: scorer tests, standard library only.

## Setup (Windows)
The MSYS2 `python` on PATH cannot load Windows wheels; use the python.org build.

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
