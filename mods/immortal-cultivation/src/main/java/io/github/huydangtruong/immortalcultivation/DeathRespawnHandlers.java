package io.github.huydangtruong.immortalcultivation;

import io.github.huydangtruong.immortalcultivation.wanderer.BloodMoon;
import io.github.huydangtruong.immortalcultivation.wanderer.DeathMatch;
import io.github.huydangtruong.immortalcultivation.wanderer.Wanderer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/** Death Match protection, item/XP return and the heart loss on respawn (moved from ImmortalCultivation.onInitialize). */
final class DeathRespawnHandlers {
	private static final Identifier AFTER_ATTACHMENT_COPY = Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "after_attachment_copy");

	private DeathRespawnHandlers() {}

	static void register() {
		// Death Match (D25): mark a death that fell to another player during the match. Set to the computed value on every
		// player death (not only to true), so a stale flag can never protect a later, normal death. Never cancels the death.
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
			if (entity instanceof ServerPlayer sp) {
				HeartsAttachment.setDeathMatchProtected(sp,
					DeathMatch.isProtectedDeath(sp.level().getServer(), sp, source));
			}
			return true;
		});
		// Items and XP: the Player mixin skips the drops while the flag is set; here they come back to the new player.
		// COPY_FROM runs inside PlayerList.respawn, after vanilla restoreFrom and before AFTER_RESPAWN.
		ServerPlayerEvents.COPY_FROM.register((old, p, alive) -> {
			if (!alive && HeartsAttachment.isDeathMatchProtected(old)) {
				p.getInventory().replaceWith(old.getInventory());
				p.experienceLevel = old.experienceLevel;
				p.totalExperience = old.totalExperience;
				p.experienceProgress = old.experienceProgress;
				p.setScore(old.getScore());
			} else if (!alive && !p.level().getGameRules().get(net.minecraft.world.level.gamerules.GameRules.KEEP_INVENTORY)) {
				io.github.huydangtruong.immortalcultivation.passive.Passives.restore(old, p); // D148 Inventory protect
			}
		});
		ServerPlayerEvents.JOIN.register(p -> {
			BloodMoon.onJoin(p);
			Wanderer.onJoin(p);
			HeartsAttachment.setActive(p, ImmortalCultivation.enabled(p.level().getServer()));
			HealthApplier.apply(p, false);
		});
		// Fabric copies attachments old -> new player in its own AFTER_RESPAWN listener (default phase). Run after it,
		// or that copy overwrites what we set (docs/system/02-findings.md). Data is read from the old player anyway.
		ServerPlayerEvents.AFTER_RESPAWN.register((old, p, alive) -> BloodMoon.onRespawn(p));
		ServerPlayerEvents.AFTER_RESPAWN.addPhaseOrdering(Event.DEFAULT_PHASE, AFTER_ATTACHMENT_COPY);
		ServerPlayerEvents.AFTER_RESPAWN.register(AFTER_ATTACHMENT_COPY, (old, p, alive) -> {
			HeartsData before = HeartsAttachment.get(old);
			boolean on = ImmortalCultivation.enabled(p.level().getServer());
			HeartsAttachment.setActive(p, on);
			// Death Match: the death fell to another player during the match. The flag was copied onto the new player;
			// clear it on every path so it never outlives this respawn.
			boolean protectedDeath = !alive && HeartsAttachment.isDeathMatchProtected(old);
			HeartsAttachment.setDeathMatchProtected(p, false);
			if (!on) {
				// World switched off: no heart lost, no message; keep the data so turning it on again restores everything.
				HeartsAttachment.set(p, before);
				HealthApplier.apply(p, true);
				return;
			}
			if (alive) {
				HeartsAttachment.set(p, before);
				HealthApplier.apply(p, false);
				return;
			}
			if (protectedDeath) {
				HeartsAttachment.set(p, before); // no heart lost
				HealthApplier.apply(p, true);
				p.sendSystemMessage(QuestEventMessages.deathMatchProtected());
				return;
			}
			HeartRules.DeathResult r = HeartRules.onDeath(before);
			HeartsAttachment.set(p, r.data());
			HealthApplier.apply(p, true);
			p.sendSystemMessage(r.outcome() == HeartRules.DeathOutcome.LOST
				? HeartMessages.lost(HeartRules.maxHearts(r.data()))
				: HeartMessages.floor());
		});
	}
}
