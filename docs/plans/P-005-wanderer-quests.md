# P-005 — The Wanderer: NPC, shared quest screen (collect + kill), elixir rewards

> **Tóm tắt cho Huy:** sau 3–5 ngày Minecraft, một **Lữ Khách** xuất hiện cách ~1000 block (tọa độ báo trên chat). Bấm vào: màn hình nhiệm vụ + nút Nhận → 2–4 dòng "Tìm 64 Sắt" (ô bỏ đồ) / "Giết 16 Zombie" (số đếm), tiến độ chung cho mọi người. Dòng xong → ô linh dược, ai lấy trước được. Thử nhanh: `/quest summon`. Bản 0.5.0.

- **Status:** ready
- **Mod / decision:** `permahearts`, D11–D14 · **Branch:** `feat/P-005-wanderer` · **Depends on:** P-004 (merged, 0.4.0: `ElixirStat`, `ElixirItems.get`)

## Goal

Rules: [spec.md](../mods/permahearts/spec.md) section "Lữ Khách và nhiệm vụ". This plan does quest types **COLLECT** and **KILL** only; P-006 adds BLOOD_MOON, EXPLORE, BOSS into the same model, so make the line type an enum and keep per-type code in one `switch` each (model, screen row, generator pool).

## Scope

- In: `mods/permahearts/` (new package `io.github.huydangtruong.permahearts.wanderer` + client screen), `mod_version=0.5.0`, docs below. The root `build.gradle` **only** if the class-tweaker step below is needed.
- Out: blood moon / explore / boss (P-006), custom entity type or renderer (we reuse the vanilla Wandering Trader), textures (draw the screen with `fill`/`text`), pinned versions.

## Design

### Pure model (no `net.minecraft` imports; DFU `Codec` is fine, like `HeartsData`) — package `...permahearts.wanderer`

- `enum QuestType { COLLECT, KILL }` (P-006 appends more).
- `record QuestLine(QuestType type, String target, int required, int progress, String reward, boolean claimed)`: `target` = item id (`minecraft:iron_ingot`) or entity type id (`minecraft:zombie`); `reward` = `ElixirStat.id()`; `done()` = `progress >= required`. `CODEC`.
- `record Quest(long id, List<QuestLine> lines, boolean accepted)` + `CODEC`, `allDoneAndClaimed()`.
- `enum Phase { WAITING, PLANNED, PRESENT }`.
- `record WandererData(Phase phase, long nextDay, int x, int y, int z, Optional<String> npcUuid, Optional<Quest> quest, long questCounter)`; `DEFAULT` = WAITING, `nextDay = -1`. `CODEC` (use `Codec.STRING.optionalFieldOf`, enum via `Codec.STRING.xmap(Phase::valueOf, Phase::name)`).
- `QuestLogic` (static, pure): `int deposit(Quest, int line, String itemId, int count)` → amount taken (0 if wrong item / not accepted / done / not COLLECT), returns new `Quest` through a small result record; `Optional<…> kill(Quest, String entityId)` → first not-done KILL line with that target +1; `claim(Quest, int line)` → only if done and not claimed; `complete(Quest, int line)` (admin); `accept(Quest)`.
- `QuestGenerator.generate(long id, RandomGenerator rng)`: `2 + rng.nextInt(3)` lines; each line COLLECT or KILL 50/50; no two lines with the same target; reward = random `ElixirStat` (uniform). Pools (id, count):
  - COLLECT: iron_ingot 64, gold_ingot 32, diamond 8, emerald 8, coal 64, redstone 64, lapis_lazuli 64, carrot 64, potato 64, wheat 64, mutton 32, beef 32, oak_log 64, bone 32, string 32, gunpowder 16, ender_pearl 8, leather 32.
  - KILL: zombie 16, skeleton 16, spider 16, creeper 8, enderman 8, witch 4, drowned 8.
  - `static int daysUntilNext(RandomGenerator rng)` = 3..5 inclusive.
  - `static double[] planOffset(RandomGenerator rng)` = random angle, distance 900..1100 → `{dx, dz}`.

### Server glue

| File (new unless noted) | What |
|---|---|
| `wanderer/WandererSavedData.java` | `extends SavedData`, holds a `WandererData` (`get()`, `set(d)` → `setDirty()`). `TYPE = new SavedDataType<>(Identifier.fromNamespaceAndPath("permahearts","wanderer"), WandererSavedData::new, CODEC_xmap, null)` — try `null` for `DataFixTypes` (`SavedDataStorage` has null checks); if it throws, report. `static WandererSavedData of(MinecraftServer s)` = `s.getDataStorage().computeIfAbsent(TYPE)` |
| `wanderer/Wanderer.java` | Facade used by events, commands, menu **and game tests** (no UI needed to test): `currentDay(server)` = `server.overworld().getOverworldClockTime() / 24000L`; `plan(server, ServerPlayer near)`; `spawnNpc(ServerLevel, BlockPos, Quest)`; `summon(ServerPlayer)` (admin: NPC 3 blocks in front, new quest, PRESENT); `accept(server)`; `deposit(server, line, ItemStack)` (shrinks the stack by the amount taken); `onKill(server, ServerPlayer, EntityType<?>)`; `claim(server, line, ServerPlayer)` → `ItemStack` (or EMPTY if someone was faster); `farewellIfFinished(server)`; `reset(server)`; `startQuestForTest(server, BlockPos, Quest)` |
| `wanderer/WandererNpc.java` | Spawn: `EntityTypes.WANDERING_TRADER.create(level, EntitySpawnReason.EVENT)`, `snapTo(x+0.5, y, z+0.5, yaw, 0)`, `setNoAi(true)`, `setPersistenceRequired()`, `setDespawnDelay(0)` (0 = never despawns, see `WanderingTrader#maybeDespawn`), `setCustomName(Messages.wandererName())`, `setCustomNameVisible(true)`, set attachment `WANDERER = true`, `level.addFreshEntity`. Store its UUID. `static boolean isWanderer(Entity)` = `getAttachedOrElse(WANDERER, false)` |
| `HeartsAttachment.java` (edit) | `WANDERER`: `AttachmentType<Boolean>` id `permahearts:wanderer`, **persistent** (`Codec.BOOL`) + `.syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.all())` (the client must know, see interaction) |
| `wanderer/WandererScheduler.java` | `ServerTickEvents.END_SERVER_TICK`, work every 20 ticks, only when `PermaHearts.enabled(server)`. WAITING: `nextDay < 0` → `day + daysUntilNext`. `day >= nextDay` and Overworld has players → pick a random Overworld player, `planOffset`, PLANNED at (px+dx, pz+dz), chat to everyone `Messages.wandererPlanned(x, z)`. PLANNED: any Overworld player within 96 blocks horizontally and `level.isPositionEntityTicking(pos)` → surface `y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z)`; if the block below is water (`getFluidState(..).is(FluidTags.WATER)`) put a 3×3 `Blocks.OAK_PLANKS` raft at y-1; spawn with a fresh `QuestGenerator` quest; PRESENT. PRESENT: every 100 ticks, if `isPositionEntityTicking(npcPos)` and `level.getEntity(uuid) == null` → respawn at the stored position (same quest, new UUID) |
| `PermaHearts.java` (edit) | Register: scheduler; `UseEntityCallback.EVENT` → if `hand == MAIN_HAND` and `isWanderer(entity)`: client → `InteractionResult.SUCCESS`; server → if switch off send `Messages.disabledHint()`, else open the menu; return `SUCCESS` (this also blocks the trade screen). Other hand → `SUCCESS` too for wanderers (no trade). `ServerLivingEntityEvents.ALLOW_DAMAGE` → `false` for wanderers. `ServerEntityEvents.ENTITY_LOAD` → a wanderer whose UUID is not the stored `npcUuid` (or phase ≠ PRESENT) is `discard()`ed (stale copy after reset/respawn). In the existing `AFTER_DEATH` listener also call `Wanderer.onKill` when the kill credit is a `ServerPlayer` and the switch is on (any mob, not only `Enemy`). `QuestCommand.register` |
| `wanderer/QuestCommand.java` | `/quest` (everyone): phase PRESENT/PLANNED → coordinates + distance from you; WAITING → "next Wanderer in about N days"; plus each line's progress. Admin (`Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)`): `/quest summon`, `/quest skip` (reset then `nextDay = today`, so it plans on the next scheduler pass), `/quest complete <line 1..4>`, `/quest reset` |

### Menu + screen

- `wanderer/QuestSnapshot`: `record(long questId, List<LineView> lines, boolean accepted)`, `LineView(QuestType type, String target, int required, String reward)`; `STREAM_CODEC` with `ByteBufCodecs.STRING_UTF8`, `VAR_INT`, `VAR_LONG`, `BOOL`, `.apply(ByteBufCodecs.list(4))` (javap-verified names).
- `wanderer/WandererMenus`: `TYPE = Registry.register(BuiltInRegistries.MENU, id("wanderer"), new ExtendedMenuType<>(WandererMenu::new, QuestSnapshot.STREAM_CODEC))` (`net.fabricmc.fabric.api.menu.v1.ExtendedMenuType(ExtendedFactory<T,D>, StreamCodec<? super RegistryFriendlyByteBuf, D>)`; check the factory's parameter order with javap). Open with `player.openMenu(new ExtendedMenuProvider<QuestSnapshot>() { getScreenOpeningData(ServerPlayer), getDisplayName(), createMenu(int, Inventory, Player) })`.
- `wanderer/WandererMenu extends AbstractContainerMenu`:
  - Client ctor `(int id, Inventory inv, QuestSnapshot snap)`; server ctor also gets the server + NPC entity.
  - Data slots (`addDataSlot`): `accepted`, `progress[0..3]`, `claimed[0..3]`. Server slots read the live quest every `broadcastChanges` (anonymous `DataSlot` whose `get()` reads `WandererSavedData`), so another player's deposit shows up live. Client slots: `DataSlot.standalone()` initialised from the snapshot.
  - Per line i, two `Slot`s at the same x,y, exactly one `isActive()`: **deposit** slot (per-menu `SimpleContainer(4)`; active when accepted, COLLECT, not done; `mayPlace` only the target item) and **reward** slot (active when done and not claimed; `mayPlace` false). On the server, deposit `setChanged()` → `Wanderer.deposit(...)` (leftover stays in the slot); `removed(Player)` → `clearContainer(player, depositContainer)` gives leftovers back.
  - Reward container: on the server **one shared `Container` object per quest** (`RewardContainer`, kept in `Wanderer` keyed by quest id) whose `getItem(i)` returns a cached `new ItemStack(ElixirItems.get(stat))` while done && !claimed, else EMPTY; `removeItem`/`removeItemNoUpdate`/`setItem(i, EMPTY)` mark the line claimed (whichever path vanilla takes), then `farewellIfFinished`. Shared object ⇒ the other open menu sees the slot empty on its next `broadcastChanges`. Client: plain `SimpleContainer(4)`.
  - `addStandardInventorySlots(inv, x, y)` for the player inventory. `quickMoveStack`: reward slot → player inventory; player inventory → the active deposit slot that accepts it; otherwise EMPTY.
  - `clickMenuButton(player, 0)` → `Wanderer.accept`. Ids 10..13 are reserved for P-006 line buttons.
  - `stillValid`: NPC alive, within 8 blocks, phase PRESENT and same quest id.
- Client `client/WandererScreen extends AbstractContainerScreen<WandererMenu>` (register in `PermaHeartsClient` with `MenuScreens.register(WandererMenus.TYPE, WandererScreen::new)`):
  - Size about 230×222. Panel drawn with `GuiGraphicsExtractor.fill`/`outline` (dark brown panel, lighter slot squares at every active slot; find which `extract*` hook draws behind slots: `extractContents`/`extractRenderState` on `AbstractContainerScreen` per javap).
  - Not accepted: title "Lữ Khách", 3–4 lines intro text (`Messages.wandererIntro()`, word-wrapped), button **Nhận nhiệm vụ** (`Button.builder(text, b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0))`). Slots hidden (isActive false).
  - Accepted: one row per line: icon (`g.item(...)` or whatever 26.3 calls item rendering in `GuiGraphicsExtractor`, check javap; skip icon if unclear) + text `Messages.lineCollect(itemName, progress, required)` / `lineKill(entityName, …)`, slot on the right; done & unclaimed → "Phần thưởng: <elixir name>" + reward slot; claimed → "Đã nhận". Item name from `BuiltInRegistries.ITEM.getValue(Identifier.parse(target)).getName()`; entity name from `BuiltInRegistries.ENTITY_TYPE.getValue(...).getDescription()` (verify names).
  - Rebuild widgets when `accepted` flips (check in `containerTick`).
- If `MenuScreens.register` is not accessible at compile time (Fabric's class tweaker for it is not `transitive`): add `mods/permahearts/src/main/resources/permahearts.classtweaker` (`classTweaker v1 official` + `accessible method net/minecraft/client/gui/screens/MenuScreens register (Lnet/minecraft/world/inventory/MenuType;Lnet/minecraft/client/gui/screens/MenuScreens$ScreenConstructor;)V` + `accessible class net/minecraft/client/gui/screens/MenuScreens$ScreenConstructor`), reference it from `fabric.mod.json` and, in the root `build.gradle` subprojects block, `def ct = file("src/main/resources/${mod_id}.classtweaker"); if (ct.exists()) { loom { accessWidenerPath = ct } }` (check the Loom 1.19 property name). Note what worked in 02-findings.

### Messages (`Messages.java` + `en_us.json` + `vi_vn.json`)

`wandererName` "Wanderer" / "Lữ Khách"; `wandererPlanned(x, z)` "A Wanderer is waiting at X %s, Z %s" / "Một Lữ Khách đang chờ ở X %s, Z %s"; `wandererIntro` (VN: "Ta là kẻ lữ hành qua nhiều cõi. Giúp ta vài việc, ta sẽ tặng ngươi linh dược tu luyện."); `accept` "Accept" / "Nhận nhiệm vụ"; `lineCollect` "Find %s %s: %s/%s" / "Tìm %s %s: %s/%s"; `lineKill` "Kill %s %s: %s/%s" / "Giết %s %s: %s/%s"; `lineDone` "Reward: %s" / "Phần thưởng: %s"; `lineClaimed` "Claimed" / "Đã nhận"; `killProgress` overlay "Quest: %s %s/%s" / "Nhiệm vụ: %s %s/%s"; `lineCompleted` broadcast "Quest line done: %s. Reward waits at the Wanderer" / "Xong một nhiệm vụ: %s. Phần thưởng đang chờ ở Lữ Khách"; `farewell` "The Wanderer thanks you and moves on" / "Lữ Khách cảm ơn và lên đường"; `/quest` status lines (where, distance, next in N days). Menu title = `wandererName`.

## Tests

- JUnit `QuestLogicTest`: deposit wrong item → 0; deposit 70 of 64 needed → takes 64, done; deposit before accept → 0; kill matching → +1, non-matching → empty; claim not-done → refused, done → ok, twice → refused; `allDoneAndClaimed`; generator: 2..4 lines, distinct targets, rewards valid ids (100 seeds); `daysUntilNext` in 3..5; `planOffset` distance in 900..1100. `WandererData` codec round-trip (JsonOps).
- Game tests (each: `enable(context, true)`, `Wanderer.reset(server)` at start **and** in `finally`; game tests share one server and one saved data):
  - `summonSpawnsWanderer`: `Wanderer.summon(p)` → PRESENT, a `WanderingTrader` with `WANDERER` attachment within 5 blocks, `isNoAi()`, 2–4 lines; `ALLOW_DAMAGE`: hurt it with player attack → health unchanged.
  - `collectDepositShared`: `startQuestForTest` with COLLECT iron_ingot 10 (reward strength) + KILL zombie 2; `accept`; `deposit(line 0, 6 iron)` → progress 6, stack empty; second "player" deposits 10 → progress 10, 6 left in the stack; done.
  - `killCountsForQuest`: same quest, kill 2 zombies with `playerAttack` → line 1 done; a cow does nothing.
  - `claimFirstComeFirstServed`: line done → `claim(line, p1)` gives `ElixirItems.get(STRENGTH)`; `claim(line, p2)` → EMPTY.
  - `farewellWhenFinished`: complete + claim every line → NPC gone (`getEntity(uuid) == null` or removed), phase WAITING, `nextDay - today` in 3..5.
  - `schedulerPlansFarAway`: WAITING with `nextDay = today`, call the scheduler's work method directly → PLANNED, horizontal distance from the player 900..1100.
  - Menu-only behaviour (slots, screen) is not game-tested; say so in the report.

## Verify

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
./gradlew :permahearts:build
```

Do not run `runClient`, `scripts/install.sh`, `scripts/package.sh`.

## Docs

- `docs/mods/README.md`: 0.5.0.
- `docs/workflow/playtest.md`: "`/gamerule permahearts:immortal_cultivation true`, `/quest summon` → bấm Lữ Khách → Nhận → bỏ đồ / giết mob → lấy linh dược; người thứ hai mở cùng lúc thấy cùng tiến độ; `/quest complete 1` để thử nhanh; `/quest skip` → tọa độ trên chat, đi tới thì NPC hiện".
- `docs/system/02-findings.md`: SavedData, ExtendedMenuType, menu screen registration, clock/day, anything corrected.

## Commit

`PermaHearts 0.5.0: the Wanderer NPC with shared collect/kill quests and elixir rewards (P-005)`; push the branch, never `main`.

## Done when

- [ ] `./gradlew :permahearts:build` green
- [ ] Report in team.md format (say: screen compiled, not seen in game)

## Risks / ask first

- `UseEntityCallback` on the client needs the `WANDERER` attachment synced (`all()`); if the trade screen still opens in game, Huy will see it at playtest: note in the report what you relied on.
- If the shared reward container approach fights vanilla slot sync, keep the rule (first come first served, the other player sees it vanish) and explain your alternative in the report; do not add custom networking unless needed.
