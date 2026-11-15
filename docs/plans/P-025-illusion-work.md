# P-025 — Illusion work modes (farm, fish, chop, shear, milk, breed)

> Tóm tắt (VN): món đồ trên tay lúc dùng chiêu quyết định việc của phân thân: cuốc làm ruộng, cần câu câu cá, rìu chặt cây, kéo cắt lông cừu, xô vắt sữa, lúa mì / cà rốt / khoai tây cho thú ăn. Thành quả bỏ vào rương gần nhất. Có đánh nhau thì đánh bằng món đang cầm rồi quay lại làm. Tồn tại 30 → 50 giây, hồi 110 giây.
> Decision: D73 (builds on D71, P-024). Branch: dev on `feat/P-025-dev`, merges into `feat/P-025-illusion-work`.

- **Status:** ready
- **Dev:** one Sonnet subagent

## Numbers (pure Java in `TechniqueRules`, JUnit-tested)

- **Lifetime and cooldown:**
  - `illusionSeconds(level) = linear(level, 30, 50)`. It was 20 → 40.
  - `Technique.ILLUSION` cooldown is **110 s**. It was 100.
- **Work area:**
  - `illusionWorkRadius(level) = linear(level, 8, 16)` blocks around the **cast point**.
  - The clones work there and do not follow the owner.
- **Work speed:** `illusionWorkTicks(level)` is the pause between two actions (one crop, one log, one sheep):
  - 20 ticks at Lv 1, falling linearly to 8 ticks at Lv 20;
  - a block still takes its break time on top.
- **Carry and chest:**
  - Each clone carries at most **9 stacks**.
  - Chest search: the nearest chest or barrel within `illusionWorkRadius + 8` of the cast point.
- **Stat lines:** add "Work area" to the detail page. "Lasts" already exists.

## Job chosen at cast

`IllusionJob` is a pure enum with `fromItemId(String id)`, so it can be JUnit-tested on item ids.

| Owner's main hand at the cast | Job |
|---|---|
| any hoe | `FARM` |
| fishing rod | `FISH` |
| any axe | `CHOP` |
| shears | `SHEAR` |
| bucket (empty) | `MILK` |
| wheat | `BREED` (cows, sheep) |
| carrot, potato | `BREED` (pigs) |
| anything else | `FIGHT` (today's behaviour) |

- **The job is locked at the cast.**
  - The clone's main hand is also locked to that item. The once-a-second gear refresh keeps copying armour and off hand only.
  - Switching the owner's item later changes nothing. A recast replaces the clones; their carried items drop where they stand.
- **No pickaxe or shovel job.** Those items give `FIGHT`.

## Each job

All jobs run inside the work area only. They count as the clone's actions, not the owner's: no XP for the owner (D73), and no use of the owner's items or durability.

- **FARM:**
  - Harvest crops at max age: wheat, carrots, potatoes, beetroot, nether wart. Break each like a player, so it gets the normal drops.
  - Replant the same crop at once, using one seed or item taken from the drops.
  - Then till dirt or grass that is next to farmland and within 4 blocks of water, and plant it from carried seeds. Never till other blocks.
- **FISH:**
  - Walk to a shore block next to open water (at least 2×2 water). Face the water and show a cast-rod arm swing.
  - Wait a random 5–30 s (Lure on the owner's rod at the cast shortens it, as in vanilla).
  - Roll the vanilla fishing loot table (`BuiltInLootTables.FISHING`), with the rod's Luck of the Sea and the clone as the looter. Play the splash and reel sounds.
  - A real `FishingHook` entity is optional: a simple line or particle effect is enough if the hook is hard to drive from a mob. Check with javap.
- **CHOP:**
  - Find a natural tree: a log column touching leaves with `LeavesBlock.PERSISTENT = false`.
  - Break the logs from the bottom up, and the connected logs of the same tree up to 64. Leave the leaves to decay naturally.
  - Replant one sapling of the same wood on the dirt where the trunk stood, if the clone has one or picks one up.
  - Never break a log with no natural leaves attached (houses, built walls).
- **SHEAR:**
  - Walk to an adult sheep that is not sheared, and shear it the vanilla way (`Shearable`). Collect the wool.
  - Wait for the next sheep.
- **MILK:**
  - Each milk bucket needs one empty bucket. Take empty buckets from the chest; the clone starts with none.
  - No chest, or no empty bucket in it: idle near the cows. Never create a bucket from nothing.
  - Put the milk buckets in the chest.
- **BREED:**
  - Feed adult animals that can breed and are not on cooldown: wheat for cows and sheep, carrots or potatoes for pigs.
  - Use the vanilla love mode, so the 5-minute breeding cooldown applies.
  - **Food is never free (Huy):**
    - At the cast, the stack in the owner's main hand is split equally between the owner and the clones. Example: 64 wheat and 1 clone → owner keeps 32, the clone carries 32; with 3 clones, 16 each. The owner keeps any remainder.
    - Take the clones' shares out of the owner's stack. Each share goes in that clone's carry.
    - A clone feeds from the nearby chest first (matching food), and from its own share only when the chest has none.
    - Unused food goes back like any carried item: into the chest, or dropped at the owner's feet.
  - The baby gets the owner as "loved by" or no player at all. Breeding XP orbs are **not** spawned.

**Collecting and storing**
- Pick up the drops of their own work within 3 blocks.
- Go to the chest when the carry is full, when the work area has no more work, or 3 s before the lifetime ends. Put the items in, and keep what does not fit.
- No chest, a full chest, or the clone vanishing early (owner dies or leaves, recast, killed): drop what it carries at the owner's feet if the owner is within the work area, otherwise where the clone stands. Never delete items.

## Fighting while working

Keep the D71 target list as it is: whoever hits the owner, or whom the owner hits, players included, even with PvP off.

- In a work job, also fight hostile mobs that come **within 6 blocks** of the clone or the owner. Ignore farther ones, so the work goes on.
- Fight with the **locked item**. Damage stays at factor × the owner's current attack damage, as in P-024. Keep that rule as it is; do not invent tool damage.
- When the target list is empty again, return to the job.

## Code shape

- `Illusion.java` is already 680 lines. Put the jobs in new files:
  - `technique/illusion/IllusionJob.java`: the pure enum.
  - one goal per job, or one `WorkGoal` with small job classes;
  - a small `IllusionCarry` for the 9 stacks and the chest logic.
- The work goal sits below `FightGoal` in priority. `IdleGoal` is used only by `FIGHT`.
- The carry must be dropped on every removal path (`remove` / `onRemoval`). Reuse the vanish code.

## Tests

- **JUnit:**
  - `IllusionJob.fromItemId` for every row of the table, plus a sword and a pickaxe (FIGHT);
  - the new numbers.
- **Game tests:** one per job, small arenas, AI on, generous `maxTicks`, and `succeedWhen` instead of fixed-tick checks (see `AngelGameTest`):
  - FARM: mature wheat around the cast point with a chest → wheat in the chest, crops replanted;
  - CHOP: a natural tree (non-persistent leaves) → logs in the chest; a log wall with persistent leaves stays untouched;
  - SHEAR: a sheep → wool in the chest;
  - MILK: a cow, a chest with 1 empty bucket → 1 milk bucket in the chest. No bucket → nothing changes;
  - BREED: two cows plus wheat → a baby cow, and no XP orbs. The owner's stack of 64 wheat becomes 32 with 1 clone. Wheat in the chest is used before the clone's share;
  - FISH: water and a chest → at least one item in the chest. Use a test hook to shorten the wait;
  - the job stays locked when the owner's main hand changes after the cast;
  - a working clone attacks a second player who hits the owner (PvP off), then goes back to work;
  - a clone vanishing with items drops them;
  - the owner's XP is unchanged after farming and fishing.
- **Client test:** not needed; no new UI.

## Done when

- `./gradlew :permahearts:build` is green. Run it once. Rerun a known flaky test once and say so.
- `docs/workflow/playtest.md` (Vietnamese) has steps for each job.
- `docs/system/02-findings.md` has the new API names.
- The technique description mentions jobs. Keep it short: "… Holding a tool when you cast sets their work: farm, fish, chop, shear, milk or breed."
- `feat/P-025-dev` is merged into `feat/P-025-illusion-work`. Do **not** push.
- Report in ≤10 lines.
