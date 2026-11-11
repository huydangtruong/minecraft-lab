package io.github.huydangtruong.permahearts.wanderer;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class WandererMenus {
	public static final ExtendedMenuType<WandererMenu, QuestSnapshot> TYPE = Registry.register(BuiltInRegistries.MENU,
		Identifier.fromNamespaceAndPath("permahearts", "wanderer"),
		new ExtendedMenuType<>(WandererMenu::new, QuestSnapshot.STREAM_CODEC));

	private WandererMenus() {}

	public static void init() {
		TYPE.getStreamCodec(); // touch the class so the menu type is registered
	}

	/** Opens the quest screen for this player (server side). */
	public static void open(ServerPlayer player, Entity npc) {
		MinecraftServer server = player.level().getServer();
		Quest quest = Wanderer.quest(server).orElse(null);
		if (quest == null) {
			return;
		}
		QuestSnapshot snap = QuestSnapshot.of(quest);
		player.openMenu(new ExtendedMenuProvider<QuestSnapshot>() {
			@Override
			public QuestSnapshot getScreenOpeningData(ServerPlayer p) {
				return snap;
			}

			@Override
			public Component getDisplayName() {
				return io.github.huydangtruong.permahearts.Messages.wandererName();
			}

			@Override
			public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
				return new WandererMenu(id, inv, snap, server, npc);
			}
		});
	}
}
