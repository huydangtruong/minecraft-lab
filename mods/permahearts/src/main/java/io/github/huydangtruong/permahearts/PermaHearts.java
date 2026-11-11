package io.github.huydangtruong.permahearts;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleEvents;
import io.github.huydangtruong.permahearts.wanderer.QuestCommand;
import io.github.huydangtruong.permahearts.wanderer.Wanderer;
import io.github.huydangtruong.permahearts.wanderer.WandererMenus;
import io.github.huydangtruong.permahearts.wanderer.WandererNpc;
import io.github.huydangtruong.permahearts.wanderer.WandererScheduler;
import io.github.huydangtruong.permahearts.wanderer.Phase;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
		HeartsAttachment.WANDERER.identifier();
		ElixirItems.init();
		WandererMenus.init();
		WandererScheduler.register();
		GameRuleEvents.changeCallback(ENABLED).register((Boolean value, MinecraftServer server) -> {
			for (ServerPlayer p : server.getPlayerList().getPlayers()) {
				HeartsAttachment.setActive(p, value);
				HealthApplier.apply(p, false);
				p.sendSystemMessage(Messages.toggled(value));
			}
		});
		CommandRegistrationCallback.EVENT.register((d, c, s) -> {
			HeartsCommand.register(d);
			QuestCommand.register(d);
		});
		// Right-click on the Wanderer opens the quest screen instead of the trade screen (SUCCESS also blocks vanilla).
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			if (!WandererNpc.isWanderer(entity)) {
				return InteractionResult.PASS;
			}
			if (hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer sp) {
				if (enabled(sp.level().getServer())) {
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
				}
			}
		});
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
			if (entity.getKillCredit() instanceof ServerPlayer killer && enabled(killer.level().getServer())) {
				Wanderer.onKill(killer.level().getServer(), killer, entity.getType());
			}
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
