package io.github.huydangtruong.permahearts;

import io.github.huydangtruong.permahearts.technique.Technique;
import io.github.huydangtruong.permahearts.technique.TechniqueAttachment;
import io.github.huydangtruong.permahearts.technique.TechniqueData;
import io.github.huydangtruong.permahearts.technique.TechniqueNet;
import io.github.huydangtruong.permahearts.technique.TechniqueRules;
import io.github.huydangtruong.permahearts.technique.warp.PortalRecord;
import io.github.huydangtruong.permahearts.technique.warp.Warp;
import io.github.huydangtruong.permahearts.technique.warp.WarpPortal;
import io.github.huydangtruong.permahearts.technique.warp.WarpSavedData;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** P-026: Cosmic Warping on the server: placing, the limit, unbreakable portals, removal, warping, the cooldown, names. */
public class WarpGameTest {
	private static ServerPlayer warper(GameTestHelper c, double x, double z) {
		FlightBladeGameTest.enable(c);
		FlightBladeGameTest.floor(c);
		ServerPlayer p = FlightBladeGameTest.survivor(c);
		FlightBladeGameTest.place(c, p, x, 1, z, 0f, 0f);
		TechniqueAttachment.set(p, TechniqueData.DEFAULT.withLevel(Technique.WARP, 1, 10)
			.withEmeralds(TechniqueRules.MAX_EMERALDS).withSlot(0, Technique.WARP));
		return p;
	}

	private static int count(GameTestHelper c, ServerPlayer p) {
		return WarpSavedData.of(c.getLevel().getServer()).get().of(p.getUUID()).size();
	}

	private static PortalRecord portalAt(GameTestHelper c, ServerPlayer owner, double x, double z) {
		Vec3 v = c.absoluteVec(new Vec3(x, 1, z));
		return Warp.spawn(c.getLevel(), owner, v.x, v.y, v.z, 0f);
	}

	private static void cleanUp(GameTestHelper c, ServerPlayer p) {
		for (PortalRecord r : WarpSavedData.of(c.getLevel().getServer()).get().of(p.getUUID())) {
			Warp.remove(p, r.id());
		}
		FlightBladeGameTest.drop(c, p);
	}

	@GameTest(maxTicks = 40)
	public void levelOnePlacesTwoPortalsAndRefusesTheThird(GameTestHelper c) {
		ServerPlayer p = warper(c, 5.5, 5.5);
		p.setXRot(60f);
		c.assertTrue(TechniqueNet.WARP_HOLD_HINT.equals(TechniqueNet.cast(p, 0, 0.5f)), "D79: a half charge is refused with the hint");
		c.assertTrue(count(c, p) == 0, "no portal from a half charge, was " + count(c, p));
		c.assertTrue(TechniqueNet.cast(p, 0, 1f) == null, "the cast is accepted");
		c.assertTrue(count(c, p) == 1, "one portal after the first right click, was " + count(c, p));
		p.setXRot(40f);
		TechniqueNet.cast(p, 0, 1f);
		c.assertTrue(count(c, p) == 2, "two portals after the second, was " + count(c, p));
		p.setXRot(50f);
		TechniqueNet.cast(p, 0, 1f);
		c.assertTrue(count(c, p) == 2, "the third is refused at level 1, was " + count(c, p));
		long now = c.getLevel().getServer().overworld().getGameTime();
		c.assertTrue(!TechniqueAttachment.get(p).onCooldown(Technique.WARP, now), "placing starts no cooldown");
		cleanUp(c, p);
		c.succeed();
	}

	@GameTest(maxTicks = 40)
	public void portalsCannotBeHurtOrBlownUp(GameTestHelper c) {
		ServerPlayer p = warper(c, 8.5, 8.5);
		PortalRecord rec = portalAt(c, p, 4.5, 4.5);
		ServerLevel sl = c.getLevel();
		WarpPortal portal = (WarpPortal) sl.getEntity(rec.id());
		c.assertTrue(portal != null, "the portal entity exists");
		sl.explode(null, rec.x(), rec.y() + 0.5, rec.z(), 6f, Level.ExplosionInteraction.TNT);
		portal.hurtServer(sl, sl.damageSources().playerAttack(p), 1000f);
		portal.hurtServer(sl, sl.damageSources().genericKill(), 1000f);
		p.attack(portal);
		portal.kill(sl);
		c.runAfterDelay(5, () -> {
			c.assertTrue(!portal.isRemoved() && portal.isAlive(), "the portal survived explosion, hits and kill");
			c.assertTrue(WarpSavedData.of(sl.getServer()).get().find(rec.id()) != null, "its list entry is still there");
			cleanUp(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 40)
	public void onlyTheOwnerRemovesAPortalAndThatRemovesTheEntity(GameTestHelper c) {
		ServerPlayer p = warper(c, 8.5, 8.5);
		ServerPlayer other = FlightBladeGameTest.survivor(c);
		PortalRecord rec = portalAt(c, p, 4.5, 4.5);
		c.assertTrue(!Warp.remove(other, rec.id()), "a stranger cannot remove it");
		c.assertTrue(sl(c).getEntity(rec.id()) != null, "still there after the refusal");
		c.assertTrue(Warp.remove(p, rec.id()), "the owner removes it");
		c.assertTrue(WarpSavedData.of(sl(c).getServer()).get().find(rec.id()) == null, "list entry gone");
		c.assertTrue(sl(c).getEntity(rec.id()) == null, "entity gone");
		FlightBladeGameTest.drop(c, other);
		cleanUp(c, p);
		c.succeed();
	}

	private static ServerLevel sl(GameTestHelper c) {
		return c.getLevel();
	}

	@GameTest(maxTicks = 40)
	public void warpingToAPortalMovesYouNearItAndStartsTheCooldown(GameTestHelper c) {
		ServerPlayer p = warper(c, 9.5, 9.5);
		PortalRecord rec = portalAt(c, p, 2.5, 2.5);
		Vec3 portalPos = new Vec3(rec.x(), rec.y(), rec.z());
		Vec3 before = p.position();
		String error = Warp.warpToPortal(p, rec.id());
		c.assertTrue(error == null, "the warp works, got: " + error);
		c.assertTrue(p.position().distanceTo(portalPos) < 3.5, "landed near the portal, distance " + p.position().distanceTo(portalPos));
		long now = c.getLevel().getServer().overworld().getGameTime();
		c.assertTrue(TechniqueAttachment.get(p).onCooldown(Technique.WARP, now), "the cooldown started");
		long left = TechniqueAttachment.get(p).cooldownLeft(Technique.WARP, now);
		c.assertTrue(left == TechniqueRules.cooldownTicks(Technique.WARP, 1), "90 s at level 1, was " + left + " ticks");
		FlightBladeGameTest.place(c, p, 9.5, 1, 9.5, 0f, 0f);
		before = p.position();
		c.assertTrue(Warp.warpToPortal(p, rec.id()) != null, "a second warp during the cooldown is refused");
		c.assertTrue(p.position().distanceTo(before) < 0.01, "and the player did not move");
		cleanUp(c, p);
		c.succeed();
	}

	@GameTest(maxTicks = 40)
	public void warpingToAnotherPlayerWorks(GameTestHelper c) {
		ServerPlayer p = warper(c, 9.5, 9.5);
		ServerPlayer q = FlightBladeGameTest.survivor(c);
		FlightBladeGameTest.place(c, q, 2.5, 1, 3.5, 0f, 0f);
		String error = Warp.warpToPlayer(p, q.getUUID());
		c.assertTrue(error == null, "the warp works, got: " + error);
		c.assertTrue(p.position().distanceTo(q.position()) < 3.5, "landed next to the other player");
		c.assertTrue(Warp.warpToPlayer(p, p.getUUID()) != null, "never to yourself");
		FlightBladeGameTest.drop(c, q);
		cleanUp(c, p);
		c.succeed();
	}

	@GameTest(maxTicks = 40)
	public void aPlayerWithoutTheTechniqueCannotWarp(GameTestHelper c) {
		ServerPlayer p = warper(c, 9.5, 9.5);
		PortalRecord rec = portalAt(c, p, 2.5, 2.5);
		TechniqueAttachment.set(p, TechniqueData.DEFAULT);
		c.assertTrue(Warp.warpToPortal(p, rec.id()) != null, "needs the technique");
		TechniqueAttachment.set(p, TechniqueData.DEFAULT.withLevel(Technique.WARP, 1, 10));
		cleanUp(c, p);
		c.succeed();
	}

	@GameTest(maxTicks = 40)
	public void aPortalGetsABiomeOrStructureName(GameTestHelper c) {
		ServerPlayer p = warper(c, 9.5, 9.5);
		BlockPos pos = c.absolutePos(new BlockPos(4, 1, 4));
		String name = Warp.autoName(c.getLevel(), pos);
		c.assertTrue(name != null && !name.isBlank(), "the name is not empty: '" + name + "'");
		PortalRecord one = portalAt(c, p, 3.5, 3.5);
		PortalRecord two = portalAt(c, p, 5.5, 5.5);
		c.assertTrue(!one.name().equals(two.name()), "the second portal of the same place gets a number: " + one.name() + " / " + two.name());
		cleanUp(c, p);
		c.succeed();
	}
}
