package pl.watrak.vantage.feature;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

/**
 * Marker carrying a food item's values from the tooltip data side to the client
 * renderer that draws them as icons.
 */
public record FoodTooltipData(FoodProperties food) implements TooltipComponent {
}
