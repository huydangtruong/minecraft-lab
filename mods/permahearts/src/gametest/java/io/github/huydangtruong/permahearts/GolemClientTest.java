package io.github.huydangtruong.permahearts;

import io.github.huydangtruong.permahearts.client.ClientSkills;
import io.github.huydangtruong.permahearts.technique.Bleeding;
import io.github.huydangtruong.permahearts.technique.Technique;
import io.github.huydangtruong.permahearts.technique.TechniqueAttachment;
import io.github.huydangtruong.permahearts.technique.TechniqueData;
import io.github.huydangtruong.permahearts.technique.TechniqueRules;
import io.github.huydangtruong.permahearts.technique.VioletGuardian;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.world.entity.monster.zombie.Husk;

/**
 * P-020: a Life Steal Blade hit (bleeding, knockback) and the Violet Guardian golems in a real client. R = technique
 * mode, cell 0 = blade, cell 1 = guardian. Screenshots 70-... to 73-... . Run with ./gradlew :permahearts:runClientGameTest.
 */
public class GolemClientTest implements FabricClientGameTest {
	private static final int LEFT = 1;
	private static final int RIGHT = 3;

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
			server.runCommand("tp @a 0 100 0 -90 0");
			server.runOnServer(s -> TechniqueAttachment.set(s.getPlayerList().getPlayers().get(0),
				TechniqueData.DEFAULT.withLevel(Technique.BLADE, 10, 20).withLevel(Technique.GOLEM, 15, 40)
					.withEmeralds(TechniqueRules.MAX_EMERALDS).withSlot(0, Technique.BLADE).withSlot(1, Technique.GOLEM)));
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
			context.waitTicks(40);
			server.runCommand("summon husk 2.2 100 0 {NoAI:1b}");
			context.waitTicks(10);

			// blade: the hit bleeds and throws the husk back
			context.getInput().pressKey(ClientSkills.KEY);
			context.waitTicks(15);
			check(context.computeOnClient(mc -> ClientSkills.focusOf(mc.player) == Technique.BLADE), "the blade is in hand");
			context.getInput().pressMouse(LEFT);
			context.waitTicks(6);
			context.takeScreenshot("70-blade-hit-knockback");
			check(server.computeOnServer(s -> husk(s).hasEffect(Bleeding.EFFECT)), "the husk bleeds");
			context.waitTicks(30);
			double dx = server.computeOnServer(s -> husk(s).getX());
			check(dx > 2.2 + 2.0 && dx < 2.2 + 6.0, "a level 10 hit throws it about 4 blocks, x = " + dx);
			server.runCommand("kill @e[type=husk]");

			// guardian: cell 1, right click casts at once; three guardians at level 15
			context.getInput().pressKey(o -> o.keyHotbarSlots[1]);
			context.waitTicks(5);
			context.getInput().holdMouse(RIGHT);
			context.waitTicks(5);
			context.getInput().releaseMouse(RIGHT);
			context.waitTicks(10);
			int n = server.computeOnServer(s -> VioletGuardian.guardiansOf(s.overworld(), s.getPlayerList().getPlayers().get(0).getUUID()).size());
			check(n == 3, "level 15 summons 3 guardians, was " + n);
			context.takeScreenshot("71-guardians-first-person");
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			context.waitTicks(10);
			context.takeScreenshot("72-guardians-third-person");

			// they fight: a monster appears near the owner
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			server.runCommand("summon husk 6 100 6 {NoAI:1b}");
			context.waitTicks(60);
			context.takeScreenshot("73-guardians-fight");
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
		}
	}

	private static Husk husk(net.minecraft.server.MinecraftServer s) {
		for (var e : s.overworld().getAllEntities()) {
			if (e instanceof Husk h) {
				return h;
			}
		}
		throw new AssertionError("no husk");
	}

	private static void check(boolean ok, String message) {
		if (!ok) {
			throw new AssertionError(message);
		}
	}
}
