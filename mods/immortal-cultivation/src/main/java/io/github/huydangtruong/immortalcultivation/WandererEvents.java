package io.github.huydangtruong.immortalcultivation;

import io.github.huydangtruong.immortalcultivation.wanderer.BloodMoon;
import io.github.huydangtruong.immortalcultivation.wanderer.Phase;
import io.github.huydangtruong.immortalcultivation.wanderer.QuestBoss;
import io.github.huydangtruong.immortalcultivation.wanderer.Wanderer;
import io.github.huydangtruong.immortalcultivation.wanderer.WandererMenus;
import io.github.huydangtruong.immortalcultivation.wanderer.WandererNpc;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;

/** Event handlers of the Wanderer NPC, its boss and the Blood Moon (moved from ImmortalCultivation.onInitialize). */
final class WandererEvents {
	private WandererEvents() {}

	/** Interaction, damage and load rules of the Wanderer NPC and its quest boss. */
	static void register() {
		// Right-click on the Wanderer opens the quest screen instead of the trade screen (SUCCESS also blocks vanilla).
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			if (!WandererNpc.isWanderer(entity)) {
				return InteractionResult.PASS;
			}
			if (hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer sp) {
				if (ImmortalCultivation.enabled(sp.level().getServer())) {
					WandererMenus.open(sp, entity);
				} else {
					sp.sendSystemMessage(Messages.disabledHint());
				}
			}
			return InteractionResult.SUCCESS;
		});
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !WandererNpc.isWanderer(entity));
		// A wanderer that is not the stored one (after a reset or respawn) is a stale copy: remove it.
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (WandererNpc.isWanderer(entity)) {
				var d = Wanderer.data(level.getServer());
				boolean current = d.phase() == Phase.PRESENT && d.npcUuid().isPresent()
					&& d.npcUuid().get().equals(entity.getUUID().toString());
				if (!current) {
					entity.discard();
				} else {
					entity.setPermanentlyInvulnerable(true); // NPCs from 0.7.0 worlds: mobs must not target them
				}
			}
			// A boss that is not the saved one (a lost boss came back after a re-summon) is a stale copy.
			if (entity.entityTags().contains(QuestBoss.TAG)) {
				var boss = Wanderer.data(level.getServer()).boss();
				if (boss.isEmpty() || !boss.get().bossUuid().equals(entity.getUUID().toString())) {
					entity.discard();
				}
			}
		});
	}

	/** Blood Moon: sleeping, leaving and the boss death. */
	static void registerBloodMoon() {
		// Blood Moon: no sleeping, the starter dying or leaving ends it, late joiners get the boss bar; boss death ends the boss line.
		EntitySleepEvents.ALLOW_SLEEPING.register((player, pos) ->
			player instanceof ServerPlayer sp && BloodMoon.running(sp.level().getServer())
				? new Player.BedSleepingProblem(Messages.bloodMoonNoSleep()) : null);
		ServerPlayerEvents.LEAVE.register(BloodMoon::onLeave);
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity.level() instanceof ServerLevel level) {
				BloodMoon.onDeath(level.getServer(), entity);
				QuestBoss.onDeath(level.getServer(), entity);
			}
		});
	}
}
