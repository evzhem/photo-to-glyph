# PhotoGlyph — engineering specification

This document, `ROADMAP.md` and `docs/` are the source of truth. If a task contradicts them, ask before changing code. Code, identifiers and commit messages are in English. The primary UI language is Russian.

## Product and non-negotiable rules

PhotoGlyph is an Android application that converts photographs of handwritten letters into **centerline strokes**, lets the user repair those strokes, and exports a handwriting set for KDraw / Kvenjoy pen plotters (the customer's device is Хорус 3 Pro). Outline tracing is not an acceptable substitute: it makes a pen draw the outline twice.

Supported repertoire: Cyrillic А–Я / а–я including Ё/ё, Latin letters, digits and basic punctuation. The grid sheet, manual stroke editor and no-fill invariant cannot be cut from the MVP.

1. Tests must execute production functions from `core/src/main/kotlin`, not copies of their logic. Every report names the actual functions executed by each test. Delete fake or placeholder tests.
2. Before claiming completion, run `./gradlew build :core:test :cli:smoke` and include its complete output. **Before M1**, when Gradle does not exist, run the real standalone M0 compilation/test/smoke command and include its complete output instead. Never fabricate Gradle results.
3. All image mathematics and vectorization are pure Kotlin/JVM. No OpenCV, NDK, Rust/JNI or other native processing libraries. A native implementation requires a measured failure of the 2 s/glyph budget and explicit user approval.
4. Every exported glyph is made of strokes, never fills. SVG must explicitly have `fill="none"` and a stroke. Test this invariant, including rejection of deliberately corrupted output.
5. Project license: MIT. Do not add GPL project dependencies (including the prohibited potrace/OpenCV integrations), copy code from other projects, or commit third-party `.gfont` sets. OpenHand (`github.com/ReNothingg/OpenHand`) has an **OpenHand Proprietary License. All Rights Reserved** license: it may be studied, but its code must not be copied. Implement `.gfont` independently from `docs/04-формат-gfont.md` and prove round-trip behavior with our own fixtures.
6. Never connect directly to the plotter via Bluetooth/COM or reverse-engineer its device protocol. Export a file for the user to put in KDraw's `gcodefonts` folder. KDraw controls the hardware.
7. Do not invent missing information. Mark assumptions in code and reports and ask the user when it becomes relevant. In particular, `.gfont` units, size and service entries are not confirmed.
8. Do not rewrite accepted milestones without a separate refactoring task.
9. Android UI uses Jetpack Compose, not XML layouts. All UI text comes from `strings.xml`, with both `ru` and `en` resources.
10. Resolve material ambiguity before coding. Synthetic M0 inputs are explicitly authorized and do not require a request for user photographs.
11. Do not commit user photos, third-party font sets, signing keys (`.jks`/keystores), credentials or secret files. Generated datasets/build outputs stay ignored; only small, original synthetic fixtures belong in Git.

## Architecture

| Area | Responsibility |
| --- | --- |
| `:core` | Pure Kotlin/JVM: `geom`, `img`, `thin`, `graph`, `norm`, `model`, `compose`, `export`, `gfont`, `plot`. No `android.*` imports, enforced by a source-scanning test in M1. No image/UI/platform I/O in production math. |
| `:app` | Android: Compose, CameraX, Room; platform image decoding and persistence. |
| `:cli` | JVM pipeline runner; `smoke`, `gfontInspect`, `testFiles`. Desktop image I/O stays here, not in core. |
| `tools/` | Original synthetic test fixtures and A4 grid-sheet generator. |

Data flow:

`Bitmap → BinaryGrid → SkeletonPixels → List<Polyline> → Glyph → GlyphSet → Export`

Every computational stage is a pure function, without mutations of caller-owned input or side effects between stages, and is independently golden-testable. Image decoding/encoding, timings and filesystem operations belong to adapters. The M0 spike may establish Kotlin source directories and a standalone compiler script; Gradle/module wiring, Android and CI belong to M1, not M0.

### Contracts (do not change without approval)

- `Vec2(x, y)`
- `Polyline(points)`
- `Stroke(path, order, widthMm)`
- `Glyph(char, strokes, advance, baselineY, sourceBitmapPng)`
- `GlyphSet(id, name, unitsPerEm=1000, baselineY, xHeight, capHeight, defaultStrokeWidthMm, glyphs: Map<String, Glyph>, createdAt)`

Glyph/model coordinates have **Y up**, origin at the bottom-left of the em-box. Raster/image-stage coordinates naturally have Y down and must be documented as such; normalization establishes model coordinates. Export adapters perform explicit coordinate transforms. Do not prematurely invent units for the plotter format.

`GlyphSet ↔ glyphset.json` uses `kotlinx.serialization` and requires a lossless round-trip test. The optional source image is PNG. Contract types not yet needed by a milestone are not implemented speculatively.

### Parameters

All tunable constants live in `PipelineParams`. Initial defaults:

| Parameter | Default |
| --- | ---: |
| Adaptive threshold window | 25 × 25 px |
| Adaptive threshold offset | 10 grayscale levels |
| Despeckling | Remove connected components with area < 12 px |
| Ramer–Douglas–Peucker epsilon | 0.6 px |
| Short-branch pruning length | < 2 px |
| Pruning passes | 2 |
| Processing budget | ≤ 2 s per glyph |
| Synthetic M0 raster | 300 × 300, white background, stroke width 6 px |

Additional tunable parameters need named defaults and documented meanings; no magic tuning numbers scattered through algorithms. Mathematical constants (BT.601 coefficients, 8-neighbour offsets, Zhang–Suen predicates) are not experimental tuning parameters and must be named or explained. Smoothing, normalization and travel optimization are M4/M5 work, not implied to exist in M0.

## Algorithms — implement independently

1. Black/white: BT.601 grayscale → adaptive threshold → remove small connected components.
2. Alignment: detect four sheet corners → perspective transform → projection-profile deskew.
3. Skeleton: Zhang–Suen thinning to a 1 px skeleton.
4. Skeleton to strokes: 8-connected pixel graph → prune branches shorter than 2 px → traverse edges → polylines → select start/end direction → RDP (ε = 0.6 px) → smoothing.
5. Normalization: shared set baseline, x-height, cap-height, em-box, sidebearings and slant.
6. Stroke order: nearest-neighbour greedy TSP + 2-opt to minimize pen-up travel.
7. Text composition: text → strokes on an A4 page with margins, kerning, line spacing, scale and slant.

**Do not hide loops.** A letter such as «ф» contains a legitimate loop in its centerline. If the spike splits a graph cycle into open strokes to satisfy the open-polyline acceptance criterion, preserve its edges and document this representation and its extra pen lifts. Report closed exported polylines separately from intrinsic graph cycles. No double outline, silent deletion of a loop, or merely hiding an SVG `Z` command counts as centerline extraction.

## Required tests as production code appears

- `ZhangSuenTest`: a 20 × 20 PNG input and independently specified expected skeleton, pixel-for-pixel.
- `SkeletonToPolylinesTest`: «Ж» gives at least three strokes; none is closed. Cover branches, cycles and edge preservation as the implementation grows.
- `BaselineTest`: three glyphs with different descenders have one common baseline.
- `SvgNoFillTest`: only explicit no-fill strokes; corrupted export is rejected.
- `GlyphSetRoundTripTest`: model ↔ JSON without loss.
- `GfontRoundTripTest`: point tolerance 1e-4, identical stroke flags.

Do not add tests for missing production functions just to make this list look complete. Baseline/serialization/gfont tests belong to later milestones. Source import scanning is mandatory in M1.

## Definition of Done

For the current milestone: real production code + real tests + a successful compilation/build + successful applicable smoke run + an entry in `CHANGELOG.md` + acceptance evidence. Human-only checks remain explicitly unverified until the human performs them. Never label a human check as automated.

The milestone report follows exactly:

1. Files changed and their purpose.
2. Complete output of build and test commands.
3. For each test, production core functions executed (or explicitly state none).
4. Acceptance table: `проверено автоматически` / `требует проверки человеком` / `не сделано`.
5. Assumptions and unknowns.
6. The `CHANGELOG.md` entry.

One milestone at a time, strictly M0 → M10. Stop and report after each; proceed only after the user's “да”. “авто” authorizes continued sequential milestones, but each still needs its report. If one pass is insufficient, stop at a compiling, green boundary and report the missing work honestly.

Commits: English, e.g. `M3: add baseline estimation with projection profile`, at most 15 per milestone. The first project commit creates these specification documents. Each milestone should have its own PR; do not rewrite `main`.

**Arena branch constraint:** this session is permanently attached to `arena/7c133358-photo-to-glyph`. Work, commits, pushes and the M0 PR must use that branch only. Never create/switch/rename branches or push to another branch. This platform constraint prevents the otherwise requested new branch per milestone; disclose it in the report. Future milestone branch/PR sequencing needs to respect this constraint rather than pretending another branch was created.

## Questions to resolve with the user at the relevant milestone

- `.gfont`: units, font size, service entries, and whether `.gfont.txt` is mandatory (M9).
- Minimum Android version; proposed API 26 (before Android implementation).
- Distribution: GitHub Releases APK or Google Play (before release work).
- Is photo-without-grid required for MVP, or is the reliable grid-only workflow sufficient? (before planning that optional feature).

Do not block the authorized M0 synthetic spike on these future decisions.
