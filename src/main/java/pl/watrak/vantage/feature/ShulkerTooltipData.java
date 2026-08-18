package pl.watrak.vantage.feature;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Contents of a shulker box, on their way to the tooltip renderer. */
public record ShulkerTooltipData(List<ItemStack> items) implements TooltipComponent {
}
