package pl.watrak.vantage.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.component.Weapon;
import org.jspecify.annotations.Nullable;
import pl.watrak.vantage.config.ConfigManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks which players currently have a disabled shield.
 *
 * <p>Two sources, because the game only tells the client the whole truth about
 * itself:
 *
 * <ul>
 *   <li>For you, the server sends a cooldown packet, so the item cooldown is
 *       authoritative and exact.
 *   <li>For anyone else, the cooldown never leaves the server. What does reach
 *       every nearby client is the disable sound — vanilla plays it with a null
 *       player, which broadcasts it — so the sound is the signal, and the
 *       duration is reconstructed from the numbers baked into vanilla.
 * </ul>
 *
 * <p>A sound carries a position and nothing else, so attributing it to the right
 * player in a crowd takes more than proximity. Three filters narrow it down, in
 * order of how much they can be trusted.
 */
public final class ShieldStatusTracker {

	/**
	 * How long a shield stays disabled, in ticks.
	 *
	 * <p>Vanilla computes {@code seconds * disableCooldownScale * 20}, where the
	 * seconds come from the attacking weapon and the scale from the shield.
	 * Every axe sets five seconds and the shield's scale is 1.0, so this is a
	 * constant in practice.
	 */
	private static final int DISABLE_TICKS = 100;

	/**
	 * Sound positions survive the trip quantised to an eighth of a block, but
	 * other players' positions are interpolated and slightly behind, so the
	 * match still needs some slack — just not enough to reach a bystander.
	 */
	private static final double MATCH_RADIUS_SQ = 2.25;

	/**
	 * By the time the sound lands the server has already stopped the victim
	 * blocking, so "is blocking" is too late to test. A short memory of who was
	 * blocking recently is what makes the test work at all.
	 */
	private static final int BLOCKING_MEMORY_TICKS = 20;

	/** How long your own swing stays a valid explanation for a disable. */
	private static final int ATTACK_MEMORY_TICKS = 10;

	private static final Map<UUID, Long> disabledUntil = new HashMap<>();
	private static final Map<UUID, Long> lastBlockingTick = new HashMap<>();

	private static @Nullable UUID lastAttacked;
	private static long lastAttackTick;

	private ShieldStatusTracker() {
	}

	// --------------------------------------------------------------- signals

	/** Samples who is blocking, since the answer is gone by the time it matters. */
	public static void tick(ClientLevel level) {
		long now = level.getGameTime();

		for (Player player : level.players()) {
			if (player.isUsingItem() && player.getUseItem().getItem() instanceof ShieldItem) {
				lastBlockingTick.put(player.getUUID(), now);
			}
		}

		lastBlockingTick.values().removeIf(tick -> now - tick > BLOCKING_MEMORY_TICKS * 4L);
		disabledUntil.values().removeIf(until -> until <= now);
	}

	/**
	 * Notes that you swung something shield-breaking at someone.
	 *
	 * <p>Detected by the weapon's own {@code disable_blocking_for_seconds} value
	 * rather than by checking for an axe, so a server's custom weapon counts if
	 * it really can disable a shield — and a decorative one does not.
	 */
	public static void onAttack(Player attacker, Entity target) {
		ItemStack weapon = attacker.getMainHandItem();
		Weapon component = weapon.get(DataComponents.WEAPON);

		if (component != null && component.disableBlockingForSeconds() > 0.0F) {
			lastAttacked = target.getUUID();
			lastAttackTick = attacker.level().getGameTime();
		}
	}

	/**
	 * Attributes a disable sound to a player.
	 *
	 * <p>Candidates must be close to the sound, holding a shield, and have been
	 * blocking a moment ago — the last of which rules out bystanders standing in
	 * the same square. Among those, a player you just hit with a shield-breaking
	 * weapon wins outright; only when your own swing explains nothing does this
	 * fall back to whoever is nearest.
	 */
	public static void onDisableSound(double x, double y, double z) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null) {
			return;
		}

		long now = level.getGameTime();
		List<Player> candidates = new ArrayList<>();

		for (Player player : level.players()) {
			if (player.distanceToSqr(x, y, z) > MATCH_RADIUS_SQ || shieldStack(player).isEmpty()) {
				continue;
			}
			Long blocking = lastBlockingTick.get(player.getUUID());
			if (blocking != null && now - blocking <= BLOCKING_MEMORY_TICKS) {
				candidates.add(player);
			}
		}

		if (candidates.isEmpty()) {
			return;
		}

		Player chosen = null;

		if (lastAttacked != null && now - lastAttackTick <= ATTACK_MEMORY_TICKS) {
			for (Player player : candidates) {
				if (player.getUUID().equals(lastAttacked)) {
					chosen = player;
					break;
				}
			}
		}

		if (chosen == null) {
			double best = Double.MAX_VALUE;
			for (Player player : candidates) {
				double distance = player.distanceToSqr(x, y, z);
				if (distance < best) {
					best = distance;
					chosen = player;
				}
			}
		}

		if (chosen != null) {
			disabledUntil.put(chosen.getUUID(), now + DISABLE_TICKS);
		}
	}

	// --------------------------------------------------------------- queries

	/** True when this entity's shield is currently disabled. */
	public static boolean isDisabled(LivingEntity entity) {
		if (!ConfigManager.get().shieldStatus || !(entity instanceof Player player)) {
			return false;
		}

		ItemStack shield = shieldStack(player);
		if (shield.isEmpty()) {
			return false;
		}

		Minecraft minecraft = Minecraft.getInstance();
		if (player == minecraft.player) {
			// Our own cooldown is synced, so prefer the real thing.
			return player.getCooldowns().isOnCooldown(shield);
		}

		Long until = disabledUntil.get(player.getUUID());
		if (until == null) {
			return false;
		}

		ClientLevel level = minecraft.level;
		if (level == null || level.getGameTime() >= until) {
			disabledUntil.remove(player.getUUID());
			return false;
		}
		return true;
	}

	/** True when this specific stack is the disabled shield of its holder. */
	public static boolean isDisabledShield(LivingEntity entity, ItemStack stack) {
		return stack.getItem() instanceof ShieldItem && isDisabled(entity);
	}

	private static ItemStack shieldStack(Player player) {
		ItemStack offhand = player.getOffhandItem();
		if (offhand.getItem() instanceof ShieldItem) {
			return offhand;
		}
		ItemStack main = player.getMainHandItem();
		return main.getItem() instanceof ShieldItem ? main : ItemStack.EMPTY;
	}

	/** Dropped on disconnect so stale state cannot follow you to another server. */
	public static void clear() {
		disabledUntil.clear();
		lastBlockingTick.clear();
		lastAttacked = null;
	}
}
