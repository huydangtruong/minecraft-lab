package io.github.huydangtruong.immortalcultivation.purgatory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.huydangtruong.immortalcultivation.purgatory.dungeon.BossRooms;
import io.github.huydangtruong.immortalcultivation.purgatory.dungeon.Dungeon;
import io.github.huydangtruong.immortalcultivation.purgatory.dungeon.DungeonRules;
import io.github.huydangtruong.immortalcultivation.purgatory.dungeon.Layout;
import io.github.huydangtruong.immortalcultivation.purgatory.dungeon.Mat;
import io.github.huydangtruong.immortalcultivation.purgatory.dungeon.Modules;
import io.github.huydangtruong.immortalcultivation.purgatory.dungeon.Room;
import io.github.huydangtruong.immortalcultivation.purgatory.dungeon.RoomKind;
import io.github.huydangtruong.immortalcultivation.purgatory.dungeon.Walker;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** P-082: every floor is connected, the boss room is the end, and no player can get stuck for good. */
class DungeonLayoutTest {
	private static final int[] FLOORS = {1, 2, 4, 5, 6, 9, 10, 15, 20, 30, 50};
	private static final int SEEDS = 25;

	@Test
	void everyFloorPassesTheStuckCheck() {
		int attempts = 0;
		int runs = 0;
		for (int floor : FLOORS) {
			for (long seed = 0; seed < SEEDS; seed++) {
				Dungeon d = Layout.generate(seed * 7919 + floor, floor);
				Walker.Result r = Walker.check(d.grid(), d.rooms(), d.start().centre, d.boss());
				assertTrue(r.ok(), "floor " + floor + " seed " + seed + ": " + r.problem());
				attempts += d.attempts();
				runs++;
			}
		}
		assertTrue(attempts < runs * 3, "most seeds work on the first tries: " + attempts + " attempts for " + runs);
	}

	@Test
	void theBossRoomIsTheEnd() {
		for (int floor : FLOORS) {
			for (long seed = 0; seed < SEEDS; seed++) {
				Dungeon d = Layout.generate(seed, floor);
				Room boss = d.boss();
				assertEquals(RoomKind.BOSS, boss.kind);
				assertEquals(1, boss.ports.size(), "one door into the boss room");
				assertNotNull(boss.boss, "a boss spot");
				assertEquals(DungeonRules.layers(floor) - 1, boss.layer, "on the top layer");
				assertEquals(RoomKind.START, d.start().kind);
				assertTrue(d.rooms().size() == DungeonRules.rooms(floor) + 1, "rooms " + d.rooms().size());
				for (Room r : d.rooms()) {
					assertFalse(r.ports.isEmpty(), r + " has a door");
					assertNotNull(r.centre, r + " has a centre");
					if (r.kind.locks) {
						assertFalse(r.gates.isEmpty(), r + " has gates");
					}
				}
			}
		}
	}

	@Test
	void twoLayersFromFloorFiveJoinedByStairs() {
		Dungeon one = Layout.generate(3, 4);
		assertTrue(one.rooms().stream().allMatch(r -> r.layer == 0));
		for (long seed = 0; seed < SEEDS; seed++) {
			Dungeon d = Layout.generate(seed, 5);
			assertEquals(1, d.rooms().stream().filter(r -> r.module.equals(Modules.STAIR_UP.id())).count());
			assertEquals(1, d.rooms().stream().filter(r -> r.module.equals(Modules.STAIR_DOWN.id())).count());
			assertTrue(d.rooms().stream().anyMatch(r -> r.layer == 1));
		}
	}

	@Test
	void manyModulesAndBossRoomsShowUp() {
		Set<String> modules = new HashSet<>();
		Set<String> bosses = new HashSet<>();
		for (int floor : FLOORS) {
			for (long seed = 0; seed < SEEDS; seed++) {
				Dungeon d = Layout.generate(seed * 31 + floor, floor);
				d.rooms().forEach(r -> modules.add(r.module));
				bosses.add(d.boss().module);
			}
		}
		for (Modules m : Modules.values()) {
			assertTrue(modules.contains(m.id()), "module used: " + m.id());
		}
		for (BossRooms b : BossRooms.values()) {
			assertTrue(bosses.contains(b.id()), "boss room used: " + b.id());
		}
	}

	@Test
	void sameSeedSameFloor() {
		Dungeon a = Layout.generate(42, 12);
		Dungeon b = Layout.generate(42, 12);
		assertEquals(a.rooms().toString(), b.rooms().toString());
		for (int x = 0; x < DungeonRules.SIZE_X; x++) {
			for (int y = 0; y < DungeonRules.SIZE_Y; y++) {
				for (int z = 0; z < DungeonRules.SIZE_Z; z++) {
					assertEquals(a.grid().mat(x, y, z), b.grid().mat(x, y, z));
				}
			}
		}
	}

	@Test
	void theCheckerCatchesASealedRoom() {
		Dungeon d = Layout.generate(7, 3);
		Room start = d.start();
		// wall the start room in: every doorway becomes solid
		for (int x = start.x0(); x < start.x1(); x++) {
			for (int z = start.z0(); z < start.z1(); z++) {
				for (int y = start.y0() + 1; y < start.y1() - 1; y++) {
					boolean edge = x == start.x0() || z == start.z0() || x == start.x1() - 1 || z == start.z1() - 1;
					if (edge) {
						d.grid().set(x, y, z, Mat.WALL, d.grid().style(x, y, z), d.grid().facing(x, y, z));
					}
				}
			}
		}
		assertFalse(Walker.check(d.grid(), d.rooms(), start.centre, d.boss()).ok());
	}

	@Test
	void theCheckerCatchesAPit() {
		Dungeon d = Layout.generate(11, 2);
		Room start = d.start();
		int[] c = start.centre;
		// a 3-deep pit right where the player stands
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				for (int y = c[1] - 1; y >= c[1] - 1; y--) {
					d.grid().set(c[0] + dx, y, c[2] + dz, Mat.AIR, d.grid().style(0, 0, 0), d.grid().facing(0, 0, 0));
				}
			}
		}
		// the floor of a layer-0 room is the bottom of the grid: make the pit 3 deep by raising walls around it instead
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
					for (int y = c[1] - 1; y <= c[1] + 1; y++) {
						d.grid().set(c[0] + dx, y, c[2] + dz, Mat.WALL, d.grid().style(0, 0, 0), d.grid().facing(0, 0, 0));
					}
				}
			}
		}
		Walker.Result r = Walker.check(d.grid(), d.rooms(), new int[] {c[0], c[1] - 1, c[2]}, d.boss());
		assertFalse(r.ok(), "a pit with 2-high walls is a trap");
	}
}
