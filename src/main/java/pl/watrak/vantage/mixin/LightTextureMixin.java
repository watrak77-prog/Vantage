package pl.watrak.vantage.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
//? if >=26.1 {
/*import net.minecraft.client.renderer.LightmapRenderStateExtractor;
*///?} else {
import net.minecraft.client.renderer.LightTexture;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;
import pl.watrak.vantage.config.ConfigManager;
import pl.watrak.vantage.config.VantageConfig;

/**
 * Fullbright and night-vision suppression, both driven by the lightmap.
 *
 * <p>26.x renamed the class and folded the work into its extract pass, but the
 * gamma is still read the same way, so only the two names below differ.
 */
//? if >=26.1 {
/*@Mixin(LightmapRenderStateExtractor.class)
*///?} else {
@Mixin(LightTexture.class)
//?}
public abstract class LightTextureMixin {

	/**
	 * Replaces the gamma the lightmap uploads to the GPU.
	 *
	 * <p>Vanilla gamma runs 0.0 to 1.0, and the shader takes it as-is, so
	 * feeding it a value above 1 is what actually lights the world up. The
	 * configured multiplier is used directly rather than scaled against the
	 * player's own gamma, because multiplying a gamma of zero would leave
	 * fullbright doing nothing at all.
	 *
	 * <p>The slice pins the injection to the {@code gamma()} call specifically;
	 * without it this would also catch the {@code hideLightningFlash} and
	 * {@code darknessEffectScale} lookups in the same method.
	 */
	@ModifyExpressionValue(
			//? if >=26.1 {
			/*method = "extract",
			*///?} else {
			method = "updateLightTexture",
			//?}
			slice = @Slice(
					from = @At(
							value = "INVOKE",
							target = "Lnet/minecraft/client/Options;gamma()Lnet/minecraft/client/OptionInstance;"
					)
			),
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;"
			)
	)
	private Object vantage$overrideGamma(Object original) {
		VantageConfig config = ConfigManager.get();
		if (!config.fullbright || !(original instanceof Double)) {
			return original;
		}
		return (double) config.gammaMultiplier;
	}
}
