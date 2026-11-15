package io.github.huydangtruong.permahearts.reward;

import io.github.huydangtruong.permahearts.PermaHearts;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

/** Registry and server logic of the reward boxes (D76): items, the data component, the menu, rolling and claiming. */
public final class RewardBoxes {
	private static final java.util.Random RNG = new java.util.Random();

	/** The rolled cards of a box; absent until the first open. */
	public static final DataComponentType<RewardBoxData> DATA = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
		Identifier.fromNamespaceAndPath(PermaHearts.MOD_ID, "reward_box"),
		DataComponentType.<RewardBoxData>builder().persistent(RewardBoxData.CODEC)
			.networkSynchronized(RewardBoxData.STREAM_CODEC).build());

	private static final Map<RewardTier, Item> ITEMS = new EnumMap<>(RewardTier.class);

	static {
		for (RewardTier tier : RewardTier.values()) {
			ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(PermaHearts.MOD_ID, tier.itemName()));
			Rarity rarity = switch (tier) {
				case SILVER -> Rarity.COMMON;
				case GOLD -> Rarity.UNCOMMON;
				case DIAMOND -> Rarity.RARE;
			};
			ITEMS.put(tier, Registry.register(BuiltInRegistries.ITEM, key,
				new RewardBoxItem(new Item.Properties().setId(key).stacksTo(1).rarity(rarity), tier)));
		}
	}

	/** What the client needs to build the screen. */
	public record OpenData(RewardTier tier, boolean offHand, List<RewardOption> options) {
		public static final net.minecraft.network.codec.StreamCodec<io.netty.buffer.ByteBuf, OpenData> STREAM_CODEC =
			net.minecraft.network.codec.StreamCodec.composite(
				net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8.map(RewardTier::valueOf, RewardTier::name), OpenData::tier,
				net.minecraft.network.codec.ByteBufCodecs.BOOL, OpenData::offHand,
				RewardBoxData.OPTIONS_STREAM_CODEC, OpenData::options,
				OpenData::new);
	}

	public static final ExtendedMenuType<RewardBoxMenu, OpenData> MENU = Registry.register(BuiltInRegistries.MENU,
		Identifier.fromNamespaceAndPath(PermaHearts.MOD_ID, "reward_box"),
		new ExtendedMenuType<>(RewardBoxMenu::new, OpenData.STREAM_CODEC));

	private RewardBoxes() {}

	/** Touches the class so everything is registered, and adds the boxes to the creative Food and Drinks tab (next to the elixirs). */
	public static void init() {
		ResourceKey<CreativeModeTab> tab = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("food_and_drinks"));
		CreativeModeTabEvents.modifyOutputEvent(tab).register(out -> {
			for (RewardTier t : RewardTier.values()) {
				out.accept(item(t));
			}
		});
	}

	public static Item item(RewardTier tier) {
		return ITEMS.get(tier);
	}

	/** A fresh, unrolled box. */
	public static ItemStack stack(RewardTier tier) {
		return new ItemStack(item(tier));
	}

	public static Component name(RewardTier tier) {
		return Component.translatable(item(tier).getDescriptionId());
	}

	public static Optional<RewardTier> tierOf(ItemStack stack) {
		return stack.getItem() instanceof RewardBoxItem b ? Optional.of(b.tier()) : Optional.empty();
	}

	/** The cards of the box, rolling and storing them first if it has none (the first open). Empty if the stack is no box. */
	public static List<RewardOption> ensureRolled(ItemStack stack) {
		Optional<RewardTier> tier = tierOf(stack);
		if (tier.isEmpty()) {
			return List.of();
		}
		RewardBoxData data = stack.get(DATA);
		if (data == null) {
			data = new RewardBoxData(RewardRoll.roll(tier.get(), RNG));
			stack.set(DATA, data);
		}
		return data.options();
	}

	/** Rolls the box in that hand if needed and opens the choose screen. */
	public static void open(ServerPlayer player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		List<RewardOption> options = ensureRolled(stack);
		if (options.isEmpty()) {
			return;
		}
		OpenData data = new OpenData(tierOf(stack).orElseThrow(), hand == InteractionHand.OFF_HAND, options);
		player.openMenu(new ExtendedMenuProvider<OpenData>() {
			@Override
			public OpenData getScreenOpeningData(ServerPlayer p) {
				return data;
			}

			@Override
			public Component getDisplayName() {
				return name(data.tier());
			}

			@Override
			public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
				return new RewardBoxMenu(id, inv, data);
			}
		});
	}

	/**
	 * Gives the selected cards of the box in that hand and consumes the box. The selection is checked against the
	 * stored roll; nothing the client sent but the mask is used. Returns false (box kept) if the hand holds no rolled
	 * box or the mask is not exactly picks cards inside the roll.
	 */
	public static boolean claim(ServerPlayer player, InteractionHand hand, int mask) {
		ItemStack box = player.getItemInHand(hand);
		Optional<RewardTier> tier = tierOf(box);
		RewardBoxData data = box.get(DATA);
		if (tier.isEmpty() || data == null || !RewardSelection.valid(mask, tier.get().picks(), data.options().size())) {
			return false;
		}
		List<ItemStack> given = new ArrayList<>();
		for (int i = 0; i < data.options().size(); i++) {
			if (RewardSelection.isSelected(mask, i)) {
				RewardOption o = data.options().get(i);
				Optional<Item> item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(o.itemId()));
				if (item.isEmpty() || o.count() <= 0) {
					return false;
				}
				given.add(new ItemStack(item.get(), o.count()));
			}
		}
		box.shrink(1);
		if (box.isEmpty()) {
			player.setItemInHand(hand, ItemStack.EMPTY); // frees the slot, so the reward cannot land in a half-dead stack
		}
		ServerLevel level = player.level();
		for (ItemStack whole : given) {
			while (!whole.isEmpty()) {
				ItemStack s = whole.split(Math.min(whole.getCount(), whole.getMaxStackSize())); // one stack at a time; the freed box slot always takes one, anything else drops at the feet
				player.getInventory().add(s);
				if (!s.isEmpty()) {
					player.spawnAtLocation(level, s);
				}
			}
		}
		level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 1f);
		player.sendSystemMessage(RewardMessages.claimed());
		return true;
	}
}
