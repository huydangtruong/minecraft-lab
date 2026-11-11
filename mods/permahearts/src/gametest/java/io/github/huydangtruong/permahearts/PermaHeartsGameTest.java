package io.github.huydangtruong.permahearts;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
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
}
