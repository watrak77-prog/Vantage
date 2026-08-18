package pl.watrak.vantage.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import pl.watrak.vantage.config.ConfigManager;

import java.util.List;

/**
 * Main settings screen: a row of category tabs over a two-column grid of
 * options, with Done at the bottom.
 *
 * <p>The grid geometry is the vanilla options layout — two 150-wide columns
 * either side of the centre line — so the screen sits at the same scale as
 * Video Settings at any GUI scale.
 */
public final class VantageScreen extends Screen {

	private static final int ROW_HEIGHT = 20;
	private static final int ROW_GAP = 5;
	private static final int COLUMN_WIDTH = 150;
	private static final int COLUMN_GAP = 10;
	private static final int TAB_WIDTH = 92;
	private static final int TAB_GAP = 4;
	private static final int TAB_Y = 28;
	private static final int FIRST_ROW_Y = 58;

	/** Remembered between openings so reopening lands on the last tab used. */
	private static int selectedCategory = 0;

	private final Screen parent;

	public VantageScreen(Screen parent) {
		super(Component.translatable("vantage.screen.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		List<OptionRegistry.Category> categories = OptionRegistry.categories();
		selectedCategory = Math.clamp(selectedCategory, 0, categories.size() - 1);

		addTabs(categories);
		addOptions(categories.get(selectedCategory).options());

		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
				.bounds(this.width / 2 - 100, this.height - 27, 200, ROW_HEIGHT)
				.build());
	}

	private void addTabs(List<OptionRegistry.Category> categories) {
		int count = categories.size();
		int totalWidth = count * TAB_WIDTH + (count - 1) * TAB_GAP;
		int startX = (this.width - totalWidth) / 2;

		for (int i = 0; i < count; i++) {
			OptionRegistry.Category category = categories.get(i);
			int index = i;

			Button tab = Button.builder(Component.translatable(category.titleKey()), button -> {
						selectedCategory = index;
						rebuildWidgets();
					})
					.bounds(startX + i * (TAB_WIDTH + TAB_GAP), TAB_Y, TAB_WIDTH, ROW_HEIGHT)
					.build();

			// The tab you are already on is greyed out, which is how vanilla
			// signals "you are here" without a dedicated selected state.
			tab.active = i != selectedCategory;
			addRenderableWidget(tab);
		}
	}

	private void addOptions(List<Option> options) {
		int leftX = this.width / 2 - COLUMN_WIDTH - COLUMN_GAP / 2;
		int rightX = this.width / 2 + COLUMN_GAP / 2;

		for (int i = 0; i < options.size(); i++) {
			int x = (i % 2 == 0) ? leftX : rightX;
			int y = FIRST_ROW_Y + (i / 2) * (ROW_HEIGHT + ROW_GAP);
			addRenderableWidget(VantageWidgets.create(options.get(i), this, x, y, COLUMN_WIDTH, ROW_HEIGHT));
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
	}

	@Override
	public void onClose() {
		// Widgets edit the config in memory; this is the single flush to disk.
		ConfigManager.save();
		this.minecraft.setScreen(this.parent);
	}
}
