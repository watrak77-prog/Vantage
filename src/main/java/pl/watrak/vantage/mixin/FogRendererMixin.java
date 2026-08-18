package pl.watrak.vantage.mixin;

import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import pl.watrak.vantage.config.ConfigManager;
import pl.watrak.vantage.feature.FogFeature;

/**
 * The view-distance haze, which vanilla applies on top of whichever fog
 * environment is active.
 *
 * <p>It is written straight into the uniform buffer after the environment loop
 * rather than stored on the {@link net.minecraft.client.renderer.fog.FogData},
 * so it has to be intercepted at the upload call. Targeting the arguments by
 * index keeps this independent of how the surrounding method is laid out.
 */
@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {

	private static final String UPDATE_BUFFER =
			"Lnet/minecraft/client/renderer/fog/FogRenderer;updateBuffer(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V";

	/**
	 * The method holding the upload call.
	 *
	 * <p>26.1 split the work in two: setupFog now only computes a FogData and
	 * hands it back, and a separate updateBuffer sends it to the GPU. The call
	 * being intercepted moved with it, so only the enclosing method differs —
	 * the target and argument positions below are unchanged.
	 */
	//? if >=26.1 {
	/*private static final String ENCLOSING = "updateBuffer(Lnet/minecraft/client/renderer/fog/FogData;)V";
	*///?} else {
	private static final String ENCLOSING = "setupFog";
	//?}

	@ModifyArg(method = ENCLOSING, at = @At(value = "INVOKE", target = UPDATE_BUFFER), index = 5)
	private float vantage$renderDistanceStart(float original) {
		return ConfigManager.get().disableTerrainFog ? FogFeature.noFogStart() : original;
	}

	@ModifyArg(method = ENCLOSING, at = @At(value = "INVOKE", target = UPDATE_BUFFER), index = 6)
	private float vantage$renderDistanceEnd(float original) {
		return ConfigManager.get().disableTerrainFog ? FogFeature.noFogEnd() : original;
	}
}
