"""D217: the trash can icon (16x16) drawn in the trash slot of the inventory screen.
Run: python3 tools/make_trash_sprite.py  -> assets/permahearts/textures/gui/sprites/trash/trash.png"""
import os
import pngkit

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "permahearts",
                   "textures", "gui", "sprites", "trash")
C = {
    'K': (55, 55, 55, 255),  # outline only (D217 fix), the dark grey of vanilla slot shadows
}
# Outline only, centred: two empty rows above and below (it used to sit on the slot's bottom edge).
ROWS = [
    "................",
    "................",
    "......KKKK......",
    "......K..K......",
    "..KKKKKKKKKKKK..",
    "...K........K...",
    "...K..K..K..K...",
    "...K..K..K..K...",
    "...K..K..K..K...",
    "...K..K..K..K...",
    "...K..K..K..K...",
    "...K..K..K..K...",
    "...K........K...",
    "....KKKKKKKK....",
    "................",
    "................",
]

def write_png(path, rows):
    pngkit.write_png(path, [[C[ch] if ch != '.' else pngkit.CLEAR for ch in row] for row in rows])

if __name__ == "__main__":
    assert all(len(r) == 16 for r in ROWS) and len(ROWS) == 16
    os.makedirs(OUT, exist_ok=True)
    write_png(os.path.join(OUT, "trash.png"), ROWS)
