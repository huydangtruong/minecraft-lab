# P-011 — Techniques (cultivation skills)

> **Tóm tắt cho Huy (VN, ≤ 3 dòng):** Chiêu thức: học/nâng bằng XP (tới Lv 20), mở tối đa 4 ô bằng ngọc lục bảo, gắn chiêu vào ô ở tab Techniques. Góc dưới phải có dãy ô ✕ + chiêu; bấm R để chuyển, giữ R hiện bảng tròn; chuột để dùng. 5 chiêu đầu: Lửa, Băng, Hồi máu, Xuyên tường, Hất tung. Làm 3 phần: A dữ liệu + bảng, B thanh chiêu + cách dùng, C các chiêu.

- **Status:** in-dev (part A)
- **Mod / decision:** `permahearts` (Immortal Cultivation), D43, D44, D45 · **Branch:** `feat/P-011-techniques-a|b|c` · **Depends on:** none (P-010 Death Match is still unmerged; it will ship after 0.9.0, as 0.10.0 or later)
- **Idea (Huy's words):** [techniques-idea.md](../mods/permahearts/techniques-idea.md)

## Goal

Every player has the same list of techniques. Experience levels learn and upgrade them, with costs rising per level; resetting refunds half. Emeralds unlock up to 4 technique slots. A skill bar right of the hotbar holds ✕ + the slotted techniques; a key cycles through them, and mouse buttons cast the selected one. The first three techniques are Fire, Ice and Healing.

## Answers (2026-11-14) — these override the tables below where they differ

- Max level **20**; level 10 numbers below are kept; levels 11–20 add **half** the per-level step.
- XP cost of the upgrade to level L: `round(5.8 + 0.4·L)` levels → 80 levels in total to level 10, 200 to level 20 (D43).
- Fire and Ice cooldowns doubled: **16 s** and **24 s**. Fire keeps the normal fire colour at level 6+ (only the burn gets stronger).
- Healing heals **every living entity** in the circle (players, pets, animals, and enemies standing in it); the circle stays where it was cast.
- Key **R**: tap = cycle; hold = radial wheel (hover, release to pick), like Essential's emote wheel (Essential also uses R; one of the two must be rebound).
- Left click = throw at once with full charge.
- Techniques always affect other players like mobs, **even with PvP off** (damage must not be blocked by the server's PvP setting).
- Two more techniques in the first version: **Phase Walk** (10 s: pass through every block at or above the feet level when cast, so the player does not fall) and **Repulse** (knock every living entity around away with a little damage; defensive).

## Proposed rules (first draft; see Answers above)

| Rule | Proposal |
|---|---|
| Max technique level | 10 |
| XP cost to reach level L | `1 + 2L` levels: 3, 5, 7 … 21 (all 10 levels = 120 levels). Paid like enchanting: the player must have the levels; the **points** actually removed are recorded per technique |
| Reset | Technique back to 0, refund **half of the recorded points** (`giveExperiencePoints`); refused while that technique is on cooldown |
| Slot unlock | Emeralds deposited toward the next slot: 64, +256, +512, +1024 (1,856 total). Deposit is partial and kept; the slot opens the moment the amount is reached |
| Assign | Select a slot (gold outline) → ✓ on a technique row puts it there. Refused while the technique **currently in that slot** is on cooldown. A technique sits in at most one slot (assigning moves it). Level 0 techniques cannot be slotted |
| Cooldowns | Per technique, in game ticks, saved, kept through death and relog |
| Skill key | Default **R** (Tab already opens the player list in multiplayer and cannot be shared cleanly); rebindable in Controls → "Immortal Cultivation". Cycles ✕ → slot 1 → … → last filled slot → ✕, skipping empty slots |
| While a technique is selected | The held item is not shown in first person (an empty, glowing hand instead); right/left click cast the technique instead of using/attacking with the item. Scrolling the hotbar or pressing 1–9 returns to ✕ |
| Without the mod / switch off | Techniques are inactive when the world switch is off (like elixirs); clients need the mod (already required) |

### Techniques (numbers are first proposals for balancing)

| Technique (in-game name) | Cast | Level 1 | Per level / level 10 | Cooldown |
|---|---|---|---|---|
| Fire — `Flame Burst` | Hold right click to charge (1 s to full, like a bow), release to throw a fire orb; left click throws an uncharged one | Range at full charge 16 blocks; impact 4 damage; fire field radius 2 for 4 s, sets entities on fire (vanilla burn, 1/s) | +2 blocks, +1 impact, field +0.1 radius and +0.2 s; **from level 6** the burn is stronger (2/s, blue soul-fire flames). Level 10: 34 blocks, 13 impact, radius 2.9, 5.8 s | 8 s |
| Ice — `Frost Orb` | Same as Fire | On impact: 2 damage, area radius 3 for 6 s; **Frostbite** inside: −20% move speed, −15% attack speed (refreshed while inside, lasts 3 s after leaving) | +0.2 radius, −4% speed, −4% attack speed. Level 10: radius 4.8, −56% / −51% | 12 s |
| Healing — `Healing Light` | Right or left click: a glowing circle (radius 4, the 8×8 area) at the caster's feet for 10 s | Heals players inside 20 health (10 hearts) spread over 10 s | +4 health per level. Level 10: 56 health (28 hearts) / 10 s | 60 s |

Fire field and ice area never place blocks (no griefing, no fire spreading). Projectiles hurt mobs and (with PvP on) other players, never the caster.

## Scope

- In: everything above, three parts, one branch and merge each.
- Out (do not do): more techniques (Huy will describe them), mana or other resources, technique items or books, third-person cast animations for other players (they see the orbs, fields and particles), balancing beyond the table.

## Parts

### A — data, rules, Techniques tab (no casting yet)

| File | Where | Change |
|---|---|---|
| `TechniqueRules.java` | `main/.../technique/` | Pure Java: costs, refund, slot thresholds, assign/reset checks, cycle order, per-level numbers. JUnit-tested |
| `Technique.java` | same | Enum FIRE, ICE, HEALING: id, name, max level, cooldown, numbers per level |
| `TechniqueData.java` | same | Record: levels, spent points, slots unlocked, emeralds toward next slot, slotted techniques[4], cooldown end ticks. Codec + StreamCodec |
| `HeartsAttachment` (or new `TechniqueAttachment`) | `main/...` | Persistent, copy on death, synced to the owner (like `StatsSync`) |
| `TechniqueNet.java` | same | C2S payloads: upgrade, reset, assign, deposit emeralds. Server validates every request |
| `StatsPanel.java` | `client/...` | Techniques tab: slot row + emerald box (click it with emeralds on the cursor to deposit the stack, like putting items in the Crimson Master's box) + vertical list (icon, name, Lv, ✓ when a slot is selected) + detail page (description, current → next numbers, Upgrade "N levels", Reset "+M XP", Back). Scrolling when the list outgrows the panel |
| sprites | `tools/make_stats_sprites.py` | Technique icons, slot frames (gold outline), ✕ icon, lock |

### B — skill bar and casting input

| File | Change |
|---|---|
| `SkillBarHud.java` (client) | Bottom-right, mirroring the HeartXP bar: ✕ + unlocked slots, selection frame like the hotbar's, cooldown sweep + seconds, charge bar while holding |
| `TechniqueKeys.java` (client) | KeyMapping "Cycle technique" (default R) |
| client mixin on `Minecraft` | While a technique is selected: cancel `startUseItem` / `startAttack` / `continueAttack`, track the use key for charging, send C2S `Cast(slot, charge)` on release/click |
| client mixin on the first-person item renderer | Hide the held item while a technique is selected |
| `TechniqueNet` | `Cast` payload: server checks slot, level > 0, cooldown, alive, switch on; starts cooldown; dispatches to the technique |

### C — the three techniques

| File | Change |
|---|---|
| `FireOrb`, `FrostOrb` entities | Thrown projectiles (item-sprite renderer: fire charge / snowball look), speed from charge, impact → field |
| `TechniqueField` entity | Area that ticks: fire (ignite + damage), ice (Frostbite), healing (heal players); particles; no blocks placed |
| `Frostbite` MobEffect | Attribute modifiers for move and attack speed scaled by level (amplifier) |
| sounds/particles | Vanilla sounds (blaze shoot, glass break, beacon/amethyst) and particles (flame, soul fire flame, snowflake, end rod) |

## Tests

- JUnit `TechniqueRulesTest`: cost table, total 120 levels, refund = half of spent points, slot thresholds 64/320/832/1856, assign refused on cooldown, cycle skips empty slots, numbers at level 1/6/10.
- Game tests: upgrade spends levels and refuses without enough XP; reset refunds half; emerald deposit opens slots at the thresholds and keeps partial deposits; assign respects cooldown; cast puts the technique on cooldown; fire orb sets a zombie on fire; ice applies Frostbite with the right speed reduction; healing circle heals a hurt player by the right amount over 10 s; nothing works with the switch off.
- Client test screenshots: Techniques tab (slots, list, selected slot, detail page), skill bar with a technique selected and on cooldown, a fire field, an ice area, the healing circle.

## Verify

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
./gradlew :permahearts:build
./gradlew :permahearts:runClientGameTest
```

## Docs

Decisions D43+ (one per answered question group), spec section "Chiêu thức", playtest steps, findings for any new API (key mappings, input mixins, projectile and effect registration on 26.3).

## Done when

- [ ] Huy answered the questions below, decisions recorded
- [ ] A, B and C merged, each with green tests and screenshots
- [ ] Two players tested together (cast on each other, heal each other)

## Phase Walk and Repulse (first numbers)

| Technique | Level 1 | Per level (half after 10) | Cooldown |
|---|---|---|---|
| Phase Walk | 10 s; blocks with bottom Y ≥ floor(feet Y at cast) have no collision for the caster (client and server movement checks), no suffocation | +0.5 s (Lv 10: 14.5 s, Lv 20: 17 s) | 90 s |
| Repulse | Radius 4; push 1.0 (plus a little upward); 2 damage | +0.2 radius, +0.15 push, +0.5 damage (Lv 10: 5.8, 2.35, 6.5) | 20 s |

Phase Walk ending inside blocks: the player is moved up to the first free space (never left suffocating). The camera inside a block shows the block overlay like vanilla; may be hidden while phasing.

## Questions for Huy (answered 2026-11-14, see Answers)

1. **Key**: Tab also opens the player list in multiplayer. Use **R** by default (rebindable), or really Tab?
2. **XP cost**: max level 10 and `1 + 2L` levels per upgrade (3, 5 … 21; 120 levels for level 10)? Or cheaper or more expensive?
3. **Healing**: heals only players (also other players / teammates) or the caster only? Mobs and tamed pets: no? Circle stays where it was cast, or follows the caster?
4. **Fire**: a magic fire field that burns entities but never sets blocks on fire (recommended), or real fire blocks?
5. **Left click**: also casts (uncharged throw), or only right click casts and left click does nothing while a technique is selected?
6. **Death Match (P-010)** and PvP: Fire and Ice hurt other players when PvP is on. OK?
