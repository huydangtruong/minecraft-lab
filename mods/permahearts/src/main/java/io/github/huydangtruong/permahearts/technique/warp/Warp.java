package io.github.huydangtruong.permahearts.technique.warp;

import io.github.huydangtruong.permahearts.PermaHearts;
import io.github.huydangtruong.permahearts.technique.Technique;
import io.github.huydangtruong.permahearts.technique.TechniqueAttachment;
import io.github.huydangtruong.permahearts.technique.TechniqueData;
import io.github.huydangtruong.permahearts.technique.TechniqueRules;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Cosmic Warping (D75), server side: placing portals, removing them, warping to a portal or another player, and the
 * menu data. The list of portals lives in {@link WarpSavedData}; the portal entities are only the visible part.
 */
public final class Warp {
	/** How far the look ray reaches when a portal is placed (blocks). */
	public static final double PLACE_REACH = 6.0;
	/** Radius of the structure search for the auto name (blocks). */
	public static final int NAME_RADIUS = 64;

	public static final int OPEN = 0, GO_PORTAL = 1, GO_PLAYER = 2, DELETE = 3;

	/** Client to server. kind: OPEN..DELETE, target: portal id or player UUID (empty for OPEN). */
	public record Request(int kind, String target) implements CustomPacketPayload {
		public static final Type<Request> TYPE = new Type<>(Identifier.fromNamespaceAndPath(PermaHearts.MOD_ID, "warp_request"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, Request::kind,
			ByteBufCodecs.STRING_UTF8, Request::target,
			Request::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** One line of the menu: a portal or a player. */
	public record Row(String id, String name, String info, int color) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Row> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, Row::id,
			ByteBufCodecs.STRING_UTF8, Row::name,
			ByteBufCodecs.STRING_UTF8, Row::info,
			ByteBufCodecs.VAR_INT, Row::color,
			Row::new);
	}

	/** Server to client: what the warp menu shows. cooldownLeft in ticks. */
	public record Menu(List<Row> portals, List<Row> players, int max, int cooldownLeft) implements CustomPacketPayload {
		public static final Type<Menu> TYPE = new Type<>(Identifier.fromNamespaceAndPath(PermaHearts.MOD_ID, "warp_menu"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Menu> CODEC = StreamCodec.composite(
			Row.CODEC.apply(ByteBufCodecs.list()), Menu::portals,
			Row.CODEC.apply(ByteBufCodecs.list()), Menu::players,
			ByteBufCodecs.VAR_INT, Menu::max,
			ByteBufCodecs.VAR_INT, Menu::cooldownLeft,
			Menu::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	private Warp() {}

	/** Registers the entity type, the packets and the stale-portal check. Called from TechniqueCasts.init. */
	public static void init() {
		WarpPortal.init();
		PayloadTypeRegistry.serverboundPlay().register(Request.TYPE, Request.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(Menu.TYPE, Menu.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(Request.TYPE, (payload, ctx) -> handle(ctx.player(), payload.kind(), payload.target()));
		// A portal whose list entry is gone (removed while its chunk was unloaded) disappears when it loads.
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof WarpPortal portal) {
				PortalRecord.All all = WarpSavedData.of(level.getServer()).get();
				if (all.find(portal.getUUID()) == null) {
					portal.discard();
				}
			}
		});
	}

	/** Runs a request of the menu. Refusals go to the overlay. */
	public static void handle(ServerPlayer p, int kind, String target) {
		String error = null;
		switch (kind) {
			case OPEN -> {
				error = checkReady(p);
				if (error == null) {
					openMenu(p);
				}
			}
			case GO_PORTAL -> error = warpToPortal(p, parse(target));
			case GO_PLAYER -> error = warpToPlayer(p, parse(target));
			case DELETE -> {
				error = checkReady(p);
				if (error == null) {
					error = remove(p, parse(target)) ? null : "That portal is not yours";
					if (error == null) {
						openMenu(p); // refresh the list
					}
				}
			}
			default -> error = "Unknown request";
		}
		if (error != null) {
			p.sendOverlayMessage(Component.literal(error).withStyle(ChatFormatting.RED));
		}
	}

	private static UUID parse(String s) {
		try {
			return UUID.fromString(s);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	/** Null when the player may use Cosmic Warping now, else the reason. */
	static String checkReady(ServerPlayer p) {
		if (!PermaHearts.enabled(p.level().getServer())) {
			return "Immortal Cultivation is off in this world";
		}
		if (!p.isAlive() || p.isSpectator()) {
			return "You cannot cast now";
		}
		if (TechniqueAttachment.get(p).level(Technique.WARP) < 1) {
			return "Learn Cosmic Warping first";
		}
		return null;
	}

	private static long now(MinecraftServer server) {
		return server.overworld().getGameTime();
	}

	// ---- placing ----

	/**
	 * Right click: puts a portal on the first solid top face the look ray meets within {@link #PLACE_REACH}. No
	 * cooldown. At the limit (or with nowhere to put it) a red message tells why. Returns true when a portal was placed.
	 */
	public static boolean place(ServerPlayer p, int level) {
		ServerLevel sl = p.level();
		PortalRecord.All all = WarpSavedData.of(sl.getServer()).get();
		List<PortalRecord> mine = all.of(p.getUUID());
		int max = TechniqueRules.warpMaxPortals(level);
		if (mine.size() >= max) {
			p.sendOverlayMessage(Component.literal("Portal limit reached (" + max + "). Remove one in the warp menu.").withStyle(ChatFormatting.RED));
			return false;
		}
		Vec3 eye = p.getEyePosition();
		Vec3 end = eye.add(p.getViewVector(1f).scale(PLACE_REACH));
		BlockHitResult hit = sl.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
		if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection() != Direction.UP) {
			p.sendOverlayMessage(Component.literal("Look at the top of a block").withStyle(ChatFormatting.RED));
			return false;
		}
		BlockPos above = hit.getBlockPos().above();
		if (!sl.isEmptyBlock(above) || !sl.isEmptyBlock(above.above())) {
			p.sendOverlayMessage(Component.literal("Not enough room for a portal").withStyle(ChatFormatting.RED));
			return false;
		}
		double x = above.getX() + 0.5, y = hit.getLocation().y, z = above.getZ() + 0.5;
		spawn(sl, p, x, y, z, p.getYRot());
		return true;
	}

	/** Creates the list entry and the entity (in that order: the load check looks the entry up). Returns the record. */
	public static PortalRecord spawn(ServerLevel sl, ServerPlayer owner, double x, double y, double z, float yaw) {
		WarpSavedData data = WarpSavedData.of(sl.getServer());
		List<String> names = new ArrayList<>();
		for (PortalRecord r : data.get().of(owner.getUUID())) {
			names.add(r.name());
		}
		String name = WarpNaming.unique(autoName(sl, BlockPos.containing(x, y, z)), names);
		UUID id = UUID.randomUUID();
		String dim = sl.dimension().identifier().toString();
		PortalRecord rec = new PortalRecord(id, owner.getUUID(), dim, x, y, z, yaw, name, now(sl.getServer()));
		data.set(data.get().with(rec));
		WarpPortal portal = new WarpPortal(WarpPortal.TYPE, sl);
		portal.setUUID(id);
		portal.setPos(x, y, z);
		portal.setYRot(yaw);
		portal.setColor(WarpColor.rgb(owner.getUUID()));
		sl.addFreshEntity(portal);
		sl.playSound(null, BlockPos.containing(x, y, z), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.4f);
		sl.sendParticles(ParticleTypes.PORTAL, x, y + 0.75, z, 30, 0.3, 0.6, 0.3, 0.1);
		return rec;
	}

	// ---- removing ----

	/**
	 * The X button: removes one of the caller's portals. The entity is discarded now when it is loaded; otherwise the
	 * missing list entry removes it when its chunk loads. Returns false when the portal is not the caller's.
	 */
	public static boolean remove(ServerPlayer p, UUID id) {
		if (id == null) {
			return false;
		}
		MinecraftServer server = p.level().getServer();
		WarpSavedData data = WarpSavedData.of(server);
		PortalRecord rec = data.get().find(id);
		if (rec == null || !rec.owner().equals(p.getUUID())) {
			return false;
		}
		data.set(data.get().without(id));
		ServerLevel sl = server.getLevel(dimensionKey(rec.dimension()));
		if (sl != null && sl.getEntity(id) instanceof WarpPortal portal) {
			portal.discard();
		}
		return true;
	}

	static ResourceKey<Level> dimensionKey(String id) {
		return ResourceKey.create(Registries.DIMENSION, Identifier.parse(id));
	}

	// ---- warping ----

	/** Warps the player to one of their own portals. Null when it worked, else the reason. */
	public static String warpToPortal(ServerPlayer p, UUID id) {
		String error = checkReady(p);
		if (error != null) {
			return error;
		}
		MinecraftServer server = p.level().getServer();
		PortalRecord rec = id == null ? null : WarpSavedData.of(server).get().find(id);
		if (rec == null || !rec.owner().equals(p.getUUID())) {
			return "That portal is gone";
		}
		error = cooldownError(p);
		if (error != null) {
			return error;
		}
		ServerLevel target = server.getLevel(dimensionKey(rec.dimension()));
		if (target == null) {
			return "That dimension is not available";
		}
		return go(p, target, new Vec3(rec.x(), rec.y(), rec.z()), rec.yaw());
	}

	/** Warps the player to another online player. Null when it worked, else the reason. */
	public static String warpToPlayer(ServerPlayer p, UUID id) {
		String error = checkReady(p);
		if (error != null) {
			return error;
		}
		ServerPlayer other = id == null ? null : p.level().getServer().getPlayerList().getPlayer(id);
		if (other == null || other == p || !other.isAlive()) {
			return "That player is not online";
		}
		error = cooldownError(p);
		if (error != null) {
			return error;
		}
		return go(p, other.level(), other.position(), other.getYRot());
	}

	private static String cooldownError(ServerPlayer p) {
		TechniqueData d = TechniqueAttachment.get(p);
		long now = now(p.level().getServer());
		if (d.onCooldown(Technique.WARP, now)) {
			return "On cooldown: " + (d.cooldownLeft(Technique.WARP, now) + 19) / 20 + " s";
		}
		return null;
	}

	/** The common end of a warp: find a free spot near the target, move, effects at both ends, start the cooldown. */
	private static String go(ServerPlayer p, ServerLevel target, Vec3 at, float yaw) {
		Vec3 spot = freeSpot(target, at);
		ServerLevel from = p.level();
		Vec3 start = p.position();
		p.stopRiding();
		if (!p.teleportTo(target, spot.x, spot.y, spot.z, Set.of(), p.getYRot(), p.getXRot(), false)) {
			return "You cannot warp now";
		}
		p.resetFallDistance();
		p.setDeltaMovement(Vec3.ZERO);
		effects(from, start);
		effects(target, spot);
		int level = TechniqueAttachment.get(p).level(Technique.WARP);
		long until = now(p.level().getServer()) + TechniqueRules.cooldownTicks(Technique.WARP, Mth.clamp(level, 1, TechniqueRules.MAX_LEVEL));
		TechniqueAttachment.set(p, TechniqueAttachment.get(p).withCooldown(Technique.WARP, until));
		return null;
	}

	private static void effects(ServerLevel sl, Vec3 at) {
		sl.playSound(null, BlockPos.containing(at), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1f, 1f);
		sl.sendParticles(ParticleTypes.PORTAL, at.x, at.y + 1, at.z, 40, 0.4, 0.8, 0.4, 0.2);
	}

	/** A spot beside the target where a player fits: the target itself, else the closest free one around it. */
	static Vec3 freeSpot(ServerLevel sl, Vec3 at) {
		sl.getChunk(Mth.floor(at.x) >> 4, Mth.floor(at.z) >> 4); // loads the one chunk
		for (int ring = 0; ring <= 2; ring++) {
			for (int dx = -ring; dx <= ring; dx++) {
				for (int dz = -ring; dz <= ring; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
						continue;
					}
					for (int dy = 0; dy <= 2; dy++) {
						double x = at.x + dx, y = at.y + dy, z = at.z + dz;
						AABB box = new AABB(x - 0.3, y, z - 0.3, x + 0.3, y + 1.8, z + 0.3);
						if (sl.noCollision(box)) {
							return new Vec3(x, y, z);
						}
					}
				}
			}
		}
		return at;
	}

	// ---- naming ----

	/** The structure within {@link #NAME_RADIUS} blocks that is nearest, else the biome (readable). Not unique yet. */
	public static String autoName(ServerLevel sl, BlockPos pos) {
		String structure = nearestStructure(sl, pos);
		if (structure != null) {
			return WarpNaming.structureName(structure);
		}
		return sl.getBiome(pos).unwrapKey().map(k -> WarpNaming.readable(k.identifier().toString())).orElse("Portal");
	}

	/** Id of the nearest structure whose box is within the radius (only chunks that are already loaded are looked at). */
	private static String nearestStructure(ServerLevel sl, BlockPos pos) {
		try {
			var registry = sl.registryAccess().lookupOrThrow(Registries.STRUCTURE);
			int cx = pos.getX() >> 4, cz = pos.getZ() >> 4, r = NAME_RADIUS / 16 + 1;
			double best = NAME_RADIUS * (double) NAME_RADIUS;
			String found = null;
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (sl.getChunkSource().getChunkNow(cx + dx, cz + dz) == null) {
						continue;
					}
					for (StructureStart start : sl.structureManager().startsForStructure(cx + dx, cz + dz, s -> true)) {
						var box = start.getBoundingBox();
						double ox = Math.max(Math.max(box.minX() - pos.getX(), pos.getX() - box.maxX()), 0);
						double oy = Math.max(Math.max(box.minY() - pos.getY(), pos.getY() - box.maxY()), 0);
						double oz = Math.max(Math.max(box.minZ() - pos.getZ(), pos.getZ() - box.maxZ()), 0);
						double d = ox * ox + oy * oy + oz * oz;
						if (d <= best) {
							Identifier key = registry.getKey(start.getStructure());
							if (key != null) {
								best = d;
								found = key.toString();
							}
						}
					}
				}
			}
			return found;
		} catch (RuntimeException e) {
			return null; // the name falls back to the biome
		}
	}

	// ---- the menu ----

	/** Builds the menu data for a player: own portals (oldest first), other online players, max, cooldown left. */
	public static Menu menu(ServerPlayer p) {
		MinecraftServer server = p.level().getServer();
		TechniqueData d = TechniqueAttachment.get(p);
		int level = Mth.clamp(d.level(Technique.WARP), 1, TechniqueRules.MAX_LEVEL);
		List<Row> portals = new ArrayList<>();
		int color = WarpColor.rgb(p.getUUID());
		for (PortalRecord r : WarpSavedData.of(server).get().of(p.getUUID())) {
			portals.add(new Row(r.id().toString(), r.name(),
				WarpNaming.coordinates(Mth.floor(r.x()), Mth.floor(r.y()), Mth.floor(r.z()), r.dimension()), color));
		}
		List<Row> players = new ArrayList<>();
		for (ServerPlayer o : server.getPlayerList().getPlayers()) {
			if (o != p && o.isAlive()) {
				players.add(new Row(o.getUUID().toString(), o.getGameProfile().name(),
					WarpNaming.coordinates(o.getBlockX(), o.getBlockY(), o.getBlockZ(), o.level().dimension().identifier().toString()),
					WarpColor.rgb(o.getUUID())));
			}
		}
		long now = now(server);
		int left = d.onCooldown(Technique.WARP, now) ? (int) d.cooldownLeft(Technique.WARP, now) : 0;
		return new Menu(portals, players, TechniqueRules.warpMaxPortals(level), left);
	}

	public static void openMenu(ServerPlayer p) {
		ServerPlayNetworking.send(p, menu(p));
	}
}
