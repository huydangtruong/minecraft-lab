package io.github.huydangtruong.permahearts;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.huydangtruong.permahearts.technique.Technique;
import io.github.huydangtruong.permahearts.technique.TechniqueAttachment;
import io.github.huydangtruong.permahearts.technique.TechniqueData;
import io.github.huydangtruong.permahearts.technique.TechniqueNet;
import io.github.huydangtruong.permahearts.technique.TechniqueRules;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Server side of the Techniques tab (P-011a): upgrade, reset, deposit, assign, the op command and the sync codec. */
public class TechniqueGameTest {
	private static void enable(GameTestHelper context, boolean on) {
		MinecraftServer s = context.getLevel().getServer();
		s.getGameRules().set(PermaHearts.ENABLED, on, s);
	}

	private static ServerPlayer player(GameTestHelper context) {
		enable(context, true);
		ServerPlayer p = context.makeMockServerPlayerInLevel();
		TechniqueAttachment.set(p, TechniqueData.DEFAULT);
		p.experienceLevel = 0;
		p.experienceProgress = 0f;
		p.totalExperience = 0;
		return p;
	}

	private static long now(ServerPlayer p) {
		return p.level().getServer().overworld().getGameTime();
	}

	private static String act(ServerPlayer p, int kind, Technique t, int slot) {
		return TechniqueNet.apply(p, kind, t == null ? "" : t.id(), slot);
	}

	@GameTest(maxTicks = 100)
	public void upgradeSpendsLevelsAndRefusesWithoutXp(GameTestHelper context) {
		ServerPlayer p = player(context);
		p.giveExperienceLevels(10);
		int before = TechniqueRules.points(p.experienceLevel, p.experienceProgress);
		context.assertTrue(act(p, TechniqueNet.UPGRADE, Technique.FIRE, 0) == null, "upgrade with 10 levels should work");
		TechniqueData d = TechniqueAttachment.get(p);
		context.assertTrue(d.level(Technique.FIRE) == 1, "fire should be level 1, was " + d.level(Technique.FIRE));
		context.assertTrue(p.experienceLevel == 10 - TechniqueRules.upgradeCost(1), "levels left should be " + (10 - TechniqueRules.upgradeCost(1)) + ", was " + p.experienceLevel);
		int paid = before - TechniqueRules.points(p.experienceLevel, p.experienceProgress);
		context.assertTrue(paid > 0 && d.spent(Technique.FIRE) == paid, "spent points should be " + paid + ", was " + d.spent(Technique.FIRE));
		// 4 levels left, the next upgrade costs 7: refused, nothing changes
		String err = act(p, TechniqueNet.UPGRADE, Technique.FIRE, 0);
		context.assertTrue(err != null, "upgrade without enough levels must be refused");
		context.assertTrue(TechniqueAttachment.get(p).level(Technique.FIRE) == 1 && p.experienceLevel == 4, "refused upgrade must not change anything");
		// bad id
		context.assertTrue(TechniqueNet.apply(p, TechniqueNet.UPGRADE, "nope", 0) != null, "unknown technique must be refused");
		context.succeed();
	}

	@GameTest(maxTicks = 100)
	public void upgradeStopsAtMaxLevel(GameTestHelper context) {
		ServerPlayer p = player(context);
		p.giveExperienceLevels(50);
		TechniqueAttachment.set(p, TechniqueData.DEFAULT.withLevel(Technique.ICE, TechniqueRules.MAX_LEVEL, 100));
		context.assertTrue(act(p, TechniqueNet.UPGRADE, Technique.ICE, 0) != null, "level 21 must be refused");
		context.assertTrue(p.experienceLevel == 50, "no levels spent at the max");
		context.succeed();
	}

	@GameTest(maxTicks = 100)
	public void resetRefundsHalfAndIsRefusedOnCooldown(GameTestHelper context) {
		ServerPlayer p = player(context);
		p.giveExperienceLevels(30);
		for (int i = 0; i < 3; i++) {
			context.assertTrue(act(p, TechniqueNet.UPGRADE, Technique.HEALING, 0) == null, "upgrade " + i);
		}
		int spent = TechniqueAttachment.get(p).spent(Technique.HEALING);
		context.assertTrue(spent > 0, "points were recorded");
		// slot it, then put it on cooldown: reset refused
		TechniqueAttachment.set(p, TechniqueAttachment.get(p).withEmeralds(64).withSlot(0, Technique.HEALING)
			.withCooldown(Technique.HEALING, now(p) + 200));
		context.assertTrue(act(p, TechniqueNet.RESET, Technique.HEALING, 0) != null, "reset on cooldown must be refused");
		context.assertTrue(TechniqueAttachment.get(p).level(Technique.HEALING) == 3, "level kept after a refused reset");
		TechniqueAttachment.set(p, TechniqueAttachment.get(p).withoutCooldowns());
		int pointsBefore = TechniqueRules.points(p.experienceLevel, p.experienceProgress);
		context.assertTrue(act(p, TechniqueNet.RESET, Technique.HEALING, 0) == null, "reset should work now");
		TechniqueData d = TechniqueAttachment.get(p);
		context.assertTrue(d.level(Technique.HEALING) == 0 && d.spent(Technique.HEALING) == 0, "level and spent points cleared");
		context.assertTrue(d.inSlot(0) == null, "reset removes the technique from its slot");
		int gained = TechniqueRules.points(p.experienceLevel, p.experienceProgress) - pointsBefore;
		int expected = TechniqueRules.refund(spent);
		context.assertTrue(Math.abs(gained - expected) <= 1, "refund should be " + expected + " points, was " + gained);
		context.assertTrue(act(p, TechniqueNet.RESET, Technique.HEALING, 0) != null, "reset of an unlearned technique is refused");
		context.succeed();
	}

	@GameTest(maxTicks = 100)
	public void depositOpensSlotsAndKeepsPartial(GameTestHelper context) {
		ServerPlayer p = player(context);
		// nothing on the cursor
		context.assertTrue(act(p, TechniqueNet.DEPOSIT, null, 0) != null, "empty cursor must be refused");
		p.containerMenu.setCarried(new ItemStack(Items.DIAMOND, 5));
		context.assertTrue(act(p, TechniqueNet.DEPOSIT, null, 0) != null, "non-emeralds must be refused");
		context.assertTrue(p.containerMenu.getCarried().getCount() == 5, "refused deposit keeps the cursor");
		// partial
		p.containerMenu.setCarried(new ItemStack(Items.EMERALD, 40));
		context.assertTrue(act(p, TechniqueNet.DEPOSIT, null, 0) == null, "deposit 40");
		TechniqueData d = TechniqueAttachment.get(p);
		context.assertTrue(d.emeralds() == 40 && d.slotsOpen() == 0, "40 emeralds: still no slot, partial kept");
		context.assertTrue(p.containerMenu.getCarried().isEmpty(), "cursor stack is used up");
		// reach 64: slot 1 opens
		p.containerMenu.setCarried(new ItemStack(Items.EMERALD, 24));
		act(p, TechniqueNet.DEPOSIT, null, 0);
		context.assertTrue(TechniqueAttachment.get(p).slotsOpen() == 1, "64 emeralds open slot 1");
		// 64 + 256 = 320 opens slot 2
		p.containerMenu.setCarried(new ItemStack(Items.EMERALD, 64));
		for (int i = 0; i < 4; i++) {
			act(p, TechniqueNet.DEPOSIT, null, 0);
			p.containerMenu.setCarried(new ItemStack(Items.EMERALD, 64));
		}
		context.assertTrue(TechniqueAttachment.get(p).emeralds() == 64 + 4 * 64 && TechniqueAttachment.get(p).slotsOpen() == 2, "320 emeralds open slot 2, got " + TechniqueAttachment.get(p).emeralds());
		// cap: only what is still needed is taken
		TechniqueAttachment.set(p, TechniqueAttachment.get(p).withEmeralds(TechniqueRules.MAX_EMERALDS - 8));
		p.containerMenu.setCarried(new ItemStack(Items.EMERALD, 64));
		act(p, TechniqueNet.DEPOSIT, null, 0);
		context.assertTrue(TechniqueAttachment.get(p).emeralds() == TechniqueRules.MAX_EMERALDS && TechniqueAttachment.get(p).slotsOpen() == 4, "all slots open");
		context.assertTrue(p.containerMenu.getCarried().getCount() == 56, "only 8 taken, 56 stay on the cursor, got " + p.containerMenu.getCarried().getCount());
		context.assertTrue(act(p, TechniqueNet.DEPOSIT, null, 0) != null, "deposit with all slots open is refused");
		context.succeed();
	}

	@GameTest(maxTicks = 100)
	public void assignRules(GameTestHelper context) {
		ServerPlayer p = player(context);
		TechniqueAttachment.set(p, TechniqueData.DEFAULT.withLevel(Technique.FIRE, 1, 10).withLevel(Technique.ICE, 2, 20).withEmeralds(320));
		context.assertTrue(act(p, TechniqueNet.ASSIGN, Technique.FIRE, 0) == null, "assign fire to slot 1");
		context.assertTrue(TechniqueAttachment.get(p).inSlot(0) == Technique.FIRE, "fire in slot 1");
		context.assertTrue(act(p, TechniqueNet.ASSIGN, Technique.FIRE, 1) == null, "move fire to slot 2");
		context.assertTrue(TechniqueAttachment.get(p).inSlot(0) == null && TechniqueAttachment.get(p).inSlot(1) == Technique.FIRE, "fire moved, not copied");
		context.assertTrue(act(p, TechniqueNet.ASSIGN, Technique.ICE, 2) != null, "slot 3 is locked");
		context.assertTrue(act(p, TechniqueNet.ASSIGN, Technique.HEALING, 0) != null, "level 0 technique cannot be slotted");
		context.assertTrue(act(p, TechniqueNet.ASSIGN, Technique.ICE, 7) != null && act(p, TechniqueNet.ASSIGN, Technique.ICE, -1) != null, "bad slot index refused");
		// the technique in slot 2 is on cooldown: no replacing, no clearing
		TechniqueAttachment.set(p, TechniqueAttachment.get(p).withCooldown(Technique.FIRE, now(p) + 300));
		context.assertTrue(act(p, TechniqueNet.ASSIGN, Technique.ICE, 1) != null, "cannot replace a technique on cooldown");
		context.assertTrue(act(p, TechniqueNet.CLEAR_SLOT, null, 1) != null, "cannot clear a technique on cooldown");
		context.assertTrue(TechniqueAttachment.get(p).inSlot(1) == Technique.FIRE, "fire still in slot 2");
		// ice into a free slot is fine even while fire cools down
		context.assertTrue(act(p, TechniqueNet.ASSIGN, Technique.ICE, 0) == null, "ice into slot 1");
		TechniqueAttachment.set(p, TechniqueAttachment.get(p).withoutCooldowns());
		context.assertTrue(act(p, TechniqueNet.CLEAR_SLOT, null, 1) == null && TechniqueAttachment.get(p).inSlot(1) == null, "clear works after the cooldown");
		context.succeed();
	}

	@GameTest(maxTicks = 100)
	public void nothingWorksWithTheSwitchOff(GameTestHelper context) {
		ServerPlayer p = player(context);
		p.giveExperienceLevels(20);
		enable(context, false);
		context.assertTrue(act(p, TechniqueNet.UPGRADE, Technique.FIRE, 0) != null, "upgrade refused with the switch off");
		p.containerMenu.setCarried(new ItemStack(Items.EMERALD, 64));
		context.assertTrue(act(p, TechniqueNet.DEPOSIT, null, 0) != null, "deposit refused with the switch off");
		context.assertTrue(TechniqueAttachment.get(p).equals(TechniqueData.DEFAULT) && p.experienceLevel == 20, "nothing changed");
		enable(context, true);
		context.succeed();
	}

	private static void run(GameTestHelper context, CommandSourceStack source, String command) {
		try {
			context.getLevel().getServer().getCommands().getDispatcher().execute(command, source);
		} catch (CommandSyntaxException e) {
			context.fail("/" + command + " failed: " + e.getMessage());
		}
	}

	@GameTest(maxTicks = 100)
	public void opCommandSetsLevelsEmeraldsAndCooldowns(GameTestHelper context) {
		ServerPlayer p = player(context);
		CommandSourceStack admin = context.getLevel().getServer().createCommandSourceStack().withEntity(p);
		run(context, admin, "technique set @s ice 7");
		context.assertTrue(TechniqueAttachment.get(p).level(Technique.ICE) == 7, "ice should be 7");
		run(context, admin, "technique emeralds @s 320");
		context.assertTrue(TechniqueAttachment.get(p).slotsOpen() == 2, "320 emeralds: 2 slots");
		TechniqueAttachment.set(p, TechniqueAttachment.get(p).withCooldown(Technique.ICE, now(p) + 500).withSlot(1, Technique.ICE));
		run(context, admin, "technique cooldowns @s clear");
		context.assertTrue(!TechniqueAttachment.get(p).onCooldown(Technique.ICE, now(p)), "cooldowns cleared");
		run(context, admin, "technique set @s ice 0");
		context.assertTrue(TechniqueAttachment.get(p).level(Technique.ICE) == 0 && TechniqueAttachment.get(p).inSlot(1) == null, "level 0 also empties the slot");
		context.succeed();
	}

	@GameTest(maxTicks = 100)
	public void syncCodecRoundTrips(GameTestHelper context) {
		TechniqueData d = TechniqueData.DEFAULT.withLevel(Technique.FIRE, 9, 321).withLevel(Technique.SACRIFICE, 2, 14)
			.withEmeralds(700).withSlot(0, Technique.SACRIFICE).withSlot(2, Technique.FIRE).withCooldown(Technique.FIRE, 987654321012L);
		ByteBuf buf = Unpooled.buffer();
		TechniqueAttachment.STREAM_CODEC.encode(buf, d);
		TechniqueData back = TechniqueAttachment.STREAM_CODEC.decode(buf);
		context.assertTrue(d.equals(back), "decoded data differs: " + back);
		context.succeed();
	}
}
