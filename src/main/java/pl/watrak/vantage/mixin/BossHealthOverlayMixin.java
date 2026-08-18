package pl.watrak.vantage.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.gui.components.BossHealthOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pl.watrak.vantage.config.ConfigManager;

/**
 * "Thick fog" is the dense fog a boss bar can request, used by the Wither and
 * the Ender Dragon.
 *
 * <p>Denying the request at the source also stops it feeding into the lightmap,
 * which is the other place vanilla consults it.
 */
@Mixin(BossHealthOverlay.class)
public abstract class BossHealthOverlayMixin {

	@ModifyReturnValue(method = "shouldCreateWorldFog", at = @At("RETURN"))
	private boolean vantage$suppressThickFog(boolean original) {
		return !ConfigManager.get().disableThickFog && original;
	}
}
