package pl.watrak.vantage.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
//? if >=26.2 {
/*import net.minecraft.client.gui.Hud;
*///?} else {
import net.minecraft.client.gui.Gui;
//?}
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.watrak.vantage.config.ConfigManager;
import pl.watrak.vantage.feature.FoodOverlay;
import pl.watrak.vantage.hud.StatusEffectHud;

import java.util.Optional;

/**
 * Head-slot camera overlay removal, plus the extra food readouts on the HUD.
 *
 * <p>26.x moved HUD drawing out of Gui into a Hud class of its own and renamed
 * each method from render to extract, but the parameters are unchanged — so only
 * the names below differ between versions.
 */
//? if >=26.2 {
/*@Mixin(Hud.class)
*///?} else {
@Mixin(Gui.class)
//?}
public abstract class GuiMixin {

	/**
	 * Reports every worn helmet as having no camera overlay.
	 *
	 * <p>Since 1.21.11 the pumpkin blur is not special-cased anywhere; it is
	 * just the {@code camera_overlay} field of the carved pumpkin's equippable
	 * component, drawn by a generic loop over the equipment slots. Emptying the
	 * optional is therefore the whole feature, and it covers modded helmets
	 * that obscure the view the same way.
	 */
	@ModifyExpressionValue(
			//? if >=26.1 {
			/*method = "extractCameraOverlays",
			*///?} else {
			method = "renderCameraOverlays",
			//?}
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/item/equipment/Equippable;cameraOverlay()Ljava/util/Optional;"
			)
	)
	private Optional<Identifier> vantage$hideHelmetOverlay(Optional<Identifier> original) {
		return ConfigManager.get().disablePumpkinBlur ? Optional.empty() : original;
	}

	/**
	 * Draws the saturation bar and restore preview over the hunger row.
	 *
	 * <p>Injected on return so it paints on top of vanilla's icons, and given
	 * vanilla's own coordinates so the overlay tracks the hunger bar wherever it
	 * ends up — it moves when the player is riding something with health.
	 */
	@Inject(
			//? if >=26.1 {
			/*method = "extractFood",
			*///?} else {
			method = "renderFood",
			//?}
			at = @At("RETURN"))
	private void vantage$foodOverlay(GuiGraphics guiGraphics, Player player, int y, int right, CallbackInfo ci) {
		FoodOverlay.renderFood(guiGraphics, player, y, right);
	}

	/**
	 * Hands effect rendering over entirely when the timers are on.
	 *
	 * <p>Replacing rather than decorating, because the block also becomes
	 * movable — two renderers both drawing icons would fight over the corner.
	 */
	@Inject(
			//? if >=26.1 {
			/*method = "extractEffects",
			*///?} else {
			method = "renderEffects",
			//?}
			at = @At("HEAD"), cancellable = true)
	private void vantage$effectTimers(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		if (StatusEffectHud.isEnabled()) {
			StatusEffectHud.render(guiGraphics, Minecraft.getInstance());
			ci.cancel();
		}
	}

	/**
	 * Ghosts in the health the held food would let you regenerate.
	 *
	 * <p>Takes vanilla's own heart geometry — left edge, baseline and row
	 * spacing — because the hearts move: extra rows stack upwards and the
	 * spacing tightens once the player has more than ten of them.
	 */
	@Inject(
			//? if >=26.1 {
			/*method = "extractHearts",
			*///?} else {
			method = "renderHearts",
			//?}
			at = @At("RETURN"))
	private void vantage$healthPreview(GuiGraphics guiGraphics, Player player, int left, int y,
	                                   int rowHeight, int regenIndex, float maxHealth, int health,
	                                   int displayHealth, int absorption, boolean blink, CallbackInfo ci) {
		FoodOverlay.renderHealthPreview(guiGraphics, player, left, y, rowHeight, maxHealth, health);
	}
}
