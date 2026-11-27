package io.github.huydangtruong.immortalcultivation.client;

import io.github.huydangtruong.immortalcultivation.technique.TechniqueRules;
import io.github.huydangtruong.immortalcultivation.technique.chrono.ChronoFreeze;
import io.github.huydangtruong.immortalcultivation.technique.chrono.ChronoNet;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/**
 * Time Rewind on the client (D268): the time stops the server reports nearby (for the grey box and the freeze of
 * projectiles and items), the player's own one (wheel and HUD timeline).
 */
public final class ClientChrono {
	/** A zone not refreshed for this long is forgotten (its owner went out of range or changed dimension). */
	private static final int STALE = 10;

	/** One time stop as last reported. */
	public record Zone(ChronoNet.Zone msg, AABB box, int age) {}

	private static final Map<Integer, Zone> ZONES = new HashMap<>();
	/** Wheel notches not yet sent (positive = back). */
	private static int pending;
	/** D272: the player let go of their own stop (sent, the server may not have answered yet). */
	private static boolean released;
	/** D272: keys that may let go: each counts once it has been up since the hold began (keys held at the cast do not). */
	private static final Map<KeyMapping, Boolean> ARMED = new HashMap<>();

	private ClientChrono() {}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(ChronoNet.Zone.TYPE, (z, ctx) -> {
			if (z.size() <= 0) {
				ZONES.remove(z.owner());
			} else {
				ZONES.put(z.owner(), new Zone(z, new AABB(z.minX(), z.minY(), z.minZ(), z.minX() + z.size(), z.minY() + z.size(),
					z.minZ() + z.size()), 0));
			}
		});
		ClientTickEvents.START_CLIENT_TICK.register(mc -> {
			if (mc.player != null && held(mc)) {
				mc.player.setOldPosAndRot(); // the held player does not tick: keep its drawn position following the server's
			}
		});
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			if (mc.player == null) {
				ZONES.clear();
				pending = 0;
				released = false;
				ARMED.clear();
				return;
			}
			tickRelease(mc);
			ZONES.replaceAll((id, z) -> new Zone(z.msg, z.box, z.age + 1));
			ZONES.values().removeIf(z -> z.age > STALE);
			if (pending != 0 && own(mc) != null) {
				ClientPlayNetworking.send(new ChronoNet.Scroll(pending));
			}
			pending = 0;
		});
		// D268: in a stopped box, projectiles and items stand still on the client too (mobs keep ticking: it only smooths the
		// positions the server sends while the recording plays)
		// D272: the player who cast it stands still too until they move or click
		ChronoFreeze.client = e -> e instanceof Player ? e == Minecraft.getInstance().player && held(Minecraft.getInstance())
			: !(e instanceof LivingEntity) && inside(e);
	}

	/** True while the player's own stop holds them. */
	public static boolean held(Minecraft mc) {
		Zone z = own(mc);
		return z != null && z.msg.casterHeld() && !released;
	}

	private static void tickRelease(Minecraft mc) {
		if (own(mc) == null) {
			released = false;
			ARMED.clear();
			return;
		}
		if (!held(mc)) {
			return;
		}
		var o = mc.options;
		boolean ownUse = ClientSkills.selectedTechnique(mc) == io.github.huydangtruong.immortalcultivation.technique.Technique.CHRONO;
		KeyMapping[] keys = {o.keyUp, o.keyDown, o.keyLeft, o.keyRight, o.keyJump, o.keyShift, o.keyAttack, o.keyUse};
		boolean first = ARMED.isEmpty();
		for (KeyMapping k : keys) {
			boolean down = k.isDown() && mc.gui.screen() == null;
			if (first) {
				ARMED.put(k, !down);
			} else if (!down) {
				ARMED.put(k, true);
			} else if (ARMED.getOrDefault(k, false) && !(k == o.keyUse && ownUse)) { // right click with Time Rewind selected recasts
				released = true;
				ClientPlayNetworking.send(new ChronoNet.Release());
				return;
			}
		}
	}

	private static boolean inside(Entity e) {
		for (Zone z : ZONES.values()) {
			if (z.box.contains(e.position())) {
				return true;
			}
		}
		return false;
	}

	/** The player's own running time stop, or null. */
	public static Zone own(Minecraft mc) {
		return mc.player == null ? null : ZONES.get(mc.player.getId());
	}

	/** All time stops around. */
	public static Iterable<Zone> zones() {
		return ZONES.values();
	}

	/** The wheel while the player's own time stop runs: down = back in time. Returns true when it was used here. */
	public static boolean scroll(Minecraft mc, double wheel) {
		if (own(mc) == null || wheel == 0) {
			return false;
		}
		pending += wheel < 0 ? 1 : -1;
		return true;
	}

	/** Ticks left of the player's own time stop (0 when none). */
	public static int ticksLeft(Minecraft mc) {
		Zone z = own(mc);
		return z == null ? 0 : Math.max(0, z.msg.ticksLeft() - z.age);
	}

	/** Seconds a wheel notch moves. */
	static float notchSeconds() {
		return TechniqueRules.CHRONO_NOTCH_TICKS / 20f;
	}
}
