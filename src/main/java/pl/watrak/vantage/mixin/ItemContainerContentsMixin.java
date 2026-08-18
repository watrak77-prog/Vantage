package pl.watrak.vantage.mixin;

import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.watrak.vantage.config.ConfigManager;
import pl.watrak.vantage.feature.ShulkerPreview;

import java.util.function.Consumer;

/**
 * Silences vanilla's written-out container contents while the grid is showing.
 *
 * <p>Vanilla already lists what is inside a shulker box, which is why this mod
 * adds no list of its own. The grid is meant to replace that list rather than
 * sit under it, so for as long as the preview key is held the text steps aside.
 */
@Mixin(ItemContainerContents.class)
public abstract class ItemContainerContentsMixin {

	@Inject(method = "addToTooltip", at = @At("HEAD"), cancellable = true)
	private void vantage$hideListDuringPreview(Item.TooltipContext context, Consumer<Component> consumer,
	                                           TooltipFlag flag, DataComponentGetter components,
	                                           CallbackInfo ci) {
		if (ConfigManager.get().shulkerTooltip && ShulkerPreview.isPreviewHeld()) {
			ci.cancel();
		}
	}
}
