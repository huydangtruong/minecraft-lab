# Findings — API đã kiểm trên 26.3

Tên và chữ ký dưới đây lấy từ `javap` jar 26.3 và mã Fabric API nhánh `26.3` (2026-11-11). Thêm vào đây mỗi khi kiểm thêm một API.

## Fabric API

- `ServerPlayerEvents`: `AFTER_RESPAWN (ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive)`, `COPY_FROM (old, new, alive)`, `JOIN (ServerPlayer)`, `LEAVE`, `ALLOW_DEATH`.
- `ServerLivingEntityEvents.AFTER_DEATH (LivingEntity entity, DamageSource source)`.
- `ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY (ServerLevel, Entity killer, LivingEntity killed, DamageSource)`. Bắn từ `LivingEntity.die` (qua `Entity#killedEntity`) và `ServerPlayer.die`.
- Data attachment: `AttachmentRegistry.create(Identifier, builder -> builder.persistent(codec).copyOnDeath().initializer(supplier))`, `createPersistent(id, codec)`. Trên entity: `getAttached`, `getAttachedOrCreate`, `setAttached`, `modifyAttached`, `hasAttached`, `removeAttached`.
- Lệnh: `CommandRegistrationCallback`; quyền `Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)` (26.x dùng `PermissionCheck`, không còn số cấp).
- Test: `fabric-loader-junit` cho JUnit; game test dùng `@net.fabricmc.fabric.api.gametest.v1.GameTest` (tham số: `environment, dimension, structure, maxTicks, setupTicks, required, …`), entrypoint `fabric-gametest` trong `src/gametest/resources/fabric.mod.json`.

## Minecraft 26.3 (tên Mojang)

- `net.minecraft.resources.Identifier` (thay `ResourceLocation`): `fromNamespaceAndPath`, `parse`.
- `Attributes.MAX_HEALTH` là `Holder<Attribute>`. `LivingEntity#getAttribute(Holder)` → `AttributeInstance` có `addOrReplacePermanentModifier(AttributeModifier)`, `removeModifier(Identifier)`, `hasModifier(Identifier)`, `setBaseValue`.
- `new AttributeModifier(Identifier, double, AttributeModifier.Operation.ADD_VALUE)`; `Operation` còn `ADD_MULTIPLIED_BASE`, `ADD_MULTIPLIED_TOTAL`.
- `LivingEntity#getKillCredit()`, `#getMaxHealth()` (final), `#setHealth(float)`.
- `ServerPlayer#restoreFrom(ServerPlayer, boolean)` sao chép attribute **base value** và **permanent modifiers** + máu → modifier vĩnh viễn sống qua cái chết; vẫn nên `apply` lại sau respawn cho chắc.
- `ServerPlayer#sendSystemMessage(Component[, boolean overlay])`, `#sendOverlayMessage(Component)`. `Component.translatableWithFallback(key, fallback, args…)`.
- `net.minecraft.world.entity.monster.Enemy` là interface mob hostile. Implement: `Monster` (nên Zombie, Skeleton, Creeper, Piglin…), `Slime`/`MagmaCube` (`monster.cubemob`), `Ghast`, `Phantom`, `Shulker`, `Hoglin`, `EnderDragon`. **Không** là Enemy: `IronGolem`, `WitherBoss` (mặc định chưa kiểm cho mọi lớp; kiểm lại nếu cần).
- Quyền lệnh: `net.minecraft.server.permissions.Permissions`, `PermissionLevel`; `Commands.LEVEL_ALL/MODERATORS/GAMEMASTERS/ADMINS/OWNERS`.
- `GameTestHelper`: `makeMockServerPlayerInLevel()`, `spawn(EntityType, BlockPos|Vec3|…)`, `succeed()`, `fail(String)`, `assertTrue(boolean, String)`.

## Build (2026-11-11)

- `./gradlew :permahearts:build` xanh trên JDK 25.0.5: JUnit (`SmokeTest`) + 2 game test; lần đầu ~2 phút (tải Minecraft + thư viện), sau đó ~6 giây.
- `scripts/install.sh` chép `fabric-api-0.166.2+26.3.jar` và `permahearts-0.1.0.jar` vào mods/ của profile Essential; `Essential-1.5.2-26.3.jar` không đổi.
- Chưa kiểm: nạp mod trong Minecraft Launcher thật (cần Huy khởi động lại game; tìm dòng `PermaHearts loaded` trong `logs/latest.log` của profile).

## Thêm (khi viết P-001)

- `CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> …)`; `EntityArgument.players()` + `EntityArgument.getPlayers(ctx, "targets")`; `CommandSourceStack#sendSuccess(Supplier<Component>, boolean broadcastToOps)`, `#sendFailure(Component)`.
- Âm thanh: `level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 1f)` (`Level#playSound(Entity, BlockPos, SoundEvent, SoundSource, float, float)`); `ServerPlayer#level()` trả `ServerLevel`.

## Thêm (khi làm P-001)

- `EntityType.ZOMBIE`/`COW` không còn; hằng số nằm ở `net.minecraft.world.entity.EntityTypes` (`EntityTypes.ZOMBIE`, `EntityTypes.COW`). Zombie ở `monster.zombie.Zombie`, Cow ở `animal.cow.Cow`.
- `AttachmentType` có `identifier()` (không phải `id()`).
- `GameTestHelper#makeMockServerPlayerInLevel()` đã `@Deprecated(forRemoval)` nhưng vẫn chạy; kill bằng `hurtServer(level, damageSources().playerAttack(p), 1000f)` cho kill credit đúng người chơi mock (game test xanh).
- `HeartsData.CODEC` đặt trong `HeartsData` (để JUnit không nạp registry); `HeartsAttachment.CODEC` trỏ tới nó.

## Bẫy đã gặp: thứ tự `AFTER_RESPAWN` (2026-11-11, sửa ở 0.1.1)

- Fabric chép attachment cũ → người mới **trong chính listener `ServerPlayerEvents.AFTER_RESPAWN` của nó** (`AttachmentEntrypoint`, phase mặc định), không phải ở `COPY_FROM`. Listener của mod khác có thể chạy trước và bị ghi đè. Bản 0.1.0 dính lỗi này: chết không mất tim, thanh máu lệch dữ liệu.
- Cách đúng: `AFTER_RESPAWN.addPhaseOrdering(Event.DEFAULT_PHASE, myPhase)` + `register(myPhase, …)`, và đọc dữ liệu từ `oldPlayer`.
- Game test: mock player (`makeMockServerPlayerInLevel`) là **Creative** và client chưa "loaded" → bất tử với damage; muốn chết thì `setHealth(0)` + `die(source)`, rồi `server.getPlayerList().respawn(p, false, Entity.RemovalReason.KILLED)`. Mọi mock player trùng tên `test-mock-player` (game test chạy song song) và UUID là selector entity → dùng `server.createCommandSourceStack().withEntity(p)` + `@s`. Chạy lệnh qua `getDispatcher().execute` để lỗi làm fail test (`performPrefixedCommand` nuốt lỗi).
- Bộ game test hiện có (9): máu theo bonus, zombie tính/bò không, kill thứ 100 +1 tim, chết −1 tim + máu đầy + giữ tiến độ, sàn 1 tim, trần 30, người mới 10 tim, lệnh admin + từ chối 31, cùng 1 test hạ tầng.

## Thêm (khi làm P-002, HUD + đồng bộ)

- Đồng bộ attachment: `builder.syncWith(StreamCodec<? super RegistryFriendlyByteBuf, A>, AttachmentSyncPredicate.targetOnly())`; `StreamCodec<ByteBuf, HeartsData>` dựng bằng `StreamCodec.composite(ByteBufCodecs.VAR_INT, getter, ..., ctor)` (4 trường) chạy được. Đồng bộ qua `setAttached`; game test với mock player vẫn xanh. Client đọc bằng `player.getAttachedOrElse(type, default)`.
- HUD: `HudElementRegistry.attachElementAfter(VanillaHudElements.CHAT, Identifier, HudElement)` (`net.fabricmc.fabric.api.client.rendering.v1.hud`); `HudElement#extractRenderState(GuiGraphicsExtractor, DeltaTracker)`.
- `GuiGraphicsExtractor` (thay `GuiGraphics`): `fill(x1,y1,x2,y2,color)`, `outline(x, y, width, height, color)`, `text(Font, Component|String, x, y, color, boolean shadow)`, `guiHeight()`.
- `Options.hideGui` không còn: dùng `Minecraft.getInstance().gui.hud.isHidden()` (`net.minecraft.client.gui.Hud`). `Player#isCreative()`, `#isSpectator()` còn.
- Source set `src/client` biên dịch cùng `build`; HUD chỉ được biên dịch, chưa thấy trong game. `WitherBoss`/`Warden` đều `extends Monster`; `EntityTypes.WARDEN` spawn và giết được trong game test (+500 XP).

## Thêm (khi làm P-003, game rule bật/tắt)

- Game rule: `GameRuleBuilder.forBoolean(false).category(GameRuleCategory.PLAYER).buildAndRegister(Identifier)` (`net.fabricmc.fabric.api.gamerule.v1`) → `net.minecraft.world.level.gamerules.GameRule<Boolean>`. Đọc `server.getGameRules().get(rule)`, ghi `GameRules#set(rule, value, server)`; `GameRuleEvents.changeCallback(rule).register((Boolean v, MinecraftServer s) -> …)`.
- `GameRules#set` gọi từ game test CÓ bắn change callback (kiểm bằng game test `toggleUpdatesOnlinePlayers`). Cần `/gamerule` trong game thật vẫn chưa thấy.
- `ByteBufCodecs.BOOL` là `StreamCodec<ByteBuf, Boolean>`; attachment không persistent + `syncWith(BOOL, targetOnly())` chạy được (mock player xanh).
- Khoá dịch: `GameRule#getDescriptionId()` = `gamerule.<ns>.<path>`; vanilla còn khoá `gamerule.<tên>.description` (đã thêm cả hai).
- Game test chạy chung một server nên dùng chung game rule: mỗi test đổi rule phải trả về `true` trong `finally`; mọi test cũ gọi `enable(context, true)` trước (mặc định rule là `false`).

## Thêm (khi làm P-004, linh dược)

- Đăng ký item: `Registry.register(BuiltInRegistries.ITEM, key, new Item(props.setId(key)))`; `Item.Properties`: `setId`, `stacksTo`, `rarity`, `component(type, v)`, `usingConvertsTo(Item)`. Uống được: `DataComponents.CONSUMABLE` = `Consumables.DEFAULT_DRINK`, glint = `DataComponents.ENCHANTMENT_GLINT_OVERRIDE`. Đúng như plan, biên dịch và chạy được.
- `Item#finishUsingItem(ItemStack, Level, LivingEntity)` gọi trực tiếp được từ game test với mock player (không cần `drink()` riêng); `appendHoverText(ItemStack, Item.TooltipContext, TooltipDisplay, Consumer<Component>, TooltipFlag)`.
- Creative tab: `CreativeModeTabEvents.modifyOutputEvent(key).register(out -> out.accept(ItemLike))` (`out` là `FabricCreativeModeTabOutput`, `accept(ItemLike)` kế thừa từ `CreativeModeTab.Output`); key `Identifier.withDefaultNamespace("food_and_drinks")`. Biên dịch được, chưa thấy trong game.
- Model item không cần texture: `assets/<ns>/items/<id>.json` với `minecraft:model` `minecraft:item/potion` + `tints` `minecraft:constant` (số nguyên ARGB có dấu). Chưa thấy trong game.
- Modifier vĩnh viễn trên `ATTACK_DAMAGE`, `MOVEMENT_SPEED`, `ARMOR`, `BLOCK/ENTITY_INTERACTION_RANGE`... sống qua respawn (game test `elixirKeptOnDeath`).


## Thêm (khi làm P-005, Lữ Khách)

- `SavedData`: `new SavedDataType<>(Identifier, Supplier<T>, Codec<T>, DataFixTypes)`; `DataFixTypes` truyền `null` chạy được (game test lưu/đọc xanh). Lấy bằng `server.getDataStorage().computeIfAbsent(TYPE)`; ghi xong gọi `setDirty()`.
- Ngày: `server.overworld().getOverworldClockTime() / 24000L` (có trên `Level`).
- Spawn: `EntityTypes.WANDERING_TRADER.create(level, EntitySpawnReason.EVENT)` → `snapTo(x,y,z,yaw,pitch)`, `setNoAi`, `setPersistenceRequired`, `setDespawnDelay(0)`, `setCustomName`, `level.addFreshEntity`. Tên hiển thị: `Item#getDescriptionId()` (final) và `EntityType#getDescription()`; `Registry#getValue(Identifier)`, `EntityType.getKey(type)` (tĩnh).
- Sự kiện Fabric: `UseEntityCallback.EVENT` `(Player, Level, InteractionHand, Entity, EntityHitResult) -> InteractionResult`; `ServerLivingEntityEvents.ALLOW_DAMAGE (LivingEntity, DamageSource, float) -> boolean`; `ServerEntityEvents.ENTITY_LOAD (Entity, ServerLevel)`.
- Menu có dữ liệu mở: `new ExtendedMenuType<>((int id, Inventory inv, D data) -> menu, StreamCodec<? super RegistryFriendlyByteBuf, D>)`; mở bằng `player.openMenu(ExtendedMenuProvider<D>)` (`getScreenOpeningData(ServerPlayer)`, `getDisplayName()`, `createMenu(int, Inventory, Player)`).
- Menu: `AbstractContainerMenu(MenuType, int)`; `addDataSlot(DataSlot)` (server dùng lớp con ẩn danh đọc giá trị sống, client dùng `DataSlot.standalone()`); `addStandardInventorySlots(Container, x, y)`; `Slot#isActive()`; `slotsChanged(Container)`, `clearContainer(Player, Container)`, `clickMenuButton(Player, int)`; `moveItemStackTo`. Nút client: `minecraft.gameMode.handleInventoryButtonClick(containerId, id)`.
- Đăng ký màn hình: `MenuScreens.register(menuType, Screen::new)` dùng được trực tiếp (Fabric `fabric-transitive-access-wideners-v1` mở sẵn `register` + `ScreenConstructor`), không cần class tweaker riêng.
- Màn hình: `AbstractContainerScreen(menu, inv, title, imageWidth, imageHeight)`; vẽ nền trong `extractBackground(GuiGraphicsExtractor, mx, my, dt)` (chạy trước widget và ô), chữ trong `extractLabels(g, mx, my)` (đã dịch về góc trên-trái bảng), ô đồ do `extractSlots`. `g.fill`, `g.text(font, Component|FormattedCharSequence, x, y, argb, shadow)`, `g.textWithWordWrap(font, FormattedText, x, y, width, color)`, `g.fakeItem(ItemStack, x, y)`, `font.split(FormattedText, width)`; `Button.builder(text, onPress).bounds(x,y,w,h).build()`; `rebuildWidgets()`. Chỉ biên dịch, chưa thấy trong game.
- Game test: mọi test chạy xong trong một lần gọi nên không xen nhau; trạng thái Lữ Khách dùng chung server nên mỗi test `Wanderer.reset` đầu và trong `finally`.

## Thêm (khi làm P-006, sự kiện nhiệm vụ)

- Đồng hồ: `server.clockManager()` (`ServerClockManager`, gói `net.minecraft.world.clock`); holder = `server.registryAccess().lookupOrThrow(Registries.WORLD_CLOCK).getOrThrow(WorldClocks.OVERWORLD)`; `moveToTimeMarker(holder, ClockTimeMarkers.NIGHT|DAY)` và `isAtTimeMarker(holder, marker)` chạy đúng ngay lập tức. `Level#isBrightOutside/isDarkOutside` dựa trên `skyDarken` chỉ cập nhật khi tick thật, nên ngay sau khi chuyển đồng hồ trong cùng một lần gọi (game test) vẫn trả giá trị cũ.
- Thanh boss: `new ServerBossEvent(UUID, Component, BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS)`, `setProgress`, `setDarkenScreen(true)`, `setCreateWorldFog(true)`, `addPlayer`, `removePlayer`, `removeAllPlayers`.
- Ngủ: `EntitySleepEvents.ALLOW_SLEEPING.register((Player, BlockPos) -> Player.BedSleepingProblem | null)`, `new Player.BedSleepingProblem(Component)`. `ServerPlayerEvents.LEAVE (ServerPlayer)`. `ServerLivingEntityEvents.AFTER_DEATH` bắn cả khi `ServerPlayer.die` (game test xanh).
- Entity: `Entity#entityTags()`, `addTag`; `ServerLevel#getEntities(EntityTypeTest.forClass(X.class), Predicate)`; `Mob#finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.EVENT, null)`, `Mob#setDropChance(EquipmentSlot, float)`, `LivingEntity#setItemSlot` public; thuộc tính `Attributes.SCALE`, `MAX_HEALTH`, `ATTACK_DAMAGE`; `Zombie#setBaby(false)`.
- Công trình: `level.structureManager().getStructureWithPieceAt(BlockPos, Predicate<Holder<Structure>>).isValid()`, `h.is(ResourceKey|TagKey)`; `StructureTags.VILLAGE/MINESHAFT/RUINED_PORTAL/SHIPWRECK`. `BuiltinStructures` không có khoá làng: dùng tag `#minecraft:village`. Chưa thử trong world thật, chỉ biên dịch.
- Game test: mock player ghi đè `isCreative()` (luôn true) và `setGameMode(SURVIVAL)` không đổi `gameMode()`; vì vậy `BloodMoon.wave(server, players)` nhận danh sách đã lọc, còn `tick` tự lọc creative/spectator.

## Thêm (khi làm P-007, client game test)

- Bật bằng `fabricApi.configureTests { enableClientGameTests = true }` → task `runClientGameTest` (run dir `mods/<id>/build/run/clientGameTest/`, ảnh ở `screenshots/`, đánh số `0000_<tên>.png`). `build` và `check` KHÔNG phụ thuộc task này. Entrypoint `fabric-client-gametest` trong `src/gametest/resources/fabric.mod.json`; class implement `FabricClientGameTest#runTest(ClientGameTestContext)`, biên dịch được trong source set `gametest` (thấy cả class client).
- `context.worldBuilder().create()` → `TestSingleplayerContext` (try-with-resources); `.getServer()` → `TestServerContext` có `runCommand(String)` (quyền op), `runOnServer(server -> …)`, `computeOnServer`. `ClientGameTestContext`: `waitTicks(n)`, `waitFor(Predicate<Minecraft>)`, `waitForScreen(Class)`, `runOnClient`, `computeOnClient`, `takeScreenshot(String)`, `getInput()`.
- `TestInput`: `pressKey(o -> o.keyUse)` mở được menu của NPC; `pressMouse(1)` thì KHÔNG (không có gì xảy ra). `setCursorPos(x, y)` tính bằng pixel cửa sổ (nhân `mc.getWindow().getGuiScale()` từ tọa độ GUI) → rê chuột lên ô để hiện tooltip.
- `tp @s … facing entity …` ngay sau khi spawn không xoay đúng người chơi; dùng `mc.player.setYRot/setXRot` trong `runOnClient` rồi chờ ~20 tick cho `mc.hitResult` bắt được entity.
- Đổi ngôn ngữ giữa chừng: `mc.options.languageCode = "vi_vn"` + `mc.getLanguageManager().setSelected("vi_vn")` + `mc.reloadResourcePacks()`. KHÔNG `join()` trong `runOnClient` (treo luồng render): lấy future bằng `computeOnClient`, `waitFor(mc -> f.isDone())`, rồi `waitTicks(~100)` cho màn Mojang tan (đang hiện thì chặn input). Tin nhắn từ server tích hợp vẫn tiếng Anh.
- `Minecraft#setScreen` không còn (có `setScreenAndShow`); đóng menu bằng `mc.player.closeContainer()`. Cửa sổ test mặc định 854x480.
