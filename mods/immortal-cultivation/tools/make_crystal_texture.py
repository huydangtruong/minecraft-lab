"""Writes the translucent turquoise crystal block texture (D143) and the Crystal Disarm effect icon. Stdlib only, original art."""
import os
import random

import pngkit

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "permahearts", "textures")


def write_png(path, w, h, pixels):
    """pixels is a flat list of w*h RGBA tuples."""
    pngkit.write_png(path, [pixels[y * w:(y + 1) * w] for y in range(h)])


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
