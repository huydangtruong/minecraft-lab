package io.github.huydangtruong.permahearts;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PermaHearts implements ModInitializer {
	public static final String MOD_ID = "permahearts";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static final Identifier AFTER_ATTACHMENT_COPY = Identifier.fromNamespaceAndPath(MOD_ID, "after_attachment_copy");

	@Override
	public void onInitialize() {
		HeartsAttachment.HEARTS.identifier(); // touch the class so the attachment is registered
		CommandRegistrationCallback.EVENT.register((d, c, s) -> HeartsCommand.register(d));
		ServerPlayerEvents.JOIN.register(p -> HealthApplier.apply(p, false));
		// Fabric copies attachments old -> new player in its own AFTER_RESPAWN listener (default phase). Run after it,
		// or that copy overwrites what we set (docs/system/02-findings.md). Data is read from the old player anyway.
		ServerPlayerEvents.AFTER_RESPAWN.addPhaseOrdering(Event.DEFAULT_PHASE, AFTER_ATTACHMENT_COPY);
		ServerPlayerEvents.AFTER_RESPAWN.register(AFTER_ATTACHMENT_COPY, (old, p, alive) -> {
			HeartsData before = HeartsAttachment.get(old);
			if (alive) {
				HeartsAttachment.set(p, before);
				HealthApplier.apply(p, false);
				return;
			}
			HeartRules.DeathResult r = HeartRules.onDeath(before);
			HeartsAttachment.set(p, r.data());
			HealthApplier.apply(p, true);
			p.sendSystemMessage(r.outcome() == HeartRules.DeathOutcome.LOST
				? Messages.lost(HeartRules.maxHearts(r.data()))
				: Messages.floor());
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof Enemy && entity.getKillCredit() instanceof ServerPlayer sp) {
				onHostileKill(sp);
			}
		});
		LOGGER.info("PermaHearts loaded");
	}

	static void onHostileKill(ServerPlayer sp) {
		HeartRules.KillResult r = HeartRules.onHostileKill(HeartsAttachment.get(sp));
		HeartsAttachment.set(sp, r.data());
		switch (r.outcome()) {
			case PROGRESS -> sp.sendOverlayMessage(Messages.progress(r.data().killProgress(), HeartRules.KILLS_PER_HEART));
			case GAINED -> {
				HealthApplier.apply(sp, false);
				sp.setHealth(Math.min(sp.getMaxHealth(), sp.getHealth() + 2f));
				sp.level().playSound(null, sp.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 1f);
				sp.sendSystemMessage(Messages.gained(HeartRules.maxHearts(r.data())));
			}
			case AT_CAP -> sp.sendSystemMessage(Messages.cap());
		}
	}
}
