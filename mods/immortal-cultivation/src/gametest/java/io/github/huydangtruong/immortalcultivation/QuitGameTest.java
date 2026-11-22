package io.github.huydangtruong.immortalcultivation;

import io.github.huydangtruong.immortalcultivation.quit.Quit;
import io.github.huydangtruong.immortalcultivation.quit.QuitCountdown;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** D218: /quit starts a countdown, a second run cancels it, zero calls the final step once. The real step (save + close) is replaced by a counter. */
public class QuitGameTest {
	private static void run(MinecraftServer server, ServerPlayer p) throws Exception {
		server.getCommands().getDispatcher().execute("quit", server.createCommandSourceStack().withEntity(p));
	}

	@GameTest
	public void startCancelAndFire(GameTestHelper c) {
		ServerPlayer p = c.makeMockServerPlayerInLevel();
		MinecraftServer server = c.getLevel().getServer();
		var oldHosted = Quit.hosted;
		var oldFinal = Quit.finalStep;
		AtomicInteger fired = new AtomicInteger();
		Quit.hosted = s -> true;
		Quit.finalStep = s -> fired.incrementAndGet();
		QuitCountdown cd = Quit.countdown();
		try {
			cd.cancel();
			run(server, p);
			if (!cd.active() || cd.ticksLeft() < 150) {
				c.fail("/quit did not start the countdown: active=" + cd.active() + " left=" + cd.ticksLeft());
				return;
			}
			run(server, p);
			if (cd.active()) {
				c.fail("a second /quit did not cancel");
				return;
			}
			for (int i = 0; i < 300; i++) {
				Quit.tick(server);
			}
			if (fired.get() != 0) {
				c.fail("the final step ran after a cancel");
				return;
			}
			run(server, p);
			for (int i = 0; i < QuitCountdown.TOTAL_TICKS + 5; i++) {
				Quit.tick(server);
			}
			if (fired.get() != 1 || cd.active()) {
				c.fail("expected the final step once, got " + fired.get());
				return;
			}
			c.succeed();
		} catch (Exception e) {
			c.fail("quit failed: " + e);
		} finally {
			cd.cancel();
			Quit.hosted = oldHosted;
			Quit.finalStep = oldFinal;
		}
	}

	@GameTest
	public void dedicatedServerDoesNothing(GameTestHelper c) {
		ServerPlayer p = c.makeMockServerPlayerInLevel();
		MinecraftServer server = c.getLevel().getServer();
		var oldHosted = Quit.hosted;
		Quit.hosted = s -> false;
		try {
			Quit.countdown().cancel();
			try {
				run(server, p);
			} catch (Exception expected) {
				// the command fails with a message; nothing may start either way
			}
			if (Quit.countdown().active()) {
				c.fail("/quit started a countdown on a dedicated server");
				return;
			}
			c.succeed();
		} finally {
			Quit.hosted = oldHosted;
		}
	}
}
