# P-029 — Thunder Strike

> **Tóm tắt cho Huy (VN, ≤ 3 dòng):** chiêu mới Thunder Strike (icon mây xám, sét vàng). Nhắm tâm màn hình vào chỗ muốn đánh, bấm chuột trái hoặc phải: vùng đó sáng lên 1 giây rồi sét đánh xuống, nổ cỡ creeper.
> Lv 1 gây 14 sát thương, mỗi level +2 (Lv 20 là 52). Vùng phá tăng 5% mỗi level. Hồi chiêu 24 s.

- **Status:** ready (start after P-028 is merged: same files)
- **Mod / decision:** `permahearts`, D86 · **Branch:** `feat/P-029-thunder-strike` · **Depends on:** P-028

## Goal

The 15th technique, `Technique.THUNDER("thunder", "Thunder Strike", 24, false)`. Follow how WARP and ILLUSION were added; these files list every technique:
- `Technique.java`, `TechniqueRules.java`, `TechniqueCasts.java`, `TechniqueNet.java`;
- client: `ClientSkills.java`, `TechniquesTab.java` (description), `FocusParticles.java`;
- `en_us.json`;
- `items/focus_thunder.json`, `models/item/focus_thunder.json`;
- the sprite `textures/gui/sprites/technique/thunder.png` made by `tools/make_technique_sprites.py`.

## Numbers (pure Java in `TechniqueRules`, JUnit)

- `thunderDamage(level) = 14 + 2 * (level - 1)` → 14 at Lv 1, 52 at Lv 20 (damage points, 2 = one heart).
- `thunderPower(level) = 3.0 * (1 + 0.05 * (level - 1))` → creeper power 3 at Lv 1, 5.85 at Lv 20 (+5% of the Lv 1 size per level, linear).
- `THUNDER_RANGE = 128` blocks, `THUNDER_WARN_TICKS = 20` (1 s).
- Cooldown 24 s at every level.
- Stat lines: Damage, Blast size (as % of a creeper, e.g. "100%" → "195%"), Range, Cooldown.

## Controls

- In technique mode, with Thunder Strike in the selected slot (or the focus item in hand), **a left or right click** casts it. It is not charged. Reuse the left-click path WARP already has in `ClientSkills`.
- Aim: the client sends the cast. The server ray-casts from the caster's eyes along the look direction up to 128 blocks, against blocks and entities (not the caster or the caster's own summons). The hit point is the target.
- If nothing is hit within 128 blocks, the strike lands on the first solid ground straight below the farthest point of the ray, or on that point itself over the void.
- The cooldown starts at the cast, not at the strike.

## The strike

1. **Warning, 1 s.**
   - A ring of particles on the ground around the target point, radius = the blast radius.
   - Use yellow/white, e.g. `ParticleTypes.ELECTRIC_SPARK` plus a few `END_ROD`, refreshed every few ticks, plus a faint fill.
   - Seen by every player nearby. It stays on that point; it does not follow a moving mob.
   - A quiet crackle sound at the start.
2. **Strike after 20 ticks**, scheduled on the server (survives the caster dying or logging out in between; skip if the chunk unloaded):
   - A lightning bolt that sets no fire. It **transforms mobs inside the blast like real lightning does** (pig → zombified piglin, villager → witch, creeper → charged, and so on), before the damage is dealt.
   - Thunder sound.
   - An **explosion** at the point with power `thunderPower(level)`. It breaks blocks and drops them like a creeper explosion (respect the `mobGriefing` game rule the way a creeper does). No fire.
   - **Damage** comes from the technique, not from the explosion's own damage (set the explosion to deal no entity damage):
     - every living entity within the blast radius takes `thunderDamage(level)` at the centre, falling off linearly to half at the edge;
     - the source is the caster, so kills count as theirs.
   - **Not hurt:** the caster, the caster's own summons (clones, golems, angels…), and other players when the existing PvP / Death Match checks say no. Use the same helper the other area techniques use.
   - Knockback like an explosion, for the entities it damages.

## Icon

- The sprite in `tools/make_technique_sprites.py`: a grey cloud with a yellow lightning bolt coming out of it, same size and style as the other technique icons. Original art.
- Copy it to the focus item texture, the way the others are made.

## Tests

- **JUnit (`TechniqueRulesTest` or a new `ThunderRulesTest`):**
  - damage 14, 32 and 52 at Lv 1, 10 and 20;
  - power 3.0 and 5.85;
  - cooldown 24;
  - the stat lines.
- **Server game tests (`ThunderGameTest`):**
  - a cast at a zombie 10 blocks away: nothing happens for 19 ticks, then the zombie takes damage and blocks near the point are gone;
  - the caster standing at the edge of the blast is not hurt;
  - with `mobGriefing` false, no block breaks;
  - aiming at the open sky strikes the ground below the 128-block point;
  - a pig in the blast becomes a zombified piglin;
  - a level-20 strike breaks a larger crater than a level-1 one.
- **Client test:** cast with a real left click (`pressMouse` with `MOUSE_BUTTON_LEFT`). Take screenshot `99-thunder-warning` during the warning and `100-thunder-strike` after it, and look at them.

## Verify

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
./gradlew :permahearts:build
./gradlew :permahearts:runClientGameTest
```

## Docs

- `docs/mods/permahearts/techniques-levels.md`: a Thunder Strike row.
- `docs/workflow/playtest.md`: a Thunder Strike step (VN).
- `02-findings.md`: the lightning and explosion API names you checked.

## Done when

- [ ] The aim, the 1 s warning, and the strike work with a left or right click.
- [ ] The numbers match D86.
- [ ] The caster and their summons are never hurt.
- [ ] The icon is in the tab.
- [ ] The build and client tests are green, and the screenshots have been looked at.

## Risks / ask first

- If the explosion API makes "no entity damage" hard, ask before letting the vanilla explosion damage stand.
- How a new technique becomes obtainable (focus item drop, quest, etc.) follows what the last technique (Cosmic Warping) did. Copy that, do not invent a new way.
