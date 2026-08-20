package pl.watrak.vantage.mixin;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.watrak.vantage.feature.CombatHitboxes;

//? if <1.21.11 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
*///?}

/**
 * Where the player outlines are drawn.
 *
 * <p>This class runs once a frame whatever the debug view is set to — each
 * individual debug renderer decides for itself whether to draw anything — so
 * joining it costs nothing when the feature is off and saves inventing a render
 * hook of our own.
 *
 * <p>What the method is called and what it hands over changed with 1.21.11:
 * before it, debug shapes were written straight into a buffer, and since it they
 * are handed to a collector that draws them later.
 */
@Mixin(DebugRenderer.class)
public abstract class DebugRendererMixin {

	//? if >=1.21.11 {
	@Inject(method = "emitGizmos", at = @At("TAIL"))
	private void vantage$outlinePlayers(Frustum frustum, double x, double y, double z,
	                                    float partialTick, CallbackInfo ci) {
		CombatHitboxes.emit(frustum, partialTick);
	}
	//?} else {
	/*@Inject(method = "render", at = @At("TAIL"))
	private void vantage$outlinePlayers(PoseStack poseStack, Frustum frustum,
	                                    MultiBufferSource.BufferSource buffers,
	                                    double camX, double camY, double camZ, boolean translucent, CallbackInfo ci) {
		CombatHitboxes.emit(poseStack, buffers, frustum, camX, camY, camZ, translucent);
	}
	*///?}
}
