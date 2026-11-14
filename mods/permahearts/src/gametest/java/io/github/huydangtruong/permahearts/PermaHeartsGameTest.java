package io.github.huydangtruong.permahearts;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class PermaHeartsGameTest {
	private static void enable(GameTestHelper context, boolean on) {
		MinecraftServer s = context.getLevel().getServer();
		s.getGameRules().set(PermaHearts.ENABLED, on, s);
	}

	private static void kill(GameTestHelper context, ServerPlayer p, LivingEntity e) {
		ServerLevel level = context.getLevel();
		e.hurtServer(level, level.damageSources().playerAttack(p), 1000f);
	}

	@GameTest(maxTicks = 100)
	public void healthMatchesBonus(GameTestHelper context) {
		enable(context, true);
		ServerPlayer p = context.makeMockServerPlayerInLevel();
		HeartsAttachment.set(p, new HeartsData(-3, 0, 0, 0));
		HealthApplier.apply(p, true);
		context.assertTrue(p.getMaxHealth() == 14f, "max health should be 14, was " + p.getMaxHealth());
		context.succeed();
	}

	@GameTest(maxTicks = 100)
	public void hostileKillGivesXpCowDoesNot(GameTestHelper context) {
		enable(context, true);
		ServerPlayer p = context.makeMockServerPlayerInLevel();
		HeartsAttachment.set(p, HeartsData.DEFAULT);
		Zombie z = context.spawn(EntityTypes.ZOMBIE, 1, 1, 1);
		int expected = (int) z.getMaxHealth();
		kill(context, p, z);
		context.assertTrue(HeartsAttachment.get(p).heartXp() == expected, "zombie kill should give " + expected + " XP, was " + HeartsAttachment.get(p).heartXp());
		kill(context, p, context.spawn(EntityTypes.COW, 2, 1, 1));
		context.assertTrue(HeartsAttachment.get(p).heartXp() == expected, "cow kill must not change XP");
		context.succeed();
	}

	@GameTest(maxTicks = 100)
	public void fullBarGivesHeart(GameTestHelper context) {
		enable(context, true);
		ServerPlayer p = context.makeMockServerPlayerInLevel();
		HeartsAttachment.set(p, new HeartsData(0, 480, 99, 0));
		HealthApplier.apply(p, true);
		kill(context, p, context.spawn(EntityTypes.ZOMBIE, 1, 1, 1));
		context.assertTrue(HeartRules.maxHearts(HeartsAttachment.get(p)) == 11, "should have 11 hearts");
		context.assertTrue(p.getMaxHealth() == 22f, "max health should be 22, was " + p.getMaxHealth());
		context.succeed();
	}

	@GameTest(maxTicks = 100)
	public void bigMobGivesHeart(GameTestHelper context) {
		enable(context, true);
		ServerPlayer p = context.makeMockServerPlayerInLevel();
		HeartsAttachment.set(p, HeartsData.DEFAULT);
		HealthApplier.apply(p, true);
		kill(context, p, context.spawn(EntityTypes.WARDEN, 1, 1, 1));
		HeartsData d = HeartsAttachment.get(p);
		context.assertTrue(HeartRules.maxHearts(d) == 11, "warden (500 XP) should give 11 hearts, had " + HeartRules.maxHearts(d) + " xp " + d.heartXp());
		context.succeed();
	}

	/**
	 * Runs the player's real death (ServerPlayer.die) and respawns it through the vanilla PlayerList, like the
	 * respawn button. The game-test mock player is Creative and never "loaded", so it is invulnerable to damage:
	 * set health to 0 and call die() directly instead of hurting it.
	 */
	private static ServerPlayer dieAndRespawn(GameTestHelper context, ServerPlayer p) {
		p.setHealth(0f);
		p.die(context.getLevel().damageSources().generic());
		context.assertTrue(p.isDeadOrDying(), "player should be dead");
		return context.getLevel().getServer().getPlayerList().respawn(p, false, Entity.RemovalReason.KILLED);
	}

	@GameTest(maxTicks = 100)
	public void deathCostsOneHeartAndRespawnsFull(GameTestHelper context) {
		enable(context, true);
		ServerPlayer p = context.makeMockServerPlayerInLevel();
		HeartsAttachment.set(p, new HeartsData(0, 42, 542, 0));
		HealthApplier.apply(p, true);
		ServerPlayer r = dieAndRespawn(context, p);
		HeartsData d = HeartsAttachment.get(r);
		context.assertTrue(HeartRules.maxHearts(d) == 9, "should have 9 hearts after dying, had " + HeartRules.maxHearts(d));
		context.assertTrue(d.heartXp() == 42, "HeartXP must survive death, was " + d.heartXp());
		context.assertTrue(d.totalHostileKills() == 542, "total kills must survive death");
		context.assertTrue(d.totalDeaths() == 1, "deaths should be 1, was " + d.totalDeaths());
		context.assertTrue(r.getMaxHealth() == 18f, "max health should be 18, was " + r.getMaxHealth());
		context.assertTrue(r.getHealth() == 18f, "should respawn with full health 18, was " + r.getHealth());
		// Second death keeps stacking.
		ServerPlayer r2 = dieAndRespawn(context, r);
		context.assertTrue(r2.getMaxHealth() == 16f, "max health after 2 deaths should be 16, was " + r2.getMaxHealth());
		context.succeed();
	}

	@GameTest(maxTicks = 100)
	public void deathAtOneHeartStaysAtOne(GameTestHelper context) {
		enable(context, true);
		ServerPlayer p = context.makeMockServerPlayerInLevel();
		HeartsAttachment.set(p, HeartRules.withHearts(HeartsData.DEFAULT, 1));
		HealthApplier.apply(p, true);
		context.assertTrue(p.getMaxHealth() == 2f, "1 heart = 2 health, was " + p.getMaxHealth());
		ServerPlayer r = dieAndRespawn(context, p);
		context.assertTrue(HeartRules.maxHearts(HeartsAttachment.get(r)) == 1, "floor is 1 heart");
		context.assertTrue(r.getMaxHealth() == 2f, "max health should stay 2, was " + r.getMaxHealth());
		context.assertTrue(r.getHealth() == 2f, "should respawn with 2 health, was " + r.getHealth());
		context.succeed();
	}

	@GameTest(maxTicks = 100)
	public void killAtCapKeepsMaxHearts(GameTestHelper context) {
		enable(context, true);
		ServerPlayer p = context.makeMockServerPlayerInLevel();
		HeartsAttachment.set(p, HeartRules.withHearts(HeartsData.DEFAULT, 300));
		HealthApplier.apply(p, true);
		context.assertTrue(p.getMaxHealth() == 600f, "300 hearts = 600 health, was " + p.getMaxHealth());
		kill(context, p, context.spawn(EntityTypes.ZOMBIE, 1, 1, 1));
		HeartsData d = HeartsAttachment.get(p);
		context.assertTrue(HeartRules.maxHearts(d) == 300, "cap is 300 hearts");
		context.assertTrue(d.heartXp() == 0, "XP stays 0 at cap, was " + d.heartXp());
		context.assertTrue(p.getMaxHealth() == 600f, "max health should stay 600");
		context.succeed();
	}

	@GameTest(maxTicks = 100)
	public void joinWithDefaultDataHasTenHearts(GameTestHelper context) {
		enable(context, true);
		ServerPlayer p = context.makeMockServerPlayerInLevel();
		context.assertTrue(p.getMaxHealth() == 20f, "new player should have 20 health, was " + p.getMaxHealth());
		context.succeed();
	}

	/** Runs a command through the dispatcher so errors fail the test instead of being swallowed. */
	private static void run(GameTestHelper context, CommandSourceStack source, String command) {
		try {
			context.getLevel().getServer().getCommands().getDispatcher().execute(command, source);
		} catch (CommandSyntaxException e) {
			context.fail("/" + command + " failed: " + e.getMessage());
		}
	}

	@GameTest(maxTicks = 100)
	public void adminCommandsChangeData(GameTestHelper context) {
		enable(context, true);
		ServerPlayer p = context.makeMockServerPlayerInLevel();
		MinecraftServer server = context.getLevel().getServer();
		// Admin permissions, executing as this player so @s targets it (mock players all share one name, and a UUID is an entity selector).
		CommandSourceStack admin = server.createCommandSourceStack().withEntity(p);
		String name = "@s";
		run(context, admin, "hearts set " + name + " 45");
		context.assertTrue(HeartRules.maxHearts(HeartsAttachment.get(p)) == 45, "set should give 45 hearts, had " + HeartRules.maxHearts(HeartsAttachment.get(p)));
		context.assertTrue(p.getMaxHealth() == 90f, "max health should be 90, was " + p.getMaxHealth());
		run(context, admin, "hearts xp " + name + " 250");
		context.assertTrue(HeartsAttachment.get(p).heartXp() == 250, "XP should be 250");
		// Out of range is rejected, data unchanged.
		boolean rejected = false;
		try {
			server.getCommands().getDispatcher().execute("hearts set " + name + " 301", admin);
		} catch (CommandSyntaxException e) {
			rejected = true;
		}
		context.assertTrue(rejected, "hearts set 301 should be a syntax error");
		context.assertTrue(HeartRules.maxHearts(HeartsAttachment.get(p)) == 45, "301 must be rejected");
		// Plain /hearts as the player runs without changing anything.
		run(context, p.createCommandSourceStack(), "hearts");
		context.assertTrue(HeartRules.maxHearts(HeartsAttachment.get(p)) == 45, "/hearts must not change data");
		context.succeed();
	}

	@GameTest(maxTicks = 100)
	public void disabledWorldIgnoresDeathsAndKills(GameTestHelper context) {
		try {
			enable(context, true);
			ServerPlayer p = context.makeMockServerPlayerInLevel();
			HeartsAttachment.set(p, new HeartsData(5, 100, 0, 0));
			HealthApplier.apply(p, true);
			context.assertTrue(p.getMaxHealth() == 30f, "15 hearts should be 30 health while on, was " + p.getMaxHealth());
			enable(context, false);
			HealthApplier.apply(p, true);
			context.assertTrue(p.getMaxHealth() == 20f, "off: max health should be 20, was " + p.getMaxHealth());
			kill(context, p, context.spawn(EntityTypes.ZOMBIE, 1, 1, 1));
			context.assertTrue(HeartsAttachment.get(p).heartXp() == 100, "off: kill must not give XP, was " + HeartsAttachment.get(p).heartXp());
			ServerPlayer r = dieAndRespawn(context, p);
			HeartsData d = HeartsAttachment.get(r);
			context.assertTrue(HeartRules.maxHearts(d) == 15, "off: hearts unchanged, had " + HeartRules.maxHearts(d));
			context.assertTrue(d.heartXp() == 100, "off: XP unchanged, was " + d.heartXp());
			context.assertTrue(d.totalDeaths() == 0, "off: deaths not counted, was " + d.totalDeaths());
			context.assertTrue(r.getMaxHealth() == 20f, "off: max health 20 after respawn, was " + r.getMaxHealth());
			context.assertTrue(r.getHealth() == 20f, "off: health 20 after respawn, was " + r.getHealth());
			context.assertTrue(!r.getAttachedOrElse(HeartsAttachment.ACTIVE, false), "ACTIVE should be false");
			enable(context, true);
			HealthApplier.apply(r, false);
			context.assertTrue(r.getMaxHealth() == 30f, "on again: max health 30, was " + r.getMaxHealth());
			context.succeed();
		} finally {
			enable(context, true);
		}
	}

	@GameTest(maxTicks = 100)
	public void toggleUpdatesOnlinePlayers(GameTestHelper context) {
		try {
			enable(context, true);
			ServerPlayer p = context.makeMockServerPlayerInLevel();
			HeartsAttachment.set(p, new HeartsData(5, 0, 0, 0));
			HealthApplier.apply(p, true);
			context.assertTrue(p.getMaxHealth() == 30f, "on: max health 30, was " + p.getMaxHealth());
			enable(context, false);
			context.assertTrue(p.getMaxHealth() == 20f, "callback off: max health 20, was " + p.getMaxHealth());
			context.assertTrue(!p.getAttachedOrElse(HeartsAttachment.ACTIVE, false), "callback off: ACTIVE false");
			enable(context, true);
			context.assertTrue(p.getMaxHealth() == 30f, "callback on: max health 30, was " + p.getMaxHealth());
			context.assertTrue(p.getAttachedOrElse(HeartsAttachment.ACTIVE, false), "callback on: ACTIVE true");
			context.succeed();
		} finally {
			enable(context, true);
		}
	}

	private static void drink(ServerPlayer p, ElixirStat stat) {
		Item item = ElixirItems.get(stat);
		item.finishUsingItem(new ItemStack(item), p.level(), p);
	}

	private static void near(GameTestHelper context, double expected, double actual, String what) {
		context.assertTrue(Math.abs(expected - actual) < 1e-6, what + ": expected " + expected + ", was " + actual);
	}

	@GameTest(maxTicks = 100)
	public void elixirAddsAttackDamage(GameTestHelper context) {
		enable(context, true);
		ServerPlayer p = context.makeMockServerPlayerInLevel();
		HeartsAttachment.setCultivation(p, CultivationData.DEFAULT);
		StatApplier.apply(p);
		double base = p.getAttributeValue(Attributes.ATTACK_DAMAGE);
		drink(p, ElixirStat.STRENGTH);
		drink(p, ElixirStat.STRENGTH);
		near(context, base + 1.0, p.getAttributeValue(Attributes.ATTACK_DAMAGE), "attack damage");
		context.assertTrue(HeartsAttachment.getCultivation(p).level(ElixirStat.STRENGTH) == 2, "level should be 2");
		context.succeed();
	}

	/** Eats one item the vanilla way (Item#finishUsingItem runs the food's onConsume, where the mixin hooks in). */
	private static void eat(ServerPlayer p, Item item) {
		item.finishUsingItem(new ItemStack(item), p.level(), p);
	}

	@GameTest(maxTicks = 100)
	public void vitalityBoostsNaturalRegen(GameTestHelper context) {
		try {
			enable(context, true);
			ServerPlayer p = context.makeMockServerPlayerInLevel();
			HeartsAttachment.setCultivation(p, CultivationData.DEFAULT);
			float plain = regenOnce(p);
			near(context, 5f / 6f, plain, "no elixir: saturation 5 heals 5/6 health");
			for (int i = 0; i < 4; i++) {
				drink(p, ElixirStat.VITALITY);
			}
			near(context, 1.8f * 5f / 6f, regenOnce(p), "4 Vitality: regen from food +80% (+20% each)");
			enable(context, false);
			near(context, 5f / 6f, regenOnce(p), "switch off: vanilla regen");
			context.succeed();
		} finally {
			enable(context, true);
		}
	}

	/** Full food bar with 5 saturation at 10 health: ticks FoodData until it heals once, returns the health gained. */
	private static float regenOnce(ServerPlayer p) {
		p.setHealth(10f);
		for (int i = 0; i < 20; i++) {
			p.getFoodData().setFoodLevel(20);
			p.getFoodData().setSaturation(5f);
			p.getFoodData().tick(p);
			if (p.getHealth() > 10f) {
				return p.getHealth() - 10f;
			}
		}
		return 0f;
	}

	@GameTest(maxTicks = 100)
	public void vitalityHealsWhenEating(GameTestHelper context) {
		try {
			enable(context, true);
			ServerPlayer p = context.makeMockServerPlayerInLevel();
			HeartsAttachment.setCultivation(p, CultivationData.DEFAULT);
			p.getFoodData().setFoodLevel(0);
			p.setHealth(4f);
			eat(p, net.minecraft.world.item.Items.COOKED_BEEF);
			near(context, 4f, p.getHealth(), "no elixir: eating does not heal at once");
			drink(p, ElixirStat.VITALITY);
			drink(p, ElixirStat.VITALITY);
			p.getFoodData().setFoodLevel(0);
			eat(p, net.minecraft.world.item.Items.COOKED_BEEF); // 8 food points x 40% = 3.2 health
			near(context, 7.2f, p.getHealth(), "2 Vitality: steak heals 3.2 more");
			enable(context, false);
			p.getFoodData().setFoodLevel(0);
			eat(p, net.minecraft.world.item.Items.COOKED_BEEF);
			near(context, 7.2f, p.getHealth(), "switch off: no extra healing");
			context.succeed();
		} finally {
			enable(context, true);
		}
	}

	@GameTest(maxTicks = 100)
	public void elixirMultiplierStats(GameTestHelper context) {
		enable(context, true);
		ServerPlayer p = context.makeMockServerPlayerInLevel();
		HeartsAttachment.setCultivation(p, CultivationData.DEFAULT);
		StatApplier.apply(p);
		double speed = p.getAttributeValue(Attributes.MOVEMENT_SPEED);
		double block = p.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
		double entity = p.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
		drink(p, ElixirStat.SWIFTNESS);
		drink(p, ElixirStat.REACH);
		near(context, speed * 1.05, p.getAttributeValue(Attributes.MOVEMENT_SPEED), "movement speed");
		near(context, block + 0.5, p.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE), "block range");
		near(context, entity + 0.5, p.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE), "entity range");
		context.succeed();
	}

	@GameTest(maxTicks = 100)
	public void elixirKeptOnDeath(GameTestHelper context) {
		enable(context, true);
		ServerPlayer p = context.makeMockServerPlayerInLevel();
		HeartsAttachment.setCultivation(p, CultivationData.DEFAULT);
		StatApplier.apply(p);
		double base = p.getAttributeValue(Attributes.ARMOR);
		drink(p, ElixirStat.IRON_SKIN);
		ServerPlayer r = dieAndRespawn(context, p);
		near(context, base + 1.0, r.getAttributeValue(Attributes.ARMOR), "armor after respawn");
		context.assertTrue(HeartsAttachment.getCultivation(r).level(ElixirStat.IRON_SKIN) == 1, "level should be 1");
		context.succeed();
	}

	@GameTest(maxTicks = 100)
	public void elixirOffWhenDisabled(GameTestHelper context) {
		try {
			enable(context, true);
			ServerPlayer p = context.makeMockServerPlayerInLevel();
			HeartsAttachment.setCultivation(p, CultivationData.DEFAULT);
			StatApplier.apply(p);
			double base = p.getAttributeValue(Attributes.ATTACK_DAMAGE);
			drink(p, ElixirStat.STRENGTH);
			near(context, base + 0.5, p.getAttributeValue(Attributes.ATTACK_DAMAGE), "on");
			enable(context, false);
			near(context, base, p.getAttributeValue(Attributes.ATTACK_DAMAGE), "off");
			context.assertTrue(HeartsAttachment.getCultivation(p).level(ElixirStat.STRENGTH) == 1, "data kept while off");
			drink(p, ElixirStat.STRENGTH);
			context.assertTrue(HeartsAttachment.getCultivation(p).level(ElixirStat.STRENGTH) == 1, "drinking while off must not change level");
			enable(context, true);
			near(context, base + 0.5, p.getAttributeValue(Attributes.ATTACK_DAMAGE), "on again");
			context.succeed();
		} finally {
			enable(context, true);
		}
	}
}
