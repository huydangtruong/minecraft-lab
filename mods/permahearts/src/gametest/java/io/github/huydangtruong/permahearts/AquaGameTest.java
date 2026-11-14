package io.github.huydangtruong.permahearts;

import io.github.huydangtruong.permahearts.technique.AquaMarine;
import io.github.huydangtruong.permahearts.technique.Technique;
import io.github.huydangtruong.permahearts.technique.TechniqueCasts;
import io.github.huydangtruong.permahearts.technique.TechniqueRules;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * P-022 (D64): Aqua Marine: air, swim speed against vanilla Dolphin's Grace, underwater mining, timing.
 * The fake survival player is not ticked by the world (no physics, no air loss), so these tests call
 * baseTick() and travel() themselves, the way the real tick would.
 */
public class AquaGameTest {
	/** A pool of water (still source blocks) around the player's spot, with a stone floor. */
	private static ServerPlayer diver(GameTestHelper c) {
		FlightBladeGameTest.enable(c);
		FlightBladeGameTest.floor(c);
		for (int x = 0; x < 12; x++) {
			for (int y = 1; y <= 7; y++) {
				for (int z = 0; z < 12; z++) {
					c.setBlock(x, y, z, Blocks.WATER);
				}
			}
		}
		ServerPlayer p = FlightBladeGameTest.survivor(c);
		FlightBladeGameTest.place(c, p, 5.5, 3, 5.5, 0f, 0f);
		p.baseTick(); // notice the water (twice: the eye flag follows the water flag)
		p.baseTick();
		return p;
	}

	/**
	 * Steady horizontal swim speed in blocks per tick: runs the real LivingEntity.travel in water 200 times, always
	 * moving the player back to the start so it never leaves the pool, and returns the last tick's displacement.
	 * Sprint state is set by the caller (Aqua Marine reads it when it is cast). Open water: the player is off the ground.
	 */
	private static double swimSpeed(ServerPlayer p) {
		Vec3 start = p.position();
		p.setDeltaMovement(Vec3.ZERO);
		double last = 0;
		for (int i = 0; i < 200; i++) {
			p.travel(new Vec3(0, 0, 1));
			Vec3 now = p.position();
			last = Math.hypot(now.x - start.x, now.z - start.z);
			p.setPos(start);
		}
		return last;
	}

	@GameTest(maxTicks = 140)
	public void aquaKeepsTheAirFullThenEnds(GameTestHelper c) {
		ServerPlayer p = diver(c);
		int max = p.getMaxAirSupply();
		c.assertTrue(p.isUnderWater(), "the diver is under water");
		p.setAirSupply(max);
		c.onEachTick(p::baseTick); // the air only goes down in the player's own tick
		c.runAfterDelay(30, () -> {
			c.assertTrue(p.getAirSupply() < max - 10, "control: without Aqua Marine the air goes down, was " + p.getAirSupply());
			c.assertTrue(TechniqueCasts.cast(p, Technique.AQUA, 1, 1f), "cast works");
			c.assertTrue(p.getAirSupply() == max, "air is full on cast");
			c.assertTrue(p.getAttached(AquaMarine.END) - p.level().getGameTime() == TechniqueRules.aquaSeconds(1) * 20, "lasts 30 s at level 1");
			c.runAfterDelay(40, () -> {
				c.assertTrue(p.getAirSupply() >= max - 1, "air stays full while it lasts, was " + p.getAirSupply());
				c.assertTrue(AquaMarine.active(p), "active");
				c.assertTrue(p.hasEffect(MobEffects.DOLPHINS_GRACE), "dolphin's grace running");
				p.setAttached(AquaMarine.END, p.level().getGameTime() + 2); // fast-forward the 30 s
				c.runAfterDelay(6, () -> {
					c.assertTrue(p.getAttached(AquaMarine.END) == null, "state cleared at the end");
					c.assertTrue(!p.hasEffect(MobEffects.DOLPHINS_GRACE), "hidden effect removed at the end");
					c.assertTrue(p.getAttribute(Attributes.SUBMERGED_MINING_SPEED).getValue() == TechniqueRules.SUBMERGED_MINING_BASE, "mining back to vanilla");
					c.assertTrue(p.getAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY).getModifier(AquaMarine.SWIM_ID) == null, "swim modifier removed");
					c.runAfterDelay(30, () -> {
						c.assertTrue(p.getAirSupply() < max - 10, "after the end the air goes down again, was " + p.getAirSupply());
						FlightBladeGameTest.drop(c, p);
						c.succeed();
					});
				});
			});
		});
	}

	@GameTest(maxTicks = 40)
	public void swimSpeedMatchesDolphinsGraceAtLevel1AndDoublesTheBonusAtLevel20(GameTestHelper c) {
		ServerPlayer p = diver(c);
		c.assertTrue(p.isInWater(), "the diver is in water");
		for (boolean sprint : new boolean[] {true, false}) {
			{
				p.setSprinting(sprint);
				double plain = swimSpeed(p);
				p.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 100000, 0));
				double dolphin = swimSpeed(p);
				p.removeEffect(MobEffects.DOLPHINS_GRACE);
				TechniqueCasts.cast(p, Technique.AQUA, 1, 1f);
				double lv1 = swimSpeed(p);
				AquaMarine.end(p);
				TechniqueCasts.cast(p, Technique.AQUA, 20, 1f);
				double lv20 = swimSpeed(p);
				AquaMarine.end(p);
				double bonus = dolphin - plain;
				String label = " (sprint=" + sprint + ")";
				System.out.println("AQUA-MEASURE sprint=" + sprint + " plain=" + plain + " dolphin=" + dolphin
					+ " lv1=" + lv1 + " lv20=" + lv20 + " dolphinBonus=" + bonus + " lv20bonus/dolphinBonus=" + (lv20 - plain) / bonus);
				c.assertTrue(bonus > 0.05, "Dolphin's Grace is faster than plain swimming: " + plain + " -> " + dolphin + label);
				c.assertTrue(Math.abs(lv1 - dolphin) < 0.03 * dolphin, "level 1 = Dolphin's Grace speed: " + lv1 + " vs " + dolphin + label);
				c.assertTrue(Math.abs((lv20 - plain) / bonus - 2.0) < 0.1, "level 20 bonus is 2 x Dolphin's Grace: " + (lv20 - plain) / bonus + label);
			}
		}
		FlightBladeGameTest.drop(c, p);
		c.succeed();
	}

	@GameTest(maxTicks = 40)
	public void miningSpeedFollowsTheLevelAndGoesBack(GameTestHelper c) {
		ServerPlayer p = diver(c);
		var mining = p.getAttribute(Attributes.SUBMERGED_MINING_SPEED);
		c.assertTrue(Math.abs(mining.getValue() - 0.2) < 1e-9, "vanilla 0.2, was " + mining.getValue());
		TechniqueCasts.cast(p, Technique.AQUA, 1, 1f);
		c.assertTrue(Math.abs(mining.getValue() - 1.0) < 1e-9, "level 1 = 1.0 (aqua affinity), was " + mining.getValue());
		TechniqueCasts.cast(p, Technique.AQUA, 20, 1f);
		c.assertTrue(Math.abs(mining.getValue() - 2.0) < 1e-9, "level 20 = 2.0, was " + mining.getValue());
		c.runAfterDelay(5, () -> {
			c.assertTrue(Math.abs(mining.getValue() - 2.0) < 1e-9, "still 2.0 while it lasts, was " + mining.getValue());
			AquaMarine.end(p);
			c.assertTrue(Math.abs(mining.getValue() - 0.2) < 1e-9, "back to 0.2, was " + mining.getValue());
			FlightBladeGameTest.drop(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 40)
	public void aquaEndsWhenThePlayerDies(GameTestHelper c) {
		ServerPlayer p = diver(c);
		TechniqueCasts.cast(p, Technique.AQUA, 5, 1f);
		p.kill(c.getLevel());
		c.runAfterDelay(3, () -> {
			c.assertTrue(p.getAttached(AquaMarine.END) == null, "state cleared on death");
			FlightBladeGameTest.drop(c, p);
			c.succeed();
		});
	}
}
