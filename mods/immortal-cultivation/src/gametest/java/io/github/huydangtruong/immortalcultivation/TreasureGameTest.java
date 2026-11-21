package io.github.huydangtruong.immortalcultivation;

import io.github.huydangtruong.immortalcultivation.treasure.DeadlyTreasure;
import io.github.huydangtruong.immortalcultivation.treasure.MegaMobs;
import io.github.huydangtruong.immortalcultivation.treasure.TreasureRules;
import io.github.huydangtruong.immortalcultivation.treasure.TreasureSavedData;
import io.github.huydangtruong.immortalcultivation.wanderer.Wanderer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

/** Deadly Treasure (P-059, D150): chest, loot, guards, lock, success, failure, trigger, Mega damage. */
public class TreasureGameTest {
	private static TreasureSavedData fresh(GameTestHelper c) {
		MinecraftServer s = c.getLevel().getServer();
		s.getGameRules().set(ImmortalCultivation.ENABLED, true, s);
		TreasureSavedData d = DeadlyTreasure.data(s);
		d.clear();
		d.id++;
		d.phase = TreasureRules.Phase.ACTIVE;
		d.endAt = Wanderer.clockTime(s) + TreasureRules.DURATION_TICKS;
		return d;
	}

	/** {non-empty stacks, 1 if a level bottle is among them}. */
	private static int[] stacks(ServerLevel level, BlockPos chest) {
		ChestBlockEntity box = (ChestBlockEntity) level.getBlockEntity(chest);
		int n = 0;
		int bottle = 0;
		for (int i = 0; i < box.getContainerSize(); i++) {
			if (!box.getItem(i).isEmpty()) {
				n++;
				bottle |= box.getItem(i).is(io.github.huydangtruong.immortalcultivation.reward.RewardBoxes.BOTTLE) ? 1 : 0;
			}
		}
		return new int[] {n, bottle};
	}

	@GameTest(maxTicks = 100)
	public void megaEndermanStays(GameTestHelper c) {
		ServerLevel level = c.getLevel();
		TreasureSavedData d = fresh(c);
		DeadlyTreasure.place(level, d, c.absolutePos(new BlockPos(1, 1, 1)), false, "endermen");
		List<Mob> g = guards(level, d);
		c.assertTrue(!g.isEmpty() && g.get(0) instanceof net.minecraft.world.entity.monster.Enderman, "Mega Endermen");
		net.minecraft.world.entity.monster.Enderman e = (net.minecraft.world.entity.monster.Enderman) g.get(0);
		c.assertTrue(!e.teleport(e.getX() + 8, e.getY(), e.getZ()), "a Mega Enderman cannot teleport");
		c.assertTrue(!e.isSensitiveToWater(), "nor fears water");
		DeadlyTreasure.fail(level.getServer(), d);
		c.succeed();
	}

	private static List<Mob> guards(ServerLevel level, TreasureSavedData d) {
		List<Mob> out = new ArrayList<>();
		for (String id : d.guards) {
			if (level.getEntity(UUID.fromString(id)) instanceof Mob m) {
				out.add(m);
			}
		}
		return out;
	}

	@GameTest(maxTicks = 200)
	public void guardedChestOpensWhenAllGuardsDie(GameTestHelper c) {
		ServerLevel level = c.getLevel();
		TreasureSavedData d = fresh(c);
		DeadlyTreasure.Spot spot = DeadlyTreasure.place(level, d, c.absolutePos(new BlockPos(1, 1, 1)), false, "blazes");
		BlockPos chest = spot.chest();
		c.assertTrue(level.getBlockEntity(chest) instanceof ChestBlockEntity, "a chest stands at " + chest);
		c.assertTrue(stacks(level, chest)[0] == 0, "empty while locked: nothing for hoppers or explosions to take");
		level.setBlock(chest, Blocks.AIR.defaultBlockState(), 3); // blown up by TNT, say
		DeadlyTreasure.tick(level.getServer());
		c.assertTrue(level.getBlockEntity(chest) instanceof ChestBlockEntity, "a destroyed chest comes back");
		List<Mob> g = guards(level, d);
		c.assertTrue(g.size() == 6, "6 Mega Blazes, got " + g.size());
		c.assertTrue(Math.abs(g.get(0).getMaxHealth() - 80f) < 0.01f, "x4 health: " + g.get(0).getMaxHealth());
		c.assertTrue(MegaMobs.isMega(g.get(0)), "guards carry the Mega attachment");
		c.assertTrue(DeadlyTreasure.locked(level, chest), "locked while guards live");
		for (Mob m : g) {
			m.kill(level);
		}
		c.assertTrue(!DeadlyTreasure.locked(level, chest), "open when all guards are dead");
		int[] loot = stacks(level, chest);
		c.assertTrue(loot[0] >= 8 && loot[1] == 1, "bottle, apple and 6+ more once open: " + loot[0]);
		c.assertTrue(d.phase == TreasureRules.Phase.IDLE, "event over: " + d.phase);
		level.setBlock(chest, Blocks.AIR.defaultBlockState(), 3);
		c.succeed();
	}

	@GameTest(maxTicks = 200)
	public void timeUpRemovesChestAndGuards(GameTestHelper c) {
		ServerLevel level = c.getLevel();
		TreasureSavedData d = fresh(c);
		BlockPos chest = DeadlyTreasure.place(level, d, c.absolutePos(new BlockPos(1, 1, 1)), false, "brutes").chest();
		List<Mob> g = guards(level, d);
		c.assertTrue(g.size() == 6, "6 brutes");
		d.endAt = Wanderer.clockTime(level.getServer()) - 1;
		DeadlyTreasure.tick(level.getServer());
		c.assertTrue(d.phase == TreasureRules.Phase.IDLE, "failed -> idle");
		c.assertTrue(!(level.getBlockEntity(chest) instanceof ChestBlockEntity), "chest gone");
		c.assertTrue(g.stream().allMatch(Entity::isRemoved), "guards gone");
		c.succeed();
	}

	@GameTest(maxTicks = 100)
	public void crimsonMasterLeavingGivesItsNextVisitToTheTreasure(GameTestHelper c) { // D172
		MinecraftServer s = c.getLevel().getServer();
		TreasureSavedData d = DeadlyTreasure.data(s);
		d.clear();
		long next = Wanderer.data(s).nextDay();
		DeadlyTreasure.onCrimsonMasterLeft(s);
		c.assertTrue(d.phase == TreasureRules.Phase.PENDING && d.startAt == next * 24000L, "pending for the next visit day " + next + ": " + d.startAt);
		c.assertTrue(DeadlyTreasure.takesCrimsonSlot(s), "the Crimson Master skips that day");
		long first = d.startAt;
		DeadlyTreasure.onCrimsonMasterLeft(s);
		c.assertTrue(d.startAt == first, "a second trigger changes nothing");
		d.clear();
		c.assertTrue(!DeadlyTreasure.takesCrimsonSlot(s), "idle: the Crimson Master comes");
		c.succeed();
	}

	@GameTest(maxTicks = 100)
	public void megaHitsHarder(GameTestHelper c) {
		ServerLevel level = c.getLevel();
		Mob m = MegaMobs.create(level, TreasureRules.group("blazes").guards().get(0), c.absolutePos(new BlockPos(1, 1, 1)), 0,
			c.absolutePos(new BlockPos(1, 1, 1)));
		float hit = MegaMobs.scaleDamage(level.damageSources().mobAttack(m), 2f);
		c.assertTrue(Math.abs(hit - 8f) < 0.001f, "x4 damage: " + hit);
		c.succeed();
	}
}
