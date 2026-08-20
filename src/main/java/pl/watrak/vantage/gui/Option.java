package pl.watrak.vantage.gui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * One row in the settings screen, described rather than drawn.
 *
 * <p>Options bind straight to fields on the config object through the supplied
 * accessors, so the screen never has to know what it is editing and adding a
 * feature means adding one line to {@link OptionRegistry} instead of touching
 * layout code.
 *
 * <p>Most options take their label from {@code vantage.option.<id>}. The
 * constructors that accept an explicit label exist for rows generated at
 * runtime, such as the per-item scale sliders, where the text is an item name
 * rather than a fixed translation key.
 */
public sealed interface Option {

	/** Identifier used to build translation keys, not shown to the user. */
	String id();

	/** Text shown before the colon on the widget. */
	Component label();

	/** Tooltip key; the screen skips the tooltip when no translation exists. */
	default String tooltipKey() {
		return "vantage.option." + id() + ".tooltip";
	}

	static Component defaultLabel(String id) {
		return Component.translatable("vantage.option." + id);
	}

	/** An on/off setting, rendered as a button reading "Name: ON". */
	record Toggle(String id, Component label, BooleanSupplier getter, Consumer<Boolean> setter) implements Option {

		public Toggle(String id, BooleanSupplier getter, Consumer<Boolean> setter) {
			this(id, defaultLabel(id), getter, setter);
		}
	}

	/**
	 * A whole-number setting, rendered as a slider reading "Name: 15".
	 *
	 * @param step the granularity the slider snaps to. A slider is only about
	 *             150 pixels wide, so over a wide range a single pixel covers
	 *             several units and specific round values become impossible to
	 *             land on by dragging. Snapping makes them reachable.
	 */
	record Slider(String id, Component label, int min, int max, int step,
	              IntSupplier getter, IntConsumer setter, Format format) implements Option {

		public Slider(String id, int min, int max, IntSupplier getter, IntConsumer setter) {
			this(id, defaultLabel(id), min, max, 1, getter, setter, String::valueOf);
		}

		public Slider(String id, int min, int max, IntSupplier getter, IntConsumer setter, Format format) {
			this(id, defaultLabel(id), min, max, 1, getter, setter, format);
		}

		/** Turns the raw value into the text shown after the colon. */
		@FunctionalInterface
		public interface Format {
			String apply(int value);
		}
	}

	/**
	 * A setting that steps through a fixed list, rendered as a button reading
	 * "Name: Top left".
	 *
	 * <p>Indexed rather than generic so an enum setting costs one line at the
	 * call site and the screen never has to know what it is cycling.
	 */
	record Choice(String id, Component label, List<Component> choices,
	              IntSupplier getter, IntConsumer setter) implements Option {

		public Choice(String id, List<Component> choices, IntSupplier getter, IntConsumer setter) {
			this(id, defaultLabel(id), choices, getter, setter);
		}

		public Component selected() {
			int index = Math.clamp(getter.getAsInt(), 0, choices.size() - 1);
			return choices.get(index);
		}

		public void advance() {
			setter.accept((getter.getAsInt() + 1) % choices.size());
		}
	}

	/**
	 * An on/off setting that has more to it, shown as a toggle with a settings icon
	 * beside it.
	 *
	 * <p>Keeping the switch and its settings on one row says plainly that they
	 * belong together, and costs a row rather than two — which matters on a
	 * screen that has to stay one page.
	 */
	record Tunable(String id, Component label, BooleanSupplier getter, Consumer<Boolean> setter,
	               Function<Screen, Screen> factory) implements Option {

		public Tunable(String id, BooleanSupplier getter, Consumer<Boolean> setter,
		               Function<Screen, Screen> factory) {
			this(id, defaultLabel(id), getter, setter, factory);
		}

		/** The plain switch, for the widget that draws the left-hand part. */
		public Toggle asToggle() {
			return new Toggle(id, label, getter, setter);
		}
	}

	/** A button that opens another screen, for settings too big for one row. */
	record Link(String id, Component label, Function<Screen, Screen> factory) implements Option {

		public Link(String id, Function<Screen, Screen> factory) {
			this(id, defaultLabel(id), factory);
		}
	}
}
