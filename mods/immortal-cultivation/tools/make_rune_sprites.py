"""Rune item and Rune Forge block textures (D241-D242). Run: python3 tools/make_rune_sprites.py"""
import os
import random
from pngkit import write_png, rgba, CLEAR

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "permahearts", "textures")

O = rgba("1a1418")   # outline
D = rgba("3b3540")   # dark stone
M = rgba("554d5c")   # mid stone
L = rgba("736a7a")   # light edge
G = rgba("ff9a2e")   # glyph
H = rgba("ffd27a")   # glyph glint

RUNE = [
    "                ",
    "      OOOO      ",
    "     OLLMMO     ",
    "    OLLMMMDO    ",
    "   OLLMGGMMDO   ",
    "   OLMMGMMMDO   ",
    "  OLLMMGGGMDDO  ",
    "  OLMMMGMHMDDO  ",
    "  OLMMMGMGMDDO  ",
    "  OLMMGGGMMDDO  ",
    "   OLMMGMMDDO   ",
    "   OLMMGMMDDO   ",
    "    OMMMMDDO    ",
    "     OMMDDO     ",
    "      OOOO      ",
    "                ",
]
KEY = {" ": CLEAR, "O": O, "D": D, "M": M, "L": L, "G": G, "H": H}


def grid(rows):
    return [[KEY[c] for c in r] for r in rows]


def stone(seed, base=("2f2a33", "3b3540", "463f4c")):
    rnd = random.Random(seed)
    cols = [rgba(c) for c in base]
    return [[cols[rnd.randrange(3)] for _ in range(16)] for _ in range(16)]


def frame(px, edge=rgba("1a1418")):
    for i in range(16):
        px[0][i] = px[15][i] = px[i][0] = px[i][15] = edge
    return px


def top():
    px = frame(stone(1))
    iron = rgba("b8b8c0")
    for y in range(3, 13):
        for x in range(3, 13):
            px[y][x] = iron if (x in (3, 12) or y in (3, 12)) else rgba("2a2228")
    # a glowing glyph on the anvil plate
    for x, y in [(7, 5), (8, 5), (7, 6), (6, 7), (7, 7), (8, 7), (9, 7), (7, 8), (7, 9), (8, 9), (8, 10), (7, 10)]:
        px[y][x] = G
    px[7][9] = H
    return px


def side(front):
    px = frame(stone(2 if front else 3))
    plank = [rgba("6b4a2b"), rgba("5a3d22")]
    for y in range(11, 15):
        for x in range(1, 15):
            px[y][x] = plank[(x // 4 + y) % 2]
    for x in range(1, 15):
        px[10][x] = rgba("b8b8c0")
    if front:
        for y in range(3, 9):
            for x in range(5, 11):
                px[y][x] = rgba("140c0c")
        for x, y in [(6, 7), (7, 6), (8, 7), (9, 6), (7, 8), (8, 8)]:
            px[y][x] = G
        px[6][8] = H
    return px


def bottom():
    plank = [rgba("6b4a2b"), rgba("5a3d22")]
    return frame([[plank[(x // 4 + y) % 2] for x in range(16)] for y in range(16)])


write_png(os.path.join(ROOT, "item", "rune.png"), grid(RUNE))
write_png(os.path.join(ROOT, "block", "rune_forge_top.png"), top())
write_png(os.path.join(ROOT, "block", "rune_forge_side.png"), side(False))
write_png(os.path.join(ROOT, "block", "rune_forge_front.png"), side(True))
write_png(os.path.join(ROOT, "block", "rune_forge_bottom.png"), bottom())
