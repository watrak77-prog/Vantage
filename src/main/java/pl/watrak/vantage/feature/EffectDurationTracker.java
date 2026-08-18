package pl.watrak.vantage.feature;

import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * How full each status effect bar should be.
 *
 * <p>A {@link MobEffectInstance} only knows how much time it has left, never how
 * much it started with, so a bar has nothing to measure against. The longest
 * duration seen for each effect is remembered here and used as the full mark.
 *
 * <p>Drinking another potion pushes the remaining time back up, and taking that
 * as the new full mark is what makes the bar refill rather than jump backwards.
 *
 * <p>Effects that end are forgotten, because a stale mark is worse than none: a
 * remembered eight-minute Regeneration would draw a fresh thirty-second one as
 * almost empty.
 */
public final class EffectDurationTracker {

	private static final Map<Holder<MobEffect>, Integer> PEAK = new HashMap<>();

	private EffectDurationTracker() {
	}

	/** Refreshes the marks from the effects currently active, dropping the rest. */
	public static void update(List<MobEffectInstance> active) {
		PEAK.keySet().removeIf(held -> active.stream().noneMatch(e -> e.getEffect().equals(held)));

		for (MobEffectInstance effect : active) {
			if (!effect.isInfiniteDuration()) {
				PEAK.merge(effect.getEffect(), effect.getDuration(), Math::max);
			}
		}
	}

	/** Fraction of the bar to fill, from 0 to 1. */
	public static float progress(MobEffectInstance effect) {
		if (effect.isInfiniteDuration()) {
			return 1.0F;
		}

		Integer peak = PEAK.get(effect.getEffect());
		if (peak == null || peak <= 0) {
			return 1.0F;
		}
		return Mth.clamp((float) effect.getDuration() / peak, 0.0F, 1.0F);
	}

	/** Dropped on disconnect so a new world does not inherit the old marks. */
	public static void clear() {
		PEAK.clear();
	}

}
