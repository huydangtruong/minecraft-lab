package io.github.huydangtruong.immortalcultivation;

import io.github.huydangtruong.immortalcultivation.client.ClientGrasp;
import io.github.huydangtruong.immortalcultivation.client.ClientSkills;
import io.github.huydangtruong.immortalcultivation.client.GraspScreen;
import io.github.huydangtruong.immortalcultivation.technique.Technique;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueAttachment;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueData;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueRules;
import io.github.huydangtruong.immortalcultivation.technique.grasp.Grasp;
import io.github.huydangtruong.immortalcultivation.technique.grasp.GraspData;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.world.entity.Mob;

/**
 * P-053: Soul Grasp in a real client. Aim at a zombie (outline), hold right click (beam and charge), then the list screen
 * with two held mobs. Screenshots 95-grasp-aim, 96-grasp-tether, 97-grasp-list.
 */
public class GraspClientTest implements FabricClientGameTest {
	private static final int LEFT = 1;
	private static final int RIGHT = 3;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			server.runCommand("gamerule permahearts:immortal_cultivation true");
			server.runCommand("time set midnight"); // night: the undead do not burn, the outline stays visible
			server.runCommand("weather clear");
			server.runCommand("gamemode survival @a");
			server.runCommand("fill -24 99 -24 24 99 24 grass_block");
			server.runCommand("fill -24 100 -24 24 118 24 air");
			server.runCommand("tp @a 0 100 -3 0 0");
			server.runCommand("summon zombie 0 100 4 {NoAI:1b,PersistenceRequired:1b,Silent:1b,active_effects:[{id:'minecraft:fire_resistance',duration:100000,show_particles:0b}]}");
			server.runCommand("summon skeleton 3 100 6 {NoAI:1b,PersistenceRequired:1b,Silent:1b,active_effects:[{id:'minecraft:fire_resistance',duration:100000,show_particles:0b}]}");
			server.runOnServer(s -> TechniqueAttachment.set(s.getPlayerList().getPlayers().get(0),
				TechniqueData.DEFAULT.withLevel(Technique.GRASP, 10, 10)
					.withEmeralds(TechniqueRules.MAX_EMERALDS).withSlot(0, Technique.GRASP)));
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
			context.waitTicks(40);
			context.getInput().pressKey(ClientSkills.KEYS[0]);
			context.waitTicks(15);
			check(context.computeOnClient(mc -> ClientSkills.focusOf(mc.player) == Technique.GRASP), "the focus is in hand");
			context.takeScreenshot("95-grasp-aim");

			// hold right click: the beam and the charge bar
			context.getInput().holdMouse(RIGHT);
			context.waitTicks(12);
			check(context.computeOnClient(mc -> ClientGrasp.tethering()), "the tether is running on the client");
			context.takeScreenshot("96-grasp-tether");
			context.waitTicks(40);
			context.getInput().releaseMouse(RIGHT);
			context.waitTicks(5);
			int held = server.computeOnServer(s -> GraspData.of(s).held(s.getPlayerList().getPlayers().get(0).getUUID()));
			check(held == 1, "the zombie is grasped after the charge, held " + held);

			// a second mob, then the list
			server.runOnServer(s -> {
				var p = s.getPlayerList().getPlayers().get(0);
				for (Mob m : s.overworld().getEntitiesOfClass(net.minecraft.world.entity.monster.skeleton.Skeleton.class,
					p.getBoundingBox().inflate(20))) {
					Grasp.tame(p, m, 10);
				}
			});
			context.waitTicks(5);
			context.getInput().pressMouse(LEFT);
			context.waitTicks(15);
			check(context.computeOnClient(mc -> mc.gui.screen() instanceof GraspScreen), "a left click opens the list");
			context.takeScreenshot("97-grasp-list");
		}
	}

	private static void check(boolean ok, String message) {
		if (!ok) {
			throw new AssertionError(message);
		}
	}
}
