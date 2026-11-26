package io.github.huydangtruong.immortalcultivation;

import io.github.huydangtruong.immortalcultivation.purgatory.Purgatory;
import io.github.huydangtruong.immortalcultivation.purgatory.DungeonBuilder;
import io.github.huydangtruong.immortalcultivation.purgatory.PurgatoryFloor;
import io.github.huydangtruong.immortalcultivation.purgatory.PurgatoryRules;
import io.github.huydangtruong.immortalcultivation.purgatory.dungeon.Dungeon;
import io.github.huydangtruong.immortalcultivation.purgatory.dungeon.Layout;
import io.github.huydangtruong.immortalcultivation.purgatory.dungeon.Room;
import io.github.huydangtruong.immortalcultivation.purgatory.dungeon.RoomKind;
import io.github.huydangtruong.immortalcultivation.purgatory.PurgatorySavedData;
import io.github.huydangtruong.immortalcultivation.rune.Runes;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
import io.github.huydangtruong.immortalcultivation.purgatory.PurgatoryNet;
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
			n += inStack(p.getInventory().getItem(i));
		}
		return n;
	}

	/** Runes in a stack: a rune, or the runes inside a reward bag (D261). */
	private static int inStack(net.minecraft.world.item.ItemStack s) {
		if (s.is(Runes.ITEM)) {
			return s.getCount();
		}
		var c = s.get(net.minecraft.core.component.DataComponents.CONTAINER);
		return c == null ? 0 : (int) c.nonEmptyItemCopyStream().filter(x -> x.is(Runes.ITEM)).count();
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
		var skin = d.skin();
		c.assertTrue(!skin.isEmpty(), "a bedrock skin");
		for (int[] k : skin) {
			if (!level.getBlockState(pos.set(o.getX() + k[0], o.getY() + k[1], o.getZ() + k[2])).is(Blocks.BEDROCK)) {
				c.fail("no bedrock at " + k[0] + " " + k[1] + " " + k[2]);
			}
		}
		Room start = d.start();
		var wall = new net.minecraft.core.BlockPos(o.getX() + start.x0(), o.getY() + start.y0() + 2, o.getZ() + start.z0() + 5);
		c.assertTrue(Purgatory.guarded(level, wall), "skills leave the dungeon alone");
		c.assertTrue(!Purgatory.guarded(level, wall.offset(0, 0, 500)), "but not the world around it");
		Purgatory.finish(p, false);
		for (int[] k : skin) {
			if (!level.getBlockState(pos.set(o.getX() + k[0], o.getY() + k[1], o.getZ() + k[2])).isAir()) {
				c.fail("skin left at " + k[0] + " " + k[1] + " " + k[2]);
			}
		}
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

	/** P-082, D259: walking into a locked room seals its doors and spawns its mobs; two waves die, the doors open. */
	@GameTest(maxTicks = 120)
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
				c.assertTrue(r.room(room.index) == PurgatorySavedData.Run.SEALED_LAST, "a second wave");
				var second = level.getEntitiesOfClass(Mob.class, Purgatory.inside(r.slot), m -> m.entityTags().contains(Purgatory.TAG));
				c.assertTrue(!second.isEmpty(), "second wave mobs came");
				second.forEach(m -> m.kill(level));
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
		});
	}

	/** D265: a pillar room seals until its Seal Pillar (30 hearts, still, no drops) is destroyed; killing mobs does not open it. */
	@GameTest(maxTicks = 80)
	public void aSealPillarRoomOpensWhenThePillarBreaks(GameTestHelper c) {
		ServerPlayer p = fighter(c);
		p.getAbilities().invulnerable = true;
		long seed = 1;
		while (Layout.generate(seed, 1).rooms().stream().noneMatch(x -> x.kind == RoomKind.PILLAR)) {
			seed++;
		}
		Purgatory.nextSeedForTests = seed;
		c.assertTrue(Purgatory.startSolo(p) == null, "solo starts");
		PurgatorySavedData.Run r = data(c).runOf(id(p));
		c.assertTrue(r.seed == seed && r.floor == 1, "the chosen floor");
		Room room = PurgatoryFloor.dungeon(r).rooms().stream().filter(x -> x.kind == RoomKind.PILLAR).findFirst().orElseThrow();
		PurgatorySavedData.Run run = r;
		Room pr = room;
		go(c, p, run, pr.centre);
		c.runAfterDelay(3, () -> {
			var level = plevel(c);
			c.assertTrue(run.room(pr.index) == PurgatorySavedData.Run.SEALED, "sealed");
			var pillars = level.getEntitiesOfClass(Mob.class, Purgatory.inside(run.slot), m -> m.entityTags().contains(PurgatoryFloor.PILLAR_TAG));
			c.assertTrue(pillars.size() == 1, "one pillar: " + pillars.size());
			Mob pillar = pillars.get(0);
			c.assertTrue(Math.abs(pillar.getMaxHealth() - 60) < 0.01, "30 hearts: " + pillar.getMaxHealth());
			c.assertTrue(pillar.isNoAi(), "the pillar stands still");
			c.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.Display.BlockDisplay.class, Purgatory.inside(run.slot),
				e -> e.entityTags().contains(PurgatoryFloor.PILLAR_TAG)).size() == 2, "two blocks tall");
			level.getEntitiesOfClass(Mob.class, Purgatory.inside(run.slot), m -> m.entityTags().contains(Purgatory.TAG)).forEach(m -> m.kill(level));
			c.runAfterDelay(15, () -> {
				c.assertTrue(run.room(pr.index) == PurgatorySavedData.Run.SEALED, "killing the mobs does not open it");
				net.minecraft.world.phys.AABB near = pillar.getBoundingBox().inflate(1.5);
				level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, near).forEach(e -> e.discard()); // mob drops
				pillar.hurtServer(level, level.damageSources().playerAttack(p), 1000f);
				c.runAfterDelay(15, () -> {
					c.assertTrue(run.room(pr.index) == PurgatorySavedData.Run.DONE, "open after the pillar breaks");
					for (int[] g : pr.gates) {
						c.assertTrue(level.getBlockState(DungeonBuilder.world(run.slot, g)).isAir(), "doors open");
					}
					c.assertTrue(level.getEntities((net.minecraft.world.entity.Entity) null, Purgatory.inside(run.slot),
						e -> e.entityTags().contains(PurgatoryFloor.PILLAR_TAG)).isEmpty(), "the pillar is gone");
					c.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, near).isEmpty(),
						"no drops");
					Purgatory.finish(p, false);
					GameTestKit.drop(c, p);
					c.succeed();
				});
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
			c.assertTrue(Math.abs(b.getScale() - 2.0) < 0.01, "D261: twice the size: " + b.getScale());
			c.assertTrue(r.ticksLeft < PurgatoryRules.RUN_TICKS, "the clock runs");
			b.kill(level);
			c.runAfterDelay(3, () -> {
				c.assertTrue(data(c).runOf(id(p)) == r && r.won(), "D260: the winner stays a while");
				c.assertTrue(data(c).solo(id(p)).floor == 2, "won: floor 2");
				c.assertTrue(p.experienceLevel == PurgatoryRules.rewardLevels(1), "paid");
				c.assertTrue(runes(p) == PurgatoryRules.rewardRunes(1, 1), "D261: the runes, in a bag: " + runes(p));
				c.assertTrue(level.getEntitiesOfClass(Mob.class, Purgatory.inside(r.slot)).isEmpty(), "the mobs are gone");
				c.assertTrue(PurgatoryNet.stateFor(c.getLevel().getServer(), data(c), p).mode() == PurgatoryNet.MODE_CLEARED, "shown as cleared");
				int ticks = r.victoryTicks;
				c.runAfterDelay(5, () -> {
					c.assertTrue(r.victoryTicks < ticks && r.ticksLeft > 0, "the 30 s count down");
					c.assertTrue(PurgatoryNet.apply(p, PurgatoryNet.COME_BACK, 0) == null, "Come back now");
					c.assertTrue(data(c).runOf(id(p)) == null, "the run closed");
					c.assertTrue(p.gameMode.getGameModeForPlayer() == GameType.SURVIVAL, "back home");
					c.assertTrue(data(c).solo(id(p)).floor == 2 && p.experienceLevel == PurgatoryRules.rewardLevels(1), "paid once");
					GameTestKit.drop(c, p);
					c.succeed();
				});
			});
		});
	}

	/** D260: without Come back now the winners go back when the 30 seconds are up. */
	@GameTest(maxTicks = 40)
	public void winnersGoBackAfterTheCountdown(GameTestHelper c) {
		ServerPlayer p = fighter(c);
		c.assertTrue(Purgatory.startSolo(p) == null, "solo starts");
		PurgatorySavedData.Run r = data(c).runOf(id(p));
		r.boss = PurgatorySavedData.Run.BOSS_DEAD;
		c.runAfterDelay(2, () -> {
			c.assertTrue(r.won(), "won");
			r.victoryTicks = 2;
			c.runAfterDelay(4, () -> {
				c.assertTrue(data(c).runOf(id(p)) == null, "the run closed");
				c.assertTrue(data(c).solo(id(p)).floor == 2, "won once: floor 2");
				c.assertTrue(p.gameMode.getGameModeForPlayer() == GameType.SURVIVAL, "back home");
				GameTestKit.drop(c, p);
				c.succeed();
			});
		});
	}

	/** D261: a full inventory: the rune bag waits on the dungeon floor and lands at the winner's feet back home. */
	@GameTest(maxTicks = 40)
	public void aBagLeftBehindFollowsTheWinnerHome(GameTestHelper c) {
		ServerPlayer p = fighter(c);
		c.assertTrue(Purgatory.startSolo(p) == null, "solo starts");
		for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
			p.getInventory().setItem(i, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIRT, 64));
		}
		PurgatorySavedData.Run r = data(c).runOf(id(p));
		r.boss = PurgatorySavedData.Run.BOSS_DEAD;
		c.runAfterDelay(2, () -> {
			var level = plevel(c);
			var bags = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, Purgatory.inside(r.slot), e -> inStack(e.getItem()) > 0);
			c.assertTrue(bags.size() == 1 && inStack(bags.get(0).getItem()) == PurgatoryRules.rewardRunes(1, 1), "one bag on the floor: " + bags.size());
			p.teleportTo(p.getX() + 3, p.getY(), p.getZ()); // walked away from it
			c.assertTrue(PurgatoryNet.apply(p, PurgatoryNet.COME_BACK, 0) == null, "Come back now");
			var home = p.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, p.getBoundingBox().inflate(2), e -> inStack(e.getItem()) > 0);
			c.assertTrue(home.size() == 1, "the bag is at their feet at home");
			c.assertTrue(!Purgatory.inside(r.slot).contains(home.get(0).position()), "outside the dungeon");
			home.forEach(net.minecraft.world.entity.Entity::discard);
			GameTestKit.drop(c, p);
			c.succeed();
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
