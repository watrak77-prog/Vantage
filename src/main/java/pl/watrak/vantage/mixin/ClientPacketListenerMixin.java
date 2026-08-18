package pl.watrak.vantage.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.watrak.vantage.feature.ShieldStatusTracker;

/** Listens for the sound that gives away a disabled shield. */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

	/**
	 * Notes the position of every shield disable, including other players'.
	 *
	 * <p>Vanilla broadcasts this sound to everyone nearby rather than only to
	 * the player it happened to, which makes it the one signal about an
	 * opponent's shield that actually crosses the network.
	 *
	 * <p>Injected at the tail so it runs after the packet has been handed to the
	 * main thread, rather than on the network thread where the map is not safe
	 * to touch.
	 */
	@Inject(method = "handleSoundEvent", at = @At("TAIL"))
	private void vantage$noteShieldDisable(ClientboundSoundPacket packet, CallbackInfo ci) {
		if (packet.getSound().value() == SoundEvents.SHIELD_BREAK.value()) {
			ShieldStatusTracker.onDisableSound(packet.getX(), packet.getY(), packet.getZ());
		}
	}
}
