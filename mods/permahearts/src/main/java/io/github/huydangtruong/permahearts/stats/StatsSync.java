package io.github.huydangtruong.permahearts.stats;

import io.github.huydangtruong.permahearts.ElixirStat;
import io.github.huydangtruong.permahearts.HeartsAttachment;
import io.github.huydangtruong.permahearts.PermaHearts;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.storage.LevelData;

/** Server side of the stats panel (D31): computes PlayerStats every second and syncs it to the player when it changed. */
public final class StatsSync {
	private static final StreamCodec<ByteBuf, PlayerStats.Row> ROW = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8, PlayerStats.Row::id,
		ByteBufCodecs.DOUBLE, PlayerStats.Row::total,
		ByteBufCodecs.DOUBLE, PlayerStats.Row::base,
		ByteBufCodecs.DOUBLE, PlayerStats.Row::gear,
		ByteBufCodecs.DOUBLE, PlayerStats.Row::bonus,
		PlayerStats.Row::new);

	private static final StreamCodec<ByteBuf, PlayerStats.Spawn> SPAWN = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, PlayerStats.Spawn::x,
		ByteBufCodecs.VAR_INT, PlayerStats.Spawn::y,
		ByteBufCodecs.VAR_INT, PlayerStats.Spawn::z,
		ByteBufCodecs.STRING_UTF8, PlayerStats.Spawn::dimension,
		ByteBufCodecs.BOOL, PlayerStats.Spawn::personal,
		PlayerStats.Spawn::new);

	public static final StreamCodec<ByteBuf, PlayerStats> CODEC = StreamCodec.composite(
		ROW.apply(ByteBufCodecs.list()), PlayerStats::rows,
		ByteBufCodecs.VAR_INT, PlayerStats::vitality,
		SPAWN, PlayerStats::spawn,
		PlayerStats::new);

	/** Not saved: rebuilt every second. Synced to its own player only. */
	public static final AttachmentType<PlayerStats> STATS = AttachmentRegistry.create(
		Identifier.fromNamespaceAndPath(PermaHearts.MOD_ID, "stats"),
		b -> b.syncWith(CODEC, AttachmentSyncPredicate.targetOnly()));

	private record Line(String id, Holder<Attribute> attribute) {}

	private static final List<Line> LINES = List.of(
		new Line("hearts", Attributes.MAX_HEALTH),
		new Line("damage", Attributes.ATTACK_DAMAGE),
		new Line("attack_speed", Attributes.ATTACK_SPEED),
		new Line("speed", Attributes.MOVEMENT_SPEED),
		new Line("armor", Attributes.ARMOR),
		new Line("reach", Attributes.BLOCK_INTERACTION_RANGE),
		new Line("hit_range", Attributes.ENTITY_INTERACTION_RANGE),
		new Line("mining", Attributes.BLOCK_BREAK_SPEED));

	private StatsSync() {}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 == 0) {
				for (ServerPlayer p : server.getPlayerList().getPlayers()) {
					update(p);
				}
			}
		});
	}

	public static void update(ServerPlayer p) {
		PlayerStats now = compute(p);
		if (!now.equals(p.getAttached(STATS))) {
			p.setAttached(STATS, now);
		}
	}

	public static PlayerStats compute(ServerPlayer p) {
		List<PlayerStats.Row> rows = new ArrayList<>();
		for (Line l : LINES) {
			AttributeInstance inst = p.getAttribute(l.attribute());
			if (inst == null) {
				continue;
			}
			List<StatMath.Mod> mods = new ArrayList<>();
			for (AttributeModifier m : inst.getModifiers()) {
				mods.add(new StatMath.Mod(m.amount(), m.operation().ordinal(), m.id().getNamespace().equals(PermaHearts.MOD_ID)));
			}
			rows.add(StatMath.split(l.id(), inst.getBaseValue(), mods));
		}
		int vitality = PermaHearts.enabled(p.level().getServer())
			? HeartsAttachment.getCultivation(p).level(ElixirStat.VITALITY) : 0;
		return new PlayerStats(List.copyOf(rows), vitality, spawn(p));
	}

	private static PlayerStats.Spawn spawn(ServerPlayer p) {
		ServerPlayer.RespawnConfig own = p.getRespawnConfig();
		boolean personal = own != null;
		LevelData.RespawnData d = personal ? own.respawnData() : p.level().getServer().getRespawnData();
		return new PlayerStats.Spawn(d.pos().getX(), d.pos().getY(), d.pos().getZ(), d.dimension().identifier().toString(), personal);
	}
}
