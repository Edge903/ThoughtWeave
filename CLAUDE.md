# ThoughtWeave – project conventions and decisions

Android memo + schedule app with on-device AI (Gemma 3 1B via LiteRT-LM) for memo summarization
and schedule extraction. Full architecture: https://claude.ai/artifact/ERQc39x57sBJ32nqSuTDxG
(revision 9, 2026-11-09). This file is the in-repo source of truth for conventions and decisions.

## Working rules
- Work phase by phase. Each phase ends with: `assembleDebug`, `assembleRelease`, `lintDebug`,
  unit tests, instrumented tests on the emulator, a launch check, and a git commit.
- Get the user's approval before starting each phase.
- Never invent APIs, dependencies or versions. Verify against Maven metadata, AAR contents (`javap`),
  or official docs, and record the verification here.
- No placeholder functionality presented as complete. Phase scaffolding uses
  `NotImplementedScreen`, which says so explicitly. None may remain at release.
- No new dependency without a reason recorded below.
- Never commit model files (`*.litertlm`, `*.task`, `*.tflite` are gitignored).

## Build configuration (verified 2026-11-04)
- AGP 9.1.1, Gradle 9.3.1, daemon JVM 21. AGP 9 **built-in Kotlin**: never apply `kotlin-android` or kapt.
- **Kotlin 2.3.21**: AGP 9.1.1 bundles KGP 2.2.10, which cannot read LiteRT-LM 0.19.0's Kotlin 2.4
  metadata. Declaring the Compose compiler plugin at 2.3.21 raises KGP for the build
  (`buildEnvironment` shows `2.2.10 -> 2.3.21`). LiteRT-LM raises runtime kotlin-stdlib to 2.4.0; this works.
  A compile probe against `litertlm-android:0.19.0` (Engine, EngineConfig, Backend, ModelInfo,
  ConversationConfig, sendMessageAsync Flow) succeeded with Java target 11.
- **compileSdk 36.1 / targetSdk 36 kept.** Compose 1.12+ (BOM 2026.08.00+), Navigation 2.10+ and
  Lifecycle 2.11+ need compileSdk 37 (AAR `minCompileSdk=37`). So: Compose BOM 2026.06.01
  (UI 1.11.4, Material 3 1.4.0) and Navigation 2.9.8. Do not upgrade these without raising compileSdk.
- minSdk 24 with core-library desugaring (`java.time`).
- KSP 2.3.13 + Hilt 2.60.1 (Hilt Gradle plugin supports AGP 9 since 2.59).
- `espresso-core 3.7.0` must stay in androidTest: Compose ui-test otherwise pulls an older Espresso
  that calls `InputManager.getInstance`, which was removed on API 36.
- Use `androidx.compose.ui.test.junit4.v2.createAndroidComposeRule` (v1 is deprecated).
- Room 2.8.5 (`minCompileSdk=34`, verified from the AAR 2026-11-08) with the Room Gradle plugin; schemas
  are committed under `app/schemas`. `room-ktx` is not used: its 2.8.5 jar is empty and its APIs are in
  `room-runtime`. kotlinx-coroutines-android 1.11.0 is declared directly because repositories expose `Flow`.
- Lifecycle 2.10.0 and `hilt-lifecycle-viewmodel-compose` 1.3.0 (both `minCompileSdk=35`; 2.11.0 and 1.4.0
  need 37, verified from the AARs 2026-11-08). kotlinx-coroutines-test 1.11.0 is for ViewModel unit tests.
- `appcompat` 1.8.0 (`minCompileSdk=34`, verified 2026-11-11) is back, only for per-app language on Android 12
  and below, which Google's backward-compatible API supports through AppCompat alone (user decision, 2026-11-11).
- Lint "newer version available" warnings for AGP, Kotlin, Compose BOM, Navigation, Lifecycle, Hilt ViewModel Compose, LiteRT-LM and core-ktx are
  expected (pinned on purpose, see above).

## Architecture
- Single `:app` module (plus `:gemma_pack` AI-pack module from Phase 5b). MVVM + small domain layer.
- Packages under `io.wit117.thoughtweave`: `navigation/`, `core/ui/{theme,component}`, `core/common`,
  `domain/{model,repository,usecase}`, `data/{local,ai,repository,di}`, `feature/...`.
- Immutable UI state via `StateFlow`; one-off effects (navigation, messages) are separate from state.
- Use cases only where they hold real logic.
- Navigation Compose with type-safe `@Serializable` routes (`navigation/Routes.kt`). Enum route
  arguments need `@Keep`.
- The outer Scaffold in `ThoughtWeaveApp` owns only the bottom bar. Each screen owns its own Scaffold
  (top bar + `ContextFab`). Tab switches use `navigateToTopLevel` (saveState/restoreState).
- Icons: Material Symbols vector drawables copied from google/material-design-icons
  (`symbols/android/<name>/materialsymbolsoutlined/<name>_24px.xml`, Apache-2.0), with
  `android:tint` removed. No icon library dependency. List the license in S05 "기타 오픈소스".
- Languages: `AppLanguages` (`domain/model/AppLanguage.kt`) is the single list of supported languages; add new
  ones there. Per-app UI language goes through `AppCompatDelegate.setApplicationLocales` (`MainActivity` is an
  `AppCompatActivity` with an AppCompat window theme; `autoStoreLocales` in the manifest covers Android 12 and
  below). `generateLocaleConfig` lists languages in Android 13+ settings; the default is in `res/resources.properties`.
- UI strings: English in `values/`, Korean in `values-ko/`. All user-visible text goes through resources.
  Use phrase line breaking (`LineBreak.WordBreak.Phrase`) for multi-line Korean text.

## Screens (IDs are stable; keep them in code comments)
| ID | Name | Notes |
|---|---|---|
| S01 | 메모 목록 | Tab. FAB → S02-A. Star icon left of each card toggles pin. Overflow → S05. Search. |
| S02-A/B | 메모 작성 / 조회·수정 | Shared memo editor. Delete only in S02-B. S02-B FAB → POP03 AI mode (save first if dirty). |
| S02-C | AI 합성 결과 편집 | Shared memo editor. Saves a new memo plus source references (sources never shown). |
| S03 | AI 메모 통합 | Tab. Two-column grid, multi-select, long-press → POP01. FAB needs ≥2 selected, AI ready and consent. |
| S04 | 일정 확인 | Tab. Grouped by deadline date. Tap → POP02. FAB → POP03 MANUAL. |
| S05 | 라이선스 및 고지 | Licenses, notices, agreement, withdraw AI consent. |
| S06 | 설정 | From the S01 overflow, above S05. App language, AI input language; AI output language only once a translation pair is verified. |
| POP01 | 메모 미리보기 | In-screen sheet on S03; selection untouched. |
| POP02 | 일정 상세 | Shared schedule editor, `EXISTING` mode, with delete. |
| POP03 | 일정 추출 및 생성 | Shared schedule editor, `MANUAL` / `AI_EXTRACTED`. |
| POP04 | AI 기능 이용 동의 | Consent before any AI use. |

## Product decisions
- Synthesis has one mode only: summarize. No mode selector, no mode column.
- S03 selection: cleared after a successful save; kept on cancel, back or failure.
- S01 and S03 order: pinned first, then most recently updated. No sort menu.
- Schedule: title and deadline (`end_at`) required; `start_at` optional and ≤ deadline when set;
  all-day supported. Saving only on an explicit 저장 press. AI never invents timestamps; it fails instead.
- Memo title optional; empty title is stored as "" and displayed as "무제" / "Untitled".
- Pinning does not change `updated_at`, so it never reorders memos within their group.

## Inference
- `GemmaInferenceEngine` loads on first use (GPU, then CPU; CPU only on x86 emulators), runs one request at a
  time and frees the engine after 60 s idle. Instrumented tests script replies through the debug-only
  `InferenceEngineTestOverride`; it is never set in the running app.
- Schedule extraction: `readScheduleDates` reads dates and times from the memo in code; the model only writes
  the title. Tested on the real model (2026-11-09): Gemma 3 1B returned null or garbage dates from JSON prompts.
  A memo with no readable date never reaches the model. A written year is used as written, never guessed; a
  weekday next to an explicit date belongs to that date, and the explicit date wins if they disagree
  (user decision, 2026-11-11). Cases are in `CommaDateFormatsTest`.
- Summaries use a system instruction ("condense, never answer") and one worked example whose memo asks a question.
  On the real model this stopped it answering questions and removed markdown; an example with a dated memo made it
  drop whole memos, so keep the example undated. `withoutInventedYears` removes years absent from the sources,
  because prompting did not stop the model adding them.
- Language profiles (`data/ai/language`): each language's date rules, prompts and example live in one
  `LanguageProfile`, registered in `LanguageProfiles`. Written-out dates of every language are always read;
  numeric order ("12/3") follows the input language. Prompts follow the output language.
  `ResolveAiLanguageUseCase` picks input (setting, else script detection, else app language) and output per request.
- Translation (output ≠ input) is offered only for pairs in `LanguageProfiles.verifiedTranslations`. en↔ko failed on
  Gemma 3 1B (2026-11-11): it changed dates and weekdays, dropped facts and garbled titles, so the set is empty and
  S06 hides the output setting. Re-measure when the model changes.
- Gemma 3 1B answered Korean memos in English when told to use "the same language", so Korean memos get a Korean
  prompt. Its context is 4,096 tokens; over-long input fails with a "too long" error, reported to the user.

## Consent (POP04)
- `AiAvailability` (`AiAvailabilityUseCase`) gates AI actions. S03: FAB stays disabled until READY; its banner
  opens POP04. S02-B: extraction opens POP04 first, then continues.
- Acceptance is stored per `CURRENT_AGREEMENT_VERSION` in SharedPreferences, excluded from backup and device
  transfer so consent is per install. Bump the version whenever the agreement text changes.
- Removing `STATUS: DRAFT` from both agreement assets is what releases the `bundleRelease` guard. Do this only
  after legal review, and fill in the provider, contact and governing-law placeholders.

## Legal texts (S05)
- `assets/legal` holds verbatim copies: Gemma Terms and Prohibited Use Policy from ai.google.dev (2026-11-08),
  Apache-2.0, and GPLv2 + Classpath Exception (`desugar_jdk_libs` ships under it, per its POM).
- `other_open_source.txt` lists only what the app ships today. Play Core SDK terms come from the `ai-delivery`
  AAR's LICENSE file; LiteRT-LM's license and third-party notices come from its AAR. Refresh them on upgrades.

## On-device AI (Phase 5)
- Model `gemma3-1b-it-int4.litertlm`, 584,417,280 bytes,
  SHA-256 `1325ae366d31950f137c9c357b9fa89448b176d76998180c08ceaca78bba98be`, LiteRT-LM 0.19.0.
- Delivery: Play AI pack **fast-follow** (`com.google.android.play:ai-delivery:0.2.0-beta01`).
  No external hosting, no INTERNET permission. Wi-Fi by default; mobile data only after the user taps
  to allow it (`showConfirmationDialog`). Load only `STORAGE_FILES` packs with asset offset 0.
- `ai-delivery` 0.2.0-beta01 API and status codes verified with `javap` (2026-11-09). Failed Play tasks are
  read as `ApiException.statusCode`. `:gemma_pack` uses AGP's `com.android.ai-pack` plugin.
- `asset-delivery` (required by `ai-delivery`) brings WorkManager and its `WAKE_LOCK`, `ACCESS_NETWORK_STATE`
  and `RECEIVE_BOOT_COMPLETED` permissions. The release manifest still has no `INTERNET` permission.
- The model is gitignored and copied to `gemma_pack/src/main/assets` by hand. `bundleRelease` fails if it is
  missing. The download may start before consent; consent gates AI use only.
- Debug builds: model import via the system file picker (`src/debug` only).
- Integrity: full SHA-256 on first install or import; size + marker on later launches; full hash only
  after an engine init failure.
- Only `GemmaInferenceEngine` imports LiteRT-LM. Only the delivery layer imports Play APIs.
  The UI sees `AiAvailability` only.
- Gemma notices: "Gemma is provided under and subject to the Gemma Terms of Use found at
  ai.google.dev/gemma/terms" ships in the pack and in base assets.
- **The ThoughtWeave user agreement is a DRAFT that requires legal review.** `bundleRelease` must fail
  while it is marked DRAFT (override `-PallowDraftLegal=true` for internal testing only).

## Testing
- Emulator: AVD `Medium_Phone_API_36.1` (x86_64, CPU inference only).
- Real device: the user's Galaxy S23 (SM8550) for GPU, performance and memory measurements.
  Its data partition is 8G, because the real model does not fit in the default 6G.
- Local AI-pack testing: AGP's cached bundletool 1.18.3 with `--local-testing` (approved). The cached jar is not
  runnable: use main class `com.android.tools.build.bundletool.BundleToolMain` on JDK 11+, with AGP's resolved
  classpath from `buildEnvironment`, `--aapt2` from the SDK, and the debug keystore. Build with
  `bundleRelease -PallowDraftLegal=true`.
- Play fast-follow and Wi-Fi consent flows: Play internal testing track (deferred until Console access).
