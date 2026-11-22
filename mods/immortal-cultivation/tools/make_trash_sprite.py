"""D217: the trash can icon (16x16) drawn in the trash slot of the inventory screen.
Run: python3 tools/make_trash_sprite.py  -> assets/permahearts/textures/gui/sprites/trash/trash.png"""
import zlib, struct, os

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "permahearts",
                   "textures", "gui", "sprites", "trash")
C = {
    'K': (40, 40, 48, 255),     # outline
    'L': (150, 156, 168, 255),  # lid light
    'l': (112, 118, 130, 255),  # lid shadow
    'B': (176, 182, 194, 255),  # body light
    'b': (132, 138, 150, 255),  # body mid
    'd': (92, 98, 110, 255),    # body dark / slats
    'H': (210, 214, 224, 255),  # handle highlight
}
ROWS = [
    "................",
    "................",
    "......KKKK......",
    ".....KHHHHK.....",
    "..KKKKKKKKKKKK..",
    "..KLLLLLLLLLlK..",
    "..KllllllllllK..",
    "...KKKKKKKKKK...",
    "...KBdBdBdBbK...",
    "...KBdBdBdBbK...",
    "...KBdBdBdBbK...",
    "...KBdBdBdBbK...",
    "...KBdBdBdBbK...",
    "...KBdBdBdBbK...",
    "....KbbbbbbK....",
    "....KKKKKKKK....",
]

def write_png(path, rows):
    raw = b''
    for row in rows:
        raw += b'\0' + b''.join(bytes(C[ch]) if ch != '.' else bytes((0, 0, 0, 0)) for ch in row)
    def chunk(t, d):
        c = struct.pack('>I', len(d)) + t + d
        return c + struct.pack('>I', zlib.crc32(t + d) & 0xffffffff)
    w, h = len(rows[0]), len(rows)
    with open(path, 'wb') as f:
        f.write(b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0))
                + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))

if __name__ == "__main__":
    assert all(len(r) == 16 for r in ROWS) and len(ROWS) == 16
    os.makedirs(OUT, exist_ok=True)
    write_png(os.path.join(OUT, "trash.png"), ROWS)
