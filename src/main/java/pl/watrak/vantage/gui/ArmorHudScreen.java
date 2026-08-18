package pl.watrak.vantage.gui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Placement and styling for the armour widget.
 *
 * <p>Split out of the HUD tab because these nine settings only matter once the
 * widget is on, and crowding them in beside every other HUD toggle made the tab
 * hard to scan.
 */
public final class ArmorHudScreen extends OptionListScreen {

	public ArmorHudScreen(Screen parent) {
		super(Component.translatable("vantage.screen.armor_hud"), parent);
	}

	@Override
	protected List<Option> options() {
		return OptionRegistry.armorHudOptions();
	}
}
