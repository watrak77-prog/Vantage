package pl.watrak.vantage.gui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Where item scaling applies, and by how much.
 *
 * <p>Held items and dropped items were two separate switches on the View tab,
 * which read as two features when they are one: the same per-item sizes, shown
 * in two places. They sit together here behind a single switch instead.
 */
public final class ItemScaleOptionsScreen extends OptionListScreen {

	public ItemScaleOptionsScreen(Screen parent) {
		super(Component.translatable("vantage.screen.item_scale_options"), parent);
	}

	@Override
	protected List<Option> options() {
		return OptionRegistry.itemScaleOptions();
	}
}
