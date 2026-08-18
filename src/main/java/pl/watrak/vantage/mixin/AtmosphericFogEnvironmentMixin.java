package pl.watrak.vantage.mixin;

import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.watrak.vantage.config.ConfigManager;
import pl.watrak.vantage.config.VantageConfig;
import pl.watrak.vantage.feature.FogFeature;

/**
 * Outdoor fog, which carries two independently toggleable parts: the haze over
 * terrain and the fog blended into the sky and clouds.
 *
 * <p>Injected on return rather than cancelled, so vanilla still computes the
 * part the player chose to keep.
 */
@Mixin(AtmosphericFogEnvironment.class)
public abstract class AtmosphericFogEnvironmentMixin {

	/*
	 * 1.21.11 replaced the entity and block position with the camera that had
	 * been derived from them, so the whole parameter list changes rather than a
	 * single type. Mixin matches injections by descriptor, which makes this one
	 * of the few differences the compiler cannot catch.
	 */
	//? if >=1.21.11 {
	@Inject(method = "setupFog", at = @At("RETURN"))
	private void vantage$stripAtmosphericFog(FogData fogData, Camera camera, ClientLevel clientLevel,
	                                         float f, DeltaTracker deltaTracker, CallbackInfo ci) {
	//?} else {
	/*@Inject(method = "setupFog", at = @At("RETURN"))
	private void vantage$stripAtmosphericFog(FogData fogData, Entity entity, BlockPos blockPos,
                                          ClientLevel clientLevel, float f, DeltaTracker deltaTracker,
                                          CallbackInfo ci) {
	*///?}
		VantageConfig config = ConfigManager.get();

		if (config.disableTerrainFog) {
			fogData.environmentalStart = FogFeature.noFogStart();
			fogData.environmentalEnd = FogFeature.noFogEnd();
		}

		if (config.disableSkyFog) {
			fogData.skyEnd = FogFeature.noFogEnd();
			fogData.cloudEnd = FogFeature.noFogEnd();
		}
	}
}
