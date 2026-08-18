package pl.watrak.vantage.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
//? if >=1.21.9 {
import net.minecraft.client.renderer.state.CameraRenderState;
//?}
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.watrak.vantage.feature.ItemRenderFeature;
import pl.watrak.vantage.feature.ScaledRenderState;

/**
 * Scales items lying on the ground.
 *
 * <p>Separate from the first-person scale so a big totem in the hand does not
 * force a big totem on the floor, and vice versa — they solve different
 * problems, one visibility in a fight and one spotting drops.
 */
@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin {

	/** The item stack is only reachable here, so the scale is resolved now. */
	@Inject(method = "extractRenderState", at = @At("RETURN"))
	private void vantage$captureScale(ItemEntity itemEntity, ItemEntityRenderState state,
	                                  float partialTick, CallbackInfo ci) {
		((ScaledRenderState) state).vantage$setScale(ItemRenderFeature.droppedScale(itemEntity.getItem()));
	}

	/**
	 * Pushed unconditionally rather than only when a scale is set, so the pop
	 * below never has to re-evaluate the condition to stay balanced.
	 */
	//? if >=1.21.9 {
	@Inject(method = "submit", at = @At("HEAD"))
	private void vantage$pushScale(ItemEntityRenderState state, PoseStack poseStack,
	                               SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
	//?} else {
	/*@Inject(method = "render", at = @At("HEAD"))
	private void vantage$pushScale(ItemEntityRenderState state, PoseStack poseStack,
	                               MultiBufferSource bufferSource, int light, CallbackInfo ci) {
	*///?}
		poseStack.pushPose();

		float scale = ((ScaledRenderState) state).vantage$getScale();
		if (scale != 1.0F) {
			// Applied before vanilla's hover offset so a larger item also floats
			// proportionally higher, instead of sinking into the ground.
			poseStack.scale(scale, scale, scale);
		}
	}

	//? if >=1.21.9 {
	@Inject(method = "submit", at = @At("RETURN"))
	private void vantage$popScale(ItemEntityRenderState state, PoseStack poseStack,
	                              SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
	//?} else {
	/*@Inject(method = "render", at = @At("RETURN"))
	private void vantage$popScale(ItemEntityRenderState state, PoseStack poseStack,
	                              MultiBufferSource bufferSource, int light, CallbackInfo ci) {
	*///?}
		poseStack.popPose();
	}
}
