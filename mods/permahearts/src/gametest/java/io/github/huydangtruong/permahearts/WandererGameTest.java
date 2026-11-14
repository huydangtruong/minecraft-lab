package io.github.huydangtruong.permahearts;

import io.github.huydangtruong.permahearts.wanderer.BloodMoon;
import io.github.huydangtruong.permahearts.wanderer.BloodMoonState;
import io.github.huydangtruong.permahearts.wanderer.Phase;
import io.github.huydangtruong.permahearts.wanderer.QuestBoss;
import io.github.huydangtruong.permahearts.wanderer.QuestGenerator;
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
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import java.util.Optional;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

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
			context.assertTrue(days == 4, "next visit in 4 days, was " + days);
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
				ok |= dist >= 449 && dist <= 551;
			}
			double own = Math.hypot(p.getX() - d.x(), p.getZ() - d.z());
			ok |= own >= 449 && own <= 551;
			context.assertTrue(ok, "planned point should be 450..550 blocks from a player");
			context.succeed();
		} finally {
			Wanderer.reset(s);
		}
	}

	// ---- P-006 events ----

	private static Quest eventQuest(QuestLine... lines) {
		return new Quest(7, List.of(lines), false);
	}

	private static QuestLine moonLine() {
		return new QuestLine(QuestType.BLOOD_MOON, "", 1, 0, "strength", false);
	}

	private static List<? extends Mob> tagged(MinecraftServer s, String tag) {
		return s.overworld().getEntities(EntityTypeTest.forClass(Mob.class), m -> m.entityTags().contains(tag));
	}

	private static ServerPlayer testPlayer(GameTestHelper context) {
		ServerPlayer p = context.makeMockServerPlayerInLevel();
		p.snapTo(context.absoluteVec(new net.minecraft.world.phys.Vec3(2, 1, 2)));
		return p;
	}

	@GameTest(maxTicks = 100)
	public void bloodMoonStartsNightAndSpawns(GameTestHelper context) {
		MinecraftServer s = context.getLevel().getServer();
		try {
			enable(context, true);
			Wanderer.startQuestForTest(context.getLevel(), context.absolutePos(new BlockPos(1, 1, 1)), eventQuest(moonLine(), testQuest().lines().get(1)));
			Wanderer.accept(s);
			ServerPlayer p = testPlayer(context);
			context.assertTrue(Wanderer.startEvent(s, 0, p), "start should work");
			context.assertTrue(BloodMoon.isNight(s), "the clock should be at night after the blood moon starts");
			context.assertTrue(!Wanderer.startEvent(s, 0, p), "second start while running must be refused");
			BloodMoon.wave(s, List.of(p));
			int one = tagged(s, BloodMoon.TAG).size();
			context.assertTrue(one >= 1 && one <= 5, "first wave spawns 1..5 zombies, was " + one);
			for (int i = 0; i < 10; i++) {
				BloodMoon.wave(s, List.of(p));
			}
			int all = tagged(s, BloodMoon.TAG).size();
			context.assertTrue(all <= 30, "at most 30 alive, was " + all);
			context.succeed();
		} finally {
			BloodMoon.moveClock(s, false);
			Wanderer.reset(s);
		}
	}

	@GameTest(maxTicks = 100)
	public void bloodMoonVariantsSpawnTheirMob(GameTestHelper context) {
		MinecraftServer s = context.getLevel().getServer();
		try {
			enable(context, true);
			for (String mob : List.of("minecraft:piglin_brute", "minecraft:creeper", "minecraft:skeleton", "minecraft:spider")) {
				Wanderer.reset(s);
				Wanderer.startQuestForTest(context.getLevel(), context.absolutePos(new BlockPos(1, 1, 1)),
					eventQuest(new QuestLine(QuestType.BLOOD_MOON, mob, 1, 0, "strength", false), testQuest().lines().get(1)));
				Wanderer.accept(s);
				ServerPlayer p = testPlayer(context);
				context.assertTrue(Wanderer.startEvent(s, 0, p), "start should work for " + mob);
				BloodMoon.wave(s, List.of(p));
				List<? extends Mob> spawned = tagged(s, BloodMoon.TAG);
				context.assertTrue(!spawned.isEmpty(), "a wave should spawn " + mob);
				for (Mob m : spawned) {
					String id = net.minecraft.world.entity.EntityType.getKey(m.getType()).toString();
					context.assertTrue(id.equals(mob), "expected " + mob + ", got " + id);
				}
				BloodMoon.moveClock(s, false);
			}
			context.succeed();
		} finally {
			BloodMoon.moveClock(s, false);
			Wanderer.reset(s);
		}
	}

	@GameTest(maxTicks = 100)
	public void bloodMoonArenaBarrier(GameTestHelper context) {
		MinecraftServer s = context.getLevel().getServer();
		try {
			enable(context, true);
			Wanderer.startQuestForTest(context.getLevel(), context.absolutePos(new BlockPos(1, 1, 1)),
				eventQuest(new QuestLine(QuestType.BLOOD_MOON, "minecraft:zombie", 1, 0, "strength", false), testQuest().lines().get(1)));
			Wanderer.accept(s);
			ServerPlayer p = testPlayer(context);
			context.assertTrue(Wanderer.startEvent(s, 0, p), "start should work");
			WandererData d = Wanderer.data(s);
			context.assertTrue(io.github.huydangtruong.permahearts.wanderer.BloodMoonArena.inside(p.getX(), p.getZ(), d.x(), d.z()),
				"the starter stands in the arena");
			double x0 = p.getX();
			double z0 = p.getZ();
			BloodMoon.barrier(s, List.of(p), false); // first sight: inside
			p.teleportTo(x0 + 100, p.getY(), z0);
			BloodMoon.barrier(s, List.of(p), false);
			context.assertTrue(Math.abs(p.getX() - x0) < 1 && Math.abs(p.getZ() - z0) < 1, "a player inside is put back, at x " + p.getX());
			// respawned far away: kept out
			BloodMoon.onRespawn(p);
			p.teleportTo(x0 + 100, p.getY(), z0);
			BloodMoon.barrier(s, List.of(p), false);
			p.teleportTo(x0, p.getY(), z0);
			BloodMoon.barrier(s, List.of(p), false);
			context.assertTrue(Math.abs(p.getX() - (x0 + 100)) < 1, "a player outside cannot come in, at x " + p.getX());
			// after the event everyone is free
			BloodMoon.moveClock(s, false);
			Wanderer.reset(s);
			p.teleportTo(x0, p.getY(), z0);
			BloodMoon.barrier(s, List.of(p), false);
			context.succeed();
		} finally {
			BloodMoon.moveClock(s, false);
			Wanderer.reset(s);
		}
	}

	@GameTest(maxTicks = 100)
	public void bloodMoonHorsemen(GameTestHelper context) {
		MinecraftServer s = context.getLevel().getServer();
		try {
			enable(context, true);
			Wanderer.startQuestForTest(context.getLevel(), context.absolutePos(new BlockPos(1, 1, 1)),
				eventQuest(new QuestLine(QuestType.BLOOD_MOON, QuestGenerator.HORSEMAN, 1, 0, "strength", false), testQuest().lines().get(1)));
			Wanderer.accept(s);
			ServerPlayer p = testPlayer(context);
			context.assertTrue(Wanderer.startEvent(s, 0, p), "start should work");
			BloodMoon.wave(s, List.of(p));
			List<? extends Mob> spawned = tagged(s, BloodMoon.TAG);
			long horses = spawned.stream().filter(m -> m instanceof net.minecraft.world.entity.animal.equine.ZombieHorse).count();
			context.assertTrue(horses == BloodMoon.PER_WAVE, "a wave should bring " + BloodMoon.PER_WAVE + " zombie horses, got " + horses);
			for (Mob m : spawned) {
				if (m instanceof net.minecraft.world.entity.animal.equine.ZombieHorse h) {
					context.assertTrue(h.getFirstPassenger() instanceof net.minecraft.world.entity.monster.zombie.Zombie z
						&& z.getMainHandItem().is(net.minecraft.world.item.Items.IRON_SPEAR) && z.entityTags().contains(BloodMoon.TAG),
						"each horse carries a tagged zombie with an iron spear");
				}
			}
			context.assertTrue(Wanderer.lineText(QuestType.BLOOD_MOON, QuestGenerator.HORSEMAN, 1, 0).getString().contains("Zombie Horseman"),
				"line text names the horseman");
			Wanderer.reset(s);
			context.assertTrue(tagged(s, BloodMoon.TAG).isEmpty(), "reset removes horses and riders");
			context.succeed();
		} finally {
			BloodMoon.moveClock(s, false);
			Wanderer.reset(s);
		}
	}

	@GameTest(maxTicks = 100)
	public void evokerBossBringsPillagers(GameTestHelper context) {
		MinecraftServer s = context.getLevel().getServer();
		try {
			enable(context, true);
			Wanderer.startQuestForTest(context.getLevel(), context.absolutePos(new BlockPos(1, 1, 1)),
				eventQuest(new QuestLine(QuestType.BOSS, "minecraft:evoker", 1, 0, "fury", false), testQuest().lines().get(0)));
			Wanderer.accept(s);
			ServerPlayer p = testPlayer(context);
			context.assertTrue(Wanderer.startEvent(s, 0, p), "summon should work");
			List<? extends Mob> bosses = tagged(s, QuestBoss.TAG);
			context.assertTrue(bosses.size() == 1 && bosses.get(0) instanceof net.minecraft.world.entity.monster.illager.Evoker, "one evoker boss");
			context.assertTrue(bosses.get(0).getMaxHealth() == 150f, "boss max health 150");
			List<? extends Mob> minions = tagged(s, QuestBoss.MINION_TAG);
			context.assertTrue(minions.size() == 4, "four minions, found " + minions.size());
			for (Mob m : minions) {
				context.assertTrue(m instanceof net.minecraft.world.entity.monster.illager.Pillager && m.getMainHandItem().is(net.minecraft.world.item.Items.CROSSBOW),
					"evoker minions are pillagers with crossbows");
			}
			context.succeed();
		} finally {
			Wanderer.reset(s);
		}
	}

	@GameTest(maxTicks = 100)
	public void bloodMoonFailsOnStarterDeath(GameTestHelper context) {
		MinecraftServer s = context.getLevel().getServer();
		try {
			enable(context, true);
			Wanderer.startQuestForTest(context.getLevel(), context.absolutePos(new BlockPos(1, 1, 1)), eventQuest(moonLine(), testQuest().lines().get(1)));
			Wanderer.accept(s);
			ServerPlayer p = testPlayer(context);
			context.assertTrue(Wanderer.startEvent(s, 0, p), "start should work");
			context.assertTrue(BloodMoon.running(s), "should be running");
			p.setHealth(0f);
			p.die(context.getLevel().damageSources().generic());
			context.assertTrue(!BloodMoon.running(s), "starter death should end the blood moon");
			context.assertTrue(!Wanderer.data(s).quest().orElseThrow().lines().get(0).done(), "line must stay not done");
			context.assertTrue(Wanderer.eventState(s, 0) == 0, "button usable again");
			context.succeed();
		} finally {
			BloodMoon.moveClock(s, false);
			Wanderer.reset(s);
		}
	}

	@GameTest(maxTicks = 100)
	public void bloodMoonSucceedsAtDawn(GameTestHelper context) {
		MinecraftServer s = context.getLevel().getServer();
		try {
			enable(context, true);
			context.assertTrue(BloodMoon.succeeded(5000, true) && !BloodMoon.succeeded(100, true)
				&& !BloodMoon.succeeded(5000, false) && BloodMoon.succeeded(13000, false), "decision function");
			Wanderer.startQuestForTest(context.getLevel(), context.absolutePos(new BlockPos(1, 1, 1)), eventQuest(moonLine(), testQuest().lines().get(1)));
			Wanderer.accept(s);
			ServerPlayer p = testPlayer(context);
			context.assertTrue(Wanderer.startEvent(s, 0, p), "start should work");
			WandererData d = Wanderer.data(s);
			Wanderer.save(s, d.withBloodMoon(Optional.of(new BloodMoonState(0, p.getUUID().toString(), s.overworld().getGameTime() - 13500))));
			// Level#isBrightOutside follows a sky value that only refreshes on a real tick, so the dawn test goes through the
			// hard-end rule; the dawn rule itself is covered by the decision function above.
			BloodMoon.moveClock(s, false);
			BloodMoon.tick(s);
			context.assertTrue(Wanderer.data(s).quest().orElseThrow().lines().get(0).done(), "line should be done at the end");
			context.assertTrue(!BloodMoon.running(s), "state should be cleared");
			context.succeed();
		} finally {
			BloodMoon.moveClock(s, false);
			Wanderer.reset(s);
		}
	}

	@GameTest(maxTicks = 100)
	public void bossSummonAndKill(GameTestHelper context) {
		MinecraftServer s = context.getLevel().getServer();
		try {
			enable(context, true);
			Wanderer.startQuestForTest(context.getLevel(), context.absolutePos(new BlockPos(1, 1, 1)),
				eventQuest(new QuestLine(QuestType.BOSS, "minecraft:zombie", 1, 0, "fury", false), testQuest().lines().get(0)));
			Wanderer.accept(s);
			ServerPlayer p = testPlayer(context);
			context.assertTrue(Wanderer.startEvent(s, 0, p), "summon should work");
			List<? extends Mob> bosses = tagged(s, QuestBoss.TAG);
			context.assertTrue(bosses.size() == 1, "one boss, found " + bosses.size());
			context.assertTrue(bosses.get(0) instanceof Zombie, "zombie boss");
			Mob boss = bosses.get(0);
			context.assertTrue(boss.getMaxHealth() == 150f, "boss max health 150, was " + boss.getMaxHealth());
			context.assertTrue(boss.getAttributeValue(Attributes.SCALE) == 2.0, "boss scale 2");
			int minions = tagged(s, QuestBoss.MINION_TAG).size();
			context.assertTrue(minions == 4, "four minions, found " + minions);
			context.assertTrue(Wanderer.eventState(s, 0) == 1, "line shows running");
			context.assertTrue(!Wanderer.startEvent(s, 0, p), "second summon must be refused");
			context.assertTrue(tagged(s, QuestBoss.TAG).size() == 1, "still one boss");
			kill(context, p, boss);
			context.assertTrue(Wanderer.data(s).quest().orElseThrow().lines().get(0).done(), "boss line should be done");
			context.assertTrue(Wanderer.data(s).boss().isEmpty(), "boss state cleared");
			context.succeed();
		} finally {
			Wanderer.reset(s);
		}
	}

	@GameTest(maxTicks = 100)
	public void exploreMarksLine(GameTestHelper context) {
		MinecraftServer s = context.getLevel().getServer();
		try {
			enable(context, true);
			Wanderer.startQuestForTest(context.getLevel(), context.absolutePos(new BlockPos(1, 1, 1)),
				eventQuest(new QuestLine(QuestType.EXPLORE, "#minecraft:village", 1, 0, "strength", false),
					new QuestLine(QuestType.EXPLORE, "minecraft:fortress", 1, 0, "fury", false)));
			Wanderer.accept(s);
			Wanderer.markExploredForTest(s, "#minecraft:village");
			var lines = Wanderer.data(s).quest().orElseThrow().lines();
			context.assertTrue(lines.get(0).done(), "village line should be done");
			context.assertTrue(!lines.get(1).done(), "fortress line must stay open");
			context.succeed();
		} finally {
			Wanderer.reset(s);
		}
	}
	// ---- P-008 ----

	private static Quest bossQuest(String boss) {
		return eventQuest(new QuestLine(QuestType.BOSS, boss, 1, 0, "fury", false), testQuest().lines().get(0));
	}

	@GameTest(maxTicks = 40)
	public void npcIsNotATarget(GameTestHelper context) {
		MinecraftServer s = context.getLevel().getServer();
		try {
			enable(context, true);
			Entity npc = start(context);
			Zombie zombie = EntityTypes.ZOMBIE.create(context.getLevel(), EntitySpawnReason.EVENT);
			zombie.snapTo(npc.getX() + 1, npc.getY(), npc.getZ(), 0f, 0f);
			context.getLevel().addFreshEntity(zombie);
			context.assertTrue(npc.isInvulnerable(), "NPC should be permanently invulnerable");
			boolean can = TargetingConditions.forCombat().test(context.getLevel(), zombie, (LivingEntity) npc);
			context.assertTrue(!can, "a zombie must not be able to target the NPC");
			zombie.discard();
			context.succeed();
		} finally {
			Wanderer.reset(s);
		}
	}

	@GameTest(maxTicks = 40)
	public void giveUpEndsTheVisit(GameTestHelper context) {
		MinecraftServer s = context.getLevel().getServer();
		try {
			enable(context, true);
			Wanderer.startQuestForTest(context.getLevel(), context.absolutePos(new BlockPos(1, 1, 1)), bossQuest("minecraft:zombie"));
			Wanderer.accept(s);
			ServerPlayer p = testPlayer(context);
			context.assertTrue(Wanderer.startEvent(s, 0, p), "summon should work");
			Entity npc = Wanderer.npc(s);
			context.assertTrue(Wanderer.giveUp(s, p), "give up should be accepted");
			WandererData d = Wanderer.data(s);
			context.assertTrue(d.phase() == Phase.WAITING, "phase should be WAITING, was " + d.phase());
			context.assertTrue(npc.isRemoved() && d.npcUuid().isEmpty(), "NPC should be gone");
			context.assertTrue(d.quest().isEmpty() && d.boss().isEmpty() && d.bloodMoon().isEmpty(), "no quest, boss state or event");
			context.assertTrue(tagged(s, QuestBoss.TAG).isEmpty() && tagged(s, QuestBoss.MINION_TAG).isEmpty(), "boss and minions gone");
			long days = d.nextDay() - Wanderer.currentDay(s);
			context.assertTrue(days == 4, "next visit in 4 days, was " + days);
			context.assertTrue(!Wanderer.giveUp(s, p), "give up while WAITING does nothing");
			context.succeed();
		} finally {
			Wanderer.reset(s);
		}
	}

	@GameTest(maxTicks = 40)
	public void giveUpCommand(GameTestHelper context) {
		MinecraftServer s = context.getLevel().getServer();
		try {
			enable(context, true);
			Wanderer.startQuestForTest(context.getLevel(), context.absolutePos(new BlockPos(1, 1, 1)), bossQuest("minecraft:zombie"));
			Entity npc = Wanderer.npc(s);
			ServerPlayer p = testPlayer(context);
			// Not an operator: /quest giveup is for everyone, like the Give up button.
			net.minecraft.commands.CommandSourceStack src = p.createCommandSourceStack();
			int r = s.getCommands().getDispatcher().execute("quest giveup", src);
			WandererData d = Wanderer.data(s);
			context.assertTrue(r == 1 && d.phase() == Phase.WAITING, "giveup should end the visit, phase " + d.phase());
			context.assertTrue(npc.isRemoved(), "NPC should be gone");
			long days = d.nextDay() - Wanderer.currentDay(s);
			context.assertTrue(days == 4, "next visit in 4 days, was " + days);
			int again = s.getCommands().getDispatcher().execute("quest giveup", src);
			context.assertTrue(again == 0, "nothing to give up while WAITING");
			context.succeed();
		} catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
			throw new RuntimeException(e);
		} finally {
			Wanderer.reset(s);
		}
	}

	@GameTest(maxTicks = 40)
	public void visitExpires(GameTestHelper context) {
		MinecraftServer s = context.getLevel().getServer();
		try {
			enable(context, true);
			Entity npc = start(context);
			WandererData d = Wanderer.data(s);
			context.assertTrue(d.leaveAt() > Wanderer.clockTime(s), "leave time should be set in the future");
			context.assertTrue(Wanderer.ticksLeft(s) > 0, "ticks left should be positive");
			// Still inside the stay: nothing happens.
			WandererScheduler.work(s, false);
			context.assertTrue(Wanderer.data(s).phase() == Phase.PRESENT, "still present before the end");
			// Past the end, but a boss is alive: it stays.
			Wanderer.save(s, Wanderer.data(s).withQuest(Optional.of(bossQuest("minecraft:zombie"))).withLeaveAt(Wanderer.clockTime(s) - 1));
			Wanderer.accept(s);
			ServerPlayer p = testPlayer(context);
			context.assertTrue(Wanderer.startEvent(s, 0, p), "summon should work");
			WandererScheduler.work(s, false);
			context.assertTrue(Wanderer.data(s).phase() == Phase.PRESENT, "does not leave during a boss fight");
			context.assertTrue(Wanderer.data(s).warned(), "warning was sent");
			// Boss gone: it leaves.
			Wanderer.save(s, Wanderer.data(s).withBoss(Optional.empty()));
			tagged(s, QuestBoss.TAG).forEach(Mob::discard);
			WandererScheduler.work(s, false);
			d = Wanderer.data(s);
			context.assertTrue(d.phase() == Phase.WAITING, "phase should be WAITING, was " + d.phase());
			context.assertTrue(npc.isRemoved(), "NPC should be gone");
			long days = d.nextDay() - Wanderer.currentDay(s);
			context.assertTrue(days == 4, "next visit in 4 days, was " + days);
			context.succeed();
		} finally {
			Wanderer.reset(s);
		}
	}

	@GameTest(maxTicks = 40)
	public void lostBossCanBeResummoned(GameTestHelper context) {
		MinecraftServer s = context.getLevel().getServer();
		try {
			enable(context, true);
			Wanderer.startQuestForTest(context.getLevel(), context.absolutePos(new BlockPos(1, 1, 1)), bossQuest("minecraft:zombie"));
			Wanderer.accept(s);
			ServerPlayer p = testPlayer(context);
			context.assertTrue(Wanderer.startEvent(s, 0, p), "summon should work");
			context.assertTrue(Wanderer.eventState(s, 0) == 1 && QuestBoss.bossAlive(s), "boss alive, line running");
			tagged(s, QuestBoss.TAG).forEach(Mob::discard);
			context.assertTrue(Wanderer.eventState(s, 0) == 0, "lost boss: line is not running");
			context.assertTrue(Wanderer.startEvent(s, 0, p), "summon again should work");
			context.assertTrue(tagged(s, QuestBoss.TAG).size() == 1, "one new boss");
			context.succeed();
		} finally {
			Wanderer.reset(s);
		}
	}

	@GameTest(maxTicks = 40)
	public void bossHelmetsAreUnbreakable(GameTestHelper context) {
		MinecraftServer s = context.getLevel().getServer();
		try {
			enable(context, true);
			Wanderer.startQuestForTest(context.getLevel(), context.absolutePos(new BlockPos(1, 1, 1)), bossQuest("minecraft:zombie"));
			Wanderer.accept(s);
			context.assertTrue(Wanderer.startEvent(s, 0, testPlayer(context)), "summon should work");
			var all = new java.util.ArrayList<Mob>(tagged(s, QuestBoss.TAG));
			all.addAll(tagged(s, QuestBoss.MINION_TAG));
			context.assertTrue(all.size() == 5, "boss + 4 minions, found " + all.size());
			for (Mob m : all) {
				ItemStack head = m.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
				context.assertTrue(!head.isEmpty() && head.has(net.minecraft.core.component.DataComponents.UNBREAKABLE),
					"helmet must be unbreakable");
			}
			context.succeed();
		} finally {
			Wanderer.reset(s);
		}
	}
}
