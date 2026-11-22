package io.github.huydangtruong.immortalcultivation;

import io.github.huydangtruong.immortalcultivation.technique.Seism;
import io.github.huydangtruong.immortalcultivation.technique.Technique;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueAttachment;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueData;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueNet;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueRules;
import io.github.huydangtruong.immortalcultivation.technique.VioletGuardian;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** P-056 / D142: Seism on the server: Impact damage rules, the Wave (damage, lift, height rule, airborne skip, bosses), cooldown. */
public class SeismGameTest {
	private static ServerPlayer caster(GameTestHelper c, int level) {
		GameTestKit.enable(c);
		GameTestKit.floor(c);
		c.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true);
		ServerPlayer p = GameTestKit.survivor(c);
		GameTestKit.place(c, p, 5.5, 1, 5.5, 0f, 0f);
		TechniqueAttachment.set(p, TechniqueData.DEFAULT.withLevel(Technique.SEISM, level, 10)
			.withEmeralds(TechniqueRules.MAX_EMERALDS).withSlot(0, Technique.SEISM));
		return p;
	}

	private static Vec3 abs(GameTestHelper c, double x, double y, double z) {
		return c.absoluteVec(new Vec3(x, y, z));
	}

	private static Zombie zombie(GameTestHelper c, double x, double y, double z) {
		Zombie z1 = c.spawn(EntityTypes.ZOMBIE, new Vec3(x, y, z));
		z1.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(20.0); // a spawned zombie may roll a max-health bonus (20.9)
		z1.setHealth(20f);
		still(z1);
		z1.setOnGround(true); // a no-AI mob never lands by itself
		return z1;
	}

	private static Cow cow(GameTestHelper c, double x, double y, double z) {
		Cow cow = c.spawn(EntityTypes.COW, new Vec3(x, y, z));
		still(cow);
		cow.setOnGround(true);
		return cow;
	}

	private static void still(Mob m) {
		m.setPersistenceRequired();
		m.setNoAi(true);
		m.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 2000, 0, false, false, false));
	}

	private static boolean near(float a, float b) {
		return Math.abs(a - b) < 0.6f;
	}

	@GameTest(maxTicks = 60)
	public void impactHurtsAHostileMobFiveHeartsAndLeavesACowAlone(GameTestHelper c) {
		ServerPlayer p = caster(c, 1);
		Zombie z = zombie(c, 5.5, 1, 10.5);
		Cow cow = cow(c, 6.5, 1, 10.5);
		Seism.impact(p, 1, abs(c, 5.5, 1, 10.5));
		c.runAfterDelay(8, () -> c.assertTrue(z.getHealth() == 20f, "the boulder is still falling, health " + z.getHealth()));
		c.runAfterDelay(16, () -> {
			c.assertTrue(near(z.getHealth(), 10f), "zombie lost 10 HP at Lv 1, health " + z.getHealth());
			c.assertTrue(cow.getHealth() == 10f, "the cow is not hurt, health " + cow.getHealth());
			c.assertTrue(p.getHealth() == p.getMaxHealth(), "the caster is not hurt");
			GameTestKit.drop(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 100)
	public void waveAtLevelThreeHurtsAndLiftsGroundTargetsAndSkipsAirborneOnes(GameTestHelper c) {
		ServerPlayer p = caster(c, 3);
		Zombie z = zombie(c, 7.5, 1, 5.5);
		Cow cow = cow(c, 3.5, 1, 5.5);
		Zombie flying = zombie(c, 5.5, 3, 3.5);
		flying.setNoGravity(true);
		flying.setOnGround(false);
		c.setBlock(5, 1, 7, Blocks.STONE);
		Zombie high = zombie(c, 5.5, 2, 7.5);
		c.runAfterDelay(6, () -> {
			c.assertTrue(z.onGround() && high.onGround() && cow.onGround(), "the targets stand on the ground before the cast: z=" + z.onGround() + " high=" + high.onGround() + " cow=" + cow.onGround() + " highY=" + high.getY());
			c.assertTrue(TechniqueNet.cast(p, 0, 1f) == null, "the Wave cast is accepted");
			c.assertTrue(Seism.slowed(p), "the caster is slowed during the Wave");
		});
		c.runAfterDelay(10, () -> {
			c.assertTrue(near(z.getHealth(), 16f), "zombie took 4 HP, health " + z.getHealth());
			c.assertTrue(near(cow.getHealth(), 6f), "cow took 4 HP, health " + cow.getHealth());
			c.assertTrue(near(high.getHealth(), 18f), "one block higher: 2 HP less damage, health " + high.getHealth());
			c.assertTrue(flying.getHealth() == 20f, "the airborne zombie is skipped, health " + flying.getHealth());
			c.assertTrue(z.getY() > abs(c, 0, 2.5, 0).y || z.getDeltaMovement().y > 0.2, "the zombie was lifted, y " + z.getY() + " vy " + z.getDeltaMovement().y);
			c.assertTrue(cow.getY() > abs(c, 0, 2.5, 0).y || cow.getDeltaMovement().y > 0.2, "the cow was lifted, y " + cow.getY() + " vy " + cow.getDeltaMovement().y);
			c.assertTrue(p.getHealth() == p.getMaxHealth(), "the caster is not hurt");
		});
		c.runAfterDelay(80, () -> {
			c.assertTrue(!Seism.slowed(p), "the slow is gone after 3 s");
			GameTestKit.drop(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 60)
	public void aBossTakesWaveDamageButDoesNotMove(GameTestHelper c) {
		ServerPlayer p = caster(c, 3);
		// a Warden counts as a boss; the God Slayer is not used: it touches the global hunt state other tests rely on
		Mob b = c.spawn(EntityTypes.WARDEN, new Vec3(7.5, 1, 5.5));
		still(b);
		b.setOnGround(true);
		Vec3 v = b.position();
		c.runAfterDelay(6, () -> Seism.wave(p, 3));
		float[] before = new float[1];
		c.runAfterDelay(5, () -> before[0] = b.getHealth());
		c.runAfterDelay(12, () -> {
			c.assertTrue(b.getHealth() < before[0], "the boss took damage, health " + b.getHealth() + " of " + before[0] + " ground=" + b.onGround());
			c.assertTrue(Math.abs(b.getX() - v.x) < 0.3 && Math.abs(b.getZ() - v.z) < 0.3, "no knockback");
			c.assertTrue(b.getY() < v.y + 0.3, "no lift, y " + b.getY() + " from " + v.y);
			b.discard();
			GameTestKit.drop(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 60)
	public void waveIsNotAvailableAtLevelTwo(GameTestHelper c) {
		ServerPlayer p = caster(c, 2);
		Cow cow = cow(c, 7.5, 1, 5.5);
		c.assertTrue(TechniqueNet.cast(p, 0, 1f) == null, "a full hold still casts (as Impact)");
		c.assertTrue(!Seism.slowed(p), "no Wave: the caster is not slowed");
		c.runAfterDelay(10, () -> {
			c.assertTrue(cow.getHealth() == 10f, "no wave damage near the caster, health " + cow.getHealth());
			GameTestKit.drop(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 60)
	public void bothModesStartTheSameTwentyEightSecondCooldown(GameTestHelper c) {
		ServerPlayer p = caster(c, 3);
		long now = c.getLevel().getServer().overworld().getGameTime();
		c.assertTrue(TechniqueNet.cast(p, 0, 0f) == null, "Impact is accepted");
		c.assertTrue(TechniqueAttachment.get(p).cooldownLeft(Technique.SEISM, now) == 28 * 20, "Impact cooldown is 28 s");
		TechniqueAttachment.set(p, TechniqueAttachment.get(p).withCooldown(Technique.SEISM, 0));
		c.assertTrue(TechniqueNet.cast(p, 0, 1f) == null, "the Wave is accepted");
		c.assertTrue(Seism.slowed(p), "that was the Wave");
		c.assertTrue(TechniqueAttachment.get(p).cooldownLeft(Technique.SEISM, now) >= 28 * 20 - 1, "Wave cooldown is 28 s");
		c.assertTrue(TechniqueNet.cast(p, 0, 0f) != null, "on cooldown now");
		c.runAfterDelay(50, () -> {
			GameTestKit.drop(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 100) // P-077 B11
	public void theWaveSparesTheCastersGuardianAndOtherPlayersWithPvpOff(GameTestHelper c) {
		ServerPlayer p = caster(c, 3);
		ServerPlayer other = GameTestKit.survivor(c);
		GameTestKit.place(c, other, 7.5, 1, 5.5, 0f, 0f);
		var guardians = VioletGuardian.summon(p, 1);
		Vec3 gp = abs(c, 3.5, 1, 5.5);
		guardians.forEach(g -> g.teleportTo(gp.x, gp.y, gp.z));
		Zombie z = zombie(c, 5.5, 1, 7.5);
		net.minecraft.server.MinecraftServer server = c.getLevel().getServer();
		boolean pvp = server.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.PVP);
		server.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.PVP, false, server);
		c.runAfterDelay(6, () -> {
			other.setOnGround(true); // a player nobody moves never lands by itself
			c.assertTrue(other.onGround() && guardians.get(0).onGround() && z.onGround(), "all stand on the ground: other=" + other.onGround() + " guardian=" + guardians.get(0).onGround());
			c.assertTrue(TechniqueNet.cast(p, 0, 1f) == null, "the Wave cast is accepted");
		});
		c.runAfterDelay(12, () -> {
			server.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.PVP, pvp, server);
			c.assertTrue(near(z.getHealth(), 16f), "control: the zombie took 4 HP, health " + z.getHealth());
			c.assertTrue(other.getHealth() == other.getMaxHealth(), "another player is spared with PvP off, health " + other.getHealth());
			c.assertTrue(guardians.get(0).getHealth() == guardians.get(0).getMaxHealth(), "the caster's guardian is spared, health " + guardians.get(0).getHealth());
			guardians.forEach(g -> g.discard());
			GameTestKit.drop(c, other);
			GameTestKit.drop(c, p);
			c.succeed();
		});
	}
}
