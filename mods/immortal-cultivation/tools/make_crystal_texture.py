"""Writes the translucent turquoise crystal block texture (D143) and the Crystal Disarm effect icon. Stdlib only, original art."""
import os
import random
import struct
import zlib

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "permahearts", "textures")


def write_png(path, w, h, pixels):
    raw = b"".join(b"\x00" + bytes([c for px in pixels[y * w:(y + 1) * w] for c in px]) for y in range(h))

    def chunk(tag, data):
        body = tag + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0)) + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(png)


def block():
    rnd = random.Random(143)
    px = []
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                px.append((120, 235, 225, 215))
            else:
                shade = rnd.randint(-14, 14)
                glint = 40 if (x + y) % 8 in (0, 1) else 0
                px.append((70 + shade + glint, 214 + shade // 2 + glint // 2, 208 + shade // 2 + glint // 2, 120 + glint))
    write_png(os.path.join(ROOT, "block", "crystal_prison.png"), 16, 16, px)


def effect():
    px = []
    for y in range(18):
        for x in range(18):
            d = abs(x - 8.5) + abs(y - 8.5)
            if d < 8:
                px.append((88, 230, 216, 255) if d > 5 else (216, 255, 250, 255))
            else:
                px.append((0, 0, 0, 0))
    write_png(os.path.join(ROOT, "mob_effect", "crystal_disarm.png"), 18, 18, px)


block()
effect()
