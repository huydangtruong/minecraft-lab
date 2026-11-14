"""Draws the Frostbite effect icon (Frost Orb technique, P-011 part C): an ice-blue six-armed snowflake on a transparent
18x18 canvas. Original art.

- textures/mob_effect/frostbite.png
Run: python3 -I mods/permahearts/tools/make_frostbite_icon.py
"""
import importlib.util
import os

HERE = os.path.dirname(__file__)
spec = importlib.util.spec_from_file_location("hearts", os.path.join(HERE, "make_heart_sprites.py"))
hearts = importlib.util.module_from_spec(spec)
spec.loader.exec_module(hearts)
write_png, rgba, CLEAR = hearts.write_png, hearts.rgba, hearts.CLEAR

OUT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "permahearts", "textures", "mob_effect")

DARK, MID, LIGHT, WHITE = rgba("2f6fa8"), rgba("5fb4e6"), rgba("a8e0ff"), rgba("f4fcff")

# 18x18: 'd' dark outline-ish, 'm' mid, 'l' light, 'w' white core.
ROWS = [
    "........dd........",
    ".......dmmd.......",
    "..dd...dlld...dd..",
    "..dmd..dlld..dmd..",
    "...dmd.dlld.dmd...",
    "....dmddlldmmd....",
    ".....ddmllmdd.....",
    "dd....dmllmd....dd",
    "dmmmllmlwwlmllmmmd",
    "dmmmllmlwwlmllmmmd",
    "dd....dmllmd....dd",
    ".....ddmllmdd.....",
    "....dmddlldmmd....",
    "...dmd.dlld.dmd...",
    "..dmd..dlld..dmd..",
    "..dd...dlld...dd..",
    ".......dmmd.......",
    "........dd........",
]
COLORS = {"d": DARK, "m": MID, "l": LIGHT, "w": WHITE, ".": CLEAR}


def main():
    os.makedirs(OUT, exist_ok=True)
    px = [[COLORS[c] for c in row] for row in ROWS]
    write_png(os.path.join(OUT, "frostbite.png"), px)


if __name__ == "__main__":
    main()
