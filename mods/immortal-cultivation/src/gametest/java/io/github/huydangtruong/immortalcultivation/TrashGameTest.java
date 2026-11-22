package io.github.huydangtruong.immortalcultivation;

import io.github.huydangtruong.immortalcultivation.trash.TrashNet;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** D217: the trash slot handler destroys exactly the carried stack, on the server. (The mock player cannot be made a spectator; that rule is in TrashRulesTest.) */
public class TrashGameTest {
	@GameTest
	public void carriedStackIsDestroyed(GameTestHelper c) {
		ServerPlayer p = c.makeMockServerPlayerInLevel();
		p.getInventory().clearContent();
		p.getInventory().setItem(0, new ItemStack(Items.IRON_INGOT, 3));
		p.containerMenu.setCarried(new ItemStack(Items.DIAMOND, 7));
		int items = c.getLevel().getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(16)).size();
		if (!TrashNet.apply(p)) {
			c.fail("apply returned false with a stack on the cursor");
			return;
		}
		if (!p.containerMenu.getCarried().isEmpty()) {
			c.fail("the cursor still holds " + p.containerMenu.getCarried());
			return;
		}
		if (p.getInventory().countItem(Items.DIAMOND) != 0 || p.getInventory().countItem(Items.IRON_INGOT) != 3) {
			c.fail("inventory changed wrongly");
			return;
		}
		if (c.getLevel().getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(16)).size() != items) {
			c.fail("the stack was dropped as an item entity");
			return;
		}
		if (TrashNet.apply(p)) {
			c.fail("an empty cursor must do nothing");
			return;
		}
		c.succeed();
	}
}
