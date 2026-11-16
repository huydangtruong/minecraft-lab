# P-038 — Simple reward box roll, Heart Elixir, God Slayer HeartXP, Enlightenment +24

> **Tóm tắt cho Huy (VN, ≤ 3 dòng):** Rương quà rút thẻ theo luật 50% linh dược / 50% đồ, tỉ lệ ngang nhau (D108); thêm Bình Tâm Mạch +1 tim ở rương vàng trở lên; giết God Slayer được HeartXP; Bình Giác Ngộ +24 level.

- **Status:** ready
- **Mod / decision:** `permahearts`, D108 (+ BL-017, BL-018, BL-019, BL-020) · **Branch:** `feat/P-038-rewards`
- Wording source: rows BL-017 to BL-020 in `docs/backlog/BACKLOG.md`.
- Files: `reward/RewardRoll.java` (+ its JUnit test), `reward/RewardBoxes.java`, `reward/EnlightenmentBottleItem.java`, `ElixirItems.java` / `ElixirItem.java`, `HeartRules.java`, `PermaHearts.java` (kill handler), `Messages.java`, `lang/en_us.json`, item model/texture for the new elixir (follow how the other elixirs are made).

## 1. Simple reward roll (BL-020, D108)
Replace the special-chance logic in `RewardRoll.roll`:
- Each card: **50% elixir, 50% item**.
- Elixir: uniform over the box's elixir list (may repeat). Silver: the 7 stat elixirs. Gold, diamond and emerald: the 7 + the Heart Elixir (8).
- Item: uniform over the box's item list, **no repeats** inside one box (pick from the items not yet used). No separate "special" step.
  - Silver: the 8 base items (amounts as today from `QuestGenerator.collectAmount`).
  - Gold: the 8 base + enchanted golden apple ×4; its emerald card is **×32**.
  - Diamond: the 8 base + enchanted golden apple ×4 + netherite ingot ×1; emerald card **×32**.
  - Emerald: the 8 base + totem, Bottle of Enlightenment, Mending book, Sharpness V book, Protection V book.
- Remove `SPECIAL_CHANCE`, `EMERALD_SPECIAL_CHANCE` and `specialChance`. Keep the lists as plain per-tier item lists. Already rolled boxes keep their cards.
- JUnit: over many seeded rolls each elixir and each item shows up within a few percent of the uniform rate; no item repeats; Heart Elixir never in silver.

## 2. Heart Elixir (BL-017)
- New item `permahearts:elixir_heart`, name **"Heart Elixir"**, pink-red color, stacks like the other elixirs, drink like them.
- Drinking: **+1 max heart** permanently through `HeartRules` (same as a heart level-up; cap 300 = `HeartRules.MAX`). At the cap: not consumed, show a short message. Show a message like the level-up one when it works.
- Not a stat elixir: keep it out of `ElixirStat` and the stats screen. It is only in reward boxes (gold and up) and the creative tab next to the elixirs.
- Pure rule in `HeartRules` (e.g. `addHeart`) with JUnit; a server game test that drinking it raises max hearts by 1.

## 3. God Slayer gives HeartXP (BL-018)
- Today `PermaHearts` AFTER_DEATH only gives HeartXP for `entity instanceof Enemy`, and `GodSlayer` is a `PathfinderMob` without `Enemy`. Add the God Slayer explicitly to that check (do **not** add `Enemy` to the class: that would make iron golems and Illusion clones attack it on their own). XP = its max health as for other mobs (500 at Lv 1 = +1 heart).
- Game test: the boss killed with a player as kill credit gives that player 500 HeartXP.

## 4. Bottle of Enlightenment +24 (BL-019)
- `EnlightenmentBottleItem.LEVELS` 16 → 24, the tooltip/lang text and comments too.

## Verify
```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
./gradlew :permahearts:build
```
One reward-box client screenshot (a gold box showing its cards) and the Heart Elixir in the inventory into an empty folder `/private/tmp/claude-501/-Users-truongdanghuy-Coding-Minecraft-Lab/27b354fc-b3c2-4308-a712-0331ea32eee9/scratchpad/p038/`. Every Minecraft you open is muted.

## Docs
`docs/mods/permahearts/spec.md` (reward box and elixir lines), `docs/workflow/playtest.md` line. D108 is already in `01-decisions.md`.

## Commit
Branch `feat/P-038-dev` from `feat/P-038-rewards`, small commits, merge into `feat/P-038-rewards`. Do not push. Do not commit `__pycache__` files. English messages ending with the Co-Authored-By line.
