package pl.watrak.vantage.feature;

import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import pl.watrak.vantage.gui.FoodTooltipComponent;
import pl.watrak.vantage.gui.ShulkerTooltipComponent;

/** Wires the food tooltip data to the component that draws it. */
public final class FoodTooltip {

	private FoodTooltip() {
	}

	public static void register() {
		TooltipComponentCallback.EVENT.register(data -> {
			if (data instanceof FoodTooltipData food) {
				return new FoodTooltipComponent(food.food());
			}
			if (data instanceof ShulkerTooltipData shulker) {
				return new ShulkerTooltipComponent(shulker.items());
			}
			return null;
		});
	}
}
