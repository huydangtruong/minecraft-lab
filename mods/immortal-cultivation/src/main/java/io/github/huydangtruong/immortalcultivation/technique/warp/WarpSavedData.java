package io.github.huydangtruong.immortalcultivation.technique.warp;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Every portal of the world (D75), kept in the overworld data folder so it survives owners being offline. */
public class WarpSavedData extends SavedData {
	private static final Codec<WarpSavedData> CODEC = PortalRecord.All.CODEC.xmap(WarpSavedData::new, WarpSavedData::get);

	public static final SavedDataType<WarpSavedData> TYPE = new SavedDataType<>(
		Identifier.fromNamespaceAndPath("permahearts", "warp_portals"), WarpSavedData::new, CODEC, null);

	private PortalRecord.All data;

	public WarpSavedData() {
		this(PortalRecord.All.EMPTY);
	}

	private WarpSavedData(PortalRecord.All data) {
		this.data = data;
	}

	public PortalRecord.All get() {
		return data;
	}

	public void set(PortalRecord.All d) {
		this.data = d;
		setDirty();
	}

	public static WarpSavedData of(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}
}
