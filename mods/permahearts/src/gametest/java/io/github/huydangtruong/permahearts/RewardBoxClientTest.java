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
			List<RewardOption> options = tier == RewardTier.GOLD
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
		// A real click on the second card, in GUI coordinates (the screen's own mouseClicked).
		context.runOnClient(mc -> {
			var screen = (AbstractContainerScreen<?>) mc.gui.screen();
			int[] at = panelOrigin(mc);
			int left = at[0];
			int top = at[1];
			screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(left + 8 + 54 + 25, top + 30 + 40,
				new net.minecraft.client.input.MouseButtonInfo(0, 0)), false);
		});
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
		context.runOnClient(mc -> mc.player.closeContainer());
		context.waitTicks(5);
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
