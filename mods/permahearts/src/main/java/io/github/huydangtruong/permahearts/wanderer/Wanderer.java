package io.github.huydangtruong.permahearts.wanderer;

import io.github.huydangtruong.permahearts.ElixirItems;
import io.github.huydangtruong.permahearts.ElixirStat;
import io.github.huydangtruong.permahearts.HeartsAttachment;
import io.github.huydangtruong.permahearts.Messages;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.random.RandomGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.item.ItemStack;

/**
 * Facade over the Wanderer state: used by events, commands, the menu and the game tests.
 * The NPC and everything about it lives in the Overworld. Not thread-safe; call from the server thread.
 */
public final class Wanderer {
	private static final RandomGenerator RNG = new java.util.Random();
	private static final Map<Long, RewardContainer> REWARDS = new HashMap<>();

	private Wanderer() {}

	// ---- state ----

	public static WandererData data(MinecraftServer server) {
		return WandererSavedData.of(server).get();
	}

	public static void save(MinecraftServer server, WandererData d) {
		WandererSavedData.of(server).set(d);
	}

	public static long currentDay(MinecraftServer server) {
		return server.overworld().getOverworldClockTime() / 24000L;
	}

	public static Optional<Quest> quest(MinecraftServer server) {
		return data(server).quest();
	}

	/** The loaded NPC entity, or null. */
	public static Entity npc(MinecraftServer server) {
		WandererData d = data(server);
		if (d.npcUuid().isEmpty()) {
			return null;
		}
		try {
			return server.overworld().getEntity(UUID.fromString(d.npcUuid().get()));
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	static RewardContainer rewards(MinecraftServer server, long questId) {
		RewardContainer c = REWARDS.get(questId);
		if (c == null || c.server() != server) {
			c = new RewardContainer(server, questId);
			REWARDS.put(questId, c);
		}
		return c;
	}

	// ---- life cycle ----

	/** WAITING -> PLANNED at a point 900..1100 blocks from the given player. */
	public static void plan(MinecraftServer server, ServerPlayer near) {
		double[] off = QuestGenerator.planOffset(RNG);
		int x = (int) Math.round(near.getX() + off[0]);
		int z = (int) Math.round(near.getZ() + off[1]);
		save(server, data(server).withPhase(Phase.PLANNED).withPos(x, 0, z).withNpc(Optional.empty()).withQuest(Optional.empty()));
		server.getPlayerList().broadcastSystemMessage(Messages.wandererPlanned(x, z), false);
	}

	public static Quest newQuest(MinecraftServer server) {
		WandererData d = data(server);
		long id = d.questCounter() + 1;
		save(server, d.withQuestCounter(id));
		return QuestGenerator.generate(id, RNG);
	}

	/** Creates the NPC at pos and makes the phase PRESENT with the given quest. Returns it, or null on failure. */
	public static Entity spawnNpc(ServerLevel level, BlockPos pos, Quest quest, float yaw) {
		MinecraftServer server = level.getServer();
		WanderingTrader npc = WandererNpc.create(level, pos, yaw);
		if (npc == null) {
			return null;
		}
		// Store the UUID before adding: the ENTITY_LOAD listener discards wanderers that are not the stored one.
		save(server, data(server).withPhase(Phase.PRESENT).withPos(pos.getX(), pos.getY(), pos.getZ())
			.withNpc(Optional.of(npc.getUUID().toString())).withQuest(Optional.of(quest)));
		level.addFreshEntity(npc);
		return npc;
	}

	/** Admin: new quest, NPC 3 blocks in front of the player. */
	public static Entity summon(ServerPlayer p) {
		MinecraftServer server = p.level().getServer();
		reset(server);
		BlockPos pos = p.blockPosition().relative(p.getDirection(), 3);
		float yaw = p.getYRot() + 180f;
		return spawnNpc(p.level(), pos, newQuest(server), yaw);
	}

	/** Test helper: reset, then spawn the NPC with a ready-made quest. */
	public static Entity startQuestForTest(ServerLevel level, BlockPos pos, Quest quest) {
		reset(level.getServer());
		return spawnNpc(level, pos, quest, 0f);
	}

	public static void accept(MinecraftServer server) {
		WandererData d = data(server);
		d.quest().ifPresent(q -> {
			if (!q.accepted()) {
				save(server, d.withQuest(Optional.of(QuestLogic.accept(q))));
			}
		});
	}

	/** Puts as much of the stack as the line needs into the quest and shrinks the stack. Returns the amount taken. */
	public static int deposit(MinecraftServer server, int line, ItemStack stack) {
		WandererData d = data(server);
		if (d.quest().isEmpty() || stack.isEmpty()) {
			return 0;
		}
		String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
		Quest before = d.quest().get();
		QuestLogic.DepositResult r = QuestLogic.deposit(before, line, id, stack.getCount());
		if (r.taken() > 0) {
			stack.shrink(r.taken());
			save(server, d.withQuest(Optional.of(r.quest())));
			announceIfDone(server, before, r.quest(), line);
		}
		return r.taken();
	}

	/** Any mob death credited to a player. */
	public static void onKill(MinecraftServer server, ServerPlayer killer, EntityType<?> type) {
		WandererData d = data(server);
		if (d.phase() != Phase.PRESENT || d.quest().isEmpty()) {
			return;
		}
		String id = EntityType.getKey(type).toString();
		Quest before = d.quest().get();
		Optional<QuestLogic.KillResult> r = QuestLogic.kill(before, id);
		if (r.isEmpty()) {
			return;
		}
		save(server, d.withQuest(Optional.of(r.get().quest())));
		QuestLine l = r.get().quest().lines().get(r.get().line());
		killer.sendOverlayMessage(Messages.killProgress(type.getDescription(), l.progress(), l.required()));
		announceIfDone(server, before, r.get().quest(), r.get().line());
	}

	private static void announceIfDone(MinecraftServer server, Quest before, Quest after, int line) {
		if (!before.lines().get(line).done() && after.lines().get(line).done()) {
			server.getPlayerList().broadcastSystemMessage(Messages.lineCompleted(lineName(after.lines().get(line))), false);
		}
	}

	/** Admin: finish a line (0-based). Returns false if there is no such line. */
	public static boolean complete(MinecraftServer server, int line) {
		WandererData d = data(server);
		if (d.quest().isEmpty() || line < 0 || line >= d.quest().get().lines().size()) {
			return false;
		}
		Quest before = d.quest().get();
		Quest after = QuestLogic.complete(before, line);
		save(server, d.withQuest(Optional.of(after)));
		announceIfDone(server, before, after, line);
		return true;
	}

	/** First come, first served: the elixir for a done line, or EMPTY if it is not done or already taken. */
	public static ItemStack claim(MinecraftServer server, int line, ServerPlayer by) {
		WandererData d = data(server);
		if (d.quest().isEmpty()) {
			return ItemStack.EMPTY;
		}
		Optional<Quest> after = QuestLogic.claim(d.quest().get(), line);
		if (after.isEmpty()) {
			return ItemStack.EMPTY;
		}
		Optional<ElixirStat> stat = ElixirStat.byId(d.quest().get().lines().get(line).reward());
		if (stat.isEmpty()) {
			return ItemStack.EMPTY;
		}
		save(server, d.withQuest(after));
		return new ItemStack(ElixirItems.get(stat.get()));
	}

	/** If every line is done and claimed: the NPC says goodbye and leaves, and the next visit is 3..5 days away. */
	public static void farewellIfFinished(MinecraftServer server) {
		WandererData d = data(server);
		if (d.phase() != Phase.PRESENT || d.quest().isEmpty() || !d.quest().get().allDoneAndClaimed()) {
			return;
		}
		Entity npc = npc(server);
		if (npc != null) {
			npc.discard();
		}
		REWARDS.remove(d.quest().get().id());
		save(server, d.withPhase(Phase.WAITING).withNpc(Optional.empty()).withQuest(Optional.empty())
			.withNextDay(currentDay(server) + QuestGenerator.daysUntilNext(RNG)));
		server.getPlayerList().broadcastSystemMessage(Messages.farewell(), false);
	}

	/** Removes the NPC and forgets everything (admin and tests). */
	public static void reset(MinecraftServer server) {
		Entity npc = npc(server);
		if (npc != null) {
			npc.discard();
		}
		REWARDS.clear();
		save(server, WandererData.DEFAULT);
	}

	/** Admin: reset, then plan on the next scheduler pass. */
	public static void skip(MinecraftServer server) {
		reset(server);
		save(server, WandererData.DEFAULT.withNextDay(currentDay(server)));
	}

	// ---- text helpers (also used by the screen) ----

	public static Component targetName(QuestType type, String target) {
		Identifier id = Identifier.parse(target);
		return switch (type) {
			case COLLECT -> Component.translatable(BuiltInRegistries.ITEM.getValue(id).getDescriptionId());
			case KILL -> BuiltInRegistries.ENTITY_TYPE.getValue(id).getDescription();
		};
	}

	public static Component lineText(QuestType type, String target, int required, int progress) {
		Component name = targetName(type, target);
		return switch (type) {
			case COLLECT -> Messages.lineCollect(required, name, progress);
			case KILL -> Messages.lineKill(required, name, progress);
		};
	}

	public static Component lineName(QuestLine l) {
		return targetName(l.type(), l.target());
	}

	public static Component rewardName(String reward) {
		return ElixirStat.byId(reward)
			.map(s -> (Component) Component.translatable(ElixirItems.get(s).getDescriptionId()))
			.orElse(Component.literal(reward));
	}
}
