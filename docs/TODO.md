# ThoughtWeave – TODO

Open tasks. Remove an item (or move it to CLAUDE.md as a decision) once it is done.

## App
- [ ] **AI agreement legal review.** After review, remove `STATUS: DRAFT` from both agreement assets and fill in
      the provider, contact and governing-law placeholders (see CLAUDE.md, Consent).

## Requirements document (`docs/요구사항_정의서.docx`)
- [ ] **PER-04 / PER-05 targets.** Measure AI summary (2–3 memos) and schedule extraction times on the Galaxy S23
      (GPU) and replace the "OO초" placeholders.

## Voice dictation (V1 and V2 done 2026-11-18; V3 dropped)
- [ ] **Real-device check.** Dictate in Korean and English on the Galaxy S23. The emulator's on-device recognizer
      runs but hears no voice, so the flows were tested with a scripted recognizer. Check on the S23: a long dictation
      with pauses (segmented session, no piece inserted twice), the 10 s silence end, the S06 language states and a
      download. Check also that recognition works without the `<queries>` entry; remove it if it is not needed.
- [ ] **Microphone in the legal texts.** Mention microphone use in the agreement at the legal review
      (bump `CURRENT_AGREEMENT_VERSION`), and in the Play Data safety form (audio processed on device, not collected).

## Phase 6 (on hold since 2026-11-11)
- [ ] **Start Phase 6** when the hold is lifted: Galaxy S23 GPU and performance measurements, full flow tests,
      offline, themes, accessibility, Korean pass.
