package io.github.huydangtruong.immortalcultivation.technique.grasp;

import io.github.huydangtruong.immortalcultivation.Uuids;
import io.github.huydangtruong.immortalcultivation.technique.SafeTeleport;
import io.github.huydangtruong.immortalcultivation.technique.grasp.GraspCalls;
import io.github.huydangtruong.immortalcultivation.technique.grasp.GraspRules;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/** Soul Grasp calls: bringing a held mob back, loading the chunk of a far one (D168, D198; split from Grasp, P-076). */
final class GraspCallService {
	private static final Map<UUID, Long> CALL_READY = new HashMap<>();
	/** D198: calls that wait for a far mob's chunk to load (each holds a temporary chunk ticket). */
	private static final GraspCalls CALLS = new GraspCalls();

	private GraspCallService() {}

	static void clear() {
		CALL_READY.clear();
		CALLS.clear();
	}

	static long readyAt(UUID mob) {
		return CALL_READY.getOrDefault(mob, 0L);
	}

	static int pending() {
		return CALLS.size();
	}

	static void call(ServerPlayer p, String target) {
		UUID id = Uuids.parseOrNull(target);
		GraspData.Entry e = id == null ? null : GraspData.of(p.level().getServer()).find(p.getUUID(), id);
		if (e == null) {
			return;
		}
		long now = p.level().getGameTime();
		if (CALL_READY.getOrDefault(id, 0L) > now) {
			p.sendOverlayMessage(Component.literal("Call again in " + (CALL_READY.get(id) - now + 19) / 20 + " s"));
			return;
		}
		if (CALLS.has(id)) {
			p.sendOverlayMessage(Component.literal("Calling..."));
			return;
		}
		Mob m = loadedMob(p.level().getServer(), id);
		if (m != null) {
			arrive(p, m, false, now);
			Grasp.sendMenu(p);
		} else if (startLoad(p, e, false, now)) {
			p.sendOverlayMessage(Component.literal("Calling..."));
		} else {
			p.sendOverlayMessage(Component.literal(NOT_FOUND));
		}
	}

	/** How long a call waits for a far mob (ticks); only the game tests change it. */
	private static int callTimeout = GraspCalls.TIMEOUT_TICKS;

	private static final String NOT_FOUND = "Could not find it where it was last seen";

	/** D198: the grasped mob with this uuid when it is loaded in any dimension (and alive), else null. */
	private static Mob loadedMob(MinecraftServer server, UUID id) {
		return server.overworld().getEntityInAnyDimension(id) instanceof Mob m && m.isAlive() && m.hasAttached(GraspedMob.ATTACHMENT) ? m : null;
	}

	/**
	 * D198: brings a loaded mob to the player: another dimension first (the entity is recreated there: same uuid, attachment
	 * and stats), then the arrival spot (Call: 2 blocks ahead; Call all: the mob's ring spot) and the cooldown.
	 */
	private static void arrive(ServerPlayer p, Mob m, boolean all, long now) {
		Mob mob = m;
		ServerLevel to = p.level();
		if (m.level() != to) {
			Entity moved = m.teleport(new TeleportTransition(to, p.position(), Vec3.ZERO, m.getYRot(), m.getXRot(), TeleportTransition.DO_NOTHING));
			if (!(moved instanceof Mob nm)) {
				return;
			}
			mob = nm;
		}
		Vec3 want = all ? GraspGoals.followSpot(mob, p) : p.position().add(p.getLookAngle().multiply(2, 0, 2));
		callTo(p, mob, want, now);
	}

	/** Teleports a mob to a spot near a point and starts its Call cooldown. */
	private static void callTo(ServerPlayer p, Mob m, Vec3 want, long now) {
		// B10 (as the follow-teleport, D170): ground and no liquid. If the wanted spot is unsafe, the same distance from the owner
		// at other angles; then the owner's own spot; only when all fail the old free spot.
		ServerLevel sl = p.level();
		Vec3 at = null;
		double rx = want.x - p.getX();
		double rz = want.z - p.getZ();
		for (int deg : new int[] {0, 45, -45, 90, -90, 135, -135, 180}) {
			double a = Math.toRadians(deg);
			Vec3 spot = new Vec3(p.getX() + rx * Math.cos(a) - rz * Math.sin(a), want.y, p.getZ() + rx * Math.sin(a) + rz * Math.cos(a));
			at = SafeTeleport.spotOrNull(sl, m, spot);
			if (at != null) {
				break;
			}
		}
		if (at == null) {
			at = SafeTeleport.spotOrNull(sl, m, p.position());
		}
		if (at == null) {
			at = GraspGoals.freeSpotNear(sl, m, want);
		}
		m.getNavigation().stop();
		m.snapTo(at.x, at.y, at.z);
		m.setTarget(null);
		CALL_READY.put(m.getUUID(), now + GraspRules.CALL_COOLDOWN_TICKS);
		sl.sendParticles(ParticleTypes.END_ROD, at.x, at.y + m.getBbHeight() / 2, at.z, 12, 0.4, 0.4, 0.4, 0.05);
	}

	/** D198: D168's Call all: every ready mob comes, loaded ones now, the others after their chunk loads; says how many. */
	static void allCall(ServerPlayer p) {
		long now = p.level().getGameTime();
		MinecraftServer server = p.level().getServer();
		int came = 0, waiting = 0, calling = 0, lost = 0;
		for (GraspData.Entry e : new ArrayList<>(GraspData.of(server).get(p.getUUID()).mobs())) {
			if (CALL_READY.getOrDefault(e.mob(), 0L) > now) {
				waiting++;
			} else if (CALLS.has(e.mob())) {
				calling++;
			} else if (loadedMob(server, e.mob()) instanceof Mob m) {
				arrive(p, m, true, now);
				came++;
			} else if (startLoad(p, e, true, now)) {
				calling++;
			} else {
				lost++;
			}
		}
		String msg = came + (came == 1 ? " mob came" : " mobs came");
		if (calling > 0) {
			msg += ", " + calling + " calling...";
		}
		if (waiting > 0) {
			msg += ", " + waiting + " still waiting";
		}
		if (lost > 0) {
			msg += ", " + lost + " not found";
		}
		p.sendOverlayMessage(Component.literal(msg));
		Grasp.sendMenu(p);
	}

	// ---- D198: calling a mob that is not loaded ----

	private static ServerLevel levelOf(MinecraftServer server, String dim) {
		Identifier id = Identifier.tryParse(dim);
		return id == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
	}

	/** Loads the chunk where the mob was last seen (temporary ticket) and waits for it. False when that place does not exist. */
	private static boolean startLoad(ServerPlayer p, GraspData.Entry e, boolean all, long now) {
		MinecraftServer server = p.level().getServer();
		ServerLevel lv = levelOf(server, e.dim());
		if (lv == null) {
			return false;
		}
		ChunkPos cp = ChunkPos.containing(BlockPos.containing(e.x(), e.y(), e.z()));
		if (!CALLS.start(new GraspCalls.Pending(e.mob(), p.getUUID(), e.dim(), cp.x(), cp.z(), all, now + callTimeout))) {
			return false;
		}
		lv.getChunkSource().addTicketWithRadius(TicketType.PORTAL, cp, GraspCalls.TICKET_RADIUS);
		return true;
	}

	/** Ends a waiting call and gives back its chunk ticket (kept while another waiting call needs the same chunk). */
	private static void endCall(MinecraftServer server, GraspCalls.Pending pd) {
		CALLS.finish(pd.mob());
		if (!CALLS.chunkStillNeeded(pd) && levelOf(server, pd.dim()) instanceof ServerLevel lv) {
			lv.getChunkSource().removeTicketWithRadius(TicketType.PORTAL, new ChunkPos(pd.chunkX(), pd.chunkZ()), GraspCalls.TICKET_RADIUS);
		}
	}

	static void cancelCalls(MinecraftServer server, UUID owner) {
		for (GraspCalls.Pending pd : CALLS.of(owner)) {
			endCall(server, pd);
		}
	}

	static void cancelAllCalls(MinecraftServer server) {
		for (GraspCalls.Pending pd : CALLS.all()) {
			endCall(server, pd);
		}
	}

	/** Every tick: a waiting call whose mob has loaded brings it; one that ran out of time (or lost its owner) lets go. */
	static void tickCalls(MinecraftServer server) {
		long now = server.overworld().getGameTime();
		for (GraspCalls.Pending pd : CALLS.all()) {
			ServerPlayer p = server.getPlayerList().getPlayer(pd.owner());
			GraspData.Entry e = p == null ? null : GraspData.of(server).find(pd.owner(), pd.mob());
			if (p == null || !p.isAlive() || e == null) {
				endCall(server, pd); // owner left or died, or the mob was released or died while we waited
				continue;
			}
			Mob m = loadedMob(server, pd.mob());
			if (m != null) {
				endCall(server, pd);
				arrive(p, m, pd.all(), p.level().getGameTime());
				Grasp.sendMenu(p);
			} else if (now >= pd.deadline()) {
				endCall(server, pd);
				p.sendOverlayMessage(Component.literal("Could not find ").append(Grasp.nameOf(BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(e.type())), e.number()))
					.append(" where it was last seen"));
				Grasp.sendMenu(p);
			}
		}
	}

	/** Test helper: how many ticks a call waits for a far mob (the default is {@link GraspCalls#TIMEOUT_TICKS}). */
	static void setCallTimeout(int ticks) {
		callTimeout = ticks;
	}
}
