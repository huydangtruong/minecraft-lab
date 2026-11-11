package io.github.huydangtruong.permahearts.wanderer;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** World-wide Wanderer state, stored in the overworld data folder. */
public class WandererSavedData extends SavedData {
	private static final Codec<WandererSavedData> CODEC = WandererData.CODEC.xmap(WandererSavedData::new, WandererSavedData::get);

	public static final SavedDataType<WandererSavedData> TYPE = new SavedDataType<>(
		Identifier.fromNamespaceAndPath("permahearts", "wanderer"), WandererSavedData::new, CODEC, null);

	private WandererData data;

	public WandererSavedData() {
		this(WandererData.DEFAULT);
	}

	private WandererSavedData(WandererData data) {
		this.data = data;
	}

	public WandererData get() {
		return data;
	}

	public void set(WandererData d) {
		this.data = d;
		setDirty();
	}

	public static WandererSavedData of(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}
}
