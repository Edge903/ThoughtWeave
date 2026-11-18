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

## Voice dictation (V1 done 2026-11-18)
- [ ] **Real-device check.** Dictate in Korean and English on the Galaxy S23. The emulator has no on-device
      recognizer, so V1 was tested with a scripted recognizer only. Check also that recognition works without the
      `<queries>` entry; remove it if it is not needed.
- [ ] **V2: language download and long dictation.** S06 "Voice input" section with installed and supported speech
      languages and a download button (API 33+, progress on 34+); segmented sessions (API 33+) so dictation
      continues past pauses.
- [ ] **Microphone in the legal texts.** Mention microphone use in the agreement at the legal review
      (bump `CURRENT_AGREEMENT_VERSION`), and in the Play Data safety form (audio processed on device, not collected).
- [ ] **Documents.** Add the mic to S02-A/B in the requirements document and the screen design deck.

## Phase 6 (on hold since 2026-11-11)
- [ ] **Start Phase 6** when the hold is lifted: Galaxy S23 GPU and performance measurements, full flow tests,
      offline, themes, accessibility, Korean pass. Publish plan revision 10 first (proposed, not yet published).
