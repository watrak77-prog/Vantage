package pl.watrak.vantage.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import pl.watrak.vantage.feature.ShieldUseState;

/** Exposes the use-item fields so the client can reconstruct them. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityShieldMixin implements ShieldUseState {

	@Shadow
	protected ItemStack useItem;

	@Shadow
	protected int useItemRemaining;

	@Override
	public void vantage$setUseItem(ItemStack stack, int remainingTicks) {
		this.useItem = stack;
		this.useItemRemaining = remainingTicks;
	}
}
