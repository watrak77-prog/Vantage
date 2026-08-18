package pl.watrak.vantage.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import pl.watrak.vantage.config.ConfigManager;
import pl.watrak.vantage.config.VantageConfig;

/**
 * Line width and colours for the player outlines.
 *
 * <p>Two colours rather than one: the whole point of the feature is telling at a
 * glance whether someone is in range, so both states are worth setting.
 *
 * <p>Written as its own screen rather than a list of options because a colour
 * wants to be picked by eye. Three sliders describe a colour exactly and are
 * hopeless to aim with.
 */
public final class CombatHitboxScreen extends Screen {

	private static final int ROW_HEIGHT = 20;
	private static final int PICKER_WIDTH = 150;
	private static final int PICKER_HEIGHT = 60;
	private static final int COLUMN_GAP = 20;

	private final Screen parent;

	public CombatHitboxScreen(Screen parent) {
		super(Component.translatable("vantage.screen.combat_hitboxes"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		VantageConfig config = ConfigManager.get();

		int leftX = this.width / 2 - PICKER_WIDTH - COLUMN_GAP / 2;
		int rightX = this.width / 2 + COLUMN_GAP / 2;
		int pickerY = 60;

		addRenderableWidget(new ColourPicker(leftX, pickerY, PICKER_WIDTH, PICKER_HEIGHT,
				Component.translatable("vantage.hitbox.normal"),
				() -> config.hitboxColour, v -> config.hitboxColour = v));

		addRenderableWidget(new ColourPicker(rightX, pickerY, PICKER_WIDTH, PICKER_HEIGHT,
				Component.translatable("vantage.hitbox.in_reach"),
				() -> config.hitboxReachColour, v -> config.hitboxReachColour = v));

		addRenderableWidget(VantageWidgets.create(
				new Option.Slider("hitbox_thickness", 1, 8,
						() -> config.hitboxThickness, v -> config.hitboxThickness = v),
				this, this.width / 2 - 100, pickerY + PICKER_HEIGHT + 24, 200, ROW_HEIGHT));

		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
				.bounds(this.width / 2 - 100, this.height - 27, 200, ROW_HEIGHT).build());
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFFFF);

		int leftX = this.width / 2 - PICKER_WIDTH - COLUMN_GAP / 2;
		int rightX = this.width / 2 + COLUMN_GAP / 2;

		graphics.drawString(this.font, Component.translatable("vantage.hitbox.normal"),
				leftX, 48, 0xFFFFFFFF);
		graphics.drawString(this.font, Component.translatable("vantage.hitbox.in_reach"),
				rightX, 48, 0xFFFFFFFF);
	}

	@Override
	public void onClose() {
		ConfigManager.save();
		this.minecraft.setScreen(parent);
	}
}
