"""Draws the Bottle of Enlightenment item sprite (P-030, D89): a 16x16 green-gold glowing bottle with a dark outline.

- textures/item/enlightenment_bottle.png
Run: python3 -I mods/immortal-cultivation/tools/make_enlightenment_bottle_sprite.py
"""
import importlib.util
import os

HERE = os.path.dirname(__file__)
spec = importlib.util.spec_from_file_location("hearts", os.path.join(HERE, "make_heart_sprites.py"))
hearts = importlib.util.module_from_spec(spec)
spec.loader.exec_module(hearts)
write_png, rgba, CLEAR = hearts.write_png, hearts.rgba, hearts.CLEAR

OUT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "permahearts", "textures", "item")

# c cork, g glass, w glass shine, l liquid (green), m liquid light (gold-green), s sparkle, d liquid shadow
BOTTLE = [
    "................",
    "......ccc.......",
    "......ccc.......",
    ".....wggg.......",
    "......ggg..s....",
    "......ggg.......",
    ".....gggggg.....",
    "....gmmlllgg....",
    "...gmmmllllgg...",
    "...gmmslllllg...",
    "...gmmlllllld...",
    "...glllllllld...",
    "...gllllllldd...",
    "....gddddddd....",
    ".....gggggg.....",
    "................",
]
PAL = {"c": "b07a3a", "g": "bfe8d0", "w": "ffffff", "l": "3fd45a", "m": "d8f07a", "s": "fff6a0", "d": "1f8a3a"}
EDGE = "0c3a22"


def main():
    img = [[CLEAR] * 16 for _ in range(16)]
    for y, row in enumerate(BOTTLE):
        for x, ch in enumerate(row):
            if ch != ".":
                img[y][x] = rgba(PAL[ch])
    out = [row[:] for row in img]
    for y in range(16):
        for x in range(16):
            if img[y][x] != CLEAR:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < 16 and 0 <= ny < 16 and img[ny][nx] != CLEAR:
                    out[y][x] = rgba(EDGE)
                    break
    write_png(os.path.join(OUT, "enlightenment_bottle.png"), out)
    print("wrote", os.path.normpath(os.path.join(OUT, "enlightenment_bottle.png")))


if __name__ == "__main__":
    main()
