package pl.watrak.vantage.gui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Where the status effect block sits, split out the way the armour widget is. */
public final class StatusEffectScreen extends OptionListScreen {

	public StatusEffectScreen(Screen parent) {
		super(Component.translatable("vantage.screen.status_effects"), parent);
	}

	@Override
	protected List<Option> options() {
		return OptionRegistry.statusEffectOptions();
	}
}
