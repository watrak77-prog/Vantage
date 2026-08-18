package pl.watrak.vantage.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Makes an opponent's raised shield actually look raised.
 *
 * <p>The server tells every client *that* a player is using an item, but not
 * *which* item or for how long — those stay on the server. The blocking pose is
 * chosen from exactly that missing information, so on an unmodified client an
 * enemy's shield often never visibly goes up, which in a fight reads as them not
 * blocking at all.
 *
 * <p>Always on: it only fills in information the protocol omits, never invents
 * a block, and a lowered shield on a blocking opponent is simply wrong.
 *
 * <p>The gap is closed by rebuilding the state locally: if a player is flagged
 * as using an item and is holding a shield, then the item they are using is that
 * shield. That inference is safe because the flag is authoritative — this only
 * fills in the detail the protocol leaves out, and never invents a block that
 * the server did not report.
 */
public final class ShieldFixes {

	/** Vanilla's own "effectively forever" use duration for a shield. */
	private static final int MAX_USE_TICKS = 72000;

	/** Counted down per player so the block delay elapses as it would server-side. */
	private static final Map<UUID, Integer> useTicks = new HashMap<>();

	private ShieldFixes() {
	}

	public static void tick(ClientLevel level) {
		Player self = Minecraft.getInstance().player;

		for (Player player : level.players()) {
			if (player == self) {
				// Our own use state is real, not inferred; leave it alone.
				continue;
			}

			ItemStack shield = usableShield(player);
			if (shield == null) {
				useTicks.remove(player.getUUID());
				continue;
			}

			int remaining = useTicks.getOrDefault(player.getUUID(), MAX_USE_TICKS);
			((ShieldUseState) player).vantage$setUseItem(shield, remaining);
			useTicks.put(player.getUUID(), Math.max(0, remaining - 1));
		}
	}

	/**
	 * The shield this player is currently raising, or null.
	 *
	 * <p>A shield in the offhand does not count while the main hand is busy with
	 * something else that has its own use animation — eating or drawing a bow —
	 * because then the raised item is that, not the shield.
	 */
	private static ItemStack usableShield(Player player) {
		if (!player.isUsingItem()) {
			return null;
		}

		ItemStack main = player.getMainHandItem();
		if (main.getItem() instanceof ShieldItem) {
			return main;
		}

		if (main.getUseDuration(player) != 0) {
			return null;
		}

		ItemStack offhand = player.getOffhandItem();
		return offhand.getItem() instanceof ShieldItem ? offhand : null;
	}

	/** True when this player should be drawn holding their shield up. */
	public static boolean isBlockingWithShield(Player player) {
		return usableShield(player) != null;
	}

	public static void clear() {
		useTicks.clear();
	}
}
