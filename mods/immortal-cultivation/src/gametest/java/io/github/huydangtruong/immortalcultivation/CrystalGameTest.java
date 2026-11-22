package io.github.huydangtruong.immortalcultivation;

import io.github.huydangtruong.immortalcultivation.technique.Crystal;
import io.github.huydangtruong.immortalcultivation.technique.CrystalBlock;
import io.github.huydangtruong.immortalcultivation.technique.CrystalDisarm;
import io.github.huydangtruong.immortalcultivation.technique.CrystalRules;
import io.github.huydangtruong.immortalcultivation.technique.Technique;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueAttachment;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueData;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueNet;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueRules;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** P-056 / D143: Crystal Prison on the server: the cage, who is kept or pushed out, disarm, Enderman, hardness, containers. */
public class CrystalGameTest {
	private static ServerPlayer caster(GameTestHelper c, int level) {
		GameTestKit.enable(c);
		GameTestKit.floor(c);
		c.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true);
		ServerPlayer p = GameTestKit.survivor(c);
		GameTestKit.place(c, p, 3.5, 1, 2.5, 0f, 0f);
		TechniqueAttachment.set(p, TechniqueData.DEFAULT.withLevel(Technique.CRYSTAL, level, 10)
			.withEmeralds(TechniqueRules.MAX_EMERALDS).withSlot(0, Technique.CRYSTAL));
		return p;
	}

	private static Vec3 abs(GameTestHelper c, double x, double y, double z) {
		return c.absoluteVec(new Vec3(x, y, z));
	}

	private static void still(Mob m) {
		m.setPersistenceRequired();
		m.setNoAi(true);
		m.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 2000, 0, false, false, false));
		m.setOnGround(true);
	}

	private static Zombie zombie(GameTestHelper c, double x, double z) {
		Zombie z1 = c.spawn(EntityTypes.ZOMBIE, new Vec3(x, 1, z));
		still(z1);
		return z1;
	}

	/** Crystal blocks in the 5x5x5 box of a cage centred on block column (cx, cz) with floor at y 0 (relative coordinates). */
	private static int crystals(GameTestHelper c, int cx, int cz) {
		int n = 0;
		for (int x = cx - 2; x <= cx + 2; x++) {
			for (int y = 0; y <= 4; y++) {
				for (int z = cz - 2; z <= cz + 2; z++) {
					if (c.getBlockState(new BlockPos(x, y, z)).getBlock() instanceof CrystalBlock) {
						n++;
					}
				}
			}
		}
		return n;
	}

	private static String missing(GameTestHelper c, int cx, int cz) {
		StringBuilder sb = new StringBuilder();
		for (var cell : CrystalRules.cage(cx, 0, cz).shell()) {
			BlockState st = c.getBlockState(new BlockPos(cell.x(), cell.y(), cell.z()));
			if (!(st.getBlock() instanceof CrystalBlock)) {
				sb.append(cell).append('=').append(st.getBlock().getDescriptionId()).append(' ');
			}
		}
		return sb.toString();
	}

	private static AABB interior(GameTestHelper c, int cx, int cz) {
		return new AABB(abs(c, cx - 1, 1, cz - 1), abs(c, cx + 2, 4, cz + 2));
	}

	private static AABB shellBox(GameTestHelper c, int cx, int cz) {
		return new AABB(abs(c, cx - 2, 0, cz - 2), abs(c, cx + 3, 5, cz + 3));
	}

	@GameTest(maxTicks = 260)
	public void leftClickCagesTheAimedMobClearsWaterAndTheCrystalVanishesAfterEightSeconds(GameTestHelper c) {
		ServerPlayer p = caster(c, 1);
		for (int x = 1; x <= 5; x++) {
			for (int z = 3; z <= 7; z++) {
				c.setBlock(x, 1, z, Blocks.WATER);
				c.setBlock(x, 2, z, Blocks.WATER);
			}
		}
		Zombie z = zombie(c, 3.5, 5.5);
		c.runAfterDelay(5, () -> {
			c.assertTrue(TechniqueNet.cast(p, 0, 0f) == null, "the aimed cast is accepted");
			c.assertTrue(crystals(c, 3, 5) == 54, "54 crystal blocks, found " + crystals(c, 3, 5) + " missing " + missing(c, 3, 5));
			for (int x = 2; x <= 4; x++) {
				for (int y = 1; y <= 3; y++) {
					for (int zz = 4; zz <= 6; zz++) {
						c.assertTrue(c.getBlockState(new BlockPos(x, y, zz)).isAir(), "the hollow is air at " + x + "," + y + "," + zz);
					}
				}
			}
			c.assertTrue(interior(c, 3, 5).contains(z.position()), "the zombie is inside the hollow");
			c.assertTrue(CrystalDisarm.disarmed(z), "the caged hostile mob is disarmed");
			c.assertTrue(TechniqueNet.cast(p, 0, 0f) != null, "the cooldown is running");
		});
		c.runAfterDelay(120, () -> c.assertTrue(crystals(c, 3, 5) == 54, "the cage still stands after 6 s"));
		c.runAfterDelay(200, () -> {
			c.assertTrue(crystals(c, 3, 5) == 0, "the crystal is gone after 8 s, left " + crystals(c, 3, 5));
			GameTestKit.drop(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 40)
	public void threeZombiesGiveTwoCagedAndOnePushedOut(GameTestHelper c) {
		ServerPlayer p = caster(c, 5);
		Zombie a = zombie(c, 3.5, 5.5);
		Zombie b = zombie(c, 4.2, 5.5);
		Zombie d = zombie(c, 2.8, 5.8);
		c.runAfterDelay(5, () -> {
			c.assertTrue(TechniqueNet.cast(p, 0, 0f) == null, "the aimed cast is accepted");
			AABB in = interior(c, 3, 5);
			AABB box = shellBox(c, 3, 5);
			c.assertTrue(in.contains(a.position()), "the aimed zombie is caged");
			int inside = 0;
			for (Zombie z : List.of(a, b, d)) {
				if (in.contains(z.position())) {
					inside++;
				} else {
					c.assertTrue(!z.getBoundingBox().intersects(box), "a zombie left out stands outside the cage, at " + z.position());
				}
			}
			c.assertTrue(inside == 2, "exactly two zombies are caged, found " + inside + " a=" + a.position() + " b=" + b.position() + " d=" + d.position() + " in=" + in);
			GameTestKit.drop(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 100)
	public void rightClickCagesTheCasterAndTheNearestMobAndTheCasterCanStillFight(GameTestHelper c) {
		ServerPlayer p = caster(c, 1);
		Zombie z = c.spawn(EntityTypes.ZOMBIE, new Vec3(4.5, 1, 3.5)); // keeps its AI: it would attack at once
		z.setPersistenceRequired();
		z.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 2000, 0, false, false, false));
		GameTestKit.place(c, p, 3.5, 1, 3.5, 0f, 0f);
		float[] hp = new float[1];
		c.runAfterDelay(1, () -> c.assertTrue(TechniqueNet.cast(p, 0, 1f) == null, "the self cast is accepted"));
		c.runAfterDelay(4, () -> {
			hp[0] = p.getHealth();
			c.assertTrue(crystals(c, 3, 3) == 54, "54 crystal blocks, found " + crystals(c, 3, 3));
			AABB in = interior(c, 3, 3);
			c.assertTrue(in.contains(p.position()) && in.contains(z.position()), "the caster and the zombie are inside");
			c.assertTrue(CrystalDisarm.disarmed(z) && !CrystalDisarm.disarmed(p), "only the mob is disarmed");
		});
		c.runAfterDelay(50, () -> {
			c.assertTrue(p.getHealth() == hp[0], "the disarmed zombie did not hurt the caster for 2.5 s, health " + p.getHealth() + " was " + hp[0]);
			float before = z.getHealth();
			p.attack(z);
			c.assertTrue(z.getHealth() < before, "the caster can still hit the zombie");
			GameTestKit.drop(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 40)
	public void aCagedEndermanCannotTeleportOut(GameTestHelper c) {
		ServerPlayer p = caster(c, 1);
		GameTestKit.place(c, p, 3.5, 1, 3.5, 0f, 0f);
		Enderman e = c.spawn(EntityTypes.ENDERMAN, new Vec3(4.5, 1, 3.5));
		still(e);
		c.runAfterDelay(3, () -> {
			c.assertTrue(TechniqueNet.cast(p, 0, 1f) == null, "the self cast is accepted");
			c.assertTrue(Crystal.inCage(e), "the Enderman is in the cage");
			Vec3 at = e.position();
			Vec3 far = abs(c, 6.5, 1, 6.5);
			boolean moved = e.teleport(far.x, far.y, far.z);
			c.assertTrue(!moved && e.position().distanceTo(at) < 0.01, "the Enderman did not teleport, now at " + e.position());
			GameTestKit.drop(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 20)
	public void aHigherLevelCrystalTakesLongerToBreakAndResistsBlastsBetter(GameTestHelper c) {
		ServerPlayer p = caster(c, 1);
		ServerLevel level = c.getLevel();
		BlockPos pos = c.absolutePos(new BlockPos(5, 2, 8));
		BlockState low = CrystalBlock.state(1);
		BlockState high = CrystalBlock.state(20);
		float pLow = low.getDestroyProgress(p, level, pos);
		float pHigh = high.getDestroyProgress(p, level, pos);
		c.assertTrue(pLow > pHigh * 5, "Lv 20 crystal breaks much slower: " + pLow + " vs " + pHigh);
		ExplosionDamageCalculator calc = new ExplosionDamageCalculator();
		float rLow = calc.getBlockExplosionResistance(null, level, pos, low, low.getFluidState()).orElse(-1f);
		float rHigh = calc.getBlockExplosionResistance(null, level, pos, high, high.getFluidState()).orElse(-1f);
		c.assertTrue(Math.abs(rLow - CrystalRules.crystalBlast(1)) < 0.01 && Math.abs(rHigh - CrystalRules.crystalBlast(20)) < 0.01,
			"blast resistance follows the tier: " + rLow + " and " + rHigh);
		GameTestKit.drop(c, p);
		c.succeed();
	}

	@GameTest(maxTicks = 40)
	public void aChestInTheWallSpillsItsContentsInsteadOfDeletingThem(GameTestHelper c) {
		ServerPlayer p = caster(c, 1);
		GameTestKit.place(c, p, 3.5, 1, 3.5, 0f, 0f);
		c.setBlock(1, 1, 3, Blocks.CHEST);
		c.runAfterDelay(2, () -> {
			var be = c.getLevel().getBlockEntity(c.absolutePos(new BlockPos(1, 1, 3)));
			c.assertTrue(be instanceof Container, "the chest has a container");
			((Container) be).setItem(0, new ItemStack(Items.DIAMOND, 3));
		});
		c.runAfterDelay(4, () -> {
			c.assertTrue(TechniqueNet.cast(p, 0, 1f) == null, "the self cast is accepted");
			c.assertTrue(c.getBlockState(new BlockPos(1, 1, 3)).getBlock() instanceof CrystalBlock, "the chest was replaced by crystal");
			List<ItemEntity> items = c.getLevel().getEntitiesOfClass(ItemEntity.class, shellBox(c, 3, 3).inflate(1), i -> i.getItem().is(Items.DIAMOND));
			c.assertTrue(items.size() == 1 && items.get(0).getItem().getCount() == 3, "the diamonds dropped, found " + items.size());
			GameTestKit.drop(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 20)
	public void aTooBigMobCannotBeCagedAndTheCastIsRefused(GameTestHelper c) {
		ServerPlayer p = caster(c, 1);
		var ghast = c.spawn(EntityTypes.GHAST, new Vec3(3.5, 1, 5.5));
		still(ghast);
		c.assertTrue(TechniqueNet.cast(p, 0, 0f) != null, "a ghast cannot be caged");
		c.assertTrue(crystals(c, 3, 5) == 0, "no crystal was placed");
		GameTestKit.drop(c, p);
		c.succeed();
	}

	/** D146: a disarmed player (Death Match opponent) cannot hit with a weapon, but technique damage still lands. */
	@GameTest(maxTicks = 40)
	public void aDisarmedPlayerKeepsTechniquesButCannotHit(GameTestHelper c) {
		ServerPlayer p = caster(c, 1);
		p.addEffect(new MobEffectInstance(CrystalDisarm.EFFECT, 200, 0, false, false, false));
		Zombie z = zombie(c, 5.5, 5.5);
		ServerLevel level = c.getLevel();
		float full = z.getHealth();
		z.hurtServer(level, level.damageSources().playerAttack(p), 4);
		c.assertTrue(z.getHealth() == full, "a melee hit by a disarmed player does nothing, health " + z.getHealth());
		z.hurtServer(level, level.damageSources().indirectMagic(z, p), 4); // a technique orb: direct entity is not the player
		c.assertTrue(z.getHealth() < full, "technique damage from a disarmed player still lands, health " + z.getHealth());
		GameTestKit.drop(c, p);
		c.succeed();
	}
}
