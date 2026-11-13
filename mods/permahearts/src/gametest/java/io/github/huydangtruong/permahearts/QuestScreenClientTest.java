package io.github.huydangtruong.permahearts;

import io.github.huydangtruong.permahearts.wanderer.DeathMatch;
import io.github.huydangtruong.permahearts.wanderer.DeathMatchRules;
import io.github.huydangtruong.permahearts.wanderer.Quest;
import io.github.huydangtruong.permahearts.wanderer.QuestLine;
import io.github.huydangtruong.permahearts.wanderer.QuestType;
import io.github.huydangtruong.permahearts.wanderer.Wanderer;
import io.github.huydangtruong.permahearts.wanderer.WandererMenu;
import io.github.huydangtruong.permahearts.wanderer.QuestBoss;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.server.level.ServerPlayer;

/**
 * Not a check: opens a real client in a throwaway world and takes screenshots of the Wanderer quest screen so a
 * human can look at them. Run with ./gradlew :permahearts:runClientGameTest. Fails only on exceptions.
 */
public class QuestScreenClientTest implements FabricClientGameTest {
	private static Quest quest() {
		return new Quest(1, List.of(
			new QuestLine(QuestType.COLLECT, "minecraft:iron_ingot", 64, 0, "strength", false),
			new QuestLine(QuestType.DEATH_MATCH, DeathMatchRules.TARGET, 1, 0, "fury", false),
			new QuestLine(QuestType.BLOOD_MOON, "minecraft:piglin_brute", 1, 0, "swiftness", false),
			new QuestLine(QuestType.BOSS, "minecraft:evoker", 1, 0, "iron_skin", false)), false);
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			server.runCommand("gamerule permahearts:immortal_cultivation true");
			server.runCommand("time set day");
			server.runCommand("weather clear");
			context.waitTicks(40);
			shots(context, server, ""); // the mod's text is English only (D16)
			bossesLiveOnTheSurface(context, server);
			caveBoss(context, server);
		}
	}

	private static Quest bossQuest(String type) {
		return new Quest(2, List.of(new QuestLine(QuestType.BOSS, "minecraft:" + type, 1, 0, "iron_skin", false)), false);
	}

	private static List<? extends Mob> bosses(net.minecraft.server.MinecraftServer s) {
		return s.overworld().getEntities(EntityTypeTest.forClass(Mob.class), m -> m.entityTags().contains(QuestBoss.TAG));
	}

	/**
	 * Each boss type is summoned next to the player on the flat surface (the setting of the 0.7.0 report), must still be
	 * alive after 200 ticks, and a boss that is removed by hand must free the Summon button. Unload events are logged.
	 */
	private static void bossesLiveOnTheSurface(ClientGameTestContext context, TestServerContext server) {
		context.runOnClient(mc -> mc.player.closeContainer());
		// (runCommand has no player entity, so "@s" selects nobody: set the mode on the server player directly.)
		server.runOnServer(s -> s.getPlayerList().getPlayers().get(0).setGameMode(net.minecraft.world.level.GameType.CREATIVE));
		for (String type : new String[] {"zombie", "skeleton", "evoker", "blaze", "vindicator"}) {
			server.runOnServer(s -> {
				Wanderer.reset(s);
				ServerPlayer p = s.getPlayerList().getPlayers().get(0);
				Wanderer.startQuestForTest(p.level(), p.blockPosition().relative(p.getDirection(), 3), bossQuest(type));
				Wanderer.accept(s);
				if (!Wanderer.startEvent(s, 0, p)) {
					throw new AssertionError("summon refused for " + type);
				}
			});
			context.waitTicks(200);
			String result = server.computeOnServer(s -> {
				if (!QuestBoss.bossAlive(s)) {
					return "boss not alive after 200 ticks";
				}
				if (Wanderer.eventState(s, 0) != 1) {
					return "eventState should be 1";
				}
				// Daylight, open sky: the boss must not burn or lose health to the sun.
				for (Mob b : bosses(s)) {
					if (b.isOnFire() || b.getHealth() < b.getMaxHealth()) {
						return "boss burned or took damage in daylight (fire=" + b.isOnFire() + ", hp=" + b.getHealth() + ")";
					}
				}
				bosses(s).forEach(Entity::discard);
				if (Wanderer.eventState(s, 0) != 0) {
					return "eventState should be 0 after the boss is removed";
				}
				ServerPlayer p = s.getPlayerList().getPlayers().get(0);
				if (!Wanderer.startEvent(s, 0, p)) {
					return "second summon refused";
				}
				return bosses(s).size() == 1 ? "" : "expected one new boss, found " + bosses(s).size();
			});
			if (!result.isEmpty()) {
				throw new AssertionError("Boss type " + type + ": " + result);
			}
		}
		server.runOnServer(Wanderer::reset);
	}

	/** Stone box around an air room, far above the flat world: the boss must appear inside the room, not on top of it. */
	private static void caveBoss(ClientGameTestContext context, TestServerContext server) {
		int[] c = server.computeOnServer(s -> {
			BlockPos b = s.getPlayerList().getPlayers().get(0).blockPosition();
			return new int[] {b.getX(), b.getY() + 40, b.getZ()};
		});
		int x = c[0];
		int y = c[1];
		int z = c[2];
		// Floor at y-1, room y..y+5 (6 high, 11x11), 8 blocks of stone roof above it.
		server.runCommand("fill " + (x - 8) + " " + (y - 1) + " " + (z - 8) + " " + (x + 8) + " " + (y + 13) + " " + (z + 8) + " stone");
		server.runCommand("fill " + (x - 5) + " " + y + " " + (z - 5) + " " + (x + 5) + " " + (y + 5) + " " + (z + 5) + " air");
		// Light: a glowstone ceiling so the screenshot shows the boss.
		server.runCommand("fill " + (x - 5) + " " + (y + 5) + " " + (z - 5) + " " + (x + 5) + " " + (y + 5) + " " + (z + 5) + " glowstone");
		server.runCommand("tp @p " + (x + 0.5) + " " + y + " " + (z + 4.5) + " 180 0");
		context.waitTicks(10);
		double[] r = server.computeOnServer(s -> {
			ServerPlayer p = s.getPlayerList().getPlayers().get(0);
			Wanderer.reset(s);
			Entity npc = Wanderer.startQuestForTest(p.level(), new BlockPos(x, y, z), bossQuest("vindicator"));
			Wanderer.accept(s);
			if (!Wanderer.startEvent(s, 0, p)) {
				throw new AssertionError("cave summon refused");
			}
			Mob boss = bosses(s).get(0);
			return new double[] {boss.getX(), boss.getY(), boss.getZ(), npc.getY(), Math.hypot(boss.getX() - npc.getX(), boss.getZ() - npc.getZ())};
		});
		if (Math.abs(r[1] - r[3]) > 4 || r[4] > 8) {
			throw new AssertionError("Cave boss is not near the NPC: boss y=" + r[1] + " npc y=" + r[3] + " horizontal=" + r[4]);
		}
		// Step back to a corner of the room so the boss fits in the picture.
		server.runCommand("tp @p " + (x - 4.5) + " " + y + " " + (z - 4.5));
		context.waitTicks(10);
		// Aim at the boss where it stands now (it walks around).
		double[] now = server.computeOnServer(s -> {
			Mob boss = bosses(s).get(0);
			return new double[] {boss.getX(), boss.getY() + boss.getBbHeight() / 2, boss.getZ()};
		});
		context.runOnClient(mc -> {
			double dx = now[0] - mc.player.getX();
			double dy = now[1] - mc.player.getEyeY();
			double dz = now[2] - mc.player.getZ();
			mc.player.setYRot((float) (Math.toDegrees(Math.atan2(-dx, dz))));
			mc.player.setXRot((float) (-Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)))));
		});
		context.waitTicks(3);
		context.takeScreenshot("05-cave-boss");
		server.runOnServer(Wanderer::reset);
	}

	private static void shots(ClientGameTestContext context, TestServerContext server, String suffix) {
		context.runOnClient(mc -> mc.player.closeContainer());
		float[] yawAtSpawn = new float[1];
		server.runOnServer(s -> {
			Wanderer.reset(s);
			ServerPlayer p = s.getPlayerList().getPlayers().get(0);
			yawAtSpawn[0] = p.getYRot();
			Wanderer.startQuestForTest(p.level(), p.blockPosition().relative(p.getDirection(), 3), quest());
		});
		// The Wanderer stands 3 blocks ahead of the player's facing at spawn time; aim straight at it.
		context.runOnClient(mc -> {
			mc.player.setYRot(yawAtSpawn[0]);
			mc.player.setXRot(5f);
		});
		context.waitTicks(20);
		context.takeScreenshot("00-world-view" + suffix);
		context.getInput().pressKey(o -> o.keyUse); // right-click
		context.waitForScreen(AbstractContainerScreen.class);
		context.waitTicks(10);
		context.takeScreenshot("01-quest-offer" + suffix);

		context.runOnClient(mc -> mc.gameMode.handleInventoryButtonClick(mc.player.containerMenu.containerId, 0));
		context.waitTicks(20);
		context.takeScreenshot("02-quest-lines" + suffix);

		// Death Match (line 2, row index 1): one player online, so the Start button is inactive; hover it for the reason.
		double[] dm = context.computeOnClient(mc -> {
			double scale = mc.getWindow().getGuiScale();
			int left = (mc.getWindow().getGuiScaledWidth() - WandererMenu.PANEL_W) / 2;
			int top = (mc.getWindow().getGuiScaledHeight() - WandererMenu.PANEL_H) / 2;
			return new double[] {(left + WandererMenu.QUEST_W - 6 - 31) * scale,
				(top + WandererMenu.LINE_SLOT_Y0 + WandererMenu.LINE_STEP + 8) * scale};
		});
		context.getInput().setCursorPos(dm[0], dm[1]);
		context.waitTicks(30);
		context.takeScreenshot("02b-death-match-needs-players" + suffix);
		// Pretend three players are online (a second real client is not available here), then press Start.
		server.runOnServer(s -> DeathMatch.setOnlineForTest(3));
		context.waitTicks(5);
		context.runOnClient(mc -> mc.gameMode.handleInventoryButtonClick(mc.player.containerMenu.containerId, 11));
		context.waitTicks(20);
		context.getInput().setCursorPos(10, 10);
		context.waitTicks(5);
		context.takeScreenshot("02c-death-match-running" + suffix);
		server.runOnServer(s -> DeathMatch.setOnlineForTest(null));

		// First Give up click: the label turns into the confirm text (a second click would send the NPC away).
		double[] giveUp = context.computeOnClient(mc -> {
			double scale = mc.getWindow().getGuiScale();
			int left = (mc.getWindow().getGuiScaledWidth() - WandererMenu.PANEL_W) / 2;
			int top = (mc.getWindow().getGuiScaledHeight() - WandererMenu.PANEL_H) / 2;
			return new double[] {(left + WandererMenu.QUEST_W / 2.0) * scale, (top + WandererMenu.GIVE_UP_Y + WandererMenu.GIVE_UP_H / 2.0) * scale};
		});
		context.getInput().setCursorPos(giveUp[0], giveUp[1]);
		context.waitTicks(2);
		// (left click is button 1 in this API; deliver it to the screen directly, in GUI coordinates)
		context.runOnClient(mc -> {
			double scale = mc.getWindow().getGuiScale();
			mc.gui.screen().mouseClicked(new net.minecraft.client.input.MouseButtonEvent(giveUp[0] / scale, giveUp[1] / scale,
				new net.minecraft.client.input.MouseButtonInfo(1, 0)), false);
		});
		context.waitTicks(5);
		context.takeScreenshot("06-give-up-confirm" + suffix);
		context.waitTicks(70); // the confirm label times out again

		context.runOnClient(mc -> mc.player.closeContainer());
		server.runCommand("quest complete 1");
		context.waitTicks(5);
		context.getInput().pressKey(o -> o.keyUse);
		context.waitForScreen(AbstractContainerScreen.class);
		context.waitTicks(10);
		context.takeScreenshot("03-line-completed" + suffix);

		// Hover the reward slot (the first slot of the menu) so its tooltip shows.
		double[] cursor = context.computeOnClient(mc -> {
			net.minecraft.world.inventory.Slot slot = mc.player.containerMenu.slots.get(0);
			double scale = mc.getWindow().getGuiScale();
			double left = (mc.getWindow().getGuiScaledWidth() - WandererMenu.PANEL_W) / 2.0;
			double top = (mc.getWindow().getGuiScaledHeight() - WandererMenu.PANEL_H) / 2.0;
			return new double[] {(left + slot.x + 8) * scale, (top + slot.y + 8) * scale};
		});
		context.getInput().setCursorPos(cursor[0], cursor[1]);
		context.waitTicks(10);
		context.takeScreenshot("04-reward-tooltip" + suffix);
		context.runOnClient(mc -> mc.player.closeContainer());
	}
}
