"""Draws the heart bar layer sprites (D36, D37): 9x9 PNGs for layers 1-14 (layer 0 is the vanilla red heart).

Layers 1-4: bright colours (hearts 21-100), 5-9: pastels (101-200), 10-14: textures (201-300): cherry blossom,
galaxy, green fire, soul fire, crimson lava on top (D37, D38).
Run: python3 -I mods/permahearts/tools/make_heart_sprites.py   (writes into src/main/resources/...)
"""
import os
import struct
import zlib

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "permahearts",
                   "textures", "gui", "sprites", "hud", "heart")

# M = main colour, S = shadow, H = highlight. Fits the vanilla heart container.
SHAPE = [
    ".........",
    "..MM.MM..",
    ".MHMMMMM.",
    ".MMMMMMM.",
    ".SMMMMMS.",
    "..SMMMS..",
    "...SMS...",
    "....S....",
    ".........",
]

# layer: (name, main, shadow, highlight)
PLAIN = {
    1: ("orange", "ff8a1c", "c25e00", "ffdcae"),      # 21-40
    2: ("pink", "ff4fc4", "c42391", "ffcdef"),        # 41-60
    3: ("purple", "a64dff", "7426c4", "e5ccff"),      # 61-80
    4: ("blue", "3b6bff", "2340b8", "cbd8ff"),        # 81-100
    5: ("coral", "ff7a6b", "c4473a", "ffd6d0"),       # 101-120
    6: ("peach", "ffb06b", "c4783a", "ffe6cc"),       # 121-140
    7: ("baby pink", "ffa3dd", "c96aa6", "ffe3f5"),   # 141-160
    8: ("lavender", "c9a3ff", "9469d1", "f0e6ff"),    # 161-180
    9: ("mint", "5ee8b5", "2fa67a", "d6fff0"),        # 181-200
}

# layer: (name, rows, palette); each row covers the same cells as SHAPE.
TEXTURED = {
    10: ("cherry blossom", [  # 201-220
        ".........",
        "..lp.pl..",
        ".pwpppdp.",
        ".wywpwpd.",
        ".dwpwywd.",
        "..dpdwd..",
        "...ddd...",
        "....d....",
        ".........",
    ], {"p": "ffb7d5", "l": "ffe0ee", "d": "e86aa0", "y": "ffe27a", "w": "ffffff"}),
    11: ("galaxy", [  # 221-240
        ".........",
        "..vn.nw..",
        ".vmvnnnv.",
        ".nmmwnvn.",
        ".nvmmncn.",
        "..wnvmn..",
        "...nvn...",
        "....n....",
        ".........",
    ], {"n": "1a1240", "v": "4b2a8a", "m": "b04ad6", "w": "ffffff", "c": "7fd8ff"}),
    12: ("green fire", [  # 241-260 (D38: was red fire at 261-280)
        ".........",
        "..tk.kt..",
        ".krkkkrk.",
        ".rorkror.",
        ".oyoroyo.",
        "..oyyyo..",
        "...oyo...",
        "....o....",
        ".........",
    ], {"k": "0a1f0a", "t": "14400f", "r": "1f7a1f", "o": "3fe03f", "y": "c8ff6b"}),
    13: ("soul fire", [  # 261-280
        ".........",
        "..tk.kt..",
        ".kakkkak.",
        ".acakaca.",
        ".cwcacwc.",
        "..cwwwc..",
        "...cwc...",
        "....c....",
        ".........",
    ], {"k": "1b2a35", "t": "0d4a5a", "a": "1aa6c4", "c": "3ff2ff", "w": "e6ffff"}),
    14: ("crimson lava drop", [  # 281-300, the top tier (D38: Huy's pick "Giot nham": cooled crust on top, glowing below)
        ".........",
        "..Km.mK..",
        ".KmKKmKK.",
        ".mRmRMRm.",
        ".RrRrRrR.",
        "..rOrOr..",
        "...OYO...",
        "....Y....",
        ".........",
    ], {"K": "1f0208", "m": "5c0418", "M": "8a0a2a", "R": "c4103a", "r": "e8264a", "O": "ff5a3a", "Y": "ffb36b"}),
}

CLEAR = (0, 0, 0, 0)


def rgba(h):
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def write_png(path, px):
    h, w = len(px), len(px[0])
    raw = b"".join(b"\x00" + bytes(v for c in row for v in c) for row in px)

    def chunk(t, c):
        return struct.pack(">I", len(c)) + t + c + struct.pack(">I", zlib.crc32(t + c) & 0xFFFFFFFF)

    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
                + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


def save(layer, full):
    for row_in, row_shape in zip(full, SHAPE):
        assert len(row_in) == 9
    half = [[p if x <= 4 else CLEAR for x, p in enumerate(row)] for row in full]
    write_png(os.path.join(OUT, f"layer_{layer}_full.png"), full)
    write_png(os.path.join(OUT, f"layer_{layer}_half.png"), half)


def main():
    os.makedirs(OUT, exist_ok=True)
    for layer, (_, m, s, h) in PLAIN.items():
        col = {"M": rgba(m), "S": rgba(s), "H": rgba(h)}
        save(layer, [[col.get(c, CLEAR) for c in row] for row in SHAPE])
    for layer, (_, rows, pal) in TEXTURED.items():
        full = []
        for y, row in enumerate(rows):
            line = []
            for x, c in enumerate(row):
                inside = SHAPE[y][x] != "."
                assert inside == (c != "."), f"layer {layer} row {y} col {x} does not match the heart shape"
                line.append(rgba(pal[c]) if inside else CLEAR)
            full.append(line)
        save(layer, full)
    print("wrote layers 1-14 to", os.path.normpath(OUT))


if __name__ == "__main__":
    main()
