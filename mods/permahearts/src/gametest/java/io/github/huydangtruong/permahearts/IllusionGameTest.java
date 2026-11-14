package io.github.huydangtruong.permahearts;

import io.github.huydangtruong.permahearts.technique.Illusion;
import io.github.huydangtruong.permahearts.technique.IllusionTargets;
import io.github.huydangtruong.permahearts.technique.Technique;
import io.github.huydangtruong.permahearts.technique.TechniqueAttachment;
import io.github.huydangtruong.permahearts.technique.TechniqueCasts;
import io.github.huydangtruong.permahearts.technique.TechniqueRules;
import io.github.huydangtruong.permahearts.technique.VioletGuardian;
import java.util.List;
import java.util.Set;
import net.minecraft.world.level.block.Blocks;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;

/** P-024 (D71): Illusion clones: count and stats, gear for looks, targets with PvP off, kills and lifetime. */
public class IllusionGameTest {
	private static void arm(ServerPlayer p, int level) {
		TechniqueAttachment.set(p, TechniqueAttachment.get(p).withLevel(Technique.ILLUSION, level, 10).withEmeralds(TechniqueRules.MAX_EMERALDS).withSlot(0, Technique.ILLUSION));
	}

	private static ServerPlayer owner(GameTestHelper c) {
		FlightBladeGameTest.enable(c);
		FlightBladeGameTest.floor(c);
		ServerPlayer p = FlightBladeGameTest.survivor(c);
		FlightBladeGameTest.place(c, p, 5.5, 1, 5.5, 0f, 0f);
		return p;
	}

	private static List<Illusion> clones(GameTestHelper c, ServerPlayer p) {
		return Illusion.clonesOf(c.getLevel(), p.getUUID());
	}

	@GameTest(maxTicks = 60)
	public void cloneCountAndStatsFollowTheLevel(GameTestHelper c) {
		ServerPlayer p = owner(c);
		p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
		p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
		p.doTick(); // applies the gear attributes (a mock player is not ticked by the server)
		c.runAfterDelay(3, () -> {
			double dmg = p.getAttributeValue(Attributes.ATTACK_DAMAGE);
			double armor = p.getAttributeValue(Attributes.ARMOR);
			c.assertTrue(Math.abs(dmg - 7) < 1e-6 && Math.abs(armor - 6) < 1e-6, "the owner has sword damage 7 and armour 6, was " + dmg + " / " + armor);
			arm(p, 1);
			int[][] cases = {{1, 1}, {10, 2}, {20, 3}};
			for (int[] k : cases) {
				c.assertTrue(TechniqueCasts.cast(p, Technique.ILLUSION, k[0], 1f), "cast works at level " + k[0]);
				List<Illusion> mine = clones(c, p);
				c.assertTrue(mine.size() == k[1], "level " + k[0] + " makes " + k[1] + " clones (recasting replaces), was " + mine.size());
				double f = TechniqueRules.illusionFactor(k[0]);
				for (Illusion a : mine) {
					c.assertTrue(Math.abs(a.getMaxHealth() - f * p.getMaxHealth()) < 1e-3 && a.getHealth() == a.getMaxHealth(), "health " + a.getHealth() + " at level " + k[0]);
					c.assertTrue(Math.abs(a.getAttributeValue(Attributes.ARMOR) - f * 6) < 1e-3, "armour is the share of 6, was " + a.getAttributeValue(Attributes.ARMOR));
					c.assertTrue(a.getMainHandItem().is(Items.DIAMOND_SWORD) && a.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE), "the clone wears the owner's gear");
					c.assertTrue(Math.abs(a.getAttributeValue(Attributes.ATTACK_DAMAGE) - 1.0) < 1e-6, "the copied sword adds no damage on its own, was " + a.getAttributeValue(Attributes.ATTACK_DAMAGE));
					c.assertTrue(a.getDropChances().byEquipment(EquipmentSlot.MAINHAND) == 0f && a.getDropChances().byEquipment(EquipmentSlot.CHEST) == 0f, "the copies never drop");
					c.assertTrue(a.getCustomName() != null && a.getCustomName().getString().equals(p.getGameProfile().name()) && a.isCustomNameVisible(), "the owner's name is shown");
					c.assertTrue(a.endTick() - c.getLevel().getGameTime() == Math.round(TechniqueRules.illusionSeconds(k[0]) * 20), "lasts " + TechniqueRules.illusionSeconds(k[0]) + " s");
				}
			}
			FlightBladeGameTest.drop(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 60)
	public void aCloneHitsWithAShareOfTheOwnersAttackAndTheOwnerCannotHurtIt(GameTestHelper c) {
		ServerPlayer p = owner(c);
		p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
		p.doTick();
		c.runAfterDelay(3, () -> {
			arm(p, 20);
			TechniqueCasts.cast(p, Technique.ILLUSION, 20, 1f);
			Illusion a = clones(c, p).get(0);
			Zombie z = c.spawn(EntityTypes.ZOMBIE, new Vec3(8.5, 1, 8.5));
			z.setNoAi(true);
			z.getAttribute(Attributes.ARMOR).setBaseValue(0); // a zombie has 2 armour of its own
			float before = z.getHealth();
			a.doHurtTarget(c.getLevel(), z);
			double taken = before - z.getHealth();
			c.assertTrue(Math.abs(taken - 0.4 * 7) < 0.05, "a Lv 20 clone hits for 40% of 7 = 2.8, did " + taken);
			float hp = a.getHealth();
			boolean hurt = a.hurtServer(c.getLevel(), c.getLevel().damageSources().playerAttack(p), 10f);
			c.assertTrue(!hurt && a.getHealth() == hp, "the owner's hit does nothing to a clone");
			Illusion other = clones(c, p).get(1);
			boolean hurtByClone = a.hurtServer(c.getLevel(), c.getLevel().damageSources().mobAttack(other), 10f);
			c.assertTrue(!hurtByClone && a.getHealth() == hp, "a clone's hit does nothing to a sibling");
			a.setSprinting(true);
			c.assertTrue(Math.abs(a.getAttributeValue(Attributes.MOVEMENT_SPEED) - 0.31 * (1 + Illusion.SPRINT_BONUS)) < 1e-6, "sprinting adds the clone bonus, was " + a.getAttributeValue(Attributes.MOVEMENT_SPEED));
			FlightBladeGameTest.drop(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 100)
	public void withPvpOffWhoeverHitsTheOwnerBecomesATargetAndTheCloneHurtsThem(GameTestHelper c) {
		ServerPlayer p = owner(c);
		ServerPlayer q = FlightBladeGameTest.survivor(c);
		MinecraftServer server = c.getLevel().getServer();
		boolean pvp = server.getGameRules().get(GameRules.PVP);
		server.getGameRules().set(GameRules.PVP, false, server);
		try {
			FlightBladeGameTest.place(c, q, 7.5, 1, 5.5, 0f, 0f);
			arm(p, 20);
			TechniqueCasts.cast(p, Technique.ILLUSION, 20, 1f);
			q.setHealth(20f);
			boolean refused = !p.hurtServer(c.getLevel(), c.getLevel().damageSources().playerAttack(q), 2f);
			c.assertTrue(refused, "control: PvP off refuses the player's hit on the owner");
			c.assertTrue(IllusionTargets.idsOf(p.getUUID()).contains(q.getUUID()), "the attacker is on the list although the hit was refused");
			List<Illusion> mine = clones(c, p);
			c.runAfterDelay(40, () -> {
				try {
					c.assertTrue(mine.stream().anyMatch(a -> a.getTarget() == q), "a clone targets the second player");
					q.setInvulnerableTime(0); // the (now faster) clones may have hit it already
					float before = q.getHealth();
					boolean landed = mine.get(0).doHurtTarget(c.getLevel(), q);
					double expected = 0.4 * p.getAttributeValue(Attributes.ATTACK_DAMAGE);
					c.assertTrue(Math.abs((before - q.getHealth()) - expected) < 0.01, "the clone hit lands with PvP off: " + before + " -> " + q.getHealth() + " inv=" + q.getInvulnerableTime() + " dist=" + mine.get(0).distanceTo(q) + " dmgattr=" + mine.get(0).getAttributeValue(Attributes.ATTACK_DAMAGE) + " alive=" + q.isAlive() + " gm=" + q.gameMode() + " landed=" + landed + " invTo=" + q.isInvulnerableTo(c.getLevel(), c.getLevel().damageSources().mobAttack(mine.get(0))) + " hurtTime=" + q.hurtTime + " abs=" + q.getAbsorptionAmount() + " armor=" + q.getAttributeValue(Attributes.ARMOR));
				} finally {
					server.getGameRules().set(GameRules.PVP, pvp, server);
					FlightBladeGameTest.drop(c, q);
					FlightBladeGameTest.drop(c, p);
				}
				c.succeed();
			});
		} catch (RuntimeException e) {
			server.getGameRules().set(GameRules.PVP, pvp, server);
			throw e;
		}
	}

	@GameTest(maxTicks = 100)
	public void withPvpOffWhomTheOwnerHitsBecomesATarget(GameTestHelper c) {
		ServerPlayer p = owner(c);
		ServerPlayer q = FlightBladeGameTest.survivor(c);
		MinecraftServer server = c.getLevel().getServer();
		boolean pvp = server.getGameRules().get(GameRules.PVP);
		server.getGameRules().set(GameRules.PVP, false, server);
		try {
			FlightBladeGameTest.place(c, q, 7.5, 1, 5.5, 0f, 0f);
			arm(p, 1);
			TechniqueCasts.cast(p, Technique.ILLUSION, 1, 1f);
			q.setHealth(20f);
			boolean refused = !q.hurtServer(c.getLevel(), c.getLevel().damageSources().playerAttack(p), 2f);
			c.assertTrue(refused && q.getHealth() == 20f, "control: PvP off refuses the owner's hit");
			c.assertTrue(IllusionTargets.idsOf(p.getUUID()).contains(q.getUUID()), "the player the owner hit is on the list");
			Illusion a = clones(c, p).get(0);
			c.runAfterDelay(40, () -> {
				try {
					c.assertTrue(a.getTarget() == q, "the clone targets the player the owner hit, was " + a.getTarget());
				} finally {
					server.getGameRules().set(GameRules.PVP, pvp, server);
					FlightBladeGameTest.drop(c, q);
					FlightBladeGameTest.drop(c, p);
				}
				c.succeed();
			});
		} catch (RuntimeException e) {
			server.getGameRules().set(GameRules.PVP, pvp, server);
			throw e;
		}
	}

	@GameTest(maxTicks = 100)
	public void hostileMobsAreTargetsCreepersToo(GameTestHelper c) {
		ServerPlayer p = owner(c);
		arm(p, 1);
		TechniqueCasts.cast(p, Technique.ILLUSION, 1, 1f);
		Mob creeper = c.spawn(EntityTypes.CREEPER, new Vec3(7.5, 1, 7.5));
		creeper.setNoAi(true);
		Illusion a = clones(c, p).get(0);
		c.runAfterDelay(40, () -> {
			c.assertTrue(a.getTarget() == creeper, "the clone targets the creeper, was " + a.getTarget());
			FlightBladeGameTest.drop(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 40)
	public void ownersOwnThingsNeverJoinTheList(GameTestHelper c) {
		ServerPlayer p = owner(c);
		arm(p, 1);
		TechniqueCasts.cast(p, Technique.ILLUSION, 1, 1f);
		Illusion a = clones(c, p).get(0);
		IllusionTargets.noteDamage(a, c.getLevel().damageSources().playerAttack(p));
		TechniqueCasts.cast(p, Technique.GOLEM, 1, 1f);
		VioletGuardian g = VioletGuardian.guardiansOf(c.getLevel(), p.getUUID()).get(0);
		IllusionTargets.noteDamage(g, c.getLevel().damageSources().playerAttack(p));
		c.assertTrue(IllusionTargets.idsOf(p.getUUID()).isEmpty(), "the owner's clone and guardian are never targets, list " + IllusionTargets.idsOf(p.getUUID()));
		c.assertTrue(!a.canAttack(p), "a clone never attacks its owner");
		FlightBladeGameTest.drop(c, p);
		c.succeed();
	}

	@GameTest(maxTicks = 60)
	public void aCloneKillGivesTheOwnerCredit(GameTestHelper c) {
		ServerPlayer p = owner(c);
		arm(p, 1);
		TechniqueCasts.cast(p, Technique.ILLUSION, 1, 1f);
		Illusion a = clones(c, p).get(0);
		Zombie z = c.spawn(EntityTypes.ZOMBIE, new Vec3(6.5, 1, 6.5));
		z.setNoAi(true);
		z.setHealth(0.1f);
		int kills = HeartsAttachment.get(p).totalHostileKills();
		a.doHurtTarget(c.getLevel(), z);
		c.assertTrue(!z.isAlive(), "the clone killed the zombie");
		int now = HeartsAttachment.get(p).totalHostileKills();
		c.assertTrue(now == kills + 1, "the owner's kill count went " + kills + " -> " + now);
		FlightBladeGameTest.drop(c, p);
		c.succeed();
	}

	@GameTest(maxTicks = 80)
	public void clonesVanishAfterTheirTimeAndWhenTheOwnerLeaves(GameTestHelper c) {
		ServerPlayer p = owner(c);
		arm(p, 10);
		TechniqueCasts.cast(p, Technique.ILLUSION, 10, 1f);
		List<Illusion> mine = clones(c, p);
		c.assertTrue(mine.size() == 2 && IllusionTargets.active(p.getUUID()), "two clones and an open target list");
		Illusion a = mine.get(0);
		Illusion b = mine.get(1);
		a.setEndTick(c.getLevel().getGameTime() + 2); // fast-forward
		c.runAfterDelay(8, () -> {
			c.assertTrue(!a.isAlive(), "the first clone vanished after its time");
			c.assertTrue(b.isAlive() && IllusionTargets.active(p.getUUID()), "the other stays, the list stays");
			FlightBladeGameTest.drop(c, p); // the owner leaves
			c.runAfterDelay(8, () -> {
				c.assertTrue(!b.isAlive(), "the clone vanishes with its owner");
				c.assertTrue(!IllusionTargets.active(p.getUUID()), "the list is cleared when the clones end");
				c.succeed();
			});
		});
	}

	/** Runs a clone along a long strip; every tick the owner (and for sprinting a target) is put ahead of it. Measures blocks/s over ticks 20..80. */
	private static void measureSpeed(GameTestHelper c, boolean sprint, double expected) {
		FlightBladeGameTest.enable(c);
		for (int x = 0; x < 22; x++) {
			c.setBlock(x, 0, 5, Blocks.STONE);
			c.setBlock(x, 0, 6, Blocks.STONE);
		}
		ServerPlayer p = FlightBladeGameTest.survivor(c);
		arm(p, 1);
		FlightBladeGameTest.place(c, p, 2.5, 1, 5.5, 0f, 0f);
		TechniqueCasts.cast(p, Technique.ILLUSION, 1, 1f);
		Illusion a = clones(c, p).get(0);
		Vec3 s0 = c.absoluteVec(new Vec3(2.5, 1, 5.5));
		a.snapTo(s0.x, s0.y, s0.z);
		a.setDeltaMovement(Vec3.ZERO);
		a.setEndTick(c.getLevel().getGameTime() + 100000);
		// The clone stays inside the loaded test area: after every tick its step is counted and it is put back at the start.
		Vec3 dir = c.absoluteVec(new Vec3(1, 0, 0)).subtract(c.absoluteVec(Vec3.ZERO));
		double[] total = {0};
		c.onEachTick(() -> {
			long t = c.getTick();
			if (t > 80) {
				return;
			}
			double step = Math.hypot(a.getX() - s0.x, a.getZ() - s0.z);
			if (t > 20) {
				total[0] += step;
			}
			a.snapTo(s0.x, a.getY(), s0.z);
			// 6-12 blocks from the owner: the clone walks; over 12 (under 24): it sprints, like a player running after the owner
			double ahead = sprint ? 14 : 9;
			p.teleportTo(c.getLevel(), s0.x + ahead * dir.x, a.getY(), s0.z + ahead * dir.z, Set.of(), 0f, 0f, false);
			a.setTarget(null);
			if (t == 80) {
				double speed = total[0] / 3.0; // ticks 21..80 = 3 s
				boolean sp = a.isSprinting();
				FlightBladeGameTest.drop(c, p);
				System.out.println("ILLUSION-SPEED " + (sprint ? "sprint" : "walk") + " " + speed + " blocks/s");
				c.assertTrue(Math.abs(speed - expected) <= expected * 0.10, (sprint ? "sprint" : "walk") + " speed " + speed + " blocks/s, expected " + expected + " (+-10%), sprinting=" + sp);
				c.succeed();
			}
		});
	}

	@GameTest(maxTicks = 100)
	public void aCloneWalksAtAboutAPlayersWalkingSpeed(GameTestHelper c) {
		measureSpeed(c, false, 4.3);
	}

	@GameTest(maxTicks = 100)
	public void aCloneSprintsAtAboutAPlayersSprintingSpeed(GameTestHelper c) {
		measureSpeed(c, true, 5.6);
	}
}
