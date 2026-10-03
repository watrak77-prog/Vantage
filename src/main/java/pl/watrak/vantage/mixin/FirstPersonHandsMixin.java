package pl.watrak.vantage.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pl.watrak.vantage.config.ConfigManager;
import pl.watrak.vantage.feature.ItemRenderFeature;

/**
 * Keeps a held map on screen while rowing a boat, on 26.3 and later.
 *
 * <p>This is the same fix as the one in {@code ItemInHandRendererMixin}, moved
 * because the code it corrects moved. Up to 26.2 the hand height was wound down
 * inside the renderer's own tick; 26.3 split the per-tick state out into this
 * class, which fills a render state the renderer later reads. The renderer no
 * longer has a tick at all, so there is nothing left to hook there.
 *
 * <p>The target is named as a string and the config carrying this is marked
 * optional, so on every earlier version — where the class does not exist — Mixin
 * skips it instead of failing.
 */
@Mixin(targets = "net.minecraft.client.player.FirstPersonHandsAndItems")
public abstract class FirstPersonHandsMixin {

	@ModifyExpressionValue(
			method = "tick",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isHandsBusy()Z")
	)
	private boolean vantage$keepMapUpInBoat(boolean handsBusy) {
		if (!handsBusy || !ConfigManager.get().showMapInBoat) {
			return handsBusy;
		}

		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			return handsBusy;
		}

		return !ItemRenderFeature.isHoldingMap(player);
	}
}
