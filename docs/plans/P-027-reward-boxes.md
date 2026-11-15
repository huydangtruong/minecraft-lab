# P-027 — Reward boxes (silver, gold, diamond) for quest lines

> Tóm tắt (VN): mỗi dòng nhiệm vụ thưởng rương quà thay cho bình linh dược. Rương bạc 3 ô, vàng 4 ô, kim cương 5 ô. Chuột phải mở màn hình, chọn 1 ô (viền vàng), Claim hoặc Close. Quà giữ nguyên khi đóng mở lại.
> Decision: D76. Branch: dev on `feat/P-027-dev`, merges into `feat/P-027-reward-boxes`.

- **Status:** merged
- **Dev:** one Sonnet subagent. Another dev works on P-026 (techniques, a new technique, `technique/`) at the same time. Do not touch technique code.

## Today

- `wanderer/QuestGenerator.generate` gives each `QuestLine` a `reward` = a random `ElixirStat` id.
- Claiming a line gives that elixir. Find the claim code (`WandererMenu` / `RewardContainer` / `QuestLogic`).

## Rules (pure Java, JUnit-tested)

- **Tiers:** `RewardTier { SILVER(3 options, 1 pick), GOLD(4, 1), DIAMOND(5, 1) }`. Keep `picks` as a field, so a later tier can allow 2 or more.
- **Tier by line type:**
  - `COLLECT`, `KILL` → SILVER;
  - `EXPLORE`, `BLOOD_MOON` → GOLD;
  - `BOSS`, `DEATH_MATCH` → DIAMOND.
- **Rolling the options** (`RewardRoll.roll(tier, rng)` → a list of `RewardOption(itemId, count)`):
  - **Each option independently: 50% elixir, 50% item.** So a box can be all elixirs or all items.
  - **Elixir:** one bottle of a random `ElixirStat`. Duplicates allowed.
  - **Item:** from the base pool, with the **same amounts for every tier** (D76: as in the collect quests):
    - iron ingot 64, gold ingot 64, diamond 16, emerald 16;
    - redstone 64, lapis lazuli 64, ender pearl 32, gunpowder 32.
    - Read the amounts from `QuestGenerator.COLLECT` where the item is there, so they stay in sync.
  - **Specials:** an item option rolls a special first, at 20%:
    - GOLD: 4 enchanted golden apples, or 32 emeralds;
    - DIAMOND: 4 enchanted golden apples, 1 netherite ingot, or 32 emeralds;
    - SILVER has none.
  - No two identical item options in one box. Elixirs may repeat.
- Keep the numbers (50%, 20%, the pools) as constants in one place.

## The item

- Three items: `permahearts:silver_reward_box`, `gold_reward_box`, `diamond_reward_box`.
  - Stack size 1, so each box holds its own roll.
  - English names: "Silver Reward Box" and so on. Rarity colour on the name.
  - Original 16×16 sprites: a small chest or gift box in silver, gold and diamond-cyan. Generate them like the other sprites in `tools/`.
- **Roll on first open:**
  - The options and the chosen state live in a **data component** on the stack. Register a custom one (`DataComponentType` with a codec), checked with javap / Fabric API.
  - Closing and reopening shows the same options. Dropping, chest storage and relogging keep it.
- **The quest claim:** gives the box of the line's tier instead of the elixir.
  - Lines saved with an old elixir `reward` give a box too: ignore the stored id. Keep the field for save compatibility.
  - Update the quest screen so the reward slot shows the box icon instead of the elixir.

## The screen

- **Right click** with the box in hand. The server rolls the options if the stack has none, then opens the screen.
  - Use a menu or a packet-opened screen, whichever the mod already uses for its quest screen.
- **Layout:**
  - the box name as the title;
  - a row of N option cards, each with the item icon, the count, and the name, with a tooltip on hover;
  - a **Claim** button and a **Close** button.
- **Selecting:**
  - Clicking a card selects it with a **gold outline**. Clicking another card moves the selection, while `picks == 1`.
  - With `picks > 1`, up to `picks` cards can be selected. The code path must work, even though no tier uses it yet.
  - Claim is disabled until exactly `picks` cards are selected.
- **Claim:**
  - The server validates the selection against the stored roll. Never trust client item ids.
  - It gives the items to the inventory, or drops them at the feet if full, removes one box from the hand, and plays a level-up sound.
- **Close:** closes. The box stays, with the same options.
- Look: the mod's existing screen style, readable at GUI scale 2 and 3.

## Tests

- **JUnit:**
  - tier by line type;
  - roll sizes (3 / 4 / 5);
  - over many seeded rolls, both elixirs and items appear, all-elixir and all-item boxes occur, specials appear only in GOLD and DIAMOND, and silver never has netherite;
  - item amounts equal the collect amounts;
  - no duplicate item options;
  - the selection rule for `picks = 1` and `picks = 2`.
- **Game tests:**
  - claiming a COLLECT line gives a silver box, a BOSS line a diamond box;
  - opening rolls and stores; opening again returns the same options;
  - claiming option k gives exactly that item and count, and consumes the box;
  - a forged selection index out of range is refused and the box is kept;
  - an old saved line with an elixir reward still gives a box.
- **Client test:** a screenshot of a gold box screen with one card selected (gold outline), plus the quest screen showing a box as the reward. Look at both yourself. Client tests stay muted: keep `MuteTestClientMixin`.

## Done when

- `./gradlew :permahearts:build` is green, and the client test is green with the screenshots checked.
- `docs/workflow/playtest.md` (Vietnamese) has steps.
- `02-findings.md` has the new API names.
- `docs/mods/permahearts/spec.md` has the reward rules.
- Merged into `feat/P-027-reward-boxes`, not pushed.
- Report in ≤10 lines.
