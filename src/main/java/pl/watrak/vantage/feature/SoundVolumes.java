package pl.watrak.vantage.feature;

import pl.watrak.vantage.config.ConfigManager;
import pl.watrak.vantage.config.VantageConfig;

import java.util.Map;

/**
 * Per-sound volume levels.
 *
 * <p>Separate from the category sliders vanilla already has, because those are
 * far too coarse for the usual complaint — one specific sound being painfully
 * loud does not mean the whole category should be quieter.
 */
public final class SoundVolumes {

	/** Percentage meaning "leave this sound alone". */
	public static final int FULL_PERCENT = 100;

	private SoundVolumes() {
	}

	/** Multiplier for a sound id, 1.0 when it has no level of its own. */
	public static float multiplierFor(String soundId) {
		VantageConfig config = ConfigManager.get();
		if (!config.soundVolumes || config.soundVolumeLevels.isEmpty()) {
			return 1.0F;
		}
		return resolve(config.soundVolumeLevels, soundId);
	}

	/** Split out with no Minecraft types so the rule can be tested directly. */
	public static float resolve(Map<String, Integer> levels, String soundId) {
		if (levels == null || levels.isEmpty() || soundId == null) {
			return 1.0F;
		}

		Integer level = levels.get(soundId);
		if (level == null || level == FULL_PERCENT) {
			return 1.0F;
		}
		return Math.max(0, level) / 100.0F;
	}

	public static boolean isFull(int percent) {
		return percent == FULL_PERCENT;
	}
}
