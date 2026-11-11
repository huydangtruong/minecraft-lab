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
import net.minecraft.world.entity.monster.zombie.Zombie;

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
	public void killAtCapKeepsFiftyHearts(GameTestHelper context) {
		enable(context, true);
		ServerPlayer p = context.makeMockServerPlayerInLevel();
		HeartsAttachment.set(p, HeartRules.withHearts(HeartsData.DEFAULT, 50));
		HealthApplier.apply(p, true);
		context.assertTrue(p.getMaxHealth() == 100f, "50 hearts = 100 health, was " + p.getMaxHealth());
		kill(context, p, context.spawn(EntityTypes.ZOMBIE, 1, 1, 1));
		HeartsData d = HeartsAttachment.get(p);
		context.assertTrue(HeartRules.maxHearts(d) == 50, "cap is 50 hearts");
		context.assertTrue(d.heartXp() == 0, "XP stays 0 at cap, was " + d.heartXp());
		context.assertTrue(p.getMaxHealth() == 100f, "max health should stay 100");
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
			server.getCommands().getDispatcher().execute("hearts set " + name + " 51", admin);
		} catch (CommandSyntaxException e) {
			rejected = true;
		}
		context.assertTrue(rejected, "hearts set 51 should be a syntax error");
		context.assertTrue(HeartRules.maxHearts(HeartsAttachment.get(p)) == 45, "51 must be rejected");
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
}
