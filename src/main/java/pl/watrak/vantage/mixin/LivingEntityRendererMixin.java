package pl.watrak.vantage.mixin;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ShieldItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.watrak.vantage.feature.DisabledShieldState;
import pl.watrak.vantage.feature.ShieldStatusTracker;

/** Records whether an entity's shield is disabled while its state is built. */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

	/**
	 * The last point at which the entity and its render state are both in hand.
	 * After this the renderer only sees the state, so the answer has to be
	 * resolved now and carried along.
	 */
	@Inject(method = "extractRenderState", at = @At("RETURN"))
	private void vantage$captureShieldStatus(LivingEntity entity, LivingEntityRenderState state,
	                                         float partialTick, CallbackInfo ci) {
		((DisabledShieldState) state).vantage$setDisabledShieldArm(vantage$disabledShieldArm(entity));
	}

	/**
	 * Which hand is holding a shield that is currently disabled, if either.
	 *
	 * <p>The offhand is checked first because that is where a shield normally
	 * lives, but holding one in the main hand is legal and blocks just the same.
	 */
	@Unique
	private static HumanoidArm vantage$disabledShieldArm(LivingEntity entity) {
		if (!ShieldStatusTracker.isDisabled(entity)) {
			return null;
		}
		if (entity.getOffhandItem().getItem() instanceof ShieldItem) {
			return entity.getMainArm().getOpposite();
		}
		if (entity.getMainHandItem().getItem() instanceof ShieldItem) {
			return entity.getMainArm();
		}
		return null;
	}
}
