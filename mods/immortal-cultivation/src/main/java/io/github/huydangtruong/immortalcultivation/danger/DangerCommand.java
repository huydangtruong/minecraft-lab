package io.github.huydangtruong.immortalcultivation.danger;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.huydangtruong.immortalcultivation.danger.DangerRules.Affix;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.phys.Vec3;

/** /danger info, /danger set <0..30>, /danger elite <affix...>: operators only (P-054). */
public final class DangerCommand {
	private DangerCommand() {}

	public static void register(CommandDispatcher<CommandSourceStack> d) {
		d.register(Commands.literal("danger")
			.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
			.then(Commands.literal("info").executes(ctx -> info(ctx.getSource())))
			.then(Commands.literal("set")
				.then(Commands.argument("level", IntegerArgumentType.integer(0, DangerRules.MAX_LEVEL))
					.executes(ctx -> {
						int n = IntegerArgumentType.getInteger(ctx, "level");
						MinecraftServer server = ctx.getSource().getServer();
						DangerSavedData.of(server).pendingAtTick = 0;
						Danger.set(server, n, true);
						ctx.getSource().sendSuccess(() -> Component.literal("Danger level set to " + n), true);
						return n;
					})))
			.then(Commands.literal("elite")
				.then(Commands.argument("affixes", StringArgumentType.greedyString())
					.executes(ctx -> elite(ctx.getSource(), StringArgumentType.getString(ctx, "affixes"))))));
	}

	private static int info(CommandSourceStack src) {
		MinecraftServer server = src.getServer();
		int stored = Danger.storedLevel(server);
		int lvl = Danger.level(server);
		DangerSavedData data = DangerSavedData.of(server);
		String next = data.pendingAtTick == 0 ? "none pending"
			: Math.max(0, (data.pendingAtTick - server.overworld().getGameTime()) / 20) + " s";
		String text = "Danger level " + stored + (lvl == stored ? "" : " (switched off)") + ", next increase: " + next
			+ ". Mobs +" + Math.round(DangerRules.spawnBonus(lvl) * 100) + "% (cap x" + String.format("%.1f", Danger.spawnMultiplier(server))
			+ "), strength +" + Math.round(DangerRules.strengthBonus(lvl) * 100) + "%, day spawns "
			+ Math.round(DangerRules.daySpawnFactor(lvl) * 100) + "%, elite chance " + Math.round(DangerRules.eliteChance(lvl) * 100) + "%.";
		src.sendSuccess(() -> Component.literal(text), false);
		return lvl;
	}

	private static int elite(CommandSourceStack src, String args) {
		List<Affix> list = new ArrayList<>();
		for (String w : args.trim().split("\\s+")) {
			Affix a = Affix.byName(w);
			if (a == null) {
				src.sendFailure(Component.literal("Unknown affix: " + w));
				return 0;
			}
			if (!list.contains(a)) {
				list.add(a);
			}
		}
		ServerLevel level = src.getLevel();
		Zombie z = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
		if (z == null) {
			return 0;
		}
		Vec3 at = src.getPosition().add(Vec3.directionFromRotation(0, src.getRotation().y).scale(3));
		z.snapTo(at.x, at.y, at.z, 0f, 0f);
		z.addTag(DangerMobs.APPLIED_TAG);
		DangerMobs.applyStrength(z, Danger.level(src.getServer()));
		DangerMobs.makeElite(z, list);
		level.addFreshEntity(z);
		src.sendSuccess(() -> Component.literal("Summoned an elite zombie"), true);
		return 1;
	}
}
