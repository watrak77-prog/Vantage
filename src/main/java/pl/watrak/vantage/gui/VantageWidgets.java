package pl.watrak.vantage.gui;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
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

	private static final Identifier GEAR = VantageClient.id("textures/gui/gear.png");
	private static final int GEAR_ICON = 12;
	private static final int GEAR_TEXTURE = 16;

	/** Present but quiet until pointed at, then as bright as the label. */
	private static final int GEAR_IDLE = 0xFFB4B4B4;
	private static final int GEAR_HOVER = 0xFFFFFFFF;
	/** How much of a row's right-hand end belongs to the gear. */
	private static final int GEAR_ZONE = 22;

	/**
	 * A switch with its own settings, as one row rather than two widgets.
	 *
	 * <p>The gear sits inside the frame at the right-hand end: a separate button
	 * beside it read as an unrelated control, when it belongs to the very switch
	 * it stands next to. Clicks landing on that end open the settings, the rest
	 * of the row still toggles.
	 */
	private static Button tunable(Option.Tunable option, Screen parent, int x, int y, int width, int height) {
		Option.Toggle toggle = option.asToggle();

		return new Button(x, y, width, height, toggleText(toggle),
				button -> button.setMessage(toggleText(toggle)),
				supplier -> supplier.get()) {

			@Override
			public void onPress(net.minecraft.client.input.InputWithModifiers input) {
				if (input instanceof MouseButtonEvent click && overGear(click.x())) {
					Minecraft.getInstance().setScreen(option.factory().apply(parent));
					return;
				}
				toggle.setter().accept(!toggle.getter().getAsBoolean());
				setMessage(toggleText(toggle));
			}

			private boolean overGear(double mouseX) {
				return mouseX >= getX() + getWidth() - GEAR_ZONE;
			}

			@Override
			protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
				// Overriding this replaces everything the button draws, frame
				// included, so the frame has to be asked for explicitly.
				renderDefaultSprite(graphics);

				// The label is centred on the part of the row that is still the
				// switch, so it never slides under the gear.
				int labelWidth = getWidth() - GEAR_ZONE;
				graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(),
						getX() + labelWidth / 2, getY() + (getHeight() - 8) / 2, 0xFFFFFFFF);

				int edge = getX() + getWidth() - GEAR_ZONE;
				boolean pointed = mouseX >= edge && mouseX < getX() + getWidth()
						&& mouseY >= getY() && mouseY < getY() + getHeight();

				// Drawn smaller than the texture and tinted on the way, so the
				// same white image serves both states.
				graphics.blit(RenderPipelines.GUI_TEXTURED, GEAR,
						edge + (GEAR_ZONE - GEAR_ICON) / 2,
						getY() + (getHeight() - GEAR_ICON) / 2,
						0.0F, 0.0F,
						GEAR_ICON, GEAR_ICON,
						GEAR_TEXTURE, GEAR_TEXTURE,
						GEAR_TEXTURE, GEAR_TEXTURE,
						pointed ? GEAR_HOVER : GEAR_IDLE);
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
			// Only the switch is built here; the gear beside it is a second
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
