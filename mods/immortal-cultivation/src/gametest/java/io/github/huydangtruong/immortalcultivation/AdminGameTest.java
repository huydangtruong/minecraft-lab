package io.github.huydangtruong.immortalcultivation;

import io.github.huydangtruong.immortalcultivation.admin.AdminNetworking;
import io.github.huydangtruong.immortalcultivation.danger.Danger;
import io.github.huydangtruong.immortalcultivation.danger.DangerSavedData;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.commands.Commands;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** P-060 (D155): the server side of the /admin panel: an OP's lines run in order, a non-OP's run nothing, and every line the panel can build parses. */
public class AdminGameTest {
	@GameTest
	public void opLinesRunInOrderAndNonOpRunsNothing(GameTestHelper c) {
		MinecraftServer server = c.getLevel().getServer();
		ServerPlayer p = c.makeMockServerPlayerInLevel();
		FlightBladeGameTest.enable(c);
		DangerSavedData ds = DangerSavedData.of(server);
		int dangerBefore = ds.level;
		var id = p.nameAndId();
		try {
			server.getPlayerList().deop(id);
			int ran = AdminNetworking.run(p, List.of("hearts set @s 40", "danger set 7"));
			if (ran != 0 || HeartRules.maxHearts(HeartsAttachment.get(p)) == 40 || ds.level == 7) {
				c.fail("a non-OP request ran something (ran " + ran + ")");
				return;
			}
			server.getPlayerList().op(id, java.util.Optional.of(net.minecraft.server.permissions.LevelBasedPermissionSet.GAMEMASTER), java.util.Optional.empty());
			ran = AdminNetworking.run(p, List.of("/hearts set @s 30", "", "  danger set 5  "));
			if (ran != 2) {
				c.fail("expected 2 lines to run, got " + ran);
				return;
			}
			if (HeartRules.maxHearts(HeartsAttachment.get(p)) != 30 || Danger.storedLevel(server) != 5) {
				c.fail("lines did not take effect: hearts " + HeartRules.maxHearts(HeartsAttachment.get(p)) + ", danger " + Danger.storedLevel(server));
				return;
			}
			c.succeed();
		} finally {
			ds.level = dangerBefore;
			server.getPlayerList().deop(id);
		}
	}

	@GameTest
	public void everyLineThePanelBuildsParses(GameTestHelper c) {
		MinecraftServer server = c.getLevel().getServer();
		ServerPlayer p = c.makeMockServerPlayerInLevel();
		var id = p.nameAndId();
		List<String> lines = List.of(
			"hearts set @s 10", "hearts xp @s 0", "technique emeralds @s 64", "testmode full", "testmode reset", "technique cooldowns @s clear", "experience set @s 30 levels", "technique nocooldown @s true", "technique nocooldown @s false",
			"technique set @s fire 5", "technique cast fire 1",
			"quest summon", "quest skip", "quest complete 1", "quest giveup", "quest reset",
			"boss summon @s", "boss level 3", "boss freeze stand", "boss freeze fly", "boss freeze fly_forward", "boss freeze off", "boss giveup",
			"danger set 12", "danger info", "danger elite swift blazing",
			"treasure status", "treasure start", "treasure here", "treasure here ", "treasure lineup 2", "treasure giveup",
			"gamerule permahearts:immortal_cultivation true", "gamerule permahearts:danger_level false", "gamerule permahearts:danger_spawn_ceiling 4",
			"time set day", "time set night", "weather clear", "weather rain", "gamemode creative", "gamemode survival",
			"kill @e[type=#permahearts:hostile,distance=..32]");
		try {
			server.getPlayerList().op(id, java.util.Optional.of(net.minecraft.server.permissions.LevelBasedPermissionSet.GAMEMASTER), java.util.Optional.empty());
			for (String line : lines) {
				var parse = server.getCommands().getDispatcher().parse(line.strip(), p.createCommandSourceStack());
				try {
					Commands.validateParseResults(parse);
				} catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
					c.fail("does not parse: '" + line + "': " + e.getMessage());
					return;
				}
			}
			c.succeed();
		} finally {
			server.getPlayerList().deop(id);
		}
	}
}
