# P-057 — Passives tab

> **Tóm tắt cho Huy (VN, ≤ 3 dòng):** tab thứ 3 "Passives" trong bảng Cultivation: 5 nội tại 10 cấp (giảm hồi chiêu, nhìn trong tối, giữ đói, thêm XP, giữ đồ khi chết), mỗi cấp 5 cấp XP (D148).

- **Status:** done (coded by the main session, no dev agent, at Huy's request)
- **Mod / decision:** `immortal-cultivation`, D148 · **Branch:** `feat/P-057-passives` · **Depends on:** none

## Rules (pure Java, `passive/PassiveRules.java`, JUnit `PassiveRulesTest`)
- `MAX_LEVEL = 10`, `COST_LEVELS = 5` vanilla XP levels per upgrade (server: `giveExperienceLevels(-5)`).
- Reduce cooldown: `3 + 1.5 (L − 1)` %; `reducedCooldown(base, L) = max(1, round(base × (1 − pct/100)))`.
- Increase vision: `0.2 + (L − 1)/9` (1.2 at Lv 10).
- Hunger sustain: floor `4 + L` food points (2 points = 1 drumstick): 2.5 → 7 drumsticks.
- XP gain bonus: `3 L` %; fractional bonus rounded up with that probability.
- Inventory protect: slot groups in order selected hotbar slot, offhand (40), head (39), feet (36), chest (38), legs (37), hotbar 0–8, rows 9–17, 18–26, 27–35.

## Data and network
- `passive/PassiveData` record (`levels` map by passive id, `vision_on`), attachment `permahearts:passives` (persistent, copyOnDeath, synced to its own player).
- `passive/PassiveNet`: payload `permahearts:passive_action` (kind UPGRADE=0 / TOGGLE_VISION=1, passive id). `apply` checks the world switch, the level cap and the 5 levels.

## Glue (`passive/Passives`)
- Cooldown: the 4 places that start a cooldown (`TechniqueNet.cast`, Blade in `TechniqueCasts`, `Warp`, `Grasp`) wrap the ticks in `Passives.cooldown`.
- Hunger: `FoodDataMixin` injects at TAIL of `FoodData.tick` and raises the food level to the floor.
- XP: `ExperienceOrbMixin` wraps `Player.giveExperiencePoints` inside `ExperienceOrb.playerTouch` (after Mending).
- Inventory: `PlayerMixin` wraps `Inventory.dropAll` in `Player.dropEquipment` (after `destroyVanishingCursedItems`): protected stacks are taken out, the rest drops, the stacks go back on the dead player; `COPY_FROM` (normal death, keepInventory off, not a Death Match protected death) moves them to the new player.
- Every passive is 0 when the world switch is off.

## Client
- `StatsPanel.Tab.PASSIVES` (edge tab with an amethyst shard), `client/PassivesTab` (rows, gems drawn from a pixel mask, "+", detail page with a two-column table), `PassiveVision` + `PassiveVisionMixin` (lightmap: night vision, brightness and weaker Darkness above 100%) + `PassiveWaterVisionMixin` (clearer water above 100%).

## Tests
- JUnit `PassiveRulesTest`; game tests `PassiveGameTest` (cost and cap, toggle, cooldown, food floor, orb XP, slots kept through a real death and respawn); client test `PassiveClientTest` (real "+" click, gem toggle, detail pages; screenshots `passives-list`, `passives-detail-inventory`, `passives-detail-vision`).
