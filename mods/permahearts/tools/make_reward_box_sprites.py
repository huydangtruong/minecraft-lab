"""Draws the reward box item sprites (P-027, D76): a 16x16 chest in silver, gold and diamond-cyan, with a dark outline.

- textures/item/silver_reward_box.png, gold_reward_box.png, diamond_reward_box.png
Run: python3 -I mods/permahearts/tools/make_reward_box_sprites.py
"""
import importlib.util
import os

HERE = os.path.dirname(__file__)
spec = importlib.util.spec_from_file_location("hearts", os.path.join(HERE, "make_heart_sprites.py"))
hearts = importlib.util.module_from_spec(spec)
spec.loader.exec_module(hearts)
write_png, rgba, CLEAR = hearts.write_png, hearts.rgba, hearts.CLEAR

OUT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "permahearts", "textures", "item")

# b body, h highlight, d shadow, k band, l lock
CHEST = [
    "................",
    "................",
    "..hhhhhhhhhhhh..",
    "..hbbbbkkbbbbd..",
    "..hbbbbkkbbbbd..",
    "..hbbbbkkbbbbd..",
    "..bbbbbkkbbbbd..",
    "..dddddllddddd..",
    "..bbbbbllbbbbd..",
    "..hbbbbkkbbbbd..",
    "..bbbbbkkbbbbd..",
    "..bbbbbkkbbbbd..",
    "..bbbbbkkbbbbd..",
    "..dddddddddddd..",
    "................",
    "................",
]

TIERS = {
    # name: (palette, outline)
    "silver": ({"b": "b9c2cc", "h": "eef2f6", "d": "7d8896", "k": "56627a", "l": "ffd84a"}, "262c38"),
    "gold": ({"b": "f0b030", "h": "ffe680", "d": "b07a10", "k": "8a4a10", "l": "fff2b0"}, "4a2a06"),
    "diamond": ({"b": "3fd8d0", "h": "c8fffa", "d": "1a8f9a", "k": "1d5a8a", "l": "ffffff"}, "0a2e46"),
}


def build(pal, edge):
    img = [[CLEAR] * 16 for _ in range(16)]
    for y, row in enumerate(CHEST):
        for x, ch in enumerate(row):
            if ch != ".":
                img[y][x] = rgba(pal[ch])
    out = [row[:] for row in img]
    for y in range(16):
        for x in range(16):
            if img[y][x] != CLEAR:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < 16 and 0 <= ny < 16 and img[ny][nx] != CLEAR:
                    out[y][x] = rgba(edge)
                    break
    return out


def main():
    for name, (pal, edge) in TIERS.items():
        write_png(os.path.join(OUT, name + "_reward_box.png"), build(pal, edge))
    print("wrote reward box sprites to", os.path.normpath(OUT))


if __name__ == "__main__":
    main()
