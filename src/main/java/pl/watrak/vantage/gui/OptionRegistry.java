package pl.watrak.vantage.gui;

import pl.watrak.vantage.config.ConfigManager;
import net.minecraft.network.chat.Component;
import pl.watrak.vantage.config.VantageConfig;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * The whole settings screen, declared as data.
 *
 * <p>Every feature appears here exactly once. The screen reads this list and
 * lays itself out, so a new feature never needs layout code — only a field on
 * {@link VantageConfig} and a line below.
 */
public final class OptionRegistry {

	/** A tab in the settings screen and the options shown under it. */
	public record Category(String id, List<Option> options) {
		public String titleKey() {
			return "vantage.category." + id;
		}
	}

	private OptionRegistry() {
	}

	/**
	 * Labels for an enum setting, in declaration order.
	 *
	 * <p>{@link Option.Choice} works on an index, so the order here has to match
	 * the enum exactly — which is why the values are read from the enum itself
	 * rather than listed by hand.
	 */
	private static List<Component> names(String prefix, Enum<?>[] values) {
		return Arrays.stream(values)
				.map(value -> Component.translatable(prefix + "." + value.name().toLowerCase(Locale.ROOT)))
				.map(component -> (Component) component)
				.toList();
	}

	public static List<Category> categories() {
		VantageConfig c = ConfigManager.get();

		return List.of(
				new Category("light", List.of(
						new Option.Toggle("fullbright", () -> c.fullbright, v -> c.fullbright = v),
						new Option.Slider("gamma_multiplier", 1, 16,
								() -> c.gammaMultiplier, v -> c.gammaMultiplier = v),
						new Option.Toggle("disable_lava_fog",
								() -> c.disableLavaFog, v -> c.disableLavaFog = v),
						new Option.Toggle("disable_powder_snow_fog",
								() -> c.disablePowderSnowFog, v -> c.disablePowderSnowFog = v),
						new Option.Toggle("disable_blindness_fog",
								() -> c.disableBlindnessFog, v -> c.disableBlindnessFog = v),
						new Option.Toggle("disable_darkness_fog",
								() -> c.disableDarknessFog, v -> c.disableDarknessFog = v),
						new Option.Toggle("disable_water_fog",
								() -> c.disableWaterFog, v -> c.disableWaterFog = v),
						new Option.Toggle("disable_sky_fog",
								() -> c.disableSkyFog, v -> c.disableSkyFog = v),
						new Option.Toggle("disable_terrain_fog",
								() -> c.disableTerrainFog, v -> c.disableTerrainFog = v),
						new Option.Toggle("disable_thick_fog",
								() -> c.disableThickFog, v -> c.disableThickFog = v)
				)),

				new Category("view", List.of(
						new Option.Toggle("disable_pumpkin_blur",
								() -> c.disablePumpkinBlur, v -> c.disablePumpkinBlur = v),
						new Option.Slider("fire_offset", -100, 100,
								() -> c.fireOffset, v -> c.fireOffset = v, OptionRegistry::percent),
						new Option.Slider("shield_offset", -100, 100,
								() -> c.shieldOffset, v -> c.shieldOffset = v, OptionRegistry::percent),
						new Option.Toggle("show_map_in_boat",
								() -> c.showMapInBoat, v -> c.showMapInBoat = v),
						new Option.Toggle("visual_barriers",
								() -> c.visualBarriers, v -> c.visualBarriers = v),
						// One switch for the whole feature; which items it applies to
						// and by how much lives behind the settings icon.
						new Option.Tunable("item_scales",
								() -> c.itemScaleFirstPerson || c.itemScaleDropped,
								v -> {
									c.itemScaleFirstPerson = v;
									c.itemScaleDropped = v;
								},
								ItemScaleOptionsScreen::new)
				)),

				new Category("hud", List.of(
						new Option.Tunable("armor_hud",
								() -> c.armorHud, v -> c.armorHud = v, ArmorHudScreen::new),
						new Option.Toggle("food_overlay",
								() -> c.foodOverlay, v -> c.foodOverlay = v),
						new Option.Toggle("tab_heads", () -> c.tabHeads, v -> c.tabHeads = v),
						new Option.Toggle("locator_heads", () -> c.locatorHeads, v -> c.locatorHeads = v),
						new Option.Toggle("ping_number", () -> c.pingNumber, v -> c.pingNumber = v),
						new Option.Tunable("status_effect_timer",
								() -> c.statusEffectTimer, v -> c.statusEffectTimer = v,
								StatusEffectScreen::new)
				)),

				new Category("qol", List.of(
						new Option.Toggle("dark_window_title_bar",
								() -> c.darkWindowTitleBar, v -> c.darkWindowTitleBar = v),
						new Option.Toggle("no_sign_gui", () -> c.noSignGui, v -> c.noSignGui = v),
						new Option.Toggle("shulker_tooltip",
								() -> c.shulkerTooltip, v -> c.shulkerTooltip = v),
						new Option.Toggle("no_explosion_particles",
								() -> c.noExplosionParticles, v -> c.noExplosionParticles = v),
						new Option.Toggle("no_resource_pack_warning",
								() -> c.noResourcePackWarning, v -> c.noResourcePackWarning = v),
						new Option.Toggle("death_coordinates",
								() -> c.deathCoordinates, v -> c.deathCoordinates = v),
						new Option.Tunable("sound_volumes",
								() -> c.soundVolumes, v -> c.soundVolumes = v, SoundVolumeScreen::new)
				)),

				new Category("pvp", List.of(
						new Option.Tunable("combat_hitboxes",
								() -> c.combatHitboxes, v -> c.combatHitboxes = v,
								CombatHitboxScreen::new),
						new Option.Link("feature_keys", FeatureKeybindScreen::new),
						new Option.Toggle("shield_status", () -> c.shieldStatus, v -> c.shieldStatus = v),
						new Option.Toggle("hotbar_keybinds",
								() -> c.hotbarKeybinds, v -> c.hotbarKeybinds = v),
						new Option.Toggle("auto_join_party",
								() -> c.autoJoinParty, v -> c.autoJoinParty = v)
				))
		);
	}

	/** Everything behind the item scaling settings icon. */
	public static List<Option> itemScaleOptions() {
		VantageConfig c = ConfigManager.get();

		return List.of(
				new Option.Toggle("item_scale_first_person",
						() -> c.itemScaleFirstPerson, v -> c.itemScaleFirstPerson = v),
				new Option.Toggle("item_scale_dropped",
						() -> c.itemScaleDropped, v -> c.itemScaleDropped = v),
				new Option.Link("item_scales_list", ItemScaleScreen::new)
		);
	}

	/** Everything behind the "Armor HUD options" button. */
	public static List<Option> armorHudOptions() {
		VantageConfig c = ConfigManager.get();

		return List.of(
				new Option.Choice("armor_hud_anchor",
						names("vantage.armor.anchor", VantageConfig.ArmorAnchor.values()),
						() -> c.armorHudAnchor.ordinal(),
						v -> c.armorHudAnchor = VantageConfig.ArmorAnchor.values()[v]),
				new Option.Choice("armor_hud_side",
						names("vantage.armor.side", VantageConfig.ArmorSide.values()),
						() -> c.armorHudSide.ordinal(),
						v -> c.armorHudSide = VantageConfig.ArmorSide.values()[v]),
				new Option.Choice("armor_hud_style",
						names("vantage.armor.style", VantageConfig.ArmorStyle.values()),
						() -> c.armorHudStyle.ordinal(),
						v -> c.armorHudStyle = VantageConfig.ArmorStyle.values()[v]),
				new Option.Choice("armor_hud_orientation",
						names("vantage.armor.orientation", VantageConfig.ArmorOrientation.values()),
						() -> c.armorHudOrientation.ordinal(),
						v -> c.armorHudOrientation = VantageConfig.ArmorOrientation.values()[v]),
				new Option.Choice("armor_hud_durability",
						names("vantage.armor.durability", VantageConfig.ArmorDurability.values()),
						() -> c.armorHudDurability.ordinal(),
						v -> c.armorHudDurability = VantageConfig.ArmorDurability.values()[v]),
				new Option.Choice("armor_hud_visibility",
						names("vantage.armor.visibility", VantageConfig.ArmorVisibility.values()),
						() -> c.armorHudVisibility.ordinal(),
						v -> c.armorHudVisibility = VantageConfig.ArmorVisibility.values()[v]),
				new Option.Choice("armor_hud_offhand",
						names("vantage.armor.offhand", VantageConfig.ArmorOffhand.values()),
						() -> c.armorHudOffhand.ordinal(),
						v -> c.armorHudOffhand = VantageConfig.ArmorOffhand.values()[v]),
				new Option.Toggle("armor_hud_warning",
						() -> c.armorHudWarning, v -> c.armorHudWarning = v),
				new Option.Slider("armor_hud_warning_percent",
						Option.defaultLabel("armor_hud_warning_percent"), 0, 100, 5,
						() -> c.armorHudWarningPercent, v -> c.armorHudWarningPercent = v,
						OptionRegistry::percent),
				new Option.Slider("armor_hud_x", -200, 200,
						() -> c.armorHudOffsetX, v -> c.armorHudOffsetX = v),
				new Option.Slider("armor_hud_y", -200, 200,
						() -> c.armorHudOffsetY, v -> c.armorHudOffsetY = v),
				new Option.Slider("armor_hud_scale", Option.defaultLabel("armor_hud_scale"),
						25, 400, 5,
						() -> c.armorHudScale, v -> c.armorHudScale = v, OptionRegistry::percent)
		);
	}

	/** Everything behind the "Effect display options" button. */
	public static List<Option> statusEffectOptions() {
		VantageConfig c = ConfigManager.get();

		return List.of(
				new Option.Choice("status_effect_style",
						names("vantage.effect.style", VantageConfig.EffectStyle.values()),
						() -> c.statusEffectStyle.ordinal(),
						v -> c.statusEffectStyle = VantageConfig.EffectStyle.values()[v]),
				new Option.Choice("status_effect_anchor",
						names("vantage.effect.anchor", VantageConfig.EffectAnchor.values()),
						() -> c.statusEffectAnchor.ordinal(),
						v -> c.statusEffectAnchor = VantageConfig.EffectAnchor.values()[v]),
				new Option.Slider("status_effect_x", 0, 200,
						() -> c.statusEffectOffsetX, v -> c.statusEffectOffsetX = v),
				new Option.Slider("status_effect_y", 0, 200,
						() -> c.statusEffectOffsetY, v -> c.statusEffectOffsetY = v),
				new Option.Slider("status_effect_scale", 50, 200,
						c::effectScale, c::setEffectScale),
				new Option.Slider("status_effect_timer_size", 50, 200,
						c::effectTimerSize, c::setEffectTimerSize),
				new Option.Choice("status_effect_colour",
						names("vantage.effect.colour", VantageConfig.EffectTextColour.values()),
						() -> c.statusEffectColour.ordinal(),
						v -> c.statusEffectColour = VantageConfig.EffectTextColour.values()[v])
		);
	}

	private static String percent(int value) {
		return value + "%";
	}
}
