package io.github.huydangtruong.permahearts;

import com.mojang.serialization.JsonOps;
import io.github.huydangtruong.permahearts.reward.RewardBoxData;
import io.github.huydangtruong.permahearts.reward.RewardBoxMenu;
import io.github.huydangtruong.permahearts.reward.RewardBoxes;
import io.github.huydangtruong.permahearts.reward.RewardOption;
import io.github.huydangtruong.permahearts.reward.RewardRoll;
import io.github.huydangtruong.permahearts.reward.RewardTier;
import io.github.huydangtruong.permahearts.wanderer.Quest;
import io.github.huydangtruong.permahearts.wanderer.QuestLine;
import io.github.huydangtruong.permahearts.wanderer.QuestType;
import io.github.huydangtruong.permahearts.wanderer.Wanderer;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.phys.AABB;

/**
 * P-027 / D76: reward boxes. Quest claims give the tier's box, the first open rolls and stores the cards, Claim gives exactly
 * the chosen card and consumes the box, and a forged selection is refused. The screen itself is in RewardBoxClientTest.
 */
public class RewardBoxGameTest {
	private static void enable(GameTestHelper context, boolean on) {
		MinecraftServer s = context.getLevel().getServer();
		s.getGameRules().set(PermaHearts.ENABLED, on, s);
	}

	private static ItemStack fixedBox(RewardTier tier, RewardOption... options) {
		ItemStack box = RewardBoxes.stack(tier);
		box.set(RewardBoxes.DATA, new RewardBoxData(List.of(options)));
		return box;
	}

	/** D90: a claim gives some reward box (tier rolled then), unrolled; a line saved before reward boxes keeps its elixir id and is ignored. */
	@GameTest(maxTicks = 100)
	public void claimGivesARandomUnrolledBox(GameTestHelper context) {
		MinecraftServer s = context.getLevel().getServer();
		try {
			enable(context, true);
			Wanderer.reset(s);
			Quest q = new Quest(1, List.of(
				new QuestLine(QuestType.COLLECT, "minecraft:iron_ingot", 10, 0, "strength", false), // old save: elixir id
				new QuestLine(QuestType.BOSS, "minecraft:evoker", 1, 0, "iron_skin", false),
				new QuestLine(QuestType.EXPLORE, "minecraft:igloo", 1, 0, "fury", false),
				new QuestLine(QuestType.KILL, "minecraft:zombie", 2, 0, "not_a_stat", false)), false);
			context.assertTrue(Wanderer.startQuestForTest(context.getLevel(), context.absolutePos(new BlockPos(1, 1, 1)), q) != null,
				"NPC should spawn");
			Wanderer.accept(s);
			ServerPlayer p = context.makeMockServerPlayerInLevel();
			for (int i = 0; i < 4; i++) {
				Wanderer.complete(s, i);
			}
			for (int i = 0; i < 4; i++) {
				ItemStack got = Wanderer.claim(s, i, p);
				context.assertTrue(RewardBoxes.tierOf(got).isPresent(), "line " + i + " gives a reward box, got " + got);
				context.assertTrue(got.get(RewardBoxes.DATA) == null, "the box is not rolled yet");
			}
			context.succeed();
		} finally {
			Wanderer.reset(s);
		}
	}

	@GameTest(maxTicks = 100)
	public void boxStacksToOneAndStartsUnrolled(GameTestHelper context) {
		for (RewardTier t : RewardTier.values()) {
			ItemStack box = RewardBoxes.stack(t);
			context.assertTrue(box.getMaxStackSize() == 1, t + " box stacks to 1");
			context.assertTrue(box.get(RewardBoxes.DATA) == null, t + " box starts unrolled");
		}
		context.succeed();
	}

	@GameTest(maxTicks = 100)
	public void openRollsAndStoresThenKeepsTheSameCards(GameTestHelper context) {
		ServerPlayer p = context.makeMockServerPlayerInLevel();
		p.setItemInHand(InteractionHand.MAIN_HAND, RewardBoxes.stack(RewardTier.GOLD));
		RewardBoxes.open(p, InteractionHand.MAIN_HAND);
		RewardBoxData first = p.getMainHandItem().get(RewardBoxes.DATA);
		context.assertTrue(first != null && first.options().size() == 4, "a gold box rolls 4 cards, was " + first);
		context.assertTrue(p.containerMenu instanceof RewardBoxMenu m && m.options().equals(first.options()),
			"the menu shows the stored cards");
		p.closeContainer();
		RewardBoxes.open(p, InteractionHand.MAIN_HAND);
		RewardBoxData second = p.getMainHandItem().get(RewardBoxes.DATA);
		context.assertTrue(first.equals(second), "opening again keeps the same cards: " + first + " vs " + second);
		context.assertTrue(p.containerMenu instanceof RewardBoxMenu m && m.options().equals(first.options()),
			"the menu shows the same cards again");
		// Dropped, stored in a chest, relogged: the component is saved with the stack (codec round trip).
		var json = RewardBoxData.CODEC.encodeStart(JsonOps.INSTANCE, first).getOrThrow();
		context.assertTrue(RewardBoxData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow().equals(first), "codec round trip");
		ItemStack copy = p.getMainHandItem().copy();
		context.assertTrue(first.equals(copy.get(RewardBoxes.DATA)), "a copy of the stack keeps the cards");
		context.succeed();
	}

	@GameTest(maxTicks = 100)
	public void claimGivesExactlyTheChosenCard(GameTestHelper context) {
		ServerPlayer p = context.makeMockServerPlayerInLevel();
		p.getInventory().clearContent();
		p.setItemInHand(InteractionHand.MAIN_HAND, fixedBox(RewardTier.GOLD,
			new RewardOption("minecraft:diamond", 16), RewardRoll.elixir(ElixirStat.FURY), new RewardOption("minecraft:iron_ingot", 64),
			new RewardOption("minecraft:enchanted_golden_apple", 4)));
		RewardBoxes.open(p, InteractionHand.MAIN_HAND);
		RewardBoxMenu menu = (RewardBoxMenu) p.containerMenu;
		context.assertTrue(!menu.canClaim(), "Claim is disabled before a card is selected");
		context.assertTrue(!menu.clickMenuButton(p, RewardBoxMenu.CLAIM_BUTTON), "Claim without a selection does nothing");
		context.assertTrue(!p.getMainHandItem().isEmpty(), "the box is kept");
		menu.clickMenuButton(p, 0);
		menu.clickMenuButton(p, 1); // moves the selection to the elixir
		context.assertTrue(menu.selected() == 0b10, "one card selected, was " + menu.selected());
		context.assertTrue(menu.canClaim(), "Claim enabled");
		context.assertTrue(menu.clickMenuButton(p, RewardBoxMenu.CLAIM_BUTTON), "claim works");
		context.assertTrue(p.getInventory().countItem(RewardBoxes.item(RewardTier.GOLD)) == 0, "the box is consumed");
		Item fury = ElixirItems.get(ElixirStat.FURY);
		context.assertTrue(p.getInventory().countItem(fury) == 1, "got exactly one fury elixir");
		context.assertTrue(p.getInventory().countItem(Items.DIAMOND) == 0 && p.getInventory().countItem(Items.IRON_INGOT) == 0
			&& p.getInventory().countItem(Items.ENCHANTED_GOLDEN_APPLE) == 0, "nothing else was given");
		context.succeed();
	}


	@GameTest(maxTicks = 100)
	public void claimGivesTheItemAndItsCount(GameTestHelper context) {
		ServerPlayer p = context.makeMockServerPlayerInLevel();
		p.getInventory().clearContent();
		p.setItemInHand(InteractionHand.MAIN_HAND, fixedBox(RewardTier.SILVER,
			new RewardOption("minecraft:gunpowder", 32), new RewardOption("minecraft:diamond", 16),
			new RewardOption("minecraft:iron_ingot", 64)));
		RewardBoxes.open(p, InteractionHand.MAIN_HAND);
		RewardBoxMenu menu = (RewardBoxMenu) p.containerMenu;
		menu.clickMenuButton(p, 2);
		context.assertTrue(menu.clickMenuButton(p, RewardBoxMenu.CLAIM_BUTTON), "claim works");
		context.assertTrue(p.getInventory().countItem(Items.IRON_INGOT) == 64, "64 iron ingots");
		context.assertTrue(p.getInventory().countItem(Items.GUNPOWDER) == 0, "no gunpowder");
		context.assertTrue(p.getInventory().countItem(RewardBoxes.item(RewardTier.SILVER)) == 0, "box consumed");
		context.succeed();
	}

	@GameTest(maxTicks = 100)
	public void forgedSelectionIsRefusedAndTheBoxIsKept(GameTestHelper context) {
		ServerPlayer p = context.makeMockServerPlayerInLevel();
		p.getInventory().clearContent();
		p.setItemInHand(InteractionHand.MAIN_HAND, fixedBox(RewardTier.SILVER,
			new RewardOption("minecraft:diamond", 16), new RewardOption("minecraft:iron_ingot", 64),
			new RewardOption("minecraft:gunpowder", 32)));
		RewardBoxes.open(p, InteractionHand.MAIN_HAND);
		RewardBoxMenu menu = (RewardBoxMenu) p.containerMenu;
		context.assertTrue(!menu.clickMenuButton(p, 7), "card 7 does not exist in a 3-card box");
		context.assertTrue(!menu.clickMenuButton(p, 3), "card 3 does not exist in a 3-card box");
		context.assertTrue(!menu.clickMenuButton(p, -1), "negative card");
		context.assertTrue(menu.selected() == 0, "a forged click selects nothing, was " + menu.selected());
		context.assertTrue(!RewardBoxes.claim(p, InteractionHand.MAIN_HAND, 1 << 7), "mask with a card out of range");
		context.assertTrue(!RewardBoxes.claim(p, InteractionHand.MAIN_HAND, 1 << 3), "mask with card 3");
		context.assertTrue(!RewardBoxes.claim(p, InteractionHand.MAIN_HAND, 0), "empty mask");
		context.assertTrue(!RewardBoxes.claim(p, InteractionHand.MAIN_HAND, 0b11), "two cards when only one pick");
		context.assertTrue(!RewardBoxes.claim(p, InteractionHand.MAIN_HAND, -1), "all bits");
		context.assertTrue(!RewardBoxes.claim(p, InteractionHand.OFF_HAND, 1), "no box in the other hand");
		context.assertTrue(p.getMainHandItem().is(RewardBoxes.item(RewardTier.SILVER)), "the box is kept");
		context.assertTrue(p.getInventory().countItem(Items.DIAMOND) == 0 && p.getInventory().countItem(Items.IRON_INGOT) == 0
			&& p.getInventory().countItem(Items.GUNPOWDER) == 0, "nothing was given");
		context.succeed();
	}

	private static RewardOption[] sixCards() {
		return new RewardOption[] {
			new RewardOption("minecraft:diamond", 16), RewardRoll.elixir(ElixirStat.FURY), new RewardOption("minecraft:iron_ingot", 64),
			RewardOption.book("minecraft:mending", 1), new RewardOption("minecraft:totem_of_undying", 1),
			RewardOption.book("minecraft:sharpness", 5)};
	}

	/** D89: the emerald box takes two different cards, then Claim gives both and uses the box up. */
	@GameTest(maxTicks = 100)
	public void emeraldBoxTwoPicksGiveBothAndConsumeTheBox(GameTestHelper context) {
		ServerPlayer p = context.makeMockServerPlayerInLevel();
		p.getInventory().clearContent();
		p.setItemInHand(InteractionHand.MAIN_HAND, fixedBox(RewardTier.EMERALD, sixCards()));
		RewardBoxes.open(p, InteractionHand.MAIN_HAND);
		RewardBoxMenu menu = (RewardBoxMenu) p.containerMenu;
		context.assertTrue(menu.options().size() == 6 && menu.tier().picks() == 2, "6 cards, 2 picks");
		menu.clickMenuButton(p, 0);
		context.assertTrue(!menu.canClaim(), "one pick is not enough");
		context.assertTrue(!menu.clickMenuButton(p, RewardBoxMenu.CLAIM_BUTTON), "Claim with one pick does nothing");
		context.assertTrue(!p.getMainHandItem().isEmpty(), "the box is kept after one pick");
		menu.clickMenuButton(p, 4);
		menu.clickMenuButton(p, 2); // a third card is ignored
		context.assertTrue(menu.selected() == 0b10001, "two cards selected, was " + menu.selected());
		context.assertTrue(menu.clickMenuButton(p, RewardBoxMenu.CLAIM_BUTTON), "claim works");
		context.assertTrue(p.getInventory().countItem(Items.DIAMOND) == 16, "16 diamonds");
		context.assertTrue(p.getInventory().countItem(Items.TOTEM_OF_UNDYING) == 1, "a totem");
		context.assertTrue(p.getInventory().countItem(Items.IRON_INGOT) == 0, "nothing else");
		context.assertTrue(p.getInventory().countItem(RewardBoxes.item(RewardTier.EMERALD)) == 0, "the box is used up");
		context.succeed();
	}

	/** Closing the screen after one pick takes nothing: the box stays whole and can be opened again. */
	@GameTest(maxTicks = 100)
	public void emeraldBoxClosedAfterOnePickKeepsEverything(GameTestHelper context) {
		ServerPlayer p = context.makeMockServerPlayerInLevel();
		p.getInventory().clearContent();
		p.setItemInHand(InteractionHand.MAIN_HAND, fixedBox(RewardTier.EMERALD, sixCards()));
		RewardBoxes.open(p, InteractionHand.MAIN_HAND);
		((RewardBoxMenu) p.containerMenu).clickMenuButton(p, 1);
		p.closeContainer();
		context.assertTrue(p.getMainHandItem().is(RewardBoxes.item(RewardTier.EMERALD)), "box kept");
		context.assertTrue(!RewardBoxes.claim(p, InteractionHand.MAIN_HAND, 0b10), "a one-card claim is refused");
		context.assertTrue(!RewardBoxes.claim(p, InteractionHand.MAIN_HAND, 0b111), "a three-card claim is refused");
		context.assertTrue(p.getInventory().countItem(Items.DIAMOND) == 0 && p.getInventory().countItem(Items.IRON_INGOT) == 0, "nothing given");
		RewardBoxes.open(p, InteractionHand.MAIN_HAND);
		context.assertTrue(((RewardBoxMenu) p.containerMenu).selected() == 0, "reopened with no pick");
		context.succeed();
	}

	/** A Mending card gives a book that stores Mending; Sharpness V stores level 5. */
	@GameTest(maxTicks = 100)
	public void enchantedBookCardsCarryTheirEnchantment(GameTestHelper context) {
		ServerPlayer p = context.makeMockServerPlayerInLevel();
		p.getInventory().clearContent();
		p.setItemInHand(InteractionHand.MAIN_HAND, fixedBox(RewardTier.EMERALD, sixCards()));
		RewardBoxes.open(p, InteractionHand.MAIN_HAND);
		RewardBoxMenu menu = (RewardBoxMenu) p.containerMenu;
		menu.clickMenuButton(p, 3);
		menu.clickMenuButton(p, 5);
		context.assertTrue(menu.clickMenuButton(p, RewardBoxMenu.CLAIM_BUTTON), "claim works");
		var enchants = p.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		int mending = 0;
		int sharp = 0;
		int books = 0;
		for (ItemStack st : p.getInventory().getNonEquipmentItems()) {
			if (st.is(Items.ENCHANTED_BOOK)) {
				books++;
				ItemEnchantments stored = st.get(DataComponents.STORED_ENCHANTMENTS);
				context.assertTrue(stored != null, "a book stores its enchantment");
				mending += stored.getLevel(enchants.getOrThrow(Enchantments.MENDING));
				sharp += stored.getLevel(enchants.getOrThrow(Enchantments.SHARPNESS));
			}
		}
		context.assertTrue(books == 2 && mending == 1 && sharp == 5, "books " + books + " mending " + mending + " sharpness " + sharp);
		context.succeed();
	}

	@GameTest(maxTicks = 100)
	public void bottleOfEnlightenmentGivesSixteenLevelsAndIsConsumed(GameTestHelper context) {
		ServerPlayer p = context.makeMockServerPlayerInLevel();
		p.getInventory().clearContent();
		p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
		p.experienceLevel = 0;
		p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(RewardBoxes.BOTTLE, 2));
		ItemStack hand = p.getMainHandItem();
		context.assertTrue(hand.getUseDuration(p) == 32, "drinks for 32 ticks, was " + hand.getUseDuration(p));
		hand.finishUsingItem(p.level(), p);
		context.assertTrue(p.experienceLevel == 16, "16 levels, was " + p.experienceLevel);
		context.assertTrue(p.getMainHandItem().getCount() == 1, "one bottle consumed, left " + p.getMainHandItem().getCount());
		p.getMainHandItem().finishUsingItem(p.level(), p);
		context.assertTrue(p.experienceLevel == 32, "second bottle: 32 levels, was " + p.experienceLevel);
		context.succeed();
	}
}
