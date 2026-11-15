# P-035 — Fixes from Huy's 0.20.0 playtest (BL-009 to BL-012)

> **Tóm tắt cho Huy (VN, ≤ 3 dòng):** God Slayer chạy đuổi thay vì dịch chuyển sớm; sửa ngoại hình (nanh, mắt, tay, cánh khi bay, katana, tóc); thông báo Crimson Master màu cam, đếm ngược God Slayer đỏ 60 giây có nhịp tim và viền đỏ; Thần Tốc +5% mỗi bình.

- **Status:** ready
- **Mod / decision:** `permahearts`, D100–D103 (below) · **Branch:** `feat/P-035-playtest-fixes` · **Depends on:** P-034 (merged)
- Source of truth for wording: rows BL-009 to BL-012 in `docs/backlog/BACKLOG.md`. Do them in this order; commit after each.

## 1. BL-011 — God Slayer teleports too early (D100)

Huy ran a short way and the boss teleported at once instead of running after him.
- Find the cause in `godslayer/GodSlayer.java` / `GodSlayerRules.shouldTeleport` (suspect: the "no progress for `STUCK_TICKS` = 100" counter also fires when the player just runs as fast as the boss on open ground).
- New rule: the boss **runs after the target** while it has a path. Teleport only when (a) the target is in another dimension, (b) the target is farther than **64** blocks, or (c) the boss **has no path** to the target, or is physically stuck (not moving itself, colliding) for **8 s** while not in reach. Merely "distance did not shrink" never counts.
- Tests: JUnit for the new decision; game test: the player runs 20–30 blocks away on flat open ground → no teleport within 6 s, the boss is closing in by walking; the existing 60-block / Nether tests still pass (adjust the 60-block one to > 64).

## 2. BL-012 — God Slayer look (D101)

Read the BL-012 row word by word. The look stays our own design (D98: dark red skin, white hair, black armor with gold trim, glowing yellow eyes). In `GodSlayerModel` / `GodSlayerRenderer` / `tools/make_god_slayer_texture.py`:
1. **Fangs** stick out of the mouth, pointing forward and slightly up; they do not cover the face or the eyes.
2. **Remove the two armor pieces at the armpits.**
3. **Eyes:** same glow, a bit **bigger**, **slanted up toward the outside** (angry), a frowning brow above; nothing covers them. The face must be clearly visible (not hidden by horns/shoulder plates).
4. **Arms:** angled **forward** about 30–45° from horizontal so both katana tips point slightly ahead; not straight out in front, not flat to the sides like now.
5. **Katana:** the blade is **perfectly straight** with the same width to the end (no taper); the tip is **cut on a slant**, the edge side a bit shorter than the back (spine) side.
6. **Hair:** long and big, a mane falling down the back to the waist.
7. **Wings when flying:** two big wings (own design, e.g. dark red/black membrane with gold bones) shown only while in the flying mode, folded/hidden otherwise. **Flying still → body upright; flying forward → body leans forward about 45°** (smooth blend by horizontal speed). The flying state must reach the client (synced entity data).
- Client test screenshots: front, a **real side view** (camera 90° to the side), and flying (wings, lean). Look at them yourself and iterate until each point above reads clearly. Put the final PNGs in the report.

## 3. BL-010 — Announcement colors and countdown (D102)

- The Crimson Master arrival announcement (title, chat, top bar where it is colored) turns **orange** (`ChatFormatting.GOLD` or an orange RGB; pick the one that reads orange). **Red** is kept for God Slayer only.
- God Slayer warning: the **countdown number is shown for the whole 60 s** (not only the last 10), in **red**, to every player; a **heartbeat sound** plays through the whole 60 s (e.g. `WARDEN_HEARTBEAT` or two low bass-drum beats; speeds up near the end is nice).
- **Red screen edge for every player**, growing stronger as the countdown nears 0 (a client HUD vignette driven by a small sync packet or by the remaining time sent with the countdown). Gone when the boss appears.
- Tests: a client screenshot during the countdown near the end (red number + red edge).

## 4. BL-009 — Swiftness +5% per bottle (D103)

`ElixirStat.SWIFTNESS` step 0.10 → **0.05**, applies to bottles already drunk. Fix tests, `spec.md`, and anything that prints the number. Test mode (10 bottles) becomes 150%.

## Decisions to add to `docs/system/01-decisions.md` (Vietnamese, same style)

D100 (BL-011), D101 (BL-012), D102 (BL-010), D103 (BL-009, replaces the Swiftness part of D67). Mark BL-009..BL-012 done in BACKLOG as `~~BL-0NN~~ → P-035`.

## Verify

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
./gradlew :permahearts:build
```
Once at the end (client tests muted).

## Docs

`spec.md` (God Slayer teleport rule, look, countdown; Swiftness 5%), `playtest.md` lines, `02-findings.md` for new API names.

## Commit

Branch `feat/P-035-dev` from `feat/P-035-playtest-fixes`; merge into `feat/P-035-playtest-fixes`; do not push (the manager pushes).
