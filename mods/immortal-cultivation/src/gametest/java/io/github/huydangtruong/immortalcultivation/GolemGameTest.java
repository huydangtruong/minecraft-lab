package io.github.huydangtruong.immortalcultivation;

import io.github.huydangtruong.immortalcultivation.technique.Bleeding;
import io.github.huydangtruong.immortalcultivation.technique.Technique;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueAttachment;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueCasts;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueRules;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueSelection;
import io.github.huydangtruong.immortalcultivation.technique.VioletGuardian;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.phys.Vec3;

/** P-020 (D60): Life Steal Blade bleeding and knockback, and the Violet Guardian golems, on a real survival player. */
public class GolemGameTest {
	private static Husk husk(GameTestHelper c, double z) {
		Husk h = c.spawn(EntityTypes.HUSK, new Vec3(5.5, 1, z));
		h.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0); // NoAI mobs do not slide; a speed-0 mob only slides when thrown
		h.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
		h.setHealth(200f);
		return h;
	}

	@GameTest(maxTicks = 40)
	public void bleedingIsHarmfulAndHurtsOverTime(GameTestHelper c) {
		FlightBladeGameTest.enable(c);
		FlightBladeGameTest.floor(c);
		Husk h = husk(c, 5.5);
		h.addEffect(new MobEffectInstance(Bleeding.EFFECT, 100, 0));
		c.runAfterDelay(30, () -> {
			float lost = 200f - h.getHealth();
			c.assertTrue(lost >= 1f && lost <= 3f, "about 1 damage per second, lost " + lost + " in 1.5 s");
			c.succeed();
		});
	}

	// ---- Violet Guardian ----

	private static void arm(ServerPlayer p, int level) {
		TechniqueAttachment.set(p, TechniqueAttachment.get(p).withLevel(Technique.GOLEM, level, 10).withEmeralds(TechniqueRules.MAX_EMERALDS).withSlot(0, Technique.GOLEM));
	}

	@GameTest(maxTicks = 40)
	public void guardianCountAndPowerFollowTheLevel(GameTestHelper c) {
		FlightBladeGameTest.enable(c);
		FlightBladeGameTest.floor(c);
		ServerPlayer p = FlightBladeGameTest.survivor(c);
		FlightBladeGameTest.place(c, p, 5.5, 1, 5.5, 0f, 0f);
		arm(p, 1);
		int[][] cases = {{1, 1}, {9, 1}, {10, 2}, {14, 2}, {15, 3}, {19, 3}, {20, 4}};
		for (int[] k : cases) {
			c.assertTrue(TechniqueCasts.cast(p, Technique.GOLEM, k[0], 1f), "cast works at level " + k[0]);
			var mine = VioletGuardian.guardiansOf(c.getLevel(), p.getUUID());
			c.assertTrue(mine.size() == k[1], "level " + k[0] + " summons " + k[1] + ", was " + mine.size() + " (recasting removes the old ones)");
			double power = TechniqueRules.golemPower(k[0]);
			for (VioletGuardian g : mine) {
				c.assertTrue(Math.abs(g.getMaxHealth() - 100 * power) < 1e-3 && Math.abs(g.getHealth() - 100 * power) < 1e-3, "health " + g.getHealth() + " at level " + k[0]);
				c.assertTrue(Math.abs(g.getAttributeValue(Attributes.ATTACK_DAMAGE) - 15 * power) < 1e-3, "damage " + g.getAttributeValue(Attributes.ATTACK_DAMAGE));
				long left = g.endTick() - c.getLevel().getGameTime();
				c.assertTrue(left == 1800, "lasts 90 s, was " + left + " ticks");
			}
		}
		c.assertTrue(TechniqueRules.golemPower(20) == 2.0, "double at level 20");
		FlightBladeGameTest.drop(c, p);
		c.succeed();
	}

	@GameTest(maxTicks = 100)
	public void guardianIgnoresItsOwnerAndPlayers(GameTestHelper c) {
		FlightBladeGameTest.enable(c);
		FlightBladeGameTest.floor(c);
		ServerPlayer p = FlightBladeGameTest.survivor(c);
		FlightBladeGameTest.place(c, p, 5.5, 1, 5.5, 0f, 0f);
		arm(p, 1);
		TechniqueCasts.cast(p, Technique.GOLEM, 1, 1f);
		VioletGuardian g = VioletGuardian.guardiansOf(c.getLevel(), p.getUUID()).get(0);
		float hp = g.getHealth();
		boolean hurt = g.hurtServer(c.getLevel(), c.getLevel().damageSources().playerAttack(p), 10f);
		c.assertTrue(!hurt && g.getHealth() == hp, "the owner's hit does nothing");
		c.assertTrue(!g.canAttack(p), "never attacks a player");
		float php = p.getHealth();
		c.runAfterDelay(60, () -> {
			c.assertTrue(g.getTarget() == null, "no target, was " + g.getTarget());
			c.assertTrue(p.getHealth() == php, "the owner is not hurt");
			c.assertTrue(g.getHealth() == hp, "not retaliating, health " + g.getHealth());
			FlightBladeGameTest.drop(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 200)
	public void guardianAttacksAMonsterNearTheOwner(GameTestHelper c) {
		FlightBladeGameTest.enable(c);
		FlightBladeGameTest.floor(c);
		ServerPlayer p = FlightBladeGameTest.survivor(c);
		FlightBladeGameTest.place(c, p, 5.5, 1, 2.5, 0f, 0f);
		arm(p, 20);
		Husk h = c.spawn(EntityTypes.HUSK, new Vec3(5.5, 1, 8.5));
		h.setNoAi(true);
		TechniqueCasts.cast(p, Technique.GOLEM, 20, 1f);
		c.runAfterDelay(150, () -> {
			c.assertTrue(!h.isAlive() || h.getHealth() < h.getMaxHealth(), "a guardian attacked the husk, health " + h.getHealth());
			FlightBladeGameTest.drop(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 80)
	public void guardiansVanishAfterTheirTimeAndWhenTheOwnerLeaves(GameTestHelper c) {
		FlightBladeGameTest.enable(c);
		FlightBladeGameTest.floor(c);
		ServerPlayer p = FlightBladeGameTest.survivor(c);
		FlightBladeGameTest.place(c, p, 5.5, 1, 5.5, 0f, 0f);
		arm(p, 15);
		TechniqueCasts.cast(p, Technique.GOLEM, 15, 1f);
		var mine = VioletGuardian.guardiansOf(c.getLevel(), p.getUUID());
		c.assertTrue(mine.size() == 3, "three guardians");
		VioletGuardian a = mine.get(0);
		VioletGuardian b = mine.get(1);
		VioletGuardian d = mine.get(2);
		a.setEndTick(c.getLevel().getGameTime() + 2); // fast-forward the 90 s
		c.runAfterDelay(8, () -> {
			c.assertTrue(!a.isAlive(), "the first guardian vanished after its time");
			c.assertTrue(b.isAlive() && d.isAlive(), "the others stay");
			FlightBladeGameTest.drop(c, p); // the owner leaves
			c.runAfterDelay(8, () -> {
				c.assertTrue(!b.isAlive() && !d.isAlive(), "the guardians vanish with their owner");
				c.succeed();
			});
		});
	}
}
