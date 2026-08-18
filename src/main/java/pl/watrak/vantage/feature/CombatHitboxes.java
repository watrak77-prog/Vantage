package pl.watrak.vantage.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import pl.watrak.vantage.config.ConfigManager;

/**
 * Outlines players, and only players, without turning on the debug view.
 *
 * <p>F3+B answers the same question but draws every entity in sight along with
 * eye lines and vehicle boxes, which in a fight is more clutter than help. This
 * draws one box per player and nothing else.
 *
 * <p>The box turns red once the player is inside your attack range, so reach is
 * something you can see rather than judge. The range comes from the game's own
 * check, which reads the interaction-range attribute — so it stays right on
 * servers that change it rather than assuming vanilla's three blocks.
 */
public final class CombatHitboxes {

	private CombatHitboxes() {
	}

	public static void emit(Frustum frustum, float partialTick) {
		if (!ConfigManager.get().combatHitboxes) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer self = minecraft.player;
		if (self == null || minecraft.level == null) {
			return;
		}

		for (Entity entity : minecraft.level.entitiesForRendering()) {
			if (!(entity instanceof Player player) || !shouldDraw(minecraft, self, player, frustum)) {
				continue;
			}

			// Interpolated the way vanilla interpolates its own hitboxes, or the
			// box lags a step behind the model it is meant to sit on.
			Vec3 drift = player.getPosition(partialTick).subtract(player.position());
			AABB box = player.getBoundingBox().move(drift);

			boolean reachable = self.isWithinEntityInteractionRange(player, 0.0);
			int rgb = reachable
					? ConfigManager.get().hitboxReachColour
					: ConfigManager.get().hitboxColour;

			Gizmos.cuboid(box, GizmoStyle.stroke(0xFF000000 | rgb,
					ConfigManager.get().hitboxThickness));
		}
	}

	private static boolean shouldDraw(Minecraft minecraft, LocalPlayer self, Player player, Frustum frustum) {
		// Invisibility is a tactic, not a rendering accident: drawing a box
		// around someone who drank a potion would defeat the point of it, so
		// this follows the game's own debug view in leaving them out.
		if (player.isInvisible()) {
			return false;
		}

		// Never your own: you know where you are, and in third person the box
		// follows the camera around the screen for no benefit.
		if (player == self) {
			return false;
		}

		return frustum.isVisible(player.getBoundingBox());
	}

}
