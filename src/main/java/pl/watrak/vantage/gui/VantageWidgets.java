package pl.watrak.vantage.gui;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.RenderPipelines;
import pl.watrak.vantage.VantageClient;

/** Turns an {@link Option} into the widget that edits it. */
public final class VantageWidgets {

	private VantageWidgets() {
	}

	/**
	 * Sliders rather than a gear: at this size a gear turns to mush, while three
	 * bars with knobs stay readable down to the last pixel. Drawn at its native
	 * size so every pixel of the source lands on exactly one pixel of the screen.
	 */
	private static final Identifier SETTINGS = VantageClient.id("textures/gui/settings.png");
	private static final int SETTINGS_TEXTURE = 20;
	private static final int SETTINGS_ICON = 20;

	/** Present but quiet until pointed at, then as bright as the label. */
	private static final int SETTINGS_IDLE = 0xFFB4B4B4;
	private static final int SETTINGS_HOVER = 0xFFFFFFFF;
	/** How much of a row's right-hand end belongs to the settings icon. */
	private static final int SETTINGS_ZONE = 24;

	/**
	 * A switch with its own settings, as one row rather than two widgets.
	 *
	 * <p>The icon sits inside the frame at the right-hand end: a separate button
	 * beside it read as an unrelated control, when it belongs to the very switch
	 * it stands next to. Clicks landing on that end open the settings, the rest
	 * of the row still toggles.
	 */
	private static Button tunable(Option.Tunable option, Screen parent, int x, int y, int width, int height) {
		Option.Toggle toggle = option.asToggle();

		return new Button(x, y, width, height, toggleText(toggle),
				button -> button.setMessage(toggleText(toggle)),
				supplier -> supplier.get()) {

			//? if >=1.21.9 {
			@Override
			public void onPress(net.minecraft.client.input.InputWithModifiers input) {
				if (input instanceof net.minecraft.client.input.MouseButtonEvent click && overSettings(click.x())) {
					openSettings();
					return;
				}
				flip();
			}
			//?} else {
			/*// The press carries no pointer position on these versions, so the
			// click is caught a step earlier, where it still does.
			@Override
			public void onClick(double mouseX, double mouseY) {
				if (overSettings(mouseX)) {
					openSettings();
					return;
				}
				super.onClick(mouseX, mouseY);
			}

			// Reached by the keyboard as well, which has no icon to aim at.
			@Override
			public void onPress() {
				flip();
			}
			*///?}

			private void openSettings() {
				Minecraft.getInstance().setScreen(option.factory().apply(parent));
			}

			private void flip() {
				toggle.setter().accept(!toggle.getter().getAsBoolean());
				setMessage(toggleText(toggle));
			}

			private boolean overSettings(double mouseX) {
				return mouseX >= getX() + getWidth() - SETTINGS_ZONE;
			}

			//? if >=1.21.11 {
			@Override
			protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
				// Overriding this replaces everything the button draws, frame
				// included, so the frame has to be asked for explicitly.
				renderDefaultSprite(graphics);
				drawLabel(graphics, 0xFFFFFFFF);
				drawSettings(graphics, mouseX, mouseY);
			}
			//?} else {
			/*// Here the frame and the label are one method, and it is the label
			// alone that needs moving, so only that half is taken over.
			@Override
			protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
				super.renderWidget(graphics, mouseX, mouseY, partialTick);
				drawSettings(graphics, mouseX, mouseY);
			}

			@Override
			public void renderString(GuiGraphics graphics, net.minecraft.client.gui.Font font, int colour) {
				drawLabel(graphics, colour);
			}
			*///?}

			/**
			 * The label is centred on the part of the row that is still the
			 * switch, so it never slides under the icon.
			 */
			private void drawLabel(GuiGraphics graphics, int colour) {
				int labelWidth = getWidth() - SETTINGS_ZONE;
				graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(),
						getX() + labelWidth / 2, getY() + (getHeight() - 8) / 2, colour);
			}

			private void drawSettings(GuiGraphics graphics, int mouseX, int mouseY) {
				int edge = getX() + getWidth() - SETTINGS_ZONE;
				boolean pointed = mouseX >= edge && mouseX < getX() + getWidth()
						&& mouseY >= getY() && mouseY < getY() + getHeight();

				// Drawn smaller than the texture and tinted on the way, so the
				// same white image serves both states.
				graphics.blit(RenderPipelines.GUI_TEXTURED, SETTINGS,
						edge + (SETTINGS_ZONE - SETTINGS_ICON) / 2,
						getY() + (getHeight() - SETTINGS_ICON) / 2,
						0.0F, 0.0F,
						SETTINGS_ICON, SETTINGS_ICON,
						SETTINGS_TEXTURE, SETTINGS_TEXTURE,
						SETTINGS_TEXTURE, SETTINGS_TEXTURE,
						pointed ? SETTINGS_HOVER : SETTINGS_IDLE);
			}
		};
	}

	/** Width taken by the remove button beside a row, including its gap. */
	public static final int REMOVE_SPACE = 22;

	/**
	 * The small button that clears one entry from a searchable list.
	 *
	 * <p>Shown on every row rather than only on rows that have something to
	 * clear, so the rows stay the same width and nothing shifts under the
	 * pointer as a list is edited. It is simply inactive when there is nothing
	 * to remove, which also says plainly which entries are actually set.
	 */
	public static Button removeButton(int x, int y, int height, boolean enabled, Runnable action) {
		Button button = Button.builder(Component.literal("✖"), b -> action.run())
				.bounds(x, y, REMOVE_SPACE - 2, height)
				.tooltip(Tooltip.create(Component.translatable("vantage.list.remove")))
				.build();
		button.active = enabled;
		return button;
	}

	public static AbstractWidget create(Option option, Screen parent, int x, int y, int width, int height) {
		AbstractWidget widget = switch (option) {
			case Option.Toggle toggle -> toggle(toggle, x, y, width, height);
			case Option.Slider slider -> new IntSlider(slider, x, y, width, height);
			case Option.Choice choice -> choice(choice, x, y, width, height);
			case Option.Link link -> link(link, parent, x, y, width, height);
			// Only the switch is built here; the icon beside it is a second
			// widget, added by whichever screen is laying the row out.
			case Option.Tunable tunable -> tunable(tunable, parent, x, y, width, height);
		};

		// Asked of Language rather than I18n: I18n is only a thin wrapper that
		// 26.x trimmed down to get(), while this is what it delegated to and is
		// present on every version the mod targets.
		String key = option.tooltipKey();
		if (Language.getInstance().has(key)) {
			widget.setTooltip(Tooltip.create(Component.translatable(key)));
		}
		return widget;
	}

	private static Button toggle(Option.Toggle option, int x, int y, int width, int height) {
		return Button.builder(toggleText(option), button -> {
					option.setter().accept(!option.getter().getAsBoolean());
					button.setMessage(toggleText(option));
				})
				.bounds(x, y, width, height)
				.build();
	}

	private static Component toggleText(Option.Toggle option) {
		Component state = option.getter().getAsBoolean()
				? CommonComponents.OPTION_ON
				: CommonComponents.OPTION_OFF;
		return Component.empty().append(option.label()).append(": ").append(state);
	}

	private static Button choice(Option.Choice option, int x, int y, int width, int height) {
		return Button.builder(choiceText(option), button -> {
					option.advance();
					button.setMessage(choiceText(option));
				})
				.bounds(x, y, width, height)
				.build();
	}

	private static Component choiceText(Option.Choice option) {
		return Component.empty().append(option.label()).append(": ").append(option.selected());
	}

	private static Button link(Option.Link option, Screen parent, int x, int y, int width, int height) {
		return Button.builder(Component.empty().append(option.label()).append("..."),
						button -> net.minecraft.client.Minecraft.getInstance()
								.setScreen(option.factory().apply(parent)))
				.bounds(x, y, width, height)
				.build();
	}

	/**
	 * Slider over a whole-number range.
	 *
	 * <p>Writes straight into the config as it is dragged; the file is only
	 * flushed when the screen closes, so a drag does not cause a write per pixel.
	 */
	private static final class IntSlider extends AbstractSliderButton {

		private final Option.Slider option;

		IntSlider(Option.Slider option, int x, int y, int width, int height) {
			super(x, y, width, height, Component.empty(), fraction(option, option.getter().getAsInt()));
			this.option = option;
			updateMessage();
		}

		private static double fraction(Option.Slider option, int value) {
			int span = option.max() - option.min();
			return span == 0 ? 0.0 : (double) (value - option.min()) / span;
		}

		private int currentValue() {
			int span = option.max() - option.min();
			int raw = option.min() + (int) Math.round(this.value * span);

			int step = Math.max(1, option.step());
			int snapped = option.min() + Math.round((raw - option.min()) / (float) step) * step;
			return Math.clamp(snapped, option.min(), option.max());
		}

		@Override
		protected void updateMessage() {
			setMessage(Component.empty()
					.append(option.label())
					.append(": ")
					.append(option.format().apply(currentValue())));
		}

		@Override
		protected void applyValue() {
			option.setter().accept(currentValue());
		}
	}
}
