package pl.watrak.vantage.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import pl.watrak.vantage.config.ConfigManager;

import java.util.List;

/**
 * A sub-screen holding one flat list of options.
 *
 * <p>Shares the main screen's geometry — two 150-wide columns either side of the
 * centre line — so moving between them does not shift anything under the mouse.
 */
public class OptionListScreen extends Screen {

	protected static final int ROW_HEIGHT = 20;
	protected static final int ROW_GAP = 5;
	protected static final int COLUMN_WIDTH = 150;
	protected static final int COLUMN_GAP = 10;
	protected static final int FIRST_ROW_Y = 40;

	private final Screen parent;

	protected OptionListScreen(Component title, Screen parent) {
		super(title);
		this.parent = parent;
	}

	/** Rebuilt on every layout, so toggles reflect the config as it stands. */
	protected List<Option> options() {
		return List.of();
	}

	@Override
	protected void init() {
		List<Option> options = options();

		int leftX = this.width / 2 - COLUMN_WIDTH - COLUMN_GAP / 2;
		int rightX = this.width / 2 + COLUMN_GAP / 2;

		for (int i = 0; i < options.size(); i++) {
			int x = (i % 2 == 0) ? leftX : rightX;
			int y = FIRST_ROW_Y + (i / 2) * (ROW_HEIGHT + ROW_GAP);
			addRenderableWidget(VantageWidgets.create(options.get(i), this, x, y, COLUMN_WIDTH, ROW_HEIGHT));
		}

		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
				.bounds(this.width / 2 - 100, this.height - 27, 200, ROW_HEIGHT)
				.build());
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFFFF);
	}

	@Override
	public void onClose() {
		ConfigManager.save();
		this.minecraft.setScreen(this.parent);
	}
}
