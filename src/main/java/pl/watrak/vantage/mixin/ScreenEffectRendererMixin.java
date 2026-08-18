package pl.watrak.vantage.mixin;

import net.minecraft.client.renderer.ScreenEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import pl.watrak.vantage.config.ConfigManager;

/** Moves the first-person fire overlay out of the middle of the screen. */
@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {

	/**
	 * Shifts the two fire quads vertically.
	 *
	 * <p>Vanilla translates each quad by a fixed -0.3 on Y before drawing it.
	 * Adjusting that argument moves the flames without touching the rest of the
	 * pose stack, so nothing drawn afterwards inherits the offset.
	 *
	 * <p>Static because the method it targets is static.
	 *
	 * <p>26.2 rebuilt this: the two quads are assembled inside a geometry
	 * callback, and the same -0.3 now lands on a matrix rather than on the pose
	 * stack. The argument being adjusted is still the Y of a translate, so the
	 * handler is unchanged and only the place it attaches to differs.
	 *
	 * <p>That place is a lambda, whose name is assigned by the compiler and is
	 * therefore tied to this specific build — one of the few targets here that
	 * needs rechecking on every version bump.
	 */
	@ModifyArg(
			//? if >=26.2 {
			/*method = "lambda$submitFire$0",
			at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;translate(FFF)Lorg/joml/Matrix4f;"),
			*///?} else {
			method = "renderFire",
			at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"),
			//?}
			index = 1
	)
	private static float vantage$offsetFire(float y) {
		return y + ConfigManager.get().fireOffset / 100.0F;
	}
}
