package pl.watrak.vantage.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * A colour chosen by eye rather than by three numbers.
 *
 * <p>Red, green and blue sliders describe a colour precisely and are miserable
 * to steer: reaching a particular shade means guessing at three values that only
 * mean something together. This is the arrangement everyone already knows — a
 * shade square for the current hue, with a hue strip beside it.
 *
 * <p>The square is filled a column at a time and then darkened with one gradient
 * over the top, which is a hundred or so draws rather than one per pixel.
 */
public final class ColourPicker extends AbstractWidget {

	private static final int HUE_WIDTH = 12;
	private static final int GAP = 4;
	private static final int BORDER = 0xFF000000;
	private static final int FRAME = 0xFF6E6E6E;

	private final IntSupplier getter;
	private final IntConsumer setter;

	private float hue;
	private float saturation;
	private float value;
	private int lastSeen = -1;

	public ColourPicker(int x, int y, int width, int height, Component label,
	                    IntSupplier getter, IntConsumer setter) {
		super(x, y, width, height, label);
		this.getter = getter;
		this.setter = setter;
		adopt(getter.getAsInt());
	}

	/** Takes the stored colour apart, unless it is one we just wrote out. */
	private void adopt(int rgb) {
		float[] hsv = toHsv(rgb);
		hue = hsv[0];
		saturation = hsv[1];
		value = hsv[2];
		lastSeen = rgb;
	}

	private int squareWidth() {
		return width - HUE_WIDTH - GAP;
	}

	@Override
	protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		int stored = getter.getAsInt();
		if (stored != lastSeen) {
			adopt(stored);
		}

		int sw = squareWidth();

		graphics.fill(getX() - 1, getY() - 1, getX() + sw + 1, getY() + height + 1, FRAME);
		for (int i = 0; i < sw; i++) {
			int column = fromHsv(hue, (float) i / Math.max(1, sw - 1), 1.0F);
			graphics.fill(getX() + i, getY(), getX() + i + 1, getY() + height, column);
		}
		// One pass of black over the top turns the saturation ramp into a square.
		graphics.fillGradient(getX(), getY(), getX() + sw, getY() + height, 0x00000000, 0xFF000000);

		int hueX = getX() + sw + GAP;
		graphics.fill(hueX - 1, getY() - 1, hueX + HUE_WIDTH + 1, getY() + height + 1, FRAME);
		for (int i = 0; i < height; i++) {
			int band = fromHsv((float) i / Math.max(1, height - 1), 1.0F, 1.0F);
			graphics.fill(hueX, getY() + i, hueX + HUE_WIDTH, getY() + i + 1, band);
		}

		markerAt(graphics, getX() + Math.round(saturation * (sw - 1)),
				getY() + Math.round((1.0F - value) * (height - 1)));
		markerAt(graphics, hueX + HUE_WIDTH / 2, getY() + Math.round(hue * (height - 1)));
	}

	/** A small cross-hair that stays visible over any colour underneath. */
	private void markerAt(GuiGraphics graphics, int x, int y) {
		graphics.fill(x - 3, y, x + 4, y + 1, BORDER);
		graphics.fill(x, y - 3, x + 1, y + 4, BORDER);
		graphics.fill(x - 2, y, x + 3, y + 1, 0xFFFFFFFF);
		graphics.fill(x, y - 2, x + 1, y + 3, 0xFFFFFFFF);
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubled) {
		apply(event.x(), event.y());
	}

	@Override
	protected void onDrag(MouseButtonEvent event, double dragX, double dragY) {
		apply(event.x(), event.y());
	}

	private void apply(double mouseX, double mouseY) {
		int sw = squareWidth();
		float down = Mth.clamp((float) (mouseY - getY()) / Math.max(1, height - 1), 0.0F, 1.0F);

		if (mouseX < getX() + sw) {
			saturation = Mth.clamp((float) (mouseX - getX()) / Math.max(1, sw - 1), 0.0F, 1.0F);
			value = 1.0F - down;
		} else {
			hue = down;
		}

		lastSeen = fromHsv(hue, saturation, value) & 0xFFFFFF;
		setter.accept(lastSeen);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, getMessage());
	}

	// ---------------------------------------------------------------- colour

	private static float[] toHsv(int rgb) {
		float r = ((rgb >> 16) & 0xFF) / 255.0F;
		float g = ((rgb >> 8) & 0xFF) / 255.0F;
		float b = (rgb & 0xFF) / 255.0F;

		float max = Math.max(r, Math.max(g, b));
		float min = Math.min(r, Math.min(g, b));
		float span = max - min;

		float h = 0.0F;
		if (span > 0.0F) {
			if (max == r) {
				h = ((g - b) / span) % 6.0F;
			} else if (max == g) {
				h = (b - r) / span + 2.0F;
			} else {
				h = (r - g) / span + 4.0F;
			}
			h /= 6.0F;
			if (h < 0.0F) {
				h += 1.0F;
			}
		}
		return new float[]{h, max == 0.0F ? 0.0F : span / max, max};
	}

	private static int fromHsv(float h, float s, float v) {
		int sector = (int) (h * 6.0F) % 6;
		float f = h * 6.0F - (float) Math.floor(h * 6.0F);

		int p = Math.round(v * (1 - s) * 255);
		int q = Math.round(v * (1 - f * s) * 255);
		int t = Math.round(v * (1 - (1 - f) * s) * 255);
		int w = Math.round(v * 255);

		return switch (sector) {
			case 0 -> 0xFF000000 | (w << 16) | (t << 8) | p;
			case 1 -> 0xFF000000 | (q << 16) | (w << 8) | p;
			case 2 -> 0xFF000000 | (p << 16) | (w << 8) | t;
			case 3 -> 0xFF000000 | (p << 16) | (q << 8) | w;
			case 4 -> 0xFF000000 | (t << 16) | (p << 8) | w;
			default -> 0xFF000000 | (w << 16) | (p << 8) | q;
		};
	}
}
