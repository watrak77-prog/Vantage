package pl.watrak.vantage.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
//? if >=26.2 {
/*import net.minecraft.client.gui.contextualbar.LocatorBar;
*///?} else {
import net.minecraft.client.gui.contextualbar.LocatorBarRenderer;
//?}
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.waypoints.TrackedWaypoint;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pl.watrak.vantage.config.ConfigManager;

import java.util.Optional;
import java.util.UUID;

/**
 * Replaces the coloured dot on the locator bar with the player's own face.
 *
 * <p>The bar tells you a direction; a face tells you who. In a fight that is the
 * difference between reacting and guessing, and the skin is already loaded for
 * the tab list.
 *
 * <p>The waypoint loop lives in a lambda, which carries no stable name — the
 * target below is the compiled synthetic method, so it is one of the few places
 * in this mod that is tied to a specific Minecraft build and will need checking
 * on every version bump. The two names differ in kind rather than spelling:
 * obfuscated builds number the lambda, unobfuscated ones name it after the
 * method it was written in.
 */
//? if >=26.2 {
/*@Mixin(LocatorBar.class)
*///?} else {
@Mixin(LocatorBarRenderer.class)
//?}
public abstract class LocatorBarRendererMixin {

	/** Vanilla's dot is nine pixels; a face is eight, so it is nudged to centre. */
	private static final int FACE_SIZE = 8;
	private static final int FACE_INSET = 1;

	@WrapOperation(
			//? if >=26.1 {
			/*method = "lambda$extractRenderState$1",
			*///?} else {
			method = "method_70870",
			//?}
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite("
							+ "Lcom/mojang/blaze3d/pipeline/RenderPipeline;"
							+ "Lnet/minecraft/resources/Identifier;IIIII)V",
					ordinal = 0
			)
	)
	private void vantage$drawFaceMarker(GuiGraphics graphics, RenderPipeline pipeline, Identifier sprite,
	                                    int x, int y, int width, int height, int colour,
	                                    Operation<Void> original,
	                                    @Local(argsOnly = true) TrackedWaypoint waypoint) {
		PlayerInfo info = ConfigManager.get().locatorHeads ? playerFor(waypoint) : null;

		if (info == null) {
			original.call(graphics, pipeline, sprite, x, y, width, height, colour);
			return;
		}

		PlayerFaceRenderer.draw(graphics, info.getSkin().body().texturePath(),
				x + FACE_INSET, y + FACE_INSET, FACE_SIZE, info.showHat(), false, -1);
	}

	/** The tracked player behind this waypoint, or null when it is not a player. */
	private static PlayerInfo playerFor(TrackedWaypoint waypoint) {
		Optional<UUID> id = waypoint.id().left();
		if (id.isEmpty()) {
			return null;
		}

		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		return connection == null ? null : connection.getPlayerInfo(id.get());
	}
}
