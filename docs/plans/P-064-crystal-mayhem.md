# P-064 — Crystal Mayhem (20th technique)

> **Tóm tắt cho Huy (VN, ≤ 3 dòng):** chiêu mới Crystal Mayhem (ý tưởng của bạn Huy, D174). Chuột trái bắn mảnh thạch anh, mạnh hơn khi có ánh sáng, để lại cụm thạch anh 10 giây. Giữ chuột phải 1 giây biến các cụm thành khối thạch anh nảy mầm phát sáng, đốt và làm chậm quái trong 20 giây, rồi hồi 30 giây.

- **Status:** ready
- **Mod / decision:** `immortal-cultivation`, D174 · **Branch:** `feat/P-064-crystal-mayhem` (dev works on `feat/P-064-dev`, merges into it) · **Depends on:** none

## Goal
Add technique 20, Crystal Mayhem, exactly as D174 in `docs/system/01-decisions.md` (read it first; it is the source of truth). Look at how Solar Lance (`Lance`, `LanceRules`, hold + `RELEASE` packet, D157/D159), Crystal Prison (`Crystal`, `CrystalBlock`, `CrystalSavedData`: mod blocks placed in the world and cleaned up after restarts) and Fireball's burn field are built, and reuse their patterns.

## Behaviour (numbers from D174)
- Enum entry id **`mayhem`**, name "Crystal Mayhem", Attack group after Life Stealer, cooldown **30 s** at every level (Reduce cooldown passive applies like other techniques). Purple amethyst icon (16×16, same style). Short description (≤ 3 lines).
- **Left click — Shard Projection** (focus selected):
  - Fires one fast amethyst-shard projectile along the crosshair (no gravity, ~64 block range). Stops at the first living target or block.
  - Damage = `2 + extra × (1 + 0.05 × (L − 1))`. At the block where the target stands: `sky` = real sky light (raw sky light minus the level's sky darkening, ≥ 0) but **0 when day time % 24000 is in [16000, 23000)**; `block` = block light. The higher light level wins (tie → sky); `extra = sky / 3` (int) if sky wins, else `block / 5` (int).
  - Targets: same filter as other technique AoE (no caster, no own clones/guardians/angels/grasped mobs; Death Match opponent hit like other techniques). Use `TechniqueDamage`. Respect vanilla invulnerability frames.
  - 20 shards per batch. Per-shard delay `max(0.1, 1 − 0.05 × (L − 1))` s.
  - Before the channel is used, each shard places an **amethyst cluster** (mod block that looks like a vanilla cluster, oriented to the surface, no drops, no amethyst farming) at the impact: on the ground under the hit mob, or on the hit face of the wall/ceiling/floor. Only into air (skip if not air). Removed after **10 s**.
  - After the 20th shard of the first batch: 30 s cooldown starts, but the channel can still be used once during that cooldown (using it cancels the cooldown and starts the 20 s window).
- **Hold right click 1 s — Light Strike Reflection** (not allowed during the per-shard delay; releasing early cancels with no cost; show a small charge bar like other hold casts):
  - Chain conversion: repeatedly pick the nearest unconverted cluster within **5 blocks of the player**; if none, the nearest within 5 blocks of any already converted budding. Up to **4 + L** buddings. Converted spot becomes a **budding amethyst** mod block with decorative clusters on every face that touches air; decorative clusters are never chain targets. Converted clusters lose their 10 s timer.
  - During the 20 s window, keep converting (check every few ticks) when the player walks near other live clusters, until the cap.
  - **Active** buddings = reachable from the player through links of ≤ 5 blocks (player → budding → budding). Active ones give **light 15** (inactive: no light; a block state `lit` is fine) and every second apply **Slowness** (I, ~1.5 s, refreshed) and burn damage **`1 + 0.5 × (L − 1)`** to targets within radius **`3 + (L − 1) / 2`** (int). No fire visual (a few purple sparkle particles are fine). A target in several zones takes **one** hit per second.
  - Refills 20 shards; these shards place no clusters.
  - **20 s after the channel**: remove every budding, decorative cluster and leftover cluster of this player, drop the remaining shards, start the 30 s cooldown.
  - Channel with no cluster at all still refills and starts the 20 s window (D174 "cái giá của dùng sai").
- Death, dimension change, logout: end at once (remove blocks, cooldown starts). Server restart: no orphan blocks (saved data like `CrystalSavedData`).
- HUD: on the existing time-left line above the skill bar show the shards left (e.g. "Shards 14/20") and, during the window, the seconds left.
- `/technique cast mayhem <lv>` and the `/admin` Techniques tab must work.

## Pure Java
`MayhemRules` (no Minecraft imports): shard damage from (sky, block, level, day time), per-shard delay, max buddings, burn damage, burn radius, chain pick order on plain coordinates, active set (BFS over ≤ 5 links). JUnit `MayhemRulesTest` with Lv1/Lv10/Lv20 values: day 7 / 9.25 / 11.75; block light 15: 5 / 6.35 / 7.85; dark 2; delay 1 / 0.55 / 0.1; buddings 5 / 14 / 24; burn 1 / 5.5 / 10.5; radius 3 / 7 / 12.

## Tests
- Test scope checklist (docs/workflow/team.md "Test theo phạm vi thay đổi"): areas = new technique `mayhem` + technique registry; change type = **large** (new technique, blocks, HUD); commands = `./gradlew :immortal-cultivation:build` once and `runClientGameTest` once.
- Server game tests (`mayhem` area): shard damage in daylight vs dark; cluster placed and gone after 10 s; channel converts a chain and respects 4 + L; out-of-range budding is not lit and does not burn; burn hits once per second even with two overlapping buddings; after 20 s all blocks are gone and the 30 s cooldown runs; channel with no cluster refills.
- Client game test: one screenshot with buddings lit and the HUD line, and the Techniques tab row. Look at them yourself. Clients stay muted (`MuteTestClientMixin`, do not remove).

## Docs
`docs/system/02-findings.md` (any new API names, e.g. sky darkening and day time on 26.3), mod spec in `docs/mods/immortal-cultivation/`. Do not edit 03-status (manager does).

## Commit
Small coherent commits in English ending with `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`. Merge `feat/P-064-dev` into `feat/P-064-crystal-mayhem`. Never push `main`, never touch Huy's saves or profile, no install.sh.

## Done when
- [ ] Shard damage by light, 20 shards, per-shard delay, 10 s clusters
- [ ] Channel chain, cap, active set, light 15, slowness + burn once per second, 20 s window then 30 s cooldown
- [ ] No orphan blocks after death/logout/restart; no drops from mod blocks
- [ ] build + client game tests green, screenshots looked at; report ≤ 10 lines with the checklist

## Risks / ask first
- If the left click cannot be read while a technique focus is held without breaking blocks or swinging, use the same input path Solar Lance uses and say so.
