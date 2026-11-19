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
		ChestBlockEntity box = (ChestBlockEntity) level.getBlockEntity(chest);
		int stacks = 0;
		boolean bottle = false;
		for (int i = 0; i < box.getContainerSize(); i++) {
			if (!box.getItem(i).isEmpty()) {
				stacks++;
				bottle |= box.getItem(i).is(io.github.huydangtruong.immortalcultivation.reward.RewardBoxes.BOTTLE);
			}
		}
		c.assertTrue(stacks >= 8 && bottle, "bottle, apple and 6+ more: " + stacks);
		List<Mob> g = guards(level, d);
		c.assertTrue(g.size() == 6, "6 Mega Blazes, got " + g.size());
		c.assertTrue(Math.abs(g.get(0).getMaxHealth() - 80f) < 0.01f, "x4 health: " + g.get(0).getMaxHealth());
		c.assertTrue(MegaMobs.isMega(g.get(0)), "guards carry the Mega attachment");
		c.assertTrue(DeadlyTreasure.locked(level, chest), "locked while guards live");
		for (Mob m : g) {
			m.kill(level);
		}
		c.assertTrue(!DeadlyTreasure.locked(level, chest), "open when all guards are dead");
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
	public void leavingSchedulesInTwoDays(GameTestHelper c) {
		MinecraftServer s = c.getLevel().getServer();
		TreasureSavedData d = DeadlyTreasure.data(s);
		d.clear();
		long now = Wanderer.clockTime(s);
		DeadlyTreasure.onEventLeft(s);
		c.assertTrue(d.phase == TreasureRules.Phase.PENDING && d.startAt == now + TreasureRules.DELAY_TICKS, "pending in 2 days");
		DeadlyTreasure.onEventLeft(s);
		c.assertTrue(d.startAt == now + TreasureRules.DELAY_TICKS, "a second trigger changes nothing");
		d.clear();
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
