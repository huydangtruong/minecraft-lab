# P-030 — Crimson Master tuning, emerald reward box

> **Tóm tắt cho Huy (VN, ≤ 3 dòng):** Crimson Master chờ 5 ngày mới tới; đổi tỉ lệ loại nhiệm vụ; thêm thịt lợn 48, phù thủy còn 6, thêm Blaze / Wither Skeleton / Phantom mức 8.
> Rương mới ngọc lục bảo (xanh lá, 6 thẻ, chọn 2) với Totem, bình +16 level, sách Mending / Sharpness V / Protection V.
> Mỗi dòng nhiệm vụ bốc ngẫu nhiên loại rương: bạc 40%, vàng 30%, kim cương 20%, ngọc lục bảo 10%.

- **Status:** ready
- **Mod / decision:** `permahearts`, D87, D88, D89, D90 · **Branch:** `feat/P-030-crimson-tuning` · **Depends on:** none

## Part 1: schedule and quests (D87, D88)

File: `wanderer/QuestGenerator.java` (pure Java, JUnit in its existing test).

- The two "4 days" methods (first visit and the wait after leaving) return **5**. Update the javadoc to D87. `Wanderer.STAY_TICKS` stays 2 days.
- `pickType` weights: COLLECT 27, KILL 27, EXPLORE 15, BLOOD_MOON 15, BOSS 16, DEATH_MATCH 12 (only when offered). Total 100 without Death Match, as today.
- `COLLECT`: add `minecraft:porkchop` 48.
- `KILL`: witch 8 → **6**; add `minecraft:blaze` 8, `minecraft:wither_skeleton` 8, `minecraft:phantom` 8.
- **No two lines on the same mob in one visit**, also across KILL and BOSS (a Blaze kill line and a Blaze boss line). Check how `generate()` claims targets today; extend it if KILL and BOSS ids are claimed separately.
- Check that the kill counter counts these mobs (Phantom and Blaze are `Enemy`; the Wither Skeleton too). If the kill line has a dimension or spawn hint text, add one for Nether mobs if the code has such a place; otherwise nothing.
- Existing saves keep their current quests; new numbers apply to new visits.

## Part 2: random box tier per line (D90)

Files: `reward/RewardTier.java`, `wanderer/Wanderer.java` (line 309, 433), `wanderer/RewardContainer.java` (line 53), the quest line record and its codec.

- Add `EMERALD("emerald", 6, 2)` to `RewardTier`.
- Pure rule `RewardTier.roll(RandomGenerator)`: SILVER 40, GOLD 30, DIAMOND 20, EMERALD 10, the same for every quest type.
- The tier is **rolled when the quest is generated** and stored on the line (codec field, optional). The quest screen preview and the claim both use the stored tier.
- Old saved lines without the field fall back to today's `forLine(type)`, so nothing breaks mid-visit.

## Part 3: emerald box (D89)

Files: `reward/RewardRoll.java`, `reward/RewardOption.java`, `reward/RewardBoxes.java`, `reward/RewardBoxItem.java`, `reward/RewardBoxMenu.java`, client `RewardBoxScreen.java`, `en_us.json`, item model/texture files.

- Item `permahearts:emerald_reward_box`, Rarity EPIC (or the next one up from diamond). **Green** texture, same style as the other three boxes (original art, made the same way the others were). Name "Emerald Reward Box".
- 6 cards, **pick 2**. The screen and menu must support `picks() > 1`: the player clicks 2 different cards, each pick is given; the box is used up after the second pick. Show "Pick 2" / "1 left" text the way the screen shows the current hint. Closing after 1 pick keeps the box with the remaining pick (store picks taken in `RewardBoxData`), or gives a random second card. Choose the one that fits the current code and say which.
- Card roll: same as the others (50% elixir, otherwise item), item cards special with **25%** chance for EMERALD (`SPECIAL_CHANCE` stays 0.2 for GOLD and DIAMOND).
- `EMERALD_SPECIALS`:
  - `minecraft:totem_of_undying` 1;
  - `permahearts:enlightenment_bottle` 1 (new item, below);
  - enchanted book Mending, enchanted book Sharpness V, enchanted book Protection V.
- `RewardOption` needs a way to carry the enchantment: add an optional field (e.g. `enchant` id + level, default empty, codec `optionalFieldOf` so old saved boxes still load). Two books with different enchantments count as different items for the no-duplicate rule.
- Card tooltip/label shows the book's enchantment (vanilla tooltip does it if the stack has `STORED_ENCHANTMENTS`). Check the 26.3 names with javap (`DataComponents.STORED_ENCHANTMENTS`, `ItemEnchantments.Mutable`, `Enchantments.MENDING`, registry lookup) and add them to 02-findings.md.

**Enlightenment bottle (new item):**
- `permahearts:enlightenment_bottle`, "Bottle of Enlightenment", stack 16, Rarity RARE, glint. Texture: a green-gold glowing bottle, original art.
- Use (right click, drink animation like a potion, 32 ticks): gives **+16 experience levels** (`giveExperienceLevels(16)` or the 26.3 name), level-up sound, consumes one (not in creative). Tooltip: "Drink: +16 levels".
- Not craftable; only from the emerald box.

## Tests

- **JUnit:**
  - `pickType` weight totals and porkchop / witch 6 / blaze / wither skeleton / phantom in the lists;
  - visit delay 5;
  - over many seeds, no visit has the same mob in two lines;
  - `RewardTier.roll` over many seeds is near 40/30/20/10; EMERALD has 6 options and 2 picks;
  - `RewardRoll.roll(EMERALD)` gives 6 cards, no duplicate item (books with different enchantments are allowed together), specials come only from `EMERALD_SPECIALS`;
  - `RewardOption` codec round-trip with and without the enchantment, and an old JSON without it.
- **Server game tests:** opening an emerald box and picking 2 cards gives both items and uses up the box; a Mending book card gives a book that carries Mending; drinking the bottle raises the level by 16.
- **Client test (`RewardBoxClientTest`):** open an emerald box, real clicks on 2 cards, screenshot `101-emerald-box` before the picks and `102-emerald-box-picked` after the first. Look at them.

## Verify

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
./gradlew :permahearts:build
./gradlew :permahearts:runClientGameTest
```

## Docs

- `docs/mods/permahearts/spec.md`: section "Hộp thưởng nhiệm vụ" (VN) and the quest numbers.
- `docs/workflow/playtest.md`: an emerald box step and the bottle (VN).
- `02-findings.md`: the enchanted book and XP level API names.

## Done when

- [ ] Visits every 5 days, new weights and lists, no duplicate mob in a visit.
- [ ] Each line shows its rolled box; old saves still load.
- [ ] Emerald box: green, 6 cards, pick 2, the five specials work, the bottle gives 16 levels.
- [ ] Build and client tests green, screenshots looked at.

## Risks / ask first

- If storing the tier on the quest line breaks the save format in a way the optional field cannot cover, stop and report.
- Muted test clients only.
