package pl.watrak.vantage.mixin;

import net.minecraft.util.random.WeightedList;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.watrak.vantage.config.ConfigManager;

/**
 * Stops explosions throwing up their cloud of debris particles.
 *
 * <p>The flash is only half of what an explosion draws. The rest is a burst of
 * poof and smoke particles scaled to how many blocks were destroyed, which is
 * what actually fills the screen — and it is registered here rather than spawned
 * directly, so this is the one place it can be stopped.
 *
 * <p>Cancelled at the point the explosion is recorded, not by blocking the
 * particle types themselves: poof and smoke are used by campfires, mob deaths
 * and a dozen other things that have no reason to disappear.
 *
 * <p>This burst is a 1.21.9 addition, so on older versions there is nothing here
 * to cancel and the explosion flash is handled by {@code ParticleEngineMixin}
 * alone. The target is named as a string and the list left unparameterised so
 * this still compiles where neither class exists; the config carrying it is
 * marked optional, which is what lets Mixin skip it rather than fail.
 */
@Mixin(targets = "net.minecraft.client.multiplayer.ClientExplosionTracker")
public abstract class ClientExplosionTrackerMixin {

	@Inject(method = "track", at = @At("HEAD"), cancellable = true)
	private void vantage$dropExplosionDebris(Vec3 center, float radius, int blockCount,
	                                         WeightedList<?> particles, CallbackInfo ci) {
		if (ConfigManager.get().noExplosionParticles) {
			ci.cancel();
		}
	}
}
