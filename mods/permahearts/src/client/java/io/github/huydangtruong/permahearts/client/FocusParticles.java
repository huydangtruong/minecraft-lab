package io.github.huydangtruong.permahearts.client;

import io.github.huydangtruong.permahearts.technique.Technique;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Small particles at the hand of every player who has a technique selected (D46): fire, snow, green sparkles ... */
final class FocusParticles {
	private static final ParticleOptions DROP = new DustParticleOptions(0x5a0000, 0.7f);
	private static final ParticleOptions BLOOD = new DustParticleOptions(0x8B0000, 0.8f);

	private FocusParticles() {}

	static void tick(Minecraft mc) {
		Level level = mc.level;
		if (level == null || mc.player == null || level.getGameTime() % 3 != 0) {
			return;
		}
		for (Player p : level.players()) {
			if (p.isSpectator() || p.isInvisible() || p.distanceToSqr(mc.player) > 48 * 48) {
				continue;
			}
			Technique t = ClientSkills.focusOf(p);
			if (t != null) {
				spawn(level, p, t);
			}
		}
	}

	private static void spawn(Level level, Player p, Technique t) {
		double yaw = Math.toRadians(p.getYRot());
		double fx = -Math.sin(yaw);
		double fz = Math.cos(yaw);
		// the main hand sits to the right of the facing direction (left for left-handed players)
		double side = p.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT ? 1 : -1;
		double rx = -Math.cos(yaw) * side;
		double rz = -Math.sin(yaw) * side;
		var rnd = level.getRandom();
		double x = p.getX() + fx * 0.6 + rx * 0.4 + (rnd.nextDouble() - 0.5) * 0.12;
		double y = p.getEyeY() - 0.5 + (rnd.nextDouble() - 0.5) * 0.12;
		double z = p.getZ() + fz * 0.6 + rz * 0.4 + (rnd.nextDouble() - 0.5) * 0.12;
		switch (t) {
			case FIRE -> level.addParticle(ParticleTypes.FLAME, x, y, z, 0, 0.03, 0);
			case ICE -> level.addParticle(ParticleTypes.SNOWFLAKE, x, y, z, (rnd.nextDouble() - 0.5) * 0.02, -0.01, (rnd.nextDouble() - 0.5) * 0.02);
			case HEALING -> level.addParticle(ParticleTypes.HAPPY_VILLAGER, x, y, z, 0, 0.02, 0);
			case PHASE -> level.addParticle(ParticleTypes.PORTAL, x, y, z, (rnd.nextDouble() - 0.5) * 0.3, rnd.nextDouble() * 0.2, (rnd.nextDouble() - 0.5) * 0.3);
			case REPULSE -> {
				double a = level.getGameTime() * 0.7;
				level.addParticle(ParticleTypes.CLOUD, x, y, z, Math.cos(a) * 0.04, 0.0, Math.sin(a) * 0.04);
			}
			case SACRIFICE -> level.addParticle(BLOOD, x, y, z, 0, 0.01, 0);
			case TELEPORT -> level.addParticle(ParticleTypes.PORTAL, x, y, z, 0, 0.05, 0);
			case FLIGHT -> {
				level.addParticle(ParticleTypes.CLOUD, x, y, z, (rnd.nextDouble() - 0.5) * 0.02, -0.015, (rnd.nextDouble() - 0.5) * 0.02);
				if (rnd.nextInt(3) == 0) {
					level.addParticle(ParticleTypes.END_ROD, x, y, z, (rnd.nextDouble() - 0.5) * 0.03, -0.03, (rnd.nextDouble() - 0.5) * 0.03);
				}
			}
			case BLADE -> {
				level.addParticle(DROP, x, y - 0.1, z, 0, -0.05, 0);
				if (rnd.nextInt(2) == 0) {
					level.addParticle(DROP, x + (rnd.nextDouble() - 0.5) * 0.25, y + 0.25, z + (rnd.nextDouble() - 0.5) * 0.25, 0, -0.04, 0);
				}
			}
		}
	}
}
