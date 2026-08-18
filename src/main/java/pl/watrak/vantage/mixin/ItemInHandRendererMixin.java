package pl.watrak.vantage.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import pl.watrak.vantage.config.ConfigManager;
import pl.watrak.vantage.feature.ItemRenderFeature;

/**
 * Everything that adjusts the item held in first person: the low shield, the
 * per-item scale, the disabled-shield tint, and keeping a map up while rowing.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

	/**
	 * Applies the shield offset and the first-person scale.
	 *
	 * <p>Wrapping the render call rather than injecting around it keeps the push
	 * and pop paired even if vanilla returns early, and confines the change to
	 * the first-person path — {@code renderArmWithItem} is not used for the
	 * third-person model or for items on the ground.
	 *
	 * <p>Shields take the same route as any other item here: vanilla's BLOCK
	 * case deliberately does nothing for them, so the shield is placed entirely
	 * by its own model transform and this is the only point where it can move.
	 */
	@WrapOperation(
			//? if >=26.2 {
			/*method = "submitArmWithItem",
			*///?} else {
			method = "renderArmWithItem",
			//?}
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem("
							+ "Lnet/minecraft/world/entity/LivingEntity;"
							+ "Lnet/minecraft/world/item/ItemStack;"
							+ "Lnet/minecraft/world/item/ItemDisplayContext;"
							+ "Lcom/mojang/blaze3d/vertex/PoseStack;"
							+ "Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"
			)
	)
	private void vantage$transformHeldItem(ItemInHandRenderer self, LivingEntity entity, ItemStack stack,
	                                       ItemDisplayContext context, PoseStack poseStack,
	                                       SubmitNodeCollector collector, int light,
	                                       Operation<Void> original) {
		if (!ItemRenderFeature.isHeldTransformed(stack)) {
			original.call(self, entity, stack, context, poseStack, collector, light);
			return;
		}

		poseStack.pushPose();
		poseStack.translate(0.0F, ItemRenderFeature.verticalOffset(stack), 0.0F);

		float scale = ItemRenderFeature.firstPersonScale(stack);
		poseStack.scale(scale, scale, scale);

		original.call(self, entity, stack, context, poseStack, collector, light);
		poseStack.popPose();
	}

	/**
	 * Turns the held shield red while it is disabled.
	 *
	 * <p>Reuses the overlay texture that vanilla flashes over a hurt entity,
	 * which is already a red tint applied at the shader level — so this costs
	 * nothing extra and matches a colour players already read as "damaged".
	 *
	 * <p>There is no green counterpart: a shield that works is the normal case,
	 * and a permanent indicator for it would be noise.
	 *
	 * <p>Where the tint is applied differs by version. From 1.21.9 the hand
	 * renderer fills in a per-item render state and the overlay is its fourth
	 * argument; before that it handed the stack straight to the item renderer,
	 * where the overlay sits several arguments further along. Both are the same
	 * idea reached through a different call, so the whole target changes rather
	 * than a single name.
	 */
	@ModifyArg(
			method = "renderItem",
			//? if >=1.21.9 {
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit("
							+ "Lcom/mojang/blaze3d/vertex/PoseStack;"
							+ "Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"
			),
			index = 3
			//?} else {
			/*at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;renderStatic("
							+ "Lnet/minecraft/world/entity/LivingEntity;"
							+ "Lnet/minecraft/world/item/ItemStack;"
							+ "Lnet/minecraft/world/item/ItemDisplayContext;"
							+ "Lcom/mojang/blaze3d/vertex/PoseStack;"
							+ "Lnet/minecraft/client/renderer/MultiBufferSource;"
							+ "Lnet/minecraft/world/level/Level;III)V"
			),
			index = 7
			*///?}
	)
	private int vantage$tintDisabledShield(int overlay,
	                                       @Local(argsOnly = true) LivingEntity entity,
	                                       @Local(argsOnly = true) ItemStack stack) {
		if (entity instanceof LocalPlayer player && ItemRenderFeature.isShieldDisabled(player, stack)) {
			return OverlayTexture.pack(OverlayTexture.NO_WHITE_U, OverlayTexture.RED_OVERLAY_V);
		}
		return overlay;
	}

	/**
	 * Keeps a held map on screen while rowing a boat.
	 *
	 * <p>Rowing marks the player's hands busy, and {@code tick} responds by
	 * winding the hand height down to zero. That height feeds the map's vertical
	 * placement, which at zero translates it about a block downwards — far
	 * enough that the map leaves the bottom of the screen entirely. Reporting
	 * hands as free keeps the height up and the map where it was.
	 *
	 * <p>Limited to maps on purpose: for every other item the lowered hand is
	 * the intended rowing animation, and suppressing it everywhere would look
	 * wrong rather than helpful.
	 */
	@ModifyExpressionValue(
			method = "tick",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isHandsBusy()Z")
	)
	private boolean vantage$keepMapUpInBoat(boolean handsBusy) {
		if (!handsBusy || !ConfigManager.get().showMapInBoat) {
			return handsBusy;
		}

		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			return handsBusy;
		}

		boolean holdingMap = player.getMainHandItem().has(DataComponents.MAP_ID)
				|| player.getOffhandItem().has(DataComponents.MAP_ID);
		return !holdingMap;
	}
}
