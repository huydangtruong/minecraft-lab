package io.github.huydangtruong.permahearts;

import com.mojang.authlib.GameProfile;
import io.github.huydangtruong.permahearts.technique.Frostbite;
import io.github.huydangtruong.permahearts.technique.PhaseState;
import io.github.huydangtruong.permahearts.technique.Technique;
import io.github.huydangtruong.permahearts.technique.TechniqueCasts;
import io.github.huydangtruong.permahearts.technique.TechniqueFields;
import io.github.huydangtruong.permahearts.technique.TechniqueRules;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;

/**
 * P-011 part C: server side of the six techniques, cast through TechniqueCasts.cast. Mock players from the game-test
 * helper are always Creative (invulnerable), so they only serve as casters; {@link #survivor} builds a real survival
 * player for damage checks. Targets are Husks (sun-proof) so daylight never confuses the fire checks.
 */
public class TechniqueEffectsGameTest {
	private static void enable(GameTestHelper c, boolean on) {
		MinecraftServer s = c.getLevel().getServer();
		s.getGameRules().set(PermaHearts.ENABLED, on, s);
	}

	private static void floor(GameTestHelper c) {
		for (int x = 0; x < 12; x++) {
			for (int z = 0; z < 12; z++) {
				c.setBlock(x, 0, z, Blocks.STONE);
			}
		}
	}

	private static void place(GameTestHelper c, ServerPlayer p, double x, double y, double z, float yaw, float pitch) {
		Vec3 v = c.absoluteVec(new Vec3(x, y, z));
		p.teleportTo(c.getLevel(), v.x, v.y, v.z, Set.of(), yaw, pitch, false);
	}

	/** A survival player that takes damage (the helper's mock is Creative and never "loaded"). */
	private static ServerPlayer survivor(GameTestHelper c) {
		ServerLevel level = c.getLevel();
		MinecraftServer server = level.getServer();
		GameProfile profile = new GameProfile(UUID.randomUUID(), "tq" + Integer.toHexString((int) (Math.random() * 0xFFFFFF)));
		CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
		ServerPlayer p = new ServerPlayer(server, level, cookie.gameProfile(), cookie.clientInformation()) {
			@Override
			public GameType gameMode() {
				return GameType.SURVIVAL;
			}
		};
		Connection connection = new Connection(PacketFlow.SERVERBOUND);
		new EmbeddedChannel(connection);
		server.getPlayerList().placeNewPlayer(connection, p, cookie);
		p.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
		p.setGameMode(GameType.SURVIVAL); // placeNewPlayer used the world's default mode: drop creative abilities
		p.getAbilities().invulnerable = false;
		p.getAbilities().mayfly = false;
		p.getAbilities().flying = false;
		return p;
	}

	private static void drop(GameTestHelper c, ServerPlayer p) {
		c.getLevel().getServer().getPlayerList().remove(p);
	}

	private static Husk husk(GameTestHelper c, double x, double z, boolean noAi) {
		Husk h = c.spawn(EntityTypes.HUSK, new Vec3(x, 1, z));
		h.setNoAi(noAi);
		return h;
	}

	private static boolean fireBlocksIn(GameTestHelper c) {
		for (int x = 0; x < 12; x++) {
			for (int y = 0; y < 6; y++) {
				for (int z = 0; z < 12; z++) {
					var b = c.getBlockState(new BlockPos(x, y, z));
					if (b.is(Blocks.FIRE) || b.is(Blocks.SOUL_FIRE)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/** A long floor (x 0..59) so a full-charge orb can land far from the caster. */
	private static void longFloor(GameTestHelper c) {
		for (int x = 0; x < 60; x++) {
			for (int z = 0; z < 12; z++) {
				c.setBlock(x, 0, z, Blocks.STONE);
			}
		}
	}

	private static void fullChargeOrbLeavesItsField(GameTestHelper c, Technique t, int level, double pitch) {
		enable(c, true);
		longFloor(c);
		ServerPlayer p = c.makeMockServerPlayerInLevel();
		place(c, p, 1.5, 1, 4.5, -90f, (float) pitch);
		int before = TechniqueFields.count();
		TechniqueCasts.cast(p, t, level, 1f);
		c.runAfterDelay(80, () -> {
			c.assertTrue(TechniqueFields.count() > before, t + " orb at full charge (level " + level + ") must leave its field when it lands");
			c.succeed();
		});
	}

	@GameTest(maxTicks = 120, padding = 64)
	public void fullChargeFireOrbLandsAndBurns(GameTestHelper c) {
		fullChargeOrbLeavesItsField(c, Technique.FIRE, 1, 0);
	}

	@GameTest(maxTicks = 120, padding = 64)
	public void fullChargeFireOrbAtMaxLevelLandsAndBurns(GameTestHelper c) {
		fullChargeOrbLeavesItsField(c, Technique.FIRE, TechniqueRules.MAX_LEVEL, 0);
	}

	@GameTest(maxTicks = 120, padding = 64)
	public void fullChargeIceOrbLandsAndFreezes(GameTestHelper c) {
		fullChargeOrbLeavesItsField(c, Technique.ICE, 1, 0);
	}

	@GameTest(maxTicks = 120, padding = 64)
	public void fullChargeOrbSplashingIntoDeepWaterStillLands(GameTestHelper c) {
		enable(c, true);
		longFloor(c);
		for (int x = 6; x < 60; x++) {
			for (int z = 0; z < 12; z++) {
				for (int y = 1; y <= 3; y++) {
					c.setBlock(x, y, z, Blocks.WATER);
				}
			}
		}
		ServerPlayer p = c.makeMockServerPlayerInLevel();
		place(c, p, 1.5, 1, 4.5, -90f, 0f);
		int before = TechniqueFields.count();
		TechniqueCasts.cast(p, Technique.FIRE, 1, 1f);
		c.runAfterDelay(80, () -> {
			c.assertTrue(TechniqueFields.count() > before, "an orb ending in water must still leave its field");
			c.succeed();
		});
	}

	@GameTest(maxTicks = 120, padding = 64)
	public void fullChargeOrbThrownUpStillLands(GameTestHelper c) {
		fullChargeOrbLeavesItsField(c, Technique.FIRE, 5, -30);
	}

	@GameTest(maxTicks = 100)
	public void fireOrbBurnsAHuskAndPlacesNoFire(GameTestHelper c) {
		enable(c, true);
		floor(c);
		ServerPlayer p = c.makeMockServerPlayerInLevel();
		place(c, p, 1.5, 1, 4.5, -90f, 0f); // looking along +X
		Husk h = husk(c, 6.5, 4.5, true);
		TechniqueCasts.cast(p, Technique.FIRE, 1, 1f);
		c.runAfterDelay(30, () -> {
			c.assertTrue(h.isOnFire() || h.getHealth() < h.getMaxHealth() - 3, "husk should burn, health " + h.getHealth());
			c.assertTrue(h.getHealth() < h.getMaxHealth() - 3, "impact (4 damage) should have hurt it, health " + h.getHealth());
			c.assertTrue(h.isOnFire(), "the field should have set the husk on fire");
			c.assertTrue(!fireBlocksIn(c), "no fire block may be placed");
			c.assertTrue(TechniqueFields.count() > 0, "the fire field should exist");
			c.succeed();
		});
	}

	@GameTest(maxTicks = 100)
	public void iceOrbGivesFrostbiteWithTheRightValues(GameTestHelper c) {
		enable(c, true);
		floor(c);
		ServerPlayer caster = c.makeMockServerPlayerInLevel();
		place(c, caster, 1.5, 1, 4.5, -90f, 0f);
		Husk h = husk(c, 6.5, 4.5, true);
		TechniqueCasts.cast(caster, Technique.ICE, 10, 1f);
		c.runAfterDelay(30, () -> {
			var effect = h.getEffect(Frostbite.EFFECT);
			c.assertTrue(effect != null, "husk should have Frostbite");
			int level10 = (int) Math.round(TechniqueRules.iceSlow(10) * 100) - 1;
			c.assertTrue(effect.getAmplifier() == level10, "amplifier should be " + level10 + ", was " + effect.getAmplifier());
			var move = h.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(Identifier.fromNamespaceAndPath(PermaHearts.MOD_ID, "frostbite_move"));
			c.assertTrue(move != null && Math.abs(move.amount() + TechniqueRules.iceSlow(10)) < 1e-9,
				"move modifier should be -" + TechniqueRules.iceSlow(10) + ", was " + (move == null ? "missing" : move.amount()));
			c.assertTrue(h.getHealth() < h.getMaxHealth() - 1, "impact (2 damage) should have hurt it");
			c.succeed();
		});
	}

	@GameTest(maxTicks = 100)
	public void frostbiteSlowsAttackSpeedOfAPlayerToo(GameTestHelper c) {
		enable(c, true);
		floor(c);
		ServerPlayer caster = c.makeMockServerPlayerInLevel();
		place(c, caster, 1.5, 1, 4.5, -90f, 0f);
		ServerPlayer target = survivor(c);
		place(c, target, 6.5, 1, 4.5, 0f, 0f);
		TechniqueCasts.cast(caster, Technique.ICE, 1, 1f);
		c.runAfterDelay(30, () -> {
			try {
				var atk = target.getAttribute(Attributes.ATTACK_SPEED).getModifier(Identifier.fromNamespaceAndPath(PermaHearts.MOD_ID, "frostbite_attack"));
				c.assertTrue(atk != null && Math.abs(atk.amount() + TechniqueRules.iceAttackSlow(1)) < 1e-9,
					"attack modifier should be -0.15, was " + (atk == null ? "missing" : atk.amount()));
				var mv = target.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(Identifier.fromNamespaceAndPath(PermaHearts.MOD_ID, "frostbite_move"));
				c.assertTrue(mv != null && Math.abs(mv.amount() + 0.20) < 1e-9, "move modifier should be -0.20");
				c.succeed();
			} finally {
				drop(c, target);
			}
		});
	}

	@GameTest(maxTicks = 300)
	public void healingCircleHealsEveryLivingEntity(GameTestHelper c) {
		enable(c, true);
		floor(c);
		ServerPlayer p = c.makeMockServerPlayerInLevel();
		place(c, p, 2.5, 1, 2.5, 0f, 0f);
		var cow = c.spawn(EntityTypes.COW, new Vec3(4.5, 1, 2.5));
		cow.setNoAi(true);
		Husk h = husk(c, 1.5, 4.0, true);
		Husk far = husk(c, 7.0, 7.0, true); // outside the radius 4 circle
		for (LivingEntity e : new LivingEntity[] {cow, h, far}) {
			e.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
			e.setHealth(30);
		}
		TechniqueCasts.cast(p, Technique.HEALING, 1, 1f);
		c.runAfterDelay(215, () -> {
			float expected = 30 + (float) TechniqueRules.healingTotal(1);
			c.assertTrue(Math.abs(cow.getHealth() - expected) < 0.01f, "cow should be at " + expected + ", was " + cow.getHealth());
			c.assertTrue(Math.abs(h.getHealth() - expected) < 0.01f, "husk should be at " + expected + ", was " + h.getHealth());
			c.assertTrue(far.getHealth() == 30f, "the husk outside the circle must not heal, was " + far.getHealth());
			c.succeed();
		});
	}

	@GameTest(maxTicks = 100)
	public void repulsePushesAndHurtsButNotTheCaster(GameTestHelper c) {
		enable(c, true);
		floor(c);
		ServerPlayer p = c.makeMockServerPlayerInLevel();
		place(c, p, 1.5, 1, 1.5, 0f, 0f);
		Husk h = husk(c, 3.5, 1.5, false);
		float before = h.getHealth();
		double d0 = h.position().distanceTo(c.absoluteVec(new Vec3(1.5, 1, 1.5)));
		float pHealth = p.getHealth();
		TechniqueCasts.cast(p, Technique.REPULSE, 5, 1f);
		c.runAfterDelay(15, () -> {
			double d1 = h.position().distanceTo(p.position());
			c.assertTrue(d1 > d0 + 2, "husk should be pushed away (level 5), distance " + d0 + " -> " + d1);
			c.assertTrue(d1 > TechniqueRules.repulseRadius(5), "husk should be out of the radius, distance " + d1);
			c.assertTrue(before - h.getHealth() >= TechniqueRules.repulseDamage(5) - 0.01, "husk should take " + TechniqueRules.repulseDamage(5) + ", health " + before + " -> " + h.getHealth());
			c.assertTrue(p.getHealth() == pHealth, "the caster must not be hurt");
			c.succeed();
		});
	}

	@GameTest(maxTicks = 100)
	public void phaseWalkPassesWallsButStandsOnTheFloor(GameTestHelper c) {
		enable(c, true);
		floor(c);
		for (int y = 1; y <= 3; y++) {
			for (int z = 2; z <= 7; z++) {
				c.setBlock(6, y, z, Blocks.STONE);
			}
		}
		ServerPlayer p = c.makeMockServerPlayerInLevel();
		place(c, p, 3.5, 1, 4.5, -90f, 0f);
		double wallX = c.absoluteVec(new Vec3(6, 0, 0)).x;
		// Without Phase Walk the wall stops the player.
		p.move(MoverType.SELF, new Vec3(4, 0, 0));
		c.assertTrue(p.getX() < wallX, "the wall should stop the player, x " + p.getX() + " wall " + wallX);
		place(c, p, 3.5, 1, 4.5, -90f, 0f);
		TechniqueCasts.cast(p, Technique.PHASE, 1, 1f);
		c.assertTrue(PhaseState.phasing(p), "player should be phasing");
		p.move(MoverType.SELF, new Vec3(4, 0, 0));
		c.assertTrue(p.getX() > wallX + 1, "phasing player should pass the wall, x " + p.getX() + " wall " + wallX);
		c.assertTrue(!p.isInWall(), "no suffocation while phasing");
		// The floor below the cast height still holds.
		double y0 = p.getY();
		p.move(MoverType.SELF, new Vec3(0, -3, 0));
		c.assertTrue(p.getY() > y0 - 0.5, "the floor should hold, y " + y0 + " -> " + p.getY());
		// Ending inside the wall lifts the player to free space.
		place(c, p, 6.5, 1, 4.5, 0f, 0f);
		TechniqueCasts.endPhase(p);
		c.assertTrue(!PhaseState.phasing(p), "phase should be over");
		c.assertTrue(c.getLevel().noCollision(p, p.getBoundingBox()), "player must not be left inside blocks");
		c.assertTrue(p.getY() >= c.absoluteVec(new Vec3(0, 4, 0)).y - 1e-6, "player should be lifted above the wall, y " + p.getY());
		c.succeed();
	}

	@GameTest(maxTicks = 100)
	public void phaseWalkFloorFollowsFalls(GameTestHelper c) {
		enable(c, true);
		// A platform at y 2 with a hole (x 4..7) over lower ground at y 0, and a wall on that lower level.
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				c.setBlock(x, x < 4 ? 2 : 0, z, Blocks.STONE);
			}
		}
		for (int y = 1; y <= 2; y++) {
			c.setBlock(6, y, 4, Blocks.STONE);
		}
		ServerPlayer p = c.makeMockServerPlayerInLevel();
		place(c, p, 3.5, 3, 4.5, -90f, 0f);
		TechniqueCasts.cast(p, Technique.PHASE, 1, 1f);
		int cast = p.getAttached(PhaseState.ATTACHMENT).floorY();
		c.assertTrue(cast == c.absolutePos(new BlockPos(0, 3, 0)).getY(), "cast floor should be the feet level, was " + cast);
		// Walk over the hole and fall (the mock does not run Player.tick, so do what it does).
		p.move(MoverType.SELF, new Vec3(1.5, 0, 0));
		for (int i = 0; i < 10; i++) {
			p.move(MoverType.SELF, new Vec3(0, -0.8, 0));
			PhaseState.followFall(p);
		}
		int lowFloor = c.absolutePos(new BlockPos(0, 1, 0)).getY();
		c.assertTrue(p.onGround() && Math.abs(p.getY() - lowFloor) < 1e-6, "player should stand on the lower ground, y " + p.getY() + " expected " + lowFloor);
		int floor = p.getAttached(PhaseState.ATTACHMENT).floorY();
		c.assertTrue(floor == lowFloor, "floor should follow the fall to " + lowFloor + ", was " + floor);
		// The wall on the lower level is passed, the ground below still holds.
		double wallX = c.absoluteVec(new Vec3(6, 0, 0)).x;
		p.move(MoverType.SELF, new Vec3(2, 0, 0));
		c.assertTrue(p.getX() > wallX + 0.9, "player should pass the wall on the lower level, x " + p.getX() + " wall " + wallX);
		double y0 = p.getY();
		p.move(MoverType.SELF, new Vec3(0, -3, 0));
		c.assertTrue(p.getY() > y0 - 0.5, "the lower ground should hold, y " + y0 + " -> " + p.getY());
		// The floor never rises.
		place(c, p, 3.5, 4, 4.5, 0f, 0f);
		PhaseState.followFall(p);
		c.assertTrue(p.getAttached(PhaseState.ATTACHMENT).floorY() == floor, "the floor must not rise");
		TechniqueCasts.endPhase(p);
		c.succeed();
	}

	@GameTest(maxTicks = 100)
	public void phaseWalkDurationFollowsTheLevel(GameTestHelper c) {
		enable(c, true);
		ServerPlayer p = c.makeMockServerPlayerInLevel();
		place(c, p, 3.5, 1, 4.5, 0f, 0f);
		long now = c.getLevel().getGameTime();
		TechniqueCasts.cast(p, Technique.PHASE, 10, 1f);
		PhaseState s = p.getAttached(PhaseState.ATTACHMENT);
		long ticks = s.endTick() - now;
		c.assertTrue(Math.abs(ticks - Math.round(TechniqueRules.phaseSeconds(10) * 20)) <= 1, "level 10 should last 14.5 s, was " + ticks + " ticks");
		TechniqueCasts.endPhase(p);
		c.succeed();
	}

	@GameTest(maxTicks = 100)
	public void bloodOathCostsHealthAndBlocksDamageThenStops(GameTestHelper c) {
		enable(c, true);
		floor(c);
		ServerPlayer p = survivor(c);
		try {
			place(c, p, 5.5, 1, 5.5, 0f, 0f);
			c.assertTrue(!p.isCreative(), "test player must be a survival player");
			p.setHealth(20f);
			TechniqueCasts.cast(p, Technique.SACRIFICE, 1, 1f);
			c.assertTrue(Math.abs(p.getHealth() - 4f) < 0.01f, "20 health at level 1 should leave 4, was " + p.getHealth());
			c.assertTrue(TechniqueCasts.oathActive(p), "oath should be active");
			ServerLevel level = c.getLevel();
			boolean hit = p.hurtServer(level, level.damageSources().generic(), 3f);
			c.assertTrue(!hit && Math.abs(p.getHealth() - 4f) < 0.01f, "damage must be cancelled during the oath, health " + p.getHealth());
			c.assertTrue(TechniqueCasts.blocksDamage(p, level.damageSources().magic()), "magic damage is blocked");
			c.assertTrue(!TechniqueCasts.blocksDamage(p, level.damageSources().fellOutOfWorld()), "void damage still goes through");
			c.assertTrue(!TechniqueCasts.blocksDamage(p, level.damageSources().genericKill()), "/kill still goes through");
			// The oath ends: expire it and damage works again.
			p.setAttached(TechniqueCasts.OATH_UNTIL, level.getGameTime() - 1);
			p.hurtServer(level, level.damageSources().generic(), 3f);
			c.assertTrue(p.getHealth() < 4f, "damage should work after the oath, health " + p.getHealth());
			// Never kills the caster: 1.5 health at level 1 leaves 1.
			p.setHealth(1.5f);
			TechniqueCasts.cast(p, Technique.SACRIFICE, 1, 1f);
			c.assertTrue(p.getHealth() >= 1f, "the oath must never take the last health, was " + p.getHealth());
			p.setAttached(TechniqueCasts.OATH_UNTIL, level.getGameTime() - 1);
			c.succeed();
		} finally {
			drop(c, p);
		}
	}

	@GameTest(maxTicks = 100)
	public void bloodOathCostFollowsTheLevel(GameTestHelper c) {
		enable(c, true);
		ServerPlayer p = c.makeMockServerPlayerInLevel();
		p.setHealth(20f);
		TechniqueCasts.cast(p, Technique.SACRIFICE, 10, 1f);
		c.assertTrue(Math.abs(p.getHealth() - 10f) < 0.01f, "level 10 takes 50% of 20, was " + p.getHealth());
		p.setAttached(TechniqueCasts.OATH_UNTIL, c.getLevel().getGameTime() - 1);
		c.succeed();
	}

	@GameTest(maxTicks = 100)
	public void techniqueDamageReachesPlayersWithPvpOff(GameTestHelper c) {
		enable(c, true);
		floor(c);
		MinecraftServer server = c.getLevel().getServer();
		ServerPlayer caster = c.makeMockServerPlayerInLevel();
		ServerPlayer target = survivor(c);
		boolean pvp = server.getGameRules().get(GameRules.PVP);
		server.getGameRules().set(GameRules.PVP, false, server);
		try {
			place(c, caster, 5.5, 1, 5.5, 0f, 0f);
			place(c, target, 7.5, 1, 5.5, 0f, 0f);
			target.setHealth(20f);
			ServerLevel level = c.getLevel();
			// Control: ordinary player-vs-player damage is refused when PvP is off.
			boolean plain = target.hurtServer(level, level.damageSources().playerAttack(caster), 2f);
			c.assertTrue(!plain && target.getHealth() == 20f, "control: PvP off should refuse player attacks");
			TechniqueCasts.cast(caster, Technique.REPULSE, 1, 1f);
			c.assertTrue(Math.abs(target.getHealth() - (20f - 2f)) < 0.01f, "repulse should hurt the other player by 2, health " + target.getHealth());
			c.succeed();
		} finally {
			server.getGameRules().set(GameRules.PVP, pvp, server);
			drop(c, target);
		}
	}

	@GameTest(maxTicks = 100)
	public void nothingWorksWithTheSwitchOff(GameTestHelper c) {
		floor(c);
		ServerPlayer p = c.makeMockServerPlayerInLevel();
		place(c, p, 5.5, 1, 5.5, 0f, 0f);
		p.setHealth(20f);
		int fields = TechniqueFields.count();
		enable(c, false);
		try {
			for (Technique t : Technique.values()) {
				c.assertTrue(!TechniqueCasts.cast(p, t, 5, 1f), t.displayName() + " must do nothing with the switch off");
			}
			c.assertTrue(p.getHealth() == 20f, "Blood Oath must not cost health");
			c.assertTrue(!PhaseState.phasing(p) && !TechniqueCasts.oathActive(p), "no phase or oath with the switch off");
			c.assertTrue(TechniqueFields.count() == fields, "no field with the switch off");
		} finally {
			enable(c, true);
		}
		c.succeed();
	}
}
