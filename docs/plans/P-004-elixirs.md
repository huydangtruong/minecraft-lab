# P-004 — ImmortalCultivation switch + elixirs (permanent stats)

> **Tóm tắt cho Huy:** đổi công tắc thành **ImmortalCultivation** (`/gamerule permahearts:immortal_cultivation true`). Thêm 6 linh dược: uống là +1 cấp chỉ số vĩnh viễn (sát thương, tốc đánh, tốc chạy, giáp, tầm với, tốc đào), không trần, chết không mất. Thử bằng `/give`. Bản 0.4.0.

- **Status:** merged
- **Mod / decision:** `permahearts`, D11, D15 · **Branch:** `feat/P-004-elixirs` · **Depends on:** P-003 (merged, 0.3.0)

## Goal

Rules: [spec.md](../mods/permahearts/spec.md) sections "Công tắc ImmortalCultivation" and "Linh dược". P-005 will hand these elixirs out as quest rewards, so keep `ElixirItems.get(ElixirStat)` public and stable.

## Scope

- In: `mods/permahearts/` only, `mod_version=0.4.0`, docs below.
- Out: quests/NPC (P-005), root Gradle files, `scripts/`, pinned versions, hand-drawn textures (reuse the vanilla potion model with a tint).

## Verified API (26.3, javap)

- Items: `Item.Properties#setId(ResourceKey<Item>)`, `#stacksTo(int)`, `#rarity(Rarity)`, `#component(DataComponentType<T>, T)`, `#usingConvertsTo(Item)`. `Items.registerItem` is private → register with `Registry.register(BuiltInRegistries.ITEM, key, item)`, `key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("permahearts", "elixir_strength"))`; the `Properties` must get `.setId(key)` before `new Item(...)`.
- Drinking: `.component(DataComponents.CONSUMABLE, Consumables.DEFAULT_DRINK)` + `.usingConvertsTo(Items.GLASS_BOTTLE)`. `Item#finishUsingItem(ItemStack, Level, LivingEntity)` calls `Consumable.onConsume` (shrinks the stack, gives the bottle). Override it in `ElixirItem`: do our work on the server, then `return super.finishUsingItem(...)`.
- Glint: `.component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)`. Tooltip: override `appendHoverText(ItemStack, Item.TooltipContext, TooltipDisplay, Consumer<Component>, TooltipFlag)`.
- Item model (no texture needed): `assets/permahearts/items/elixir_strength.json` = `{"model":{"type":"minecraft:model","model":"minecraft:item/potion","tints":[{"type":"minecraft:constant","value":<signed ARGB int>}]}}` (vanilla `items/potion.json` uses the same model with a `minecraft:potion` tint; `net.minecraft.client.color.item.Constant` exists). Tint index 0 = `layer0` = `potion_overlay` (the liquid).
- Creative tab: `CreativeModeTabs` keys are private. Use `CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("food_and_drinks"))).register(out -> …)` (`net.fabricmc.fabric.api.creativetab.v1`); check the `ModifyOutput` lambda's parameter type and its `accept(ItemLike)` method with javap. If it fights you, skip the tab (we have `/give`), and say so.
- Attributes (`Holder<Attribute>`): `Attributes.ATTACK_DAMAGE`, `ATTACK_SPEED`, `MOVEMENT_SPEED`, `ARMOR`, `BLOCK_INTERACTION_RANGE`, `ENTITY_INTERACTION_RANGE`, `BLOCK_BREAK_SPEED`. `AttributeModifier.Operation.ADD_VALUE` / `ADD_MULTIPLIED_BASE`.

## Files (package `io.github.huydangtruong.permahearts`, under `mods/permahearts/src/`)

| File | Change |
|---|---|
| `main/java/.../PermaHearts.java` | Game rule id `"enabled"` → `"immortal_cultivation"` (keep the field name `ENABLED`, update the javadoc). Call `ElixirItems.init()` in `onInitialize`. Everything else stays (the toggle callback already calls `HealthApplier.apply`, which now also applies stats) |
| `main/java/.../ElixirStat.java` (new, **no Minecraft imports**) | `enum ElixirStat { STRENGTH("strength", 0.5, false), FURY("fury", 0.10, true), SWIFTNESS("swiftness", 0.05, true), IRON_SKIN("iron_skin", 1.0, false), REACH("reach", 0.5, false), MINING("mining", 0.10, true) }` with `id()`, `step()`, `multiplier()` (true → `ADD_MULTIPLIED_BASE`), `double bonus(int level) = level * step`, `static Optional<ElixirStat> byId(String)` |
| `main/java/.../CultivationData.java` (new, no Minecraft imports except the codec, like `HeartsData`) | `record CultivationData(Map<String, Integer> levels)`, `DEFAULT` = empty map, `int level(ElixirStat)`, `CultivationData plus(ElixirStat, int n)` (no cap, never below 0), `CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT).xmap(CultivationData::new, CultivationData::levels)` (a map keyed by id, so new stats can be added later; unknown keys are kept) |
| `main/java/.../HeartsAttachment.java` | Second persistent attachment `CULTIVATION`: id `permahearts:cultivation`, `.persistent(CultivationData.CODEC).copyOnDeath().initializer(() -> CultivationData.DEFAULT)`. No sync needed. Helpers `getCultivation(ServerPlayer)`, `setCultivation(ServerPlayer, CultivationData)`. Touch it in `onInitialize` like the others |
| `main/java/.../StatApplier.java` (new) | `static void apply(ServerPlayer p)`: for each `ElixirStat`, the attribute(s) it drives (REACH → both interaction ranges), modifier id `permahearts:elixir_<id>`; value `on ? stat.bonus(level) : 0`; 0 → `removeModifier`, else `addOrReplacePermanentModifier(new AttributeModifier(id, value, op))` |
| `main/java/.../HealthApplier.java` | First line of `apply(p, refill)`: `StatApplier.apply(p);` (so JOIN, respawn, toggle and `/hearts set` all re-apply stats) |
| `main/java/.../ElixirItem.java` (new) | `extends Item`, holds its `ElixirStat`. `finishUsingItem`: if `entity instanceof ServerPlayer sp`: if switch off → `sp.sendSystemMessage(Messages.elixirDisabled())` and `return stack` unchanged (not consumed). Else `setCultivation(plus(stat, 1))`, `StatApplier.apply(sp)`, sound `SoundEvents.PLAYER_LEVELUP` (or a brewing/drink sound you find), chat `Messages.elixirDrunk(stat, newLevel, totalBonusText)`; then `super.finishUsingItem`. Tooltip line: `Messages.elixirTooltip(stat)` (e.g. "+0.5 attack damage, permanent") |
| `main/java/.../ElixirItems.java` (new) | Registers the 6 items `permahearts:elixir_<id>`: `stacksTo(16)`, `rarity(Rarity.EPIC)`, CONSUMABLE drink, glass bottle, glint. `public static Item get(ElixirStat)`, `static void init()`, creative tab entries (FOOD_AND_DRINKS) |
| `main/resources/assets/permahearts/items/elixir_<id>.json` (6, new) | Model JSON above. Tints (ARGB): strength `0xFFD8352B` red, fury `0xFFF2C12E` yellow, swiftness `0xFF7CD9F2` light blue, iron_skin `0xFF8F9BA8` grey, reach `0xFFB35CE0` purple, mining `0xFF3FBF5A` green. Write them as signed ints in JSON (e.g. `0xFFD8352B` = `-2607829`; compute each) |
| `main/java/.../Messages.java` + `lang/en_us.json`, `vi_vn.json` | Item names `item.permahearts.elixir_<id>` (EN "Elixir of Strength / Fury / Swiftness / Iron Skin / Reach / Mining"; VN "Linh Dược Sức Mạnh / Cuồng Phong / Thần Tốc / Kim Cang / Trường Thủ / Khai Sơn"). Stat names `permahearts.stat.<id>` (EN "attack damage", "attack speed", "movement speed", "armor", "reach", "mining speed"; VN "sát thương", "tốc đánh", "tốc chạy", "giáp", "tầm với", "tốc đào"). `elixirDrunk`: "+%s %s! Total: +%s" / "+%s %s! Tổng: +%s" (format bonus as "0.5" or "10%" for multiplier stats: put a pure helper `ElixirStat#format(double)` in `ElixirStat` and JUnit it). `elixirTooltip`, `elixirDisabled` ("ImmortalCultivation is off in this world"). `cultivationStatus(...)`: one line listing every stat with level > 0, or "No elixirs yet". Game rule: rename keys to `gamerule.permahearts.immortal_cultivation` = "ImmortalCultivation" (both languages) and `.description` (EN "Lose hearts on death, HeartXP, elixirs and the Wanderer quests", VN "Chết mất tim, HeartXP, linh dược và nhiệm vụ Lữ Khách"). `toggled` → "ImmortalCultivation is now ON/OFF in this world" / "ImmortalCultivation đã BẬT/TẮT trong world này". `disabledHint` → command `/gamerule permahearts:immortal_cultivation true`. Remove the old `gamerule.permahearts.enabled*` keys |
| `main/java/.../HeartsCommand.java` | `/hearts` also sends `Messages.cultivationStatus(...)` as a second line |
| `mods/permahearts/gradle.properties` | `mod_version=0.4.0` |

## Tests

- JUnit `ElixirStatTest` (new): `bonus(3)` for STRENGTH = 1.5; FURY level 2 = 0.2; `byId("reach")`; `format` ("+1.5", "+20%"); `CultivationData.plus` from empty, twice, no cap at 1000; codec round-trip via `JsonOps` (like `HeartsCodecTest`) keeps an unknown key.
- Game tests (`PermaHeartsGameTest`, keep `enable(context, true)` first in every test):
  - `elixirAddsAttackDamage`: mock player, base `getAttributeValue(Attributes.ATTACK_DAMAGE)` = b; call `ElixirItems.get(STRENGTH).finishUsingItem(new ItemStack(item), level, p)` twice → b + 1.0; cultivation level 2.
  - `elixirMultiplierStats`: one SWIFTNESS → movement speed = base × 1.05 (±1e-6); one REACH → both interaction ranges +0.5.
  - `elixirKeptOnDeath`: drink IRON_SKIN, `dieAndRespawn` → new player armor +1, level 1.
  - `elixirOffWhenDisabled` (try/finally restore `true`): level 1 STRENGTH, `enable(false)` → attack damage back to base, data still level 1, drinking does not change the level; `enable(true)` → +0.5 again.
- Old tests still pass (they use `PermaHearts.ENABLED`, so the rename is transparent).

## Verify

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
./gradlew :permahearts:build      # JUnit + all game tests + client compile
```

Do not run `runClient`, `scripts/install.sh` or `scripts/package.sh`. Item looks (tint, glint) and the Game Rules screen are only checked by Huy.

## Docs

- `docs/mods/README.md`: 0.4.0.
- `docs/workflow/playtest.md`: switch command renamed; add "`/give @s permahearts:elixir_strength 4`, drink → `/hearts` shows +2 attack damage; die → still there".
- `docs/system/02-findings.md`: item registration, consumable, item model/tint facts you confirmed or corrected.

## Commit

`PermaHearts 0.4.0: ImmortalCultivation switch, six permanent-stat elixirs (P-004)`; push the branch, never `main`.

## Done when

- [ ] `./gradlew :permahearts:build` green
- [ ] Report in the team.md format (say: items not seen in game)

## Risks / ask first

- If `finishUsingItem` on a mock (creative) player does not run our code path, call a package-private `ElixirItem.drink(ServerPlayer)` from the test instead and say so.
- Unknown keys in `CultivationData` must survive a save (a future stat must not be wiped by an older jar).
