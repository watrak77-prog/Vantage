package pl.watrak.vantage.mixin;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.watrak.vantage.feature.CombatHitboxes;

/**
 * Where the player outlines are emitted.
 *
 * <p>Gizmos are only collected while a collector is active, and that window is
 * this method. It runs every frame regardless of whether the debug view is on —
 * the individual debug renderers decide for themselves whether to draw — so
 * joining them here costs nothing when the feature is off and needs no separate
 * render hook of its own.
 */
@Mixin(DebugRenderer.class)
public abstract class DebugRendererMixin {

	@Inject(method = "emitGizmos", at = @At("TAIL"))
	private void vantage$outlinePlayers(Frustum frustum, double x, double y, double z,
	                                    float partialTick, CallbackInfo ci) {
		CombatHitboxes.emit(frustum, partialTick);
	}
}
