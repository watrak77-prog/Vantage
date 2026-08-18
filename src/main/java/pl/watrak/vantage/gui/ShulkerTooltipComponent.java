package pl.watrak.vantage.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Draws a shulker box's contents as the grid they sit in.
 *
 * <p>A list of names would take nine lines and still not answer "is my gear in
 * this one" at a glance; the grid does, and it matches the layout you would see
 * on opening the box.
 */
public final class ShulkerTooltipComponent implements ClientTooltipComponent {

	private static final int COLUMNS = 9;
	private static final int SLOT = 18;
	private static final int SLOT_BACKDROP = 0x60000000;
	private static final int ITEM_INSET = 1;

	private final List<ItemStack> items;

	public ShulkerTooltipComponent(List<ItemStack> items) {
		this.items = items;
	}

	private int rows() {
		return Math.max(1, (items.size() + COLUMNS - 1) / COLUMNS);
	}

	@Override
	public int getHeight(Font font) {
		return rows() * SLOT + 2;
	}

	@Override
	public int getWidth(Font font) {
		return Math.min(items.size(), COLUMNS) * SLOT + 2;
	}

	@Override
	public void renderImage(Font font, int x, int y, int width, int height, GuiGraphics graphics) {
		for (int i = 0; i < items.size(); i++) {
			int slotX = x + (i % COLUMNS) * SLOT;
			int slotY = y + (i / COLUMNS) * SLOT;

			// A faint backdrop per slot keeps the grid readable over whatever
			// the tooltip happens to be drawn on top of.
			graphics.fill(slotX, slotY, slotX + SLOT - 2, slotY + SLOT - 2, SLOT_BACKDROP);

			ItemStack stack = items.get(i);
			if (!stack.isEmpty()) {
				graphics.renderItem(stack, slotX + ITEM_INSET, slotY + ITEM_INSET);
				graphics.renderItemDecorations(font, stack, slotX + ITEM_INSET, slotY + ITEM_INSET);
			}
		}
	}
}
