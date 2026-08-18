package pl.watrak.vantage.feature;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import pl.watrak.vantage.compat.ClientCompat;
import pl.watrak.vantage.config.ConfigManager;
import pl.watrak.vantage.gui.Option;
import pl.watrak.vantage.gui.OptionRegistry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lets any on/off setting be flipped by a key during play.
 *
 * <p>The list of what can be bound is read back out of the settings registry
 * rather than kept alongside it. A second list would drift the moment a feature
 * was added, and this way a new toggle is bindable the day it exists.
 *
 * <p>Keys are polled once a tick instead of hooking the keyboard. Twenty checks
 * a second is plenty for a toggle — no tap is shorter than that — and it keeps
 * the feature out of the input path entirely.
 */
public final class FeatureKeybinds {

	/** Was-down state per key, so holding a key toggles once rather than every tick. */
	private static final Map<Integer, Boolean> HELD = new LinkedHashMap<>();

	private FeatureKeybinds() {
	}

	/** Every on/off setting, including those living on the sub-screens. */
	public static List<Option.Toggle> bindable() {
		List<Option> all = new ArrayList<>();
		OptionRegistry.categories().forEach(category -> all.addAll(category.options()));
		all.addAll(OptionRegistry.armorHudOptions());
		all.addAll(OptionRegistry.statusEffectOptions());

		Map<String, Option.Toggle> unique = new LinkedHashMap<>();
		for (Option option : all) {
			if (option instanceof Option.Toggle toggle) {
				unique.putIfAbsent(toggle.id(), toggle);
			}
		}
		return List.copyOf(unique.values());
	}

	/** The key bound to a setting, or -1 when it has none. */
	public static int keyFor(String id) {
		return ConfigManager.get().featureKeys.getOrDefault(id, -1);
	}

	public static void bind(String id, int key) {
		if (key == InputConstants.UNKNOWN.getValue()) {
			ConfigManager.get().featureKeys.remove(id);
		} else {
			ConfigManager.get().featureKeys.put(id, key);
		}
		ConfigManager.save();
	}

	/** The key's name as the controls screen would write it. */
	public static Component keyName(int key) {
		if (key < 0) {
			return Component.translatable("vantage.keybind.unbound");
		}
		return InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName();
	}

	/**
	 * Flips anything whose key went down since the last tick.
	 *
	 * <p>Ignored while a screen is open, so binding a key does not immediately
	 * fire it and typing in chat never toggles a feature by accident.
	 */
	public static void tick(Minecraft minecraft) {
		Map<String, Integer> bindings = ConfigManager.get().featureKeys;
		if (bindings.isEmpty()) {
			HELD.clear();
			return;
		}

		if (minecraft.getWindow() == null || ClientCompat.currentScreen(minecraft) != null) {
			HELD.clear();
			return;
		}

		List<Option.Toggle> toggles = bindable();
		boolean changed = false;

		for (Map.Entry<String, Integer> binding : bindings.entrySet()) {
			int key = binding.getValue();
			boolean down = InputConstants.isKeyDown(minecraft.getWindow(), key);
			boolean wasDown = HELD.getOrDefault(key, false);
			HELD.put(key, down);

			if (!down || wasDown) {
				continue;
			}

			for (Option.Toggle toggle : toggles) {
				if (toggle.id().equals(binding.getKey())) {
					toggle.setter().accept(!toggle.getter().getAsBoolean());
					changed = true;
				}
			}
		}

		if (changed) {
			ConfigManager.save();
		}
	}

}
