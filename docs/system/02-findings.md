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

## Entity renderer swap (26.3, Fabric API 0.166.2)

- `EntityRendererRegistry.register(EntityTypes.WANDERING_TRADER, WandererRenderer::new)` replaces the vanilla renderer; `WandererRenderer extends WanderingTraderRenderer` overrides `extractRenderState(WanderingTrader, VillagerRenderState, float)` and `getTextureLocation(VillagerRenderState)`.
- Per-entity flags travel on the render state with Fabric's `RenderStateDataKey.create(Supplier<String>)` + `((FabricRenderState) state).setData/getDataOrDefault`.
- The Wanderer texture `assets/permahearts/textures/entity/wanderer.png` is the vanilla trader texture with the blue robe hue-shifted to crimson (pixels with hue 170–260°).
- `GuiGraphicsExtractor#textWithWordWrap` draws with a drop shadow; on a light grey vanilla panel that looks smudged. Split with `font.split` and draw `text(..., false)` instead.

## Thêm (khi làm P-008)

- **Heightmap trong hang**: `level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z)` trả mặt đất phía TRÊN hang, có thể cao hàng chục block; thanh boss chỉ hiện trong 64 block nên trùm "mất tích". Dùng `SpawnSpots.standSpot(level, origin, dx, dz)`: tìm tại Y của điểm gốc ±4, cần khối đặc bên dưới + 2 khối trống.
- `Entity#setPermanentlyInvulnerable(boolean)` (không có `setInvulnerable`). `TargetingConditions#test(ServerLevel, LivingEntity, LivingEntity)` gọi `LivingEntity#canBeSeenAsEnemy()` (javap -c xác nhận), mà hàm này là `!isInvulnerable() && canBeSeenByAnyone()`: NPC bất tử vĩnh viễn thì quái không nhắm được (game test `npcIsNotATarget`). Hệ quả: `/kill` không giết được NPC; `/quest reset` vẫn xóa.
- `ItemStack#set(DataComponents.UNBREAKABLE, net.minecraft.util.Unit.INSTANCE)`: mũ không vỡ nên zombie/skeleton không cháy nắng.
- Game test chạy chung một server, đồng thời, dùng chung trạng thái Lữ Khách: chỉ để test ngắn (≤40 tick); kiểm 100+ tick làm trong client game test.
- `ServerEntityEvents.ENTITY_LOAD` bắn ngay trong `addFreshEntity`: phải lưu `BossState` TRƯỚC khi thêm trùm, nếu không hook xóa trùm lạ sẽ xóa chính nó.
- Client test: nút chuột trái trong `MouseButtonInfo` là **1** (`AbstractWidget#isValidClickButton` so `button() == 1`); `mc.gui.screen()` thay `mc.screen`. `TestServerContext#runCommand` không có người thực thi: `@s` chọn không ai (lệnh "No player was found"), dùng `@p` hoặc `runOnServer`.
- **Kết quả kiểm "boss biến mất sau ~40 tick" (flat world)**: không tái hiện được khi người chơi còn sống. Cả 5 loại trùm (zombie, skeleton, evoker, blaze, vindicator) còn sống sau 200 tick, `ENTITY_UNLOAD` chỉ bắn khi test tự xóa trùm. Nguyên nhân khả dĩ: người chơi trong test ở chế độ sinh tồn (`gamemode creative @s` qua `runCommand` không chọn ai) bị trùm giết ("Player0 was slain by Zombie") → không còn người chơi giữ chunk → chunk đổi trạng thái → `stopTracking`, `removalReason == null`, đúng chữ ký đã thấy. Khi đó thấy `player=null` trong log.

## Installer, release (P-009)

- **JSON on macOS: `plutil` is enough** (no JXA fallback needed). `plutil -extract <keypath> raw -o - <file|->` works on GitHub-shaped release JSON: array index in the key path (`0.assets.1.browser_download_url`), `null` values elsewhere in the document are fine, unicode escapes and `\n` in `body` are fine. Limits: `raw` fails on a value that is `null` or an object/array (treated as "missing"), there is no array-length call (loop until the key path fails), and `-extract . raw` is invalid. `plutil -extract profiles xml1 -o - file` lists the dict keys of `launcher_profiles.json` as `\t<key>..</key>` lines (one tab = direct child); keys containing `.` cannot be used in a key path and are skipped. Not yet run against the live GitHub API (no release existed when written); fixture mimics its shape.
- bash 3.2: `read -u 3` with `exec 3<file` works; `sort -r` + a `\037` field separator avoids IFS whitespace collapsing empty fields; `printf '%-*s'` works.
- `jar` id lookup: `unzip -p <jar> fabric.mod.json | plutil -extract id raw -o - -`; `scripts/common.sh` `remove_mod_jars` uses grep on the same file (so `install.sh` deletes old `permahearts-*.jar` and `immortal-cultivation-*.jar` alike, by id).
- **Windows PowerShell 5.1 (written, NOT run: no pwsh here)**: `Invoke-WebRequest` needs `-UseBasicParsing` and `$ProgressPreference='SilentlyContinue'` (progress bar makes downloads very slow); `ConvertFrom-Json` returns ISO dates as strings (7+ returns DateTime), so dates are parsed explicitly; a single-element JSON array is unrolled, hence `@(...)`; `file://` is not supported by `Invoke-WebRequest`, the script copies those (test only); keep the file ASCII-only because the `irm | iex` and file paths decode differently.
- Release asset names come from `release.json` (`jar` field) and are matched against the release's `assets[].name`; the installer rejects a jar name with `/`, `\` or `..`.
- Launcher profile keys can contain dots: the Essential installer names its profile `Essential-installer-2026-11-11T15:39:43.477440Z`. `plutil -extract profiles.<key>…` cannot address such keys, so `minecraft-lab.command` converts the `profiles` dict once with `plutil -extract profiles xml1` and walks the XML with awk. Found by running the installer read-only against Huy's real `launcher_profiles.json` (P-009 review).
- Windows PowerShell 5.1: `'[…]' | ConvertFrom-Json` and `Invoke-RestMethod` emit a JSON array as one object, so `@(... | ConvertFrom-Json)` gives a 1-element array holding the array. Assign the result directly and let `foreach` unroll it.
- Titles (0.8.1): `ClientboundSetTitlesAnimationPacket(int fadeIn, int stay, int fadeOut)`, `ClientboundSetSubtitleTextPacket(Component)`, `ClientboundSetTitleTextPacket(Component)` sent with `ServerPlayer#connection.send`; send the subtitle before the title. A server-wide `ServerBossEvent` must be resynced with the online list every pass: respawn creates a new `ServerPlayer` object (the old one stays in `getPlayers()`). Client side the bars live in `Gui#hud.getBossOverlay()` (`BossHealthOverlay`, map `events` is private).

## Zombie horseman (26.3)

- `ZombieHorse` (`net.minecraft.world.entity.animal.equine`): `finalizeSpawn` with `EntitySpawnReason.NATURAL` creates the rider itself: `EntityTypes.ZOMBIE.create(level, JOCKEY)`, `snapTo`, `finalizeSpawn`, `setItemSlot(MAINHAND, new ItemStack(Items.IRON_SPEAR))`, `startRiding(horse, false, false)`. With `EVENT` no rider is made, so the Blood Moon builds it the same way and adds both with `ServerLevel#tryAddFreshEntityWithPassengers`.
- Spears: `Items.WOODEN_SPEAR` … `NETHERITE_SPEAR`; zombies use them through `SpearUseGoal`.

## Eating hook (26.3)

- Food is applied in `FoodProperties#onConsume(Level, LivingEntity, ItemStack, Consumable)` (a `ConsumableListener`): it calls `player.getFoodData().eat(this)`. Fabric API 0.166.2 has no "player ate" event, so `FoodPropertiesMixin` injects at `TAIL` (first mixin of the mod, `permahearts.mixins.json`). Runs on both sides: check `instanceof ServerPlayer`. A game test can eat with `item.finishUsingItem(new ItemStack(item), level, player)`.

## Inventory screen (26.3)

- Drawing is `GuiGraphicsExtractor` (`fill`, `text(font, String, x, y, color, shadow)`, `item(stack, x, y)`); widgets implement `extractContents`. `Button` is abstract: subclass with `super(x, y, w, h, msg, onPress, DEFAULT_NARRATION)` and call `extractDefaultSprite(g)`.
- Fabric screen API 5.2.4: `ScreenEvents.AFTER_INIT`, per-screen `afterBackground/afterExtract/beforeTick`, `Screens.getWidgets(screen)` (mutable, adding registers the widget).
- `InventoryScreen extends AbstractRecipeBookScreen`: field `recipeBookComponent` (`isVisible`, `toggleVisibility`, `updateScreenPosition`), recipe button at `(leftPos + 104, height / 2 - 22)`; `leftPos`, `topPos`, `imageWidth` on `AbstractContainerScreen` (client accessors in `permahearts.client.mixins.json`). `hasClickedOutside(double, double, int, int)` decides item drops.
- Minecraft: the current screen is `mc.gui.screen()`; `Minecraft#setScreen`. A creative player opening `InventoryScreen` gets `CreativeModeInventoryScreen`. Client test: `context.setScreen(supplier)`, `context.clickScreenButton(label)`.
- Respawn: `ServerPlayer#getRespawnConfig()` (null = none) → `respawnData()` (`pos()`, `dimension()`); world spawn `MinecraftServer#getRespawnData()`. Attribute modifier ops in order `ADD_VALUE`, `ADD_MULTIPLIED_BASE` (multiplies base + flat adds), `ADD_MULTIPLIED_TOTAL`.

## Blood Moon arena (D33)

- `GameTestHelper#makeMockServerPlayerInLevel` players always answer `isCreative() == true`, whatever `setGameMode` says; code that skips creative players needs a test switch (`BloodMoon.barrier(server, players, checkGameMode)`).
- Keeping a player in place: `ServerPlayer#teleportTo(ServerLevel, x, y, z, Set.of(), yRot, xRot, false)` after `stopRiding()`, then `setDeltaMovement(Vec3.ZERO)`. Red dust: `new DustParticleOptions(0xRRGGBB, scale)` with `ServerLevel#sendParticles`.
- **Ghast AI (26.3)**: `Ghast$RandomFloatAroundGoal` only picks a random point within ±16 blocks of where it is (a point under the `MOTION_BLOCKING` heightmap is pushed up, so in the open Overworld it drifts upward) and never follows a target. It keeps a wanted position set through `getMoveControl().setWantedPosition(x, y, z, 1.0)` while that point is 1..60 blocks away. Its target goal only accepts players within **4 blocks of its own height** (`NearestAttackableTargetGoal` with `|dy| <= 4`, must see): a ghast hovering above a player has to be given its target with `setTarget`. `Ghast$GhastShootFireballGoal` (package-private, public `int chargeTime`): +1 per tick while the target is within 64 blocks and in sight, warning sound at 10, shot at 20, then `chargeTime = -40` (one shot per 60 ticks); changed per entity with `@ModifyConstant(intValue = -40)` on `tick`. `Mob#setTarget` on a mock game-test player does nothing (it counts as creative); aim at a pig. `Mob#setHomeTo(BlockPos, int)` exists (spherical), not used.
- **Heart HUD (26.3)**: drawn by `Hud#extractPlayerHealth` (private), which computes rows = ceil((max(MAX_HEALTH, health) + absorption) / 20), row spacing max(10 - (rows - 2), 3), places the armour bar above all rows (`extractArmor(g, player, guiHeight - 39, rows, rowSpacing, x)`) and calls `extractHearts(...)`. Fabric API wraps each call as a HUD element: `HudElementRegistry.replaceElement(VanillaHudElements.HEALTH_BAR / ARMOR_BAR, vanilla -> element)`. Heart sprites: `minecraft:hud/heart/{container, container_hardcore, full, half, hardcore_full, poisoned_full, withered_full, frozen_full, absorbing_full, absorbing_hardcore_full, ...}`; draw with `g.blitSprite(RenderPipelines.GUI_TEXTURED, id, x, y, 9, 9)`. Our own sprites in `assets/<mod>/textures/gui/sprites/hud/heart/*.png` load into the GUI atlas as `<mod>:hud/heart/*`. `HudStatusBarHeightRegistry.replaceLeft` alone does nothing: Fabric only applies status bar offsets when the provider maps differ from vanilla (some mod added a bar), and it checks that before applying replacers, so the armour bar must be moved by hand (pose translate in a replaced `ARMOR_BAR`). Chat: `mc.gui.hud.getChat().clearMessages(false)`.

## Techniques tab (P-011a)

- **C2S payload**: `PayloadTypeRegistry.serverboundPlay().register(Type, StreamCodec<RegistryFriendlyByteBuf, T>)` in `onInitialize` (runs on both sides), `ServerPlayNetworking.registerGlobalReceiver(Type, (payload, ctx) -> ctx.player() ...)` (runs on the server thread), client `ClientPlayNetworking.send(payload)` (net.fabricmc.fabric.api.client.networking.v1). Payload `Type`: `new CustomPacketPayload.Type<>(Identifier)`.
- **StreamCodec for maps and lists**: `ByteBufCodecs.<ByteBuf, K, V, Map<K, V>>map(HashMap::new, keyCodec, valueCodec)` (explicit type args needed), `ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(maxSize))`, `ByteBufCodecs.VAR_LONG`. `StreamCodec.composite` works with 5 fields.
- **Cursor stack**: `player.containerMenu.getCarried()` / `setCarried(ItemStack)`; after changing it on the server call `containerMenu.broadcastChanges()` so the client sees it (`getCarried().shrink(n)` is enough for a deposit). `Player#experienceLevel / experienceProgress / totalExperience` are public fields; `giveExperienceLevels(-n)` takes levels away and keeps the progress bar.
- **Screen input**: `ScreenMouseEvents.allowMouseClick(screen)` gets `(Screen, MouseButtonEvent)` with `x()`, `y()` in GUI coordinates and `button()` (0 left, 1 right); return false to swallow the click. `allowMouseScroll(screen)` gets `(Screen, mouseX, mouseY, horizontal, vertical)`. These fire only for real input (client game test `getInput().setCursorPos(guiX * guiScale, guiY * guiScale)` then `pressMouse(0)`), not for a direct `screen.mouseClicked(...)` call. `Minecraft#gui.screen()` is the current screen (no `mc.screen` field).
- **GUI drawing**: `g.blitSprite(RenderPipelines.GUI_TEXTURED, id, x, y, w, h)` works for our own `textures/gui/sprites/technique/*.png` (id `permahearts:technique/fire`) and for vanilla `minecraft:widget/button`, `widget/button_highlighted`, `widget/button_disabled` at any size (nine-slice). `g.enableScissor(x0, y0, x1, y1)` / `disableScissor()`, `g.setTooltipForNextFrame(font, Component, mx, my)` works from `ScreenEvents.afterBackground`. `font.split(Component, width)` returns `List<FormattedCharSequence>`.
- **Commands**: `EntityArgument.player()` rejects `@a` and `@e` at parse time ("only one player"); use `@p` / `@s` (client-test `runCommand` has no entity, so `@s` selects nobody).

