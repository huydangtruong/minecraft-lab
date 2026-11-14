"""Draws the stats button and tab icon (D39): a taiji (yin-yang) symbol, the Daoist mark of cultivation.

- stats/icon.png (16x16): the symbol, used on the Stats tab.
- stats/button.png, stats/button_highlighted.png (20x18): a rounded button in the style of the recipe book button,
  with the symbol on it.
Run: python3 -I mods/permahearts/tools/make_stats_sprites.py
"""
import importlib.util
import math
import os

HERE = os.path.dirname(__file__)
spec = importlib.util.spec_from_file_location("hearts", os.path.join(HERE, "make_heart_sprites.py"))
hearts = importlib.util.module_from_spec(spec)
spec.loader.exec_module(hearts)
write_png, rgba, CLEAR = hearts.write_png, hearts.rgba, hearts.CLEAR

OUT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "permahearts", "textures", "gui", "sprites", "stats")

YANG = rgba("f6efdc")   # ivory
YIN = rgba("2b1d4a")    # deep indigo
RIM = rgba("120a24")    # outline
GLOW = rgba("b07cff")   # violet qi on the rim's lower right


def taiji(size, radius):
    """size x size image, taiji of the given radius centred in it."""
    c = size / 2
    img = [[CLEAR] * size for _ in range(size)]
    for y in range(size):
        for x in range(size):
            px, py = x + 0.5, y + 0.5
            d = math.hypot(px - c, py - c)
            if d > radius:
                continue
            if d > radius - 1:
                # rim: violet glow on the lower right, dark elsewhere
                img[y][x] = GLOW if (px - c) + (py - c) > radius * 0.9 else RIM
                continue
            r2 = radius / 2
            top = math.hypot(px - c, py - (c - r2))
            bottom = math.hypot(px - c, py - (c + r2))
            if top <= r2 * 0.36:
                col = YIN       # dot in the yang head
            elif bottom <= r2 * 0.36:
                col = YANG      # dot in the yin head
            elif top <= r2:
                col = YANG
            elif bottom <= r2:
                col = YIN
            else:
                col = YANG if px < c else YIN
            img[y][x] = col
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
    icon = taiji(14, 7)
    for y in range(14):
        for x in range(14):
            if icon[y][x] != CLEAR:
                img[2 + y][3 + x] = icon[y][x]
    return img


def main():
    os.makedirs(OUT, exist_ok=True)
    write_png(os.path.join(OUT, "icon.png"), taiji(16, 7.5))
    write_png(os.path.join(OUT, "button.png"), button(False))
    write_png(os.path.join(OUT, "button_highlighted.png"), button(True))
    print("wrote stats sprites to", os.path.normpath(OUT))


if __name__ == "__main__":
    main()
