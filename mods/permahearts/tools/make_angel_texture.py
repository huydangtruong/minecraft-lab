"""Draws the Warrior Angel textures (P-021, D63): original 32x32 gold and white art in the vanilla Vex model's UV layout.

- textures/entity/warrior_angel.png          (calm face)
- textures/entity/warrior_angel_charging.png (glowing eyes, open mouth: shown while the angel charges at a target)

Only the UV layout of the Vex model is followed (box faces at the same places); no Mojang texture is copied or recoloured.
Layout (u, v, w, h, d): head (0,0,5,5,5), body (0,10,3,4,2), robe (0,16,3,5,2), right arm (23,0,2,4,2),
left arm (23,6,2,4,2), wing (16,14,0,5,8): its two flat faces are at (16,22) and (24,22), each 8x5.
Run: python3 -I mods/permahearts/tools/make_angel_texture.py
"""
import importlib.util
import os

HERE = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location("hearts", os.path.join(HERE, "make_heart_sprites.py"))
hearts = importlib.util.module_from_spec(spec)
spec.loader.exec_module(hearts)
write_png, rgba, CLEAR = hearts.write_png, hearts.rgba, hearts.CLEAR

OUT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "permahearts", "textures", "entity")

PAL = {
    "W": "faf8f0",  # white
    "w": "e6e2d6",  # shaded white
    "v": "cfcabb",  # deep shade
    "g": "f2c540",  # gold
    "G": "c48a14",  # dark gold
    "h": "fff6b8",  # gold highlight
    "s": "fbe4c4",  # skin
    "t": "e8c49c",  # skin shade
    "b": "4a5c94",  # eye
    "r": "fff2a0",  # glowing eye
    "m": "8a3a2a",  # mouth
}


def paint(img, x0, y0, rows):
    for dy, row in enumerate(rows):
        for dx, ch in enumerate(row):
            if ch != ".":
                img[y0 + dy][x0 + dx] = rgba(PAL[ch])


def build(charging):
    img = [[CLEAR] * 32 for _ in range(32)]
    eye = "r" if charging else "b"
    mouth = "m" if charging else "t"
    # head 5x5x5: top (5,0), bottom (10,0), right (0,5), front (5,5), left (10,5), back (15,5)
    paint(img, 5, 0, ["gggGg", "WhWWW", "WWWWw", "WWWww", "wWWww"])  # hair top with a gold band across the front edge
    paint(img, 10, 0, ["ttttt", "ttsst", "tssst", "ttsst", "ttttt"])
    paint(img, 0, 5, ["ggggg", "WWWWw", "WsssW", "WsssW", "wtttw"])
    paint(img, 10, 5, ["ggggg", "wWWWW", "WsssW", "WsssW", "wtttw"])
    paint(img, 5, 5, ["gghgg", "WsssW", "s" + eye + "s" + eye + "s", "ss" + mouth + "ss", "wtttw"])
    paint(img, 15, 5, ["ggggg", "WWWWw", "WWWWw", "WWwWw", "wwwww"])
    # body 3x4x2 at (0,10): top (2,10), bottom (5,10), right (0,12), front (2,12), left (5,12), back (7,12)
    paint(img, 2, 10, ["gGg", "WWW"])
    paint(img, 5, 10, ["www", "www"])
    paint(img, 0, 12, ["Ww", "Ww", "Ww", "Gg"])
    paint(img, 2, 12, ["gWg", "WWW", "WhW", "ggg"])
    paint(img, 5, 12, ["wW", "wW", "wW", "gG"])
    paint(img, 7, 12, ["WWW", "WwW", "WWW", "ggg"])
    # robe 3x5x2 at (0,16): top (2,16), bottom (5,16), right (0,18), front (2,18), left (5,18), back (7,18)
    paint(img, 2, 16, ["ggg", "WWW"])
    paint(img, 5, 16, ["www", "www"])
    paint(img, 0, 18, ["Ww", "Ww", "gG", "Ww", "gG"])
    paint(img, 2, 18, ["WWW", "WhW", "gGg", "WWW", "gGg"])
    paint(img, 5, 18, ["wW", "wW", "Gg", "wW", "Gg"])
    paint(img, 7, 18, ["WWW", "WwW", "gGg", "WWW", "gGg"])
    # arms 2x4x2: right at (23,0), left at (23,6): top, bottom, right, front, left, back
    for y in (0, 6):
        paint(img, 25, y, ["WW", "WW"])
        paint(img, 27, y, ["ss", "ss"])
        paint(img, 23, y + 2, ["Ww", "Ww", "gG", "ss"])
        paint(img, 25, y + 2, ["WW", "WW", "gg", "ss"])
        paint(img, 27, y + 2, ["wW", "wW", "Gg", "ts"])
        paint(img, 29, y + 2, ["WW", "Ww", "gg", "ts"])
    # wing: two flat 8x5 faces at (16,22) and (24,22); the second is the first mirrored so both wings read the same
    wing = [
        "WWhggggG",
        ".WWgggGG",
        "..WWggGG",
        "...WwgGG",
        "....wwGG",
    ]
    paint(img, 16, 22, wing)
    paint(img, 24, 22, [row[::-1] for row in wing])
    return img


def main():
    os.makedirs(OUT, exist_ok=True)
    write_png(os.path.join(OUT, "warrior_angel.png"), build(False))
    write_png(os.path.join(OUT, "warrior_angel_charging.png"), build(True))
    print("wrote the Warrior Angel textures to", os.path.normpath(OUT))


if __name__ == "__main__":
    main()
