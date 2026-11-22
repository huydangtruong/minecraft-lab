package io.github.huydangtruong.immortalcultivation;

import com.mojang.authlib.GameProfile;
import io.github.huydangtruong.immortalcultivation.technique.Flight;
import io.github.huydangtruong.immortalcultivation.technique.Technique;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueAttachment;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueCasts;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueRules;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueSelection;
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
	// ---- Flight ----

	@GameTest(maxTicks = 60)
	public void flightGivesFlyingWithSpeedThenSlowFallsAtTheEnd(GameTestHelper c) {
		GameTestKit.enable(c);
		GameTestKit.floor(c);
		ServerPlayer p = GameTestKit.survivor(c);
		GameTestKit.place(c, p, 5.5, 1, 5.5, 0f, 0f);
		c.assertTrue(TechniqueCasts.cast(p, Technique.FLIGHT, 10, 1f), "survival player can cast Flight");
		c.assertTrue(p.getAbilities().mayfly && p.getAbilities().flying, "mayfly and flying on");
		float want = io.github.huydangtruong.immortalcultivation.technique.TechniqueRules.flyingSpeed(10);
		c.assertTrue(Math.abs(p.getAbilities().getFlyingSpeed() - want) < 1e-6, "speed as TechniqueRules at level 10, was " + p.getAbilities().getFlyingSpeed());
		long end = p.getAttached(Flight.END);
		long left = end - p.level().getGameTime();
		long ticks = Math.round(io.github.huydangtruong.immortalcultivation.technique.TechniqueRules.flightSeconds(10) * 20);
		c.assertTrue(left == ticks, "level 10 flight = " + ticks + " ticks, was " + left);
		c.assertTrue(Flight.active(p), "flight active");
		p.setAttached(Flight.END, p.level().getGameTime() + 2); // fast-forward
		c.runAfterDelay(6, () -> {
			c.assertTrue(!p.getAbilities().mayfly && !p.getAbilities().flying, "mayfly and flying off at the end");
			c.assertTrue(Math.abs(p.getAbilities().getFlyingSpeed() - 0.05f) < 1e-6, "speed restored");
			c.assertTrue(p.hasEffect(MobEffects.SLOW_FALLING), "slow falling after the end");
			c.assertTrue(p.getAttached(Flight.END) == null, "state cleared");
			GameTestKit.drop(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 40)
	public void flightEndsWhenThePlayerDies(GameTestHelper c) {
		GameTestKit.enable(c);
		GameTestKit.floor(c);
		ServerPlayer p = GameTestKit.survivor(c);
		GameTestKit.place(c, p, 5.5, 1, 5.5, 0f, 0f);
		TechniqueCasts.cast(p, Technique.FLIGHT, 1, 1f);
		c.assertTrue(p.getAbilities().mayfly, "flying before death");
		p.kill(c.getLevel());
		c.runAfterDelay(3, () -> {
			c.assertTrue(p.getAttached(Flight.END) == null, "state cleared on death");
			c.assertTrue(!p.getAbilities().mayfly, "mayfly cleared on death");
			GameTestKit.drop(c, p);
			c.succeed();
		});
	}

	@GameTest(maxTicks = 40)
	public void flightLeavesCreativePlayersAlone(GameTestHelper c) {
		GameTestKit.enable(c);
		ServerPlayer p = c.makeMockServerPlayerInLevel(); // always Creative
		float speed = p.getAbilities().getFlyingSpeed();
		boolean may = p.getAbilities().mayfly;
		c.assertTrue(!TechniqueCasts.cast(p, Technique.FLIGHT, 20, 1f), "creative: cast refused");
		c.assertTrue(p.getAttached(Flight.END) == null, "no state for a creative player");
		c.assertTrue(p.getAbilities().getFlyingSpeed() == speed && p.getAbilities().mayfly == may, "abilities untouched");
		c.succeed();
	}

	// ---- Life Steal Blade ----
}
