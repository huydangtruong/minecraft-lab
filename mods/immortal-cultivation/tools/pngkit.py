"""Shared helpers of the sprite scripts: RGBA colours and a stdlib-only PNG writer (8-bit RGBA, zlib level 9).
Import with `from pngkit import write_png, rgba, CLEAR`; run the scripts as `python3 tools/<script>.py`."""
import os
import struct
import zlib

CLEAR = (0, 0, 0, 0)


def rgba(h):
    """'rrggbb' hex string to an opaque (r, g, b, 255) tuple."""
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def write_png(path, px):
    """Writes px, a list of rows of (r, g, b, a) tuples, as a PNG. Creates the folder if needed."""
    h, w = len(px), len(px[0])
    raw = b"".join(b"\x00" + bytes(v for c in row for v in c) for row in px)

    def chunk(t, c):
        return struct.pack(">I", len(c)) + t + c + struct.pack(">I", zlib.crc32(t + c) & 0xFFFFFFFF)

    os.makedirs(os.path.dirname(path) or ".", exist_ok=True)
    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
                + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))
