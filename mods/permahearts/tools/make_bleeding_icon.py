"""Draws the Bleeding effect icon (Life Steal Blade, P-020): a dark red blood drop on a transparent 18x18 canvas. Original art.

- textures/mob_effect/bleeding.png
Run: python3 -I mods/permahearts/tools/make_bleeding_icon.py
"""
import importlib.util
import os

HERE = os.path.dirname(__file__)
spec = importlib.util.spec_from_file_location("hearts", os.path.join(HERE, "make_heart_sprites.py"))
hearts = importlib.util.module_from_spec(spec)
spec.loader.exec_module(hearts)
write_png, rgba, CLEAR = hearts.write_png, hearts.rgba, hearts.CLEAR

OUT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "permahearts", "textures", "mob_effect")

COLORS = {"d": rgba("5a0808"), "m": rgba("a81414"), "l": rgba("e03030"), "w": rgba("ff9a9a"), ".": CLEAR}
ROWS = [
    "..................",
    "........dd........",
    ".......dmmd.......",
    ".......dmmd.......",
    "......dmllmd......",
    "......dmllmd......",
    ".....dmmllmmd.....",
    ".....dmlllmmd.....",
    "....dmmlwllmmd....",
    "....dmlwwllmmd....",
    "....dmlwllllmd....",
    "....dmmllllmmd....",
    "....dmmlllmmmd....",
    ".....dmmmmmmd.....",
    "......ddmmdd......",
    ".......dddd.......",
    "..................",
    "..................",
]


def main():
    os.makedirs(OUT, exist_ok=True)
    write_png(os.path.join(OUT, "bleeding.png"), [[COLORS[c] for c in row] for row in ROWS])


if __name__ == "__main__":
    main()
