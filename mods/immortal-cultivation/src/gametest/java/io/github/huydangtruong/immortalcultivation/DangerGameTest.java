package io.github.huydangtruong.immortalcultivation;

import io.github.huydangtruong.immortalcultivation.danger.Danger;
import io.github.huydangtruong.immortalcultivation.danger.DangerMobs;
import io.github.huydangtruong.immortalcultivation.danger.DangerRules;
import io.github.huydangtruong.immortalcultivation.danger.DangerSavedData;
import io.github.huydangtruong.immortalcultivation.wanderer.QuestBoss;
import io.github.huydangtruong.immortalcultivation.wanderer.Wanderer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * P-054 (D137, D138): danger level growth, strength, exclusions, day spawns, elites, gear and the switch. The level is
 * only raised for the moment of a spawn, so tests that run at the same time never see it.
 */
public class DangerGameTest {
	private static void enable(GameTestHelper c) {
		GameTestKit.enable(c);
		MinecraftServer s = c.getLevel().getServer();
		s.getGameRules().set(Danger.ENABLED, true, s);
	}

	private static Zombie zombieAt(GameTestHelper c, int level, double x) {
		MinecraftServer s = c.getLevel().getServer();
		int before = DangerSavedData.of(s).level;
		DangerSavedData.of(s).level = level;
		try {
			Zombie z = c.spawn(EntityTypes.ZOMBIE, new Vec3(x, 1, 5.5));
			z.setNoAi(true);
			return z;
		} finally {
			DangerSavedData.of(s).level = before;
		}
	}

	private static int netheritePieces(Mob m) {
		int n = 0;
		for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
			var item = m.getItemBySlot(slot).getItem();
			n += item == Items.NETHERITE_HELMET || item == Items.NETHERITE_CHESTPLATE || item == Items.NETHERITE_LEGGINGS || item == Items.NETHERITE_BOOTS ? 1 : 0;
		}
		return n;
	}

	@GameTest(maxTicks = 20)
	public void levelGrowsOnlyAfter4800TicksAndCapsAtFifty(GameTestHelper c) {
		enable(c);
		MinecraftServer s = c.getLevel().getServer();
		DangerSavedData d = DangerSavedData.of(s);
		d.level = 0;
		d.pendingAtTick = 0;
		long now = s.overworld().getGameTime();
		Wanderer.leave(s, Component.empty());
		c.assertTrue(d.pendingAtTick >= now + 4800 && d.pendingAtTick <= now + 4800 + 5, "growth pending in 4800 ticks, was " + (d.pendingAtTick - now));
		long due = d.pendingAtTick;
		Danger.tick(s, due - 1);
		c.assertTrue(d.level == 0, "not before 4800 ticks");
		Danger.tick(s, due);
		c.assertTrue(d.level == 1, "+1 when due, was " + d.level);
		c.assertTrue(d.pendingAtTick == 0, "nothing pending after growth");
		d.level = 50;
		d.pendingAtTick = now;
		Danger.tick(s, now);
		c.assertTrue(d.level == 50, "capped at 50");
		d.level = 0;
		d.pendingAtTick = 0;
		c.succeed();
	}

	@GameTest(maxTicks = 20)
	public void zombieAtThirtyThreeHasStrengthBonusInHealthAndDamage(GameTestHelper c) {
		enable(c);
		Zombie z = zombieAt(c, 33, 3.5);
		for (int i = 0; i < 50 && io.github.huydangtruong.immortalcultivation.danger.DangerMobs.isElite(z); i++) {
			z.discard(); // level 33 can roll an elite (x2 health): this test checks the plain strength only
			z = zombieAt(c, 33, 3.5);
		}
		double k = 1 + DangerRules.strengthBonus(33);
		c.assertTrue(Math.abs(z.getMaxHealth() - 20 * k) < 0.01, "max health 20 x " + k + ", was " + z.getMaxHealth());
		c.assertTrue(Math.abs(z.getHealth() - 20 * k) < 0.01, "healed to full, was " + z.getHealth());
		c.assertTrue(Math.abs(z.getAttributeValue(Attributes.ATTACK_DAMAGE) - 3 * k) < 0.01, "damage 3 x " + k + ", was " + z.getAttributeValue(Attributes.ATTACK_DAMAGE));
		// a quest chieftain tagged before it enters the world keeps its values
		MinecraftServer s = c.getLevel().getServer();
		ServerLevel level = c.getLevel();
		Zombie boss = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
		boss.addTag(QuestBoss.TAG);
		Vec3 at = c.absoluteVec(new Vec3(5.5, 1, 5.5));
		boss.snapTo(at.x, at.y, at.z, 0f, 0f);
		boss.setNoAi(true);
		DangerSavedData.of(s).level = 33;
		try {
			level.addFreshEntity(boss);
		} finally {
			DangerSavedData.of(s).level = 0;
		}
		c.assertTrue(boss.getMaxHealth() == 20f, "a chieftain is not changed, was " + boss.getMaxHealth());
		c.succeed();
	}

	@GameTest(maxTicks = 20)
	public void switchedOffNothingChanges(GameTestHelper c) {
		enable(c);
		MinecraftServer s = c.getLevel().getServer();
		s.getGameRules().set(Danger.ENABLED, false, s);
		try {
			Zombie z = zombieAt(c, 50, 3.5);
			c.assertTrue(z.getMaxHealth() == 20f && netheritePieces(z) == 0, "no effect with the switch off");
			DangerSavedData d = DangerSavedData.of(s);
			d.pendingAtTick = 0;
			Wanderer.leave(s, Component.empty());
			c.assertTrue(d.pendingAtTick == 0, "no growth with the switch off");
			c.assertTrue(Danger.level(s) == 0, "level reads 0");
		} finally {
			s.getGameRules().set(Danger.ENABLED, true, s);
		}
		c.succeed();
	}

	@GameTest(maxTicks = 20)
	public void levelFiftyGivesNetheriteArmor(GameTestHelper c) {
		enable(c);
		for (int i = 0; i < 12; i++) {
			Zombie z = zombieAt(c, 50, 1.5 + i * 0.5);
			c.assertTrue(netheritePieces(z) >= 1, "zombie " + i + " has a netherite piece");
			c.assertTrue(Math.abs(z.getMaxHealth() - 80) < 0.01 || Math.abs(z.getMaxHealth() - 160) < 0.01, "x4 health (x8 elite), was " + z.getMaxHealth());
		}
		c.succeed();
	}

	/** D207: XP follows the extra health: x(1 + strength) for a boosted mob, x2 more for an elite, vanilla at level 0. */
	@GameTest(maxTicks = 20)
	public void boostedMobDropsXpInStepWithItsHealth(GameTestHelper c) {
		enable(c);
		ServerLevel level = c.getLevel();
		Zombie plain = zombieAt(c, 0, 2.5);
		int vanilla = plain.getExperienceReward(level, null);
		c.assertTrue(vanilla == 5, "a zombie gives 5 XP, was " + vanilla);
		Zombie z = zombieAt(c, 50, 3.5);
		while (DangerMobs.isElite(z)) {
			z.discard();
			z = zombieAt(c, 50, 3.5);
		}
		for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
			z.setItemSlot(slot, net.minecraft.world.item.ItemStack.EMPTY); // worn armor adds random vanilla XP
		}
		int boosted = z.getExperienceReward(level, null);
		c.assertTrue(boosted == 20, "x4 health gives x4 XP (5 x 4), was " + boosted);
		DangerMobs.makeElite(z, java.util.List.of(DangerRules.Affix.IRONHIDE));
		int elite = z.getExperienceReward(level, null);
		c.assertTrue(elite == 40, "an elite doubles it again, was " + elite);
		Zombie mid = zombieAt(c, 25, 4.5);
		while (DangerMobs.isElite(mid)) {
			mid.discard();
			mid = zombieAt(c, 25, 4.5);
		}
		float ratio = mid.getMaxHealth() / 20f;
		c.assertTrue(mid.getExperienceReward(level, null) == Math.round(5 * ratio), "xp follows the health ratio " + ratio);
		z.discard();
		plain.discard();
		mid.discard();
		c.succeed();
	}

	/** D206: an old save (no format field, level on the 0..30 scale) becomes the 0..50 scale once (x5/3 minus 5, D209); a new one does not. */
	@GameTest(maxTicks = 20)
	public void oldSavedLevelIsConvertedOnce(GameTestHelper c) {
		DangerSavedData old = DangerSavedData.load(30, 777L, 1);
		c.assertTrue(old.level == 45 && old.pendingAtTick == 777L, "30 becomes 45 and the timer stays, was " + old.level + "/" + old.pendingAtTick);
		c.assertTrue(DangerSavedData.load(12, 0L, 1).level == 15, "12 becomes 15");
		DangerSavedData now = DangerSavedData.load(30, 0L, DangerSavedData.FORMAT);
		c.assertTrue(now.level == 30, "a format 2 file is not converted again");
		c.assertTrue(new DangerSavedData().level == 0, "a new world starts at 0");
		c.succeed();
	}

	@GameTest(maxTicks = 80)
	public void dayTaggedMobDoesNotBurn(GameTestHelper c) {
		enable(c);
		ServerLevel level = c.getLevel();
		level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(), "time set day");
		for (int x = 0; x < 12; x++) {
			for (int z = 0; z < 12; z++) {
				c.setBlock(x, 0, z, Blocks.STONE);
			}
		}
		Zombie day = c.spawn(EntityTypes.ZOMBIE, new Vec3(3.5, 1, 3.5));
		day.addTag(DangerMobs.DAY_TAG);
		day.setNoAi(true);
		c.runAfterDelay(60, () -> {
			c.assertTrue(!day.isOnFire(), "a day-spawned zombie does not burn");
			c.succeed();
		});
	}

	@GameTest(maxTicks = 120)
	public void eliteSwiftVenomousHasOrangeNameSpeedAndPoisons(GameTestHelper c) {
		enable(c);
		ServerLevel level = c.getLevel();
		var src = level.getServer().createCommandSourceStack().withLevel(level).withPosition(c.absoluteVec(new Vec3(2.5, 1, 2.5)));
		level.getServer().getCommands().performPrefixedCommand(src, "danger elite swift venomous");
		Zombie elite = null;
		for (Zombie z : level.getEntitiesOfClass(Zombie.class, new AABB(c.absolutePos(new net.minecraft.core.BlockPos(0, 0, 0))).inflate(14))) {
			if (DangerMobs.isElite(z)) {
				elite = z;
			}
		}
		c.assertTrue(elite != null, "the command summons an elite zombie");
		c.assertTrue(elite.getCustomName() != null && elite.getCustomName().getString().equals("Swift Venomous Zombie"), "name, was " + elite.getCustomName());
		c.assertTrue(elite.getCustomName().getStyle().getColor() != null && elite.getCustomName().getStyle().getColor().getValue() == 0xFFAA00, "orange name");
		c.assertTrue(elite.isCustomNameVisible(), "name always visible");
		double speed = elite.getAttributeValue(Attributes.MOVEMENT_SPEED);
		c.assertTrue(Math.abs(speed - 0.23 * 1.4) < 0.01, "speed +40%, was " + speed);
		c.assertTrue(Math.abs(elite.getMaxHealth() - 40) < 0.01, "double health, was " + elite.getMaxHealth());
		Mob target = c.spawn(EntityTypes.PIG, new Vec3(8.5, 1, 8.5));
		target.setNoAi(true);
		target.hurtServer(level, level.damageSources().mobAttack(elite), 1f);
		c.assertTrue(target.hasEffect(MobEffects.POISON), "a venomous hit poisons");
		elite.discard();
		c.succeed();
	}

	@GameTest(maxTicks = 100)
	public void ironhideTakesLessAndRegeneratingHeals(GameTestHelper c) {
		enable(c);
		ServerLevel level = c.getLevel();
		Zombie z = c.spawn(EntityTypes.ZOMBIE, new Vec3(3.5, 1, 3.5));
		z.setNoAi(true);
		DangerMobs.makeElite(z, java.util.List.of(DangerRules.Affix.IRONHIDE, DangerRules.Affix.REGENERATING, DangerRules.Affix.BLAZING));
		float before = z.getHealth();
		z.hurtServer(level, level.damageSources().generic(), 10f);
		float lost = before - z.getHealth();
		c.assertTrue(Math.abs(lost - 6f) < 0.1f, "10 damage becomes 6, lost " + lost);
		float low = z.getHealth();
		z.hurtServer(level, level.damageSources().onFire(), 5f);
		c.assertTrue(z.getHealth() >= low, "Blazing ignores fire damage");
		c.runAfterDelay(50, () -> {
			c.assertTrue(z.getHealth() > low, "regenerates, " + low + " -> " + z.getHealth());
			z.discard();
			c.succeed();
		});
	}

	@GameTest(maxTicks = 40)
	public void naturalIllagersStayOutOfRaidsAndRaidersGlow(GameTestHelper c) { // D190
		enable(c);
		net.minecraft.server.level.ServerLevel level = c.getLevel();
		net.minecraft.core.BlockPos at = c.absolutePos(new net.minecraft.core.BlockPos(2, 1, 2));
		net.minecraft.world.entity.monster.illager.Pillager natural = net.minecraft.world.entity.EntityTypes.PILLAGER.create(level,
			net.minecraft.world.entity.EntitySpawnReason.NATURAL);
		natural.finalizeSpawn(level, level.getCurrentDifficultyAt(at), net.minecraft.world.entity.EntitySpawnReason.NATURAL, null);
		c.assertTrue(!natural.canJoinRaid(), "a natural pillager never joins a raid");
		net.minecraft.world.entity.monster.illager.Pillager wave = net.minecraft.world.entity.EntityTypes.PILLAGER.create(level,
			net.minecraft.world.entity.EntitySpawnReason.EVENT);
		wave.finalizeSpawn(level, level.getCurrentDifficultyAt(at), net.minecraft.world.entity.EntitySpawnReason.EVENT, null);
		c.assertTrue(wave.canJoinRaid(), "a raid wave pillager still joins");
		net.minecraft.world.entity.monster.illager.Vindicator in = c.spawn(net.minecraft.world.entity.EntityTypes.VINDICATOR,
			new net.minecraft.world.phys.Vec3(2.5, 1, 2.5));
		net.minecraft.world.entity.monster.illager.Vindicator out = c.spawn(net.minecraft.world.entity.EntityTypes.VINDICATOR,
			new net.minecraft.world.phys.Vec3(5.5, 1, 5.5));
		in.setNoAi(true);
		out.setNoAi(true);
		net.minecraft.world.entity.raid.Raid raid = new net.minecraft.world.entity.raid.Raid(at, net.minecraft.world.Difficulty.NORMAL);
		raid.joinRaid(level, 1, in, null, false);
		io.github.huydangtruong.immortalcultivation.danger.RaidGlow.tick(level.getServer());
		c.assertTrue(in.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING), "a raider in the raid glows");
		c.assertTrue(!out.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING), "an illager outside a raid does not");
		in.discard();
		out.discard();
		c.succeed();
	}
}
