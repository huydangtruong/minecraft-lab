package io.github.huydangtruong.immortalcultivation;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.event.Event;
import io.github.huydangtruong.immortalcultivation.wanderer.BloodMoon;
import io.github.huydangtruong.immortalcultivation.wanderer.DeathMatch;
import io.github.huydangtruong.immortalcultivation.wanderer.QuestBoss;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleEvents;
import io.github.huydangtruong.immortalcultivation.wanderer.QuestCommand;
import io.github.huydangtruong.immortalcultivation.wanderer.Wanderer;
import io.github.huydangtruong.immortalcultivation.wanderer.WandererMenus;
import io.github.huydangtruong.immortalcultivation.wanderer.WandererNpc;
import io.github.huydangtruong.immortalcultivation.wanderer.WandererScheduler;
import io.github.huydangtruong.immortalcultivation.wanderer.Phase;
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

public class ImmortalCultivation implements ModInitializer {
	/** Data namespace of every Identifier, resource folder, lang key and saved field. NEVER change it: players' saves and options depend on it (D132). */
	public static final String NAMESPACE = "permahearts";
	/** Fabric mod id (fabric.mod.json, jar, release tag). Not the data namespace. */
	public static final String MOD_ID = "immortal-cultivation";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static final Identifier AFTER_ATTACHMENT_COPY = Identifier.fromNamespaceAndPath(NAMESPACE, "after_attachment_copy");

	/** Per-world ImmortalCultivation switch (D10, D11). Default off; Game Rules screen at world creation or /gamerule permahearts:immortal_cultivation. */
	/** Own game rule group (D78): the screen draws group titles in bold yellow; the title carries the mod version. */
	public static final GameRuleCategory RULES = GameRuleCategory.register(Identifier.fromNamespaceAndPath(NAMESPACE, "immortal_cultivation"));

	public static final GameRule<Boolean> ENABLED = GameRuleBuilder.forBoolean(false)
		.category(RULES)
		.buildAndRegister(Identifier.fromNamespaceAndPath(NAMESPACE, "immortal_cultivation"));

	/** A player finished eating (FoodPropertiesMixin): Vitality elixirs heal extra (D30). */
	public static void onAte(ServerPlayer p, int nutrition) {
		if (!enabled(p.level().getServer()) || !p.isAlive()) {
			return;
		}
		float extra = ElixirStat.foodHeal(nutrition, HeartsAttachment.getCultivation(p).level(ElixirStat.VITALITY));
		if (extra > 0) {
			p.heal(extra);
		}
	}

	/** Natural regeneration from food is stronger with Vitality elixirs (D41, FoodDataMixin). */
	public static float regenMultiplier(ServerPlayer p) {
		if (!enabled(p.level().getServer())) {
			return 1f;
		}
		return (float) ElixirStat.regenMultiplier(HeartsAttachment.getCultivation(p).level(ElixirStat.VITALITY));
	}

	public static boolean enabled(MinecraftServer server) {
		return server.getGameRules().get(ENABLED);
	}

	@Override
	public void onInitialize() {
		HeartsAttachment.HEARTS.identifier(); // touch the class so the attachments are registered
		HeartsAttachment.ACTIVE.identifier();
		HeartsAttachment.CULTIVATION.identifier();
		HeartsAttachment.WANDERER.identifier();
		io.github.huydangtruong.immortalcultivation.technique.TechniqueAttachment.TECHNIQUES.identifier();
		io.github.huydangtruong.immortalcultivation.passive.PassiveAttachment.PASSIVES.identifier();
		io.github.huydangtruong.immortalcultivation.technique.TechniqueNet.register();
		io.github.huydangtruong.immortalcultivation.passive.PassiveNet.register(); // P-057: Passives tab
		io.github.huydangtruong.immortalcultivation.passive.CropGrowth.register(); // P-058: Crop growth
		io.github.huydangtruong.immortalcultivation.treasure.DeadlyTreasure.register(); // P-059: Deadly Treasure
		io.github.huydangtruong.immortalcultivation.notes.NotesNet.register(); // P-042: Notes panel
		io.github.huydangtruong.immortalcultivation.technique.TechniqueSelection.init();
		HeartsAttachment.DM_PROTECTED.identifier();
		ElixirItems.init();
		io.github.huydangtruong.immortalcultivation.technique.FocusItems.init();
		io.github.huydangtruong.immortalcultivation.technique.TechniqueCasts.init(); // P-011 part C: technique effects
		WandererMenus.init();
		io.github.huydangtruong.immortalcultivation.reward.RewardBoxes.init(); // P-027: reward boxes
		WandererScheduler.register();
		io.github.huydangtruong.immortalcultivation.godslayer.GodSlayer.init(); // P-032: God Slayer boss
		io.github.huydangtruong.immortalcultivation.godslayer.GodSlayerHunt.register();
		io.github.huydangtruong.immortalcultivation.stats.StatsSync.register();
		io.github.huydangtruong.immortalcultivation.danger.Danger.init(); // P-054: danger level
		io.github.huydangtruong.immortalcultivation.danger.DangerMobs.init();
		GameRuleEvents.changeCallback(ENABLED).register((Boolean value, MinecraftServer server) -> {
			for (ServerPlayer p : server.getPlayerList().getPlayers()) {
				HeartsAttachment.setActive(p, value);
				HealthApplier.apply(p, false);
				p.sendSystemMessage(Messages.toggled(value));
			}
		});
		CommandRegistrationCallback.EVENT.register((d, c, s) -> {
			HeartsCommand.register(d);
			TestModeCommand.register(d);
			QuestCommand.register(d);
			io.github.huydangtruong.immortalcultivation.godslayer.BossCommand.register(d);
			io.github.huydangtruong.immortalcultivation.technique.TechniqueCommand.register(d);
			io.github.huydangtruong.immortalcultivation.danger.DangerCommand.register(d);
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
			HeartsAttachment.setActive(p, enabled(p.level().getServer()));
			HealthApplier.apply(p, false);
		});
		// Fabric copies attachments old -> new player in its own AFTER_RESPAWN listener (default phase). Run after it,
		// or that copy overwrites what we set (docs/system/02-findings.md). Data is read from the old player anyway.
		ServerPlayerEvents.AFTER_RESPAWN.register((old, p, alive) -> BloodMoon.onRespawn(p));
		ServerPlayerEvents.AFTER_RESPAWN.addPhaseOrdering(Event.DEFAULT_PHASE, AFTER_ATTACHMENT_COPY);
		ServerPlayerEvents.AFTER_RESPAWN.register(AFTER_ATTACHMENT_COPY, (old, p, alive) -> {
			HeartsData before = HeartsAttachment.get(old);
			boolean on = enabled(p.level().getServer());
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
				p.sendSystemMessage(Messages.deathMatchProtected());
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
			if (entity instanceof ServerPlayer victim) {
				DeathMatch.onPlayerKilled(victim.level().getServer(), victim, source);
			}
			if (entity.getKillCredit() instanceof ServerPlayer killer && enabled(killer.level().getServer())) {
				Wanderer.onKill(killer.level().getServer(), killer, entity.getType());
			}
			if ((entity instanceof Enemy || entity instanceof io.github.huydangtruong.immortalcultivation.godslayer.GodSlayer) && entity.getKillCredit() instanceof ServerPlayer sp
				&& enabled(sp.level().getServer())) {
				onHostileKill(sp, HeartRules.xpFor(entity.getMaxHealth()));
			}
		});
		net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, be) -> {
			if (player instanceof ServerPlayer sp && ripeCrop(state)) {
				onCropHarvest(sp, 1);
			}
		});
		LOGGER.info("ImmortalCultivation loaded");
	}

	/** D140: vanilla experience (not HeartXP) for crops (player break, Illusion harvest), given straight to the player. Nothing when the mod is off or the player is Creative/Spectator. */
	public static void onCropHarvest(ServerPlayer sp, int crops) {
		if (crops > 0 && enabled(sp.level().getServer()) && !sp.isCreative() && !sp.isSpectator()) {
			sp.giveExperiencePoints(io.github.huydangtruong.immortalcultivation.passive.Passives.bonusXp(sp, HeartRules.CROP_XP * crops)); // D148 XP gain bonus
		}
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
