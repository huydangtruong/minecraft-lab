package io.github.huydangtruong.permahearts.technique;

import io.github.huydangtruong.permahearts.PermaHearts;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * The selected skill bar entry (-1 = the empty cross, 0..3 = a slot). Not saved (a new session starts on the cross),
 * synced to every player so others see the focus item in the hand (D46).
 */
public final class TechniqueSelection {
	public static final AttachmentType<Integer> SELECTED = AttachmentRegistry.create(
		Identifier.fromNamespaceAndPath(PermaHearts.MOD_ID, "technique_selection"),
		b -> b.initializer(() -> -1).syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.all()));

	private TechniqueSelection() {}

	public static void init() {
		SELECTED.identifier();
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 5 != 0) {
				return;
			}
			for (ServerPlayer p : server.getPlayerList().getPlayers()) {
				if (get(p) >= 0 && selectedTechnique(p) == null) {
					set(p, -1);
				}
			}
		});
	}

	public static int get(ServerPlayer p) {
		return p.getAttachedOrCreate(SELECTED);
	}

	public static void set(ServerPlayer p, int index) {
		if (get(p) != index) {
			p.setAttached(SELECTED, index);
		}
	}

	/** The technique the player has selected, or null (cross, empty or locked slot, level 0, switch off, dead, spectator). */
	public static Technique selectedTechnique(ServerPlayer p) {
		int index = get(p);
		if (index < 0 || !PermaHearts.enabled(p.level().getServer()) || !p.isAlive() || p.isSpectator()) {
			return null;
		}
		TechniqueData d = TechniqueAttachment.get(p);
		if (index >= d.slotsOpen()) {
			return null;
		}
		Technique t = d.inSlot(index);
		return t != null && d.level(t) >= 1 ? t : null;
	}
}
