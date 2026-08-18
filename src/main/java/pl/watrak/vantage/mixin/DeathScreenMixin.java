package pl.watrak.vantage.mixin;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.watrak.vantage.config.ConfigManager;

/**
 * Puts the place you died on the death screen.
 *
 * <p>Read from the player rather than remembered from a death event, because
 * the client keeps the body where it fell until you respawn — so the position
 * is still right there, and nothing has to be tracked in advance.
 */
@Mixin(DeathScreen.class)
public abstract class DeathScreenMixin {

	/** Below the score line, which vanilla puts at 100. */
	private static final int COORDS_Y = 115;

	@Inject(
			//? if >=26.1 {
			/*method = "extractRenderState",
			*///?} else {
			method = "render",
			//?}
			at = @At("TAIL"))
	private void vantage$showDeathCoordinates(GuiGraphics graphics, int mouseX, int mouseY,
	                                          float partialTick, CallbackInfo ci) {
		if (!ConfigManager.get().deathCoordinates) {
			return;
		}

		DeathScreen screen = (DeathScreen) (Object) this;
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null) {
			return;
		}

		BlockPos pos = player.blockPosition();
		Component line = Component.translatable("vantage.death.coordinates",
				pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.GRAY);

		graphics.drawCenteredString(minecraft.font, line, screen.width / 2, COORDS_Y, 0xFFFFFFFF);
	}
}
