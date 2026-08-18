package pl.watrak.vantage.hud;

import com.google.common.collect.Ordering;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import pl.watrak.vantage.compat.ClientCompat;
import pl.watrak.vantage.config.ConfigManager;
import pl.watrak.vantage.feature.EffectDurationTracker;
import pl.watrak.vantage.config.VantageConfig;
import pl.watrak.vantage.config.VantageConfig.EffectStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * Status effects drawn with their remaining time, at a corner of your choosing.
 *
 * <p>Takes over from vanilla rather than drawing on top of it. Vanilla pins the
 * icons to the top right and offers no say in it, so adding a timer there and
 * then wanting the block somewhere else would mean two systems fighting over
 * the same corner.
 *
 * <p>Vanilla's own arrangement is kept — beneficial effects on one row, harmful
 * on the row below — because that split is genuinely useful and moving the block
 * is a separate question from reorganising it.
 */
public final class StatusEffectHud {

	private static final int ICON = 24;
	private static final int SPACING = 25;
	private static final int ROW_GAP = 26;
	private static final int EDGE_PADDING = 1;

	private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("hud/effect_background");
	private static final Identifier BACKGROUND_AMBIENT = Identifier.withDefaultNamespace("hud/effect_background_ambient");

	/** Below which the timer turns red, matching vanilla's blink threshold. */
	private static final int URGENT_TICKS = 200;

	private static final float LABEL_SCALE = 0.75F;

	private static final int LABEL_WHITE = 0xFFFFFFFF;
	private static final int LABEL_GREY = 0xFFAAAAAA;

	/** Thin bar along the bottom edge of an icon in the compact styles. */
	private static final int SMALL_BAR_HEIGHT = 2;
	private static final int SMALL_BAR_INSET = 3;

	/**
	 * The labelled row used by the full style, built on vanilla's own inventory
	 * widget so it is the same thing the player already knows, only movable and
	 * with a bar added. The geometry below is vanilla's, not an approximation of
	 * it: sprite 32 tall, icon inset by 7, text starting at 32 with the duration
	 * on the next line.
	 */
	private static final Identifier PANEL = Identifier.withDefaultNamespace("container/inventory/effect_background");
	private static final Identifier PANEL_AMBIENT = Identifier.withDefaultNamespace("container/inventory/effect_background_ambient");

	private static final int PANEL_WIDTH = 120;
	private static final int PANEL_HEIGHT = 32;
	private static final int PANEL_GAP = 1;
	private static final int PANEL_TEXT_X = 32;
	private static final int PANEL_TEXT_Y = 7;
	private static final int PANEL_INSET = 3;

	/** Vanilla's own grey for the duration line. */
	private static final int PANEL_DURATION = -8355712;

	private static final int BAR_TRACK = 0x60000000;

	private StatusEffectHud() {
	}

	/** True when this replaces vanilla's own effect rendering entirely. */
	public static boolean isEnabled() {
		return ConfigManager.get().statusEffectTimer;
	}

	public static void render(GuiGraphics graphics, Minecraft minecraft) {
		LocalPlayer player = minecraft.player;
		// Stand down while a screen is already showing the same effects, so they
		// are not drawn twice over each other.
		Screen screen = ClientCompat.currentScreen(minecraft);
		if (player == null || (screen != null && screen.showsActiveEffects())) {
			return;
		}

		List<MobEffectInstance> beneficial = new ArrayList<>();
		List<MobEffectInstance> harmful = new ArrayList<>();

		for (MobEffectInstance effect : Ordering.natural().reverse().sortedCopy(player.getActiveEffects())) {
			if (!effect.showIcon()) {
				continue;
			}
			(effect.getEffect().value().isBeneficial() ? beneficial : harmful).add(effect);
		}

		if (beneficial.isEmpty() && harmful.isEmpty()) {
			return;
		}

		VantageConfig config = ConfigManager.get();

		// Every visible effect goes to the tracker, whichever style is drawn, so
		// switching styles mid-game does not start the bars from nothing.
		List<MobEffectInstance> shown = new ArrayList<>(beneficial);
		shown.addAll(harmful);
		EffectDurationTracker.update(shown);

		// Everything below is laid out in unscaled units and then drawn through a
		// single scale, so one factor sizes the icons in the compact styles and
		// the whole frame in the full one without either needing to know.
		float scale = config.effectScale() / 100.0F;
		int width = Math.round(graphics.guiWidth() / scale);
		int height = Math.round(graphics.guiHeight() / scale);

		graphics.pose().pushMatrix();
		graphics.pose().scale(scale, scale);

		if (config.statusEffectStyle == EffectStyle.BARS_FULL) {
			drawPanels(graphics, minecraft, config, shown, width, height);
		} else {
			int rows = (beneficial.isEmpty() ? 0 : 1) + (harmful.isEmpty() ? 0 : 1);
			int topY = originY(config, height, rows * ROW_GAP, minecraft.isDemo());
			int row = 0;

			if (!beneficial.isEmpty()) {
				drawRow(graphics, minecraft, config, beneficial, width, topY + row * ROW_GAP);
				row++;
			}
			if (!harmful.isEmpty()) {
				drawRow(graphics, minecraft, config, harmful, width, topY + row * ROW_GAP);
			}
		}

		graphics.pose().popMatrix();
	}

	private static int originY(VantageConfig config, int screenHeight, int blockHeight, boolean demo) {
		int offset = config.statusEffectOffsetY;

		return switch (config.statusEffectAnchor) {
			// The demo banner sits across the top, so the top corners start below it.
			case TOP_LEFT, TOP_RIGHT -> EDGE_PADDING + (demo ? 15 : 0) + offset;
			case BOTTOM_LEFT, BOTTOM_RIGHT -> screenHeight - blockHeight - EDGE_PADDING - offset;
		};
	}

	private static void drawRow(GuiGraphics graphics, Minecraft minecraft, VantageConfig config,
	                            List<MobEffectInstance> effects, int width, int y) {
		for (int i = 0; i < effects.size(); i++) {
			MobEffectInstance effect = effects.get(i);
			int x = iconX(config, width, i);

			drawIcon(graphics, effect, x, y);

			if (config.statusEffectStyle == EffectStyle.BARS_SMALL) {
				drawSmallBar(graphics, effect, x, y);
			} else {
				drawTimer(graphics, minecraft, effect, x, y);
			}
		}
	}

	/** Right-anchored rows grow leftwards, left-anchored rows grow rightwards. */
	private static int iconX(VantageConfig config, int screenWidth, int index) {
		int offset = config.statusEffectOffsetX;

		return switch (config.statusEffectAnchor) {
			case TOP_RIGHT, BOTTOM_RIGHT -> screenWidth - SPACING * (index + 1) - offset;
			case TOP_LEFT, BOTTOM_LEFT -> EDGE_PADDING + SPACING * index + offset;
		};
	}

	/**
	 * The icon and its frame, including vanilla's fade as an effect runs out.
	 *
	 * <p>The fade is reproduced rather than skipped because it is the only cue
	 * vanilla gives at a glance, and losing it would make this a downgrade for
	 * anyone who reads it.
	 */
	private static void drawIcon(GuiGraphics graphics, MobEffectInstance effect, int x, int y) {
		float alpha = 1.0F;

		if (effect.isAmbient()) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_AMBIENT, x, y, ICON, ICON);
		} else {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, x, y, ICON, ICON);

			if (effect.endsWithin(URGENT_TICKS)) {
				int remaining = effect.getDuration();
				int elapsed = 10 - remaining / 20;
				alpha = Mth.clamp(remaining / 10.0F / 5.0F * 0.5F, 0.0F, 0.5F)
						+ Mth.cos(remaining * (float) Math.PI / 5.0F) * Mth.clamp(elapsed / 10.0F * 0.25F, 0.0F, 0.25F);
				alpha = Mth.clamp(alpha, 0.0F, 1.0F);
			}
		}

		Holder<MobEffect> holder = effect.getEffect();
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ClientCompat.mobEffectSprite(holder),
				x + 3, y + 3, 18, 18, ARGB.white(alpha));
	}

	/**
	 * The remaining time along the bottom edge of the icon.
	 *
	 * <p>No plate behind it: a backdrop wide enough to sit behind the text also
	 * covered half the icon it belongs to. The drop shadow carries it instead,
	 * and the colour is left to preference.
	 */
	private static void drawTimer(GuiGraphics graphics, Minecraft minecraft, MobEffectInstance effect,
	                              int x, int y) {
		if (effect.isInfiniteDuration()) {
			return;
		}

		String text = format(effect.getDuration());
		float labelScale = LABEL_SCALE * (ConfigManager.get().effectTimerSize() / 100.0F);
		int textHeight = Math.round(minecraft.font.lineHeight * labelScale);

		int centreX = x + ICON / 2;
		// Lifted clear of the icon's bottom edge, which is where the frame is
		// darkest and the digits were hardest to pick out.
		int plateY = y + ICON - textHeight - 2;

		int colour = effect.getDuration() <= URGENT_TICKS
				? 0xFFFF6060
				: switch (ConfigManager.get().statusEffectColour) {
					case WHITE -> LABEL_WHITE;
					case GREY -> LABEL_GREY;
				};

		graphics.pose().pushMatrix();
		graphics.pose().scale(labelScale, labelScale);
		graphics.drawCenteredString(minecraft.font, text,
				Math.round(centreX / labelScale), Math.round(plateY / labelScale), colour);
		graphics.pose().popMatrix();
	}


	/**
	 * A strip of the effect's own colour along the bottom of its icon.
	 *
	 * <p>Inset from the frame's edge so it reads as part of the icon rather than
	 * as a border, and drawn over a dark track so a nearly empty bar is still
	 * placed rather than invisible.
	 */
	private static void drawSmallBar(GuiGraphics graphics, MobEffectInstance effect, int x, int y) {
		int thickness = barThickness();
		int left = x + SMALL_BAR_INSET;
		int right = x + ICON - SMALL_BAR_INSET;
		int top = y + ICON - SMALL_BAR_INSET - thickness;

		graphics.fill(left, top, right, top + thickness, BAR_TRACK);

		int filled = Math.round((right - left) * EffectDurationTracker.progress(effect));
		if (filled > 0) {
			graphics.fill(left, top, left + filled, top + thickness, colourOf(effect));
		}
	}

	/** One labelled row per effect, stacked from the chosen corner. */
	private static void drawPanels(GuiGraphics graphics, Minecraft minecraft, VantageConfig config,
	                               List<MobEffectInstance> effects, int width, int height) {
		int blockHeight = effects.size() * (PANEL_HEIGHT + PANEL_GAP) - PANEL_GAP;
		int x = panelX(config, width);
		int y = originY(config, height, blockHeight, minecraft.isDemo());

		for (MobEffectInstance effect : effects) {
			drawPanel(graphics, minecraft, effect, x, y);
			y += PANEL_HEIGHT + PANEL_GAP;
		}
	}

	private static int panelX(VantageConfig config, int screenWidth) {
		int offset = config.statusEffectOffsetX;

		return switch (config.statusEffectAnchor) {
			case TOP_RIGHT, BOTTOM_RIGHT -> screenWidth - PANEL_WIDTH - EDGE_PADDING - offset;
			case TOP_LEFT, BOTTOM_LEFT -> EDGE_PADDING + offset;
		};
	}

	private static void drawPanel(GuiGraphics graphics, Minecraft minecraft, MobEffectInstance effect,
	                              int x, int y) {
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED,
				effect.isAmbient() ? PANEL_AMBIENT : PANEL,
				x, y, PANEL_WIDTH, PANEL_HEIGHT);

		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ClientCompat.mobEffectSprite(effect.getEffect()),
				x + PANEL_TEXT_Y, y + PANEL_TEXT_Y, 18, 18);

		// Clipped to what is left of the frame, so a long name is cut with an
		// ellipsis instead of running past the border.
		int textWidth = PANEL_WIDTH - PANEL_TEXT_X - PANEL_INSET;
		graphics.drawString(minecraft.font,
				StringWidget.clipText(displayName(effect), minecraft.font, textWidth),
				x + PANEL_TEXT_X, y + PANEL_TEXT_Y, -1);
		graphics.drawString(minecraft.font,
				StringWidget.clipText(duration(effect, minecraft), minecraft.font, textWidth),
				x + PANEL_TEXT_X, y + PANEL_TEXT_Y + 9, PANEL_DURATION);

		int thickness = barThickness();
		int left = x + PANEL_INSET;
		int right = x + PANEL_WIDTH - PANEL_INSET;
		int top = y + PANEL_HEIGHT - PANEL_INSET - thickness;

		graphics.fill(left, top, right, top + thickness, BAR_TRACK);

		int filled = Math.round((right - left) * EffectDurationTracker.progress(effect));
		if (filled > 0) {
			graphics.fill(left, top, left + filled, top + thickness, colourOf(effect));
		}
	}

	/**
	 * Bar thickness in pixels, from the same setting that sizes the digits.
	 *
	 * <p>One knob covers both because only one of them is ever on screen: the
	 * style decides which, so a single "how big should the time be" answers it
	 * either way. Never allowed below a pixel, which would erase the bar.
	 */
	private static int barThickness() {
		int scaled = Math.round(SMALL_BAR_HEIGHT * (ConfigManager.get().effectTimerSize() / 100.0F));
		return Math.max(1, scaled);
	}

	/** The effect's own colour, opaque. */
	private static int colourOf(MobEffectInstance effect) {
		return 0xFF000000 | effect.getEffect().value().getColor();
	}

	/**
	 * The effect's name with its level, spelled the way vanilla spells it.
	 *
	 * <p>Vanilla appends a space and the enchantment level key rather than using
	 * a numeral, so following it keeps the two identical in every language.
	 */
	private static Component displayName(MobEffectInstance effect) {
		MutableComponent name = effect.getEffect().value().getDisplayName().copy();
		int amplifier = effect.getAmplifier();

		if (amplifier >= 1 && amplifier <= 9) {
			name.append(CommonComponents.SPACE)
					.append(Component.translatable("enchantment.level." + (amplifier + 1)));
		}
		return name;
	}

	/** Remaining time, formatted by vanilla so it tracks the world's tick rate. */
	private static Component duration(MobEffectInstance effect, Minecraft minecraft) {
		float tickrate = minecraft.level == null ? 20.0F : minecraft.level.tickRateManager().tickrate();
		return MobEffectUtil.formatDuration(effect, 1.0F, tickrate);
	}

	/** Seconds under a minute, m:ss above it — the shortest form that stays clear. */
	private static String format(int ticks) {
		int seconds = ticks / 20;
		return seconds < 60 ? seconds + "s" : (seconds / 60) + ":" + String.format("%02d", seconds % 60);
	}
}
