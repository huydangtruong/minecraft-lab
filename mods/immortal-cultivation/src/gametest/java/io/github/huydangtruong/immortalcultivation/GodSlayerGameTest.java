package io.github.huydangtruong.immortalcultivation;

import io.github.huydangtruong.immortalcultivation.godslayer.GodSlayer;
import io.github.huydangtruong.immortalcultivation.godslayer.GodSlayerHunt;
import io.github.huydangtruong.immortalcultivation.godslayer.GodSlayerRules;
import io.github.huydangtruong.immortalcultivation.godslayer.GodSlayerSavedData;
import io.github.huydangtruong.immortalcultivation.reward.RewardBoxes;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * P-032: the God Slayer. Everything that touches the global hunt state (saved data, the boss bar) is in ONE sequential
 * test, because game tests share one server and may run side by side.
 */
public class GodSlayerGameTest {
	private static GodSlayer boss(GameTestHelper c, double x, double y, double z, int level) {
		GodSlayer b = GodSlayer.TYPE.create(c.getLevel(), EntitySpawnReason.MOB_SUMMONED);
		b.setBossLevel(level);
		Vec3 v = c.absoluteVec(new Vec3(x, y, z));
		b.snapTo(v.x, v.y, v.z, 0f, 0f);
		c.getLevel().addFreshEntity(b);
		return b;
	}

	private static ServerPlayer strong(GameTestHelper c) {
		ServerPlayer p = GameTestKit.survivor(c);
		p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100000);
		p.setHealth(100000);
		return p;
	}

	private static void reset(MinecraftServer s, int level) {
		GodSlayerHunt.giveUp(s);
		GodSlayerSavedData d = GodSlayerHunt.data(s);
		d.clearHunt();
		d.level = level;
		d.treasuresSince = 0;
		d.setDirty();
	}

	// ---- health, damage rules ----

	@GameTest(maxTicks = 40)
	public void startsWith500HealthAtLevel1(GameTestHelper c) {
		GodSlayer b = boss(c, 5.5, 1, 5.5, 1);
		c.assertTrue(b.getMaxHealth() == 500f && b.getHealth() == 500f, "Lv 1 = 500 health, was " + b.getMaxHealth());
		GodSlayer b10 = boss(c, 8.5, 1, 8.5, 10);
		c.assertTrue(boss(c, 2.5, 1, 8.5, 3).getMaxHealth() == 700f, "Lv 3 = 700 health");
		// D98: 5 blocks tall (hitbox 4.8) at every level, strength is only numbers
		c.assertTrue(Math.abs(b.getBbHeight() - 4.8f) < 0.01f && Math.abs(b10.getBbHeight() - 4.8f) < 0.01f,
			"hitbox 4.8 tall at every level, was " + b.getBbHeight() + " and " + b10.getBbHeight());
		c.succeed();
	}

	@GameTest(maxTicks = 40)
	public void onlyPlayersHurtItAndAHitIsClampedTo40(GameTestHelper c) {
		GameTestKit.floor(c);
		GodSlayer b = boss(c, 5.5, 1, 5.5, 1);
		ServerLevel level = c.getLevel();
		Husk husk = c.spawn(EntityTypes.HUSK, new Vec3(1.5, 1, 1.5));
		b.hurtServer(level, level.damageSources().generic(), 1000f);
		b.hurtServer(level, level.damageSources().magic(), 1000f);
		b.hurtServer(level, level.damageSources().mobAttack(husk), 1000f);
		b.hurtServer(level, level.damageSources().explosion(null, null), 1000f);
		c.assertTrue(b.getHealth() == 500f, "no damage from non-players, was " + b.getHealth());
		ServerPlayer p = c.makeMockServerPlayerInLevel();
		b.hurtServer(level, level.damageSources().playerAttack(p), 1000f);
		c.assertTrue(b.getHealth() == 460f, "a player's 1000 hit takes exactly 40, health " + b.getHealth());
		c.assertTrue(b.fireImmune() && !b.canBeLeashed(), "fire immune, cannot be leashed");
		c.succeed();
	}

	// ---- slash, teleport ----

	@GameTest(maxTicks = 60)
	public void slashHitsAPlayerFor30AtLevel1(GameTestHelper c) {
		GameTestKit.enable(c);
		GameTestKit.floor(c);
		c.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true);
		ServerPlayer p = strong(c);
		GameTestKit.place(c, p, 5.5, 1, 5.5, 0f, 0f);
		p.setHealth(1000f);
		p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
		p.setHealth(1000f);
		GodSlayer b = boss(c, 5.5, 1, 7.5, 1);
		b.setHuntTarget(p.getUUID());
		c.startSequence()
			.thenWaitUntil(() -> c.assertTrue(p.getHealth() < 1000f, "the slash landed"))
			.thenExecute(() -> {
				c.assertTrue(Math.abs(p.getHealth() - 970f) < 0.01f, "one slash = 30 damage, health " + p.getHealth());
				GameTestKit.drop(c, p);
			})
			.thenSucceed();
	}

	@GameTest(maxTicks = 100)
	public void teleportsNextToATargetThatIsFarAway(GameTestHelper c) {
		GameTestKit.enable(c);
		GameTestKit.floor(c);
		ServerPlayer p = strong(c);
		GameTestKit.place(c, p, 5.5, 1, 5.5, 0f, 0f);
		GodSlayer b = boss(c, 1.5, 1, 1.5, 1);
		b.setHuntTarget(p.getUUID());
		c.runAfterDelay(2, () -> {
			// 70 blocks away: outside the 64 block limit (D100) (a snap, not a walk)
			Vec3 far = p.position().add(70, 0, 0);
			p.teleportTo(c.getLevel(), far.x, far.y, far.z, java.util.Set.of(), 0f, 0f, false);
		});
		c.startSequence()
			.thenIdle(4)
			.thenWaitUntil(() -> c.assertTrue(b.distanceTo(p) < 12, "the boss teleported near its target, distance " + b.distanceTo(p)))
			.thenExecute(() -> GameTestKit.drop(c, p))
			.thenSucceed();
	}

	/** D100: a target 25 blocks away on open ground is chased on foot, never teleported to. */
	@GameTest(maxTicks = 400)
	public void frozenBossStandsStillThenActsAgain(GameTestHelper c) {
		GameTestKit.enable(c);
		ServerPlayer p = strong(c);
		GodSlayer b = boss(c, 5.5, 1, 5.5, 1);
		Vec3 pv = c.absoluteVec(new Vec3(5.5, 1, 8.0));
		p.teleportTo(c.getLevel(), pv.x, pv.y, pv.z, java.util.Set.of(), 0f, 0f, false);
		b.setHuntTarget(p.getUUID());
		b.setFreezePose(GodSlayer.FREEZE_STAND); // what /boss freeze stand does (D104)
		float[] hp = {0};
		Vec3[] at = {Vec3.ZERO};
		float[] yaw = {0};
		c.startSequence()
			.thenIdle(5)
			.thenExecute(() -> {
				hp[0] = p.getHealth();
				at[0] = b.position();
				yaw[0] = b.getYRot();
			})
			.thenIdle(60) // 3 seconds
			.thenExecute(() -> {
				c.assertTrue(b.isFrozen() && b.freezePose() == GodSlayer.FREEZE_STAND, "still frozen");
				c.assertTrue(p.getHealth() == hp[0], "no attack while frozen, health " + hp[0] + " -> " + p.getHealth());
				double moved = Math.hypot(b.getX() - at[0].x, b.getZ() - at[0].z);
				c.assertTrue(moved < 0.05, "no movement while frozen, moved " + moved);
				c.assertTrue(Math.abs(b.getYRot() - yaw[0]) < 0.5f, "keeps its facing");
				b.setFreezePose(GodSlayer.FREEZE_NONE);
			})
			.thenWaitUntil(() -> c.assertTrue(p.getHealth() < hp[0] || b.distanceTo(p) < 3.0, "acts again after freeze off"))
			.thenExecute(() -> {
				b.discard();
				GameTestKit.drop(c, p);
			})
			.thenSucceed();
	}

	@GameTest(maxTicks = 200)
	public void runsAfterANearTargetInsteadOfTeleporting(GameTestHelper c) {
		GameTestKit.enable(c);
		ServerLevel level = c.getLevel();
		// far from the other tests' structures (they share the world and count blocks)
		net.minecraft.core.BlockPos o = c.absolutePos(new net.minecraft.core.BlockPos(0, 0, 0)).offset(4000, 0, 0);
		for (int cx = -1; cx <= 3; cx++) {
			for (int cz = -1; cz <= 1; cz++) {
				level.setChunkForced((o.getX() >> 4) + cx, (o.getZ() >> 4) + cz, true);
			}
		}
		for (int x = -10; x < 50; x++) {
			for (int z = -10; z < 30; z++) {
				level.setBlock(o.offset(x, 0, z), Blocks.STONE.defaultBlockState(), 3);
				for (int y = 1; y <= 6; y++) {
					level.setBlock(o.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
				}
			}
		}
		ServerPlayer p = strong(c);
		p.teleportTo(level, o.getX() + 2.5, o.getY() + 1, o.getZ() + 5.5, java.util.Set.of(), 0f, 0f, false);
		GodSlayer b = GodSlayer.TYPE.create(level, EntitySpawnReason.MOB_SUMMONED);
		b.setBossLevel(1);
		b.snapTo(o.getX() + 1.5, o.getY() + 1, o.getZ() + 1.5, 0f, 0f);
		level.addFreshEntity(b);
		b.setHuntTarget(p.getUUID());
		double[] last = {Double.NaN};
		double[] biggestJump = {0};
		double[] startDist = {0};
		c.runAfterDelay(4, () -> {
			p.teleportTo(level, o.getX() + 27.5, o.getY() + 1, o.getZ() + 5.5, java.util.Set.of(), 0f, 0f, false);
			startDist[0] = b.distanceTo(p);
			last[0] = b.getX();
		});
		c.startSequence()
			.thenIdle(6)
			.thenExecute(() -> {})
			.thenWaitUntil(() -> {
				if (!Double.isNaN(last[0])) {
					biggestJump[0] = Math.max(biggestJump[0], Math.abs(b.getX() - last[0]));
					last[0] = b.getX();
				}
				c.assertTrue(b.distanceTo(p) < 6, "the boss ran up to the target, distance " + b.distanceTo(p));
			})
			.thenExecute(() -> {
				c.assertTrue(biggestJump[0] < 3, "no snap: the largest step in one tick was " + biggestJump[0]);
				c.assertTrue(startDist[0] > 20 && startDist[0] < 30, "the target started 20-30 blocks away, " + startDist[0]);
				b.discard();
				for (int cx = -1; cx <= 3; cx++) {
					for (int cz = -1; cz <= 1; cz++) {
						level.setChunkForced((o.getX() >> 4) + cx, (o.getZ() >> 4) + cz, false);
					}
				}
				GameTestKit.drop(c, p);
			})
			.thenSucceed();
	}

	// ---- water ----

	@GameTest(maxTicks = 120)
	public void swimsMuchFasterThanADolphin(GameTestHelper c) {
		GameTestKit.enable(c);
		// a bedrock-walled tank 12 x 4 x 3 (bedrock: the boss cannot break it) filled with water
		for (int x = -1; x <= 12; x++) {
			for (int y = 0; y <= 5; y++) {
				for (int z = 3; z <= 7; z++) {
					boolean wall = x == -1 || x == 12 || y == 0 || y == 5 || z == 3 || z == 7;
					c.setBlock(x, y, z, wall ? Blocks.BEDROCK : Blocks.WATER);
				}
			}
		}
		Dolphin dolphin = c.spawn(EntityTypes.DOLPHIN, new Vec3(1.5, 2, 5.5));
		GodSlayer b = boss(c, 1.5, 1, 5.5, 1);
		ServerPlayer p = strong(c);
		GameTestKit.place(c, p, 11.5, 3, 5.5, 0f, 0f);
		b.setHuntTarget(p.getUUID());
		double[] best = new double[2]; // [dolphin, boss]: the largest distance per tick
		Vec3[] last = new Vec3[2];
		Vec3 end = c.absoluteVec(new Vec3(11.5, 3, 5.5));
		c.startSequence()
			.thenExecuteFor(40, () -> {
				dolphin.getNavigation().moveTo(end.x, end.y, end.z, 1.0);
				Vec3[] now = {dolphin.position(), b.position()};
				for (int i = 0; i < 2; i++) {
					if (last[i] != null) {
						best[i] = Math.max(best[i], now[i].distanceTo(last[i]));
					}
					last[i] = now[i];
				}
			})
			.thenExecute(() -> {
				c.assertTrue(best[0] > 0.05, "the dolphin did swim, best " + best[0]);
				c.assertTrue(best[1] > best[0] * 2.5, "the boss is clearly faster: boss " + best[1] + " dolphin " + best[0]);
				GameTestKit.drop(c, p);
			})
			.thenSucceed();
	}

	// ---- the whole hunt (all global state, one sequential test) ----

	@GameTest(maxTicks = 1500)
	public void huntFlow(GameTestHelper c) {
		GameTestKit.enable(c);
		GameTestKit.floor(c);
		MinecraftServer server = c.getLevel().getServer();
		server.setDifficulty(Difficulty.NORMAL, true);
		reset(server, 1);
		ServerPlayer p = strong(c);
		GameTestKit.place(c, p, 5.5, 1, 5.5, 0f, 0f);
		GodSlayer[] boss = new GodSlayer[1];
		ServerLevel nether = server.getLevel(Level.NETHER);

		c.startSequence()
			// -- its own slot (D184): after 2 treasures, from day 100, hearts --
			.thenExecute(() -> {
				GodSlayerHunt.onTreasure(server);
				c.assertTrue(!GodSlayerHunt.takesSlot(server, 100), "1 treasure: the next slot is the Crimson Master's");
				GodSlayerHunt.onTreasure(server);
				c.assertTrue(GodSlayerHunt.takesSlot(server, 100), "2 treasures: the next slot is the boss's");
				c.assertTrue(!GodSlayerHunt.takesSlot(server, 99), "never before day 100");
				c.assertTrue(GodSlayerHunt.phase(server) == GodSlayerSavedData.Phase.IDLE, "counting never starts a hunt");
				HeartsAttachment.set(p, HeartRules.withHearts(HeartsAttachment.get(p), 29));
				GodSlayerHunt.onSlot(server);
				c.assertTrue(GodSlayerHunt.phase(server) == GodSlayerSavedData.Phase.IDLE, "29 hearts: no hunt");
				c.assertTrue(!GodSlayerHunt.takesSlot(server, 100), "the slot is used even without a hunt");
				GodSlayerHunt.onTreasure(server);
				GodSlayerHunt.onTreasure(server);
				HeartsAttachment.set(p, HeartRules.withHearts(HeartsAttachment.get(p), 30));
				GodSlayerHunt.onSlot(server);
				c.assertTrue(GodSlayerHunt.phase(server) == GodSlayerSavedData.Phase.WARNING, "30 hearts in the boss's slot: warning");
				c.assertTrue(GodSlayerHunt.data(server).treasuresSince == 0, "counting starts again");
				c.assertTrue(GodSlayerHunt.currentBoss(server) == null, "no boss during the warning");
				GodSlayerHunt.giveUp(server);
				c.assertTrue(GodSlayerHunt.phase(server) == GodSlayerSavedData.Phase.IDLE, "giveup clears the warning");
				HeartsAttachment.set(p, HeartRules.withHearts(HeartsAttachment.get(p), 10));
			})
			// -- a hunt at level 1 --
			.thenExecute(() -> {
				c.assertTrue(GodSlayerHunt.start(server, p), "hunt starts");
				c.assertTrue(!GodSlayerHunt.start(server, p), "a second start is refused");
				GodSlayerHunt.skipWarning(server);
			})
			.thenWaitUntil(() -> {
				boss[0] = GodSlayerHunt.currentBoss(server);
				c.assertTrue(boss[0] != null && GodSlayerHunt.phase(server) == GodSlayerSavedData.Phase.HUNTING, "the boss appeared");
			})
			.thenExecute(() -> {
				c.assertTrue(boss[0].getMaxHealth() == 500f, "level 1 spawn has 500 health, was " + boss[0].getMaxHealth());
				c.assertTrue(boss[0].huntTarget().equals(p.getUUID()), "it hunts the target");
				nether.getChunk(0, 0); // load the chunk the boss will arrive in
				for (int x = -4; x <= 4; x++) {
					for (int z = -4; z <= 4; z++) {
						nether.setBlock(new net.minecraft.core.BlockPos(x, 120, z), Blocks.STONE.defaultBlockState(), 3);
						for (int y = 121; y <= 126; y++) {
							nether.setBlock(new net.minecraft.core.BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
						}
					}
				}
				p.teleportTo(nether, 0.5, 121, 0.5, java.util.Set.of(), 0f, 0f, false);
			})
			// -- it follows into the Nether --
			.thenWaitUntil(() -> {
				GodSlayer b = GodSlayerHunt.currentBoss(server);
				c.assertTrue(b != null && b.level() == nether, "the boss followed into the Nether");
				boss[0] = b;
			})
			// -- killed by a player: 100 levels, 5 boxes, level 2 --
			.thenExecute(() -> {
				boss[0].setHealth(1f);
				boss[0].hurtServer(nether, nether.damageSources().playerAttack(p), 5f);
			})
			.thenWaitUntil(() -> c.assertTrue(GodSlayerHunt.phase(server) == GodSlayerSavedData.Phase.IDLE, "state cleared after the kill"))
			.thenExecute(() -> {
				c.assertTrue(p.experienceLevel == GodSlayerRules.KILL_LEVELS, "+100 levels, has " + p.experienceLevel);
				int boxes = 0;
				for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
					ItemStack s = p.getInventory().getItem(i);
					if (RewardBoxes.tierOf(s).isPresent()) {
						boxes += s.getCount();
					}
				}
				c.assertTrue(boxes == GodSlayerRules.REWARD_BOXES, "5 reward boxes, had " + boxes);
				c.assertTrue(GodSlayerHunt.level(server) == 2, "boss level 2, was " + GodSlayerHunt.level(server));
				p.teleportTo(c.getLevel(), c.absoluteVec(new Vec3(5.5, 1, 5.5)).x, c.absoluteVec(new Vec3(5.5, 1, 5.5)).y,
					c.absoluteVec(new Vec3(5.5, 1, 5.5)).z, java.util.Set.of(), 0f, 0f, false);
				GodSlayerHunt.setLevel(server, 3);
				GodSlayerHunt.start(server, p);
				GodSlayerHunt.skipWarning(server);
			})
			// -- level 3 spawn, then the time limit --
			.thenWaitUntil(() -> {
				boss[0] = GodSlayerHunt.currentBoss(server);
				c.assertTrue(boss[0] != null, "the level 3 boss appeared");
			})
			.thenExecute(() -> {
				c.assertTrue(boss[0].getMaxHealth() == 700f, "level 3 spawn has 700 health, was " + boss[0].getMaxHealth());
				GodSlayerHunt.data(server).huntEndTick = server.overworld().getGameTime();
			})
			.thenWaitUntil(() -> c.assertTrue(GodSlayerHunt.phase(server) == GodSlayerSavedData.Phase.IDLE, "the hunt ended at the time limit"))
			.thenExecute(() -> {
				c.assertTrue(!boss[0].isAlive() || boss[0].isRemoved(), "the boss left");
				c.assertTrue(GodSlayerHunt.level(server) == 3, "the level is unchanged when it wins, was " + GodSlayerHunt.level(server));
				reset(server, 1);
				GameTestKit.drop(c, p);
			})
			.thenSucceed();
	}
}
