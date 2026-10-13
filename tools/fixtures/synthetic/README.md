# Original synthetic M0 fixtures

These files were authored for PhotoGlyph under the repository's MIT license.
They are **not photographs, real handwriting, rendered font glyphs, downloaded
letter images, or third-party `.gfont` sets**.

## Reproduce / verify

```sh
python3 tools/generate_m0_fixtures.py
python3 tools/generate_m0_fixtures.py --check
```

Only Python's standard library is needed. `--check` regenerates the bytes in
memory and compares every PNG and the manifest without overwriting files.

## `m0/`

Five grayscale PNGs: `1046.png` («Ж»), `1041.png` («Б»), `1074.png` («в»),
`1064.png` («Ш»), `1092.png` («ф»). 300 × 300, white background, 6 px geometric
centerline stroke width, round caps and 4 × 4 supersampling. Paths are defined
explicitly in `tools/generate_m0_fixtures.py`. `manifest.json` records the origin,
parameters, authored path counts and SHA-256 hashes.

Authored path count is **not** an acceptance oracle for a pixel-graph edge
partition: a junction may require several chains, and the original direction
and pen motion cannot be uniquely recovered. M0 reports the difference rather
than calling its output a reconstruction of the handwriting order.

## `golden/`

`bar-20-input.png`: 20 × 20, solid black rectangle x=4..15, y=8..10.

`bar-20-expected.png`: 20 × 20, black line x=5..13 at y=9.

The expected nine-pixel skeleton was specified **by hand**, using Zhang–Suen's
two simultaneous subiterations. The generator only encodes those coordinates;
it does not execute or contain a copy of a thinning algorithm. The Kotlin
`ZhangSuenTest.golden20px` invokes production `zhangSuen` and compares all 400
pixels with this expected PNG. The implementation's output is never used to
regenerate the expected fixture.

## Outputs are not goldens

Intermediate rasters and generated SVGs from the real pipeline go to ignored
`.m0-work/`. Small SVG evidence and the measured CSV are copied to `docs/m0/`
for review. They are **evidence**, not independently specified test answers.
