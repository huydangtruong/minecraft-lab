# P-020 — Life Steal Blade upgrade, Violet Guardian golems

> **Tóm tắt cho Huy (VN):** Huyết Kiếm sát thương khởi điểm +3, gây chảy máu và đánh văng (2 → 6 block). Chiêu thứ 10 **Violet Guardian**: triệu hồi Iron Golem màu tím trung thành như chó (không đánh chủ, tự đánh quái), mạnh và đông dần theo level.

- **Status:** in-dev · **Decision:** D60 · **Branch:** `feat/P-020-blade-golems` (dev works on `feat/P-020-dev`, merges back into it)

## Life Steal Blade (changes)
- `bladeDamage(level)`: base **8** at level 1 (was 5), still +0.5 per level (level 20 = 17.5).
- Each hit also applies **bleeding**: a new MobEffect `permahearts:bleeding` (harmful, dark red), 1 damage per second for 4 s, with red dust particles; refreshed by new hits. Undead mobs bleed too (simple rule).
- Each hit knocks the target back horizontally away from the attacker: about **2 blocks at level 1, up to 6 blocks at level 20** (`bladeKnockback(level)` = linear(level, 2, 6) blocks; convert to a velocity so a target on flat ground slides about that far; check in a game test with a tolerance).

## Violet Guardian (new technique `GOLEM`, id `golem`, cooldown 180 s, instant cast)
- Summons iron golems next to the caster: **1** at levels 1–9, **2** at 10–14, **3** at 15–19, **4** at 20 (`golemCount(level)`).
- Health and damage are a multiple of a vanilla iron golem's (`golemPower(level)` = linear(level, 0.5, 2.0)): half at level 1, double at level 20 (100 health and its attack damage are the vanilla base values, check with javap).
- They last **90 s** (`golemSeconds` = 90), then vanish with a puff. Recasting removes the old ones first. They also vanish when the owner dies or leaves, and are never saved (not persistent, discarded on chunk/world unload is fine).
- Behaviour like a tamed wolf: own entity type (subclass of `IronGolem` or a custom `PathfinderMob` reusing the iron golem model) with an owner UUID; goals: follow the owner (teleport near the owner when more than ~16 blocks away), attack monsters (`Enemy`) near the owner, defend the owner (attack what hurt the owner / what the owner attacks, except players), never attack players, never retaliate against the owner (the owner's hits do nothing to it, or do damage but are never retaliated: choose "no damage from the owner").
- Look: the vanilla iron golem model drawn with a **violet tint** at render time (a renderer that multiplies the colour; do not copy or edit Minecraft's texture files). Name tag optional: "<owner>'s Guardian".
- Original pixel-art icon `golem` (and focus item, model, item definition, lang "Violet Guardian Focus") via `tools/make_technique_sprites.py`; description and particles like the other techniques; stat lines (Guardians, Health, Damage, Duration, Cooldown).

## Rules for this dev
- Add the numbers to `technique/TechniqueRules.java` + `Technique.java` + JUnit (you may edit these for this plan); regenerate nothing else by hand.
- Tests: game tests (blade: 8 damage at level 1 and knockback distance, bleeding applied; golem: count per level, health/damage multiplier, ignores the owner, attacks a zombie, vanishes after its time); one client screenshot of the violet guardians and one of a blade hit. Run `./gradlew :permahearts:build` and `./gradlew :permahearts:runClientGameTest` **once at the end** (and only the failing class while fixing).
- Docs: one line per change in `docs/workflow/playtest.md` (Vietnamese); API facts in `docs/system/02-findings.md`.
