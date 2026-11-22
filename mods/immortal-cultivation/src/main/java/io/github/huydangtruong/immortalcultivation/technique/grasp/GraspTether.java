package io.github.huydangtruong.immortalcultivation.technique.grasp;

import io.github.huydangtruong.immortalcultivation.ImmortalCultivation;
import io.github.huydangtruong.immortalcultivation.technique.grasp.GraspRules;
import io.github.huydangtruong.immortalcultivation.technique.Technique;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueAttachment;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** The tether of Soul Grasp: the held right click that charges a tame (split from Grasp, P-076). */
final class GraspTether {
	private static final Identifier SLOW_ID = Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "grasp_slow");

	/** A tether in progress (server memory only). */
	private static final class Tether {
		final Mob target;
		final int duration;
		final int level;
		int age;
		int noSight;

		Tether(Mob target, int duration, int level) {
			this.target = target;
			this.duration = duration;
			this.level = level;
		}
	}

	private static final Map<UUID, Tether> TETHERS = new HashMap<>();

	private GraspTether() {}

	static boolean tethering(ServerPlayer p) {
		return TETHERS.containsKey(p.getUUID());
	}

	static void clear() {
		TETHERS.clear();
	}

	/** Every tick: advances the tethers of all players. */
	static void tickAll(MinecraftServer server) {
		for (var it = new ArrayList<>(TETHERS.entrySet()).iterator(); it.hasNext();) {
			var en = it.next();
			ServerPlayer p = server.getPlayerList().getPlayer(en.getKey());
			if (p == null) {
				TETHERS.remove(en.getKey());
			} else if (TETHERS.get(en.getKey()) == en.getValue()) {
				tickTether(p, en.getValue());
			}
		}
	}

	/** Starts a tether to the entity with this id. Refusals go to the overlay. Public for the game tests. */
	static boolean start(ServerPlayer p, int entityId) {
		MinecraftServer server = p.level().getServer();
		TechniqueData d = TechniqueAttachment.get(p);
		long now = server.overworld().getGameTime();
		if (TETHERS.containsKey(p.getUUID())) {
			return false;
		}
		if (d.onCooldown(Technique.GRASP, now)) {
			p.sendOverlayMessage(Component.literal("On cooldown: " + (d.cooldownLeft(Technique.GRASP, now) + 19) / 20 + " s"));
			return false;
		}
		Entity e = p.level().getEntity(entityId);
		if (!(e instanceof Mob mob) || !mob.isAlive() || mob.distanceTo(p) > GraspRules.SELECT_RANGE + 2 || !p.hasLineOfSight(mob)) {
			p.sendOverlayMessage(Component.literal("Look at a mob within " + (int) GraspRules.SELECT_RANGE + " blocks"));
			return false;
		}
		int lvl = d.level(Technique.GRASP);
		GraspRules.Check c = GraspRules.check(Grasp.facts(mob), mob.getMaxHealth(), lvl, Grasp.held(p));
		if (!c.ok()) {
			p.sendOverlayMessage(Component.literal(GraspRules.message(c, lvl, Grasp.held(p))));
			return false;
		}
		int duration = GraspRules.chargeTicks(mob.getMaxHealth());
		TETHERS.put(p.getUUID(), new Tether(mob, duration, lvl));
		AttributeInstance speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			speed.addOrUpdateTransientModifier(new AttributeModifier(SLOW_ID, -0.6, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
		Grasp.sendState(p, mob.getId(), duration);
		p.level().playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.6f, 0.6f);
		return true;
	}

	/** Ends the tether of a player without a cooldown. */
	static void end(ServerPlayer p, boolean notify) {
		Tether t = TETHERS.remove(p.getUUID());
		AttributeInstance speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			speed.removeModifier(SLOW_ID);
		}
		if (t != null && notify && p.connection != null) {
			Grasp.sendState(p, -1, 0);
		}
	}

	private static void tickTether(ServerPlayer p, Tether t) {
		Mob m = t.target;
		if (!p.isAlive() || p.isSpectator() || Grasp.level(p) < 1 || m.isRemoved() || !m.isAlive() || m.level() != p.level()
			|| m.distanceTo(p) > GraspRules.BREAK_RANGE) {
			end(p, true);
			return;
		}
		if (p.hasLineOfSight(m)) {
			t.noSight = 0;
		} else if (++t.noSight > GraspRules.NO_SIGHT_TICKS) {
			end(p, true);
			return;
		}
		t.age++;
		if (t.age % 4 == 0 && p.level() instanceof ServerLevel sl) {
			sl.sendParticles(ParticleTypes.END_ROD, m.getX(), m.getY() + m.getBbHeight() / 2, m.getZ(), 1, 0.3, 0.3, 0.3, 0.01);
		}
		if (t.age >= t.duration) {
			complete(p, t);
		}
	}

	private static void complete(ServerPlayer p, Tether t) {
		Mob m = t.target;
		int lvl = Grasp.level(p);
		GraspRules.Check c = GraspRules.check(Grasp.facts(m), m.getMaxHealth(), lvl, Grasp.held(p));
		end(p, true);
		if (!c.ok()) {
			p.sendOverlayMessage(Component.literal(GraspRules.message(c, lvl, Grasp.held(p))));
			return;
		}
		Grasp.tame(p, m, lvl);
		long now = p.level().getServer().overworld().getGameTime();
		TechniqueAttachment.set(p, TechniqueAttachment.get(p).withCooldown(Technique.GRASP, now + io.github.huydangtruong.immortalcultivation.passive.Passives.cooldown(p, GraspRules.COOLDOWN_SECONDS * 20L)));
	}
}
