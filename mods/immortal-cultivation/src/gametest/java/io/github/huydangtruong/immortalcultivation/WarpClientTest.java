package io.github.huydangtruong.immortalcultivation;

import com.mojang.authlib.GameProfile;
import io.github.huydangtruong.immortalcultivation.client.ClientSkills;
import io.github.huydangtruong.immortalcultivation.client.WarpScreen;
import io.github.huydangtruong.immortalcultivation.technique.Technique;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueAttachment;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueData;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueRules;
import io.github.huydangtruong.immortalcultivation.technique.warp.WarpSavedData;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.CameraType;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;

/**
 * P-026: Cosmic Warping in a real client. Right click twice places two portals; a stand-in second player is online;
 * a left click opens the menu. D276: the pencil opens the rename box; typing and Enter renames the portal.
 * Screenshots 90-warp-menu, 91-warp-portal, 92-warp-rename.
 */
public class WarpClientTest implements FabricClientGameTest {
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
			server.runCommand("fill -24 99 -24 24 99 24 grass_block");
			server.runCommand("fill -24 100 -24 24 118 24 air");
			server.runCommand("tp @a[name=!Friend] 0 100 -3 0 50");
			server.runOnServer(s -> TechniqueAttachment.set(s.getPlayerList().getPlayers().get(0),
				TechniqueData.DEFAULT.withLevel(Technique.WARP, 1, 10)
					.withEmeralds(TechniqueRules.MAX_EMERALDS).withSlot(0, Technique.WARP)));
			server.runOnServer(s -> {
				GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes("steve-friend".getBytes()), "Friend");
				CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
				ServerPlayer other = new ServerPlayer(s, s.overworld(), cookie.gameProfile(), cookie.clientInformation()) {
					@Override
					public GameType gameMode() {
						return GameType.SURVIVAL;
					}
				};
				Connection connection = new Connection(PacketFlow.SERVERBOUND);
				new EmbeddedChannel(connection);
				s.getPlayerList().placeNewPlayer(connection, other, cookie);
				other.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
				other.teleportTo(s.overworld(), 8.5, 100, 6.5, java.util.Set.of(), 0f, 0f, false);
			});
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
			context.waitTicks(40);
			context.getInput().pressKey(ClientSkills.KEYS[0]);
			context.waitTicks(15);
			check(context.computeOnClient(mc -> ClientSkills.focusOf(mc.player) == Technique.WARP), "the focus is in hand");

			// D79: a short right click places nothing; then two 1 s holds place two portals, the third is refused
			rightClick(context, 4);
			context.waitTicks(10);
			int none = server.computeOnServer(s -> WarpSavedData.of(s).get().portals().size());
			check(none == 0, "a short click must not place a portal, portals: " + none);
			context.takeScreenshot("89-warp-short-click-hint");
			rightClick(context);
			context.waitTicks(10);
			server.runCommand("tp @a[name=!Friend] 0 100 -3 0 36");
			context.waitTicks(5);
			rightClick(context);
			context.waitTicks(10);
			server.runCommand("tp @a[name=!Friend] 0 100 -3 0 25");
			context.waitTicks(5);
			rightClick(context);
			context.waitTicks(10);
			int portals = server.computeOnServer(s -> WarpSavedData.of(s).get().portals().size());
			check(portals == 2, "two portals at level 1, was " + portals);

			// the portals in the world
			server.runCommand("tp @a[name=!Friend] 0 100 -6 0 8");
			context.waitTicks(30);
			context.takeScreenshot("91-warp-portal");

			// left click: the menu
			context.getInput().pressMouse(LEFT);
			context.waitTicks(15);
			check(context.computeOnClient(mc -> mc.gui.screen() instanceof WarpScreen), "a left click opens the warp menu");
			context.takeScreenshot("90-warp-menu");

			// D276: the rename box (opened as the pencil does), clear the name, type a new one, Enter
			context.runOnClient(mc -> {
				WarpScreen ws = (WarpScreen) mc.gui.screen();
				ws.openRename(ws.firstPortal());
			});
			context.waitTicks(5);
			for (int i = 0; i < 40; i++) {
				context.getInput().pressKey(InputConstants.KEY_BACKSPACE);
			}
			context.getInput().typeChars("Nhà của tôi");
			context.waitTicks(5);
			context.takeScreenshot("92-warp-rename");
			context.getInput().pressKey(InputConstants.KEY_RETURN);
			context.waitTicks(15);
			String renamed = server.computeOnServer(s -> WarpSavedData.of(s).get().portals().stream()
				.sorted(java.util.Comparator.comparingLong(io.github.huydangtruong.immortalcultivation.technique.warp.PortalRecord::created)).findFirst().orElseThrow().name());
			check("Nhà của tôi".equals(renamed), "the first portal is renamed, was " + renamed);
			context.takeScreenshot("93-warp-renamed");
		}
	}

	/** A right click held long enough to place a portal (D79: 1 s, plus a little). */
	private static void rightClick(ClientGameTestContext context) {
		rightClick(context, 24);
	}

	private static void rightClick(ClientGameTestContext context, int ticks) {
		context.getInput().holdMouse(RIGHT);
		context.waitTicks(ticks);
		context.getInput().releaseMouse(RIGHT);
	}

	private static void check(boolean ok, String message) {
		if (!ok) {
			throw new AssertionError(message);
		}
	}
}
