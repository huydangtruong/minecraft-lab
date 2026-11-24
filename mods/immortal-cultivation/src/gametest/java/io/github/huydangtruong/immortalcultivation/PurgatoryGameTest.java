package io.github.huydangtruong.immortalcultivation;

import io.github.huydangtruong.immortalcultivation.purgatory.Purgatory;
import io.github.huydangtruong.immortalcultivation.purgatory.DungeonBuilder;
import io.github.huydangtruong.immortalcultivation.purgatory.PurgatoryFloor;
import io.github.huydangtruong.immortalcultivation.purgatory.PurgatoryRules;
import io.github.huydangtruong.immortalcultivation.purgatory.dungeon.Dungeon;
import io.github.huydangtruong.immortalcultivation.purgatory.dungeon.Room;
import io.github.huydangtruong.immortalcultivation.purgatory.dungeon.RoomKind;
import io.github.huydangtruong.immortalcultivation.purgatory.PurgatorySavedData;
import io.github.huydangtruong.immortalcultivation.rune.Runes;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** D234-D240: solo floors, rewards, losses, deaths, the dungeon (P-082) and watching. Each test ends its own run. */
public class PurgatoryGameTest {
	private static String id(ServerPlayer p) {
		return p.getUUID().toString();
	}

	private static PurgatorySavedData data(GameTestHelper c) {
		return Purgatory.data(c.getLevel().getServer());
	}

	private static int runes(ServerPlayer p) {
		int n = 0;
		for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
			n += p.getInventory().getItem(i).is(Runes.ITEM) ? p.getInventory().getItem(i).getCount() : 0;
		}
		return n;
	}

	private static ServerPlayer fighter(GameTestHelper c) {
		GameTestKit.enable(c);
		ServerPlayer p = GameTestKit.survivor(c);
		GameTestKit.place(c, p, 1.5, 2, 1.5, 0, 0);
		return p;
	}

	@GameTest(maxTicks = 40)
	public void aSoloWinPaysAndOpensTheNextFloor(GameTestHelper c) {
		ServerPlayer p = fighter(c);
		Vec3 home = p.position();
		c.assertTrue(Purgatory.startSolo(p) == null, "solo starts");
		PurgatorySavedData.Run r = data(c).runOf(id(p));
		c.assertTrue(r != null && Purgatory.fighting(p), "fighting");
		c.assertTrue(p.level() == Purgatory.level(c.getLevel().getServer()), "in the purgatory level"); // the test server has no extra dimensions
		c.assertTrue(Purgatory.inside(r.slot).contains(p.position()), "inside the box");
		c.assertTrue(Purgatory.startSolo(p) != null, "no second run");
		c.assertTrue(Purgatory.finish(p, true), "won");
		c.assertTrue(!Purgatory.fighting(p) && data(c).runOf(id(p)) == null, "run over");
		c.assertTrue(p.level() == c.getLevel() && p.position().distanceTo(home) < 0.01, "back on the exact spot");
		c.assertTrue(p.experienceLevel == PurgatoryRules.rewardLevels(1), "31 levels, got " + p.experienceLevel);
		c.assertTrue(runes(p) == PurgatoryRules.rewardRunes(1, 1), "4 runes, got " + runes(p));
		PurgatorySavedData.Solo s = data(c).solo(id(p));
		c.assertTrue(s.floor == 2 && s.cooldown == PurgatoryRules.COOLDOWN_TICKS, "floor 2 after a cooldown");
		c.assertTrue(Purgatory.startSolo(p) != null, "cooling down");
		GameTestKit.drop(c, p);
		c.succeed();
	}

	@GameTest(maxTicks = 40)
	public void giveUpKeepsTheFloorAndCostsNothing(GameTestHelper c) {
		ServerPlayer p = fighter(c);
		data(c).solo(id(p)).floor = 7;
		p.giveExperienceLevels(12);
		c.assertTrue(Purgatory.startSolo(p) == null, "solo starts");
		c.assertTrue(data(c).runOf(id(p)).floor == 7, "floor 7");
		c.assertTrue(Purgatory.giveUp(p) == null, "gave up");
		c.assertTrue(data(c).solo(id(p)).floor == 7, "floor kept");
		c.assertTrue(p.experienceLevel == 12 && runes(p) == 0, "no cost, no reward");
		GameTestKit.drop(c, p);
		c.succeed();
	}

	@GameTest(maxTicks = 60)
	public void dyingInTheBoxIsALossWithoutDeath(GameTestHelper c) {
		ServerPlayer p = fighter(c);
		Vec3 home = p.position();
		c.assertTrue(Purgatory.startSolo(p) == null, "solo starts");
		p.hurtServer(p.level(), p.damageSources().fellOutOfWorld(), 10000f);
		c.assertTrue(p.isAlive(), "the death is cancelled");
		c.runAfterDelay(5, () -> {
			c.assertTrue(data(c).runOf(id(p)) == null, "the run ended");
			c.assertTrue(p.level() == c.getLevel() && p.position().distanceTo(home) < 0.01, "sent home");
			c.assertTrue(p.isAlive() && p.getHealth() == p.getMaxHealth(), "full health");
			c.assertTrue(data(c).solo(id(p)).floor == 1, "floor kept");
			GameTestKit.drop(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 20)
	public void aTotemWorksOutsideThePurgatory(GameTestHelper c) {
		ServerPlayer p = GameTestKit.survivor(c);
		p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TOTEM_OF_UNDYING));
		p.hurtServer(p.level(), p.damageSources().generic(), 10000f);
		c.assertTrue(p.isAlive() && p.getOffhandItem().isEmpty(), "the totem saved the player");
		GameTestKit.drop(c, p);
		c.succeed();
	}

	@GameTest(maxTicks = 60)
	public void aTotemSavesAFighterAndTheRunGoesOn(GameTestHelper c) {
		ServerPlayer p = fighter(c);
		c.assertTrue(Purgatory.startSolo(p) == null, "solo starts");
		p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TOTEM_OF_UNDYING));
		p.hurtServer(p.level(), p.damageSources().generic(), 10000f);
		c.assertTrue(p.isAlive(), "alive");
		c.assertTrue(p.getOffhandItem().isEmpty(), "the totem was used");
		c.runAfterDelay(5, () -> {
			c.assertTrue(data(c).runOf(id(p)) != null, "the run goes on");
			Purgatory.giveUp(p);
			GameTestKit.drop(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 60)
	public void fightersCannotLeaveTheBox(GameTestHelper c) {
		ServerPlayer p = fighter(c);
		c.assertTrue(Purgatory.startSolo(p) == null, "solo starts");
		PurgatorySavedData.Run r = data(c).runOf(id(p));
		var box = Purgatory.inside(r.slot);
		p.teleportTo(Purgatory.level(c.getLevel().getServer()), box.maxX + 20, box.minY + 5, box.minZ - 20, java.util.Set.of(), 0, 0, false);
		c.runAfterDelay(3, () -> {
			c.assertTrue(Purgatory.inside(r.slot).contains(p.position()), "pulled back inside: " + p.position());
			Purgatory.finish(p, false);
			GameTestKit.drop(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 60)
	public void aWatcherSeesTheBoxAndGoesBack(GameTestHelper c) {
		ServerPlayer fighter = fighter(c);
		ServerPlayer watcher = GameTestKit.survivor(c);
		GameTestKit.place(c, watcher, 3.5, 2, 3.5, 0, 0);
		Vec3 home = watcher.position();
		c.assertTrue(Purgatory.startSolo(fighter) == null, "solo starts");
		PurgatorySavedData.Run r = data(c).runOf(id(fighter));
		c.assertTrue(Purgatory.watch(watcher, r.id) == null, "watching");
		c.assertTrue(watcher.level() == Purgatory.level(c.getLevel().getServer()) && Purgatory.inside(r.slot).contains(watcher.position()),
			"in the box: " + watcher.position() + " box " + Purgatory.inside(r.slot));
		c.assertTrue(Purgatory.startSolo(watcher) != null, "a watcher cannot start a floor");
		c.assertTrue(Purgatory.stopWatching(watcher) == null, "stopped");
		c.assertTrue(watcher.level() == c.getLevel() && watcher.position().distanceTo(home) < 0.01, "back on the exact spot");
		c.assertTrue(!data(c).watching.containsKey(id(watcher)), "not watching");
		Purgatory.finish(fighter, false);
		GameTestKit.drop(c, fighter);
		GameTestKit.drop(c, watcher);
		c.succeed();
	}

	private static net.minecraft.server.level.ServerLevel plevel(GameTestHelper c) {
		return Purgatory.level(c.getLevel().getServer());
	}

	private static Room first(Dungeon d, RoomKind kind) {
		for (Room r : d.rooms()) {
			if (r.kind == kind) {
				return r;
			}
		}
		throw new AssertionError("no " + kind + " room");
	}

	private static void go(GameTestHelper c, ServerPlayer p, PurgatorySavedData.Run r, int[] g) {
		Vec3 v = PurgatoryFloor.feet(r.slot, g);
		p.teleportTo(plevel(c), v.x, v.y, v.z, java.util.Set.of(), 0, 0, false);
	}

	/** P-082: the world holds exactly the floor the stuck checker passed, the fighter starts in its start room in Adventure mode. */
	@GameTest(maxTicks = 40)
	public void theDungeonInTheWorldIsTheCheckedFloor(GameTestHelper c) {
		ServerPlayer p = fighter(c);
		c.assertTrue(Purgatory.startSolo(p) == null, "solo starts");
		PurgatorySavedData.Run r = data(c).runOf(id(p));
		Dungeon d = PurgatoryFloor.dungeon(r);
		var level = plevel(c);
		c.assertTrue(p.gameMode.getGameModeForPlayer() == GameType.ADVENTURE, "adventure mode");
		c.assertTrue(d.roomAt(p.getBlockX() - DungeonBuilder.origin(r.slot).getX(),
			p.getBlockY() - DungeonBuilder.origin(r.slot).getY(), p.getBlockZ() - DungeonBuilder.origin(r.slot).getZ()) == d.start(), "in the start room");
		c.assertTrue(p.position().distanceTo(Purgatory.start(r)) < 0.01, "at the start spot");
		var o = DungeonBuilder.origin(r.slot);
		var pos = new net.minecraft.core.BlockPos.MutableBlockPos();
		for (Room room : d.rooms()) {
			for (int x = room.x0(); x < room.x1(); x++) {
				for (int y = room.y0(); y < room.y1(); y++) {
					for (int z = room.z0(); z < room.z1(); z++) {
						var want = DungeonBuilder.state(d.grid(), x, y, z).getBlock();
						var got = level.getBlockState(pos.set(o.getX() + x, o.getY() + y, o.getZ() + z)).getBlock();
						if (want != got) {
							c.fail("block " + x + " " + y + " " + z + " of " + room + ": want " + want + " got " + got);
						}
					}
				}
			}
		}
		Purgatory.finish(p, false);
		for (Room room : d.rooms()) {
			for (int x = room.x0(); x < room.x1(); x += 3) {
				for (int z = room.z0(); z < room.z1(); z += 3) {
					c.assertTrue(level.getBlockState(pos.set(o.getX() + x, o.getY() + room.y0(), o.getZ() + z)).isAir(), "cleared");
				}
			}
		}
		GameTestKit.drop(c, p);
		c.succeed();
	}

	/** P-082: walking into a locked room seals its doors and spawns its mobs; they die, the doors open. */
	@GameTest(maxTicks = 80)
	public void aLockedRoomSealsUntilItsMobsDie(GameTestHelper c) {
		ServerPlayer p = fighter(c);
		p.getAbilities().invulnerable = true;
		c.assertTrue(Purgatory.startSolo(p) == null, "solo starts");
		PurgatorySavedData.Run r = data(c).runOf(id(p));
		Room room = first(PurgatoryFloor.dungeon(r), RoomKind.LOCKED);
		go(c, p, r, room.centre);
		c.runAfterDelay(3, () -> {
			var level = plevel(c);
			c.assertTrue(r.room(room.index) == PurgatorySavedData.Run.SEALED, "sealed");
			for (int[] g : room.gates) {
				c.assertTrue(level.getBlockState(DungeonBuilder.world(r.slot, g)).is(Blocks.IRON_BARS), "bars at the doors");
			}
			var mobs = level.getEntitiesOfClass(Mob.class, Purgatory.inside(r.slot), m -> m.entityTags().contains(Purgatory.TAG));
			c.assertTrue(!mobs.isEmpty(), "mobs came");
			mobs.forEach(m -> m.kill(level));
			c.runAfterDelay(15, () -> {
				c.assertTrue(r.room(room.index) == PurgatorySavedData.Run.DONE, "open again");
				for (int[] g : room.gates) {
					c.assertTrue(level.getBlockState(DungeonBuilder.world(r.slot, g)).isAir(), "doors open");
				}
				Purgatory.finish(p, false);
				GameTestKit.drop(c, p);
				c.succeed();
			});
		});
	}

	/** P-082: the boss waits in the last room; killing it wins the floor. */
	@GameTest(maxTicks = 80)
	public void killingTheBossWinsTheFloor(GameTestHelper c) {
		ServerPlayer p = fighter(c);
		p.getAbilities().invulnerable = true;
		c.assertTrue(Purgatory.startSolo(p) == null, "solo starts");
		PurgatorySavedData.Run r = data(c).runOf(id(p));
		Room boss = PurgatoryFloor.dungeon(r).boss();
		go(c, p, r, boss.centre);
		c.runAfterDelay(3, () -> {
			var level = plevel(c);
			var bosses = level.getEntitiesOfClass(Mob.class, Purgatory.inside(r.slot), m -> m.entityTags().contains(PurgatoryFloor.BOSS_TAG));
			c.assertTrue(bosses.size() == 1, "one boss: " + bosses.size());
			Mob b = bosses.get(0);
			c.assertTrue(b.getMaxHealth() > 40, "a strong boss: " + b.getMaxHealth());
			c.assertTrue(r.ticksLeft < PurgatoryRules.RUN_TICKS, "the clock runs");
			b.kill(level);
			c.runAfterDelay(3, () -> {
				c.assertTrue(data(c).runOf(id(p)) == null, "the run ended");
				c.assertTrue(data(c).solo(id(p)).floor == 2, "won: floor 2");
				c.assertTrue(p.experienceLevel == PurgatoryRules.rewardLevels(1), "paid");
				c.assertTrue(level.getEntitiesOfClass(Mob.class, Purgatory.inside(r.slot)).isEmpty(), "dungeon emptied");
				GameTestKit.drop(c, p);
				c.succeed();
			});
		});
	}

	/** D255: running out of time loses the floor (and costs nothing). */
	@GameTest(maxTicks = 40)
	public void runningOutOfTimeLoses(GameTestHelper c) {
		ServerPlayer p = fighter(c);
		c.assertTrue(Purgatory.startSolo(p) == null, "solo starts");
		data(c).runOf(id(p)).ticksLeft = 1;
		c.runAfterDelay(3, () -> {
			c.assertTrue(data(c).runOf(id(p)) == null, "the run ended");
			c.assertTrue(data(c).solo(id(p)).floor == 1 && runes(p) == 0, "lost, nothing paid");
			c.assertTrue(p.gameMode.getGameModeForPlayer() == GameType.SURVIVAL, "survival again");
			GameTestKit.drop(c, p);
			c.succeed();
		});
	}
}
