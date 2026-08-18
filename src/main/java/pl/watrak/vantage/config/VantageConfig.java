package pl.watrak.vantage.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Plain data holder for every setting in the mod.
 *
 * <p>Deliberately free of any Minecraft type so it can be serialised, diffed and
 * unit-tested without a client running, and so it survives the mapping changes
 * between 1.21.1 and 26.2 untouched. Field names are the JSON keys; renaming a
 * field silently resets that setting for existing users, so don't.
 */
public final class VantageConfig {

	/** Where the armour widget sits. HOTBAR tucks it against the hotbar itself. */
	public enum ArmorAnchor { HOTBAR, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

	/** Which side of the hotbar the widget attaches to. */
	public enum ArmorSide { LEFT, RIGHT }

	/** How much of a slot is drawn behind each piece. */
	public enum ArmorStyle { HOTBAR, SLOTS, NONE }

	public enum ArmorOrientation { HORIZONTAL, VERTICAL }

	/** How the remaining durability of a piece is spelled out. */
	public enum ArmorDurability { NONE, BAR, NUMERIC, PERCENTAGE }

	/** Corner the status effect block is pinned to. */
	public enum EffectAnchor { TOP_RIGHT, TOP_LEFT, BOTTOM_RIGHT, BOTTOM_LEFT }

	/** What the widget does about the offhand slot sharing its edge. */
	public enum ArmorOffhand { IGNORE, ADHERE, RESERVE }

	/** When the widget is worth showing at all. */
	public enum ArmorVisibility {
		ALWAYS, IF_ANY_PRESENT, NOT_EMPTY;

		/** Whether gaps are drawn for missing pieces or simply closed up. */
		public boolean drawsEmptySlots() {
			return this != NOT_EMPTY;
		}
	}

	// ---------------------------------------------------------------- light

	public boolean fullbright = false;
	/** Multiplies the vanilla gamma. 1 = untouched, 16 = as bright as it gets. */
	public int gammaMultiplier = 15;

	// --------------------------------------------------------------- screen

	public boolean disablePumpkinBlur = true;
	/** Vertical shift of the first-person fire overlay, in percent. */
	public int fireOffset = -30;
	/** Vertical shift of the first-person shield model, in percent. */
	public int shieldOffset = -20;
	public boolean showMapInBoat = true;

	// ------------------------------------------------------------------ fog

	public boolean disableLavaFog = true;
	public boolean disablePowderSnowFog = true;
	public boolean disableBlindnessFog = true;
	public boolean disableDarknessFog = true;
	public boolean disableWaterFog = true;
	public boolean disableSkyFog = false;
	public boolean disableTerrainFog = true;
	public boolean disableThickFog = true;

	// ---------------------------------------------------------------- world

	public boolean visualBarriers = false;

	// ----------------------------------------------------------------- item

	/** Apply the per-item scales to the item held in first person. */
	public boolean itemScaleFirstPerson = false;
	/** Apply the per-item scales to items lying on the ground. */
	public boolean itemScaleDropped = false;
	/** Item id -> render scale in percent, shared by both. Absent means 100. */
	public Map<String, Integer> itemScales = new LinkedHashMap<>();

	// ------------------------------------------------------------------ hud

	public boolean armorHud = false;
	public ArmorAnchor armorHudAnchor = ArmorAnchor.HOTBAR;
	public ArmorSide armorHudSide = ArmorSide.LEFT;
	public ArmorStyle armorHudStyle = ArmorStyle.HOTBAR;
	public ArmorOrientation armorHudOrientation = ArmorOrientation.HORIZONTAL;
	public ArmorDurability armorHudDurability = ArmorDurability.BAR;
	public ArmorVisibility armorHudVisibility = ArmorVisibility.NOT_EMPTY;
	public ArmorOffhand armorHudOffhand = ArmorOffhand.ADHERE;
	/** Nudged left by default so the widget clears the hotbar's rounded edge. */
	public int armorHudOffsetX = -7;
	public int armorHudOffsetY = -2;
	public int armorHudScale = 100;
	/** Icon, shake and sound together when a piece is about to break. */
	public boolean armorHudWarning = true;
	/** Durability percentage at or below which a piece counts as critical. */
	public int armorHudWarningPercent = 10;

	/** Reddens a disabled shield; it has no placement of its own. */
	public boolean shieldStatus = false;

	/**
	 * Saturation overlay, restore preview and food tooltips, together.
	 *
	 * <p>One switch rather than three: they are three views of the same
	 * information, and nobody wants the saturation bar without knowing what a
	 * steak would do to it.
	 */
	public boolean foodOverlay = false;

	public boolean chatHeads = false;
	public boolean tabHeads = false;
	public boolean locatorHeads = false;
	public boolean hotbarKeybinds = false;
	/** Latency as a number in the tab list instead of five bars. */
	public boolean pingNumber = false;
	/** Colour of the effect timer text. */
	public enum EffectTextColour { WHITE, GREY }

	/**
	 * How a status effect's remaining time is shown.
	 *
	 * <p>NUMBERS keeps the digits on the icon. The bar styles trade the exact
	 * figure for something readable at a glance mid-fight: SMALL adds a strip
	 * along the bottom of the existing icons, FULL replaces them with a labelled
	 * row per effect, which is far easier to read but takes real screen space.
	 */
	public enum EffectStyle { NUMBERS, BARS_SMALL, BARS_FULL }

	/** Remaining time drawn on status effect icons, and where the block sits. */
	public boolean statusEffectTimer = false;
	public EffectAnchor statusEffectAnchor = EffectAnchor.TOP_RIGHT;
	public int statusEffectOffsetX = 0;
	public int statusEffectOffsetY = 0;
	public EffectTextColour statusEffectColour = EffectTextColour.WHITE;
	public EffectStyle statusEffectStyle = EffectStyle.NUMBERS;
	/** Percent. Scales the icons, or the whole frame in the full style. */
	/**
	 * Sizes, in percent, kept per display style rather than shared.
	 *
	 * <p>The styles are different enough that one number cannot serve them all: a
	 * grid of icons and a column of labelled panels want different sizes, and
	 * "timer size" means digit height in one style and bar thickness in another.
	 * Sharing a value meant that tuning one style quietly ruined the last one,
	 * so each remembers its own and switching back restores what was set.
	 */
	public Map<String, Integer> statusEffectScales = new LinkedHashMap<>();
	public Map<String, Integer> statusEffectTimerSizes = new LinkedHashMap<>();

	/** Overall size for the style in use. */
	public int effectScale() {
		return statusEffectScales.getOrDefault(statusEffectStyle.name(), 100);
	}

	public void setEffectScale(int percent) {
		statusEffectScales.put(statusEffectStyle.name(), clamp(percent, 50, 200));
	}

	/** Digit height or bar thickness, depending on the style in use. */
	public int effectTimerSize() {
		return statusEffectTimerSizes.getOrDefault(statusEffectStyle.name(), 100);
	}

	public void setEffectTimerSize(int percent) {
		statusEffectTimerSizes.put(statusEffectStyle.name(), clamp(percent, 50, 200));
	}
	/** New chat messages slide in instead of appearing. */
	public boolean chatAnimation = false;

	// ------------------------------------------------------------------ qol

	/** Ask Windows for a dark title bar. Ignored on every other system. */
	public boolean darkWindowTitleBar = false;
	public boolean noSignGui = false;
	public boolean shulkerTooltip = true;
	/** Hide the explosion puffs from TNT and end crystals. */
	public boolean noExplosionParticles = false;
	/** Skip the "made for a different version" resource pack prompt. */
	public boolean noResourcePackWarning = false;
	/** Show where you died on the death screen. */
	public boolean deathCoordinates = false;

	/** Per-sound volume in percent. Absent means untouched. */
	public boolean soundVolumes = false;
	public Map<String, Integer> soundVolumeLevels = new LinkedHashMap<>();

	// ------------------------------------------------------------------ pvp

	/** Outline players only, red once they are inside your attack range. */
	public boolean combatHitboxes = false;
	public int hitboxColour = 0xFFFFFF;
	public int hitboxReachColour = 0xFF4040;
	/** Line width in pixels. */
	public int hitboxThickness = 2;

	public boolean autoJoinParty = false;

	/** Setting id to GLFW key, for toggling features without opening the menu. */
	public Map<String, Integer> featureKeys = new LinkedHashMap<>();

	/**
	 * Clamps every numeric field back into its legal range.
	 *
	 * <p>Called after loading, because the config file is a plain text file the
	 * user can edit by hand, and a hand-typed gamma of 9999 should not blind
	 * them permanently.
	 */
	public void sanitise() {
		if (featureKeys == null) {
			featureKeys = new LinkedHashMap<>();
		}

		gammaMultiplier = clamp(gammaMultiplier, 1, 16);
		fireOffset = clamp(fireOffset, -100, 100);
		shieldOffset = clamp(shieldOffset, -100, 100);

		if (itemScales == null) {
			itemScales = new LinkedHashMap<>();
		} else {
			itemScales.replaceAll((id, scale) -> clamp(scale == null ? 100 : scale, 25, 300));
			// 100 means "unchanged", so an entry at 100 is just clutter that
			// would otherwise sit in the file forever.
			itemScales.entrySet().removeIf(entry -> entry.getValue() == 100);
		}

		armorHudOffsetX = clamp(armorHudOffsetX, -200, 200);
		armorHudOffsetY = clamp(armorHudOffsetY, -200, 200);
		armorHudScale = clamp(armorHudScale, 25, 400);

		// Enums come back null when the file names a value that no longer
		// exists, which a hand edit or a downgrade can both produce.
		if (armorHudAnchor == null) {
			armorHudAnchor = ArmorAnchor.HOTBAR;
		}
		if (armorHudSide == null) {
			armorHudSide = ArmorSide.LEFT;
		}
		if (armorHudStyle == null) {
			armorHudStyle = ArmorStyle.HOTBAR;
		}
		if (armorHudOrientation == null) {
			armorHudOrientation = ArmorOrientation.HORIZONTAL;
		}
		if (armorHudDurability == null) {
			armorHudDurability = ArmorDurability.BAR;
		}
		if (armorHudVisibility == null) {
			armorHudVisibility = ArmorVisibility.NOT_EMPTY;
		}
		if (armorHudOffhand == null) {
			armorHudOffhand = ArmorOffhand.ADHERE;
		}

		armorHudWarningPercent = clamp(armorHudWarningPercent, 0, 100);
		statusEffectOffsetX = clamp(statusEffectOffsetX, 0, 200);
		statusEffectOffsetY = clamp(statusEffectOffsetY, 0, 200);
		if (statusEffectAnchor == null) {
			statusEffectAnchor = EffectAnchor.TOP_RIGHT;
		}
		if (statusEffectScales == null) {
			statusEffectScales = new LinkedHashMap<>();
		}
		if (statusEffectTimerSizes == null) {
			statusEffectTimerSizes = new LinkedHashMap<>();
		}
		hitboxThickness = clamp(hitboxThickness, 1, 8);
		hitboxColour = clamp(hitboxColour, 0, 0xFFFFFF);
		hitboxReachColour = clamp(hitboxReachColour, 0, 0xFFFFFF);
		statusEffectScales.replaceAll((style, value) -> clamp(value, 50, 200));
		statusEffectTimerSizes.replaceAll((style, value) -> clamp(value, 50, 200));
		if (statusEffectStyle == null) {
			statusEffectStyle = EffectStyle.NUMBERS;
		}
		if (statusEffectColour == null) {
			statusEffectColour = EffectTextColour.WHITE;
		}

		if (soundVolumeLevels == null) {
			soundVolumeLevels = new LinkedHashMap<>();
		} else {
			soundVolumeLevels.replaceAll((id, level) -> clamp(level == null ? 100 : level, 0, 200));
			// 100 means untouched, so keeping it would just grow the file.
			soundVolumeLevels.entrySet().removeIf(entry -> entry.getValue() == 100);
		}
	}

	private static int clamp(int value, int min, int max) {
		return value < min ? min : Math.min(value, max);
	}
}
