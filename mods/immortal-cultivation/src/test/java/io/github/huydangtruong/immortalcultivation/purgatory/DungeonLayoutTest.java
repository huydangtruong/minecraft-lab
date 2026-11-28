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
				int n = d.rooms().size() - 1;
				assertTrue(n >= DungeonRules.rooms(floor) + 1 && n <= DungeonRules.rooms(floor) + DungeonRules.EXTRA_ROOMS_MAX, "rooms " + n);
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

	/** D259: every face of every room touches another room or bedrock skin, never the void; the skin is never inside a room. */
	@Test
	void roomsAreWrappedInBedrock() {
		int[][] faces = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
		for (int floor : new int[] {1, 6, 30}) {
			for (long seed = 1; seed <= 10; seed++) {
				Dungeon d = Layout.generate(seed, floor);
				Set<Long> skin = new HashSet<>();
				for (int[] k : d.skin()) {
					assertFalse(d.built(k[0], k[1], k[2]), "skin inside a room");
					skin.add(key(k[0], k[1], k[2]));
				}
				for (Room r : d.rooms()) {
					for (int x = r.x0(); x < r.x1(); x++) {
						for (int y = r.y0(); y < r.y1(); y++) {
							for (int z = r.z0(); z < r.z1(); z++) {
								for (int[] f : faces) {
									int nx = x + f[0], ny = y + f[1], nz = z + f[2];
									assertTrue(d.built(nx, ny, nz) || skin.contains(key(nx, ny, nz)),
										"open to the void at " + nx + " " + ny + " " + nz + " floor " + floor + " seed " + seed);
								}
							}
						}
					}
				}
			}
		}
	}

	private static long key(int x, int y, int z) {
		return ((long) (x + 2) << 40) | ((long) (y + 2) << 20) | (z + 2);
	}

	/** D259: longer floors, more mobs, two waves in locked rooms, about half the plain rooms locked. */
	@Test
	void floorsAreLongerAndFuller() {
		assertEquals(10, DungeonRules.rooms(1));
		assertEquals(10, DungeonRules.rooms(2));
		assertEquals(20, DungeonRules.rooms(99));
		assertEquals(5, DungeonRules.wave(RoomKind.LOCKED, 1, 1));
		assertEquals(2, DungeonRules.waves(RoomKind.LOCKED));
		assertEquals(1, DungeonRules.waves(RoomKind.OPEN));
		assertEquals(4, DungeonRules.wave(RoomKind.CORRIDOR, 1, 1)); // D261: was 2
		assertEquals(5, DungeonRules.wave(RoomKind.OPEN, 1, 1));
		assertEquals(3, DungeonRules.wave(RoomKind.STAIRS, 1, 1)); // D274
		assertEquals(5, DungeonRules.wave(RoomKind.BOSS, 1, 1));
		assertEquals(11, DungeonRules.wave(RoomKind.BOSS, 99, 1));
		assertEquals(9, DungeonRules.wave(RoomKind.LOCKED, 1, 2)); // D273: 5 x 1.8
		assertEquals(13, DungeonRules.wave(RoomKind.LOCKED, 1, 3)); // 5 x 2.6
		assertEquals(24, DungeonRules.wave(RoomKind.TREASURE, 1, 4)); // 7 x 3.4
		for (long seed = 1; seed <= 30; seed++) {
			Dungeon d = Layout.generate(seed, 1);
			long locked = d.rooms().stream().filter(r -> r.kind == RoomKind.LOCKED || r.kind == RoomKind.PILLAR).count(); // D265: pillar rooms seal too
			assertTrue(locked >= 3, "locked rooms " + locked + " seed " + seed);
		}
	}

	/** D262: floors are 1 to 6 rooms longer than the standard and most rooms are a plain way through, with few forks. */
	@Test
	void mostRoomsHaveTwoDoorsAndLengthsVary() {
		Set<Integer> lengths = new HashSet<>();
		int rooms = 0;
		int forks = 0;
		for (int floor : new int[] {1, 2, 6, 15}) {
			for (long seed = 1; seed <= 40; seed++) {
				Dungeon d = Layout.generate(seed, floor);
				lengths.add(d.rooms().size() - 1 - DungeonRules.rooms(floor));
				for (Room r : d.rooms()) {
					if (r.kind != RoomKind.BOSS && r.kind != RoomKind.START) {
						rooms++;
						forks += r.ports.size() >= 3 ? 1 : 0;
					}
				}
			}
		}
		assertTrue(lengths.contains(1) && lengths.contains(DungeonRules.EXTRA_ROOMS_MAX) && !lengths.contains(0), "lengths " + lengths);
		assertTrue(forks * 4 < rooms, "forks " + forks + " of " + rooms);
	}

	/** D263: about half the rooms have a wooden gift chest, on a reachable spot; the floors still pass the stuck check. */
	@Test
	void aboutHalfTheRoomsHaveAGift() {
		int rooms = 0;
		int gifts = 0;
		for (int floor : FLOORS) {
			for (long seed = 0; seed < SEEDS; seed++) {
				Dungeon d = Layout.generate(seed * 131 + floor, floor);
				for (Room r : d.rooms()) {
					if (r.kind != RoomKind.START && r.kind != RoomKind.BOSS && r.kind != RoomKind.TREASURE) {
						rooms++;
						gifts += r.gifts.size();
						for (int[] g : r.gifts) {
							assertEquals(Mat.CHEST, d.grid().mat(g[0], g[1], g[2]));
						}
					}
				}
			}
		}
		System.out.println("gifts " + gifts + " of " + rooms);
		assertTrue(gifts > rooms * 0.4 && gifts < rooms * 0.6, "gifts " + gifts + " of " + rooms);
	}

	/** D265: pillar rooms show up, and their Seal Pillar stands on a spot a player can reach. */
	@Test
	void sealPillarsCanBeReached() {
		int pillars = 0;
		int rooms = 0;
		for (int floor : FLOORS) {
			for (long seed = 0; seed < SEEDS; seed++) {
				Dungeon d = Layout.generate(seed * 17 + floor, floor);
				Walker.Result res = Walker.check(d.grid(), d.rooms(), d.start().centre, d.boss());
				for (Room r : d.rooms()) {
					rooms++;
					if (r.kind == RoomKind.PILLAR) {
						pillars++;
						assertNotNull(r.pillar, r.toString());
						assertTrue(res.reachable(r.pillar[0], r.pillar[1], r.pillar[2]), r + " pillar " + java.util.Arrays.toString(r.pillar));
					}
				}
			}
		}
		System.out.println("pillar rooms " + pillars + " of " + rooms);
		assertTrue(pillars * 12 > rooms, "pillar rooms " + pillars + " of " + rooms);
	}

	/** D269: skills break the inside of a module, never its shell, a staircase or pillar room, or the skin. */
	@Test
	void skillsBreakTheInsideOnly() {
		Dungeon d = Layout.generate(7, 6);
		for (Room r : d.rooms()) {
			boolean whole = r.kind == RoomKind.STAIRS || r.kind == RoomKind.PILLAR;
			assertTrue(d.guarded(r.x0(), r.y0() + 2, r.z0() + 5), r + " wall");
			assertTrue(d.guarded(r.x0() + 5, r.y0(), r.z0() + 5), r + " floor");
			assertTrue(d.guarded(r.x0() + 5, r.y1() - 1, r.z0() + 5), r + " ceiling");
			for (int[] g : r.gates) {
				assertTrue(d.guarded(g[0], g[1], g[2]), r + " gate");
			}
			assertEquals(whole, d.guarded(r.x0() + 7, r.y0() + 3, r.z0() + 7), r + " inside");
		}
		assertTrue(d.guarded(-1, 0, 0), "the skin");
	}
}

