package io.github.huydangtruong.immortalcultivation.treasure;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.huydangtruong.immortalcultivation.ImmortalCultivation;
import io.github.huydangtruong.immortalcultivation.reward.RewardBoxes;
import io.github.huydangtruong.immortalcultivation.reward.RewardOption;
import io.github.huydangtruong.immortalcultivation.wanderer.SpawnSpots;
import io.github.huydangtruong.immortalcultivation.wanderer.Wanderer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.random.RandomGenerator;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Deadly Treasure (P-059, D150): 2 days after the Crimson Master or the God Slayer left, a chest guarded by Mega mobs
 * appears ~1000 blocks from a player (60% in a cave, else on the surface; underwater when the surface is deep water).
 * The chest opens only when every guard is dead. After 1 day with guards left, chest and guards vanish (failed).
 */
public final class DeadlyTreasure {
	/** Blue-violet, the colour of every Deadly Treasure message. */
	public static final int COLOR = 0x7B6BFF;
	private static final RandomGenerator RNG = new java.util.Random();

	private DeadlyTreasure() {}

	public static TreasureSavedData data(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TreasureSavedData.TYPE);
	}

	public static void register() {
		MegaMobs.init();
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 == 0 && ImmortalCultivation.enabled(server)) {
				tick(server);
			}
		});
		ServerLivingEntityEvents.AFTER_DEATH.register(DeadlyTreasure::onDeath);
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			MegaMobs.Mega m = MegaMobs.mega(entity);
			if (m != null && m.treasure() != 0) {
				TreasureSavedData d = data(level.getServer());
				if (d.phase != TreasureRules.Phase.ACTIVE || d.id != m.treasure()) {
					entity.discard(); // a guard of an event that is over
				}
			}
		});
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			if (world instanceof ServerLevel sl && locked(sl, hit.getBlockPos())) {
				int left = data(sl.getServer()).guards.size();
				player.sendOverlayMessage(text(left + " Mega guard" + (left == 1 ? "" : "s") + " still protect this treasure"));
				return InteractionResult.FAIL;
			}
			return InteractionResult.PASS;
		});
		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, be) -> !(world instanceof ServerLevel sl && locked(sl, pos)));
		CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> registerCommand(dispatcher));
	}

	/** The guarded chest of the running event: cannot be opened or broken. */
	public static boolean locked(ServerLevel level, BlockPos pos) {
		if (level != level.getServer().overworld()) {
			return false;
		}
		TreasureSavedData d = data(level.getServer());
		return d.phase == TreasureRules.Phase.ACTIVE && d.placed && d.chest == pos.asLong() && !d.guards.isEmpty();
	}

	/** D172: the Crimson Master left, so the treasure takes its next visit day (if nothing is pending or running). */
	public static void onCrimsonMasterLeft(MinecraftServer server) {
		TreasureSavedData d = data(server);
		if (d.phase == TreasureRules.Phase.IDLE) {
			d.startAt = TreasureRules.slotStart(Wanderer.data(server).nextDay());
			d.phase = TreasureRules.Phase.PENDING;
			d.setDirty();
		}
	}

	/** D172: the Crimson Master skips its visit day while the treasure waits for it or runs. */
	public static boolean takesCrimsonSlot(MinecraftServer server) {
		return TreasureRules.crimsonSkips(data(server).phase);
	}

	// ---- life cycle ----

	public static void tick(MinecraftServer server) {
		TreasureSavedData d = data(server);
		ServerLevel ow = server.overworld();
		long clock = Wanderer.clockTime(server);
		cleanup(ow, d);
		switch (d.phase) {
			case IDLE -> {
			}
			case PENDING -> {
				if (clock >= d.startAt) {
					List<ServerPlayer> players = ow.players().stream().filter(p -> !p.isSpectator()).toList();
					if (!players.isEmpty()) {
						start(server, players.get(RNG.nextInt(players.size())), true);
					}
				}
			}
			case ACTIVE -> {
				if (clock >= d.endAt) {
					fail(server, d);
				} else if (!d.placed) {
					for (ServerPlayer p : ow.players()) {
						double dx = p.getX() - d.x;
						double dz = p.getZ() - d.z;
						if (dx * dx + dz * dz <= TreasureRules.PLACE_NEAR * TreasureRules.PLACE_NEAR
							&& ow.isPositionEntityTicking(new BlockPos(d.x, p.getBlockY(), d.z))) {
							place(ow, d, new BlockPos(d.x, p.getBlockY(), d.z), null, null);
							break;
						}
					}
				} else {
					guardTick(ow, d);
				}
			}
		}
	}

	/** Starts the event: far = 1000 blocks from the player in a random direction, else at the player (tests, command). */
	public static void start(MinecraftServer server, ServerPlayer near, boolean far) {
		TreasureSavedData d = data(server);
		double a = RNG.nextDouble() * Math.PI * 2;
		int dist = far ? TreasureRules.DISTANCE : 0;
		d.id++;
		d.phase = TreasureRules.Phase.ACTIVE;
		d.x = (int) Math.floor(near.getX() + Math.cos(a) * dist);
		d.z = (int) Math.floor(near.getZ() + Math.sin(a) * dist);
		d.endAt = Wanderer.clockTime(server) + TreasureRules.DURATION_TICKS;
		d.placed = false;
		d.guards.clear();
		d.setDirty();
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			title(p, text("lies near X " + d.x + ", Z " + d.z + " (1 day)"));
		}
		server.getPlayerList().broadcastSystemMessage(text("Deadly Treasure: a guarded chest lies near X " + d.x + ", Z " + d.z
			+ ". Kill every Mega guard to open it. It vanishes in 1 day."), false);
	}

	public record Spot(BlockPos chest, boolean water, boolean cave) {}

	/**
	 * Places chest, loot and guards near origin. cave/water null = roll (60% cave; deep water at the surface = underwater).
	 * groupId null = random group of the right kind.
	 */
	public static Spot place(ServerLevel level, TreasureSavedData d, BlockPos origin, Boolean cave, String groupId) {
		Spot spot = findSpot(level, origin, cave != null ? cave : RNG.nextDouble() < TreasureRules.CAVE_CHANCE);
		TreasureRules.Group group = groupId != null ? TreasureRules.group(groupId) : TreasureRules.pickGroup(spot.water(), RNG);
		BlockPos pos = spot.chest();
		if (!spot.water()) {
			BlockPos below = pos.below();
			if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
				level.setBlock(below, Blocks.COBBLESTONE.defaultBlockState(), 3);
			}
			level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
		}
		level.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.WATERLOGGED, spot.water()), 3);
		d.placed = true;
		d.chest = pos.asLong();
		d.group = group.id();
		d.guards.clear();
		for (TreasureRules.Guard g : group.guards()) {
			for (int i = 0; i < g.count(); i++) {
				Mob m = MegaMobs.create(level, g, guardSpot(level, pos, spot.water()), d.id, pos);
				if (m != null && level.addFreshEntity(m)) {
					d.guards.add(m.getUUID().toString());
				}
			}
		}
		d.setDirty();
		String where = spot.water() ? "underwater" : spot.cave() ? "in a cave" : "on the surface";
		level.getServer().getPlayerList().broadcastSystemMessage(text("Deadly Treasure found " + where + " at X " + pos.getX() + ", Y "
			+ pos.getY() + ", Z " + pos.getZ() + ", guarded by " + d.guards.size() + " " + group.name() + "."), false);
		if (d.guards.isEmpty()) {
			success(level.getServer(), d);
		}
		return spot;
	}

	private static BlockPos guardSpot(ServerLevel level, BlockPos chest, boolean water) {
		for (int tries = 0; tries < 20; tries++) {
			int dx = RNG.nextInt(9) - 4;
			int dz = RNG.nextInt(9) - 4;
			if (Math.abs(dx) < 2 && Math.abs(dz) < 2) {
				continue;
			}
			if (water) {
				BlockPos p = chest.offset(dx, RNG.nextInt(3), dz);
				if (level.getFluidState(p).is(FluidTags.WATER) && level.getBlockState(p).getCollisionShape(level, p).isEmpty()) {
					return p;
				}
			} else {
				BlockPos p = SpawnSpots.standSpot(level, chest, dx, dz);
				if (!p.equals(chest)) {
					return p;
				}
			}
		}
		return chest.above();
	}

	/** Cave: a roomy dark air pocket within 32 blocks; else the surface (underwater if the water is 3+ deep). */
	static Spot findSpot(ServerLevel level, BlockPos origin, boolean wantCave) {
		if (wantCave) {
			BlockPos cave = findCave(level, origin);
			if (cave != null) {
				return new Spot(cave, false, true);
			}
		}
		int x = origin.getX();
		int z = origin.getZ();
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		BlockPos top = new BlockPos(x, y, z);
		if (level.getFluidState(top.below()).is(FluidTags.WATER)) {
			BlockPos floor = top.below();
			int depth = 0;
			while (level.getFluidState(floor).is(FluidTags.WATER) && floor.getY() > level.getMinY() + 1) {
				floor = floor.below();
				depth++;
			}
			if (depth >= 3) {
				return new Spot(floor.above(), true, false);
			}
			return new Spot(top, false, false); // shallow: the chest stands on the water (cobblestone under it)
		}
		return new Spot(top, false, false);
	}

	private static BlockPos findCave(ServerLevel level, BlockPos origin) {
		for (int tries = 0; tries < 160; tries++) {
			int x = origin.getX() + RNG.nextInt(65) - 32;
			int z = origin.getZ() + RNG.nextInt(65) - 32;
			int top = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z) - 8;
			for (int y = top; y > level.getMinY() + 6; y--) {
				BlockPos p = new BlockPos(x, y, z);
				BlockState here = level.getBlockState(p);
				if (!here.isAir()) {
					continue;
				}
				BlockPos below = p.below();
				if (level.getBlockState(below).isFaceSturdy(level, below, Direction.UP) && roomy(level, p)) {
					return p;
				}
			}
		}
		return null;
	}

	/** At least ~75% air in the 7x4x7 box above the floor, no water or lava, no sky. */
	private static boolean roomy(ServerLevel level, BlockPos floor) {
		if (level.canSeeSky(floor)) {
			return false;
		}
		int air = 0;
		for (int dx = -3; dx <= 3; dx++) {
			for (int dy = 0; dy < 4; dy++) {
				for (int dz = -3; dz <= 3; dz++) {
					BlockPos p = floor.offset(dx, dy, dz);
					if (!level.getFluidState(p).isEmpty()) {
						return false;
					}
					if (level.getBlockState(p).isAir()) {
						air++;
					}
				}
			}
		}
		return air >= 147;
	}

	/**
	 * The chest stays empty while locked (nothing for hoppers, golems or explosions to take); the loot goes in when the
	 * last guard dies. A chest blown up or otherwise removed comes back.
	 */
	private static ChestBlockEntity ensureChest(ServerLevel level, BlockPos pos) {
		if (!(level.getBlockEntity(pos) instanceof ChestBlockEntity)) {
			boolean water = level.getFluidState(pos).is(FluidTags.WATER);
			level.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.WATERLOGGED, water), 3);
		}
		return level.getBlockEntity(pos) instanceof ChestBlockEntity chest ? chest : null;
	}

	private static void fillLoot(ServerLevel level, BlockPos pos) {
		level.getChunkAt(pos); // loads the chunk if the last guard died far away
		ChestBlockEntity chest = ensureChest(level, pos);
		if (chest == null) {
			return;
		}
		List<Integer> slots = new ArrayList<>();
		for (int i = 0; i < chest.getContainerSize(); i++) {
			if (chest.getItem(i).isEmpty()) {
				slots.add(i);
			}
		}
		java.util.Collections.shuffle(slots, new java.util.Random(RNG.nextLong()));
		List<RewardOption> loot = TreasureRules.loot(RNG);
		for (int i = 0; i < loot.size() && i < slots.size(); i++) {
			chest.setItem(slots.get(i), RewardBoxes.cardStack(loot.get(i), level.registryAccess()));
		}
	}

	/** Guards with no target attack the nearest player within 24 blocks; a destroyed chest is put back. */
	private static void guardTick(ServerLevel level, TreasureSavedData d) {
		BlockPos chestPos = BlockPos.of(d.chest);
		if (level.isLoaded(chestPos)) {
			ensureChest(level, chestPos);
		}
		for (String id : d.guards) {
			if (level.getEntity(UUID.fromString(id)) instanceof Mob m && m.isAlive() && m.getTarget() == null) {
				ServerPlayer p = nearestPlayer(level, m);
				if (p != null) {
					m.setTarget(p);
				}
			}
		}
	}

	private static ServerPlayer nearestPlayer(ServerLevel level, Mob m) {
		ServerPlayer best = null;
		double bestD = TreasureRules.AGGRO_RANGE * TreasureRules.AGGRO_RANGE;
		for (ServerPlayer p : level.players()) {
			if (p.isSpectator() || p.isCreative() || !p.isAlive()) {
				continue;
			}
			double dd = p.distanceToSqr(m);
			if (dd < bestD) {
				bestD = dd;
				best = p;
			}
		}
		return best;
	}

	private static void onDeath(LivingEntity entity, net.minecraft.world.damagesource.DamageSource source) {
		MegaMobs.Mega m = MegaMobs.mega(entity);
		if (m == null || !(entity.level() instanceof ServerLevel sl)) {
			return;
		}
		TreasureSavedData d = data(sl.getServer());
		if (d.phase == TreasureRules.Phase.ACTIVE && d.id == m.treasure() && d.guards.remove(entity.getUUID().toString())) {
			d.setDirty();
			if (d.guards.isEmpty()) {
				success(sl.getServer(), d);
			}
		}
	}

	private static void success(MinecraftServer server, TreasureSavedData d) {
		BlockPos c = BlockPos.of(d.chest);
		fillLoot(server.overworld(), c);
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			title(p, text("is open! X " + c.getX() + ", Y " + c.getY() + ", Z " + c.getZ()));
		}
		server.getPlayerList().broadcastSystemMessage(text("Deadly Treasure: every guard is dead. The chest at X " + c.getX()
			+ ", Y " + c.getY() + ", Z " + c.getZ() + " is open."), false);
		d.clear();
	}

	/** Time is up with guards left (or never found): chest and guards vanish. */
	public static void fail(MinecraftServer server, TreasureSavedData d) {
		ServerLevel ow = server.overworld();
		if (d.placed) {
			d.cleanup.add(d.chest);
			for (String id : d.guards) {
				Entity e = ow.getEntity(UUID.fromString(id));
				if (e != null) {
					e.discard();
				}
			}
		}
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			title(p, text("has failed"));
		}
		server.getPlayerList().broadcastSystemMessage(text("Deadly Treasure has failed: the treasure and its guards are gone."), false);
		d.clear();
		cleanup(ow, d);
	}

	/** Removes the chests of failed events once their chunk is loaded. */
	private static void cleanup(ServerLevel ow, TreasureSavedData d) {
		if (d.cleanup.isEmpty()) {
			return;
		}
		boolean changed = d.cleanup.removeIf(packed -> {
			BlockPos pos = BlockPos.of(packed);
			if (!ow.isLoaded(pos)) {
				return false;
			}
			if (ow.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
				chest.clearContent();
				boolean water = ow.getBlockState(pos).getValue(ChestBlock.WATERLOGGED);
				ow.setBlock(pos, water ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
			}
			return true;
		});
		if (changed) {
			d.setDirty();
		}
	}

	// ---- messages ----

	public static MutableComponent text(String s) {
		return Component.literal(s).withStyle(st -> st.withColor(TextColor.fromRgb(COLOR)));
	}

	private static void title(ServerPlayer p, Component subtitle) {
		p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 100, 20));
		p.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
		p.connection.send(new ClientboundSetTitleTextPacket(text("Deadly Treasure").withStyle(ChatFormatting.BOLD)));
		p.level().playSound(null, p.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1f, 0.6f);
	}

	// ---- /treasure ----

	private static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("treasure")
			.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
			.then(Commands.literal("status").executes(c -> {
				TreasureSavedData d = data(c.getSource().getServer());
				long left = d.phase == TreasureRules.Phase.PENDING ? d.startAt - Wanderer.clockTime(c.getSource().getServer())
					: d.phase == TreasureRules.Phase.ACTIVE ? d.endAt - Wanderer.clockTime(c.getSource().getServer()) : 0;
				c.getSource().sendSuccess(() -> text("Deadly Treasure: " + d.phase + ", X " + d.x + " Z " + d.z
					+ (d.placed ? ", chest " + BlockPos.of(d.chest).toShortString() + ", " + d.guards.size() + " guards (" + d.group + ")" : "")
					+ (left > 0 ? ", " + left / 20 + " s" : "")), false);
				return 1;
			}))
			.then(Commands.literal("start").executes(c -> {
				start(c.getSource().getServer(), c.getSource().getPlayerOrException(), true);
				return 1;
			}))
			.then(Commands.literal("here")
				.executes(c -> here(c.getSource(), null))
				.then(Commands.argument("group", StringArgumentType.word())
					.suggests((c, b) -> {
						TreasureRules.GROUPS.forEach(g -> b.suggest(g.id()));
						return b.buildFuture();
					})
					.executes(c -> here(c.getSource(), StringArgumentType.getString(c, "group")))))
			.then(Commands.literal("lineup")
				.then(Commands.argument("page", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 3))
					.executes(c -> lineup(c.getSource().getPlayerOrException(),
						com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "page")))))
			.then(Commands.literal("giveup").executes(c -> {
				TreasureSavedData d = data(c.getSource().getServer());
				if (d.phase == TreasureRules.Phase.IDLE) {
					return 0;
				}
				if (d.phase == TreasureRules.Phase.PENDING) {
					d.clear();
				} else {
					fail(c.getSource().getServer(), d);
				}
				return 1;
			})));
	}

	/** Admin: 4 Mega species of page 0..3 standing still 5 blocks south of the player (to look at the glow). */
	private static int lineup(ServerPlayer p, int page) {
		List<TreasureRules.Guard> all = new ArrayList<>();
		java.util.Set<String> seen = new java.util.HashSet<>();
		for (TreasureRules.Group g : TreasureRules.GROUPS) {
			for (TreasureRules.Guard guard : g.guards()) {
				if (seen.add(guard.species())) {
					all.add(guard);
				}
			}
		}
		int n = 0;
		for (int i = page * 4; i < Math.min(all.size(), page * 4 + 4); i++) {
			BlockPos at = p.blockPosition().offset(-5 + (i - page * 4) * 3, 0, 5);
			Mob m = MegaMobs.create(p.level(), all.get(i), at, 0, at);
			if (m != null) {
				m.setNoAi(true);
				m.setYRot(180f);
				m.setYHeadRot(180f);
				m.setYBodyRot(180f);
				p.level().addFreshEntity(m);
				n++;
			}
		}
		return n;
	}

	/** Admin: start now and place right here (a few blocks away), optionally with a given group. */
	private static int here(CommandSourceStack src, String group) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer p = src.getPlayerOrException();
		if (group != null && TreasureRules.group(group) == null) {
			src.sendFailure(Component.literal("Unknown group " + group));
			return 0;
		}
		start(src.getServer(), p, false);
		TreasureSavedData d = data(src.getServer());
		BlockPos at = p.blockPosition().offset(6, 0, 0);
		TreasureRules.Group g = group == null ? null : TreasureRules.group(group);
		place(p.level(), d, at, false, group);
		return g == null ? 1 : g.size();
	}
}
