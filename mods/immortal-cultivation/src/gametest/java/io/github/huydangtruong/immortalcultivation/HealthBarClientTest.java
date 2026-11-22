package io.github.huydangtruong.immortalcultivation;

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
 * P-071 (D202, D203): the one-heart health bar (full, half, layered 60 HP, absorption + poison) and the Soul Grasp icon in the
 * skill bar. Screenshots 140-... .
 */
public class HealthBarClientTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			server.runCommand("gamerule permahearts:immortal_cultivation true");
			server.runCommand("time set day");
			server.runCommand("weather clear");
			server.runCommand("gamemode survival @a");
			server.runCommand("fill -24 99 -24 24 99 24 stone");
			server.runCommand("fill -24 100 -24 24 118 24 air");
			server.runCommand("tp @a 0 100 0 -90 10");
			server.runOnServer(s -> TechniqueAttachment.set(s.getPlayerList().getPlayers().get(0),
				TechniqueData.DEFAULT.withLevel(Technique.GRASP, 5, 20).withLevel(Technique.FIRE, 3, 10).withEmeralds(TechniqueRules.MAX_EMERALDS)
					.withSlot(0, Technique.FIRE).withSlot(1, Technique.GRASP)));
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
			server.runCommand("item replace entity @a armor.chest with diamond_chestplate");
			server.runCommand("item replace entity @a armor.head with diamond_helmet");
			server.runCommand("item replace entity @a armor.legs with diamond_leggings");
			server.runCommand("item replace entity @a armor.feet with diamond_boots");
			server.runCommand("item replace entity @a hotbar.0 with iron_sword");
			server.runCommand("item replace entity @a hotbar.1 with bread 8");
			context.waitTicks(30);
			shot(context, "140-health-full-20");

			health(server, 10f);
			context.waitTicks(8);
			shot(context, "141-health-half-20");

			health(server, 3f);
			context.waitTicks(8);
			shot(context, "142-health-low-3");

			hearts(server, 30); // 60 HP: layer 1
			health(server, 60f);
			context.waitTicks(8);
			shot(context, "143-health-layer-1-of-60");
			health(server, 25f);
			context.waitTicks(8);
			shot(context, "144-health-layer-0-of-60");

			hearts(server, 100); // 200 HP: layer 4 at full
			health(server, 200f);
			context.waitTicks(8);
			shot(context, "145-health-layer-4-of-200");

			hearts(server, 10);
			health(server, 14f);
			server.runCommand("effect give @a minecraft:absorption 100 4 true");
			server.runCommand("effect give @a minecraft:poison 100 0 true");
			health(server, 14f);
			context.waitTicks(8);
			health(server, 14f);
			context.waitTicks(2);
			shot(context, "146-health-absorption-poison");
			server.runCommand("effect clear @a minecraft:poison");

			// the Soul Grasp icon in the skill bar (slot 2 = Z X C V: X)
			server.runCommand("effect clear @a");
			health(server, 20f);
			context.waitTicks(10);
			shot(context, "147-skill-bar-soul-grasp-icon");
		}
	}

	/** The chat would cover the armour bar: clear it before every shot. */
	private static void shot(ClientGameTestContext context, String name) {
		context.runOnClient(mc -> mc.gui.hud.getChat().clearMessages(false));
		context.waitTicks(1);
		context.takeScreenshot(name);
	}

	private static void hearts(TestServerContext server, int hearts) {
		server.runOnServer(s -> {
			net.minecraft.server.level.ServerPlayer p = s.getPlayerList().getPlayers().get(0);
			HeartsAttachment.set(p, HeartRules.withHearts(HeartsData.DEFAULT, hearts));
			HealthApplier.apply(p, true);
		});
	}

	private static void health(TestServerContext server, float hp) {
		server.runOnServer(s -> s.getPlayerList().getPlayers().get(0).setHealth(hp));
	}
}
