# P-021 — Atk speed display, guardian kill credit, Warrior Angel

> Tóm tắt (VN): bảng chỉ số hiện phần tu luyện của tốc đánh bằng %; golem tím giết quái thì tính cho chủ; chiêu thứ 11 **Warrior Angel**: gọi thiên thần (dáng Vex, màu vàng trắng) bay xuyên tường đánh quái.
> Decisions: D61, D62, D63. Branch: dev on `feat/P-021-dev`, merges into `feat/P-021-warrior-angel`.

- **Status:** ready
- **Dev:** one Sonnet subagent

## 1. Atk speed line on the Stats panel (D61)

Huy read "+0.16" from Fury next to the sword's "−2.4" and expected a minus. The number is right, since Fury is `ADD_MULTIPLIED_BASE` +10% per bottle applied after the sword's −2.4, so 1.6 × 10% = 0.16. It is still confusing.

- Keep the **total** as hits per second, e.g. `1.76`.
- Show the **gear** part as today, e.g. `−2.4`.
- Show the **cultivation** part of `attack_speed` as a **percent**, e.g. `+10%` per Fury level: `ElixirStat.FURY.bonus(level) * 100`, as done for Food heal in `StatsPanel`. Do the same for any other `ADD_MULTIPLIED_BASE` elixir line that the panel shows as an absolute value, if one exists. Speed and Mining are already %.
- Files: `stats/StatsSync.java` (what is sent), `client/StatsPanel.java` (how it is drawn). Update the stats client test screenshot.

## 2. Violet Guardian kills count for the owner (D62)

Today a mob killed by a guardian has no player kill credit. So it does not count for:
- Crimson Master kill quests (`Wanderer.onKill`);
- the +1 heart per 100 hostile kills;
- HeartXP.

A tamed wolf does count, because vanilla gives the kill to its owner.

- Fix: when a `VioletGuardian` hurts a mob, give the owner the vanilla player kill credit on the target, so that `getKillCredit()` returns the owner, exactly like a tamed wolf. Find the 26.3 name with javap (something like `LivingEntity#setLastHurtByPlayer`) and add it to `02-findings.md`.
- Only when the owner is online and in the same level.
- Game test: a guardian kills a zombie → the owner's kill-quest line or heart kill progress goes up by 1.
- The same credit applies to Warrior Angels (part 3).

## 3. Warrior Angel — technique 11 (D63)

New `Technique.ANGEL`, id `angel`, display name **"Warrior Angel"**. Instant cast, not charged, **cooldown 120 s**. Summons angels that fight for the caster, built like the Violet Guardian (`technique/VioletGuardian.java`): reuse its owner, target, lifetime and despawn logic. Prefer a small shared helper over a copy where it stays simple.

- **Entity:** `WarriorAngel extends Vex`. Vex already flies and passes through walls (`noPhysics`).
  - Entity type `permahearts:warrior_angel`, `noSave`, no loot, no XP.
  - Not persistent. Vanishes with a puff when the time runs out, when the owner dies or leaves, or on recast.
  - Vanilla Vex has its own limited-life timer: disable it or set it to the angel's lifetime.
- **AI:**
  - Follows the owner (flying, stays within ~6 blocks when idle).
  - Attacks hostile mobs (`Enemy`) near the owner, and whatever attacks or is attacked by the owner, except players.
  - **Never attacks players or the owner.** Owner hits do no damage to it.
  - Replace Vex's vanilla goals: it must not target players or follow an Evoker owner.
- **Numbers** (pure Java in `TechniqueRules`, JUnit-tested):
  - `angelCount(level)`: 2 at Lv 1–4, 3 at Lv 5–9, 4 at Lv 10–14, 5 at Lv 15–19, 6 at Lv 20.
  - `angelHealthFactor(level) = linear(level, 1.0, 2.0)` × vanilla Vex max health. Read the vanilla value from `Vex.createAttributes()`.
  - `angelDamageFactor(level) = linear(level, 0.5, 2.0)` × vanilla Vex attack damage.
  - `ANGEL_SECONDS = 60`.
  - Stat lines on the detail page: Angels, Health, Damage, Lasts.
- **Look:**
  - Vex model and animations (`VexModel`, renderer subclass of `VexRenderer`).
  - Its texture is an **original** 64×64 (or whatever size the Vex UV layout uses) **gold and white** texture, drawn by a new script `tools/make_angel_texture.py` in the Vex model's UV layout: white robe/body, gold wings, gold trim and halo-like head band, soft highlights.
  - Do **not** copy or recolour Mojang's vex texture file; only follow the UV layout.
  - Keep the Vex charging pose. No sword item in hand is needed; if the renderer draws the held item, give it nothing or an iron sword.
- **Icon:** original 16×16 in `tools/make_technique_sprites.py`: a white angel wing with a gold halo, plus the `focus_angel` item copy. Add `angel` to the item list there.
- **Wiring:** everything the GOLEM technique needed:
  - exhaustive switches (`TechniqueCasts`, `FocusParticles`, `TechniquesTab.description`, …);
  - lang `en_us.json`;
  - entity renderer registration;
  - description: "Summons warrior angels that fly through walls and fight for you."
  - Regenerate `docs/mods/permahearts/techniques-levels.md` if a generator exists.
- **Tests:**
  - JUnit for the numbers.
  - A game test: cast at Lv 1 → 2 angels with Vex health and half Vex damage; at Lv 20 → 6 with double both; owner hit does no damage; an angel kills a zombie and the owner gets credit; angels vanish after the lifetime (use a test hook as for the guardian).
  - A client test screenshot of angels next to the player (third person) to check the colours.

## Done when

- `./gradlew :permahearts:build` is green.
- The client tests are green, and the dev has looked at the angel and stats screenshots itself.
- `playtest.md` (Vietnamese) has steps for the three parts. `02-findings.md` has the new API names.
- `feat/P-021-dev` is merged into `feat/P-021-warrior-angel`.
- Report in ≤10 lines.
