package pl.watrak.vantage.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import pl.watrak.vantage.config.ConfigManager;
import pl.watrak.vantage.feature.FeatureKeybinds;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Assigns a key to any on/off setting, so it can be flipped mid-fight.
 *
 * <p>Laid out like the sound and item lists because it answers the same problem:
 * far more entries than fit on a screen, most of which the player will never
 * touch. The search box is the way in, and an empty box shows what is already
 * bound so the screen opens on what there is to undo.
 */
public final class FeatureKeybindScreen extends Screen {

	private static final int ROW_HEIGHT = 20;
	private static final int ROW_GAP = 5;
	private static final int COLUMN_WIDTH = 310;
	private static final int KEY_WIDTH = 90;
	private static final int SEARCH_Y = 28;
	private static final int FIRST_ROW_Y = 58;

	private static final int MIN_ROWS = 3;
	private static final int MAX_ROWS = 9;

	private final Screen parent;
	private final List<AbstractWidget> rows = new ArrayList<>();

	private EditBox search;
	private Button previous;
	private Button next;

	private String query = "";
	private List<Option.Toggle> results = List.of();
	private int page;

	/** The setting waiting for a key, or null when nothing is listening. */
	private String listening;

	public FeatureKeybindScreen(Screen parent) {
		super(Component.translatable("vantage.screen.feature_keys"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		search = new EditBox(this.font, this.width / 2 - 155, SEARCH_Y, COLUMN_WIDTH, ROW_HEIGHT,
				Component.translatable("vantage.search"));
		search.setMaxLength(64);
		search.setHint(Component.translatable("vantage.search.feature_hint"));
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
			ConfigManager.get().featureKeys.clear();
			ConfigManager.save();
			listening = null;
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
		listening = null;
		refreshResults();
		rebuildRows();
	}

	private void refreshResults() {
		String needle = query.trim().toLowerCase(Locale.ROOT);
		List<Option.Toggle> all = FeatureKeybinds.bindable();

		if (needle.isEmpty()) {
			results = all.stream()
					.filter(toggle -> FeatureKeybinds.keyFor(toggle.id()) >= 0)
					.toList();
			return;
		}

		results = all.stream()
				.filter(toggle -> toggle.label().getString().toLowerCase(Locale.ROOT).contains(needle)
						|| toggle.id().contains(needle))
				.toList();
	}

	/** As many rows as fit between the search box and the buttons. */
	private int rowsPerPage() {
		int available = (this.height - 52 - ROW_GAP) - FIRST_ROW_Y;
		return Math.clamp(available / (ROW_HEIGHT + ROW_GAP), MIN_ROWS, MAX_ROWS);
	}

	private void rebuildRows() {
		rows.forEach(this::removeWidget);
		rows.clear();

		int perPage = rowsPerPage();
		int pages = Math.max(1, (results.size() + perPage - 1) / perPage);
		page = Math.clamp(page, 0, pages - 1);

		int first = page * perPage;
		int last = Math.min(results.size(), first + perPage);

		for (int i = first; i < last; i++) {
			Option.Toggle toggle = results.get(i);
			int y = FIRST_ROW_Y + (i - first) * (ROW_HEIGHT + ROW_GAP);

			int rowWidth = COLUMN_WIDTH - VantageWidgets.REMOVE_SPACE;
			int x = this.width / 2 - 155;

			rows.add(addRenderableWidget(Button.builder(rowLabel(toggle), b -> {
				listening = toggle.id();
				rebuildRows();
			}).bounds(x, y, rowWidth, ROW_HEIGHT).build()));

			rows.add(addRenderableWidget(VantageWidgets.removeButton(
					x + rowWidth + 2, y, ROW_HEIGHT,
					FeatureKeybinds.keyFor(toggle.id()) >= 0,
					() -> {
						FeatureKeybinds.bind(toggle.id(), InputConstants.UNKNOWN.getValue());
						listening = null;
						refreshResults();
						rebuildRows();
					})));
		}

		previous.active = page > 0;
		next.active = page < pages - 1;
	}

	private Component rowLabel(Option.Toggle toggle) {
		if (toggle.id().equals(listening)) {
			return Component.translatable("vantage.keybind.listening", toggle.label());
		}
		return Component.translatable("vantage.keybind.row", toggle.label(),
				FeatureKeybinds.keyName(FeatureKeybinds.keyFor(toggle.id())));
	}

	/**
	 * While a row is listening, the next key belongs to it rather than to the
	 * screen — including Escape, which clears the binding instead of closing.
	 */
	@Override
	public boolean keyPressed(KeyEvent event) {
		if (listening == null) {
			return super.keyPressed(event);
		}

		FeatureKeybinds.bind(listening,
				event.key() == InputConstants.KEY_ESCAPE ? InputConstants.UNKNOWN.getValue() : event.key());
		listening = null;
		refreshResults();
		rebuildRows();
		return true;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);

		if (results.isEmpty()) {
			Component message = query.trim().isEmpty()
					? Component.translatable("vantage.feature_keys.empty")
					: Component.translatable("vantage.feature_keys.no_results");
			graphics.drawCenteredString(this.font, message, this.width / 2, FIRST_ROW_Y + 8, 0xA0A0A0);
		}
	}

	@Override
	public void onClose() {
		ConfigManager.save();
		this.minecraft.setScreen(parent);
	}

}
