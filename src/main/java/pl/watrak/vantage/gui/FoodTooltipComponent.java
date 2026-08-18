package pl.watrak.vantage.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.food.FoodProperties;

/**
 * Draws a food item's nutrition and saturation as icons instead of words.
 *
 * <p>Two rows: drumsticks for hunger, the same drumsticks tinted gold for
 * saturation, matching how both read on the HUD. Icons overlap at half width so
 * a filling meal does not stretch the tooltip across the screen.
 */
public final class FoodTooltipComponent implements ClientTooltipComponent {

	private static final Identifier FOOD_FULL = Identifier.withDefaultNamespace("hud/food_full");
	private static final Identifier FOOD_HALF = Identifier.withDefaultNamespace("hud/food_half");

	private static final int ICON_SIZE = 9;
	private static final int ICON_STEP = 5;
	private static final int ROW_HEIGHT = 10;

	/** Beyond this the tooltip stops growing and a count is shown instead. */
	private static final int MAX_ICONS = 10;

	private static final int SATURATION_TINT = 0xFFFFD24A;

	private final FoodProperties food;

	public FoodTooltipComponent(FoodProperties food) {
		this.food = food;
	}

	/** Half-icons needed for a value measured in half-units. */
	private static int halves(float value) {
		return Math.max(0, Mth.ceil(value));
	}

	private static int iconsFor(float value) {
		return Math.min(MAX_ICONS, Mth.ceil(halves(value) / 2.0F));
	}

	@Override
	public int getHeight(Font font) {
		return food.saturation() > 0.0F ? ROW_HEIGHT * 2 : ROW_HEIGHT;
	}

	@Override
	public int getWidth(Font font) {
		int widest = Math.max(iconsFor(food.nutrition()), iconsFor(food.saturation()));
		return widest * ICON_STEP + (ICON_SIZE - ICON_STEP);
	}

	@Override
	public void renderImage(Font font, int x, int y, int width, int height, GuiGraphics graphics) {
		drawRow(graphics, x, y, food.nutrition(), 0xFFFFFFFF);

		if (food.saturation() > 0.0F) {
			drawRow(graphics, x, y + ROW_HEIGHT, food.saturation(), SATURATION_TINT);
		}
	}

	/**
	 * One row of icons for a value in half-units.
	 *
	 * <p>Drawn right to left within each icon the way the HUD does, so a value
	 * ending on a half shows the half sprite rather than a full one.
	 */
	private void drawRow(GuiGraphics graphics, int x, int y, float value, int tint) {
		int totalHalves = halves(value);
		int icons = iconsFor(value);

		for (int i = 0; i < icons; i++) {
			boolean full = (i + 1) * 2 <= totalHalves;
			Identifier sprite = full ? FOOD_FULL : FOOD_HALF;
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite,
					x + i * ICON_STEP, y, ICON_SIZE, ICON_SIZE, tint);
		}
	}
}
