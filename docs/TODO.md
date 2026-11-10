# ThoughtWeave – TODO

Open tasks. Remove an item (or move it to CLAUDE.md as a decision) once it is done.

## App
- [ ] **Schedule extraction misses comma-separated dates.** A date written with a comma separator was not
      detected, while the Korean form (`n월 m일`) was. Test more formats to pin down which ones fail, then fix
      `readScheduleDates` (`data/ai/parse/ScheduleDateReader.kt`) and add unit tests for them.
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

## Repository
- [ ] **Commit** `docs/요구사항_정의서.docx`, the images in `docs/images/` and this file.
