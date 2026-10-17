# Changelog

All notable changes are documented here. Code and commits are in English; reports distinguish measured evidence from assumptions.

## [Unreleased]

### User priority — 2026-10-17

- Reaffirm the main deliverable: personal handwriting alphabets exported as separate `.gfont` files. Black-and-white preparation is an internal/supporting stage; do not spend the next implementation slice polishing picture contours instead of the alphabet-to-export workflow.

### M2 — 2026-10-15 (in progress)

- Record the confirmed product outcomes: camera-first photo of a handwritten page → detected letters → a possibly incomplete alphabet; KDraw owns missing-letter substitution. A separate photo-to-image mode returns black contours; file format/background/actions remain unspecified.
- Change the Android entry flow to request camera permission and open a full-screen CameraX preview immediately; retain gallery import as fallback and remove synthetic letter/demo controls and fixture assets from the user app.
- Keep the browser single-glyph lab explicitly developer-only; it is not the Android product screen.
- Continue M2 photo preparation (page corners, crop/rotation, perspective and B/W); page B/W now preserves small components (dots/accents) by default, with a production test.
- Add separate pure Kotlin/JVM Sobel + non-maximum-suppression picture contours, a Compose camera/import preview mode, and a sensitivity control. Four synthetic tests execute `blackContourImage`/`PipelineParams`; full Gradle/Android CI run `38799609923` passes. Real Android/photo quality checks remain pending.
- Re-run `./tools/m2-standalone-check.sh` on 2026-10-16: all 17 page-preparation, 3 InkComponents, 4 picture-contour and 5 preparation API checks pass; the full Gradle/Android build and tests also pass in run `38883213683`.
- On 2026-10-17 the user confirmed on-device camera use and gallery import; screenshots show page preparation output at 601 × 800. They also show that the current quad still covers the whole photograph, leaving tabletop noise in the binary result; corner detection/manual correction acceptance remains open. See `docs/M2-report.md`.
- Automatic handwriting recognition and connected-letter splitting remain unimplemented. Manual set assembly, local storage and `.gfont` export were added in later slices; do not infer OCR from M2 page preparation.

### M3 — 2026-10-16 (foundation only; incomplete)

- Add deterministic 8-connected ink-component detection/cropping and synthetic production-function checks. This only finds candidate regions; it does not assign letter labels, group detached marks, or split cursive words.
- Add a serializable partial `GlyphSet` data model and `kotlinx.serialization` JSON round-trip/invariant tests. This is not yet wired to Android local storage or `.gfont` export.
- Pass full GitHub Actions Gradle/Android build, core/JUnit tests, lint and CLI smoke in run `38799609923` at commit `e58d36d`; publish debug APK artifact `photoglyph-m1-debug-apk` (ID `12003392740`). `./tools/m2-standalone-check.sh` also passes, but intentionally excludes the serialization package; it is not a substitute for Gradle tests.
- M3 recognition remains incomplete: OCR/character assignment, correction UI, app-local storage and export are not implemented.
- Fix the existing Kotlin formatting violation that caused the earlier Gradle/Android build to fail before it could publish the debug APK.

### M5 — 2026-10-16 (local storage and reviewed-glyph normalization foundations; incomplete)

- Add app-private JSON file storage for independent partial `GlyphSet` snapshots, with hashed filenames, atomic replacement and explicit corruption errors; add four JUnit tests that invoke the production store and JSON codec.
- Add a camera-first entry point to a read-only local set list with Russian/English loading, empty and error states. The screen cannot yet create or edit a set.
- Add `normalizeGlyphSet` for manually reviewed raster glyphs: caller-supplied per-glyph baselines and reference metrics are transformed from image Y-down coordinates into a shared model Y-up baseline and scale. This is not OCR or automatic baseline/metric detection.
- Pass full GitHub Actions Gradle/Android verification, core tests, Debug/Release app unit tests, lint, formatting and synthetic CLI smoke in run `38883213683` (commit `2aedf07`); both new normalizer tests pass. The earlier storage-only run `38849746285` also passed all four store tests in both variants.
- This is a partial foundation, not completion of M5: automatic glyph recognition/metric estimation, user flows to create/edit/save recognized glyphs, device QA and `.gfont` export remain outstanding. See `docs/M5-report.md`. The planned Room backend is not wired.

### M9 — 2026-10-17 (documented `.gfont` codec foundation; incomplete)

- Start `.gfont` export early, following the user's reiterated priority of personal handwriting alphabets. Implement pure Kotlin/JVM ZIP record encode/decode for the documented glyph-entry subset with big-endian fields, Unicode entry names, Y-up/Y-down transform, bounds validation, and no guessed service entries or sidecar.
- Add `GfontRoundTripTest` against the production codec, including partial alphabet preservation, exact stroke flags, malformed records, and unsupported supplementary-plane code points.
- Add `.gfont` export from the library for every saved set. Scale/KDraw compatibility remain unverified; the codec does not guess service entries or sidecars.

### M3 — 2026-10-17 (manual page-to-alphabet path; incomplete)

- Add `vectorizeBinary` to pass an already prepared binary crop through component cleanup, Zhang–Suen thinning and graph-to-open-chain extraction without applying a second threshold. Keep small detached marks by default; add synthetic production-function tests for line preservation and detached marks.
- Connect the prepared-page screen to a manual alphabet builder: drag one crop at a time, inspect the turquoise centerline draft, enter one supported BMP character, adjust a per-glyph baseline, mark actual cap/x-height reference glyphs, and save only confirmed glyphs as a local partial set. Saving opens the local library, where each set exports separately as `.gfont`.
- Explicitly do not call this OCR: connected cursive is not split, labels and reference samples are user-selected, the M0 centerline graph is preliminary, pen order is not recovered, and there is no path editor. No personal image was added to Git.
- Pass full GitHub Actions build, `:core:test`, Android lint/format/detekt, app unit tests, and CLI smoke in run `39017154083`. `BinaryVectorizeTest` calls the production `vectorizeBinary` function; full six-part report: `docs/M3-manual-builder-report.md`.
- Add a Russian/Latin character-case picker. Each uppercase and lowercase form is independently selectable and tracked (`А` and `а` are separate keys); 3 app unit tests exercise the production catalog/progress function, while core JSON and `.gfont` production-function checks preserve both forms together. Letters are still manually cropped and confirmed; no OCR is implied.

### M1 — 2026-10-13

- Add the official checksum-pinned Gradle 8.11.1 wrapper and :core/:app/:cli/:tools build structure, ktlint/detekt and Android 8.0+ Compose foundation.
- Bridge the existing real-core M0 suite into JUnit and add a production source-policy function with architectural tests for forbidden Android imports.
- Verify the actual Gradle build/core tests/CLI smoke on GitHub Actions, including an unchanged-source commit (`eaeb99b`); publish complete logs and the generated debug APK. Record the network-blocked local Gradle attempt separately, never as a successful local Android build.
- Add a live browser diagnostic sandbox backed by the actual core, parameter controls, intermediate PNGs, no-fill SVG download, explicit upload consent, in-memory image processing and bounded inputs; no OCR, font storage or fake .gfont output.
- Add eight HTTP/integration/recovery test cases shared by JUnit and a clearly labeled standalone runner, plus seven actual browser checks on our own synthetic inputs.
- Apply mechanical ktlint formatting only to M0 sources; no accepted mathematical behavior or contracts are changed. Keep test provenance and pixel goldens intact.

### Requirements clarification — 2026-10-13

- Record the user's notebook-photo, fonts-only workflow and allow independently saved/exported partial alphabets without fabricating missing glyphs.
- Confirm local phone storage without accounts/cloud sync, and KDraw-owned cross-set substitution; PhotoGlyph must not merge sets or invent fallback metadata.
- Flag the original input/segmentation/page milestones and 31–43 person-day estimate for scope reconciliation before implementation. No M1 or recognition/storage/export functionality is claimed or started.

### M0 — 2026-10-13

- Establish the MIT license, engineering specification, full M0–M10 roadmap (31–43 person-days), README and repository hygiene rules in the first project commit.
- Document the known `.gfont` binary layout and KDraw / Хорус 3 Pro file-installation boundary, with explicit unresolved format and hardware questions.
- Add a pure Kotlin/JVM spike: BT.601 grayscale, clipped-window adaptive threshold, eight-connected despeckling, simultaneous-pass Zhang–Suen thinning, pixel-graph traversal, strict short-branch pruning, edge-preserving cycle splitting, RDP and explicit no-fill SVG export/validation.
- Add five original synthetic 300 × 300 / 6 px PNG fixtures («Ж», «Б», «в», «Ш», «ф»), a reproducible standard-library generator, provenance/hashes and an independently hand-specified 20 × 20 thinning golden.
- Add 21 real-core tests, compiled against the production JAR, and CLI smoke with per-glyph stroke/closed-path/cycle counts and cold/median/max timings. Reject deliberately corrupted filled/styled SVG and external entities.
- Add standalone pre-Gradle verification and an optional checksum-pinned, ignored local compiler/runtime bootstrap. No native processing libraries, copied project code, third-party font sets or product GPL dependencies.
- Record measured SVG/CSV evidence and complete command output in `docs/M0-report.md` and `docs/m0/`. Decision **B**: centerlines are viable on synthetic inputs, but junction cleanup/preprocessing is required; «Ж» fragments into eight chains and diagonals remain jagged. Real handwriting and Inkscape acceptance are not claimed.
- Stop after M0 for user review. M1/Gradle/Android/CI, normalization, editor, `.gfont` and hardware validation have not started.
