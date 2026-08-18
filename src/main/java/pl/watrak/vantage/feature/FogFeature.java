package pl.watrak.vantage.feature;

import net.minecraft.client.renderer.fog.FogData;

/**
 * Shared logic for the fog toggles.
 *
 * <p>Turning a fog type off is not a matter of skipping its setup: an untouched
 * {@link FogData} is all zeros, and zero start and end distances mean the fog
 * shader drowns everything at point blank range. Disabling fog therefore means
 * pushing its distances past anything the player can see, which is what vanilla
 * itself does for its "no fog" buffer.
 */
public final class FogFeature {

	/**
	 * Distances used to mean "never". Vanilla uses {@link Float#MAX_VALUE} for
	 * its empty fog buffer; a large finite value is used here instead so that
	 * any interpolation the shader does between start and end stays ordinary
	 * arithmetic rather than producing infinities.
	 */
	private static final float NO_FOG_START = 1.0E9F;
	private static final float NO_FOG_END = 2.0E9F;

	private FogFeature() {
	}

	/** Pushes every distance on {@code data} out of sight. */
	public static void disable(FogData data) {
		data.environmentalStart = NO_FOG_START;
		data.environmentalEnd = NO_FOG_END;
		data.skyEnd = NO_FOG_END;
		data.cloudEnd = NO_FOG_END;
	}

	public static float noFogStart() {
		return NO_FOG_START;
	}

	public static float noFogEnd() {
		return NO_FOG_END;
	}
}
