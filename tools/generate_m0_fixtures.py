#!/usr/bin/env python3
"""Generate our own MIT-licensed M0 rasters using only Python's standard library.

These are deliberately simple geometric approximations, NOT real handwriting,
font rendering, downloaded photographs, or outputs of the thinning algorithm.
Golden skeleton pixels are specified by hand independently of production code.
"""

import argparse
import hashlib
import json
import math
from pathlib import Path
import struct
import zlib

ROOT = Path(__file__).resolve().parent.parent
FIXTURES = ROOT / "tools" / "fixtures" / "synthetic"
CANVAS_PX = 300
STROKE_WIDTH_PX = 6
SUPERSAMPLING = 4
WHITE = 255
BLACK = 0
PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"


def ellipse(cx, cy, rx, ry, segments=64):
    return [
        (cx + rx * math.cos(2 * math.pi * i / segments),
         cy + ry * math.sin(2 * math.pi * i / segments))
        for i in range(segments + 1)
    ]


GLYPHS = {
    "Ж": [
        [(60, 50), (150, 150), (240, 250)],
        [(240, 50), (150, 150), (60, 250)],
        [(150, 40), (150, 260)],
    ],
    "Б": [
        [(222, 52), (74, 52), (74, 242)],
        [(74, 142), (143, 140), (190, 154), (219, 181),
         (219, 213), (193, 239), (148, 244), (74, 242)],
    ],
    "в": [
        [(94, 232), (94, 122), (104, 74), (137, 54), (165, 63),
         (170, 86), (159, 106), (96, 150), (157, 137), (189, 154),
         (198, 181), (183, 213), (143, 229), (94, 232)],
    ],
    "Ш": [
        [(62, 58), (62, 238), (150, 238), (150, 58)],
        [(150, 238), (238, 238), (238, 58)],
    ],
    "ф": [ellipse(150, 150, 82, 75), [(150, 35), (150, 274)]],
}


def distance_squared(x, y, start, end):
    ax, ay = start
    bx, by = end
    dx, dy = bx - ax, by - ay
    denominator = dx * dx + dy * dy
    t = min(1.0, max(0.0, ((x - ax) * dx + (y - ay) * dy) / denominator)) if denominator else 0.0
    return (x - ax - t * dx) ** 2 + (y - ay - t * dy) ** 2


def rasterize(paths):
    # Analytical distance to hand-authored segments; 4x4 antialiasing, round caps.
    radius = STROKE_WIDTH_PX / 2
    segments = []
    for path in paths:
        for start, end in zip(path, path[1:]):
            bounds = (min(start[0], end[0]) - radius, min(start[1], end[1]) - radius,
                      max(start[0], end[0]) + radius, max(start[1], end[1]) + radius)
            segments.append((start, end, bounds))
    pixels = bytearray([WHITE]) * (CANVAS_PX * CANVAS_PX)
    for y in range(CANVAS_PX):
        for x in range(CANVAS_PX):
            nearby = [(a, b) for a, b, (left, top, right, bottom) in segments
                      if x + 1 >= left and x <= right and y + 1 >= top and y <= bottom]
            if not nearby:
                continue
            covered = 0
            for sy in range(SUPERSAMPLING):
                for sx in range(SUPERSAMPLING):
                    px = x + (sx + 0.5) / SUPERSAMPLING
                    py = y + (sy + 0.5) / SUPERSAMPLING
                    if any(distance_squared(px, py, a, b) <= radius * radius for a, b in nearby):
                        covered += 1
            pixels[y * CANVAS_PX + x] = round(WHITE * (1 - covered / SUPERSAMPLING ** 2))
    return pixels


def chunk(kind, data):
    return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xffffffff)


def png(width, height, pixels):
    rows = b"".join(b"\0" + bytes(pixels[y * width:(y + 1) * width]) for y in range(height))
    header = struct.pack(">IIBBBBB", width, height, 8, 0, 0, 0, 0)
    return PNG_SIGNATURE + chunk(b"IHDR", header) + chunk(b"IDAT", zlib.compress(rows, 9)) + chunk(b"IEND", b"")


def golden_bar():
    width = height = 20
    source = bytearray([WHITE]) * (width * height)
    expected = bytearray(source)
    # A solid 12x3 rectangle. By the two simultaneous Zhang-Suen passes,
    # its fixed-point skeleton is x=5..13 at y=9 (nine foreground pixels).
    # This expected answer is hand-specified, never computed by production code.
    for y in range(8, 11):
        for x in range(4, 16):
            source[y * width + x] = BLACK
    for x in range(5, 14):
        expected[9 * width + x] = BLACK
    return png(width, height, source), png(width, height, expected)


def generated_files():
    outputs = {}
    manifest = {
        "provenance": "Original geometric synthetic fixtures, not real handwriting or font rendering",
        "license": "MIT",
        "widthPx": CANVAS_PX,
        "heightPx": CANVAS_PX,
        "strokeWidthPx": STROKE_WIDTH_PX,
        "supersampling": SUPERSAMPLING,
        "background": "white",
        "glyphs": [],
    }
    for char, paths in GLYPHS.items():
        name = f"{ord(char)}.png"
        data = png(CANVAS_PX, CANVAS_PX, rasterize(paths))
        outputs[FIXTURES / "m0" / name] = data
        manifest["glyphs"].append({"char": char, "codePoint": ord(char), "file": name,
                                   "authoredPathCount": len(paths), "sha256": hashlib.sha256(data).hexdigest()})
    outputs[FIXTURES / "m0" / "manifest.json"] = (json.dumps(manifest, ensure_ascii=False, indent=2) + "\n").encode()
    source, expected = golden_bar()
    outputs[FIXTURES / "golden" / "bar-20-input.png"] = source
    outputs[FIXTURES / "golden" / "bar-20-expected.png"] = expected
    return outputs


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="Verify deterministic fixtures without writing")
    args = parser.parse_args()
    for path, data in generated_files().items():
        relative = path.relative_to(ROOT)
        if args.check:
            if not path.exists() or path.read_bytes() != data:
                raise SystemExit(f"FAIL fixture differs or is missing: {relative}")
            print(f"PASS fixture: {relative}")
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(data)
            print(f"Generated: {relative}")


if __name__ == "__main__":
    main()
