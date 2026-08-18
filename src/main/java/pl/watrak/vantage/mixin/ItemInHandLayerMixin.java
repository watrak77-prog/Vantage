package pl.watrak.vantage.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import pl.watrak.vantage.feature.DisabledShieldState;

/**
 * Reddens a disabled shield on the entity model.
 *
 * <p>This is the path used for every player you can see and for your own body
 * in third person, so it is what makes the tint work in all F5 camera modes and
 * on opponents — the first-person renderer only ever draws your own hands.
 */
@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin {

	@ModifyArg(
			//? if >=1.21.9 {
			method = "submitArmWithItem",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit("
							+ "Lcom/mojang/blaze3d/vertex/PoseStack;"
							+ "Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"
			),
			//?} else {
			/*method = "renderArmWithItem",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;render("
							+ "Lcom/mojang/blaze3d/vertex/PoseStack;"
							+ "Lnet/minecraft/client/renderer/MultiBufferSource;II)V"
			),
			*///?}
			index = 3
	)
	private int vantage$tintDisabledShield(int overlay,
	                                       @Local(argsOnly = true) ArmedEntityRenderState state,
	                                       @Local(argsOnly = true) HumanoidArm arm) {
		if (arm == ((DisabledShieldState) state).vantage$getDisabledShieldArm()) {
			return OverlayTexture.pack(OverlayTexture.NO_WHITE_U, OverlayTexture.RED_OVERLAY_V);
		}
		return overlay;
	}
}
