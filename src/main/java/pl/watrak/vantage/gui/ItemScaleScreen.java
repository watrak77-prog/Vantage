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
import net.minecraft.world.item.Item;
import pl.watrak.vantage.config.ConfigManager;
import pl.watrak.vantage.config.VantageConfig;
import pl.watrak.vantage.feature.ItemScaleSettings;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Search-driven editor for per-item first-person scale.
 *
 * <p>An empty box lists the items that already carry an override, so the screen
 * opens on what there is to edit. Typing searches the whole item registry by
 * name or id, which is how new items get added — there is no fixed list to
 * outgrow, and modded items are reachable too.
 */
public final class ItemScaleScreen extends Screen {

	private static final int ROW_HEIGHT = 20;
	private static final int ROW_GAP = 5;
	private static final int COLUMN_WIDTH = 150;
	private static final int COLUMN_GAP = 10;
	private static final int SEARCH_Y = 28;
	private static final int FIRST_ROW_Y = 58;
	private static final int MIN_ROWS = 3;
	private static final int MAX_ROWS = 6;

	/** Cap on search hits, so a one-letter query cannot build a thousand rows. */
	private static final int MAX_RESULTS = 240;

	private final Screen parent;

	/** Row widgets only; rebuilt on every query or page change. */
	private final List<AbstractWidget> rows = new ArrayList<>();

	private EditBox search;
	private Button previous;
	private Button next;

	/** Held outside the widget so it survives the screen being laid out again. */
	private String query = "";
	private List<String> results = List.of();
	private int page;

	public ItemScaleScreen(Screen parent) {
		super(Component.translatable("vantage.screen.item_scales"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		search = new EditBox(this.font, this.width / 2 - 155, SEARCH_Y, 310, ROW_HEIGHT,
				Component.translatable("vantage.search"));
		search.setMaxLength(64);
		search.setHint(Component.translatable("vantage.search.hint"));
		// Set the text before attaching the listener, so restoring the query
		// after a resize does not look like the user typing.
		search.setValue(query);
		search.setResponder(this::onQueryChanged);
		addRenderableWidget(search);
		setInitialFocus(search);

		int navY = this.height - 52;

		previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> {
			page--;
			rebuildRows();
		}).bounds(this.width / 2 - 155, navY, 20, ROW_HEIGHT).build());

		next = addRenderableWidget(Button.builder(Component.literal(">"), button -> {
			page++;
			rebuildRows();
		}).bounds(this.width / 2 + 135, navY, 20, ROW_HEIGHT).build());

		addRenderableWidget(Button.builder(Component.translatable("vantage.button.reset_all"), button -> {
			ConfigManager.get().itemScales.clear();
			refreshResults();
			rebuildRows();
		}).bounds(this.width / 2 - 60, navY, 120, ROW_HEIGHT).build());

		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
				.bounds(this.width / 2 - 100, this.height - 27, 200, ROW_HEIGHT)
				.build());

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
		// Only the rows are replaced, so the search box keeps its focus and
		// caret position while the user is still typing.
		rebuildRows();
	}

	private void refreshResults() {
		VantageConfig config = ConfigManager.get();
		String needle = query.trim().toLowerCase(Locale.ROOT);

		if (needle.isEmpty()) {
			results = List.copyOf(config.itemScales.keySet());
			return;
		}

		List<String> found = new ArrayList<>();
		for (Item item : BuiltInRegistries.ITEM) {
			Identifier id = BuiltInRegistries.ITEM.getKey(item);
			if (id == null) {
				continue;
			}

			String idText = id.toString();
			if (idText.toLowerCase(Locale.ROOT).contains(needle)
					|| displayName(item).getString().toLowerCase(Locale.ROOT).contains(needle)) {
				found.add(idText);
				if (found.size() >= MAX_RESULTS) {
					break;
				}
			}
		}

		// Items already carrying an override sort first, so re-finding something
		// you set earlier does not mean scrolling past every near-match.
		found.sort(Comparator
				.comparing((String id) -> !config.itemScales.containsKey(id))
				.thenComparing(id -> id));
		results = List.copyOf(found);
	}

	/** Rows that fit above the button row; two entries per row. */
	private int rowsPerPage() {
		int available = (this.height - 52) - FIRST_ROW_Y - ROW_GAP;
		return Math.clamp(available / (ROW_HEIGHT + ROW_GAP), MIN_ROWS, MAX_ROWS);
	}

	private int perPage() {
		return rowsPerPage() * 2;
	}

	private int pageCount() {
		return Math.max(1, (results.size() + perPage() - 1) / perPage());
	}

	private void rebuildRows() {
		rows.forEach(this::removeWidget);
		rows.clear();

		page = Math.clamp(page, 0, pageCount() - 1);

		int leftX = this.width / 2 - COLUMN_WIDTH - COLUMN_GAP / 2;
		int rightX = this.width / 2 + COLUMN_GAP / 2;
		int from = page * perPage();
		int to = Math.min(results.size(), from + perPage());

		for (int i = from; i < to; i++) {
			int slot = i - from;
			int x = (slot % 2 == 0) ? leftX : rightX;
			int y = FIRST_ROW_Y + (slot / 2) * (ROW_HEIGHT + ROW_GAP);
			int rowWidth = COLUMN_WIDTH - VantageWidgets.REMOVE_SPACE;
			String id = results.get(i);

			rows.add(addRenderableWidget(
					VantageWidgets.create(scaleOption(id), this, x, y, rowWidth, ROW_HEIGHT)));
			rows.add(addRenderableWidget(VantageWidgets.removeButton(
					x + rowWidth + 2, y, ROW_HEIGHT,
					ConfigManager.get().itemScales.containsKey(id),
					() -> {
						ConfigManager.get().itemScales.remove(id);
						refreshResults();
						rebuildRows();
					})));
		}

		previous.active = page > 0;
		next.active = page < pageCount() - 1;
	}

	/**
	 * Builds a slider bound to one entry of the item-scale map.
	 *
	 * <p>Steps of 5 so that 100% — the value that means "leave it alone" — can
	 * actually be landed on by dragging; at single-unit granularity a 150 pixel
	 * slider cannot reliably hit any specific number across this range.
	 */
	private Option.Slider scaleOption(String itemId) {
		VantageConfig config = ConfigManager.get();

		return new Option.Slider(
				"item_scale",
				displayName(itemId),
				25, 300, 5,
				() -> config.itemScales.getOrDefault(itemId, ItemScaleSettings.NEUTRAL_PERCENT),
				value -> {
					if (ItemScaleSettings.isNeutral(value)) {
						config.itemScales.remove(itemId);
					} else {
						config.itemScales.put(itemId, value);
					}
				},
				value -> value + "%"
		);
	}

	private static Component displayName(Item item) {
		return Component.translatable(item.getDescriptionId());
	}

	/** Localised item name, falling back to the raw id for unknown items. */
	private static Component displayName(String itemId) {
		return BuiltInRegistries.ITEM.getOptional(Identifier.parse(itemId))
				.<Component>map(ItemScaleScreen::displayName)
				.orElseGet(() -> Component.literal(itemId));
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);

		if (results.isEmpty()) {
			String key = query.trim().isEmpty() ? "vantage.item_scales.empty" : "vantage.item_scales.no_results";
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

		// Overrides that silently do nothing are confusing, so say so rather
		// than letting the numbers imply an effect they are not having.
		VantageConfig config = ConfigManager.get();
		if (!config.itemScaleFirstPerson && !config.itemScaleDropped && !config.itemScales.isEmpty()) {
			graphics.drawCenteredString(this.font, Component.translatable("vantage.item_scales.disabled"),
					this.width / 2, this.height - 66, 0xFFFF6060);
		}
	}

	@Override
	public void onClose() {
		ConfigManager.save();
		this.minecraft.setScreen(this.parent);
	}
}
