package io.github.huydangtruong.immortalcultivation;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Helpers shared by the server game tests (not a test class itself: it has no @GameTest methods). */
public final class GameTestKit {
	private static final AtomicInteger PROFILES = new AtomicInteger();

	private GameTestKit() {
	}

	/** Turns the mod's master game rule on. */
	public static void enable(GameTestHelper c) {
		MinecraftServer s = c.getLevel().getServer();
		s.getGameRules().set(ImmortalCultivation.ENABLED, true, s);
	}

	/** A 12 x 12 stone floor at y 0 of the test structure. */
	public static void floor(GameTestHelper c) {
		for (int x = 0; x < 12; x++) {
			for (int z = 0; z < 12; z++) {
				c.setBlock(x, 0, z, Blocks.STONE);
			}
		}
	}

	/** Teleports the player to a point given relative to the test structure. */
	public static void place(GameTestHelper c, ServerPlayer p, double x, double y, double z, float yaw, float pitch) {
		Vec3 v = c.absoluteVec(new Vec3(x, y, z));
		p.teleportTo(c.getLevel(), v.x, v.y, v.z, Set.of(), yaw, pitch, false);
	}

	/** A survival player that takes damage (the helper's mock is Creative and never "loaded"). */
	public static ServerPlayer survivor(GameTestHelper c) {
		ServerLevel level = c.getLevel();
		MinecraftServer server = level.getServer();
		GameProfile profile = new GameProfile(UUID.randomUUID(), "gt" + Integer.toHexString(PROFILES.incrementAndGet()));
		CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
		ServerPlayer p = new ServerPlayer(server, level, cookie.gameProfile(), cookie.clientInformation()) {
			@Override
			public GameType gameMode() {
				return GameType.SURVIVAL;
			}
		};
		Connection connection = new Connection(PacketFlow.SERVERBOUND);
		new EmbeddedChannel(connection);
		server.getPlayerList().placeNewPlayer(connection, p, cookie);
		p.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
		p.setGameMode(GameType.SURVIVAL); // placeNewPlayer used the world's default mode: drop creative abilities
		p.getAbilities().invulnerable = false;
		p.getAbilities().mayfly = false;
		p.getAbilities().flying = false;
		return p;
	}

	/** Removes a player made by {@link #survivor} from the server. */
	public static void drop(GameTestHelper c, ServerPlayer p) {
		c.getLevel().getServer().getPlayerList().remove(p);
	}
}
