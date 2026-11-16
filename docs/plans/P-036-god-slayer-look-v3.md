# P-036 — God Slayer look, round 3 (BL-013)

> **Tóm tắt cho Huy (VN, ≤ 3 dòng):** Gắn đầu liền thân, đầu nhỏ đi 10%; katana lưỡi quay ra trước, mũi vát ở đầu; tóc một mảnh trắng hình thang; cánh mới hai khúc xương theo mẫu Huy đã duyệt (D105).

- **Status:** ready
- **Mod / decision:** `permahearts`, D105 · **Branch:** `feat/P-036-look-v3` · **Depends on:** P-035 (merged, released 0.21.0)
- Source of truth for wording: row BL-013 in `docs/backlog/BACKLOG.md`. Colors stay ours (D98).
- Files: `client/GodSlayerModel.java`, `client/GodSlayerRenderer.java` if needed, `tools/make_god_slayer_texture.py` (box sizes must match the model), the texture PNGs, `GodSlayerClientTest`.

## 1. Head

- **No gap** between the head and the body: today there is an empty space between them. The head (and the neck if any) must sit on the chest/collar so no sky shows through from any side.
- **Head 10% smaller**: width (sides) −10% and height −10%; the forehead is too high, so take most of the height from above the eyes. Keep the eyes, brow, tusks readable (D101); horns move with the head.

## 2. Katana (both blades)

Today the cutting edge faces inward and the spine faces forward, and the slanted cut is at the handle end. Fix:
- The **cutting edge faces forward and outward**, the **spine faces back/inward**.
- The **slanted cut is at the tip** (the far end from the handle), the edge side shorter than the spine side, the slant facing **forward and up**.
- Blade stays straight with the same width to the end (D101). Check it in a close side screenshot.

## 3. Hair

Replace the current hair with **one simple white piece**: it starts at the **top back of the head**, falls **straight down to mid-back**, and **widens toward the bottom like a trapezoid**. It must **cover the whole back of the head** (today a bare white strip of head shows). No other hair pieces needed apart from what frames the face from the front, if any; keep it simple. The mockup below shows the idea (stacked slices, top 14 wide, about 27 wide at the bottom).

## 4. Wings (approved mockup)

Reference: `docs/mods/permahearts/god-slayer-wings-mockup.html` (open it in a browser; `build()` and `feather()` are the geometry; slider defaults are Huy's approved values). Units there = model pixels. Mockup axes: y up from the feet, the back is −z; convert to the model's axes (y down in `ModelPart`). Remove the current wing (one bone with a fan at its end plus a short fan at the shoulder) and build, for each side (mirror for the left):

- **Inner bone (gold, 4.5 × 4.5 cross-section):** from the middle of the back (x = ±4, about 3 px below the shoulder line, just behind the back), 34 long, going **outward and up 29°**, slightly backward (z −0.22 per unit of length, before normalizing). A gold 6-cube joint at its end (the "elbow").
- **Outer bone (gold, 4.5):** from the elbow, 33 long, going **outward and down 7°**, slightly backward (z −0.12). A gold 5-cube at its end. From behind, the two wings read as **^ ^**.
- **11 feathers per wing**, each a flat 4 wide × 1.2 thick box with a black 3-long tip, alternating dark red `#3a0f0f` / red `#7a1414`, behind the bones. Each root is **2 px below the bone centre line** so no root shows above the bone. Each hangs from its root and is rotated **in the wing plane, from straight down toward the outside**:
  - 6 on the inner bone, spread evenly from 10% to 90% of its length; 5 on the outer bone, from 6% to 98%.
  - **Angles, evenly spaced from innermost to outermost:** 4°, 10.9°, 17.8°, 24.7°, 31.6°, 38.5°, 45.4°, 52.3°, 59.2°, 66.1°, 73°.
  - **Lengths, innermost to outermost:** 17, 18.5, 20, 21.5, 28, 29.5 (inner bone), 37, 42, 45, 47, 52 (outer bone).
  - Feathers are separate with gaps between them (no solid sheet).
- Keep D101 behavior: wings show **only while flying**; flap by rotating the whole wing; hover upright, flying forward leans up to 45°. Scale everything up or down a little only if it clips the hair, horns or arms; say so in the report.

## Tests and screenshots

- Client screenshots (look at them yourself and iterate): `front`, a **true side view** (camera exactly 90° to the side, not three-quarter), `back` (hair covers the back of the head; wings ^ ^ when frozen in fly), `face` close-up, `katana` close-up from the side, `flying-back`, `flying-side`. Use `/boss freeze` states (D104) in the test.
- Copy the final PNGs to `/private/tmp/claude-501/-Users-truongdanghuy-Coding-Minecraft-Lab/27b354fc-b3c2-4308-a712-0331ea32eee9/scratchpad/p036/` and list them in the report.
- Server game tests must stay green.

## Verify

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
./gradlew :permahearts:build
```
Once at the end, plus the God Slayer client test(s) for the screenshots (muted).

## Docs

`spec.md` look lines; `playtest.md` line for the new look; D105 is already in `01-decisions.md`.

## Commit

Branch `feat/P-036-dev` from `feat/P-036-look-v3`, small commits, merge into `feat/P-036-look-v3`. Do not push (the manager pushes). English messages ending with the Co-Authored-By line.
