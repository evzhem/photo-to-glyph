# PhotoGlyph roadmap

Source of truth: `AGENTS.md` and `docs/`. Work strictly in order, **one milestone per cycle**: implement → report → wait for the user's “да”. Do not start M1 before the M0 report and approval. If the user says “авто”, continue in order but still report every milestone.

Estimates are person-days, not promises of elapsed time. Total: **31–43 person-days**.

| Milestone | Scope | Acceptance criteria | Person-days | Status |
| --- | --- | --- | ---: | --- |
| **M0** | Photo → B/W → Zhang–Suen → polylines → SVG spike on «Ж», «Б», «в», «Ш», «ф». If photos are absent, use original synthetic 300 × 300 PNGs, 6 px strokes on white. `docs/M0-report.md`, per-letter stroke counts, closed-polyline counts and timings; decision A/B/C. | SVG opens in Inkscape as strokes, not outlines. Zero closed exported polylines. Report honestly lists failures and synthetic-vs-real limitations. A: skeleton viable; B: preprocessing needed; C: change to PNG-only export, which requires a product decision. | 1–2 | In progress: specification |
| **M1** | Gradle `:core` / `:app` / `:cli` / tools structure, ktlint/detekt, no-`android.*` source test, `:cli:smoke`, GitHub Actions. | Green CI on a no-functional-change commit: `./gradlew build :core:test :cli:smoke`. | 1–2 | Not started |
| **M2** | Printable A4 grid sheet with fiducial marks and character labels; CameraX capture; gallery import; four-corner auto-detection with manual repair; perspective rectification; B/W sliders. | A sheet photographed at about 30° under side lighting becomes a straight binary sheet; cell borders coincide with projection-profile valleys. | 3–4 | Not started |
| **M3** | Fiducial-based cell detection with fallback; glyph crops; prefill character by cell; manual correction; merge/split; coverage indicator (e.g. 41/66). | Reference sheet is labeled 100% automatically; a bad sheet is repaired manually in ≤ 1 minute. | 3–4 | Not started |
| **M4** | Production Zhang–Suen → graph → pruning → polylines → RDP + smoothing → stroke order (greedy TSP + 2-opt). | ≥ 80% of fixtures have the correct stroke count and zero closed exported polylines; processing ≤ 2 s/glyph. M0 is a feasibility spike, not acceptance of the production pipeline. | 4–6 | Not started |
| **M5** | Shared baseline, x-height, cap-height, em-box, sidebearings, slant; `glyphset.json` ↔ `GlyphSet`; Room set storage. | Set glyphs align to a common baseline (three-descender test); JSON round-trip is lossless. | 3 | Not started |
| **M6** | `ExportAdapter`, SVG/DXF/PNG/JSON, share and print. | SVG opens in Inkscape as strokes; DXF opens in LibreCAD; no-fill test rejects deliberately corrupted SVG. | 3–4 | Not started |
| **M7** | `TextComposer`, A4 preview, writing animation in stroke order. | «Привет, мир!» composes onto A4 with size/slant/spacing settings; page export contains only strokes. | 3–4 | Not started |
| **M8** | Stroke editor: delete/split/merge/reorder/reverse, recalculate with parameters, finger drawing, undo/redo, source-photo comparison. | A broken fixture glyph is repaired to a usable state in ≤ 60 s, evidenced by video. | 4–5 | Not started |
| **M9** | CLI `gfontInspect` on an authorized real set, resolve format unknowns, independent `GfontCodec`, round-trip test, KDraw validation, instructions, SVG/PNG fallback. | KDraw writes text on paper using our set; «Проверка недостающих слов» reports no missing characters. No direct hardware protocol integration. | 3–5 | Not started |
| **M10** | QA: 3 handwritings × 3 lighting conditions; speed and APK-size measurements; empty/error states; signed APK in GitHub Releases; README screenshots. | QA evidence and release artifacts exist, signing secrets are not committed, known limitations are documented. Release channel requires confirmation. | 3–4 | Not started |

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
5. Photo-without-grid mode.

**Never remove:** the grid sheet, manual stroke editor or prohibition of filled glyphs.

## Pending decisions

Proposed Android minimum: API 26; user confirmation required. Confirm GitHub Releases APK vs Google Play and whether grid-free photos are required in MVP. `.gfont` units/font size/service entries/companion text file remain unconfirmed until M9.
