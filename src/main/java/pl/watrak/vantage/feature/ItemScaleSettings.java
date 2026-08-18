package pl.watrak.vantage.feature;

import java.util.Map;

/**
 * Decides what scale an item renders at, with no Minecraft types involved.
 *
 * <p>Kept free of game classes so the rule can be tested directly. The rule is
 * small but it is the one place where the master toggle and the per-item
 * overrides have to agree, and getting it wrong is invisible until something
 * renders at the wrong size.
 */
public final class ItemScaleSettings {

	/** Percentage meaning "leave this item alone". */
	public static final int NEUTRAL_PERCENT = 100;

	private ItemScaleSettings() {
	}

	/**
	 * Resolves the render scale for one item.
	 *
	 * <p>Returns exactly 1 whenever scaling is switched off, whatever overrides
	 * happen to be stored. A stored value of {@link #NEUTRAL_PERCENT} also
	 * resolves to 1, so an entry left behind at 100 cannot keep an item scaled
	 * — the map is a set of deliberate changes, and 100 is not one.
	 */
	public static float resolve(boolean enabled, Map<String, Integer> overrides, String itemId) {
		if (!enabled || overrides == null || overrides.isEmpty() || itemId == null) {
			return 1.0F;
		}

		Integer percent = overrides.get(itemId);
		if (percent == null || percent == NEUTRAL_PERCENT) {
			return 1.0F;
		}

		return percent / 100.0F;
	}

	public static boolean isNeutral(int percent) {
		return percent == NEUTRAL_PERCENT;
	}
}
