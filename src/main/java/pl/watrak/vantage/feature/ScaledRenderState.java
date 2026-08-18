package pl.watrak.vantage.feature;

/**
 * Carries a render scale on a dropped item's render state.
 *
 * <p>The scale has to be worked out from the {@code ItemEntity}, which is only
 * in scope while the render state is being extracted, but applied later when
 * that state is submitted — and by then the item stack is gone. This interface
 * is mixed into the render state so the value can travel between the two.
 */
public interface ScaledRenderState {

	void vantage$setScale(float scale);

	float vantage$getScale();
}
