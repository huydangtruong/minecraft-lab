package io.github.huydangtruong.permahearts;

import io.github.huydangtruong.permahearts.technique.Technique;
import io.github.huydangtruong.permahearts.technique.TechniqueAttachment;
import io.github.huydangtruong.permahearts.technique.TechniqueCasts;
import io.github.huydangtruong.permahearts.technique.TechniqueRules;
import io.github.huydangtruong.permahearts.technique.VioletGuardian;
import io.github.huydangtruong.permahearts.technique.WarriorAngel;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.phys.Vec3;

/** P-021 (D62, D63): Warrior Angel (count, power, owner safety, fighting, lifetime) and kill credit for summons. */
public class AngelGameTest {
	private static void arm(ServerPlayer p, Technique t, int level) {
		TechniqueAttachment.set(p, TechniqueAttachment.get(p).withLevel(t, level, 10).withEmeralds(TechniqueRules.MAX_EMERALDS).withSlot(0, t));
	}

	private static ServerPlayer owner(GameTestHelper c) {
		FlightBladeGameTest.enable(c);
		FlightBladeGameTest.floor(c);
		ServerPlayer p = FlightBladeGameTest.survivor(c);
		FlightBladeGameTest.place(c, p, 5.5, 1, 5.5, 0f, 0f);
		return p;
	}

	@GameTest(maxTicks = 40)
	public void angelCountAndPowerFollowTheLevel(GameTestHelper c) {
		ServerPlayer p = owner(c);
		arm(p, Technique.ANGEL, 1);
		double vexHealth = Vex.createAttributes().build().getValue(Attributes.MAX_HEALTH);
		double vexDamage = Vex.createAttributes().build().getValue(Attributes.ATTACK_DAMAGE);
		int[][] cases = {{1, 2}, {5, 3}, {10, 4}, {15, 5}, {20, 6}};
		for (int[] k : cases) {
			c.assertTrue(TechniqueCasts.cast(p, Technique.ANGEL, k[0], 1f), "cast works at level " + k[0]);
			var mine = WarriorAngel.angelsOf(c.getLevel(), p.getUUID());
			c.assertTrue(mine.size() == k[1], "level " + k[0] + " summons " + k[1] + ", was " + mine.size() + " (recasting removes the old ones)");
			double hp = vexHealth * TechniqueRules.angelHealthFactor(k[0]);
			double dmg = vexDamage * TechniqueRules.angelDamageFactor(k[0]);
			for (WarriorAngel a : mine) {
				c.assertTrue(Math.abs(a.getMaxHealth() - hp) < 1e-3 && Math.abs(a.getHealth() - hp) < 1e-3, "health " + a.getHealth() + " at level " + k[0]);
				c.assertTrue(Math.abs(a.getAttributeValue(Attributes.ATTACK_DAMAGE) - dmg) < 1e-3, "damage " + a.getAttributeValue(Attributes.ATTACK_DAMAGE));
				c.assertTrue(a.endTick() - c.getLevel().getGameTime() == 1200, "lasts 60 s");
			}
		}
		var lv1 = TechniqueCasts.cast(p, Technique.ANGEL, 1, 1f);
		var a = WarriorAngel.angelsOf(c.getLevel(), p.getUUID()).get(0);
		c.assertTrue(lv1 && a.getMaxHealth() == vexHealth && a.getAttributeValue(Attributes.ATTACK_DAMAGE) == vexDamage / 2, "level 1: vex health, half vex damage");
		FlightBladeGameTest.drop(c, p);
		c.succeed();
	}

	@GameTest(maxTicks = 100)
	public void angelIgnoresItsOwnerAndPlayers(GameTestHelper c) {
		ServerPlayer p = owner(c);
		arm(p, Technique.ANGEL, 1);
		TechniqueCasts.cast(p, Technique.ANGEL, 1, 1f);
		WarriorAngel a = WarriorAngel.angelsOf(c.getLevel(), p.getUUID()).get(0);
		float hp = a.getHealth();
		boolean hurt = a.hurtServer(c.getLevel(), c.getLevel().damageSources().playerAttack(p), 10f);
		c.assertTrue(!hurt && a.getHealth() == hp, "the owner's hit does nothing");
		c.assertTrue(!a.canAttack(p), "never attacks a player");
		float php = p.getHealth();
		c.runAfterDelay(60, () -> {
			c.assertTrue(a.getTarget() == null, "no target, was " + a.getTarget());
			c.assertTrue(p.getHealth() == php, "the owner is not hurt");
			c.assertTrue(a.getHealth() == hp, "not retaliating, health " + a.getHealth());
			FlightBladeGameTest.drop(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 220)
	public void angelsFlyToAMonsterAndHurtIt(GameTestHelper c) {
		ServerPlayer p = owner(c);
		FlightBladeGameTest.place(c, p, 5.5, 1, 2.5, 0f, 0f);
		arm(p, Technique.ANGEL, 20);
		Husk h = c.spawn(EntityTypes.HUSK, new Vec3(5.5, 1, 9.5));
		h.setNoAi(true);
		TechniqueCasts.cast(p, Technique.ANGEL, 20, 1f);
		c.runAfterDelay(180, () -> {
			c.assertTrue(!h.isAlive() || h.getHealth() < h.getMaxHealth(), "an angel hurt the husk, health " + h.getHealth());
			FlightBladeGameTest.drop(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 60)
	public void anAngelKillGivesTheOwnerCredit(GameTestHelper c) {
		ServerPlayer p = owner(c);
		arm(p, Technique.ANGEL, 1);
		TechniqueCasts.cast(p, Technique.ANGEL, 1, 1f);
		WarriorAngel a = WarriorAngel.angelsOf(c.getLevel(), p.getUUID()).get(0);
		Zombie z = c.spawn(EntityTypes.ZOMBIE, new Vec3(6.5, 1, 6.5));
		z.setNoAi(true);
		z.setHealth(1f);
		int kills = HeartsAttachment.get(p).totalHostileKills();
		a.doHurtTarget(c.getLevel(), z);
		c.assertTrue(!z.isAlive(), "the angel killed the zombie");
		int now = HeartsAttachment.get(p).totalHostileKills();
		c.assertTrue(now == kills + 1, "the owner's kill count went " + kills + " -> " + now);
		FlightBladeGameTest.drop(c, p);
		c.succeed();
	}

	@GameTest(maxTicks = 60)
	public void aGuardianKillGivesTheOwnerCredit(GameTestHelper c) {
		ServerPlayer p = owner(c);
		arm(p, Technique.GOLEM, 1);
		TechniqueCasts.cast(p, Technique.GOLEM, 1, 1f);
		VioletGuardian g = VioletGuardian.guardiansOf(c.getLevel(), p.getUUID()).get(0);
		Zombie z = c.spawn(EntityTypes.ZOMBIE, new Vec3(6.5, 1, 6.5));
		z.setNoAi(true);
		z.setHealth(1f);
		int kills = HeartsAttachment.get(p).totalHostileKills();
		g.doHurtTarget(c.getLevel(), z);
		c.assertTrue(!z.isAlive(), "the guardian killed the zombie");
		int now = HeartsAttachment.get(p).totalHostileKills();
		c.assertTrue(now == kills + 1, "the owner's kill count went " + kills + " -> " + now);
		FlightBladeGameTest.drop(c, p);
		c.succeed();
	}

	@GameTest(maxTicks = 80)
	public void angelsVanishAfterTheirTimeAndWhenTheOwnerLeaves(GameTestHelper c) {
		ServerPlayer p = owner(c);
		arm(p, Technique.ANGEL, 10);
		TechniqueCasts.cast(p, Technique.ANGEL, 10, 1f);
		var mine = WarriorAngel.angelsOf(c.getLevel(), p.getUUID());
		c.assertTrue(mine.size() == 4, "four angels");
		WarriorAngel a = mine.get(0);
		WarriorAngel b = mine.get(1);
		WarriorAngel d = mine.get(2);
		a.setEndTick(c.getLevel().getGameTime() + 2); // fast-forward the 60 s
		c.runAfterDelay(8, () -> {
			c.assertTrue(!a.isAlive(), "the first angel vanished after its time");
			c.assertTrue(b.isAlive() && d.isAlive(), "the others stay");
			FlightBladeGameTest.drop(c, p); // the owner leaves
			c.runAfterDelay(8, () -> {
				c.assertTrue(!b.isAlive() && !d.isAlive(), "the angels vanish with their owner");
				c.succeed();
			});
		});
	}
}
