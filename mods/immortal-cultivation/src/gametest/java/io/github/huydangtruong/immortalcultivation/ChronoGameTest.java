package io.github.huydangtruong.immortalcultivation;

import io.github.huydangtruong.immortalcultivation.technique.Technique;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueAttachment;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueData;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueNet;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueRules;
import io.github.huydangtruong.immortalcultivation.technique.chrono.Chrono;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/* padding = 24 on every test: a time stop freezes everything in 16 blocks, never a neighbour test's mobs. */

/** P-083 / D268: Time Rewind on the server: recording, the stop, rewinding mobs, blocks and the caster, the resume. */
public class ChronoGameTest {
	private static ServerPlayer caster(GameTestHelper c, int level) {
		GameTestKit.enable(c);
		GameTestKit.floor(c);
		c.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true);
		ServerPlayer p = GameTestKit.survivor(c);
		GameTestKit.place(c, p, 3.5, 1, 2.5, 0f, 0f);
		TechniqueAttachment.set(p, TechniqueData.DEFAULT.withLevel(Technique.CHRONO, level, 10)
			.withEmeralds(TechniqueRules.MAX_EMERALDS).withSlot(0, Technique.CHRONO));
		return p;
	}

	private static Zombie zombie(GameTestHelper c, double x, double z) {
		Zombie m = c.spawn(EntityTypes.ZOMBIE, new Vec3(x, 1, z));
		still(m);
		return m;
	}

	private static void still(Mob m) {
		m.setPersistenceRequired();
		m.setNoAi(true);
		m.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 2000, 0, false, false, false));
	}

	private static Vec3 abs(GameTestHelper c, double x, double y, double z) {
		return c.absoluteVec(new Vec3(x, y, z));
	}

	@GameTest(maxTicks = 120, padding = 24)
	public void chronoRewindsMobsBlocksAndTheCasterButNotWhatChangedDuringTheStop(GameTestHelper c) {
		ServerPlayer p = caster(c, 1);
		c.setBlock(5, 1, 5, Blocks.STONE);
		c.setBlock(6, 1, 5, Blocks.STONE);
		Zombie moved = zombie(c, 2.5, 6.5);
		Zombie killed = zombie(c, 7.5, 6.5);
		Zombie[] late = new Zombie[1];
		c.runAfterDelay(20, () -> p.connection.teleport(abs(c, 4.5, 1, 2.5).x, abs(c, 4.5, 1, 2.5).y, abs(c, 4.5, 1, 2.5).z, 0f, 0f));
		c.runAfterDelay(12, () -> killed.kill(c.getLevel()));
		c.runAfterDelay(25, () -> {
			moved.teleportTo(abs(c, 5.5, 1, 8.5).x, abs(c, 5.5, 1, 8.5).y, abs(c, 5.5, 1, 8.5).z);
			moved.setHealth(6);
			moved.setRemainingFireTicks(5000); // (zombies burn in the sun anyway: far more than that)
			c.setBlock(5, 1, 5, Blocks.AIR);
			p.setHealth(8);
			p.getFoodData().setFoodLevel(5);
		});
		c.runAfterDelay(30, () -> late[0] = zombie(c, 4.5, 9.5));
		c.runAfterDelay(40, () -> {
			c.assertTrue(!killed.isAlive() && killed.isRemoved(), "the killed zombie is gone before the cast");
			c.assertTrue(Chrono.recorded(p) >= 35, "about 40 ticks are recorded, found " + Chrono.recorded(p));
			c.assertTrue(TechniqueNet.cast(p, 0, 1f) == null, "the cast is accepted");
			c.assertTrue(Chrono.active(p), "time is stopped");
			c.assertTrue(Chrono.held(p), "D272: the caster is held");
			moved.kill(c.getLevel()); // killed in the stop: dying, frozen
			c.setBlock(6, 1, 5, Blocks.AIR); // broken during the stop: rewinding does not bring it back
			Chrono.scroll(p, 3); // 30 ticks back: before the zombie moved and the block broke, after the kill
		});
		c.runAfterDelay(43, () -> {
			c.assertTrue(Chrono.shown(p) > 0 && Chrono.shown(p) < 30, "the rewind plays smoothly, shown " + Chrono.shown(p));
		});
		c.runAfterDelay(60, () -> {
			c.assertTrue(Chrono.shown(p) == 30, "the rewind reached 1.5 s back, shown " + Chrono.shown(p));
			Vec3 home = abs(c, 2.5, 1, 6.5);
			Zombie m = (Zombie) c.getLevel().getEntity(moved.getUUID());
			c.assertTrue(m != null && m.isAlive() && m.deathTime == 0, "D272: the zombie killed in the stop is alive again");
			c.assertTrue(m.getId() != moved.getId(), "D272: as a fresh copy (clients drop the dying one)");
			c.assertTrue(m.position().distanceTo(home) < 0.1, "the zombie is back where it stood, at " + m.position());
			c.assertTrue(m.getHealth() == m.getMaxHealth(), "its health is back, " + m.getHealth());
			c.assertTrue(m.getRemainingFireTicks() < 1000, "D272: the fire it caught later is gone: " + m.getRemainingFireTicks());
			c.assertTrue(c.getBlockState(new BlockPos(5, 1, 5)).is(Blocks.STONE), "the block broken before the stop is back");
			c.assertTrue(c.getBlockState(new BlockPos(6, 1, 5)).isAir(), "the block broken during the stop stays broken");
			c.assertTrue(late[0].isRemoved(), "the zombie that came later is gone");
			c.assertTrue(p.getHealth() == p.getMaxHealth(), "the caster's health is back, " + p.getHealth());
			c.assertTrue(p.getFoodData().getFoodLevel() == 20, "the caster's food is back, " + p.getFoodData().getFoodLevel());
			Zombie back = (Zombie) c.getLevel().getEntity(killed.getUUID());
			c.assertTrue(back != null && back.isAlive() && back.deathTime == 0 && back.getHealth() == back.getMaxHealth(), "the killed zombie is alive again");
			c.assertTrue(p.position().distanceTo(abs(c, 3.5, 1, 2.5)) < 0.1, "D272: the held caster went back to where they stood, at " + p.position());
		});
		c.runAfterDelay(70, () -> {
			Zombie back = (Zombie) c.getLevel().getEntity(killed.getUUID());
			Vec3 at = back.position();
			c.assertTrue(back.position().equals(at), "frozen");
			c.assertTrue(TechniqueNet.cast(p, 0, 1f) == null, "the recast lets time run again");
			c.assertTrue(!Chrono.active(p), "time runs again");
			c.assertTrue(TechniqueNet.cast(p, 0, 1f) != null, "the 140 s cooldown runs from the resume");
			long left = TechniqueAttachment.get(p).cooldownLeft(Technique.CHRONO, c.getLevel().getServer().overworld().getGameTime());
			c.assertTrue(left > 139 * 20 && left <= 140 * 20, "cooldown is 140 s, " + left);
		});
		c.runAfterDelay(80, () -> {
			c.assertTrue(late[0].isRemoved(), "the later zombie never comes back");
			GameTestKit.drop(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 80, padding = 24)
	public void chronoFreezesTheBoxLetsTheCasterHitAndEndsByItself(GameTestHelper c) {
		ServerPlayer p = caster(c, 1);
		Zombie z = zombie(c, 3.5, 5.5);
		z.setNoAi(false);
		c.runAfterDelay(5, () -> {
			c.assertTrue(Chrono.usable(p) == 0, "no stop yet");
			c.assertTrue(TechniqueNet.cast(p, 0, 1f) == null, "the cast is accepted");
			z.setDeltaMovement(0, 0.5, 0);
			Chrono.release(p);
			c.assertTrue(!Chrono.held(p) && Chrono.active(p), "D272: the caster let go, the box stays stopped");
		});
		c.runAfterDelay(15, () -> {
			c.assertTrue(z.position().distanceTo(abs(c, 3.5, 1, 5.5)) < 0.01, "the zombie inside does not move, at " + z.position());
			float before = z.getHealth();
			z.hurtServer(c.getLevel(), c.getLevel().damageSources().playerAttack(p), 3);
			c.assertTrue(z.getHealth() < before, "a frozen mob can be hit");
			float twice = z.getHealth();
			c.runAfterDelay(12, () -> {
				z.hurtServer(c.getLevel(), c.getLevel().damageSources().playerAttack(p), 3);
				c.assertTrue(z.getHealth() < twice, "and hit again");
			});
		});
		c.runAfterDelay(40, () -> {
			c.assertTrue(Chrono.active(p), "still stopped after 1.75 s");
			// end it early the way the duration would: no slot any more
			TechniqueAttachment.set(p, TechniqueAttachment.get(p).withSlot(0, null));
		});
		c.runAfterDelay(42, () -> {
			c.assertTrue(!Chrono.active(p), "the stop ended");
			c.assertTrue(TechniqueAttachment.get(p).onCooldown(Technique.CHRONO, c.getLevel().getServer().overworld().getGameTime()),
				"the cooldown runs");
		});
		c.runAfterDelay(50, () -> {
			c.assertTrue(z.position().y > abs(c, 3.5, 1, 5.5).y + 0.05, "the zombie moves again (the push it got is applied)");
			GameTestKit.drop(c, p);
			c.succeed();
		});
	}
}
