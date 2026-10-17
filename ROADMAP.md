# PhotoGlyph roadmap

Source of truth: `AGENTS.md` and `docs/`. Follow milestone order unless the user explicitly reprioritizes a deliverable. On 2026-10-15 the user authorized continuing without intermediate replies; on 2026-10-17 they prioritized the alphabet-to-`.gfont` path over contour work, so a small M9 codec foundation was started early and is documented here. Keep each slice tested and documented; leave real device/KDraw checks explicitly open.

Estimates are person-days, not promises of elapsed time. Total for the **original plan**: **31–43 person-days**.

## User clarification — 2026-10-13

`docs/01-требования-mvp.md` records the clarified target: ordinary notebook photos, multiple **partial** handwriting sets, local phone storage without an account, and separate `.gfont` exports. KDraw owns cross-set substitution; PhotoGlyph must not merge sets or fabricate missing glyphs. Page composition/printing is not part of the requested fonts-only workflow.

## User clarification — 2026-10-15: confirmed product outcomes

The user confirmed two distinct PhotoGlyph workflows:

1. **Handwriting alphabet:** photograph a page with handwritten letters; PhotoGlyph detects the letters and produces an alphabet/handwriting set from what is present. The set may be incomplete. KDraw supplies missing letters from other alphabets; PhotoGlyph must not invent missing glyphs or merge sets.
2. **Picture contours:** from a photo of a picture, produce a separate picture showing black contours. This is not handwriting centerline extraction and does not create an alphabet.

**Android entry flow:** when the Android app opens, request camera permission if needed and immediately show the camera preview once granted. If access is denied or the camera is unavailable, provide photo import as a fallback. The separate-letter web lab is a developer diagnostic, not the product home screen.

For the contour mode, only the black-contour image result is confirmed. File format, background, editing controls, and copy/share/download actions are not specified here; do not treat earlier design proposals as approved requirements. The page layout and letter-segmentation/recognition approach also remain implementation questions.

## User priority clarification — 2026-10-17

The user reiterated that the primary deliverable is their own handwriting alphabet exported as `.gfont`; black-and-white page preparation is an internal step, not the product result, and the separate picture-contour mode is secondary. Do not let further contour-mode polish displace the alphabet → review → partial-set save → `.gfont` path. An early codec/export foundation is now connected to a manual per-letter crop/label builder and local save. Automatic page segmentation/character recognition, connected-cursive splitting, stroke editing, and real KDraw compatibility remain unfinished.

## Remaining work plan — checked 2026-10-17

GitHub tracking: [Issue #2 — План оставшихся работ: PhotoGlyph](https://github.com/evzhem/photo-to-glyph/issues/2) and [Issue #3 — Android camera-first entry](https://github.com/evzhem/photo-to-glyph/issues/3). Issue #3 records the camera-first clarification and current code status.

### Current baseline

- **M0 — complete:** synthetic core spike, 21 production-function checks and SVG smoke are recorded in `docs/M0-report.md`.
- **M1 — complete:** Gradle/CI foundation and diagnostic sandbox; PR #1 is merged.
- **M2 — partial; device evidence received:** photo import/camera and page preparation are implemented. The 2026-10-16 `./tools/m2-standalone-check.sh` passed (17 page-preparation checks, 3 ink-component checks, 4 picture-contour checks and 5 preparation API checks, plus M0/M1 standalone checks); full GitHub Actions run [38883213683](https://github.com/evzhem/photo-to-glyph/actions/runs/38883213683) passes Gradle build/tests, Android lint and CLI smoke. On 2026-10-17 the user confirmed camera use and gallery import on Android; screenshots show prepared output. However, the green quad still covers the photo bounds and the binary result contains tabletop noise, so page-boundary correction/rectification is not yet accepted. Permission-denial/camera-unavailable branches and device model/API are also unknown. We cannot reproduce device checks locally (`adb`/SDK unavailable). See [`docs/M2-report.md`](docs/M2-report.md).
- **Picture contours — code and CI implemented:** independent Kotlin/JVM Sobel/NMS image processing, a separate Android preview mode and four synthetic production-function tests are present; full Gradle/Android CI run [39017154083](https://github.com/evzhem/photo-to-glyph/actions/runs/39017154083) passes. Real-photo quality and device behavior remain unverified.
- **M3 — partial manual assembly path:** page preparation preserves small marks; the core returns deterministic 8-connected ink components/pixel-exact crops and `vectorizeBinary` turns a selected binary crop into open centerline chains without thresholding it again. Android now offers separate Cyrillic/Latin uppercase and lowercase pickers with independent per-case progress (for example, `А` and `а` are different glyph entries), then lets a user crop and label one letter at a time, inspect the turquoise trace, adjust each baseline and choose actual cap/x-height reference samples. It saves only confirmed glyphs into one independent partial set and opens the local library for export. No OCR, automatic cursive segmentation, pen-order recovery or stroke editor exists. The earlier manual builder passed CI run [39017154083](https://github.com/evzhem/photo-to-glyph/actions/runs/39017154083); the newly added case picker/tests still need CI. Human/device acceptance is also pending. See the original [`docs/M3-report.md`](docs/M3-report.md) and the manual-builder follow-up [`docs/M3-manual-builder-report.md`](docs/M3-manual-builder-report.md).
- **M5 foundations and manual app flow:** app-private JSON persistence, library, shared normalization and the manual page-to-set builder are implemented; two normalizer checks and four store checks pass in CI. The builder requires explicit character labels, baselines and cap/x-height references; automatic label/metric detection, sidebearing/slant editing, stroke correction and device QA remain unfinished. Full build/test/lint/smoke for the initial builder run [39017154083](https://github.com/evzhem/photo-to-glyph/actions/runs/39017154083) passed; the new case-picker follow-up awaits CI. See [`docs/M5-report.md`](docs/M5-report.md).

### Ordered implementation plan

1. **Keep M2 page-quality QA open without blocking the alphabet path.** Camera-first permission/capture and gallery import are implemented and the user confirmed both on Android. The current prepared screenshot still contains tabletop noise; exact page-boundary/rectification and permission-denial/camera-unavailable evidence remain pending. Per the 2026-10-17 priority, this QA must not displace the manual alphabet builder; it will be addressed as input quality for the full page workflow.
2. **Specify automatic page recognition.** Define ordinary-page assumptions, reading order, segmentation, character confirmation, and repeated/ambiguous marks. A manual crop-and-label path now exists; it does not solve automatic segmentation or connected cursive. Do not require a grid or select an OCR/model/dependency without a documented basis. Establish synthetic expectations and acceptance criteria before adding automatic recognition.
3. **Extend alphabet assembly from manual review toward recognition.** The app now offers a separate Cyrillic/Latin uppercase/lowercase picker and progress, then allows one manually selected crop, one confirmed BMP label, baseline adjustment and caller-selected cap/x-height references; only confirmed glyphs are saved. Next, improve segmentation/recognition without pretending components are letters. Incomplete sets remain valid and missing glyphs must never be invented.
4. **Make centerline extraction production-ready.** Improve the M0 spike where needed (the synthetic «Ж» currently yields 8 chains instead of 3 and diagonals are jagged); add production tests for stroke topology, cycles, no-fill output and the per-glyph performance budget. The manual builder shows a preliminary trace; preserve centerlines rather than tracing both edges of a pen stroke.
5. **Complete correction and local partial-set tools.** App-private JSON snapshots, separate sets, the library and manual page-to-set save path now exist. Still required: edit/reorder/split/merge paths, improve labels after auto-recognition, handle drafts/recovery, and confirm the local workflow on Android. Do not fill gaps or merge sets.
6. **Validate `.gfont` with KDraw.** The documented ZIP glyph-record codec, round-trip validation and separate library export are implemented. Format scale/optional service records and KDraw acceptance remain unknown; use a permitted real file/device check before claiming compatibility. Do not connect to the plotter or implement PhotoGlyph substitution.
7. **Separate black-contour image mode — code/CI implemented.** The app has a dedicated camera/import mode and shows the output of a separate pure Kotlin Sobel/NMS function. Four production-function checks cover a hand-specified edge, a blank image, threshold behavior and validation; full current-branch Gradle CI run [39017154083](https://github.com/evzhem/photo-to-glyph/actions/runs/39017154083) passes. The white raster backing is an implementation choice, not a confirmed output contract; no save/export format or copy/share/download actions are added. Real-photo/device acceptance remains pending.
8. **End-to-end QA and release preparation.** Test both workflows on real Android hardware with consented test inputs; verify privacy, errors, performance and Russian/English UI; update docs and CI. Confirm the distribution channel before packaging a release.

No new time estimates are assigned: page-recognition method and its acceptance criteria have not been finalized. The contour core/UI has an initial implementation, but its quality on real photos and any persistent output contract remain unverified/unspecified.

The older milestone table below is retained for traceability. Its M2/M3 recognition and M6 export/page details are draft estimates, not authoritative acceptance criteria for the clarified workflows. Existing image-preparation code does not mean page-letter recognition or picture-contour extraction is complete.

| Milestone | Scope | Acceptance criteria | Person-days | Status |
| --- | --- | --- | ---: | --- |
| **M0** | Photo → B/W → Zhang–Suen → polylines → SVG spike on «Ж», «Б», «в», «Ш», «ф». If photos are absent, use original synthetic 300 × 300 PNGs, 6 px strokes on white. `docs/M0-report.md`, per-letter stroke counts, closed-polyline counts and timings; decision A/B/C. | SVG opens in Inkscape as strokes, not outlines. Zero closed exported polylines. Report honestly lists failures and synthetic-vs-real limitations. A: skeleton viable; B: preprocessing needed; C: change to PNG-only export, which requires a product decision. | 1–2 | Synthetic spike verified; branch B; awaiting human Inkscape check and user acceptance |
| **M1** | Gradle `:core` / `:app` / `:cli` / tools structure, ktlint/detekt, no-`android.*` source test, `:cli:smoke`, GitHub Actions. | Green CI on a no-functional-change commit: `./gradlew build :core:test :cli:smoke`. | 1–2 | Verified in CI, including unchanged-source commit; local Gradle download blocked; sandbox running |
| **M2** | Shared image input/preparation: gallery/file import, CameraX capture, crop/quarter-turn rotation, four-corner page detection with manual repair, perspective rectification and B/W controls. Ordinary notebook photos are primary; the printable A4 grid with marks/labels remains an additional input. Page OCR is separate work; picture contours have their own module and tests. | A page photographed at about 30° under side lighting becomes a straight binary image; original grid-profile alignment remains a test case. Separate synthetic automated evidence from actual camera/photo acceptance. | Original 3–4; revised scope not newly estimated | In progress; device QA pending |
| **M3** | Ordinary-page input, glyph crops, label confirmation, manual correction and partial-set coverage. | Automatic recognition/cropping is independently validated; wrong labels can be repaired; missing glyphs are never fabricated. | 3–4 (original grid estimate, not revised) | Partial: manual crop-to-label builder now has a Cyrillic/Latin case picker and separate uppercase/lowercase progress, plus baseline/reference controls, core crop-to-centerline path, local save/export. New case-picker tests await CI. Automatic segmentation/recognition, connected-cursive splitting, stroke editing and device QA remain open. |
| **M4** | Production Zhang–Suen → graph → pruning → polylines → RDP + smoothing → stroke order (greedy TSP + 2-opt). | ≥ 80% of fixtures have the correct stroke count and zero closed exported polylines; processing ≤ 2 s/glyph. M0 is a feasibility spike, not acceptance of the production pipeline. | 4–6 | Not started |
| **M5** | Shared baseline, x-height, cap-height, em-box, sidebearings, slant; `glyphset.json` ↔ `GlyphSet`; local set storage. | Set glyphs align to a common baseline (three-descender test); JSON round-trip is lossless. | 3 | Partial: JSON codec, app-private store/library, production normalization and a manual builder with user-supplied baselines/reference samples pass CI. Automatic metrics, sidebearing/slant completion, planned Room backend and stroke editor remain pending. |
| **M6** | `ExportAdapter`, SVG/DXF/PNG/JSON, share and print. | SVG opens in Inkscape as strokes; DXF opens in LibreCAD; no-fill test rejects deliberately corrupted SVG. | 3–4 | Not started |
| **M7** | `TextComposer`, A4 preview, writing animation in stroke order. | «Привет, мир!» composes onto A4 with size/slant/spacing settings; page export contains only strokes. | 3–4 | Not started |
| **M8** | Stroke editor: delete/split/merge/reorder/reverse, recalculate with parameters, finger drawing, undo/redo, source-photo comparison. | A broken fixture glyph is repaired to a usable state in ≤ 60 s, evidenced by video. | 4–5 | Not started |
| **M9** | CLI `gfontInspect` on an authorized real set, resolve format unknowns, independent `GfontCodec`, round-trip test, KDraw validation, instructions, SVG/PNG fallback. | KDraw writes text on paper using our set; «Проверка недостающих слов» reports no missing characters. No direct hardware protocol integration. | 3–5 | Partial, started early per 2026-10-17 priority: documented-subset pure Kotlin/JVM codec, round-trip/malformed-record tests, and separate library export pass CI; manual alphabet creation now feeds saved sets. No real-file, scale/service-entry or KDraw compatibility evidence yet; see [`docs/M9-codec-foundation-report.md`](docs/M9-codec-foundation-report.md). |
| **M10** | QA: 3 handwritings × 3 lighting conditions; speed and APK-size measurements; empty/error states; signed APK in GitHub Releases; README screenshots. | QA evidence and release artifacts exist, signing secrets are not committed, known limitations are documented. Release channel requires confirmation. | 3–4 | Not started |

## Separate picture-contour mode — clarified 2026-10-15

See `docs/05-рисунок-по-контуру.md`. The confirmed result is a separate image with black contours derived from a photo. It must not be confused with the centerline strokes used for handwriting alphabets. Core/UI code and synthetic production-function tests are implemented independently of glyph no-fill/open-polyline validation; picture contours may naturally close. Full Gradle/Android CI passed in run [39017154083](https://github.com/evzhem/photo-to-glyph/actions/runs/39017154083); real-photo/device acceptance remains pending. The preview uses a white raster backing as an engineering choice, not as a confirmed output-format/background requirement. No file format or extra save/copy/share actions are specified or implemented.

## Gates

- Each milestone requires production code, tests calling that code, successful applicable build and smoke, a `CHANGELOG.md` entry and a six-part report (`AGENTS.md`).
- Before M1 there is no Gradle: use a real standalone Kotlin compilation/test/smoke command and include the full output.
- Human-only device/UI/Inkscape checks cannot be replaced by a claimed automated pass.
- At most 15 English commits per milestone. First commit: project documents. The active Arena session is fixed to `arena/6d0fb259-photo-to-glyph`; work, commits and pushes remain on it, and `main` is never modified. Do not claim separate milestone branches or PRs when the platform fixes one branch.
- M9 cannot be hardware-accepted on synthetic round trips alone. Real-file scale/service-entry questions and device validation remain explicit unknowns.

## Scope reduction order

Remove, in this order if time is constrained:

1. Tesseract character hints (not a required dependency).
2. DXF.
3. Writing animation.
4. TTF (not a required outline-font implementation).

**Never remove:** the confirmed page-photo → detected letters → alphabet workflow, acceptance of incomplete alphabets, KDraw-owned missing-letter substitution, the separate black-contour image result, the grid sheet, the manual stroke editor, or the prohibition of filled handwriting glyphs.

## Pending decisions

Android minimum: API 26, following the proposal and the user's authorization to continue with M1. Confirm GitHub Releases APK vs Google Play. Grid-free notebook photos are the clarified primary workflow, and a manual crop builder now exists. Before adding automatic segmentation/recognition, document the approach, test criteria and revised scope; do not re-ask the answered product-direction question or block manual alphabet work. `.gfont` units/font size/service entries/companion text file remain unconfirmed until an authorized real-file/KDraw check; the current codec is explicitly partial. KDraw-owned substitution is user-reported and needs real-program validation.
