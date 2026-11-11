package io.github.huydangtruong.permahearts;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PermaHearts implements ModInitializer {
	public static final String MOD_ID = "permahearts";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static final Identifier AFTER_ATTACHMENT_COPY = Identifier.fromNamespaceAndPath(MOD_ID, "after_attachment_copy");

	/** Per-world ImmortalCultivation switch (D10, D11). Default off; Game Rules screen at world creation or /gamerule permahearts:immortal_cultivation. */
	public static final GameRule<Boolean> ENABLED = GameRuleBuilder.forBoolean(false)
		.category(GameRuleCategory.PLAYER)
		.buildAndRegister(Identifier.fromNamespaceAndPath(MOD_ID, "immortal_cultivation"));

	public static boolean enabled(MinecraftServer server) {
		return server.getGameRules().get(ENABLED);
	}

	@Override
	public void onInitialize() {
		HeartsAttachment.HEARTS.identifier(); // touch the class so the attachments are registered
		HeartsAttachment.ACTIVE.identifier();
		HeartsAttachment.CULTIVATION.identifier();
		ElixirItems.init();
		GameRuleEvents.changeCallback(ENABLED).register((Boolean value, MinecraftServer server) -> {
			for (ServerPlayer p : server.getPlayerList().getPlayers()) {
				HeartsAttachment.setActive(p, value);
				HealthApplier.apply(p, false);
				p.sendSystemMessage(Messages.toggled(value));
			}
		});
		CommandRegistrationCallback.EVENT.register((d, c, s) -> HeartsCommand.register(d));
		ServerPlayerEvents.JOIN.register(p -> {
			HeartsAttachment.setActive(p, enabled(p.level().getServer()));
			HealthApplier.apply(p, false);
		});
		// Fabric copies attachments old -> new player in its own AFTER_RESPAWN listener (default phase). Run after it,
		// or that copy overwrites what we set (docs/system/02-findings.md). Data is read from the old player anyway.
		ServerPlayerEvents.AFTER_RESPAWN.addPhaseOrdering(Event.DEFAULT_PHASE, AFTER_ATTACHMENT_COPY);
		ServerPlayerEvents.AFTER_RESPAWN.register(AFTER_ATTACHMENT_COPY, (old, p, alive) -> {
			HeartsData before = HeartsAttachment.get(old);
			boolean on = enabled(p.level().getServer());
			HeartsAttachment.setActive(p, on);
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
			HeartRules.DeathResult r = HeartRules.onDeath(before);
			HeartsAttachment.set(p, r.data());
			HealthApplier.apply(p, true);
			p.sendSystemMessage(r.outcome() == HeartRules.DeathOutcome.LOST
				? Messages.lost(HeartRules.maxHearts(r.data()))
				: Messages.floor());
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof Enemy && entity.getKillCredit() instanceof ServerPlayer sp
				&& enabled(sp.level().getServer())) {
				onHostileKill(sp, HeartRules.xpFor(entity.getMaxHealth()));
			}
		});
		LOGGER.info("PermaHearts loaded");
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
			// AT_CAP only happens when already at the cap: overlay, not chat, so farming at 50 hearts doesn't spam chat.
			case AT_CAP -> sp.sendOverlayMessage(Messages.cap());
		}
	}
}
