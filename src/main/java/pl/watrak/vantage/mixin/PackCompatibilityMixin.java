package pl.watrak.vantage.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.server.packs.repository.PackCompatibility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pl.watrak.vantage.config.ConfigManager;

/**
 * Stops resource packs being flagged as made for another version.
 *
 * <p>The pack format number is bumped for any format change at all, so a pack
 * that only replaces textures is marked incompatible by versions it works
 * perfectly well on — and then hidden behind a confirmation every time.
 *
 * <p>Only the label and the prompt change. A pack that genuinely relies on a
 * format the game no longer understands still fails to load the parts it cannot
 * read; this does not make it work, it stops the game insisting on asking.
 */
@Mixin(PackCompatibility.class)
public abstract class PackCompatibilityMixin {

	@ModifyReturnValue(method = "isCompatible", at = @At("RETURN"))
	private boolean vantage$treatAsCompatible(boolean original) {
		return ConfigManager.get().noResourcePackWarning || original;
	}
}
