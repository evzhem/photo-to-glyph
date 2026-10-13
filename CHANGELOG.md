# Changelog

All notable changes are documented here. Code and commits are in English; reports distinguish measured evidence from assumptions.

## [Unreleased]

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
