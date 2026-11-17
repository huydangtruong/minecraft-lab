package io.github.huydangtruong.immortalcultivation;

import io.github.huydangtruong.immortalcultivation.technique.Illusion;
import io.github.huydangtruong.immortalcultivation.technique.Technique;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueCasts;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.server.level.ServerPlayer;

/**
 * P-024: two Illusion clones next to the player in a real client (third person), to check that they look like the
 * owner: same skin, same armour and weapon, and the name tag. Screenshot 81-... . Run with ./gradlew :immortal-cultivation:runClientGameTest.
 */
public class IllusionClientTest implements FabricClientGameTest {
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
			server.runCommand("item replace entity @a armor.head with diamond_helmet");
			server.runCommand("item replace entity @a armor.chest with iron_chestplate");
			server.runCommand("item replace entity @a armor.legs with golden_leggings");
			server.runCommand("item replace entity @a armor.feet with netherite_boots");
			server.runCommand("item replace entity @a weapon.mainhand with diamond_sword");
			server.runCommand("item replace entity @a weapon.offhand with shield");
			context.waitTicks(30);
			server.runOnServer(s -> TechniqueCasts.cast(s.getPlayerList().getPlayers().get(0), Technique.ILLUSION, 10, 1f));
			context.waitTicks(5);
			// Freeze the clones in a row beside the player, facing the same way, so the picture is repeatable.
			int n = server.computeOnServer(s -> {
				ServerPlayer p = s.getPlayerList().getPlayers().get(0);
				p.teleportTo(s.overworld(), 0.5, 100, 0.5, java.util.Set.of(), -90f, 10f, false);
				List<Illusion> mine = Illusion.clonesOf(s.overworld(), p.getUUID());
				double[] z = {-1.9, 1.9};
				for (int i = 0; i < mine.size(); i++) {
					Illusion c = mine.get(i);
					c.setNoAi(true);
					c.snapTo(0.5, 100, 0.5 + z[i % 2], -90f, 10f);
					c.setYHeadRot(-90f);
					c.setYBodyRot(-90f);
				}
				return mine.size();
			});
			if (n != 2) {
				throw new AssertionError("level 10 makes 2 clones, was " + n);
			}
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			context.waitTicks(25);
			context.takeScreenshot("81-illusion-third-person");

			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
		}
	}
}
