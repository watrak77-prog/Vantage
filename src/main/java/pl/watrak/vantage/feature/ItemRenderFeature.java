package pl.watrak.vantage.feature;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import pl.watrak.vantage.config.ConfigManager;
import pl.watrak.vantage.config.VantageConfig;

/** Sizing and placement of rendered items. */
public final class ItemRenderFeature {

	private ItemRenderFeature() {
	}

	/**
	 * Vertical shift for the held item, in world units.
	 *
	 * <p>Only shields move, and only when the shield offset is configured away
	 * from zero. The configured value is a percentage, scaled down here so that
	 * the full -100 lands roughly a block below the eye, which is far enough to
	 * clear the crosshair without the shield leaving the screen.
	 */
	public static float verticalOffset(ItemStack stack) {
		VantageConfig config = ConfigManager.get();
		if (config.shieldOffset == 0 || !(stack.getItem() instanceof ShieldItem)) {
			return 0.0F;
		}
		return config.shieldOffset / 100.0F;
	}

	/** Scale for the item held in first person. */
	public static float firstPersonScale(ItemStack stack) {
		return scale(stack, ConfigManager.get().itemScaleFirstPerson);
	}

	/** Scale for an item lying on the ground. */
	public static float droppedScale(ItemStack stack) {
		return scale(stack, ConfigManager.get().itemScaleDropped);
	}

	private static float scale(ItemStack stack, boolean enabled) {
		if (!enabled || stack.isEmpty()) {
			return 1.0F;
		}
		Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		return ItemScaleSettings.resolve(true, ConfigManager.get().itemScales,
				id == null ? null : id.toString());
	}

	/** True when the held item needs the pose stack touched at all. */
	public static boolean isHeldTransformed(ItemStack stack) {
		return verticalOffset(stack) != 0.0F || firstPersonScale(stack) != 1.0F;
	}

	/**
	 * True while this held shield is disabled by an axe hit. Shown by tinting
	 * the shield rather than adding an indicator, because the moment it matters
	 * you are already looking at the shield.
	 */
	public static boolean isShieldDisabled(Player player, ItemStack stack) {
		return ShieldStatusTracker.isDisabledShield(player, stack);
	}
}
