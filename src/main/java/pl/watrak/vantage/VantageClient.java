package pl.watrak.vantage;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.watrak.vantage.compat.ClientCompat;
import pl.watrak.vantage.config.ConfigManager;
import pl.watrak.vantage.feature.ArmorWarning;
import pl.watrak.vantage.feature.EffectDurationTracker;
import pl.watrak.vantage.feature.FeatureKeybinds;
import pl.watrak.vantage.feature.AutoJoinParty;
import pl.watrak.vantage.feature.FoodTooltip;
import pl.watrak.vantage.feature.ShieldFixes;
import pl.watrak.vantage.feature.ShulkerPreview;
import pl.watrak.vantage.feature.ShieldStatusTracker;
import pl.watrak.vantage.feature.WindowTheme;
import pl.watrak.vantage.gui.VantageScreen;
import pl.watrak.vantage.hud.ArmorHudElement;
import pl.watrak.vantage.hud.HotbarKeybindsElement;

public final class VantageClient implements ClientModInitializer {

	public static final String MOD_ID = "vantage";
	public static final Logger LOGGER = LoggerFactory.getLogger("Vantage");

	/**
	 * The group this mod's keys are listed under in the controls screen.
	 *
	 * <p>1.21.9 turned categories into registered objects; before that a category
	 * was just the translation key naming it. Both spellings resolve to the same
	 * key, so the language file needs only the one entry.
	 */
	//? if >=1.21.9 {
	public static final KeyMapping.Category CATEGORY =
			KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
	//?} else {
	/*public static final String CATEGORY = "key.category.vantage.main";
	*///?}

	private static KeyMapping openSettings;

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitializeClient() {
		ConfigManager.load();

		openSettings = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.vantage.open_settings",
				InputConstants.Type.KEYSYM,
				InputConstants.KEY_RSHIFT,
				CATEGORY
		));

		registerHud();
		registerShieldTracking();
		FoodTooltip.register();
		ShulkerPreview.register();
		AutoJoinParty.register();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			// consumeClick drains the queued presses, so holding the key does
			// not reopen the screen every tick.
			while (openSettings.consumeClick()) {
				if (ClientCompat.currentScreen(client) == null) {
					client.setScreen(new VantageScreen(null));
				}
			}

			FeatureKeybinds.tick(client);

			if (client.level != null) {
				ShieldStatusTracker.tick(client.level);
				ShieldFixes.tick(client.level);
			}
			if (client.player != null) {
				ArmorWarning.tick(client.player);
			}

			// Ticked rather than applied once, so toggling it in the settings
			// takes effect without a restart. It no-ops unless the value changed.
			WindowTheme.sync();
		});

		MixinSelfCheck.runIfDevelopment();
		LOGGER.info("Vantage ready.");
	}

	/** Both elements draw over the hotbar, so they attach just after it. */
	private void registerHud() {
		HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, id("armor_hud"), new ArmorHudElement());
		HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, id("hotbar_keybinds"), new HotbarKeybindsElement());
	}

	private void registerShieldTracking() {
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			ShieldStatusTracker.onAttack(player, entity);
			return InteractionResult.PASS;
		});

		// Cooldowns and blocking history belong to one server; carrying them
		// across a reconnect would mark the wrong people.
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ShieldStatusTracker.clear());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ShieldFixes.clear());
		// Same reasoning for the effect bars: a fresh world should not measure
		// its effects against durations seen on the last one.
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> EffectDurationTracker.clear());
	}
}
