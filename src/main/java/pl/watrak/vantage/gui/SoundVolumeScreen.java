package pl.watrak.vantage.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import pl.watrak.vantage.config.ConfigManager;
import pl.watrak.vantage.config.VantageConfig;
import pl.watrak.vantage.feature.SoundVolumes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Search-driven volume control for individual sounds.
 *
 * <p>An empty box lists the sounds already turned down, so the screen opens on
 * what there is to undo. Typing searches every registered sound id, which is
 * how new ones get added — with well over a thousand of them, a fixed list was
 * never an option.
 */
public final class SoundVolumeScreen extends Screen {

	private static final int ROW_HEIGHT = 20;
	private static final int ROW_GAP = 5;
	/**
	 * One wide column rather than the two the item list uses.
	 *
	 * <p>Item names are two or three words; sound ids are dotted paths of forty
	 * characters that share long prefixes. Splitting them across two 150 pixel
	 * buttons meant every label was cut to an unreadable stub, so the row gets
	 * the full width instead and nothing needs cutting.
	 */
	private static final int COLUMN_WIDTH = 310;
	private static final int SEARCH_Y = 28;
	private static final int FIRST_ROW_Y = 58;
	/** Never more rows than fit above the buttons, whatever the GUI scale. */
	private static final int MIN_ROWS = 3;
	private static final int MAX_ROWS = 9;

	private static final int MAX_RESULTS = 240;

	/** What a full-width row holds once the percentage is appended. */
	private static final int MAX_LABEL_CHARS = 42;

	private final Screen parent;
	private final List<AbstractWidget> rows = new ArrayList<>();

	private EditBox search;
	private Button previous;
	private Button next;

	private String query = "";
	private List<String> results = List.of();
	private int page;

	public SoundVolumeScreen(Screen parent) {
		super(Component.translatable("vantage.screen.sound_volumes"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		search = new EditBox(this.font, this.width / 2 - 155, SEARCH_Y, 310, ROW_HEIGHT,
				Component.translatable("vantage.search"));
		search.setMaxLength(64);
		search.setHint(Component.translatable("vantage.search.sound_hint"));
		search.setValue(query);
		search.setResponder(this::onQueryChanged);
		addRenderableWidget(search);
		setInitialFocus(search);

		int navY = this.height - 52;

		previous = addRenderableWidget(Button.builder(Component.literal("<"), b -> {
			page--;
			rebuildRows();
		}).bounds(this.width / 2 - 155, navY, 20, ROW_HEIGHT).build());

		next = addRenderableWidget(Button.builder(Component.literal(">"), b -> {
			page++;
			rebuildRows();
		}).bounds(this.width / 2 + 135, navY, 20, ROW_HEIGHT).build());

		addRenderableWidget(Button.builder(Component.translatable("vantage.button.reset_all"), b -> {
			ConfigManager.get().soundVolumeLevels.clear();
			refreshResults();
			rebuildRows();
		}).bounds(this.width / 2 - 60, navY, 120, ROW_HEIGHT).build());

		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
				.bounds(this.width / 2 - 100, this.height - 27, 200, ROW_HEIGHT).build());

		refreshResults();
		rebuildRows();
	}

	private void onQueryChanged(String text) {
		if (text.equals(query)) {
			return;
		}
		query = text;
		page = 0;
		refreshResults();
		rebuildRows();
	}

	private void refreshResults() {
		VantageConfig config = ConfigManager.get();
		String needle = query.trim().toLowerCase(Locale.ROOT);

		if (needle.isEmpty()) {
			results = List.copyOf(config.soundVolumeLevels.keySet());
			return;
		}

		List<String> found = new ArrayList<>();
		for (Identifier id : BuiltInRegistries.SOUND_EVENT.keySet()) {
			String text = id.toString();
			if (text.toLowerCase(Locale.ROOT).contains(needle)) {
				found.add(text);
				if (found.size() >= MAX_RESULTS) {
					break;
				}
			}
		}

		// Sounds already turned down sort first, so re-finding one is quick.
		found.sort(Comparator
				.comparing((String id) -> !config.soundVolumeLevels.containsKey(id))
				.thenComparing(id -> id));
		results = List.copyOf(found);
	}

	/**
	 * How many rows fit between the search box and the button row.
	 *
	 * <p>Computed rather than fixed: a fixed count that fits at one GUI scale
	 * runs straight through the buttons at another, which is exactly what a
	 * hard-coded nine did.
	 */
	private int rowsPerPage() {
		int available = (this.height - 52) - FIRST_ROW_Y - ROW_GAP;
		return Math.clamp(available / (ROW_HEIGHT + ROW_GAP), MIN_ROWS, MAX_ROWS);
	}

	private int pageCount() {
		int perPage = rowsPerPage();
		return Math.max(1, (results.size() + perPage - 1) / perPage);
	}

	private void rebuildRows() {
		rows.forEach(this::removeWidget);
		rows.clear();
		page = Math.clamp(page, 0, pageCount() - 1);

		int perPage = rowsPerPage();
		int x = this.width / 2 - COLUMN_WIDTH / 2;
		int from = page * perPage;
		int to = Math.min(results.size(), from + perPage);

		for (int i = from; i < to; i++) {
			String id = results.get(i);
			int y = FIRST_ROW_Y + (i - from) * (ROW_HEIGHT + ROW_GAP);
			int rowWidth = COLUMN_WIDTH - VantageWidgets.REMOVE_SPACE;

			rows.add(addRenderableWidget(
					VantageWidgets.create(volumeOption(id), this, x, y, rowWidth, ROW_HEIGHT)));
			rows.add(addRenderableWidget(VantageWidgets.removeButton(
					x + rowWidth + 2, y, ROW_HEIGHT,
					ConfigManager.get().soundVolumeLevels.containsKey(id),
					() -> {
						ConfigManager.get().soundVolumeLevels.remove(id);
						refreshResults();
						rebuildRows();
					})));
		}

		previous.active = page > 0;
		next.active = page < pageCount() - 1;
	}

	/**
	 * A slider bound to one sound's level.
	 *
	 * <p>Steps of 5 so that 100% — the value meaning "untouched" — can actually
	 * be landed on by dragging, and 0% for silencing something outright.
	 */
	private Option.Slider volumeOption(String soundId) {
		VantageConfig config = ConfigManager.get();

		return new Option.Slider(
				"sound_volume",
				shortName(soundId),
				0, 200, 5,
				() -> config.soundVolumeLevels.getOrDefault(soundId, SoundVolumes.FULL_PERCENT),
				value -> {
					if (SoundVolumes.isFull(value)) {
						config.soundVolumeLevels.remove(soundId);
					} else {
						config.soundVolumeLevels.put(soundId, value);
					}
				},
				value -> value + "%"
		);
	}

	/**
	 * The sound id without its namespace, trimmed only if it is truly enormous.
	 *
	 * <p>Trimming takes from the front, since ids are told apart by their
	 * endings — "entity.zombie.attack_wooden_door" and "...attack_iron_door"
	 * share everything up to the last word.
	 */
	private static Component shortName(String soundId) {
		String text = soundId.startsWith("minecraft:") ? soundId.substring("minecraft:".length()) : soundId;
		return Component.literal(text.length() <= MAX_LABEL_CHARS
				? text
				: "..." + text.substring(text.length() - MAX_LABEL_CHARS));
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);

		if (results.isEmpty()) {
			String key = query.trim().isEmpty() ? "vantage.sound_volumes.empty" : "vantage.sound_volumes.no_results";
			graphics.drawCenteredString(this.font, Component.translatable(key),
					this.width / 2, FIRST_ROW_Y + 20, 0xFFA0A0A0);
		} else {
			// In the empty stretch of the button row, between the back arrow
			// and Reset all. Above the rows it sat under the search box; below
			// them it sat on a slider. This gap is the only place nothing else
			// ever occupies.
			graphics.drawCenteredString(this.font, Component.literal((page + 1) + " / " + pageCount()),
					this.width / 2 - 97, this.height - 46, 0xFFA0A0A0);
		}

		VantageConfig config = ConfigManager.get();
		if (!config.soundVolumes && !config.soundVolumeLevels.isEmpty()) {
			graphics.drawCenteredString(this.font, Component.translatable("vantage.sound_volumes.disabled"),
					this.width / 2, this.height - 66, 0xFFFF6060);
		}
	}

	@Override
	public void onClose() {
		ConfigManager.save();
		this.minecraft.setScreen(this.parent);
	}
}
