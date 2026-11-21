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
		FlightBladeGameTest.enable(c);
		FlightBladeGameTest.floor(c);
		c.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true);
		ServerPlayer p = FlightBladeGameTest.survivor(c);
		FlightBladeGameTest.place(c, p, 5.5, 1, 0.5, 0f, 0f);
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
			FlightBladeGameTest.drop(c, p);
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
			FlightBladeGameTest.drop(c, p);
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
			FlightBladeGameTest.drop(c, p);
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
			FlightBladeGameTest.drop(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 30)
	public void theDrainTakesAPercentHealsTheCasterAndLeavesBleedingAndSlow(GameTestHelper c) { // D158, D159
		ServerPlayer p = caster(c, Technique.BLADE);
		Zombie z = zombie(c, 5.5, 6.5);
		p.setHealth(10f);
		double speed = z.getAttributeValue(Attributes.MOVEMENT_SPEED);
		c.assertTrue(LifeStealer.cast(p, 1), "the cast finds the zombie under the crosshair");
		c.assertTrue(Math.abs(z.getHealth() - 13f) < 0.01f, "36% of 20 = 7.2, capped at 7 (D181), health " + z.getHealth());
		c.assertTrue(Math.abs(p.getHealth() - 17f) < 0.01f, "the caster gets the 7 back, health " + p.getHealth());
		c.assertTrue(z.hasEffect(Bleeding.EFFECT), "the target is bleeding");
		c.assertTrue(z.hasEffect(StealerSlow.EFFECT), "the target is slowed");
		double slowed = z.getAttributeValue(Attributes.MOVEMENT_SPEED);
		c.assertTrue(Math.abs(slowed - speed * 0.2) < 1e-6, "exactly -80% speed: " + speed + " -> " + slowed);
		int left = z.getEffect(StealerSlow.EFFECT).getDuration();
		c.assertTrue(left > 30 && left <= 40, "about 2 s of slow, ticks " + left);
		FlightBladeGameTest.drop(c, p);
		c.succeed();
	}

	@GameTest(maxTicks = 80)
	public void theBleedingAndSlowWearOffAfterTwoSeconds(GameTestHelper c) { // D159
		ServerPlayer p = caster(c, Technique.BLADE);
		Zombie z = zombie(c, 5.5, 6.5);
		double speed = z.getAttributeValue(Attributes.MOVEMENT_SPEED);
		LifeStealer.cast(p, 1);
		c.runAfterDelay(20, () -> c.assertTrue(z.hasEffect(Bleeding.EFFECT) && z.hasEffect(StealerSlow.EFFECT), "both still on after 1 s"));
		c.runAfterDelay(55, () -> {
			c.assertTrue(!z.hasEffect(Bleeding.EFFECT) && !z.hasEffect(StealerSlow.EFFECT), "both gone after 2 s");
			c.assertTrue(Math.abs(z.getAttributeValue(Attributes.MOVEMENT_SPEED) - speed) < 1e-6, "the speed is back");
			FlightBladeGameTest.drop(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 30)
	public void theDrainIsCappedOnBigTargets(GameTestHelper c) {
		ServerPlayer p = caster(c, Technique.BLADE);
		Zombie z = zombie(c, 5.5, 6.5);
		z.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
		z.setHealth(200f);
		LifeStealer.cast(p, 1);
		c.assertTrue(Math.abs(z.getHealth() - 193f) < 0.01f, "36% of 200 is capped at 7, health " + z.getHealth());
		FlightBladeGameTest.drop(c, p);
		c.succeed();
	}

	@GameTest(maxTicks = 20)
	public void withNoTargetTheCastFailsAndTheLanceNeedsNoTarget(GameTestHelper c) {
		ServerPlayer p = caster(c, Technique.BLADE);
		c.assertTrue(!LifeStealer.cast(p, 1), "nothing under the crosshair: the cast fails");
		FlightBladeGameTest.drop(c, p);
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
			FlightBladeGameTest.drop(c, p);
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
			FlightBladeGameTest.drop(c, p);
			c.succeed();
		});
	}
}
