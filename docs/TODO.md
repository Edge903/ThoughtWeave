# ThoughtWeave – TODO

Open tasks. Remove an item (or move it to CLAUDE.md as a decision) once it is done.

## App
- [ ] **Memo DB backup policy.** Decide whether the memo database is included in cloud backup and device
      transfer (today it is; only `agreement.xml` is excluded). Update `backup_rules.xml` /
      `data_extraction_rules.xml` if needed, then SEC-03 and STC-01 in the requirements document.
- [ ] **R8 in release.** `isMinifyEnabled = false` for release. Decide whether to turn it on (came up for STC-07).
- [ ] **AI agreement legal review.** After review, remove `STATUS: DRAFT` from both agreement assets and fill in
      the provider, contact and governing-law placeholders (see CLAUDE.md, Consent).

## Requirements document (`docs/요구사항_정의서.docx`)
- [ ] **PER-04 / PER-05 targets.** Measure AI summary (2–3 memos) and schedule extraction times on the Galaxy S23
      (GPU) and replace the "OO초" placeholders.
- [ ] **2.5.2 screenshots (optional).** Retake with the device language set to Korean so they match the quoted
      labels; retake S03 "before consent" (`images/S03_1.png`) from a release build (it shows "Import model (debug)").
- [ ] **TOC.** Update the table of contents in Word (select it, F9, "Update entire table") after edits.

## Screen design document (`docs/04_화면설계서.pptx`)
Built from `docs/04_화면설계서+양식.pptx` with Korean emulator captures of sample data (2026-11-11).
- [ ] **Form footer.** The cover layout still has the form's confidentiality text ("petdoc", "Your company
      name") and "Copyrights 2025 © AISoftware". Kept as given; decide whether to change them.
- [ ] **Planning schedule.** The 서비스 개요 gantt shows only commit dates (11/3–11/11). Add earlier planning work
      (requirements, wireframes) if its dates are known.
- [ ] **AI times.** 기대효과 says the PER-04/05 times come after the Galaxy S23 measurement; fill them in then.
- [ ] **Keep in sync.** Retake the affected captures and update the slide when a screen changes.

## Language settings (planned 2026-11-11, before Phase 6)
Decided: UI, input and output language settings on a new S06; English and Korean now, structured so a language
is added in one place. `appcompat` returns for per-app UI language on Android 7–12. Output languages other than
the input are offered only for pairs that pass a real-model test. One global input setting, default Auto.
- [ ] **Translation with a stronger model.** en↔ko failed on Gemma 3 1B (dates changed, facts dropped), so no
      output language is offered. Re-run the translation measurement if the model changes, and add passing pairs
      to `LanguageProfiles.verifiedTranslations`.

## Summarization 200M evaluation (planned 2026-11-17, not started)
Proposal: add MediaPipe Text Summarizer (`tasks-text` 1.2.3, `summarization_quant_200m_2modes.litertlm`, TLDR /
KEYPOINTS) next to Gemma 3 1B. API verified with `javap`. Model confirmed 2026-11-17: official source
`storage.googleapis.com/mediapipe-models/text_summarizer/200m/latest/summarization_quant_200m_2modes.litertlm`,
117,587,968 bytes, MD5 `779d63f6cde3f9e6f64dbc031793543b` (GCS ETag, Last-Modified 2026-08-12), Gemma Terms of Use
(docs page). Supported languages are still undocumented. Desktop-only until a gate passes; the app is not touched.
Stop at the first failed gate and record the result in CLAUDE.md (Inference) as a rejected option.
- [x] **0. Prerequisites (done 2026-11-17).** Model copy at `D:\_P\scrapyard\summarization_quant_200m_2modes.litertlm`
      (handed over by the user): size and MD5 match the official file; SHA-256
      `8b2d4ef09236adb9ead3127325526ba1aa5a59feb7c5de2d3f5958f27479de59`. Python 3.12.10 installed with winget
      (user scope; the MSYS2 `python` on PATH cannot load Windows wheels). Venv `tools/summary-bench/.venv`
      (gitignored) with `mediapipe==1.2.3`. Python API: `text_summarizer.TextSummarizer`, `TextSummarizerOptions(
      base_options, mode, max_num_tokens, cache_dir, min_log_severity)`, `Mode.TLDR` / `Mode.KEYPOINTS`. Smoke test
      (one English memo, CPU) ran in both modes, load 0.2 s, summarize 0.3–0.4 s. Already visible: TLDR dropped the
      launch date, and both modes changed "runs" to "took place". Benchmark tooling goes in `tools/summary-bench/`
      (no app dependency); the model stays outside the repo.
- [x] **1. Benchmark set (done 2026-11-17).** The 2026-11-11 sets were not kept, so new ones were written:
      `tools/summary-bench/sets.json` (3 Korean + 1 English three-memo sets, 35 facts, 3 questions, 10 dates) and
      `score.py` (facts, open questions, dates missing/added, language; see its README). Self-check scores the
      memos themselves at 35/35 with no date changes; `test_score.py` covers the edge cases. New sets mean the old
      31/35 is not comparable; step 2 sets the baseline.
- [x] **2. Baseline A (done 2026-11-17).** `RealModelSummaryBench` (androidTest, skipped unless the model and
      sets are in /data/local/tmp) runs the app's own synthesis path and keeps the AI part. Emulator CPU, 3 runs
      per set: **29/35 facts, 2/3 questions, 3 date changes**, 4–6 s per set after an 11.5 s first load
      (`tools/summary-bench/results/A-gemma.json`). Runs were identical, so repeats show no run-to-run noise;
      Gate 3 must be judged per set instead. Read by hand, every loss the scorer reported is real: 12.1 and
      15만원 dropped (ko-1); "12월 14일 월요일" became "다음 주 수요일" (ko-2); "12/1부터" became "12월부터" and the
      privacy-policy to-do was dropped (ko-3); the car/subway question became a statement (en-1). The scorer does
      not catch wrong facts with no checked token: en-1 also turned "Tom pays for the hotel" into "Tom will cover
      all costs for the return flight".
- [ ] **Gate 1 – Korean.** B on the Korean memos, both modes: is the output Korean and readable? If not, stop.
- [ ] **3. B (200M alone).** TLDR and KEYPOINTS on each set as one concatenated text; desktop latency.
      Gate 2: B keeps at least as many facts as A, with no more date changes.
- [ ] **4. C (200M per memo → Gemma merge).** Only if Gate 2 passes, or if B is close and per-memo keypoints
      might help Gemma. Gate 3: C beats A on facts or dates by a clear margin, not within run-to-run noise.
- [ ] **5. On-device check.** Only if a configuration passed: Galaxy S23 latency and peak memory with both
      runtimes loaded in turn, `tasks-text` native library next to `litertlm-android` 0.19.0, APK/AAB size.
- [ ] **6. Integration decision.** If everything passed: plan revision, approval, then a phase (AI pack for the
      second model, S05 notices, agreement text + `CURRENT_AGREEMENT_VERSION` bump, pinned dependency).

## Phase 6 (on hold since 2026-11-11)
- [ ] **Start Phase 6** when the hold is lifted: Galaxy S23 GPU and performance measurements, full flow tests,
      offline, themes, accessibility, Korean pass. Publish plan revision 10 first (proposed, not yet published).
