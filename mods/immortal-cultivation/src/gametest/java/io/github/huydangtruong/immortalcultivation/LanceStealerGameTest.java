package io.github.huydangtruong.immortalcultivation;

import io.github.huydangtruong.immortalcultivation.technique.Bleeding;
import io.github.huydangtruong.immortalcultivation.technique.Lance;
import io.github.huydangtruong.immortalcultivation.technique.LanceRules;
import io.github.huydangtruong.immortalcultivation.technique.LifeStealer;
import io.github.huydangtruong.immortalcultivation.technique.StealerSlow;
import io.github.huydangtruong.immortalcultivation.technique.Technique;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueAttachment;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueData;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueRules;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.phys.AABB;
import java.util.List;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** P-061: Solar Lance (D157, D159) and Life Stealer (D158, D159) on the server. The caster looks along +Z from (5.5, 1, 0.5). */
public class LanceStealerGameTest {
	private static ServerPlayer caster(GameTestHelper c, Technique t) {
		GameTestKit.enable(c);
		GameTestKit.floor(c);
		c.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true);
		ServerPlayer p = GameTestKit.survivor(c);
		GameTestKit.place(c, p, 5.5, 1, 0.5, 0f, 0f);
		TechniqueAttachment.set(p, TechniqueData.DEFAULT.withLevel(t, 1, 10).withEmeralds(TechniqueRules.MAX_EMERALDS).withSlot(0, t));
		return p;
	}

	private static Zombie zombie(GameTestHelper c, double x, double z) {
		Zombie e = c.spawn(EntityTypes.ZOMBIE, new Vec3(x, 1, z));
		e.setPersistenceRequired();
		e.setNoAi(true);
		e.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.STONE_BUTTON)); // no sunburn
		return e;
	}

	/** A zombie that obeys gravity (a NoAI mob hovers) but cannot walk away. */
	private static Zombie faller(GameTestHelper c, double x, double z) {
		Zombie e = zombie(c, x, z);
		e.setNoAi(false);
		e.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);
		return e;
	}

	private static long now(GameTestHelper c) {
		return c.getLevel().getServer().overworld().getGameTime();
	}

	@GameTest(maxTicks = 60)
	public void theBeamHurtsEverythingOnTheLineAndNothingOffIt(GameTestHelper c) {
		ServerPlayer p = caster(c, Technique.LANCE);
		Zombie near = zombie(c, 5.5, 3.5);
		Zombie far = zombie(c, 5.5, 6.5);
		Zombie off = zombie(c, 7.0, 4.5);
		c.assertTrue(Lance.cast(p, 1), "the beam starts");
		c.runAfterDelay(25, () -> {
			c.assertTrue(near.getHealth() < near.getMaxHealth(), "the near zombie on the line is hurt");
			c.assertTrue(far.getHealth() < far.getMaxHealth(), "the far zombie on the same line is hurt too");
			c.assertTrue(off.getHealth() == off.getMaxHealth(), "the zombie off the line is not hurt");
			Lance.release(p);
			GameTestKit.drop(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 60)
	public void theBeamNeverHoldsOffOtherDamage(GameTestHelper c) { // D183: the vanilla damage cooldown stays 0
		ServerPlayer p = caster(c, Technique.LANCE);
		Zombie z = zombie(c, 5.5, 3.5);
		java.util.concurrent.atomic.AtomicInteger most = new java.util.concurrent.atomic.AtomicInteger();
		c.assertTrue(Lance.cast(p, 1), "the beam starts");
		c.onEachTick(() -> most.accumulateAndGet(z.damageCooldownTime, Math::max));
		c.runAfterDelay(25, () -> {
			c.assertTrue(z.getHealth() < z.getMaxHealth(), "the beam hurt the zombie");
			c.assertTrue(most.get() == 0, "no damage cooldown left by the beam, saw " + most.get());
			Lance.release(p);
			GameTestKit.drop(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 80)
	public void theBeamSlowsWhatItCrossesAndPushesItAwayFromTheCaster(GameTestHelper c) { // D201
		ServerPlayer p = caster(c, Technique.LANCE);
		Zombie z = faller(c, 5.5, 3.5);
		Zombie off = faller(c, 7.5, 3.5);
		double z0 = z.getZ();
		c.assertTrue(Lance.cast(p, 1), "the beam starts");
		c.runAfterDelay(25, () -> {
			c.assertTrue(z.hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS), "the zombie on the line is slowed");
			c.assertTrue(z.getEffect(net.minecraft.world.effect.MobEffects.SLOWNESS).getAmplifier() == LanceRules.SLOW_AMPLIFIER, "Slowness II");
			c.assertTrue(z.getZ() - z0 > 0.5, "the zombie was pushed along the beam, moved " + (z.getZ() - z0));
			c.assertTrue(!off.hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS), "the zombie off the line is not slowed");
			Lance.release(p);
			GameTestKit.drop(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 160)
	public void theDamageDoublesAfterThreeSeconds(GameTestHelper c) {
		ServerPlayer p = caster(c, Technique.LANCE);
		Zombie z = zombie(c, 5.5, 6.5);
		float full = z.getMaxHealth();
		Lance.cast(p, 1);
		c.runAfterDelay(55, () -> {
			float lost = full - z.getHealth();
			c.assertTrue(Math.abs(lost - 3f) < 0.6f, "the first 3 s cost about 3 health, lost " + lost);
		});
		c.runAfterDelay(115, () -> {
			float lost = full - z.getHealth();
			c.assertTrue(Math.abs(lost - 9f) < 1.1f, "the next 3 s cost about 6 more, lost " + lost);
			Lance.release(p);
			GameTestKit.drop(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 300)
	public void theBeamStopsAfterTwelveSecondsAndTheCooldownStarts(GameTestHelper c) {
		ServerPlayer p = caster(c, Technique.LANCE);
		Lance.cast(p, 1);
		c.runAfterDelay(100, () -> c.assertTrue(Lance.active(p), "still on after 5 s"));
		c.runAfterDelay(LanceRules.DURATION_TICKS + 15, () -> {
			c.assertTrue(!Lance.active(p), "off after 12 s");
			long left = TechniqueAttachment.get(p).cooldownEnd(Technique.LANCE) - now(c);
			c.assertTrue(left > 620 && left <= 720, "the 36 s cooldown started when the beam ended, left " + left);
			GameTestKit.drop(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 120)
	public void releasingEarlyStopsTheDamageAndStartsTheCooldown(GameTestHelper c) { // D159
		ServerPlayer p = caster(c, Technique.LANCE);
		Zombie z = zombie(c, 5.5, 6.5);
		Lance.cast(p, 1);
		float[] atRelease = new float[1];
		c.runAfterDelay(30, () -> {
			c.assertTrue(Lance.active(p), "running before the release");
			Lance.release(p);
			atRelease[0] = z.getHealth();
			c.assertTrue(atRelease[0] < z.getMaxHealth(), "it did hurt before the release");
			c.assertTrue(!Lance.active(p), "off at once");
			long left = TechniqueAttachment.get(p).cooldownEnd(Technique.LANCE) - now(c);
			c.assertTrue(left > 670 && left <= 720, "the 36 s cooldown starts at the release, left " + left);
		});
		c.runAfterDelay(80, () -> {
			c.assertTrue(z.getHealth() == atRelease[0], "no more damage after the release, " + atRelease[0] + " -> " + z.getHealth());
			GameTestKit.drop(c, p);
			c.succeed();
		});
	}

	private static List<Bat> bats(GameTestHelper c) {
		return c.getLevel().getEntitiesOfClass(Bat.class, new AABB(c.absoluteVec(new Vec3(2.0, 0, 3.0)), c.absoluteVec(new Vec3(9.0, 6, 10.0))));
	}

	/** Height of e above the floor of the test (the zombies stand at relative y = 1). */
	private static double up(GameTestHelper c, LivingEntity e) {
		return e.getY() - c.absoluteVec(new Vec3(0, 1, 0)).y;
	}

	@GameTest(maxTicks = 120) // D158, D159, D180, D181
	public void theDrainTakesAPercentOver2sHealsTheCasterAndLeavesBleedingAndSlow(GameTestHelper c) {
		ServerPlayer p = caster(c, Technique.BLADE);
		Zombie z = zombie(c, 5.5, 6.5);
		z.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20.0); // a spawned zombie rolls a small random max-health bonus (20.9)
		z.setHealth(20f);
		p.setHealth(10f);
		double speed = z.getAttributeValue(Attributes.MOVEMENT_SPEED);
		c.assertTrue(LifeStealer.cast(p, 1), "the cast finds the zombie under the crosshair");
		c.assertTrue(z.getHealth() == 20f, "nothing is drained at the cast: the bats are still flying in");
		c.runAfterDelay(28, () -> { // mid drain: pulses at ticks 10, 14, ... 46 of 0.7 each
			c.assertTrue(z.getHealth() < 20f && z.getHealth() > 13.5f, "half drained, health " + z.getHealth());
			c.assertTrue(Math.abs((20f - z.getHealth()) - (p.getHealth() - 10f)) < 0.01f, "the caster is healed what the pulses took");
			c.assertTrue(!z.hasEffect(StealerSlow.EFFECT), "no slow before the end");
		});
		c.runAfterDelay(49, () -> { // the last pulse is at tick 46; the bleeding starts at the end (tick 50)
			c.assertTrue(Math.abs(z.getHealth() - 13f) < 0.01f, "36% of 20 = 7.2, capped at 7 (D181), health " + z.getHealth());
			c.assertTrue(Math.abs(p.getHealth() - 17f) < 0.01f, "the caster gets the 7 back, health " + p.getHealth());
		});
		c.runAfterDelay(54, () -> {
			c.assertTrue(z.hasEffect(Bleeding.EFFECT), "the target is bleeding");
			c.assertTrue(z.hasEffect(StealerSlow.EFFECT), "the target is slowed");
			double slowed = z.getAttributeValue(Attributes.MOVEMENT_SPEED);
			c.assertTrue(Math.abs(slowed - speed * 0.2) < 1e-6, "exactly -80% speed: " + speed + " -> " + slowed);
			int left = z.getEffect(StealerSlow.EFFECT).getDuration();
			c.assertTrue(left > 30 && left <= 40, "about 2 s of slow, ticks " + left);
			GameTestKit.drop(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 160)
	public void theBleedingAndSlowWearOffAfterTwoSeconds(GameTestHelper c) { // D159: counted from the end of the drain
		ServerPlayer p = caster(c, Technique.BLADE);
		Zombie z = zombie(c, 5.5, 6.5);
		double speed = z.getAttributeValue(Attributes.MOVEMENT_SPEED);
		LifeStealer.cast(p, 1);
		c.runAfterDelay(40, () -> c.assertTrue(!z.hasEffect(Bleeding.EFFECT), "not yet bleeding during the drain"));
		c.runAfterDelay(70, () -> c.assertTrue(z.hasEffect(Bleeding.EFFECT) && z.hasEffect(StealerSlow.EFFECT), "both still on 1 s after the drain"));
		c.runAfterDelay(105, () -> {
			c.assertTrue(!z.hasEffect(Bleeding.EFFECT) && !z.hasEffect(StealerSlow.EFFECT), "both gone 2 s after the drain");
			c.assertTrue(Math.abs(z.getAttributeValue(Attributes.MOVEMENT_SPEED) - speed) < 1e-6, "the speed is back");
			GameTestKit.drop(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 80)
	public void theDrainIsCappedOnBigTargets(GameTestHelper c) {
		ServerPlayer p = caster(c, Technique.BLADE);
		Zombie z = zombie(c, 5.5, 6.5);
		z.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
		z.setHealth(200f);
		LifeStealer.cast(p, 1);
		c.runAfterDelay(49, () -> {
			c.assertTrue(Math.abs(z.getHealth() - 193f) < 0.01f, "36% of 200 is capped at 7, health " + z.getHealth());
			GameTestKit.drop(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 100) // D180
	public void fourBatsLiftTheTargetTwoBlocksThenItFallsAndTheBatsVanish(GameTestHelper c) {
		ServerPlayer p = caster(c, Technique.BLADE);
		Zombie z = faller(c, 5.5, 6.5);
		LifeStealer.cast(p, 1);
		c.assertTrue(bats(c).size() == 4, "four bats appear at the cast, " + bats(c).size());
		c.runAfterDelay(35, () -> {
			c.assertTrue(Math.abs(up(c, z) - 2.0) < 0.05, "held 2 blocks up, height " + up(c, z));
			c.assertTrue(bats(c).size() == 4, "the bats are still around it");
			c.assertTrue(z.isNoGravity(), "no gravity while held");
			c.assertTrue(z.entityTags().contains("permahearts_stealer_lifted"), "a lifted mob is tagged (B13)");
		});
		c.runAfterDelay(48, () -> c.assertTrue(Math.abs(up(c, z) - 2.0) < 0.05, "still held just before the end, height " + up(c, z)));
		c.runAfterDelay(85, () -> {
			c.assertTrue(up(c, z) < 0.05, "back on the ground, height " + up(c, z));
			c.assertTrue(!z.isNoGravity(), "gravity is back");
			c.assertTrue(!z.entityTags().contains("permahearts_stealer_lifted"), "the tag is gone with the lift");
			c.assertTrue(z.getHealth() > 11.5f, "no fall damage (only a little bleeding): health " + z.getHealth());
			c.assertTrue(bats(c).isEmpty(), "no bat left: " + bats(c).size());
			GameTestKit.drop(c, p);
			c.succeed();
		});
	}

	/** P-077 B13: a mob that loads with the lift tag (a crash mid-drain) comes down at once. */
	@GameTest(maxTicks = 20)
	public void aTaggedFloatingMobComesDownWhenItLoads(GameTestHelper c) {
		Zombie z = EntityTypes.ZOMBIE.create(c.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
		Vec3 v = c.absoluteVec(new Vec3(5.5, 3, 5.5));
		z.snapTo(v.x, v.y, v.z, 0f, 0f);
		z.setNoGravity(true);
		z.addTag("permahearts_stealer_lifted");
		c.getLevel().addFreshEntity(z); // ENTITY_LOAD fires here
		c.assertTrue(!z.isNoGravity(), "gravity is back after loading");
		c.assertTrue(!z.entityTags().contains("permahearts_stealer_lifted"), "the tag is cleared");
		z.discard();
		c.succeed();
	}

	@GameTest(maxTicks = 80) // D180: ceiling
	public void underALowCeilingTheTargetIsLiftedLessAndNeverIntoABlock(GameTestHelper c) {
		ServerPlayer p = caster(c, Technique.BLADE);
		Zombie z = faller(c, 5.5, 6.5);
		c.setBlock(new BlockPos(5, 4, 6), Blocks.STONE); // the head (1.95 tall, from y = 1) can rise by 1.05 at most
		LifeStealer.cast(p, 1);
		c.runAfterDelay(35, () -> {
			double h = up(c, z);
			c.assertTrue(h > 0.9 && h <= 1.06, "lifted about 1 block, height " + h);
			c.assertTrue(c.getLevel().noCollision(z), "never inside a block");
		});
		c.runAfterDelay(70, () -> {
			c.assertTrue(up(c, z) < 0.05, "down again, height " + up(c, z));
			GameTestKit.drop(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 80) // D180
	public void aTargetKilledMidDrainEndsItAtOnce(GameTestHelper c) {
		ServerPlayer p = caster(c, Technique.BLADE);
		Zombie z = zombie(c, 5.5, 6.5);
		LifeStealer.cast(p, 1);
		c.runAfterDelay(25, () -> z.kill(c.getLevel()));
		c.runAfterDelay(30, () -> c.assertTrue(!LifeStealer.draining(z) || bats(c).size() <= 4, "the bats are leaving"));
		c.runAfterDelay(45, () -> {
			c.assertTrue(!LifeStealer.draining(z), "the drain is over");
			c.assertTrue(bats(c).isEmpty(), "no bat left: " + bats(c).size());
			GameTestKit.drop(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 80) // D180
	public void ifTheCasterDiesTheDrainStopsAndTheTargetFalls(GameTestHelper c) {
		ServerPlayer p = caster(c, Technique.BLADE);
		Zombie z = faller(c, 5.5, 6.5);
		LifeStealer.cast(p, 1);
		c.runAfterDelay(30, () -> {
			c.assertTrue(up(c, z) > 1.5, "lifted before the caster dies");
			p.kill(c.getLevel());
		});
		c.runAfterDelay(34, () -> {
			c.assertTrue(!LifeStealer.draining(z), "the drain stopped");
			c.assertTrue(!z.isNoGravity(), "gravity is back");
			c.assertTrue(bats(c).isEmpty(), "the bats are gone: " + bats(c).size());
		});
		c.runAfterDelay(60, () -> {
			c.assertTrue(up(c, z) < 0.05, "the target is back on the ground, height " + up(c, z));
			c.assertTrue(!z.hasEffect(StealerSlow.EFFECT), "no slow when the drain was cut short");
			c.succeed();
		});
	}

	@GameTest(maxTicks = 80) // D180: bosses are bitten but never lifted
	public void aBossIsDrainedButNotLifted(GameTestHelper c) {
		ServerPlayer p = caster(c, Technique.BLADE);
		Zombie z = zombie(c, 5.5, 6.5);
		z.addTag(io.github.huydangtruong.immortalcultivation.wanderer.QuestBoss.TAG);
		LifeStealer.cast(p, 1);
		c.runAfterDelay(35, () -> {
			c.assertTrue(up(c, z) < 0.05, "a boss stays on the ground, height " + up(c, z));
			c.assertTrue(z.getHealth() < 20f, "but it is drained, health " + z.getHealth());
			c.assertTrue(bats(c).size() == 4, "and the bats are on it");
		});
		c.runAfterDelay(70, () -> {
			c.assertTrue(bats(c).isEmpty(), "bats gone");
			GameTestKit.drop(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 20)
	public void withNoTargetTheCastFailsAndTheLanceNeedsNoTarget(GameTestHelper c) {
		ServerPlayer p = caster(c, Technique.BLADE);
		c.assertTrue(!LifeStealer.cast(p, 1), "nothing under the crosshair: the cast fails");
		GameTestKit.drop(c, p);
		c.succeed();
	}

	@GameTest(maxTicks = 40) // D161
	public void theBeamBreaksAStoneBlockAfterAboutSixTicks(GameTestHelper c) {
		ServerPlayer p = caster(c, Technique.LANCE);
		BlockPos stone = new BlockPos(5, 2, 3); // on the eye-height line of the caster
		c.setBlock(stone, Blocks.STONE);
		Lance.cast(p, 1);
		c.runAfterDelay(3, () -> c.assertTrue(c.getBlockState(stone).is(Blocks.STONE), "still standing after 3 ticks"));
		c.runAfterDelay(14, () -> {
			c.assertTrue(c.getBlockState(stone).isAir(), "the stone is broken after about 6 ticks");
			Lance.release(p);
			GameTestKit.drop(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 40) // D161
	public void theBeamNeverBreaksBedrock(GameTestHelper c) {
		ServerPlayer p = caster(c, Technique.LANCE);
		BlockPos rock = new BlockPos(5, 2, 3);
		c.setBlock(rock, Blocks.BEDROCK);
		Lance.cast(p, 1);
		c.runAfterDelay(25, () -> {
			c.assertTrue(c.getBlockState(rock).is(Blocks.BEDROCK), "bedrock stays");
			Lance.release(p);
			GameTestKit.drop(c, p);
			c.succeed();
		});
	}
}
