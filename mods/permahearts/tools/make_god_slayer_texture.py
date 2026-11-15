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
    "W": "f4f1ea",  # white hair
    "w": "d6d1c4",  # hair shade
    "B": "4a1612",  # dark red / eye socket
    "I": "f0e6c8",  # ivory horn tip
    "i": "cdbf96",  # ivory shade
    "E": "ffe14a",  # glowing eye
    "S": "b9bcc8",  # blade steel
    "s": "8a8e9c",  # blade shade
    "H": "e8ecf4",  # blade edge light
    "P": "6b3b20",  # grip wrap
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


def build_skin():
    img = [[CLEAR] * 64 for _ in range(64)]

    # ---- head 8x8x8 at (0,0) ----
    f = box_uv(0, 0, 8, 8, 8)
    solid(img, *f["top"], "R")
    solid(img, *f["bottom"], "r")
    solid(img, *f["back"], "R")
    for side in ("right", "left"):
        solid(img, *f[side], "R")
        x, y, _, _ = f[side]
        paint(img, x + 3, y + 3, ["rr", "rr"])  # ear
    paint(img, f["front"][0], f["front"][1], [
        "RRRRRRRR",
        "RKRRRRKR",
        "RRKRRKRR",
        "RBBRRBBR",
        "RRBRRBRR",
        "RRRrrRRR",
        "RKWKKWKR",
        "RRrrrrRR",
    ])

    # ---- hat = the white hair, 8x8x8 at (32,0), fully outside the face ----
    h = box_uv(32, 0, 8, 8, 8)
    solid(img, *h["top"], "W")
    paint(img, h["top"][0], h["top"][1], ["WWwWWWwW", "WWWWWwWW", "wWWWWWWW", "WWWwWWWW", "WWWWWWwW", "WwWWWWWW", "WWWWwWWW", "WWWWWWWW"])
    paint(img, h["front"][0], h["front"][1], ["WWWWWWWW", "WWwWWwWW", "W.W..W.W", "........", "........", "........", "........", "........"])
    paint(img, h["right"][0], h["right"][1], ["WWWWWWWW", "WWWWWWWW", "WwWWWww.", "WWwWw...", "WWWWw...", "WwWWw...", "WWWw....", "WwWW...."])
    paint(img, h["left"][0], h["left"][1], ["WWWWWWWW", "WWWWWWWW", ".wwWWwWW", "...wWwWW", "...wWWWW", "...wWWwW", "....wWWW", "....WWwW"])
    paint(img, h["back"][0], h["back"][1], ["WWWWwWWW", "WWwWWWWW", "WWWWWwWW", "wWWWWWWW", "WWWwWWWW", "WWWWWWwW", "WwWWWWWW", "WWWWwWWW"])

    # ---- body 8x12x4 at (16,16): black kimono, open chest, belt, gold crest on the back ----
    b = box_uv(16, 16, 8, 12, 4)
    solid(img, *b["top"], "K")
    solid(img, *b["bottom"], "K")
    paint(img, b["front"][0], b["front"][1], [
        "KKRRRRKK",
        "KKrRRrKK",
        "KKKRRKKK",
        "KKKrrKKK",
        "KKKKKKKK",
        "BBBBBBBB",
        "BBBGGBBB",
        "KkKKKKkK",
        "KkKKKKkK",
        "KkKKKKkK",
        "KkKKKKkK",
        "KkKKKKkK",
    ])
    for side in ("right", "left"):
        paint(img, b[side][0], b[side][1], ["KKKK"] * 5 + ["BBBB"] * 2 + ["KkKK"] * 5)
    paint(img, b["back"][0], b["back"][1], [
        "KKKKKKKK", "KKKKKKKK", "KKKGGKKK", "KKGKKGKK", "KKKGGKKK", "BBBBBBBB",
        "BBBBBBBB", "KkKKKKkK", "KkKKKKkK", "KkKKKKkK", "KkKKKKkK", "KkKKKKkK",
    ])

    # ---- arms 4x12x4: right at (40,16), left at (32,48): black sleeve, red forearm, black bracer ----
    for u, v in ((40, 16), (32, 48)):
        a = box_uv(u, v, 4, 12, 4)
        solid(img, *a["top"], "K")
        solid(img, *a["bottom"], "R")
        arm = ["KKKK"] * 5 + ["GGGG"] + ["RRRR"] + ["KKKK"] * 3 + ["RRRR", "rRRr"]
        for face in ("right", "front", "left", "back"):
            paint(img, a[face][0], a[face][1], arm)

    # ---- legs 4x12x4: right at (0,16), left at (16,48): black hakama, bare red feet ----
    for u, v in ((0, 16), (16, 48)):
        lg = box_uv(u, v, 4, 12, 4)
        solid(img, *lg["top"], "K")
        solid(img, *lg["bottom"], "r")
        pant = ["KkKK"] * 9 + ["RRRR", "RRRR", "rRRr"]
        for face in ("right", "front", "left", "back"):
            paint(img, lg[face][0], lg[face][1], pant)

    # ---- horn base 2x2x2 at (0,32), horn tip 1x3x1 at (8,32) ----
    hb = box_uv(0, 32, 2, 2, 2)
    for face, (x, y, w, hh) in hb.items():
        solid(img, x, y, w, hh, "B" if face in ("top", "bottom", "back") else "r")
    ht = box_uv(8, 32, 1, 3, 1)
    for face, (x, y, w, hh) in ht.items():
        solid(img, x, y, w, hh, "i" if face in ("right", "back") else "I")

    # ---- shoulder plate 6x2x6 at (16,32): black lamellae with a gold lace ----
    p = box_uv(16, 32, 6, 2, 6)
    paint(img, p["top"][0], p["top"][1], ["GKKKKG", "KkkkkK", "KKKKKK", "KkkkkK", "KKKKKK", "GKKKKG"])
    solid(img, *p["bottom"], "K")
    for face in ("right", "front", "left", "back"):
        paint(img, p[face][0], p[face][1], ["KKKKKK", "GKGKGK"])
    return img


def build_eyes():
    img = [[CLEAR] * 64 for _ in range(64)]
    # head front face is at (8,8); the eyes sit at columns 1-2 and 5-6 of rows 3-4, slanting down to the nose
    for x, y in ((9, 11), (10, 11), (10, 12), (13, 11), (14, 11), (13, 12)):
        put(img, x, y, "E")
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
