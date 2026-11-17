package io.github.huydangtruong.immortalcultivation;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationGameRulesScreen;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.gamerules.GameRules;

/** D78: the yellow "Immortal Cultivation <version>" game rule group and the yellow key category. Screenshots 95-... and 96-... . */
public class LabelsClientTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		context.setScreen(() -> new WorldCreationGameRulesScreen(new GameRules(FeatureFlags.DEFAULT_FLAGS), r -> { }));
		context.waitTicks(5);
		context.getInput().typeChars("cultivation");
		context.waitTicks(5);
		context.takeScreenshot("95-create-world-game-rules-group");

		context.setScreen(() -> new KeyBindsScreen(null, context.computeOnClient(mc -> mc.options)));
		context.waitTicks(5);
		context.getInput().setCursorPos(200, 150);
		for (int i = 0; i < 40; i++) {
			context.getInput().scroll(-10);
		}
		context.waitTicks(5);
		context.takeScreenshot("96-controls-key-category");
		context.setScreen(() -> null);
	}
}
