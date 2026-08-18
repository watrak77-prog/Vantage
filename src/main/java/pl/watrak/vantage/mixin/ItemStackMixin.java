package pl.watrak.vantage.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pl.watrak.vantage.config.ConfigManager;
import pl.watrak.vantage.feature.FoodTooltipData;
import pl.watrak.vantage.feature.ShulkerPreview;
import pl.watrak.vantage.feature.ShulkerTooltipData;

import java.util.List;
import java.util.Optional;

/** Attaches the food icon block to edible items' tooltips. */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

	/**
	 * An item may only carry one tooltip image, so anything that already has
	 * one — a bundle showing its contents, for instance — keeps it. Food never
	 * uses the slot in vanilla, which is why it is free to take.
	 */
	@ModifyReturnValue(method = "getTooltipImage", at = @At("RETURN"))
	private Optional<TooltipComponent> vantage$addTooltipImage(Optional<TooltipComponent> original) {
		if (original.isPresent()) {
			return original;
		}

		ItemStack stack = (ItemStack) (Object) this;

		// The grid replaces the text list rather than joining it, so holding the
		// key swaps one view for the other instead of stacking both.
		if (ConfigManager.get().shulkerTooltip && ShulkerPreview.isPreviewHeld()) {
			List<ItemStack> items = ShulkerPreview.contentsOf(stack);
			if (!items.isEmpty()) {
				return Optional.of(new ShulkerTooltipData(items));
			}
		}

		if (ConfigManager.get().foodOverlay) {
			FoodProperties food = stack.get(DataComponents.FOOD);
			if (food != null) {
				return Optional.of(new FoodTooltipData(food));
			}
		}

		return original;
	}
}
