package io.github.huydangtruong.immortalcultivation;

import io.github.huydangtruong.immortalcultivation.client.ClientSkills;
import io.github.huydangtruong.immortalcultivation.technique.Seism;
import io.github.huydangtruong.immortalcultivation.technique.Technique;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueAttachment;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueData;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueRules;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;

/**
 * P-056: Seism in a real client. Screenshots 110-seism-boulder (mid-fall), 111-seism-impact, 112-seism-wave (spikes) and
 * 113-seism-techniques-tab. A real left click casts the Impact.
 */
public class SeismClientTest implements FabricClientGameTest {
	private static final int LEFT = 1;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			server.runCommand("gamerule permahearts:immortal_cultivation true");
			server.runCommand("time set day");
			server.runCommand("weather clear");
			server.runCommand("gamemode survival @a");
			server.runCommand("fill -24 99 -24 24 99 24 grass_block");
			server.runCommand("fill -24 100 -24 24 118 24 air");
			server.runCommand("tp @a 0 100 0 0 8");
			server.runOnServer(s -> TechniqueAttachment.set(s.getPlayerList().getPlayers().get(0),
				TechniqueData.DEFAULT.withLevel(Technique.SEISM, 10, 10)
					.withEmeralds(TechniqueRules.MAX_EMERALDS).withSlot(0, Technique.SEISM)));
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
			context.waitTicks(40);
			context.getInput().pressKey(ClientSkills.KEYS[0]);
			context.waitTicks(15);
			check(context.computeOnClient(mc -> ClientSkills.focusOf(mc.player) == Technique.SEISM), "the focus is in hand");

			// Impact: three husks in front of the player, then a real left click
			server.runCommand("summon husk 0 100 11 {NoAI:1b,Tags:[\"seism_t\"]}");
			server.runCommand("summon husk 2 100 12 {NoAI:1b,Tags:[\"seism_t\"]}");
			context.waitTicks(5);
			context.getInput().pressMouse(LEFT);
			context.waitTicks(2);
			context.takeScreenshot("110-seism-boulder");
			context.waitTicks(3);
			context.takeScreenshot("111-seism-impact");
			boolean hurt = server.computeOnServer(s -> s.overworld().getEntities(net.minecraft.world.entity.EntityTypes.HUSK, e -> e.getHealth() < e.getMaxHealth()).size() >= 1);
			check(hurt, "the boulder hurt a husk");
			boolean cooling = server.computeOnServer(s -> TechniqueAttachment.get(s.getPlayerList().getPlayers().get(0))
				.onCooldown(Technique.SEISM, s.overworld().getGameTime()));
			check(cooling, "the cooldown started at the cast");

			// Wave: cast directly (the real input is a 1 s hold), third person to see the spikes around the caster
			server.runCommand("kill @e[tag=seism_t]");
			server.runOnServer(s -> {
				var p = s.getPlayerList().getPlayers().get(0);
				TechniqueAttachment.set(p, TechniqueAttachment.get(p).withCooldown(Technique.SEISM, 0));
				Seism.wave(p, 10);
			});
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			context.waitTicks(3);
			context.takeScreenshot("112-seism-wave");
			context.waitTicks(70);
			boolean slowGone = server.computeOnServer(s -> !Seism.slowed(s.getPlayerList().getPlayers().get(0)));
			check(slowGone, "the slow is gone after the Wave");
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));

			// the Techniques tab with Seism in the list
			context.waitTicks(5);
			context.setScreen(() -> new net.minecraft.client.gui.screens.inventory.InventoryScreen(
				net.minecraft.client.Minecraft.getInstance().player));
			context.waitTicks(5);
			context.clickScreenButton("Cultivation");
			context.waitTicks(3);
			context.clickScreenButton("Techniques");
			context.waitTicks(5);
			context.takeScreenshot("113-seism-techniques-tab");
			context.runOnClient(mc -> mc.player.closeContainer());
		}
	}

	private static void check(boolean ok, String message) {
		if (!ok) {
			throw new AssertionError(message);
		}
	}
}
