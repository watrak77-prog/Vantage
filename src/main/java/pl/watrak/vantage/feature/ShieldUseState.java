package pl.watrak.vantage.feature;

import net.minecraft.world.item.ItemStack;

/**
 * Lets the client fill in the item-use state the server never sends for other
 * players.
 */
public interface ShieldUseState {

	void vantage$setUseItem(ItemStack stack, int remainingTicks);
}
