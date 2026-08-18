package pl.watrak.vantage.mixin;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.watrak.vantage.config.ConfigManager;

/**
 * Drops the smoke clouds thrown up by explosions.
 *
 * <p>A crystal or TNT detonation fills the screen at exactly the moment you
 * most need to see through it. Only the puffs go: the flash, the sound and the
 * damage are all untouched, so nothing about reading the fight changes except
 * that you can still see it.
 */
@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {

	@Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
	private void vantage$dropExplosionParticles(ParticleOptions options, double x, double y, double z,
	                                            double dx, double dy, double dz,
	                                            CallbackInfoReturnable<Particle> cir) {
		if (!ConfigManager.get().noExplosionParticles) {
			return;
		}

		if (options.getType() == ParticleTypes.EXPLOSION || options.getType() == ParticleTypes.EXPLOSION_EMITTER) {
			cir.setReturnValue(null);
		}
	}
}
