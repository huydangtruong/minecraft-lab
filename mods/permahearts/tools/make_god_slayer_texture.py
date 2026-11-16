"""Draws the God Slayer textures (P-032, D96): an original red Oni with white hair, black samurai plates and a katana.

- textures/entity/god_slayer.png       64x64, the player skin layout (head, hat = white hair, body, arms, legs) plus the
                                       extra boxes of the model: horns at (0,32) and (8,32), shoulder plates at (16,32)
- textures/entity/god_slayer_eyes.png  64x64, only the glowing yellow eye pixels on the face (drawn bright in the dark)
- textures/item/katana.png             16x16, a diagonal katana for the item model (handheld)

Only the UV layout of the vanilla humanoid model is followed (box faces at the same places); no Mojang texture is
copied or recoloured. Run: python3 -I mods/permahearts/tools/make_god_slayer_texture.py
"""
import importlib.util
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location("hearts", os.path.join(HERE, "make_heart_sprites.py"))
hearts = importlib.util.module_from_spec(spec)
spec.loader.exec_module(hearts)
write_png, rgba, CLEAR = hearts.write_png, hearts.rgba, hearts.CLEAR

ASSETS = os.path.join(HERE, "..", "src", "main", "resources", "assets", "permahearts", "textures")

PAL = {
    "R": "c8372d",  # red skin
    "r": "a82a22",  # skin shade
    "L": "e0534a",  # skin light
    "K": "1b1b23",  # black cloth
    "k": "30303c",  # dark grey cloth highlight
    "G": "d8b04a",  # gold trim
    "D": "3a0f0f",  # feather dark red
    "Q": "7a1414",  # feather red
    "Z": "1d1c21",  # feather tip black
    "W": "f4f1ea",  # white hair
    "w": "d6d1c4",  # hair shade
    "B": "4a1612",  # dark red / eye socket
    "I": "f0e6c8",  # ivory horn tip
    "O": "f0cf6e",  # horn amber
    "o": "b98a34",  # horn shade
    "F": "fffaf0",  # fang
    "i": "cdbf96",  # ivory shade
    "E": "ffe14a",  # glowing eye
    "S": "b9bcc8",  # blade steel
    "s": "8a8e9c",  # blade shade
    "H": "e8ecf4",  # blade edge light
    "P": "6b3b20",  # grip wrap
    "N": "5a6a8a",  # dim glowing blade edge (eyes layer only)
}


def put(img, x, y, ch):
    if ch != ".":
        img[y][x] = rgba(PAL[ch])


def paint(img, x0, y0, rows):
    for dy, row in enumerate(rows):
        for dx, ch in enumerate(row):
            put(img, x0 + dx, y0 + dy, ch)


def solid(img, x0, y0, w, h, ch):
    paint(img, x0, y0, [ch * w] * h)


def box_uv(u, v, w, h, d):
    """Face rectangles (x, y, w, h) of a box with texture offset (u, v): top, bottom, right, front, left, back."""
    return {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h), "back": (u + d + w + d, v + d, w, h),
    }


TEX = 128   # sheet width
TEXH = 320  # sheet height

# name: (w, h, d, kind). The same sizes are used by GodSlayerModel.java; the packed texture offsets are printed with --uv.
BOXES = {
    "head": (16, 14, 15, "head"),
    "brow": (16, 3, 3, "skin"),
    "fang": (2, 2, 6, "fang"),
    "hair_top": (17, 4, 17, "hair"),
    "hair_slice": (32, 4, 2, "hair"),
    "bone_inner": (5, 34, 5, "gold_flat"),
    "bone_outer": (5, 33, 5, "gold_flat"),
    "joint_elbow": (6, 6, 6, "gold_flat"),
    "joint_tip": (5, 5, 5, "gold_flat"),
    "feather_dark": (4, 40, 2, "fill_dark"),
    "feather_red": (4, 40, 2, "fill_red"),
    "feather_tip": (5, 3, 2, "fill_black"),
    "spike": (3, 8, 3, "hair"),
    "horn1": (7, 8, 7, "horn_base"),
    "horn2": (6, 9, 6, "horn"),
    "horn3": (5, 9, 5, "horn"),
    "horn4": (3, 8, 3, "horn"),
    "collar": (20, 5, 18, "armor"),
    "chest": (28, 18, 15, "chest"),
    "waist": (18, 10, 11, "armor"),
    "belt": (20, 4, 13, "rope"),
    "knot": (5, 6, 3, "gold"),
    "tasset_front": (20, 10, 2, "armor"),
    "tasset_side": (2, 10, 11, "armor"),
    "arm": (9, 14, 9, "sleeve"),
    "forearm": (8, 14, 8, "forearm"),
    "fist": (7, 6, 7, "skin"),
    "handle": (3, 12, 3, "grip"),
    "guard": (8, 2, 5, "gold"),
    "blade": (4, 28, 2, "steel"),
    "thigh": (11, 12, 11, "hakama"),
    "shin": (9, 11, 9, "hakama"),
    "foot": (9, 3, 12, "skin"),
}


def pack():
    """Shelf-pack the boxes into the 128x256 sheet (largest first); returns {name: (u, v)}."""
    order = sorted(BOXES, key=lambda n: -(BOXES[n][1] + BOXES[n][2]))
    uv, x, y, row = {}, 0, 0, 0
    for n in order:
        w, h, d, _ = BOXES[n]
        bw, bh = 2 * (w + d), h + d
        if x + bw > TEX:
            x, y, row = 0, y + row, 0
        assert y + bh <= TEXH, "texture sheet is full"
        uv[n] = (x, y)
        x += bw
        row = max(row, bh)
    return uv


UV = pack()


def noise(x, y, k=0):
    return ((x * 73856093) ^ (y * 19349663) ^ (k * 83492791)) % 11


def face_char(kind, face, x, y, w, h):
    """The colour letter of one texel of a box face (x, y inside a w*h rectangle)."""
    n = noise(x, y, len(face))
    if kind == "skin":
        if face == "bottom" or y == h - 1 and face not in ("top",):
            return "r"
        return "r" if n == 0 else ("L" if n == 1 and face == "top" else "R")
    if kind == "head":
        return "r" if n < 2 or face == "bottom" else "R"
    if kind == "hair":
        if x % 4 == 0 and n < 8:
            return "w"
        return "w" if n == 0 else "W"
    if kind == "horn_base":
        return "B" if y % 4 < 2 else "O"
    if kind == "horn":
        if y % 4 == 3:
            return "o"
        return "o" if (face in ("right", "back") and x % 2 == 0) else "O"
    if kind == "fang":
        return "F" if face != "bottom" and x % 2 == 0 or y == 0 else "i"
    if kind.startswith("fill_"):
        return {"fill_dark": "D", "fill_red": "Q", "fill_black": "Z"}[kind]
    if kind == "wing":
        # a dark red membrane fading to black at the tip; the outer long edge is a gold bone
        if x == 0 and face in ("front", "back"):
            return "G"
        if face in ("front", "back"):
            return "K" if (y > h * 0.7 and x % 3 == 1) or y % 9 == 8 else ("B" if y % 2 == 0 and x > 1 else "r")
        return "B"
    if kind == "armor":
        if face == "top" or face == "bottom":
            return "K"
        if y % 4 == 3:
            return "G" if x % 2 == 0 else "B"
        return "k" if x % 4 == 0 else "K"
    if kind == "rope":
        return "G" if (x + y) % 3 else "P"
    if kind == "gold_flat":
        # plain gold for the wing bones; only the outer texels of each face get the slightly darker shade
        return "o" if x in (0, w - 1) or y in (0, h - 1) else "G"
    if kind == "gold":
        return "G" if n > 1 else "P"
    if kind == "sleeve":
        if face == "bottom":
            return "G"
        return "B" if y % 5 == 4 else ("k" if x % 4 == 0 else "K")
    if kind == "forearm":
        if face in ("top", "bottom"):
            return "R"
        if y >= h - 8:
            return "G" if y == h - 8 else ("K" if x % 4 else "k")  # black bracer
        return "R" if n > 0 else "r"
    if kind == "grip":
        return "P" if (x + y) % 3 else "K"
    if kind == "steel":
        # one straight blade, the sharp edge on the model -x side (front column 0, back last column, the "right" face)
        if face == "front":
            if x == 0:
                return "H"
            return "S" if x < w - 1 else "s"
        if face == "back":
            return "H" if x == w - 1 else ("s" if x == 0 else "S")
        return "H"
    if kind == "hakama":
        if face == "top":
            return "K"
        return "k" if x % 4 == 0 else ("B" if y == h - 1 else "K")
    if kind == "chest":
        return "K"
    return "K"


def paint_box(img, name):
    w, h, d, kind = BOXES[name]
    u, v = UV[name]
    if kind.startswith("fill_"):
        solid(img, u, v, 2 * (w + d), h + d, face_char(kind, "front", 0, 0, 1, 1))
        return
    for face, (x0, y0, fw, fh) in box_uv(u, v, w, h, d).items():
        for dy in range(fh):
            for dx in range(fw):
                put(img, x0 + dx, y0 + dy, face_char(kind, face, dx, dy, fw, fh))
    if kind == "steel":
        slant_blade(img, name)


def slant_blade(img, name):
    """Cuts the blade tip (the far end, the bottom rows of the side faces) on a slant with transparent pixels (the mob
    texture is alpha-tested): the edge side is shorter than the spine side. The end face keeps only the spine column."""
    w, h, d, _ = BOXES[name]
    u, v = UV[name]
    f = box_uv(u, v, w, h, d)
    cut = lambda i: max(0, 3 - i)  # i = distance from the edge side: 3, 2, 1, 0 rows
    x, y, fw, fh = f["front"]
    for col in range(fw):
        for r in range(cut(col)):
            img[y + fh - 1 - r][x + col] = CLEAR
    x, y, fw, fh = f["back"]
    for col in range(fw):
        for r in range(cut(fw - 1 - col)):
            img[y + fh - 1 - r][x + col] = CLEAR
    x, y, fw, fh = f["right"]  # the edge side face
    for col in range(fw):
        for r in range(3):
            img[y + fh - 1 - r][x + col] = CLEAR
    x, y, fw, fh = f["bottom"]  # the end face: only the spine column is still closed
    for col in range(fw - 1):
        for r in range(fh):
            img[y + r][x + col] = CLEAR


HEAD_FRONT = [
    "RRRRRRRRRRRRRRRR",
    "RRRRRRRRRRRRRRRR",
    "RRRRRRRRRRRRRRRR",
    "RKKKRRRRRRRRKKKR",
    "RRKKKKRRRRKKKKRR",
    "RRRRKKKKKKKKRRRR",
    "RRBBRRRrrRRRBBRR",
    "RRRBBBRrrRBBBRRR",
    "RRRRRBBRrrBBRRRR",
    "RRRRRRrBBrRRRRRR",
    "RRRRRRRRRRRRRRRR",
    "RRRRKKKKKKKKRRRR",
    "RRRKWBBBBBBWKRRR",
    "RRRRRrrRRrrRRRRR",
]


def paint_head(img):
    paint_box(img, "head")
    u, v = UV["head"]
    f = box_uv(u, v, 16, 14, 15)["front"]
    paint(img, f[0], f[1], HEAD_FRONT)
    # ears on the sides
    for face in ("right", "left"):
        x, y, _, _ = box_uv(u, v, 16, 14, 15)[face]
        paint(img, x + 5, y + 4, ["rr", "rr", "rr"])


def paint_chest(img):
    paint_box(img, "chest")
    u, v = UV["chest"]
    f = box_uv(u, v, 28, 18, 15)
    # open black kimono: bare red chest in a V, gold-laced plate across the belly, gold crest on the back
    rows = []
    for y in range(18):
        row = ""
        for x in range(28):
            half = abs(x - 13.5)
            ch = "K" if x % 7 else "k"
            if y < 9 and half < 6 - y * 0.6:
                ch = "R" if (x + y) % 5 else "r"
            elif y >= 9 and y % 4 == 1:
                ch = "G"
            elif y >= 9 and x % 5 == 0:
                ch = "B"
            row += ch
        rows.append(row)
    paint(img, f["front"][0], f["front"][1], rows)
    back = []
    for y in range(18):
        row = ""
        for x in range(28):
            ch = "K" if x % 7 else "k"
            if 4 <= y <= 12 and abs(x - 13.5) + abs(y - 8) < 5.5 and (abs(x - 13.5) + abs(y - 8)) % 3 > 0:
                ch = "G"
            row += ch
        back.append(row)
    paint(img, f["back"][0], f["back"][1], back)


def build_skin():
    img = [[CLEAR] * TEX for _ in range(TEXH)]
    for name in BOXES:
        if name == "head":
            paint_head(img)
        elif name == "chest":
            paint_chest(img)
        else:
            paint_box(img, name)
    return img


def build_eyes():
    img = [[CLEAR] * TEX for _ in range(TEXH)]
    u, v = UV["head"]
    fx, fy, _, _ = box_uv(u, v, 16, 14, 15)["front"]
    # the two slanted eyes of HEAD_FRONT (the dark K rows 2-5 are the brows; the eyes sit in the B sockets)
    left = ((2, 6), (3, 6), (3, 7), (4, 7), (5, 7), (5, 8), (6, 8))
    for dx, dy in left:
        put(img, fx + dx, fy + dy, "E")
        put(img, fx + 15 - dx, fy + dy, "E")
    # a faint glowing edge on the sharp side of each blade (front first column, back last column)
    bu, bv = UV["blade"]
    bf = box_uv(bu, bv, 4, 28, 2)
    x, y, w, h = bf["front"]
    for dy in range(0, h - 3):
        put(img, x, y + dy, "N")
    x, y, w, h = bf["back"]
    for dy in range(0, h - 3):
        put(img, x + w - 1, y + dy, "N")
    return img


def build_katana():
    img = [[CLEAR] * 16 for _ in range(16)]
    # a diagonal blade from the guard at the bottom left to the tip at the top right (handheld holds the lower left)
    for i in range(11):
        x, y = 4 + i, 11 - i
        put(img, x, y, "S")
        put(img, x + 1, y, "s")
        if i < 10:
            put(img, x, y - 1, "H")
    put(img, 15, 0, "H")
    # guard (tsuba) and grip (tsuka)
    for x, y in ((3, 12), (4, 12), (3, 11), (5, 13), (4, 13), (2, 12), (3, 13)):
        put(img, x, y, "G")
    for x, y in ((2, 14), (1, 15), (3, 15), (2, 15)):
        put(img, x, y, "P")
    put(img, 0, 15, "K")
    return img


def main():
    if "--uv" in sys.argv:
        for n, (u, v) in sorted(UV.items()):
            print("%s %d %d" % (n, u, v))
        return
    ent = os.path.join(ASSETS, "entity")
    item = os.path.join(ASSETS, "item")
    os.makedirs(ent, exist_ok=True)
    os.makedirs(item, exist_ok=True)
    write_png(os.path.join(ent, "god_slayer.png"), build_skin())
    write_png(os.path.join(ent, "god_slayer_eyes.png"), build_eyes())
    write_png(os.path.join(item, "katana.png"), build_katana())
    print("wrote the God Slayer textures to", os.path.normpath(ASSETS))


if __name__ == "__main__":
    main()
