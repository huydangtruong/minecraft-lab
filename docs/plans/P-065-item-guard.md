# P-065 — Item guard passive (7th passive)

> **Tóm tắt cho Huy (VN, ≤ 3 dòng):** passive mới Item guard: đồ không vỡ khi hết độ bền, mà thành đồ hỏng mang nợ độ bền (100% ở Lv1 → 10% ở Lv10). Sửa bằng đe hoặc Mending trả nợ xong là dùng lại.

- **Status:** ready
- **Mod / decision:** `immortal-cultivation`, D176 (source of truth, read it) · **Branch:** `feat/P-065-item-guard` (dev on `feat/P-065-dev`) · **Depends on:** D175 commit already on the branch

## Build
- `Passive` enum entry id `item_guard`, name "Item guard", 7th row of the Passives tab (P-057/P-058 pattern: `Passive`, `PassiveRules`, `Passives`, tab screen, detail page with the 10-level table, gem icon). Gem: **red ruby** (own shape, same style as the other gems).
- `PassiveRules.debtPercent(level)` = 100 − 10 × (level − 1) (Lv1 100 … Lv10 10); `debt(level, maxDamage)` = ceil(pct × maxDamage / 100). Value text on the row like the others, e.g. "debt 70%".
- When an item with durability would break while used/worn by a player whose Item guard level ≥ 1 and switched on (and the mod is on): it does not break, it becomes **broken** and stores its debt on the stack (data component in namespace `permahearts`, codec-safe for saves).
- Broken stack: stays in place; unusable: no mining speed or tool/weapon bonus (acts like an empty hand), bow/crossbow/trident/fishing rod/shield/flint and steel etc. cannot be used, armor and its enchantments give nothing, elytra cannot glide. Red tooltip line "Broken: −N durability"; item bar red.
- Repair: anvil (material units or combining two items) and Mending pay the debt first, then normal durability. Broken items must be picked by Mending. Once debt is 0 and durability ≥ 1 the item works normally again and the component is removed. Grindstone / crafting-grid combine: treat like anvil combine if simple, else leave vanilla and note it.
- Debt is fixed at break time (changing the passive later does not change it). Passive off / mod off / no passive: vanilla breaking.
- Pure rules in `PassiveRules` / a small `ItemGuardRules` (no Minecraft imports): debt, repair split (amount → debt paid, durability gained), usable check. JUnit for Lv1/Lv5/Lv10 and repair splits.

## Tests
- Checklist (docs/workflow/team.md): areas = passive + item durability mixins; change type = **large** (new passive, several mixins, UI row) → `./gradlew :immortal-cultivation:build` once and `runClientGameTest` once.
- Server game tests (`passive` area or new `item_guard`): a player with Lv1 breaking a diamond pickaxe keeps it with debt 1561; Lv10 → debt 157; broken pickaxe mines no faster than a hand; broken chestplate gives 0 armor; anvil repair with diamonds pays debt first; Mending orbs pay debt then repair; without the passive the item breaks as vanilla.
- Client: screenshot of the Passives tab with the 7th row and a broken item tooltip; look at them. Clients muted.

## Docs / commit
Findings in `02-findings.md`, mod spec in `docs/mods/immortal-cultivation/`. English commits ending `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`. Merge into `feat/P-065-item-guard` locally, no push, never `main`, no install.sh.
