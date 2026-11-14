package io.github.huydangtruong.permahearts;

import com.mojang.authlib.GameProfile;
import io.github.huydangtruong.permahearts.technique.Flight;
import io.github.huydangtruong.permahearts.technique.Technique;
import io.github.huydangtruong.permahearts.technique.TechniqueAttachment;
import io.github.huydangtruong.permahearts.technique.TechniqueCasts;
import io.github.huydangtruong.permahearts.technique.TechniqueRules;
import io.github.huydangtruong.permahearts.technique.TechniqueSelection;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** P-014: Flight and Life Steal Blade on a real survival player (the helper's mock player is always Creative). */
public class FlightBladeGameTest {
	private static void enable(GameTestHelper c) {
		MinecraftServer s = c.getLevel().getServer();
		s.getGameRules().set(PermaHearts.ENABLED, true, s);
	}

	private static void floor(GameTestHelper c) {
		for (int x = 0; x < 12; x++) {
			for (int z = 0; z < 12; z++) {
				c.setBlock(x, 0, z, Blocks.STONE);
			}
		}
	}

	private static void place(GameTestHelper c, ServerPlayer p, double x, double y, double z, float yaw, float pitch) {
		Vec3 v = c.absoluteVec(new Vec3(x, y, z));
		p.teleportTo(c.getLevel(), v.x, v.y, v.z, Set.of(), yaw, pitch, false);
	}

	private static ServerPlayer survivor(GameTestHelper c) {
		ServerLevel level = c.getLevel();
		MinecraftServer server = level.getServer();
		GameProfile profile = new GameProfile(UUID.randomUUID(), "fb" + Integer.toHexString((int) (Math.random() * 0xFFFFFF)));
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
		p.setGameMode(GameType.SURVIVAL);
		p.getAbilities().invulnerable = false;
		p.getAbilities().mayfly = false;
		p.getAbilities().flying = false;
		return p;
	}

	private static void drop(GameTestHelper c, ServerPlayer p) {
		c.getLevel().getServer().getPlayerList().remove(p);
	}

	// ---- Flight ----

	@GameTest(maxTicks = 60)
	public void flightGivesFlyingWithSpeedThenSlowFallsAtTheEnd(GameTestHelper c) {
		enable(c);
		floor(c);
		ServerPlayer p = survivor(c);
		place(c, p, 5.5, 1, 5.5, 0f, 0f);
		c.assertTrue(TechniqueCasts.cast(p, Technique.FLIGHT, 10, 1f), "survival player can cast Flight");
		c.assertTrue(p.getAbilities().mayfly && p.getAbilities().flying, "mayfly and flying on");
		float want = io.github.huydangtruong.permahearts.technique.TechniqueRules.flyingSpeed(10);
		c.assertTrue(Math.abs(p.getAbilities().getFlyingSpeed() - want) < 1e-6, "speed as TechniqueRules at level 10, was " + p.getAbilities().getFlyingSpeed());
		long end = p.getAttached(Flight.END);
		long left = end - p.level().getGameTime();
		long ticks = Math.round(io.github.huydangtruong.permahearts.technique.TechniqueRules.flightSeconds(10) * 20);
		c.assertTrue(left == ticks, "level 10 flight = " + ticks + " ticks, was " + left);
		c.assertTrue(Flight.active(p), "flight active");
		p.setAttached(Flight.END, p.level().getGameTime() + 2); // fast-forward
		c.runAfterDelay(6, () -> {
			c.assertTrue(!p.getAbilities().mayfly && !p.getAbilities().flying, "mayfly and flying off at the end");
			c.assertTrue(Math.abs(p.getAbilities().getFlyingSpeed() - 0.05f) < 1e-6, "speed restored");
			c.assertTrue(p.hasEffect(MobEffects.SLOW_FALLING), "slow falling after the end");
			c.assertTrue(p.getAttached(Flight.END) == null, "state cleared");
			drop(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 40)
	public void flightEndsWhenThePlayerDies(GameTestHelper c) {
		enable(c);
		floor(c);
		ServerPlayer p = survivor(c);
		place(c, p, 5.5, 1, 5.5, 0f, 0f);
		TechniqueCasts.cast(p, Technique.FLIGHT, 1, 1f);
		c.assertTrue(p.getAbilities().mayfly, "flying before death");
		p.kill(c.getLevel());
		c.runAfterDelay(3, () -> {
			c.assertTrue(p.getAttached(Flight.END) == null, "state cleared on death");
			c.assertTrue(!p.getAbilities().mayfly, "mayfly cleared on death");
			drop(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 40)
	public void flightLeavesCreativePlayersAlone(GameTestHelper c) {
		enable(c);
		ServerPlayer p = c.makeMockServerPlayerInLevel(); // always Creative
		float speed = p.getAbilities().getFlyingSpeed();
		boolean may = p.getAbilities().mayfly;
		c.assertTrue(!TechniqueCasts.cast(p, Technique.FLIGHT, 20, 1f), "creative: cast refused");
		c.assertTrue(p.getAttached(Flight.END) == null, "no state for a creative player");
		c.assertTrue(p.getAbilities().getFlyingSpeed() == speed && p.getAbilities().mayfly == may, "abilities untouched");
		c.succeed();
	}

	// ---- Life Steal Blade ----

	private static void arm(ServerPlayer p, int level) {
		TechniqueAttachment.set(p, TechniqueAttachment.get(p).withLevel(Technique.BLADE, level, 10).withEmeralds(TechniqueRules.MAX_EMERALDS).withSlot(0, Technique.BLADE));
		TechniqueSelection.set(p, 0);
	}

	private static Husk husk(GameTestHelper c) {
		Husk h = c.spawn(EntityTypes.HUSK, new Vec3(5.5, 1, 7.5));
		h.setNoAi(true);
		return h;
	}

	@GameTest(maxTicks = 40)
	public void bladeHitDamageHealAndHeldSwordDoesNotCount(GameTestHelper c) {
		enable(c);
		floor(c);
		ServerPlayer p = survivor(c);
		place(c, p, 5.5, 1, 5.0, 0f, 0f); // looking along +Z at the husk 2.5 blocks away
		Husk h = husk(c);
		arm(p, 1);
		p.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD)); // must not add damage
		p.setHealth(5f);
		float hh = h.getHealth();
		String error = TechniqueCasts.bladeAttack(p, h.getId());
		c.assertTrue(error == null, "hit refused: " + error);
		float dealt = hh - h.getHealth();
		c.assertTrue(dealt <= 6f && dealt > 5.8f, "base 1 + 5 = 6 damage minus the Husk armour, was " + dealt);
		c.assertTrue(Math.abs(p.getHealth() - (5f + dealt)) < 1e-3, "healed by the damage dealt, was " + p.getHealth());
		c.assertTrue(TechniqueAttachment.get(p).onCooldown(Technique.BLADE, c.getLevel().getServer().overworld().getGameTime()), "landed hit starts the cooldown");
		String again = TechniqueCasts.bladeAttack(p, h.getId());
		c.assertTrue(again != null && again.startsWith("On cooldown"), "second hit refused: " + again);
		drop(c, p);
		c.succeed();
	}

	@GameTest(maxTicks = 40)
	public void bladeAddsTheStrengthElixirBonus(GameTestHelper c) {
		enable(c);
		floor(c);
		ServerPlayer p = survivor(c);
		place(c, p, 5.5, 1, 5.0, 0f, 0f);
		Husk h = husk(c);
		arm(p, 2); // +5.5
		p.getAttribute(Attributes.ATTACK_DAMAGE).addPermanentModifier(new AttributeModifier(
			Identifier.fromNamespaceAndPath(PermaHearts.MOD_ID, "test_strength"), 2.0, AttributeModifier.Operation.ADD_VALUE));
		float hh = h.getHealth();
		p.setHealth(10f);
		c.assertTrue(TechniqueCasts.bladeAttack(p, h.getId()) == null, "hit works");
		float dealt = hh - h.getHealth();
		double want = 1 + 2 + TechniqueRules.bladeDamage(2);
		c.assertTrue(dealt <= want && dealt > want - 0.3, "base + elixir + blade = " + want + ", was " + dealt);
		c.assertTrue(Math.abs(p.getHealth() - (10 + dealt)) < 1e-3, "heal equals damage dealt");
		drop(c, p);
		c.succeed();
	}

	@GameTest(maxTicks = 40)
	public void bladeRefusesOutOfRangeAndWhenNotSelected(GameTestHelper c) {
		enable(c);
		floor(c);
		ServerPlayer p = survivor(c);
		place(c, p, 1.5, 1, 1.5, 0f, 0f);
		Husk h = c.spawn(EntityTypes.HUSK, new Vec3(1.5, 1, 10.5));
		h.setNoAi(true);
		c.assertTrue(TechniqueCasts.bladeAttack(p, h.getId()) != null, "not selected: refused");
		arm(p, 1);
		c.assertTrue(TechniqueCasts.bladeAttack(p, h.getId()) != null, "9 blocks away: refused");
		c.assertTrue(h.getHealth() == h.getMaxHealth(), "no damage");
		drop(c, p);
		c.succeed();
	}
}
