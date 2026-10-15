# PhotoGlyph roadmap

Source of truth: `AGENTS.md` and `docs/`. Work strictly in order. On 2026-10-15 the user explicitly authorized continuing the remaining product work sequentially without intermediate replies; earlier stop-and-wait instructions are superseded for this authorized continuation. Keep each stage tested and documented; report only at completion or an objective blocker. This does not resolve requirements that genuinely need user/device/KDraw input.

Estimates are person-days, not promises of elapsed time. Total for the **original plan**: **31–43 person-days**.

## User clarification — 2026-10-13

`docs/01-требования-mvp.md` records the clarified target: ordinary notebook photos, multiple **partial** handwriting sets, local phone storage without an account, and separate `.gfont` exports. KDraw owns cross-set substitution; PhotoGlyph must not merge sets or fabricate missing glyphs. Page composition/printing is not part of the requested fonts-only workflow.

## User clarification — 2026-10-15: confirmed product outcomes

The user confirmed two distinct PhotoGlyph workflows:

1. **Handwriting alphabet:** photograph a page with handwritten letters; PhotoGlyph detects the letters and produces an alphabet/handwriting set from what is present. The set may be incomplete. KDraw supplies missing letters from other alphabets; PhotoGlyph must not invent missing glyphs or merge sets.
2. **Picture contours:** from a photo of a picture, produce a separate picture showing black contours. This is not handwriting centerline extraction and does not create an alphabet.

**Android entry flow:** when the Android app opens, request camera permission if needed and immediately show the camera preview once granted. If access is denied or the camera is unavailable, provide photo import as a fallback. The separate-letter web lab is a developer diagnostic, not the product home screen.

For the contour mode, only the black-contour image result is confirmed. File format, background, editing controls, and copy/share/download actions are not specified here; do not treat earlier design proposals as approved requirements. The page layout and letter-segmentation/recognition approach also remain implementation questions.

## Remaining work plan — checked 2026-10-15

GitHub tracking: [Issue #2 — План оставшихся работ: PhotoGlyph](https://github.com/evzhem/photo-to-glyph/issues/2) and [Issue #3 — Android camera-first entry](https://github.com/evzhem/photo-to-glyph/issues/3). Issue #3 records the camera-first clarification and current code status.

### Current baseline

- **M0 — complete:** synthetic core spike, 21 production-function checks and SVG smoke are recorded in `docs/M0-report.md`.
- **M1 — complete:** Gradle/CI foundation and diagnostic sandbox; PR #1 is merged.
- **M2 — partial:** photo import/camera and page preparation are present. The latest `./tools/m2-standalone-check.sh` passed (16 page-preparation checks, 4 picture-contour checks and 5 preparation API checks, plus M0/M1 standalone checks). The last GitHub Actions Gradle build passed before the picture-contour changes; CI for the current changes is pending. Real Android device/emulator behavior is still unverified.
- **Picture contours — code implemented:** independent Kotlin/JVM Sobel/NMS image processing, a separate Android preview mode and four synthetic production-function tests are present. Real-photo quality, current-branch Gradle CI and device behavior remain unverified.
- **Not implemented end-to-end:** handwritten page → recognized alphabet, correction/editor, local partial-alphabet library and `.gfont` export.

### Ordered implementation plan

1. **Finish M2 device verification and camera-first entry.** The app code requests camera permission on launch, opens a full-screen CameraX preview after permission, keeps gallery import as fallback, and removes synthetic single-letter/demo controls from the user app. GitHub Actions Gradle build/tests/smoke pass. Remaining: test the permission/capture/fallback flow and page preparation on an Android device/emulator; record manual versus automated results in an M2 report.
2. **Specify page recognition.** Define page/layout assumptions, reading order, segmentation, how letters are identified, and how repeated/ambiguous marks are handled. Keep ordinary page photos as the primary input; do not require a grid unless the user changes that requirement. Establish original synthetic test pages and acceptance criteria before selecting an algorithm/model.
3. **Implement letter detection and alphabet assembly.** Segment the page into glyphs, identify the symbols, and produce a set from only the letters actually found. Test detection/cropping/labels against independent synthetic expectations. Never invent missing glyphs; incomplete sets must remain valid.
4. **Make centerline extraction production-ready.** Improve the M0 spike where needed (the synthetic «Ж» currently yields 8 chains instead of 3 and diagonals are jagged); add production tests for stroke topology, cycles, no-fill output and the per-glyph performance budget. Preserve centerlines rather than tracing both edges of a pen stroke.
5. **Support correction and local partial sets.** Let users review/correct mistaken letter labels and strokes; create, name and persist multiple sets locally; allow saving/exporting partial sets without filling gaps or merging sets.
6. **Export and validate with KDraw.** Resolve `.gfont` format details, implement independent export and round-trip tests, then verify separate partial sets and KDraw-owned missing-letter substitution with real files. Do not connect to the plotter or implement PhotoGlyph substitution.
7. **Separate black-contour image mode — code implemented.** The app has a dedicated camera/import mode and shows the output of a separate pure Kotlin Sobel/NMS function. Four production-function checks cover a hand-specified edge, a blank image, threshold behavior and validation. The white raster backing is an implementation choice, not a confirmed output contract; no save/export format or copy/share/download actions are added. Current-branch Gradle CI and real-photo/device acceptance remain pending.
8. **End-to-end QA and release preparation.** Test both workflows on real Android hardware with consented test inputs; verify privacy, errors, performance and Russian/English UI; update docs and CI. Confirm the distribution channel before packaging a release.

No new time estimates are assigned: page-recognition method and its acceptance criteria have not been finalized. The contour core/UI has an initial implementation, but its quality on real photos and any persistent output contract remain unverified/unspecified.

The older milestone table below is retained for traceability. Its M2/M3 recognition and M6 export/page details are draft estimates, not authoritative acceptance criteria for the clarified workflows. Existing image-preparation code does not mean page-letter recognition or picture-contour extraction is complete.

| Milestone | Scope | Acceptance criteria | Person-days | Status |
| --- | --- | --- | ---: | --- |
| **M0** | Photo → B/W → Zhang–Suen → polylines → SVG spike on «Ж», «Б», «в», «Ш», «ф». If photos are absent, use original synthetic 300 × 300 PNGs, 6 px strokes on white. `docs/M0-report.md`, per-letter stroke counts, closed-polyline counts and timings; decision A/B/C. | SVG opens in Inkscape as strokes, not outlines. Zero closed exported polylines. Report honestly lists failures and synthetic-vs-real limitations. A: skeleton viable; B: preprocessing needed; C: change to PNG-only export, which requires a product decision. | 1–2 | Synthetic spike verified; branch B; awaiting human Inkscape check and user acceptance |
| **M1** | Gradle `:core` / `:app` / `:cli` / tools structure, ktlint/detekt, no-`android.*` source test, `:cli:smoke`, GitHub Actions. | Green CI on a no-functional-change commit: `./gradlew build :core:test :cli:smoke`. | 1–2 | Verified in CI, including unchanged-source commit; local Gradle download blocked; sandbox running |
| **M2** | Shared image input/preparation: gallery/file import, CameraX capture, crop/quarter-turn rotation, four-corner page detection with manual repair, perspective rectification and B/W controls. Ordinary notebook photos are primary; the printable A4 grid with marks/labels remains an additional input. Page OCR is separate work; picture contours have their own module and tests. | A page photographed at about 30° under side lighting becomes a straight binary image; original grid-profile alignment remains a test case. Separate synthetic automated evidence from actual camera/photo acceptance. | Original 3–4; revised scope not newly estimated | In progress; device QA pending |
| **M3** | Fiducial-based cell detection with fallback; glyph crops; prefill character by cell; manual correction; merge/split; coverage indicator (e.g. 41/66). | Reference sheet is labeled 100% automatically; a bad sheet is repaired manually in ≤ 1 minute. | 3–4 | Not started |
| **M4** | Production Zhang–Suen → graph → pruning → polylines → RDP + smoothing → stroke order (greedy TSP + 2-opt). | ≥ 80% of fixtures have the correct stroke count and zero closed exported polylines; processing ≤ 2 s/glyph. M0 is a feasibility spike, not acceptance of the production pipeline. | 4–6 | Not started |
| **M5** | Shared baseline, x-height, cap-height, em-box, sidebearings, slant; `glyphset.json` ↔ `GlyphSet`; Room set storage. | Set glyphs align to a common baseline (three-descender test); JSON round-trip is lossless. | 3 | Not started |
| **M6** | `ExportAdapter`, SVG/DXF/PNG/JSON, share and print. | SVG opens in Inkscape as strokes; DXF opens in LibreCAD; no-fill test rejects deliberately corrupted SVG. | 3–4 | Not started |
| **M7** | `TextComposer`, A4 preview, writing animation in stroke order. | «Привет, мир!» composes onto A4 with size/slant/spacing settings; page export contains only strokes. | 3–4 | Not started |
| **M8** | Stroke editor: delete/split/merge/reorder/reverse, recalculate with parameters, finger drawing, undo/redo, source-photo comparison. | A broken fixture glyph is repaired to a usable state in ≤ 60 s, evidenced by video. | 4–5 | Not started |
| **M9** | CLI `gfontInspect` on an authorized real set, resolve format unknowns, independent `GfontCodec`, round-trip test, KDraw validation, instructions, SVG/PNG fallback. | KDraw writes text on paper using our set; «Проверка недостающих слов» reports no missing characters. No direct hardware protocol integration. | 3–5 | Not started |
| **M10** | QA: 3 handwritings × 3 lighting conditions; speed and APK-size measurements; empty/error states; signed APK in GitHub Releases; README screenshots. | QA evidence and release artifacts exist, signing secrets are not committed, known limitations are documented. Release channel requires confirmation. | 3–4 | Not started |

## Separate picture-contour mode — clarified 2026-10-15

See `docs/05-рисунок-по-контуру.md`. The confirmed result is a separate image with black contours derived from a photo. It must not be confused with the centerline strokes used for handwriting alphabets. Core/UI code and synthetic production-function tests are implemented independently of glyph no-fill/open-polyline validation; picture contours may naturally close. This branch has not yet passed Gradle/Android CI or real-photo/device acceptance. The preview uses a white raster backing as an engineering choice, not as a confirmed output-format/background requirement. No file format or extra save/copy/share actions are specified or implemented.

## Gates

- Each milestone requires production code, tests calling that code, successful applicable build and smoke, a `CHANGELOG.md` entry and a six-part report (`AGENTS.md`).
- Before M1 there is no Gradle: use a real standalone Kotlin compilation/test/smoke command and include the full output.
- Human-only device/UI/Inkscape checks cannot be replaced by a claimed automated pass.
- At most 15 English commits per milestone. First commit: project documents. M0 has a separate PR on the fixed Arena branch `arena/7c133358-photo-to-glyph`; never mutate `main`.
- M9 cannot be hardware-accepted on synthetic round trips alone. Real-file scale/service-entry questions and device validation remain explicit unknowns.

## Scope reduction order

Remove, in this order if time is constrained:

1. Tesseract character hints (not a required dependency).
2. DXF.
3. Writing animation.
4. TTF (not a required outline-font implementation).

**Never remove:** the confirmed page-photo → detected letters → alphabet workflow, acceptance of incomplete alphabets, KDraw-owned missing-letter substitution, the separate black-contour image result, the grid sheet, the manual stroke editor, or the prohibition of filled handwriting glyphs.

## Pending decisions

Android minimum: API 26, following the proposal and the user's authorization to continue with M1. Confirm GitHub Releases APK vs Google Play. Grid-free notebook photos are the clarified primary workflow; their segmentation/recognition approach and revised milestone scope still need agreement, not a repeated question about whether the user wants them. `.gfont` units/font size/service entries/companion text file remain unconfirmed until M9. KDraw-owned substitution is user-reported and needs real-program validation in M9.
