# P-024 — Illusion technique (clones of the caster)

> Tóm tắt (VN): chiêu thứ 13 **Illusion**: tạo phân thân giống hệt bạn (skin, tên, đồ), đánh quái và đánh kẻ bạn đánh / kẻ đánh bạn (kể cả người chơi, kể cả khi tắt PvP). Sát thương và máu 20% → 40% của bản thật; 1 / 2 / 3 phân thân ở Lv 1 / 10 / 20; tồn tại 20 → 40 giây; hồi 100 giây.
> Decision: D71. Branch: dev on `feat/P-024-dev`, merges into `feat/P-024-illusion`.

- **Status:** merged
- **Dev:** one Sonnet subagent

## Numbers (pure Java in `TechniqueRules`, JUnit-tested)

- `Technique.ILLUSION("illusion", "Illusion", 100, false)`: instant cast, cooldown **100 s**.
- `illusionCount(level)`: 1 at Lv 1–9, 2 at Lv 10–19, 3 at Lv 20.
- `illusionFactor(level) = linear(level, 0.20, 0.40)`: the share of the owner's damage and health.
- `illusionSeconds(level) = linear(level, 20, 40)`.
- Stat lines on the detail page: Clones, Damage (%), Health (%), Lasts.

## The clone entity

Entity type `permahearts:illusion`. Model it on `WarriorAngel` / `VioletGuardian`:
- Reuse `OwnedSummon` and the owner, lifetime and despawn logic.
- `noSave`, no loot, no XP, not persistent.
- Vanishes with a smoke puff when its time runs out, the owner dies or leaves, or the owner recasts (a recast replaces the old clones).

**Look: indistinguishable from the owner at a glance.**
- **Base:** a `PathfinderMob` (it needs AI) drawn with the player model and the **owner's skin**. Sync the owner UUID and get the skin from the client's player info.
  - Minecraft 26.3 has `Avatar`, `Mannequin`, `ClientMannequin` and `AvatarRenderer`. Check with javap what can be reused for the model and the skin.
  - `WandererRenderer` is a player-shaped renderer already in this mod.
  - Slim and wide arm models must match the owner's skin.
- **Name tag:** the owner's name above the head, always visible like a real player's.
- **Gear:**
  - Copy the owner's armour, main hand and off hand at the cast, then refresh about every second, so a weapon swap shows on the clones too.
  - The copies never drop (drop chance 0).
  - The copies are **for looks only**: their attribute modifiers must not add to the clone's stats (see Stats).
- **Sounds:** player hurt sound when hit. On death, a smoke puff only, no death message.

**On cast:**
- Spawn the clones around the owner with a smoke puff.
- Then **shuffle positions**: the owner and the clones swap places at random, within 2–3 blocks, so a watcher loses track of the real one. Keep everyone on safe standing spots (reuse `SpawnSpots`).

**Stats** (recomputed when spawned):
- Max health = factor × the owner's max health, bonus hearts included.
- Melee damage = factor × the owner's current `ATTACK_DAMAGE` value (weapon, Strength elixir and effects already included). Do it in the clone's hurt-target code, so the copied weapon's own modifier does not add on top.
- Armour and toughness = factor × the owner's values.
- Movement:
  - Walking and sprinting at a player's pace (about 4.3 and 5.6 blocks/s).
  - Can jump.
  - Shows sprint particles when sprinting, like a player.

## Who the clones attack (D71)

Each owner has one **target list** shared by all their clones. It starts with the hostile mobs (`Enemy`) near the owner, creepers included (Huy: clones do attack creepers). It **grows**:

- **Whoever hits the owner:** any player or mob that attacks the owner, melee or projectile, is added.
- **Whoever the owner hits:** any entity the owner attacks is added, players included.
- **Both rules work even with PvP off and even when the hit did no damage.** A hit blocked by PvP-off still counts as an attack.
  - Melee: Fabric `AttackEntityCallback`, in both directions.
  - Projectiles and other damage: a damage hook that runs before the PvP check (for example `ServerLivingEntityEvents.ALLOW_DAMAGE`). Check with javap where 26.3 blocks PvP damage.
- A clone's own hit on a player must land even with PvP off: the damage source is the clone, not a player. Verify this.
- **The list is never touched by:**
  - the owner, the owner's other clones, Violet Guardians and Warrior Angels;
  - the owner's tamed animals.
- The list is cleared when the clones end. Removed or dead entities drop off it.
- The owner's hits do no damage to their own clones.

## AI: hard to tell from a real player

A "mood" that changes every 2–5 s at random, per clone, so the clones do not move in sync:

- **Chase:**
  - Sprint to the target.
  - Jump-hit like a player crit (falling hit), then circle-strafe around the target.
  - Hit at the player's attack cooldown rhythm, not every tick.
- **Back off:**
  - Sometimes, and more often when below 40% health, run away 5–8 blocks.
  - Then turn around and attack again.
- **Ranged:** if holding a bow or crossbow, keep 6–12 blocks away and shoot. Arrow damage × factor.
- **Idle:** no target, so follow the owner and stay within ~6 blocks.
  - Wander a little and look around.
  - Sometimes jump, sometimes sneak for a moment.
  - Sometimes look at the nearest player.

Keep each piece small: vanilla goals where they fit (look at `DefendOwnerGoal`, which is `TargetGoal`-based), plus one small mood controller.

## Kills

- **Mobs:** a mob killed by a clone counts for the owner, as in D62 (`OwnedSummon.giveKillCredit`): quests, hearts, HeartXP.
- **Players:** a player killed by a clone does **not** count as a Death Match kill (manager proposal). The owner must do it.

## Look of the technique

- **Icon:** an original 16×16 in `tools/make_technique_sprites.py`, two overlapping player silhouettes, the back one faded. Add a `focus_illusion` item copy.
- **Particles:** smoke and a few white sparkles.
- **Description:** "Creates illusions of yourself that fight for you. Who attacks you, or whom you attack, becomes their target."
- **Wiring:** everything the ANGEL technique needed:
  - exhaustive switches (`TechniqueCasts`, `FocusParticles`, `TechniquesTab.description`, …);
  - `en_us.json`;
  - renderer registration.

## Tests

- **JUnit:** the numbers.
- **Game tests:**
  - **Spawning and stats:**
    - Lv 1 gives 1 clone with 20% of the owner's max health.
    - Lv 10 gives 2 clones; Lv 20 gives 3 clones with 40%.
    - A clone's hit does 40% of the owner's attack damage at Lv 20.
    - The owner's hit does no damage to their clone.
  - **Targeting:** with PvP **off**:
    - a second mock player attacks the owner → the clones target it, and a clone's hit damages it;
    - the owner attacks a second player → the clones target that one.
  - **Kills and lifetime:**
    - A clone kills a zombie → the owner gets the credit.
    - The clones vanish after their lifetime (use a test hook as for the angel).
- **Client test:** a screenshot of the clones next to the player in third person, wearing gear. Check:
  - same skin;
  - same armour and weapon;
  - the name tag.

  Look at it yourself.

## Done when

- `./gradlew :permahearts:build` is green, and the client test is green with the screenshot checked.
- `docs/workflow/playtest.md` (Vietnamese) has Illusion steps.
- `docs/system/02-findings.md` has the new API names.
- `feat/P-024-dev` is merged into `feat/P-024-illusion`. Do **not** push.
- Report in ≤10 lines.
