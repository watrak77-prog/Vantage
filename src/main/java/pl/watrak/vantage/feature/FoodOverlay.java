package pl.watrak.vantage.feature;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import pl.watrak.vantage.VantageClient;
import pl.watrak.vantage.config.ConfigManager;
import pl.watrak.vantage.config.VantageConfig;

/**
 * Food information vanilla keeps hidden: how much saturation is left, and what
 * the food in your hand would restore.
 *
 * <p>Saturation decides when the hunger bar starts dropping at all and how fast
 * you regenerate, so playing without it visible means guessing.
 *
 * <p>Exhaustion is deliberately absent. The server never sends it — only health,
 * food and saturation travel to the client — so a client-only mod cannot show a
 * real value for it, and showing a made-up one would be worse than showing none.
 */
public final class FoodOverlay {

	/** Saturation shards, from AppleSkin, which is public domain. */
	private static final Identifier ICONS = VantageClient.id("textures/gui/appleskin_icons.png");
	private static final int TEXTURE_SIZE = 256;

	private static final Identifier FOOD_FULL = Identifier.withDefaultNamespace("hud/food_full");
	private static final Identifier FOOD_HALF = Identifier.withDefaultNamespace("hud/food_half");
	private static final Identifier HEART_FULL = Identifier.withDefaultNamespace("hud/heart/full");
	private static final Identifier HEART_HALF = Identifier.withDefaultNamespace("hud/heart/half");

	private static final int ICON_COUNT = 10;
	private static final int ICON_SIZE = 9;
	private static final int ICON_STEP = 8;
	private static final int MAX_FOOD = 20;

	/**
	 * Saturation consumed per point of health regenerated.
	 *
	 * <p>Vanilla heals {@code min(saturation, 6) / 6} health per cycle while
	 * adding the same figure as exhaustion, and four exhaustion burns one
	 * saturation — so a full point of health costs six exhaustion, or one and a
	 * half saturation.
	 */
	private static final float SATURATION_PER_HEALTH = 1.5F;

	/** A full fade out and back, in milliseconds. */
	private static final long PULSE_PERIOD = 1600L;

	private static final int OPAQUE = 0xFFFFFFFF;

	private FoodOverlay() {
	}

	/**
	 * Opacity for anything that is a prediction rather than a fact.
	 *
	 * <p>Ramps to full and back to nothing, so the icons underneath show through
	 * at the bottom of each cycle. That alternation is what separates "this is
	 * what you have" from "this is what you would get" without needing a second
	 * colour or a legend.
	 */
	private static float pulse() {
		float phase = (Util.getMillis() % PULSE_PERIOD) / (PULSE_PERIOD / 2.0F);
		return phase > 1.0F ? 2.0F - phase : phase;
	}

	private static int pulseTint() {
		int alpha = Mth.clamp(Math.round(pulse() * 255.0F), 0, 255);
		return (alpha << 24) | 0xFFFFFF;
	}

	// ------------------------------------------------------------ hunger row

	public static void renderFood(GuiGraphics graphics, Player player, int y, int right) {
		VantageConfig config = ConfigManager.get();
		if (!config.foodOverlay) {
			return;
		}

		FoodData food = player.getFoodData();
		FoodProperties held = heldFood(player);

		renderSaturation(graphics, food, held, y, right);
		if (held != null) {
			renderHungerPreview(graphics, food, held, y, right);
		}
	}

	/** Left edge of the hunger icon at {@code index}, counting from the right. */
	private static int iconX(int right, int index) {
		return right - index * ICON_STEP - ICON_SIZE;
	}

	/**
	 * Saturation drawn as shards over the hunger icons.
	 *
	 * <p>Four sprites cover a quarter, half, three quarters and a whole point,
	 * so the overlay steps down finely as saturation drains rather than dropping
	 * a whole icon at a time.
	 */
	private static void renderSaturation(GuiGraphics graphics, FoodData food,
	                                     @Nullable FoodProperties held, int y, int right) {
		float saturation = Mth.clamp(food.getSaturationLevel(), 0.0F, MAX_FOOD);

		if (held != null) {
			float preview = Mth.clamp(saturationAfterEating(food, held), 0.0F, MAX_FOOD);
			if (preview > saturation) {
				// Starts where the real saturation ends, so the pulse only ever
				// covers the part that would be gained.
				drawSaturation(graphics, preview, (int) (saturation / 2.0F), y, right, pulseTint());
			}
		}

		drawSaturation(graphics, saturation, 0, y, right, OPAQUE);
	}

	private static void drawSaturation(GuiGraphics graphics, float level, int firstIcon,
	                                   int y, int right, int tint) {
		int last = Math.min(Mth.ceil(level / 2.0F), ICON_COUNT);

		for (int i = Math.max(firstIcon, 0); i < last; i++) {
			float remaining = level / 2.0F - i;
			int u = remaining >= 1.0F ? 27 : remaining > 0.5F ? 18 : remaining > 0.25F ? 9 : 0;

			graphics.blit(RenderPipelines.GUI_TEXTURED, ICONS, iconX(right, i), y,
					u, 0, ICON_SIZE, ICON_SIZE, TEXTURE_SIZE, TEXTURE_SIZE, tint);
		}
	}

	/** Saturation the player would have after eating, mirroring FoodData.add. */
	private static float saturationAfterEating(FoodData food, FoodProperties held) {
		int foodAfter = Math.min(food.getFoodLevel() + held.nutrition(), MAX_FOOD);
		return Math.min(food.getSaturationLevel() + held.saturation(), foodAfter);
	}

	/**
	 * Pulsing hunger icons covering what the held food would refill.
	 *
	 * <p>Icon {@code i} stands for food values {@code i*2+1} and {@code i*2+2},
	 * matching how vanilla picks between its full and half sprites. Icons
	 * vanilla already filled solid are skipped so the pulse only marks the gain.
	 */
	private static void renderHungerPreview(GuiGraphics graphics, FoodData food,
	                                        FoodProperties held, int y, int right) {
		int current = food.getFoodLevel();
		int restored = Math.min(current + held.nutrition(), MAX_FOOD);
		if (restored <= current) {
			return;
		}

		int tint = pulseTint();

		for (int i = 0; i < ICON_COUNT; i++) {
			int value = i * 2 + 1;
			if (value < current) {
				continue;
			}

			int x = iconX(right, i);
			if (value < restored) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FOOD_FULL, x, y, ICON_SIZE, ICON_SIZE, tint);
			} else if (value == restored && value != current) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FOOD_HALF, x, y, ICON_SIZE, ICON_SIZE, tint);
			}
		}
	}

	// ------------------------------------------------------------ hearts row

	/**
	 * Pulsing hearts for the health the held food would eventually restore.
	 *
	 * <p>An estimate by nature: the food does not heal directly, it tops up
	 * saturation which natural regeneration then spends. It assumes regeneration
	 * is allowed to run, which is part of why it pulses rather than sitting
	 * there as a promise.
	 */
	public static void renderHealthPreview(GuiGraphics graphics, Player player, int left, int y,
	                                       int rowHeight, float maxHealth, int health) {
		if (!ConfigManager.get().foodOverlay) {
			return;
		}

		FoodProperties held = heldFood(player);
		if (held == null) {
			return;
		}

		FoodData food = player.getFoodData();
		float gained = (saturationAfterEating(food, held) - food.getSaturationLevel()) / SATURATION_PER_HEALTH;
		if (gained <= 0.0F) {
			return;
		}

		int target = Math.min(Mth.ceil(maxHealth), health + Math.round(gained));
		if (target <= health) {
			return;
		}

		int tint = pulseTint();
		int hearts = Mth.ceil(maxHealth / 2.0F);

		for (int i = 0; i < hearts; i++) {
			int halfValue = i * 2 + 1;
			int fullValue = i * 2 + 2;
			if (fullValue <= health) {
				continue;
			}

			int x = left + (i % 10) * 8;
			int heartY = y - (i / 10) * rowHeight;

			if (fullValue <= target) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HEART_FULL, x, heartY, ICON_SIZE, ICON_SIZE, tint);
			} else if (halfValue <= target && halfValue > health) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HEART_HALF, x, heartY, ICON_SIZE, ICON_SIZE, tint);
			}
		}
	}

	// ---------------------------------------------------------------- lookup

	/** Edible item in either hand, main hand winning, or null when holding none. */
	public static @Nullable FoodProperties heldFood(Player player) {
		FoodProperties main = foodOf(player.getMainHandItem());
		return main != null ? main : foodOf(player.getOffhandItem());
	}

	public static @Nullable FoodProperties foodOf(ItemStack stack) {
		return stack.isEmpty() ? null : stack.get(DataComponents.FOOD);
	}
}
