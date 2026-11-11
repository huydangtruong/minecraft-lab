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
