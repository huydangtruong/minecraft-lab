package io.github.huydangtruong.immortalcultivation.technique;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;

/**
 * Illusion targets (D71): one list per owner, shared by all their clones. It grows with whoever hits the owner and whoever
 * the owner hits (players too), also when PvP is off and when the hit did no damage. Server only, never saved; the list
 * exists only while the owner has clones.
 */
public final class IllusionTargets {
	private static final Map<UUID, Set<UUID>> LISTS = new HashMap<>();

	private IllusionTargets() {}

	/** Registers the hooks. Called once from Illusion.init. */
	static void init() {
		// Melee by a player on anything (also when the hit is later blocked or does nothing).
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
			if (!world.isClientSide()) {
				noteAttack(player, entity);
			}
			return InteractionResult.PASS;
		});
		// Projectiles and other damage on non-player victims. Player victims go through ServerPlayerHurtMixin, because
		// ServerPlayer#hurtServer refuses the damage under PvP-off before LivingEntity#hurtServer (and this event) is reached.
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (!(entity instanceof ServerPlayer)) {
				noteDamage(entity, source);
			}
			return true;
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(s -> LISTS.clear());
	}

	/** Called from ServerPlayerHurtMixin at the head of ServerPlayer#hurtServer. */
	public static void noteDamage(Entity victim, DamageSource source) {
		if (!LISTS.isEmpty() && source.getEntity() != null) {
			noteAttack(source.getEntity(), victim);
		}
	}

	static void open(UUID owner) {
		LISTS.put(owner, new LinkedHashSet<>());
	}

	static void close(UUID owner) {
		LISTS.remove(owner);
	}

	public static boolean active(UUID owner) {
		return LISTS.containsKey(owner);
	}

	/** Records one attack: adds the victim to the attacker's list when the attacker is an owner, and the attacker to the victim's list. */
	static void noteAttack(Entity attacker, Entity victim) {
		if (LISTS.isEmpty() || attacker == null || victim == null) {
			return;
		}
		if (attacker instanceof Player p && active(p.getUUID())) {
			consider(p, victim);
		}
		if (victim instanceof Player v && active(v.getUUID())) {
			consider(v, attacker);
		}
		if (victim instanceof Illusion c && c.owner() != null && active(c.owner()) && !c.owner().equals(attacker.getUUID())) {
			Player o = c.level().getPlayerByUUID(c.owner());
			if (o != null) {
				consider(o, attacker);
			}
		}
	}

	private static void consider(Player owner, Entity e) {
		if (eligible(owner, e)) {
			LISTS.get(owner.getUUID()).add(e.getUUID());
		}
	}

	/** True when the owner's clones may fight this entity: never the owner, their clones, guardians, angels or tamed animals. */
	static boolean eligible(Player owner, Entity e) {
		if (!(e instanceof LivingEntity le) || !le.isAlive() || le.isSpectator() || e == owner) {
			return false;
		}
		if (e instanceof OwnedSummon s && owner.getUUID().equals(s.owner())) {
			return false;
		}
		return !(e instanceof TamableAnimal t && t.isOwnedBy(owner));
	}

	/** The living entities on the owner's list, found in the level; dead and gone ones drop off the list. */
	static List<LivingEntity> of(ServerLevel level, UUID owner) {
		Set<UUID> ids = LISTS.get(owner);
		List<LivingEntity> out = new ArrayList<>();
		if (ids == null) {
			return out;
		}
		var it = ids.iterator();
		while (it.hasNext()) {
			Entity e = level.getEntity(it.next());
			if (e instanceof LivingEntity le && le.isAlive()) {
				out.add(le);
			} else {
				it.remove(); // dead, removed or left this level
			}
		}
		return out;
	}

	/** For tests: the ids on the list. */
	public static Set<UUID> idsOf(UUID owner) {
		Set<UUID> s = LISTS.get(owner);
		return s == null ? Set.of() : Set.copyOf(s);
	}
}
