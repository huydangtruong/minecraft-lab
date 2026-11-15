"""Draws the Cosmic Warping portal textures (P-026, D75): original art, 64x64, shader-free.

- textures/entity/warp_portal_ring.png: purple outline ring with a soft halo (static)
- textures/entity/warp_portal_swirl.png: white spiral arms, tinted per owner by the renderer and rotated over time
Run: python3 -I mods/permahearts/tools/make_warp_textures.py
"""
import importlib.util
import math
import os

HERE = os.path.dirname(__file__)
spec = importlib.util.spec_from_file_location("hearts", os.path.join(HERE, "make_heart_sprites.py"))
hearts = importlib.util.module_from_spec(spec)
spec.loader.exec_module(hearts)
write_png = hearts.write_png

OUT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "permahearts", "textures", "entity")
N = 64


def px(r, g, b, a):
    return (r, g, b, max(0, min(255, int(a))))


def ring():
    img = []
    for y in range(N):
        row = []
        for x in range(N):
            dx, dy = (x + 0.5) / N - 0.5, (y + 0.5) / N - 0.5
            d = math.hypot(dx, dy)  # 0 centre, 0.5 edge of the texture
            if d < 0.36:
                row.append(px(40, 10, 70, 70 * (d / 0.36) ** 2))  # a faint dark disc under the swirl
            elif d < 0.46:
                t = (d - 0.36) / 0.10  # 0 inner edge .. 1 outer edge of the band
                core = 1 - abs(t - 0.5) * 2
                row.append(px(int(122 + 90 * core), int(42 + 50 * core), int(214 + 41 * core), 235 * min(1, core * 2.2 + 0.25)))
            elif d < 0.5:
                t = (d - 0.46) / 0.04
                row.append(px(180, 92, 255, 120 * (1 - t) ** 2))  # halo
            else:
                row.append(px(0, 0, 0, 0))
        img.append(row)
    return img


def swirl():
    img = []
    for y in range(N):
        row = []
        for x in range(N):
            dx, dy = (x + 0.5) / N - 0.5, (y + 0.5) / N - 0.5
            d = math.hypot(dx, dy)
            if d > 0.37:
                row.append(px(255, 255, 255, 0))
                continue
            a = math.atan2(dy, dx)
            v = 0.5 + 0.5 * math.sin(3 * a + d * 26)
            edge = 1 - (d / 0.37) ** 3
            alpha = (50 + 150 * v) * edge + 90 * max(0, 1 - d / 0.12)  # bright core
            row.append(px(255, 255, 255, alpha))
        img.append(row)
    return img


def main():
    os.makedirs(OUT, exist_ok=True)
    write_png(os.path.join(OUT, "warp_portal_ring.png"), ring())
    write_png(os.path.join(OUT, "warp_portal_swirl.png"), swirl())
    print("wrote warp portal textures to", os.path.normpath(OUT))


if __name__ == "__main__":
    main()
