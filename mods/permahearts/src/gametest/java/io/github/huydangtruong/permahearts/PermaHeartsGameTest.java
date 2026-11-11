package io.github.huydangtruong.permahearts;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public class PermaHeartsGameTest {
	@GameTest
	public void infrastructureRuns(GameTestHelper context) {
		context.succeed();
	}
}
