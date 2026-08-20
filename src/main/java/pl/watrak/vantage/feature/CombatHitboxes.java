package pl.watrak.vantage.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import pl.watrak.vantage.config.ConfigManager;

//? if >=1.21.11 {
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
//?} else {
/*import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShapeRenderer;
*///?}

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
 *
 * <p>Which players get a box is settled once, in {@link #forEachTarget}; only the
 * drawing differs between versions, and the two ways of doing it are far enough
 * apart to be worth writing out separately.
 */
public final class CombatHitboxes {

	private CombatHitboxes() {
	}

	//? if >=1.21.11 {
	/**
	 * Hands each box to the collector that is open for this frame.
	 *
	 * <p>Thickness is the stroke width the collector already understands, so
	 * there is nothing to approximate.
	 */
	public static void emit(Frustum frustum, float partialTick) {
		forEachTarget(frustum, partialTick, (box, argb) ->
				Gizmos.cuboid(box, GizmoStyle.stroke(argb, ConfigManager.get().hitboxThickness)));
	}
	//?} else {
	/*// Draws the boxes into the frame's line batch, which is what these versions
	// offer in place of a collector.
	//
	// Two things follow from that. The pose stack sits at the camera, so every
	// box has to be brought back to it. And the line width belongs to the render
	// type, shared with everything else in the same batch, so setting it here
	// would thicken vanilla's lines too — thickness is nested boxes instead.
	public static void emit(PoseStack poseStack, MultiBufferSource.BufferSource buffers,
	                        Frustum frustum, double camX, double camY, double camZ, boolean translucent) {
		// From 1.21.9 the debug shapes are drawn in two passes and this runs
		// once for each. Lines belong in the opaque one, and without this check
		// every box would be drawn a second time over the top of itself.
		if (translucent) {
			return;
		}

		// The call site has no partial tick to hand over, so it is asked for.
		float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
		VertexConsumer lines = buffers.getBuffer(RenderType.lines());

		forEachTarget(frustum, partialTick, (box, argb) -> {
			float a = (argb >> 24 & 0xFF) / 255.0F;
			float r = (argb >> 16 & 0xFF) / 255.0F;
			float g = (argb >> 8 & 0xFF) / 255.0F;
			float b = (argb & 0xFF) / 255.0F;

			AABB local = box.move(-camX, -camY, -camZ);
			for (int step = 0; step < ConfigManager.get().hitboxThickness; step++) {
				ShapeRenderer.renderLineBox(poseStack.last(), lines, local.inflate(step * 0.004), r, g, b, a);
			}
		});
	}
	*///?}

	/** Walks the players worth drawing and works out the colour for each. */
	private static void forEachTarget(Frustum frustum, float partialTick, Outline outline) {
		if (!ConfigManager.get().combatHitboxes) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer self = minecraft.player;
		if (self == null || minecraft.level == null) {
			return;
		}

		for (Entity entity : minecraft.level.entitiesForRendering()) {
			if (!(entity instanceof Player player) || !shouldDraw(self, player, frustum)) {
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

			outline.draw(box, 0xFF000000 | rgb);
		}
	}

	private static boolean shouldDraw(LocalPlayer self, Player player, Frustum frustum) {
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

	/** What to do with one box, once the shared code has settled on it. */
	private interface Outline {
		void draw(AABB box, int argb);
	}

}
