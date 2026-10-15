#!/usr/bin/env python3
"""Original analytic 3D-camera fixture: A4 grid, 30-degree yaw, side illumination.
No Kotlin/production transform is imported. Test expectations use the specified
camera geometry and hand-defined grid coordinates, not output of the core.
"""
import argparse
import hashlib
import json
import math
from pathlib import Path
import struct
import zlib

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / 'tools/fixtures/synthetic/m2'
WIDTH, HEIGHT = 420, 594
PHOTO_W, PHOTO_H = 720, 840
YAW = math.radians(30)
ROLL = math.radians(6)
FOCAL, DISTANCE = 1050.0, 900.0
GRID_X = [24 + index * 62 for index in range(7)]
GRID_Y = [56 + index * 46 for index in range(12)]


def png(width, height, pixels):
    def chunk(name, body):
        return struct.pack('>I', len(body)) + name + body + struct.pack('>I', zlib.crc32(name + body))
    raw = b''.join(b'\x00' + bytes(pixels[y * width:(y + 1) * width]) for y in range(height))
    return b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', width, height, 8, 0, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b'')


def ink(u, v):
    if 24 <= u <= 396 and 56 <= v <= 562:
        if any(abs(u - x) < 1 for x in GRID_X) or any(abs(v - y) < 1 for y in GRID_Y):
            return True
        # Original simple pen marks within cells, not font glyphs or OCR labels.
        x, y = (u - 24) % 62, (v - 56) % 46
        if 17 < x < 43 and 17 < y < 32 and abs(y - (x / 2 + 8)) < 1.5:
            return True
    return any(abs(u - x) < 3 and abs(v - y) < 3 for x, y in [(24, 24), (396, 24), (396, 570), (24, 570)])


def project(u, v):
    x, y = u - WIDTH / 2, v - HEIGHT / 2
    depth = DISTANCE - math.sin(YAW) * x
    camera_x = math.cos(YAW) * x * math.cos(ROLL) - y * math.sin(ROLL)
    camera_y = math.cos(YAW) * x * math.sin(ROLL) + y * math.cos(ROLL)
    return [PHOTO_W / 2 + FOCAL * camera_x / depth, PHOTO_H / 2 + FOCAL * camera_y / depth]


def make():
    reference = [28 if ink(x + 0.5, y + 0.5) else 245 for y in range(HEIGHT) for x in range(WIDTH)]
    photo = []
    for y in range(PHOTO_H):
        for x in range(PHOTO_W):
            a = (x + 0.5 - PHOTO_W / 2) / FOCAL
            b = (y + 0.5 - PHOTO_H / 2) / FOCAL
            ray_x = a * math.cos(ROLL) + b * math.sin(ROLL)
            ray_y = -a * math.sin(ROLL) + b * math.cos(ROLL)
            plane_x = DISTANCE * ray_x / (math.cos(YAW) + math.sin(YAW) * ray_x)
            depth = DISTANCE - plane_x * math.sin(YAW)
            u, v = plane_x + WIDTH / 2, depth * ray_y + HEIGHT / 2
            if 0 <= u < WIDTH and 0 <= v < HEIGHT:
                lighting = 0.72 + 0.28 * u / WIDTH
                value = round((28 if ink(u, v) else 245) * lighting)
            else:
                value = 64 + ((x * 13 + y * 7) % 11) - 5
            photo.append(value)
    files = {'grid-reference.png': png(WIDTH, HEIGHT, reference), 'grid-yaw30-side-light.png': png(PHOTO_W, PHOTO_H, photo)}
    manifest = {'provenance': 'Original Python analytic camera-ray/plane fixture, no production code used', 'synthetic': True, 'yawDegrees': 30, 'rollDegrees': 6, 'referenceWidth': WIDTH, 'referenceHeight': HEIGHT, 'photoWidth': PHOTO_W, 'photoHeight': PHOTO_H, 'quad': [project(0, 0), project(WIDTH, 0), project(WIDTH, HEIGHT), project(0, HEIGHT)], 'gridColumns': GRID_X, 'gridRows': GRID_Y, 'sha256': {name: hashlib.sha256(data).hexdigest() for name, data in files.items()}}
    files['manifest.json'] = (json.dumps(manifest, ensure_ascii=False, indent=2) + '\n').encode()
    return files


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()
    for name, data in make().items():
        path = OUT / name
        if args.check:
            if not path.is_file() or path.read_bytes() != data:
                raise SystemExit(f'FAIL original M2 fixture: {path.relative_to(ROOT)}')
            print(f'PASS fixture: {path.relative_to(ROOT)}')
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(data)
            print(f'WRITE original synthetic fixture: {path.relative_to(ROOT)} ({len(data)} bytes)')


if __name__ == '__main__':
    main()
