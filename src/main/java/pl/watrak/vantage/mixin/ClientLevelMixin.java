package pl.watrak.vantage.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.watrak.vantage.config.ConfigManager;

/**
 * Makes barrier blocks visible without holding one.
 *
 * <p>Vanilla already knows how to show them: it marks barriers with a particle,
 * but only in creative and only while a barrier is the main-hand item. Both
 * conditions live in one method, so lifting them is the whole feature — and the
 * result looks exactly like vanilla's own barrier display rather than a
 * bolted-on overlay.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {

	@Inject(method = "getMarkerParticleTarget", at = @At("HEAD"), cancellable = true)
	private void vantage$alwaysMarkBarriers(CallbackInfoReturnable<Block> cir) {
		if (ConfigManager.get().visualBarriers) {
			cir.setReturnValue(Blocks.BARRIER);
		}
	}

}
