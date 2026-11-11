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

public class PermaHeartsGameTest {
	private static void kill(GameTestHelper context, ServerPlayer p, LivingEntity e) {
		ServerLevel level = context.getLevel();
		e.hurtServer(level, level.damageSources().playerAttack(p), 1000f);
	}

	@GameTest(maxTicks = 100)
	public void healthMatchesBonus(GameTestHelper context) {
		ServerPlayer p = context.makeMockServerPlayerInLevel();
		HeartsAttachment.set(p, new HeartsData(-3, 0, 0, 0));
		HealthApplier.apply(p, true);
		context.assertTrue(p.getMaxHealth() == 14f, "max health should be 14, was " + p.getMaxHealth());
		context.succeed();
	}

	@GameTest(maxTicks = 100)
	public void hostileKillCountsCowDoesNot(GameTestHelper context) {
		ServerPlayer p = context.makeMockServerPlayerInLevel();
		HeartsAttachment.set(p, HeartsData.DEFAULT);
		kill(context, p, context.spawn(EntityTypes.ZOMBIE, 1, 1, 1));
		context.assertTrue(HeartsAttachment.get(p).killProgress() == 1, "zombie kill should give progress 1, was " + HeartsAttachment.get(p).killProgress());
		kill(context, p, context.spawn(EntityTypes.COW, 2, 1, 1));
		context.assertTrue(HeartsAttachment.get(p).killProgress() == 1, "cow kill must not change progress");
		context.succeed();
	}

	@GameTest(maxTicks = 100)
	public void hundredthKillGivesHeart(GameTestHelper context) {
		ServerPlayer p = context.makeMockServerPlayerInLevel();
		HeartsAttachment.set(p, new HeartsData(0, 99, 99, 0));
		HealthApplier.apply(p, true);
		kill(context, p, context.spawn(EntityTypes.ZOMBIE, 1, 1, 1));
		context.assertTrue(HeartRules.maxHearts(HeartsAttachment.get(p)) == 11, "should have 11 hearts");
		context.assertTrue(p.getMaxHealth() == 22f, "max health should be 22, was " + p.getMaxHealth());
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
		ServerPlayer p = context.makeMockServerPlayerInLevel();
		HeartsAttachment.set(p, new HeartsData(0, 42, 542, 0));
		HealthApplier.apply(p, true);
		ServerPlayer r = dieAndRespawn(context, p);
		HeartsData d = HeartsAttachment.get(r);
		context.assertTrue(HeartRules.maxHearts(d) == 9, "should have 9 hearts after dying, had " + HeartRules.maxHearts(d));
		context.assertTrue(d.killProgress() == 42, "kill progress must survive death, was " + d.killProgress());
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
	public void killAtCapKeepsThirtyHearts(GameTestHelper context) {
		ServerPlayer p = context.makeMockServerPlayerInLevel();
		HeartsAttachment.set(p, HeartRules.withProgress(HeartRules.withHearts(HeartsData.DEFAULT, 30), 99));
		HealthApplier.apply(p, true);
		context.assertTrue(p.getMaxHealth() == 60f, "30 hearts = 60 health, was " + p.getMaxHealth());
		kill(context, p, context.spawn(EntityTypes.ZOMBIE, 1, 1, 1));
		HeartsData d = HeartsAttachment.get(p);
		context.assertTrue(HeartRules.maxHearts(d) == 30, "cap is 30 hearts");
		context.assertTrue(d.killProgress() == 0, "progress resets at cap, was " + d.killProgress());
		context.assertTrue(p.getMaxHealth() == 60f, "max health should stay 60");
		context.succeed();
	}

	@GameTest(maxTicks = 100)
	public void joinWithDefaultDataHasTenHearts(GameTestHelper context) {
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
		ServerPlayer p = context.makeMockServerPlayerInLevel();
		MinecraftServer server = context.getLevel().getServer();
		// Admin permissions, executing as this player so @s targets it (mock players all share one name, and a UUID is an entity selector).
		CommandSourceStack admin = server.createCommandSourceStack().withEntity(p);
		String name = "@s";
		run(context, admin, "hearts set " + name + " 25");
		context.assertTrue(HeartRules.maxHearts(HeartsAttachment.get(p)) == 25, "set should give 25 hearts, had " + HeartRules.maxHearts(HeartsAttachment.get(p)));
		context.assertTrue(p.getMaxHealth() == 50f, "max health should be 50, was " + p.getMaxHealth());
		run(context, admin, "hearts progress " + name + " 77");
		context.assertTrue(HeartsAttachment.get(p).killProgress() == 77, "progress should be 77");
		// Out of range is rejected, data unchanged.
		boolean rejected = false;
		try {
			server.getCommands().getDispatcher().execute("hearts set " + name + " 31", admin);
		} catch (CommandSyntaxException e) {
			rejected = true;
		}
		context.assertTrue(rejected, "hearts set 31 should be a syntax error");
		context.assertTrue(HeartRules.maxHearts(HeartsAttachment.get(p)) == 25, "31 must be rejected");
		// Plain /hearts as the player runs without changing anything.
		run(context, p.createCommandSourceStack(), "hearts");
		context.assertTrue(HeartRules.maxHearts(HeartsAttachment.get(p)) == 25, "/hearts must not change data");
		context.succeed();
	}
}
