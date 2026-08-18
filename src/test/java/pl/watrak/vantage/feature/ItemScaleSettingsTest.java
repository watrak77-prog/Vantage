package pl.watrak.vantage.feature;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemScaleSettingsTest {

	private static Map<String, Integer> overrides(String id, int percent) {
		Map<String, Integer> map = new LinkedHashMap<>();
		map.put(id, percent);
		return map;
	}

	@Test
	void appliesTheOverrideWhenEnabled() {
		assertEquals(1.5F,
				ItemScaleSettings.resolve(true, overrides("minecraft:totem_of_undying", 150),
						"minecraft:totem_of_undying"));
	}

	@Test
	void returnsOneForEveryItemWhenDisabled() {
		// The reported bug: overrides kept applying after the master toggle was
		// switched off.
		assertEquals(1.0F,
				ItemScaleSettings.resolve(false, overrides("minecraft:totem_of_undying", 150),
						"minecraft:totem_of_undying"));
	}

	@Test
	void treatsAStoredHundredAsNoChange() {
		// The other half of the report: dragging back to 100% has to actually
		// restore the item, even if the entry lingers in the map.
		assertEquals(1.0F,
				ItemScaleSettings.resolve(true, overrides("minecraft:totem_of_undying", 100),
						"minecraft:totem_of_undying"));
	}

	@Test
	void leavesItemsWithoutAnOverrideAlone() {
		assertEquals(1.0F,
				ItemScaleSettings.resolve(true, overrides("minecraft:totem_of_undying", 150),
						"minecraft:diamond_sword"));
	}

	@Test
	void toleratesMissingState() {
		assertEquals(1.0F, ItemScaleSettings.resolve(true, null, "minecraft:stone"));
		assertEquals(1.0F, ItemScaleSettings.resolve(true, Map.of(), "minecraft:stone"));
		assertEquals(1.0F, ItemScaleSettings.resolve(true, overrides("minecraft:stone", 150), null));
	}
}
