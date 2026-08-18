package pl.watrak.vantage.mixin;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import pl.watrak.vantage.feature.DisabledShieldState;

/** Adds the disabled-shield hand to every living entity's render state. */
@Mixin(LivingEntityRenderState.class)
public abstract class LivingEntityRenderStateMixin implements DisabledShieldState {

	@Unique
	private HumanoidArm vantage$disabledShieldArm;

	@Override
	public void vantage$setDisabledShieldArm(HumanoidArm arm) {
		this.vantage$disabledShieldArm = arm;
	}

	@Override
	public HumanoidArm vantage$getDisabledShieldArm() {
		return this.vantage$disabledShieldArm;
	}
}
