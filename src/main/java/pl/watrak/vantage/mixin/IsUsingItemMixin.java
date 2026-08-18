package pl.watrak.vantage.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.IsUsingItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pl.watrak.vantage.feature.ShieldFixes;

/**
 * Picks the blocking shield model for other players.
 *
 * <p>Vanilla decides with {@code isUsingItem() && getUseItem() == stack}. The
 * second half compares object identity against a stack the server never told the
 * client about, so for anyone but yourself it is false and the shield stays
 * lowered. Answering from the reconstructed state fixes the model without
 * touching how your own shield behaves.
 */
@Mixin(IsUsingItem.class)
public abstract class IsUsingItemMixin {

	@ModifyReturnValue(method = "get", at = @At("RETURN"))
	private boolean vantage$blockingForOtherPlayers(boolean original, ItemStack stack,
	                                                @Nullable ClientLevel level,
	                                                @Nullable LivingEntity owner, int seed,
	                                                ItemDisplayContext context) {
		if (original || !(owner instanceof Player player) || player == Minecraft.getInstance().player) {
			return original;
		}
		if (!(stack.getItem() instanceof ShieldItem)) {
			return original;
		}

		return ShieldFixes.isBlockingWithShield(player);
	}
}
