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
		io.github.huydangtruong.immortalcultivation.passive.ItemGuard.init(); // P-065: Item guard debt component
		io.github.huydangtruong.immortalcultivation.technique.TechniqueNet.register();
		io.github.huydangtruong.immortalcultivation.passive.PassiveNet.register(); // P-057: Passives tab
		io.github.huydangtruong.immortalcultivation.passive.CropGrowth.register(); // P-058: Crop growth
		io.github.huydangtruong.immortalcultivation.treasure.DeadlyTreasure.register(); // P-059: Deadly Treasure
		io.github.huydangtruong.immortalcultivation.danger.RaidGlow.register(); // D190: raid mobs glow
		// D217: Notes disabled
		// io.github.huydangtruong.immortalcultivation.notes.NotesNet.register(); // P-042: Notes panel
		io.github.huydangtruong.immortalcultivation.trash.TrashNet.register(); // D217: trash slot
		io.github.huydangtruong.immortalcultivation.quit.QuitNet.register(); // D218: /quit
		io.github.huydangtruong.immortalcultivation.quit.Quit.register();
		io.github.huydangtruong.immortalcultivation.admin.AdminNetworking.register(); // P-060: /admin panel
		io.github.huydangtruong.immortalcultivation.technique.TechniqueSelection.init();
		HeartsAttachment.DM_PROTECTED.identifier();
		ElixirItems.init();
		io.github.huydangtruong.immortalcultivation.technique.FocusItems.init();
		io.github.huydangtruong.immortalcultivation.technique.TechniqueCasts.init(); // P-011 part C: technique effects
		io.github.huydangtruong.immortalcultivation.technique.chrono.Chrono.init(); // D268: Time Rewind
		WandererMenus.init();
		io.github.huydangtruong.immortalcultivation.reward.RewardBoxes.init(); // P-027: reward boxes
		WandererScheduler.register();
		io.github.huydangtruong.immortalcultivation.godslayer.GodSlayer.init(); // P-032: God Slayer boss
		io.github.huydangtruong.immortalcultivation.godslayer.GodSlayerHunt.register();
		io.github.huydangtruong.immortalcultivation.stats.StatsSync.register();
		io.github.huydangtruong.immortalcultivation.danger.Danger.init(); // P-054: danger level
		io.github.huydangtruong.immortalcultivation.danger.DangerMobs.init();
		io.github.huydangtruong.immortalcultivation.rune.Runes.init(); // D241-D242: runes and the Rune Forge
		io.github.huydangtruong.immortalcultivation.purgatory.Purgatory.register(); // D234-D240: Purgatory
		GameRuleEvents.changeCallback(ENABLED).register((Boolean value, MinecraftServer server) -> {
			for (ServerPlayer p : server.getPlayerList().getPlayers()) {
				HeartsAttachment.setActive(p, value);
				HealthApplier.apply(p, false);
				p.sendSystemMessage(HeartMessages.toggled(value));
			}
		});
		CommandRegistrationCallback.EVENT.register((d, c, s) -> {
			HeartsCommand.register(d);
			TestModeCommand.register(d);
			QuestCommand.register(d);
			io.github.huydangtruong.immortalcultivation.godslayer.BossCommand.register(d);
			io.github.huydangtruong.immortalcultivation.technique.TechniqueCommand.register(d);
			io.github.huydangtruong.immortalcultivation.danger.DangerCommand.register(d);
			io.github.huydangtruong.immortalcultivation.admin.AdminCommand.register(d);
			io.github.huydangtruong.immortalcultivation.quit.Quit.registerCommand(d);
			io.github.huydangtruong.immortalcultivation.purgatory.PurgatoryCommand.register(d); // D234
		});
		WandererEvents.register();
		WandererEvents.registerBloodMoon();
		DeathRespawnHandlers.register();
		KillXpHandler.register();
		LOGGER.info("ImmortalCultivation loaded");
	}

	/** D140: vanilla experience (not HeartXP) for crops (player break, Illusion harvest), given straight to the player. Nothing when the mod is off or the player is Creative/Spectator. */
	public static void onCropHarvest(ServerPlayer sp, int crops) {
		if (crops > 0 && enabled(sp.level().getServer()) && !sp.isCreative() && !sp.isSpectator()) {
			sp.giveExperiencePoints(io.github.huydangtruong.immortalcultivation.passive.Passives.bonusXp(sp, HeartRules.CROP_XP * crops)); // D148 XP gain bonus
		}
	}
}
