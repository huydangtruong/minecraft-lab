package io.github.huydangtruong.permahearts.wanderer;

import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * One per quest, shared by every open menu. Slot i shows the elixir for line i while that line is done and not
 * claimed. Taking it out (any path vanilla uses) claims the line, so the other player's slot goes empty on their next sync.
 */
final class RewardContainer implements Container {
	private final MinecraftServer server;
	private final long questId;

	RewardContainer(MinecraftServer server, long questId) {
		this.server = server;
		this.questId = questId;
	}

	MinecraftServer server() {
		return server;
	}

	private Optional<QuestLine> line(int i) {
		Optional<Quest> q = Wanderer.data(server).quest();
		if (q.isEmpty() || q.get().id() != questId || i < 0 || i >= q.get().lines().size()) {
			return Optional.empty();
		}
		QuestLine l = q.get().lines().get(i);
		return l.done() && !l.claimed() ? Optional.of(l) : Optional.empty();
	}

	@Override
	public int getContainerSize() {
		return 4;
	}

	@Override
	public boolean isEmpty() {
		for (int i = 0; i < 4; i++) {
			if (line(i).isPresent()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public ItemStack getItem(int i) {
		return line(i).flatMap(l -> io.github.huydangtruong.permahearts.ElixirStat.byId(l.reward()))
			.map(s -> new ItemStack(io.github.huydangtruong.permahearts.ElixirItems.get(s)))
			.orElse(ItemStack.EMPTY);
	}

	private ItemStack take(int i) {
		ItemStack shown = getItem(i);
		if (shown.isEmpty()) {
			return ItemStack.EMPTY;
		}
		ItemStack got = Wanderer.claim(server, i, null);
		if (!got.isEmpty()) {
			Wanderer.farewellIfFinished(server);
		}
		return got;
	}

	@Override
	public ItemStack removeItem(int i, int amount) {
		return take(i);
	}

	@Override
	public ItemStack removeItemNoUpdate(int i) {
		return take(i);
	}

	@Override
	public void setItem(int i, ItemStack stack) {
		// Vanilla clears the slot after a pickup/quick-move; a non-empty stack is never placed here (mayPlace is false).
		if (stack.isEmpty()) {
			take(i);
		}
	}

	@Override
	public void setChanged() {}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	@Override
	public void clearContent() {}
}
