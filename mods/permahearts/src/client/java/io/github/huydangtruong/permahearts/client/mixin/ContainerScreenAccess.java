package io.github.huydangtruong.permahearts.client.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The stats panel moves the inventory to the right the way the recipe book does (D31). */
@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccess {
	@Accessor("leftPos")
	int permahearts$leftPos();

	@Accessor("leftPos")
	void permahearts$setLeftPos(int leftPos);

	@Accessor("topPos")
	int permahearts$topPos();

	@Accessor("imageWidth")
	int permahearts$imageWidth();
}
