"""Draws the cultivation panel button (D40): a stone tablet in warm brown tones with red-orange spirit wings,
on a rounded button in the style of the recipe book button. Original art (the Stats tab shows the Nether Star item).

- stats/button.png, stats/button_highlighted.png (20x18)
Run: python3 -I mods/permahearts/tools/make_stats_sprites.py
"""
import importlib.util
import os

HERE = os.path.dirname(__file__)
spec = importlib.util.spec_from_file_location("hearts", os.path.join(HERE, "make_heart_sprites.py"))
hearts = importlib.util.module_from_spec(spec)
spec.loader.exec_module(hearts)
write_png, rgba, CLEAR = hearts.write_png, hearts.rgba, hearts.CLEAR

OUT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "permahearts", "textures", "gui", "sprites", "stats")

SLAB = {"o": "211515", "k": "352323", "d": "49312b", "m": "584737", "l": "7a5d4c", "L": "947662"}
EMBLEM = {"d": "8a1a0a", "m": "d43a10", "b": "ff6a1a", "h": "ffb04a"}
WINGS = [  # 8x8, placed at (4, 4) on the tablet
    "m......m",
    "mb....bm",
    ".mb..bm.",
    ".mbhhbm.",
    "..bhhb..",
    "...bb...",
    "...mm...",
    "...dd...",
]


TOP, BOTTOM = 2, 13  # tablet rows in the 16x16 icon: 12 tall, so it clears the button's bottom edge (D40, Huy)
LEFT, RIGHT = 2, 13


def tablet_icon():
    """16x16: a stone tablet with chipped corners, light top-left edge, dark bottom-right, and the wings on it."""
    img = [[CLEAR] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            if not (LEFT <= x <= RIGHT and TOP <= y <= BOTTOM) or (x in (LEFT, RIGHT) and y in (TOP, BOTTOM)):
                continue
            edge = x in (LEFT, RIGHT) or y in (TOP, BOTTOM)
            if edge:
                c = SLAB["o"]
            elif y == TOP + 1 and x == LEFT + 1:
                c = SLAB["L"]
            elif y == TOP + 1 or x == LEFT + 1:
                c = SLAB["l"]
            elif y == BOTTOM - 1 or x == RIGHT - 1:
                c = SLAB["k"]
            else:
                c = SLAB["d"] if (x * 7 + y * 3) % 5 < 2 else SLAB["m"]
            img[y][x] = rgba(c)
    for y, row in enumerate(WINGS):
        for x, ch in enumerate(row):
            if ch != ".":
                img[TOP + 2 + y][4 + x] = rgba(EMBLEM[ch])
    return img


def button(highlighted):
    """20x18 rounded frame like the vanilla recipe book button."""
    w, h = 20, 18
    edge = rgba("00073e") if highlighted else rgba("000000")
    light = rgba("ffffff")
    face = rgba("8892c9") if highlighted else rgba("c6c6c6")
    dark = rgba("343e75") if highlighted else rgba("555555")
    img = [[CLEAR] * w for _ in range(h)]
    for x in range(2, 18):
        img[0][x] = edge
        img[17][x] = edge
    img[1][1] = edge
    img[1][18] = dark
    img[16][1] = edge
    img[16][18] = dark
    for x in range(2, 18):
        img[1][x] = light
        img[16][x] = dark
    for y in range(2, 16):
        img[y][0] = edge
        img[y][1] = light
        img[y][18] = dark
        img[y][19] = edge
        for x in range(2, 18):
            img[y][x] = face
    icon = tablet_icon()
    for y in range(16):
        for x in range(16):
            if icon[y][x] != CLEAR and 1 <= y + 1 <= 16 and 1 <= x + 2 <= 18:
                img[y + 1][x + 2] = icon[y][x]
    return img


def main():
    os.makedirs(OUT, exist_ok=True)
    write_png(os.path.join(OUT, "button.png"), button(False))
    write_png(os.path.join(OUT, "button_highlighted.png"), button(True))
    print("wrote stats sprites to", os.path.normpath(OUT))


if __name__ == "__main__":
    main()
