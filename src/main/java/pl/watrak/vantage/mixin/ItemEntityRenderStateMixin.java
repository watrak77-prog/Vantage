package pl.watrak.vantage.mixin;

import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import pl.watrak.vantage.feature.ScaledRenderState;

/** Adds the render scale field to a dropped item's render state. */
@Mixin(ItemEntityRenderState.class)
public abstract class ItemEntityRenderStateMixin implements ScaledRenderState {

	@Unique
	private float vantage$scale = 1.0F;

	@Override
	public void vantage$setScale(float scale) {
		this.vantage$scale = scale;
	}

	@Override
	public float vantage$getScale() {
		return this.vantage$scale;
	}
}
