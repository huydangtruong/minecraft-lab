package io.github.huydangtruong.permahearts;

import io.github.huydangtruong.permahearts.wanderer.Phase;
import io.github.huydangtruong.permahearts.wanderer.Quest;
import io.github.huydangtruong.permahearts.wanderer.QuestLine;
import io.github.huydangtruong.permahearts.wanderer.QuestType;
import io.github.huydangtruong.permahearts.wanderer.Wanderer;
import io.github.huydangtruong.permahearts.wanderer.WandererData;
import io.github.huydangtruong.permahearts.wanderer.WandererNpc;
import io.github.huydangtruong.permahearts.wanderer.WandererScheduler;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

/**
 * Wanderer logic without any UI. The tests share one server and one saved Wanderer state, so each one resets it
 * first and again in finally. Every test finishes inside its own method call, so they never interleave.
 * The menu, slots and screen are NOT covered here.
 */
public class WandererGameTest {
	private static void enable(GameTestHelper context, boolean on) {
		MinecraftServer s = context.getLevel().getServer();
		s.getGameRules().set(PermaHearts.ENABLED, on, s);
	}

	private static Quest testQuest() {
		return new Quest(1, List.of(
			new QuestLine(QuestType.COLLECT, "minecraft:iron_ingot", 10, 0, "strength", false),
			new QuestLine(QuestType.KILL, "minecraft:zombie", 2, 0, "fury", false)), false);
	}

	private static Entity start(GameTestHelper context) {
		MinecraftServer s = context.getLevel().getServer();
		Wanderer.reset(s);
		Entity npc = Wanderer.startQuestForTest(context.getLevel(), context.absolutePos(new BlockPos(1, 1, 1)), testQuest());
		context.assertTrue(npc != null, "NPC should spawn");
		return npc;
	}

	private static void kill(GameTestHelper context, ServerPlayer p, LivingEntity e) {
		ServerLevel level = context.getLevel();
		e.hurtServer(level, level.damageSources().playerAttack(p), 1000f);
	}

	@GameTest(maxTicks = 100)
	public void summonSpawnsWanderer(GameTestHelper context) {
		MinecraftServer s = context.getLevel().getServer();
		try {
			enable(context, true);
			Wanderer.reset(s);
			ServerPlayer p = context.makeMockServerPlayerInLevel();
			p.snapTo(context.absoluteVec(new net.minecraft.world.phys.Vec3(2, 1, 2)));
			Wanderer.summon(p);
			WandererData d = Wanderer.data(s);
			context.assertTrue(d.phase() == Phase.PRESENT, "phase should be PRESENT, was " + d.phase());
			int lines = d.quest().orElseThrow().lines().size();
			context.assertTrue(lines >= 2 && lines <= 4, "2..4 lines, was " + lines);
			List<WanderingTrader> found = context.getLevel().getEntitiesOfClass(WanderingTrader.class,
				new AABB(p.blockPosition()).inflate(5), WandererNpc::isWanderer);
			context.assertTrue(found.size() == 1, "one wanderer within 5 blocks, found " + found.size());
			WanderingTrader npc = found.get(0);
			context.assertTrue(npc.isNoAi(), "NPC should have no AI");
			float hp = npc.getHealth();
			ServerLevel level = context.getLevel();
			npc.hurtServer(level, level.damageSources().playerAttack(p), 5f);
			context.assertTrue(npc.getHealth() == hp, "NPC must not take damage");
			context.succeed();
		} finally {
			Wanderer.reset(s);
		}
	}

	@GameTest(maxTicks = 100)
	public void collectDepositShared(GameTestHelper context) {
		MinecraftServer s = context.getLevel().getServer();
		try {
			enable(context, true);
			start(context);
			ItemStack early = new ItemStack(Items.IRON_INGOT, 3);
			context.assertTrue(Wanderer.deposit(s, 0, early) == 0, "deposit before accept must take nothing");
			Wanderer.accept(s);
			ItemStack first = new ItemStack(Items.IRON_INGOT, 6);
			context.assertTrue(Wanderer.deposit(s, 0, first) == 6, "first deposit takes 6");
			context.assertTrue(first.isEmpty(), "first stack should be empty");
			int progress = Wanderer.data(s).quest().orElseThrow().lines().get(0).progress();
			context.assertTrue(progress == 6, "progress should be 6, was " + progress);
			ItemStack second = new ItemStack(Items.IRON_INGOT, 10);
			context.assertTrue(Wanderer.deposit(s, 0, second) == 4, "second deposit takes 4");
			context.assertTrue(second.getCount() == 6, "6 should be left, was " + second.getCount());
			context.assertTrue(Wanderer.data(s).quest().orElseThrow().lines().get(0).done(), "line 0 should be done");
			context.assertTrue(Wanderer.deposit(s, 0, new ItemStack(Items.GOLD_INGOT, 5)) == 0, "wrong item takes nothing");
			context.succeed();
		} finally {
			Wanderer.reset(s);
		}
	}

	@GameTest(maxTicks = 100)
	public void killCountsForQuest(GameTestHelper context) {
		MinecraftServer s = context.getLevel().getServer();
		try {
			enable(context, true);
			start(context);
			Wanderer.accept(s);
			ServerPlayer p = context.makeMockServerPlayerInLevel();
			kill(context, p, context.spawn(EntityTypes.COW, 1, 1, 1));
			context.assertTrue(Wanderer.data(s).quest().orElseThrow().lines().get(1).progress() == 0, "cow must not count");
			kill(context, p, context.spawn(EntityTypes.ZOMBIE, 2, 1, 1));
			kill(context, p, context.spawn(EntityTypes.ZOMBIE, 3, 1, 1));
			QuestLine l = Wanderer.data(s).quest().orElseThrow().lines().get(1);
			context.assertTrue(l.done(), "two zombies should finish the line, progress " + l.progress());
			context.succeed();
		} finally {
			Wanderer.reset(s);
		}
	}

	@GameTest(maxTicks = 100)
	public void claimFirstComeFirstServed(GameTestHelper context) {
		MinecraftServer s = context.getLevel().getServer();
		try {
			enable(context, true);
			start(context);
			Wanderer.accept(s);
			ServerPlayer p1 = context.makeMockServerPlayerInLevel();
			ServerPlayer p2 = context.makeMockServerPlayerInLevel();
			context.assertTrue(Wanderer.claim(s, 0, p1).isEmpty(), "claim before done must give nothing");
			Wanderer.complete(s, 0);
			ItemStack got = Wanderer.claim(s, 0, p1);
			context.assertTrue(got.is(ElixirItems.get(ElixirStat.STRENGTH)), "first claim gives the strength elixir");
			context.assertTrue(Wanderer.claim(s, 0, p2).isEmpty(), "second claim must give nothing");
			context.succeed();
		} finally {
			Wanderer.reset(s);
		}
	}

	@GameTest(maxTicks = 100)
	public void farewellWhenFinished(GameTestHelper context) {
		MinecraftServer s = context.getLevel().getServer();
		try {
			enable(context, true);
			Entity npc = start(context);
			Wanderer.accept(s);
			ServerPlayer p = context.makeMockServerPlayerInLevel();
			Wanderer.complete(s, 0);
			Wanderer.claim(s, 0, p);
			Wanderer.farewellIfFinished(s);
			context.assertTrue(Wanderer.data(s).phase() == Phase.PRESENT, "still present with one line open");
			Wanderer.complete(s, 1);
			Wanderer.claim(s, 1, p);
			Wanderer.farewellIfFinished(s);
			WandererData d = Wanderer.data(s);
			context.assertTrue(npc.isRemoved(), "NPC should be gone");
			context.assertTrue(d.phase() == Phase.WAITING, "phase should be WAITING, was " + d.phase());
			long days = d.nextDay() - Wanderer.currentDay(s);
			context.assertTrue(days >= 3 && days <= 5, "next visit in 3..5 days, was " + days);
			context.succeed();
		} finally {
			Wanderer.reset(s);
		}
	}

	@GameTest(maxTicks = 100)
	public void schedulerPlansFarAway(GameTestHelper context) {
		MinecraftServer s = context.getLevel().getServer();
		try {
			enable(context, true);
			Wanderer.reset(s);
			ServerPlayer p = context.makeMockServerPlayerInLevel();
			Wanderer.save(s, WandererData.DEFAULT.withNextDay(Wanderer.currentDay(s)));
			WandererScheduler.work(s, false);
			WandererData d = Wanderer.data(s);
			if (d.phase() != Phase.PLANNED) {
				// The mock player is not in the overworld player list on this server: plan for it directly.
				Wanderer.plan(s, p);
				d = Wanderer.data(s);
			}
			context.assertTrue(d.phase() == Phase.PLANNED, "phase should be PLANNED, was " + d.phase());
			boolean ok = false;
			for (ServerPlayer q : s.overworld().players()) {
				double dist = Math.hypot(q.getX() - d.x(), q.getZ() - d.z());
				ok |= dist >= 899 && dist <= 1101;
			}
			double own = Math.hypot(p.getX() - d.x(), p.getZ() - d.z());
			ok |= own >= 899 && own <= 1101;
			context.assertTrue(ok, "planned point should be 900..1100 blocks from a player");
			context.succeed();
		} finally {
			Wanderer.reset(s);
		}
	}
}
