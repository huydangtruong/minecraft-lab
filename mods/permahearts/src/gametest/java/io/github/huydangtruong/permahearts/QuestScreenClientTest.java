package io.github.huydangtruong.permahearts;

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
			new QuestLine(QuestType.KILL, "minecraft:zombie", 16, 0, "fury", false),
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
			announcement(context, server);
			bossesLiveOnTheSurface(context, server);
			caveBoss(context, server);
			statsPanel(context, server);
			bloodMoonEdge(context, server);
			layeredHearts(context, server);
		}
	}

	/** D26: when a visit is planned every player gets a big title and a boss bar with the coordinates; leaving hides the bar. */
	private static void announcement(ClientGameTestContext context, TestServerContext server) {
		context.runOnClient(mc -> mc.player.closeContainer());
		server.runOnServer(s -> {
			Wanderer.reset(s);
			Wanderer.plan(s, s.getPlayerList().getPlayers().get(0));
		});
		context.waitTicks(25);
		context.takeScreenshot("07-master-announced");
		String bar = server.computeOnServer(s -> io.github.huydangtruong.permahearts.wanderer.WandererBar.text());
		if (!bar.startsWith("Crimson Master: X ")) {
			throw new AssertionError("boss bar missing or wrong: \"" + bar + "\"");
		}
		server.runOnServer(s -> Wanderer.leave(s, net.minecraft.network.chat.Component.literal("test")));
		context.waitTicks(25);
		if (server.computeOnServer(s -> io.github.huydangtruong.permahearts.wanderer.WandererBar.shown())) {
			throw new AssertionError("boss bar still shown after the Crimson Master left");
		}
		server.runOnServer(Wanderer::reset);
	}

	/**
	 * D31: the inventory gets a stats button next to the recipe book button; it opens a panel on the left with each stat
	 * as "total (base +gear +cultivation)", food healing and the respawn point.
	 */
	private static void statsPanel(ClientGameTestContext context, TestServerContext server) {
		context.runOnClient(mc -> mc.player.closeContainer());
		server.runCommand("gamemode survival @a"); // the creative inventory is another screen
		server.runCommand("clear @a");
		server.runCommand("give @a diamond_sword");
		server.runCommand("give @a iron_chestplate");
		server.runCommand("spawnpoint @a 120 70 -45");
		server.runCommand("effect give @a speed 120 0 true");
		server.runOnServer(s -> {
			ServerPlayer p = s.getPlayerList().getPlayers().get(0);
			HeartsAttachment.set(p, new HeartsData(5, 0, 0, 0));
			HealthApplier.apply(p, true);
			HeartsAttachment.setCultivation(p, CultivationData.DEFAULT.plus(ElixirStat.STRENGTH, 2).plus(ElixirStat.FURY, 1)
				.plus(ElixirStat.SWIFTNESS, 2).plus(ElixirStat.REACH, 1).plus(ElixirStat.VITALITY, 2));
			StatApplier.apply(p);
		});
		context.waitTicks(30); // stats sync once a second
		context.setScreen(() -> new net.minecraft.client.gui.screens.inventory.InventoryScreen(
			net.minecraft.client.Minecraft.getInstance().player));
		context.waitTicks(5);
		context.takeScreenshot("08-inventory-stats-button");
		context.clickScreenButton("Cultivation");
		context.waitTicks(5);
		boolean open = context.computeOnClient(mc -> {
			try {
				return (Boolean) Class.forName("io.github.huydangtruong.permahearts.client.StatsPanel").getMethod("isOpen").invoke(null);
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		});
		if (!open) {
			throw new AssertionError("the cultivation button did not open the panel");
		}
		context.takeScreenshot("09-stats-panel");
		// D39: the second tab (techniques) is empty for now
		context.clickScreenButton("Techniques");
		context.waitTicks(5);
		String tab = context.computeOnClient(mc -> {
			try {
				return String.valueOf(Class.forName("io.github.huydangtruong.permahearts.client.StatsPanel").getMethod("tab").invoke(null));
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		});
		if (!tab.equals("TECHNIQUES")) {
			throw new AssertionError("the Techniques tab was not selected: " + tab);
		}
		context.takeScreenshot("09b-techniques-tab");
		context.clickScreenButton("Stats");
		context.waitTicks(2);
		context.runOnClient(mc -> mc.player.closeContainer());
		server.runCommand("gamemode creative @a");
	}

	/** D36: more than 20 hearts stay in 2 rows, extra hearts in other colours over the first ones; armour sits above. */
	private static void layeredHearts(ClientGameTestContext context, TestServerContext server) {
		context.runOnClient(mc -> mc.player.closeContainer());
		server.runCommand("gamemode survival @a");
		server.runCommand("effect clear @a");
		server.runCommand("item replace entity @a armor.chest with iron_chestplate");
		server.runCommand("item replace entity @a armor.head with iron_helmet");
		hearts(context, server, 57, -1);
		context.takeScreenshot("12-hearts-57-full");
		hearts(context, server, 57, 83); // 41.5 hearts left: layer 3 gone, half of heart 42
		context.takeScreenshot("13-hearts-57-hurt");
		hearts(context, server, 100, -1);
		context.takeScreenshot("14-hearts-100-full");
		hearts(context, server, 150, -1); // D37: pastels, baby pink over peach
		context.takeScreenshot("15-hearts-150-full");
		hearts(context, server, 300, 461); // 230.5 hearts: galaxy over cherry blossom
		context.takeScreenshot("16-hearts-230-galaxy");
		hearts(context, server, 300, 541); // 270.5 hearts: soul fire over green fire
		context.takeScreenshot("17-hearts-270-soul-fire");
		hearts(context, server, 300, 501); // 250.5 hearts: green fire over galaxy
		context.takeScreenshot("17b-hearts-250-green-fire");
		hearts(context, server, 300, -1); // all crimson lava
		context.takeScreenshot("18-hearts-300-lava");
		int rows = server.computeOnServer(s -> io.github.huydangtruong.permahearts.HeartLayers.height(
			s.getPlayerList().getPlayers().get(0).getMaxHealth(), 0));
		if (rows != 20) {
			throw new AssertionError("300 hearts should use 2 rows (20 px), got " + rows);
		}
		hearts(context, server, 10, -1);
		server.runCommand("gamemode creative @a");
	}

	/** Sets max hearts (health full when health < 0, else that many health points) and waits for the client. */
	private static void hearts(ClientGameTestContext context, TestServerContext server, int hearts, int health) {
		server.runOnServer(s -> {
			ServerPlayer p = s.getPlayerList().getPlayers().get(0);
			HeartsAttachment.set(p, io.github.huydangtruong.permahearts.HeartRules.withHearts(HeartsData.DEFAULT, hearts));
			HealthApplier.apply(p, true);
			if (health >= 0) {
				p.setHealth(health);
			}
		});
		context.waitTicks(10);
		context.runOnClient(mc -> mc.gui.hud.getChat().clearMessages(false)); // chat would cover the second row
		context.waitTicks(2);
	}

	/** D33: during a Blood Moon the 96 x 96 arena edge around the Crimson Master shows as red dust near the player. */
	private static void bloodMoonEdge(ClientGameTestContext context, TestServerContext server) {
		server.runCommand("gamemode survival @a");
		server.runCommand("difficulty peaceful"); // no attackers in the picture: only the edge
		server.runOnServer(s -> {
			ServerPlayer p = s.getPlayerList().getPlayers().get(0);
			Wanderer.startQuestForTest(s.overworld(), p.blockPosition(), new Quest(3, List.of(
				new QuestLine(QuestType.BLOOD_MOON, "minecraft:zombie", 1, 0, "strength", false)), false));
			Wanderer.accept(s);
			Wanderer.startEvent(s, 0, p);
			var d = Wanderer.data(s);
			int x = d.x() + 44;
			int y = s.overworld().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, d.z());
			p.teleportTo(s.overworld(), x + 0.5, y, d.z() + 0.5, java.util.Set.of(), -90f, 10f, false);
		});
		context.waitTicks(60);
		context.takeScreenshot("10-blood-moon-edge");
		server.runOnServer(s -> {
			io.github.huydangtruong.permahearts.wanderer.BloodMoon.moveClock(s, false);
			Wanderer.reset(s);
		});
		server.runCommand("difficulty normal");
		server.runCommand("gamemode creative @a");
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
		for (String type : new String[] {"zombie", "skeleton", "evoker", "blaze", "vindicator", "ghast"}) {
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
			if (type.equals("ghast")) {
				ghastShot(context, server);
			}
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
				// Minions wear helmets: zombies, skeletons and phantoms must not burn either.
				for (Mob m : s.overworld().getEntities(EntityTypeTest.forClass(Mob.class), e -> e.entityTags().contains(QuestBoss.MINION_TAG))) {
					if (m.isOnFire()) {
						return "minion " + m.getType().getDescriptionId() + " burned in daylight";
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

	/** D35: picture of the Ghast boss with its phantoms, from the ground. */
	private static void ghastShot(ClientGameTestContext context, TestServerContext server) {
		double[] at = server.computeOnServer(s -> {
			Mob boss = bosses(s).get(0);
			return new double[] {boss.getX(), boss.getY() + boss.getBbHeight() / 2, boss.getZ()};
		});
		context.runOnClient(mc -> {
			double dx = at[0] - mc.player.getX();
			double dy = at[1] - mc.player.getEyeY();
			double dz = at[2] - mc.player.getZ();
			mc.player.setYRot((float) (Math.toDegrees(Math.atan2(-dx, dz))));
			mc.player.setXRot((float) (-Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)))));
		});
		context.waitTicks(5);
		context.takeScreenshot("11-ghast-boss");
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
