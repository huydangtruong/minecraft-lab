package io.github.huydangtruong.permahearts.technique;

import io.github.huydangtruong.permahearts.PermaHearts;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * The selected skill bar entry (-1 = the empty cross, 0..3 = a slot). Not saved (a new session starts on the cross),
 * synced to every player so others see the focus item in the hand (D46).
 */
public final class TechniqueSelection {
	/** slot -1 and an empty technique id mean the cross. The id lets other clients draw the focus item without knowing our slots. */
	public record Selection(int slot, String technique) {
		public static final Selection NONE = new Selection(-1, "");
	}

	public static final AttachmentType<Selection> SELECTED = AttachmentRegistry.create(
		Identifier.fromNamespaceAndPath(PermaHearts.MOD_ID, "technique_selection"),
		b -> b.initializer(() -> Selection.NONE).syncWith(StreamCodec.composite(
			ByteBufCodecs.VAR_INT, Selection::slot,
			ByteBufCodecs.STRING_UTF8, Selection::technique,
			Selection::new), AttachmentSyncPredicate.all()));

	private TechniqueSelection() {}

	public static void init() {
		SELECTED.identifier();
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 5 != 0) {
				return;
			}
			for (ServerPlayer p : server.getPlayerList().getPlayers()) {
				if (get(p) >= 0) {
					Technique t = selectedTechnique(p);
					if (t == null) {
						set(p, -1);
					} else if (!t.id().equals(p.getAttachedOrCreate(SELECTED).technique())) {
						set(p, get(p)); // another technique was put in the slot: keep the slot, update the id
					}
				}
			}
		});
	}

	public static int get(ServerPlayer p) {
		return p.getAttachedOrCreate(SELECTED).slot();
	}

	public static void set(ServerPlayer p, int index) {
		Technique t = index < 0 ? null : TechniqueAttachment.get(p).inSlot(index);
		Selection next = t == null ? Selection.NONE : new Selection(index, t.id());
		if (!next.equals(p.getAttachedOrCreate(SELECTED))) {
			p.setAttached(SELECTED, next);
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
