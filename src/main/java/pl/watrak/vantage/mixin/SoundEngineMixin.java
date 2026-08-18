package pl.watrak.vantage.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pl.watrak.vantage.feature.SoundVolumes;

/**
 * Applies the per-sound volume levels.
 *
 * <p>Hooked where vanilla works out how loud one instance should be, which is
 * after the category sliders have had their say — so a sound turned down here
 * stays proportional to whatever the player's Music &amp; Sounds settings say.
 */
@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {

	@ModifyReturnValue(
			method = "calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F",
			at = @At("RETURN")
	)
	private float vantage$applyPerSoundVolume(float original, @Local(argsOnly = true) SoundInstance instance) {
		return original * SoundVolumes.multiplierFor(instance.getIdentifier().toString());
	}
}
