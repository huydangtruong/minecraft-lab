# P-031 — No "+" buttons, "i" on the right, /testmode

> **Tóm tắt cho Huy (VN, ≤ 3 dòng):** chọn ô chiêu thì không còn nút "+" ở các dòng chiêu; nút "i" dời sát phải.
> Lệnh /testmode full: đủ bộ netherite phù phép, 50 tim, mỗi linh dược 10 lần. /testmode reset: trả về như cũ.

- **Status:** merged
- **Mod / decision:** `permahearts`, D91, D92 · **Branch:** `feat/P-031-ui-testmode` · **Depends on:** none

## Part 1: Techniques tab (D91)

File: client `TechniquesTab.java`, test `TechniqueClientTest.java`.

- Slot-first mode: **remove the "+" buttons** from the technique rows. Clicking a row already assigns (D83); keep that and its hover highlight.
- Move the **"i" button to the right edge of the row**, exactly where the "+" button was. Nothing else changes (level bar, technique-first flow, Cancel).
- Update `hotspots(...)` and the client test (no "+" click left; slot-first assign by clicking the row). Screenshot `104-technique-rows` with a slot selected; look at it.

## Part 2: /testmode (D92)

New `TestModeCommand.java` (registered like `HeartsCommand`), permission `LEVEL_GAMEMASTERS`. Pure parts (the gear list, the enchantments, the target numbers) in a class without Minecraft imports where it makes sense.

**`/testmode full`:**
1. If the player has no saved snapshot yet, save one in a persistent player attachment (survives logout and death, `copyOnDeath`): the hearts data, the elixir data (whatever attachment `ElixirItem` / `CultivationData` / `StatApplier` use), and the full inventory including armor and offhand (store as item stack codec list). A second `full` keeps the first snapshot.
2. Equip netherite helmet, chestplate, leggings, boots (move what was worn into the inventory, or drop it if full); give netherite sword, pickaxe, axe, shovel, hoe.
3. Enchantments, each at its max level (check 26.3 names with javap; add to 02-findings.md):
   - every armor piece: Protection IV + Unbreaking III;
   - sword: Sharpness V + Fire Aspect II;
   - pickaxe: Efficiency V + Fortune III;
   - axe: Sharpness V + Efficiency V;
   - shovel: Efficiency V + Unbreaking III;
   - hoe: Efficiency V + Fortune III.
4. Hearts to **50** (`HeartRules.withHearts`), then apply health with a refill.
5. Each elixir counts as drunk **10 times** (add 10 to each stat's count through the same rule the elixir item uses, respecting its cap if it has one), then re-apply stats.
6. Message "Test mode on. /testmode reset to go back."

**`/testmode reset`:** restore the snapshot exactly (hearts, elixirs, the whole inventory and armor, replacing what is there now), re-apply health and stats, delete the snapshot. Without a snapshot: "Test mode is not on." Message "Test mode off."

## Tests

- JUnit for any pure part (enchant table, target numbers).
- Server game tests: `full` gives 4 worn netherite pieces with their 2 enchantments, 5 tools, max health 100, every elixir +10; `reset` restores the hearts, elixirs and inventory exactly; `full` twice then `reset` still gives the original state; a non-op cannot run it.

## Verify

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
./gradlew :permahearts:build
./gradlew :permahearts:runClientGameTest
```

## Docs

- `docs/workflow/playtest.md`: the slot step without "+", and a /testmode step (VN).
- `docs/mods/permahearts/spec.md`: a /testmode line in the commands (VN).
