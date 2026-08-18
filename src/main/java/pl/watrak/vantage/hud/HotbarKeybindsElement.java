package pl.watrak.vantage.hud;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import pl.watrak.vantage.compat.ClientCompat;
import pl.watrak.vantage.config.ConfigManager;

/**
 * Labels each hotbar slot with the key that selects it.
 *
 * <p>Useful the moment the slot keys are not 1-9 — remapped hotbars are common
 * in PvP, and counting slots to work out which key to hit is exactly the sort
 * of thing that loses fights.
 *
 * <p>Labels sit in the top-left of each slot, the one corner vanilla leaves
 * free: the stack count goes bottom-right and the durability bar along the
 * bottom edge.
 */
public final class HotbarKeybindsElement implements HudElement {

	private static final String MOUSE_PREFIX = "key.mouse.";

	private static final int SLOTS = 9;
	private static final int SLOT_WIDTH = 20;
	private static final int HOTBAR_WIDTH = 182;
	private static final int HOTBAR_HEIGHT = 22;

	/** The hotbar texture's own frame, which the label must clear. */
	private static final int BORDER = 1;
	/**
	 * Breathing room inside the frame. Enough that the label reads as sitting in
	 * the slot's corner rather than straddling the frame itself.
	 */
	private static final int INSET = 3;

	/** Small enough to leave the item readable underneath. */
	private static final float LABEL_SCALE = 0.75F;
	private static final int LABEL_COLOUR = 0xFFCCCCCC;

	/** Three characters is what a 20 pixel slot holds at this size. */
	private static final int MAX_LABEL_LENGTH = 3;

	@Override
	public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
		if (!ConfigManager.get().hotbarKeybinds) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || ClientCompat.hudHidden(minecraft)) {
			return;
		}

		int left = graphics.guiWidth() / 2 - HOTBAR_WIDTH / 2;
		int top = graphics.guiHeight() - HOTBAR_HEIGHT;

		graphics.pose().pushMatrix();
		graphics.pose().scale(LABEL_SCALE, LABEL_SCALE);

		for (int slot = 0; slot < SLOTS; slot++) {
			String label = label(minecraft.options.keyHotbarSlots[slot]);
			if (label.isEmpty()) {
				continue;
			}

			// Coordinates are divided back out of the scale so the labels land
			// on the slots rather than bunching towards the top-left corner.
			// The inset puts them inside the slot's frame rather than across it.
			int x = Math.round((left + BORDER + INSET + slot * SLOT_WIDTH) / LABEL_SCALE);
			int y = Math.round((top + BORDER + INSET) / LABEL_SCALE);
			graphics.drawString(minecraft.font, label, x, y, LABEL_COLOUR, true);
		}

		graphics.pose().popMatrix();
	}

	/**
	 * A short label for whatever the slot is bound to.
	 *
	 * <p>Mouse buttons are read from the binding's saved id rather than its
	 * display name, because the display name is a localised phrase — "Button 5"
	 * in English, something else elsewhere — and truncating that would leave
	 * every mouse button looking identical.
	 */
	private static String label(KeyMapping key) {
		if (key.isUnbound()) {
			return "";
		}

		String saved = key.saveString();
		if (saved.startsWith(MOUSE_PREFIX)) {
			String button = saved.substring(MOUSE_PREFIX.length());
			return "B" + switch (button) {
				case "left" -> "1";
				case "right" -> "2";
				case "middle" -> "3";
				default -> button;
			};
		}

		String name = key.getTranslatedKeyMessage().getString();
		return name.length() <= MAX_LABEL_LENGTH ? name : name.substring(0, MAX_LABEL_LENGTH);
	}
}
