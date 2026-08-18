package pl.watrak.vantage.feature;

import net.minecraft.world.entity.HumanoidArm;

/**
 * Carries "this entity is holding a disabled shield, in this hand" onto a living
 * entity's render state.
 *
 * <p>Render states are detached from their entities, so the layer that draws the
 * held item has no way back to the player it belongs to. The answer is therefore
 * worked out while the state is being extracted and read again when it is drawn.
 *
 * <p>The hand is recorded rather than a plain flag because the layer draws each
 * arm separately and has to tint only the one actually holding the shield. It is
 * also the one thing every supported version hands to that layer: older ones do
 * not pass the item stack at all, so asking the arm is what keeps a single
 * implementation working across the whole range.
 */
public interface DisabledShieldState {

	/** The hand holding a disabled shield, or null when there is none. */
	void vantage$setDisabledShieldArm(HumanoidArm arm);

	HumanoidArm vantage$getDisabledShieldArm();
}
