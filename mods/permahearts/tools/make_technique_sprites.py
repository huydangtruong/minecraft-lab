"""Draws the technique icons (P-011 part A): 16x16 original pixel art, one per technique, plus a lock, a tick and a cross.

- technique/{fire,ice,healing,phase,repulse,sacrifice}.png, technique/lock.png, technique/check.png, technique/cross.png
Each icon is drawn as a body; a dark outline is added automatically around it.
Run: python3 -I mods/permahearts/tools/make_technique_sprites.py
"""
import importlib.util
import math
import os

HERE = os.path.dirname(__file__)
spec = importlib.util.spec_from_file_location("hearts", os.path.join(HERE, "make_heart_sprites.py"))
hearts = importlib.util.module_from_spec(spec)
spec.loader.exec_module(hearts)
write_png, rgba, CLEAR = hearts.write_png, hearts.rgba, hearts.CLEAR

OUT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "permahearts", "textures", "gui", "sprites", "technique")


def blank():
    return [[CLEAR] * 16 for _ in range(16)]


def from_ascii(rows, palette):
    assert len(rows) == 16, len(rows)
    img = blank()
    for y, row in enumerate(rows):
        assert len(row) == 16, (y, row)
        for x, ch in enumerate(row):
            if ch != ".":
                img[y][x] = rgba(palette[ch])
    return img


def outline(img, color):
    """Dark outline on every clear pixel that touches the body (4 neighbours)."""
    out = [row[:] for row in img]
    for y in range(16):
        for x in range(16):
            if img[y][x] != CLEAR:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < 16 and 0 <= ny < 16 and img[ny][nx] != CLEAR:
                    out[y][x] = rgba(color)
                    break
    return out


FIRE = [
    "................",
    "......r.........",
    ".....rr.........",
    ".....rrr........",
    "....rrrr...r....",
    "....rryrr..rr...",
    "...rrryyr.rrr...",
    "...rryyyrrrrr...",
    "..rrryyyyrrrr...",
    "..rryywyyyrrr...",
    "..rryywwyyyrr...",
    "..rryywwwyyrr...",
    "..rryywhwyyrr...",
    "...rryywwyrr....",
    "....rrryyrr.....",
    ".....rrrrr......",
]
FIRE_PAL = {"r": "d4380f", "y": "ff8a1a", "w": "ffd84a", "h": "fff3b0"}

ICE = [  # a six-armed crystal drawn from rows
    "................",
    ".......ww.......",
    "......wccw......",
    ".w.....cc.....w.",
    ".wcw...cc...wcw.",
    "..wcc..cc..ccw..",
    "...wcc.cc.ccw...",
    ".wwwccbccbccwww.",
    ".wcccbbwwbbcccw.",
    "...wcc.cc.ccw...",
    "..wcc..cc..ccw..",
    ".wcw...cc...wcw.",
    ".w.....cc.....w.",
    "......wccw......",
    ".......ww.......",
    "................",
]
ICE_PAL = {"w": "eaffff", "c": "8fdcff", "b": "4aa8e8"}

HEALING = [
    "................",
    "................",
    "...hhh....hhh...",
    "..hlllh..hlllh..",
    ".hlllllhhlllllh.",
    ".hllhllllllgglh.",
    ".hlhllllllllglh.",
    ".hllllllllllglh.",
    "..hllllllllgdh..",
    "...hlllllllgh...",
    "....hlllllgdh...",
    ".....hllllgh....",
    "......hlgdh.....",
    ".......hdh......",
    "........h.......",
    "................",
]
HEALING_PAL = {"h": "e8ffe0", "l": "4ad65a", "g": "2aa43a", "d": "1c7a2a"}


def procedural(color_at):
    img = blank()
    for y in range(16):
        for x in range(16):
            c = color_at(x - 7.5, y - 7.5)
            if c:
                img[y][x] = rgba(c)
    return img


def phase_color(dx, dy):
    d = math.hypot(dx, dy)
    ang = math.degrees(math.atan2(dy, dx)) % 360
    if d <= 1.9:
        return "f4e0ff"
    if 3.2 <= d <= 4.4:
        return "b45cff" if 30 < ang < 330 else None
    if 5.6 <= d <= 7.2:
        gap = (ang < 50 or ang > 310) or 130 < ang < 230
        if gap:
            return None
        return "7a2ad6" if d < 6.4 else "5a1aa8"
    return None


def repulse_color(dx, dy):
    d = math.hypot(dx, dy)
    if d <= 1.9:
        return "ffffff"
    if 2.6 <= d <= 3.8:
        return "bcd8ff"
    if 5.4 <= d <= 7.2 and abs(dx) > 2.2 and abs(dy) > 2.2:
        return "8fb4e8" if d < 6.4 else "6c92d0"
    if (abs(dx) < 1.2 and d > 4.2 and d < 7.6) or (abs(dy) < 1.2 and d > 4.2 and d < 7.6):
        return "dfeaff"
    return None


SACRIFICE = [
    "................",
    ".......rr.......",
    ".......rr.......",
    "......rrrr......",
    "......rrrr......",
    ".....rrrrrr.....",
    "....rrrrrrrr....",
    "....rrhrrrrr....",
    "...rrrhrrrrrr...",
    "...rrrhrrrrrr...",
    "...rrrrrrrrdr...",
    "...rrrrrrrddr...",
    "....rrrrrdddr...",
    "....rrrrdddr....",
    ".....rrrrrr.....",
    "......rrrr......",
]
SACRIFICE_PAL = {"r": "d41a1a", "h": "ff9a9a", "d": "8a0a0a"}

LOCK = [
    "................",
    ".....ssssss.....",
    "....ss....ss....",
    "....ss....ss....",
    "....ss....ss....",
    "....ss....ss....",
    "..bbbbbbbbbbbb..",
    "..bllllllllllb..",
    "..bllllllllllb..",
    "..bllllkkllllb..",
    "..bllllkkllllb..",
    "..bllllllllllb..",
    "..bbbbbbbbbbbb..",
    "................",
    "................",
    "................",
]
LOCK_PAL = {"s": "b4b4be", "l": "9a9aa6", "b": "6a6a76", "d": "7a7a86", "k": "2a2a32"}

CHECK = [
    "................",
    "................",
    "..............gg",
    ".............ggg",
    "............ggg.",
    "...........ggg..",
    "..gg......ggg...",
    "..ggg....ggg....",
    "...ggg..ggg.....",
    "....gggggg......",
    ".....gggg.......",
    "......gg........",
    "................",
    "................",
    "................",
    "................",
]
CHECK_PAL = {"g": "3ad24a"}

CROSS = [
    "................",
    "................",
    "...xx......xx...",
    "....xx....xx....",
    ".....xx..xx.....",
    "......xxxx......",
    ".......xx.......",
    "......xxxx......",
    ".....xx..xx.....",
    "....xx....xx....",
    "...xx......xx...",
    "................",
    "................",
    "................",
    "................",
    "................",
]
CROSS_PAL = {"x": "c8c8d0"}

PLUS = [
    "................",
    "................",
    "................",
    ".......pp.......",
    ".......pp.......",
    ".......pp.......",
    "....pppppppp....",
    "....pppppppp....",
    ".......pp.......",
    ".......pp.......",
    ".......pp.......",
    "................",
    "................",
    "................",
    "................",
    "................",
]
PLUS_PAL = {"p": "3ad24a"}


def main():
    os.makedirs(OUT, exist_ok=True)
    icons = {
        "fire": (from_ascii(FIRE, FIRE_PAL), "5a1004"),
        "ice": (from_ascii(ICE, ICE_PAL), "17406e"),
        "healing": (from_ascii(HEALING, HEALING_PAL), "0f4a1a"),
        "phase": (procedural(phase_color), "2a0a58"),
        "repulse": (procedural(repulse_color), "1c3a6e"),
        "sacrifice": (from_ascii(SACRIFICE, SACRIFICE_PAL), "4a0606"),
        "lock": (from_ascii(LOCK, LOCK_PAL), "1a1a20"),
        "check": (from_ascii(CHECK, CHECK_PAL), "0c4a14"),
        "cross": (from_ascii(CROSS, CROSS_PAL), "30303a"),
        "plus": (from_ascii(PLUS, PLUS_PAL), "0c4a14"),
    }
    for name, (body, edge) in icons.items():
        write_png(os.path.join(OUT, name + ".png"), outline(body, edge))
    print("wrote technique sprites to", os.path.normpath(OUT))


if __name__ == "__main__":
    main()
