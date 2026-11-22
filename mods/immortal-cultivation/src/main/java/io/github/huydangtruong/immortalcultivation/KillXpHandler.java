package io.github.huydangtruong.immortalcultivation;

import io.github.huydangtruong.immortalcultivation.wanderer.DeathMatch;
import io.github.huydangtruong.immortalcultivation.wanderer.Wanderer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.monster.Enemy;

/** Kill credit (Death Match, Wanderer quests, HeartXP) and crop-break XP (moved from ImmortalCultivation.onInitialize). */
final class KillXpHandler {
	private KillXpHandler() {}

	static void register() {
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayer victim) {
				DeathMatch.onPlayerKilled(victim.level().getServer(), victim, source);
			}
			if (entity.getKillCredit() instanceof ServerPlayer killer && ImmortalCultivation.enabled(killer.level().getServer())) {
				Wanderer.onKill(killer.level().getServer(), killer, entity.getType());
			}
			if ((entity instanceof Enemy || entity instanceof io.github.huydangtruong.immortalcultivation.godslayer.GodSlayer) && entity.getKillCredit() instanceof ServerPlayer sp
				&& ImmortalCultivation.enabled(sp.level().getServer())) {
				onHostileKill(sp, HeartRules.xpFor(entity.getMaxHealth()));
			}
		});
		net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, be) -> {
			if (player instanceof ServerPlayer sp && ripeCrop(state)) {
				ImmortalCultivation.onCropHarvest(sp, 1);
			}
		});
	}

	/** D140: true when the state is a crop (CropBlock, nether wart, cocoa) in its last two growth stages. */
	public static boolean ripeCrop(net.minecraft.world.level.block.state.BlockState st) {
		var b = st.getBlock();
		if (b instanceof net.minecraft.world.level.block.CropBlock c) {
			return HeartRules.cropGivesXp(c.getAge(st), c.getMaxAge());
		}
		if (b instanceof net.minecraft.world.level.block.NetherWartBlock) {
			return HeartRules.cropGivesXp(st.getValue(net.minecraft.world.level.block.NetherWartBlock.AGE), net.minecraft.world.level.block.NetherWartBlock.MAX_AGE);
		}
		if (b instanceof net.minecraft.world.level.block.CocoaBlock) {
			return HeartRules.cropGivesXp(st.getValue(net.minecraft.world.level.block.CocoaBlock.AGE), net.minecraft.world.level.block.CocoaBlock.MAX_AGE);
		}
		return false;
	}

	static void onHostileKill(ServerPlayer sp, int xp) {
		HeartsData before = HeartsAttachment.get(sp);
		HeartRules.KillResult r = HeartRules.onHostileKill(before, xp);
		HeartsAttachment.set(sp, r.data());
		switch (r.outcome()) {
			case PROGRESS -> sp.sendOverlayMessage(Messages.xpGained(r.xpGained()));
			case GAINED -> {
				HealthApplier.apply(sp, false);
				sp.setHealth(Math.min(sp.getMaxHealth(), sp.getHealth() + 2f * r.heartsGained()));
				sp.level().playSound(null, sp.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 1f);
				sp.sendSystemMessage(Messages.gained(r.heartsGained(), HeartRules.maxHearts(r.data())));
			}
			// AT_CAP only happens when already at the cap: overlay, not chat, so farming at the cap doesn't spam chat.
			case AT_CAP -> sp.sendOverlayMessage(Messages.cap());
		}
	}
}
