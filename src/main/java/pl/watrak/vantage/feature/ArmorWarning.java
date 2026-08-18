package pl.watrak.vantage.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import pl.watrak.vantage.VantageClient;
import pl.watrak.vantage.config.ConfigManager;
import pl.watrak.vantage.config.VantageConfig;

/**
 * Warns that a piece of armour is about to break.
 *
 * <p>Two thresholds rather than one, because a percentage alone treats a helmet
 * and a set of netherite boots the same: ten percent of a high-durability piece
 * is still a lot of hits, while twenty points left is twenty points left
 * whatever it started at. Either one firing is enough.
 */
public final class ArmorWarning {

	/**
	 * Built without registering it. The sound manager resolves the file through
	 * {@code sounds.json} by name, so nothing has to enter the sound registry —
	 * which matters for a client-only mod, since an extra registry entry can
	 * trip a server's registry check on join.
	 */
	public static final SoundEvent ARMOR_BREAKING =
			SoundEvent.createVariableRangeEvent(VantageClient.id("armor_breaking"));

	/** How long the icon holds each position, in milliseconds. */
	private static final long SHAKE_STEP_MS = 130L;
	private static final int SHAKE_PIXELS = 1;

	private static final EquipmentSlot[] ARMOR = {
			EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
	};

	/** Damage value seen last tick, per slot, to notice a fresh hit. */
	private static final int[] lastDamage = new int[ARMOR.length];
	private static final boolean[] lastPresent = new boolean[ARMOR.length];

	private ArmorWarning() {
	}

	public static boolean isCritical(ItemStack stack) {
		VantageConfig config = ConfigManager.get();
		if (!config.armorHudWarning || stack.isEmpty() || !stack.isDamageableItem()) {
			return false;
		}

		int damage = stack.getDamageValue();
		int max = stack.getMaxDamage();
		double remaining = 1.0 - (double) damage / max;

		return remaining <= config.armorHudWarningPercent / 100.0;
	}

	/**
	 * Vertical offset for the warning icon, in pixels.
	 *
	 * <p>Stepped on a timer rather than randomised per frame. A per-frame value
	 * changes hundreds of times a second, which reads as a blur rather than a
	 * shake; holding each position for a moment is what makes it legible.
	 *
	 * <p>Not configurable: the useful range turned out to be one pixel either
	 * way, and anything else was either invisible or a flicker.
	 */
	public static int shakeOffset() {
		return switch ((int) ((Util.getMillis() / SHAKE_STEP_MS) % 4L)) {
			case 0 -> -SHAKE_PIXELS;
			case 2 -> SHAKE_PIXELS;
			default -> 0;
		};
	}

	/**
	 * Sounds an alert when a piece already in the danger zone takes another hit.
	 *
	 * <p>Tied to the damage changing rather than to the piece merely being low,
	 * so it fires once per hit instead of droning every tick.
	 */
	public static void tick(LocalPlayer player) {
		VantageConfig config = ConfigManager.get();
		boolean enabled = config.armorHud && config.armorHudWarning;

		for (int i = 0; i < ARMOR.length; i++) {
			ItemStack stack = player.getItemBySlot(ARMOR[i]);
			int damage = stack.isEmpty() ? 0 : stack.getDamageValue();
			boolean present = !stack.isEmpty();

			// A swapped-in piece is not a hit, so only compare like with like.
			boolean sameItem = present && lastPresent[i];
			boolean tookDamage = sameItem && damage != lastDamage[i];

			lastDamage[i] = damage;
			lastPresent[i] = present;

			if (enabled && tookDamage && isCritical(stack)) {
				play();
				return;
			}
		}
	}

	private static void play() {
		Minecraft.getInstance().getSoundManager()
				.play(SimpleSoundInstance.forUI(ARMOR_BREAKING, 1.0F));
	}
}
