package io.github.huydangtruong.permahearts;

import io.github.huydangtruong.permahearts.reward.RewardBoxData;
import io.github.huydangtruong.permahearts.reward.RewardBoxMenu;
import io.github.huydangtruong.permahearts.reward.RewardBoxes;
import io.github.huydangtruong.permahearts.reward.RewardOption;
import io.github.huydangtruong.permahearts.reward.RewardTier;
import io.github.huydangtruong.permahearts.wanderer.Quest;
import io.github.huydangtruong.permahearts.wanderer.QuestLine;
import io.github.huydangtruong.permahearts.wanderer.QuestType;
import io.github.huydangtruong.permahearts.wanderer.Wanderer;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

/**
 * P-027: screenshots of the reward box screen (gold box, one card selected, then the tooltip) and of the quest screen with
 * boxes as rewards. Not a check of the picture: a human looks at them. Run with ./gradlew :permahearts:runClientGameTest.
 */
public class RewardBoxClientTest implements FabricClientGameTest {
	private static Quest quest() {
		return new Quest(1, List.of(
			new QuestLine(QuestType.COLLECT, "minecraft:iron_ingot", 64, 0, "strength", false),
			new QuestLine(QuestType.EXPLORE, "minecraft:igloo", 1, 0, "fury", false),
			new QuestLine(QuestType.BOSS, "minecraft:evoker", 1, 0, "iron_skin", false),
			new QuestLine(QuestType.KILL, "minecraft:zombie", 16, 0, "swiftness", false)), false);
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			server.runCommand("gamerule permahearts:immortal_cultivation true");
			server.runCommand("time set day");
			server.runCommand("weather clear");
			server.runCommand("gamemode survival @a");
			context.waitTicks(40);
			boxScreen(context, server, RewardTier.SILVER, "89-reward-box-silver");
			boxScreen(context, server, RewardTier.GOLD, "90-reward-box-gold");
			boxScreen(context, server, RewardTier.DIAMOND, "93-reward-box-diamond");
			questScreen(context, server);
		}
	}

	private static void boxScreen(ClientGameTestContext context, TestServerContext server, RewardTier tier, String name) {
		server.runOnServer(s -> {
			ServerPlayer p = s.getPlayerList().getPlayers().get(0);
			p.getInventory().clearContent();
			ItemStackHelper.give(p, tier);
			// A fixed roll so the picture is repeatable and shows every kind of card.
			List<RewardOption> options = tier == RewardTier.SILVER
				? List.of(new RewardOption("minecraft:iron_ingot", 64), new RewardOption("minecraft:gold_ingot", 64),
					new RewardOption("permahearts:elixir_strength", 1))
				: tier == RewardTier.GOLD
				? List.of(new RewardOption("minecraft:diamond", 16), new RewardOption("permahearts:elixir_fury", 1),
					new RewardOption("minecraft:enchanted_golden_apple", 4), new RewardOption("minecraft:ender_pearl", 32))
				: List.of(new RewardOption("minecraft:netherite_ingot", 1), new RewardOption("permahearts:elixir_iron_skin", 1),
					new RewardOption("minecraft:redstone", 64), new RewardOption("minecraft:emerald", 32),
					new RewardOption("permahearts:elixir_reach", 1));
			p.getMainHandItem().set(RewardBoxes.DATA, new RewardBoxData(options));
			RewardBoxes.open(p, InteractionHand.MAIN_HAND);
		});
		context.waitForScreen(AbstractContainerScreen.class);
		context.waitTicks(10);
		context.takeScreenshot(name + "-open"); // nothing selected: Claim is disabled
		// A real click (mouse input, not a faked event) on the second card: right button on the diamond box, left on the others.
		clickGui(context, 8 + 54 + 25, 30 + 40,
			tier == RewardTier.DIAMOND ? InputConstants.MOUSE_BUTTON_RIGHT : InputConstants.MOUSE_BUTTON_LEFT);
		context.waitTicks(10);
		int mask = server.computeOnServer(s -> {
			ServerPlayer sp = s.getPlayerList().getPlayers().get(0);
			if (!(sp.containerMenu instanceof RewardBoxMenu m)) {
				throw new AssertionError("menu closed: " + sp.containerMenu + " hand " + sp.getMainHandItem() + " data " + sp.getMainHandItem().get(RewardBoxes.DATA));
			}
			return m.selected();
		});
		if (mask != 0b10) {
			throw new AssertionError("a click on card 1 should select only it, mask was " + mask);
		}
		// Hover the first card: its item tooltip.
		double[] hover = context.computeOnClient(mc -> {
			double scale = mc.getWindow().getGuiScale();
			int[] at = panelOrigin(mc);
			int left = at[0];
			int top = at[1];
			return new double[] {(left + 8 + 25) * scale, (top + 30 + 20) * scale};
		});
		context.getInput().setCursorPos(hover[0], hover[1]);
		context.waitTicks(5);
		context.takeScreenshot(name + "-selected-tooltip");
		context.getInput().setCursorPos(5, 5);
		context.waitTicks(5);
		context.takeScreenshot(name + "-selected");
		// A real click on Claim: the box is used up and card 1 lands in the inventory.
		String card1 = switch (tier) {
			case SILVER -> "minecraft:gold_ingot";
			case GOLD -> "permahearts:elixir_fury";
			case DIAMOND -> "permahearts:elixir_iron_skin";
		};
		clickGui(context, -1, 30 + 80 + 8 + 10, InputConstants.MOUSE_BUTTON_LEFT);
		context.waitTicks(10);
		String claimed = server.computeOnServer(s -> {
			ServerPlayer sp = s.getPlayerList().getPlayers().get(0);
			return (sp.containerMenu instanceof RewardBoxMenu ? "menu still open; " : "")
				+ (RewardBoxes.tierOf(sp.getMainHandItem()).isPresent() ? "box still in hand; " : "")
				+ (sp.getInventory().contains(st -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(st.getItem()).toString().equals(card1))
					? "" : "card 1 (" + card1 + ") not given; ")
				+ "inventory " + sp.getInventory().getNonEquipmentItems();
		});
		if (claimed.contains("still") || claimed.contains("not given")) {
			throw new AssertionError(tier + " claim by mouse failed: " + claimed);
		}
		context.waitTicks(5);
	}

	/** A real left click at GUI coordinates relative to the box panel's top-left; x = -1 means the Claim button's centre. */
	private static void clickGui(ClientGameTestContext context, int dx, int dy, int button) {
		double[] at = context.computeOnClient(mc -> {
			double scale = mc.getWindow().getGuiScale();
			int[] o = panelOrigin(mc);
			int w = 16 + 54 * ((RewardBoxMenu) mc.player.containerMenu).options().size() - 4;
			int x = dx >= 0 ? o[0] + dx : o[0] + w / 2 - 4 - 35;
			return new double[] {x * scale, (o[1] + dy) * scale};
		});
		context.getInput().setCursorPos(at[0], at[1]);
		context.waitTicks(2);
		context.getInput().pressMouse(button);
	}

	/** Top-left of the box screen: it is centred, 16 + 54 per card - 4 wide and 146 high (same numbers as RewardBoxScreen). */
	private static int[] panelOrigin(net.minecraft.client.Minecraft mc) {
		int n = ((RewardBoxMenu) mc.player.containerMenu).options().size();
		return new int[] {(mc.getWindow().getGuiScaledWidth() - (16 + 54 * n - 4)) / 2, (mc.getWindow().getGuiScaledHeight() - 146) / 2};
	}

	/** Done lines of a quest show their box as the reward (silver for collect and kill, gold for explore, diamond for a boss). */
	private static void questScreen(ClientGameTestContext context, TestServerContext server) {
		float[] yawAtSpawn = new float[1];
		server.runOnServer(s -> {
			Wanderer.reset(s);
			ServerPlayer p = s.getPlayerList().getPlayers().get(0);
			yawAtSpawn[0] = p.getYRot();
			Wanderer.startQuestForTest(p.level(), p.blockPosition().relative(p.getDirection(), 3), quest());
			Wanderer.accept(s);
			Wanderer.complete(s, 0);
			Wanderer.complete(s, 1);
			Wanderer.complete(s, 2);
		});
		context.runOnClient(mc -> {
			mc.player.setYRot(yawAtSpawn[0]);
			mc.player.setXRot(5f);
		});
		context.waitTicks(20);
		context.getInput().pressKey(o -> o.keyUse); // right-click the Wanderer
		context.waitForScreen(AbstractContainerScreen.class);
		context.waitTicks(10);
		context.takeScreenshot("92-quest-box-rewards");
		context.runOnClient(mc -> mc.player.closeContainer());
		server.runOnServer(Wanderer::reset);
	}

	/** Gives an unrolled box of the tier to the player's main hand. */
	private static final class ItemStackHelper {
		static void give(ServerPlayer p, RewardTier tier) {
			p.setItemInHand(InteractionHand.MAIN_HAND, RewardBoxes.stack(tier));
		}
	}
}
