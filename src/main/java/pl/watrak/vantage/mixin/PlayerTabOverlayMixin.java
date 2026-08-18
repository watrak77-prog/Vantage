package pl.watrak.vantage.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.watrak.vantage.compat.ClientCompat;
import pl.watrak.vantage.config.ConfigManager;

/** Forces player faces into the tab list. */
@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin {

	/** Width of the ping sprite the number replaces. */
	@Unique
	private static final int ICON_WIDTH = 10;

	/** Ceiling on the text size, so a short ping is not blown up. */
	@Unique
	private static final float MAX_PING_SCALE = 0.75F;

	/** Below this the connection is not what is costing you fights. */
	@Unique
	private static final int GOOD_PING_MS = 100;

	/** Above this it is, and the number turns red. */
	@Unique
	private static final int FAIR_PING_MS = 200;

	/** Breathing room between the name and the number. */
	@Unique
	private static final int NAME_GAP = 3;

	/** Width vanilla leaves for the face before the name starts. */
	@Unique
	private static final int FACE_WIDTH = 9;

	/**
	 * Whether a face is drawn before the name, which shifts where it ends.
	 *
	 * <p>Mirrors vanilla's own condition, plus this mod's option to force faces
	 * on — otherwise the number lands nine pixels off whenever that is in use.
	 */
	@Unique
	private static int faceWidth() {
		Minecraft minecraft = Minecraft.getInstance();
		boolean shown = ConfigManager.get().tabHeads
				|| ClientCompat.vanillaShowsTabFaces(minecraft);
		return shown ? FACE_WIDTH : 0;
	}

	/**
	 * Vanilla only draws faces when the connection is encrypted or the world is
	 * local, which quietly hides them on servers running without encryption —
	 * including most local proxies and test setups. The skins are already
	 * downloaded either way, so showing them costs nothing.
	 */
	@ModifyExpressionValue(
			// Two separate boundaries: 26.1 renamed the method that draws, while
			// the encryption check it used survived there and only went in 26.2.
			//? if >=26.1 {
			/*method = "extractRenderState",
			*///?} else {
			method = "render",
			//?}
			//? if >=26.2 {
			/*at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;onlineMode()Z")
			*///?} else {
			at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;isEncrypted()Z")
			//?}
	)
	private boolean vantage$alwaysShowFaces(boolean original) {
		return ConfigManager.get().tabHeads || original;
	}

	/**
	 * Replaces the five-bar ping icon with the latency in milliseconds.
	 *
	 * <p>The bars only distinguish five bands, the widest of which spans
	 * everything under 150 ms — so a 20 ms player and a 140 ms player look
	 * identical. The number does not hide that.
	 *
	 * <p>Right-aligned to where the icon's right edge was, so nothing else in
	 * the row shifts.
	 */
	@Inject(
			//? if >=26.1 {
			/*method = "extractPingIcon",
			*///?} else {
			method = "renderPingIcon",
			//?}
			at = @At("HEAD"), cancellable = true)
	private void vantage$drawPingNumber(GuiGraphics graphics, int width, int x, int y,
	                                    PlayerInfo playerInfo, CallbackInfo ci) {
		if (!ConfigManager.get().pingNumber) {
			return;
		}

		int latency = playerInfo.getLatency();
		String text = latency < 0 ? "?" : String.valueOf(Math.min(latency, 999));

		Font font = Minecraft.getInstance().font;

		int textWidth = font.width(text);
		float scale = Math.min(MAX_PING_SCALE, (float) ICON_WIDTH / Math.max(1, textWidth));
		int scaledWidth = Math.round(textWidth * scale);

		// Placed just past the end of this player's own name rather than at the
		// column's right edge. The column is sized for the longest name in the
		// list, so anchoring right put the number on top of shorter ones — and
		// on a restyled tab list the right edge is not where vanilla thinks it
		// is at all. Measuring the name that is actually drawn avoids both.
		Component name = ((PlayerTabOverlay) (Object) this).getNameForDisplay(playerInfo);
		int nameEnd = x + faceWidth() + font.width(name);

		int right = x + width - 1;
		int drawX = Math.min(nameEnd + NAME_GAP, right - scaledWidth);
		int drawY = y + Math.round((8 - font.lineHeight * scale) / 2.0F);

		graphics.pose().pushMatrix();
		graphics.pose().scale(scale, scale);
		graphics.drawString(font, text,
				Math.round(drawX / scale), Math.round(drawY / scale),
				colourFor(latency), true);
		graphics.pose().popMatrix();

		ci.cancel();
	}

	/**
	 * Green, yellow, red by how bad the latency actually is.
	 *
	 * <p>Tighter than vanilla's bands, which call everything under 150 ms full
	 * bars. The thresholds are set where the difference starts to be felt in a
	 * fight rather than where the sprite happens to change.
	 */
	@Unique
	private static int colourFor(int latency) {
		if (latency < 0) {
			return 0xFFAAAAAA;
		}
		if (latency < GOOD_PING_MS) {
			return 0xFF55FF55;
		}
		return latency < FAIR_PING_MS ? 0xFFFFFF55 : 0xFFFF5555;
	}
}
