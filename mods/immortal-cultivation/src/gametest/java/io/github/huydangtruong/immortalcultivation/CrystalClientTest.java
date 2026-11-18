package io.github.huydangtruong.immortalcultivation;

import io.github.huydangtruong.immortalcultivation.client.ClientSkills;
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
 * P-056: Crystal Prison in a real client. Screenshots 120-crystal-cage (first person, just after the left click),
 * 121-crystal-cage-night (third person at night: the crystal glows) and 122-crystal-techniques-tab. A real left click casts.
 */
public class CrystalClientTest implements FabricClientGameTest {
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
				TechniqueData.DEFAULT.withLevel(Technique.CRYSTAL, 10, 10)
					.withEmeralds(TechniqueRules.MAX_EMERALDS).withSlot(0, Technique.CRYSTAL)));
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
			context.waitTicks(40);
			context.getInput().pressKey(ClientSkills.KEYS[0]);
			context.waitTicks(15);
			check(context.computeOnClient(mc -> ClientSkills.focusOf(mc.player) == Technique.CRYSTAL), "the focus is in hand");

			server.runCommand("summon husk 0 100 7 {NoAI:1b,Tags:[\"crystal_t\"]}");
			context.waitTicks(5);
			context.getInput().pressMouse(LEFT);
			context.waitTicks(4);
			context.takeScreenshot("120-crystal-cage");
			int crystals = server.computeOnServer(s -> {
				int n = 0;
				for (int x = -3; x <= 3; x++) {
					for (int y = 98; y <= 105; y++) {
						for (int z = 4; z <= 10; z++) {
							if (s.overworld().getBlockState(new net.minecraft.core.BlockPos(x, y, z)).getBlock() instanceof io.github.huydangtruong.immortalcultivation.technique.CrystalBlock) {
								n++;
							}
						}
					}
				}
				return n;
			});
			check(crystals == 54, "54 crystal blocks stand, found " + crystals);
			boolean caged = server.computeOnServer(s -> !s.overworld().getEntities(net.minecraft.world.entity.EntityTypes.HUSK, e -> e.hasEffect(io.github.huydangtruong.immortalcultivation.technique.CrystalDisarm.EFFECT)).isEmpty());
			check(caged, "the husk is caged and disarmed");

			server.runCommand("time set midnight");
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			context.waitTicks(20);
			context.takeScreenshot("121-crystal-cage-night");
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));

			context.waitTicks(5);
			context.setScreen(() -> new net.minecraft.client.gui.screens.inventory.InventoryScreen(
				net.minecraft.client.Minecraft.getInstance().player));
			context.waitTicks(5);
			context.clickScreenButton("Cultivation");
			context.waitTicks(3);
			context.clickScreenButton("Techniques");
			context.waitTicks(5);
			context.takeScreenshot("122-crystal-techniques-tab");
			context.runOnClient(mc -> mc.player.closeContainer());
		}
	}

	private static void check(boolean ok, String message) {
		if (!ok) {
			throw new AssertionError(message);
		}
	}
}
