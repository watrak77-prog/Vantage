package pl.watrak.vantage.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
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
 * <p>Both hooks sit after the category sliders have had their say, so a sound
 * turned down here stays proportional to whatever the player's Music &amp;
 * Sounds settings are set to.
 *
 * <p>There are two of them because the game works the volume out in two places
 * and only one of them is shared. Starting a sound reads the level straight from
 * the two-argument form, which knows the category but not which sound it is;
 * re-reading the level of one already playing goes through the one-argument form
 * instead. Hooking only the latter left every one-shot sound at full volume —
 * which is nearly all of them — while looping sounds were quietened correctly.
 * The two paths never meet, so nothing is scaled twice.
 */
@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {

	/** A sound starting. */
	@ModifyExpressionValue(
			method = "play",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/sounds/SoundEngine;"
							+ "calculateVolume(FLnet/minecraft/sounds/SoundSource;)F"
			)
	)
	private float vantage$applyToStartingSound(float original, @Local(argsOnly = true) SoundInstance instance) {
		return original * SoundVolumes.multiplierFor(instance.getIdentifier().toString());
	}

	/** A sound already playing having its volume refreshed. */
	@ModifyReturnValue(
			method = "calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F",
			at = @At("RETURN")
	)
	private float vantage$applyToPlayingSound(float original, @Local(argsOnly = true) SoundInstance instance) {
		return original * SoundVolumes.multiplierFor(instance.getIdentifier().toString());
	}
}
