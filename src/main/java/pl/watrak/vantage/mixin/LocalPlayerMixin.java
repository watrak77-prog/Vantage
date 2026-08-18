package pl.watrak.vantage.mixin;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.watrak.vantage.config.ConfigManager;

/** Stops a freshly placed sign from grabbing the screen for its text editor. */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

	/**
	 * Declines the server's request to open the sign editor.
	 *
	 * <p>The sign is already placed by the time this arrives, so refusing the
	 * editor leaves a blank sign rather than cancelling anything. The sign can
	 * still be edited afterwards by right-clicking it, which routes through the
	 * same call and is equally suppressed — worth knowing before turning this on.
	 */
	@Inject(method = "openTextEdit", at = @At("HEAD"), cancellable = true)
	private void vantage$skipSignEditor(SignBlockEntity signBlockEntity, boolean frontText, CallbackInfo ci) {
		if (ConfigManager.get().noSignGui) {
			ci.cancel();
		}
	}
}
