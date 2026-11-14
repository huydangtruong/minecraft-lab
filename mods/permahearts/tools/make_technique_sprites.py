"""Draws the technique icons (P-011 part A): 16x16 original pixel art, one per technique, plus a lock, a tick and a cross.

- technique/{fire,ice,healing,phase,repulse,sacrifice,flight,blade,teleport,golem,angel}.png, technique/lock.png, technique/check.png, technique/cross.png
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


FIRE = [  # 14 body rows with an empty row above and below, so the outline closes on both ends (Huy)
    "................",
    "......r.........",
    ".....rr.........",
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
    "................",
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


SACRIFICE = [  # silver cross (D50), original art
    "................",
    "......hhhs......",
    "......hmms......",
    "......hmms......",
    "..hhhhhhhhhhhs..",
    "..hmmmmwmmmmms..",
    "..hmmmmmmmmmms..",
    "..sssshmmsssss..",
    "......hmms......",
    "......hmms......",
    "......hmms......",
    "......hmms......",
    "......hmms......",
    "......hmms......",
    "......ssss......",
    "................",
]
SACRIFICE_PAL = {"h": "f4f7fa", "m": "c3cad3", "s": "7e8896", "w": "ffffff"}

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

TELEPORT = [  # a violet orb leaving three light streaks behind it (D58), original art
    "................",
    "................",
    "................",
    "........VVVV....",
    "......VVvvppVV..",
    "..sss.VvppwwpV..",
    "......VppwhwpV..",
    ".wsss.VpwhhwpV..",
    "......VvppwpvV..",
    "..sss.VVvppvVV..",
    "........VVVV....",
    "................",
    "................",
    "................",
    "................",
    "................",
]
TELEPORT_PAL = {"V": "5a1aa8", "v": "7a2ad6", "p": "b45cff", "w": "e8ccff", "h": "ffffff", "s": "d9b8ff"}


GOLEM = [  # a violet iron golem face with a long nose and two glowing eyes (D60), original art
    "................",
    "....VVVVVVVV....",
    "...VvvppppvvV...",
    "..VvpppppppvvV..",
    "..VpppppppppvV..",
    "..Vppwwppwwppv..",
    "..Vppwhppwhppv..",
    "..VpppppppppvV..",
    "..VvpppnnpppvV..",
    "..VvppnnnnppvV..",
    "..VvpppnnpppvV..",
    "...VvppppppvV...",
    "...VvvppppvvV...",
    "....VVvvvvVV....",
    "................",
    "................",
]
GOLEM_PAL = {"V": "5a1aa8", "v": "7a2ad6", "p": "b45cff", "w": "e8ccff", "h": "ffffff", "n": "8a3ae8"}


ANGEL = [  # a white feathered wing under a gold halo (D63), original art
    "................",
    ".....hgggggg....",
    "....gG......Gg..",
    ".....GGGGGGGG...",
    "................",
    "........WWWW....",
    "......WWWWWWw...",
    "....WWWWWWWwv...",
    "...WWWWWWWwwv...",
    "..WWWWWWWwwv....",
    "..WWWWWWwwv.....",
    ".WWWWWwwwv......",
    ".WWWwwwv........",
    "..WWwv..........",
    "...Wv...........",
    "................",
]
ANGEL_PAL = {"W": "ffffff", "w": "e4eaf4", "v": "b8c4d8", "g": "f2c540", "G": "c48a14", "h": "fff6b8"}


def mirror(half):
    """Symmetric art: 16 rows of 8 characters (the left half) become 16 rows of 16."""
    return [row + row[::-1] for row in half]


FLIGHT = mirror([  # two feathered wings (D52), original art
    "........",
    "........",
    ".ww.....",
    ".www....",
    ".wwww...",
    "..wwlw..",
    "..wwwlw.",
    "...wwlww",
    "...wllww",
    "....wllw",
    "....wglg",
    ".....wgg",
    "......gg",
    "......wg",
    "........",
    "........",
])
FLIGHT_PAL = {"w": "f4fbff", "l": "bfe0f4", "g": "7fb0d4"}


def blade_art():
    """A sword on the diagonal with blood on the tip and drops falling (D52)."""
    img = blank()

    def put(x, y, c):
        img[y][x] = rgba(c)

    for x in range(6, 14):
        put(x, 15 - x, "e8eef4")  # bright edge
        put(x, 16 - x, "9aa6b4")  # shade
    for x, y in ((3, 8), (4, 9), (5, 10), (6, 11), (7, 12)):
        put(x, y, "d8a020")  # cross guard
    put(4, 11, "6a3a1a")
    put(3, 12, "7a4a22")
    put(2, 13, "6a3a1a")
    put(1, 14, "d8a020")
    for x, y in ((13, 2), (12, 3), (11, 4), (13, 3), (12, 4)):
        put(x, y, "c01818")  # blood on the tip
    put(11, 5, "8a0c0c")
    put(10, 6, "8a0c0c")
    for x, y in ((10, 7), (9, 9), (12, 5)):
        put(x, y, "d82020")  # drops
    put(10, 8, "a01010")
    put(9, 10, "a01010")
    return img


def main():
    os.makedirs(OUT, exist_ok=True)
    icons = {
        "fire": (from_ascii(FIRE, FIRE_PAL), "5a1004"),
        "ice": (from_ascii(ICE, ICE_PAL), "17406e"),
        "healing": (from_ascii(HEALING, HEALING_PAL), "0f4a1a"),
        "phase": (procedural(phase_color), "2a0a58"),
        "repulse": (procedural(repulse_color), "1c3a6e"),
        "sacrifice": (from_ascii(SACRIFICE, SACRIFICE_PAL), "2a2f37"),
        "flight": (from_ascii(FLIGHT, FLIGHT_PAL), "2a5a80"),
        "blade": (blade_art(), "3a0606"),
        "teleport": (from_ascii(TELEPORT, TELEPORT_PAL), "2a0a58"),
        "golem": (from_ascii(GOLEM, GOLEM_PAL), "2a0a58"),
        "angel": (from_ascii(ANGEL, ANGEL_PAL), "5a3e0c"),
        "lock": (from_ascii(LOCK, LOCK_PAL), "1a1a20"),
        "check": (from_ascii(CHECK, CHECK_PAL), "0c4a14"),
        "cross": (from_ascii(CROSS, CROSS_PAL), "30303a"),
        "plus": (from_ascii(PLUS, PLUS_PAL), "0c4a14"),
    }
    items = os.path.join(OUT, "..", "..", "..", "item")  # focus item textures = the technique icons (P-011 B)
    for name, (body, edge) in icons.items():
        img = outline(body, edge)
        write_png(os.path.join(OUT, name + ".png"), img)
        if name in ("fire", "ice", "healing", "phase", "repulse", "sacrifice", "flight", "blade", "teleport", "golem", "angel"):
            write_png(os.path.join(items, "focus_" + name + ".png"), img)
    print("wrote technique sprites to", os.path.normpath(OUT))


if __name__ == "__main__":
    main()
