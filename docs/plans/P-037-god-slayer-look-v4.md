# P-037 — God Slayer look, round 4 (BL-015)

> **Tóm tắt cho Huy (VN, ≤ 3 dòng):** Đầu mỏng, hẹp và thấp hơn; sừng thon hơn; tóc cố định ngả ra sau 10°, dài quá thắt lưng; tay đưa ra trước thêm, khuỷu tay thấp xuống (D106).

- **Status:** ready
- **Mod / decision:** `permahearts`, D106 · **Branch:** `feat/P-037-look-v4` · **Depends on:** P-036 (released 0.22.0)
- Wording source: row BL-015 in `docs/backlog/BACKLOG.md`. All numbers are relative to 0.22.0 (current `main`).
- Files: `client/GodSlayerModel.java`, `tools/make_god_slayer_texture.py` (box sizes must match the model) and the PNGs it writes, `GodSlayerClientTest`.

## 1. Head
- **Thinner (depth):** today the head box is z −7.5..7.5 (×`HEAD_SCALE`). Cut the part **behind** the head centre by about **20%** and the part **in front** by about **10%**. Keep the face, brow, fangs and eyes on the front surface (move them with it).
- **Narrower:** width another **−10%** (`HEAD_WIDTH` 1.52 → about 1.37).
- **Lower:** remove **half of the top hair** (`hair_top`, 4 → about 2 tall) and **half of the forehead** (the head rows above the brow/eyes). From the eyes down stays. The crown spikes and the horns move down with the new top.
- **Horns:** each horn cube **about 10% less volume**: keep its length, make its cross-section about 95% per side (7→6.65, 6→5.7, 5→4.75, 3→2.85). Keep the angles from P-036. Re-seat them on the narrower, lower head.

## 2. Hair (mane)
- **Fixed:** no sway or speed term in `setupAnim` (`hairBack.xRot` is a constant). Today the mane moves and the head pokes in and out of it.
- **Tilt:** the bottom of the mane is **about 10° further back** than its root (back is +z here; check the sign in the side screenshot).
- **Longer:** the mane reaches **below the belt** (past the waist). Keep the trapezoid shape (narrow at the top, wider at the bottom). Use more slices if needed.
- **No clipping in any pose:** the head turns (yaw up to ±30°, pitch, and the flying lean). Neither the head nor the horns may go through the mane. Place the mane root far enough behind the thinner head; if that is not enough, lower the head yaw factor. Say what you did.

## 3. Arms
- **Forward a little more:** `ARM_FORWARD` 0.9 → about 1.05 rad. Both arms, katanas follow.
- **Elbow lower:** today the upper arm (`arm`, 14 long) is short so the folded forearm meets it in the middle of the bicep. Make the upper arm about **4–5 px longer** and move the forearm pivot down with it, so the elbow sits clearly at the bottom end of the bicep. Keep the katanas standing straight up in the rest pose and the slash animation working.

## Tests and screenshots
Same shots as P-036: `front`, true `side` (90°), `back`, `face`, `katana`, `flying-back`, `flying-side`. Add **`back-turned`**: the head turned to its max yaw, seen from behind and the side, to show the mane does not clip. Look at every shot yourself after the last change. Copy the final PNGs into an empty folder `/private/tmp/claude-501/-Users-truongdanghuy-Coding-Minecraft-Lab/27b354fc-b3c2-4308-a712-0331ea32eee9/scratchpad/p037/`. Every Minecraft you open is muted.

## Verify
```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
./gradlew :permahearts:build
```

## Docs
`spec.md` look lines; `playtest.md` line. D106 is already in `01-decisions.md`.

## Commit
Branch `feat/P-037-dev` from `feat/P-037-look-v4`, small commits, merge into `feat/P-037-look-v4`. Do not push. Do not commit `__pycache__` files. English messages ending with the Co-Authored-By line.
