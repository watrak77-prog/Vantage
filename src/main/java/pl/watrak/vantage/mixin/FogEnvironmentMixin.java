package pl.watrak.vantage.mixin;

import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.BlindnessFogEnvironment;
import net.minecraft.client.renderer.fog.environment.DarknessFogEnvironment;
import net.minecraft.client.renderer.fog.environment.LavaFogEnvironment;
import net.minecraft.client.renderer.fog.environment.PowderedSnowFogEnvironment;
import net.minecraft.client.renderer.fog.environment.WaterFogEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.watrak.vantage.config.ConfigManager;
import pl.watrak.vantage.config.VantageConfig;
import pl.watrak.vantage.feature.FogFeature;

/**
 * The five fog types that are wholly on or off.
 *
 * <p>One mixin covers all of them because the shape is identical: push the
 * distances out of sight and skip vanilla's setup entirely. Atmospheric fog is
 * handled separately, since its terrain and sky components toggle independently.
 */
@Mixin({
		LavaFogEnvironment.class,
		PowderedSnowFogEnvironment.class,
		BlindnessFogEnvironment.class,
		DarknessFogEnvironment.class,
		WaterFogEnvironment.class
})
public abstract class FogEnvironmentMixin {

	/*
	 * 1.21.11 replaced the entity and block position with the camera that had
	 * been derived from them, so the whole parameter list changes rather than a
	 * single type. Mixin matches injections by descriptor, which makes this one
	 * of the few differences the compiler cannot catch.
	 */
	//? if >=1.21.11 {
	@Inject(method = "setupFog", at = @At("HEAD"), cancellable = true)
	private void vantage$maybeDisableFog(FogData fogData, Camera camera, ClientLevel clientLevel,
	                                     float f, DeltaTracker deltaTracker, CallbackInfo ci) {
	//?} else {
	/*@Inject(method = "setupFog", at = @At("HEAD"), cancellable = true)
	private void vantage$maybeDisableFog(FogData fogData, Entity entity, BlockPos blockPos,
                                      ClientLevel clientLevel, float f, DeltaTracker deltaTracker,
                                      CallbackInfo ci) {
	*///?}
		if (vantage$isDisabled()) {
			FogFeature.disable(fogData);
			ci.cancel();
		}
	}

	private boolean vantage$isDisabled() {
		VantageConfig config = ConfigManager.get();
		Object self = this;

		if (self instanceof LavaFogEnvironment) {
			return config.disableLavaFog;
		}
		if (self instanceof PowderedSnowFogEnvironment) {
			return config.disablePowderSnowFog;
		}
		if (self instanceof BlindnessFogEnvironment) {
			return config.disableBlindnessFog;
		}
		if (self instanceof DarknessFogEnvironment) {
			return config.disableDarknessFog;
		}
		if (self instanceof WaterFogEnvironment) {
			return config.disableWaterFog;
		}
		return false;
	}
}
