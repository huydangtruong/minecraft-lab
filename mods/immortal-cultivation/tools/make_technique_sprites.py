"""Draws the technique icons (P-011 part A): 16x16 original pixel art, one per technique, plus a lock, a tick and a cross.

- technique/{fire,ice,healing,phase,repulse,sacrifice,flight,blade,teleport,golem,angel,aqua,illusion,warp,thunder}.png, technique/lock.png, technique/check.png, technique/cross.png
Each icon is drawn as a body; a dark outline is added automatically around it.
Run: python3 -I mods/immortal-cultivation/tools/make_technique_sprites.py
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

TELEPORT = [  # a turquoise orb leaving three light streaks behind it (D58, D74 colour), original art
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
TELEPORT_PAL = {"V": "0b6e69", "v": "1a9f97", "p": "30d5c8", "w": "c4f7f2", "h": "ffffff", "s": "a6f0e9"}


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


AQUA = [  # a blue-green water drop with a wave and two bubbles (D64), original art
    "................",
    ".......ww.......",
    "......wWWw......",
    "......WWWW......",
    ".....wWWWWw...bb",
    ".....WWWWWW..bBb",
    "....wWWhWWWw..bb",
    "....WWhWWWWW....",
    "....WWWWWWWW.bb.",
    "....wWWWWWWw.bBb",
    ".....wWWWWw...bb",
    "..t...wwww......",
    ".tTt.....tt..t..",
    "tT.tT..tTtT.tTt.",
    "...tTtTt..tTt...",
    "................",
]
AQUA_PAL = {"w": "9ff4ea", "W": "2fc4c8", "h": "e8fffb", "b": "bff7ff", "B": "ffffff", "t": "38a8e0", "T": "1c78c0"}


def illusion_art():
    """Two overlapping player silhouettes: a faded one behind and a solid one in front (D71), original art."""
    img = blank()

    def rect(x0, y0, x1, y1, c):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                img[y][x] = rgba(c)

    # the faded one behind (left, higher)
    rect(2, 1, 5, 4, "a6adbd")
    rect(2, 1, 5, 1, "7a8294")
    rect(2, 5, 5, 10, "92b4de")
    rect(1, 5, 1, 10, "b9c1d1")
    rect(6, 5, 6, 10, "b9c1d1")
    rect(2, 11, 3, 13, "7d86a3")
    rect(4, 11, 5, 13, "8a93b0")
    # the solid one in front (right, lower)
    rect(7, 3, 10, 6, "f0c090")
    rect(7, 3, 10, 4, "5a3a1c")
    img[5][7] = rgba("3a4a9a")
    img[5][10] = rgba("3a4a9a")
    rect(7, 7, 10, 12, "2f8fe0")
    rect(8, 8, 9, 8, "5ab0f0")
    rect(6, 7, 6, 8, "2f8fe0")
    rect(11, 7, 11, 8, "2f8fe0")
    rect(6, 9, 6, 12, "f0c090")
    rect(11, 9, 11, 12, "f0c090")
    rect(7, 13, 8, 14, "3a3a9a")
    rect(9, 13, 10, 14, "4a4ab0")
    return img


def warp_color(dx, dy):
    """Cosmic Warping (D75): a purple ring around a coloured centre, like the portal."""
    d = math.hypot(dx, dy)
    if d <= 3.2:
        return "ffd36a" if d <= 1.6 else "ff8a4a"
    if 4.4 <= d <= 7.0:
        return "b45cff" if d < 5.6 else "7a2ad6"
    return None


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


THUNDER = [  # a grey cloud with a yellow lightning bolt coming out of it (D86), original art; empty rows above and below so the outline closes
    "................",
    "....cccc........",
    "...cccccc.cc....",
    "..ccccccccccc...",
    ".ccccccccccccgg.",
    ".gggggggggggggg.",
    "..dddddddddddd..",
    "......yyyh......",
    ".....yyyh.......",
    "....yyyyyyy.....",
    "......yyyY......",
    ".....yyyY.......",
    ".....yyY........",
    "....yyY.........",
    "....yY..........",
    "................",
]
THUNDER_PAL = {"c": "e4e8f0", "g": "aab2c0", "d": "7c8496", "y": "ffd62a", "Y": "f0a000", "h": "fff7a8"}


GRASP = [  # an open golden hand with a white spark in the palm (D134), original art
    "................",
    "...g..g..g......",
    "...Gg.Gg.Gg.g...",
    "...Gg.Gg.Gg.Gg..",
    ".g.Gg.Gg.Gg.Gg..",
    ".Gg.GgGgGgGgGg..",
    ".GgGGGGGGGGGGg..",
    "..GGGGGGGGGGGg..",
    "..GGGGhhGGGGg...",
    "..GGGGhwhGGGg...",
    "...GGGGhhGGg....",
    "....GGGGGGGg....",
    ".....GGGGGg.....",
    ".....gGGGGg.....",
    "......gggg......",
    "................",
]
GRASP_PAL = {"G": "f2c540", "g": "c48a14", "h": "fff3b0", "w": "ffffff"}


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
        "teleport": (from_ascii(TELEPORT, TELEPORT_PAL), "053a37"),
        "golem": (from_ascii(GOLEM, GOLEM_PAL), "2a0a58"),
        "angel": (from_ascii(ANGEL, ANGEL_PAL), "5a3e0c"),
        "aqua": (from_ascii(AQUA, AQUA_PAL), "0c3a5a"),
        "illusion": (illusion_art(), "1c2640"),
        "warp": (procedural(warp_color), "2a0a58"),
        "thunder": (from_ascii(THUNDER, THUNDER_PAL), "2a2f3a"),
        "grasp": (from_ascii(GRASP, GRASP_PAL), "5a3e0c"),
        "lock": (from_ascii(LOCK, LOCK_PAL), "1a1a20"),
        "check": (from_ascii(CHECK, CHECK_PAL), "0c4a14"),
        "cross": (from_ascii(CROSS, CROSS_PAL), "30303a"),
        "plus": (from_ascii(PLUS, PLUS_PAL), "0c4a14"),
    }
    items = os.path.join(OUT, "..", "..", "..", "item")  # focus item textures = the technique icons (P-011 B)
    for name, (body, edge) in icons.items():
        img = outline(body, edge)
        write_png(os.path.join(OUT, name + ".png"), img)
        if name in ("fire", "ice", "healing", "phase", "repulse", "sacrifice", "flight", "blade", "teleport", "golem", "angel", "aqua", "illusion", "warp", "thunder", "grasp"):
            write_png(os.path.join(items, "focus_" + name + ".png"), img)
    print("wrote technique sprites to", os.path.normpath(OUT))


if __name__ == "__main__":
    main()
