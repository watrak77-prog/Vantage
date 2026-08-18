package pl.watrak.vantage.hud;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import pl.watrak.vantage.VantageClient;
import pl.watrak.vantage.compat.ClientCompat;
import pl.watrak.vantage.config.ConfigManager;
import pl.watrak.vantage.feature.ArmorWarning;
import pl.watrak.vantage.config.VantageConfig;
import pl.watrak.vantage.config.VantageConfig.ArmorOrientation;
import pl.watrak.vantage.config.VantageConfig.ArmorStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * A hotbar-shaped widget showing worn armour.
 *
 * <p>Vanilla's armour bar gives a single number that says nothing about which
 * piece is nearly gone. This shows the pieces themselves, styled to sit against
 * the hotbar as though it had always had four more slots.
 */
public final class ArmorHudElement implements HudElement {

	private static final Identifier HOTBAR_SPRITE = Identifier.withDefaultNamespace("hud/hotbar");

	/** Vanilla's hotbar texture: a one pixel border either side of nine 20px slots. */
	private static final int HOTBAR_TEXTURE_WIDTH = 182;
	private static final int HOTBAR_TEXTURE_HEIGHT = 22;
	private static final int BORDER = 1;
	private static final int SLOT = 20;
	private static final int SLOT_PADDING = 3;

	private static final int HOTBAR_HALF_WIDTH = 91;

	/** Width vanilla's offhand slot sticks out past the end of the hotbar. */
	private static final int OFFHAND_SLOT_WIDTH = 29;

	private static final Identifier WARNING_TEXTURE = VantageClient.id("textures/gui/armor_warn.png");
	private static final int WARNING_SIZE = 8;

	private static final RandomSource RANDOM = RandomSource.create();

	/** Helmet first, so the column reads top-down the way the body does. */
	private static final EquipmentSlot[] ARMOR = {
			EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
	};

	private static final int SLOT_BACKDROP = 0x60000000;

	@Override
	public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
		VantageConfig config = ConfigManager.get();
		if (!config.armorHud) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || ClientCompat.hudHidden(minecraft)) {
			return;
		}

		List<ItemStack> pieces = collect(player, config);
		if (pieces.isEmpty()) {
			return;
		}

		boolean horizontal = config.armorHudOrientation == ArmorOrientation.HORIZONTAL;
		int slots = pieces.size();
		int width = horizontal ? slots * SLOT + BORDER * 2 : SLOT + BORDER * 2;
		int height = horizontal ? SLOT + BORDER * 2 : slots * SLOT + BORDER * 2;

		float scale = config.armorHudScale / 100.0F;
		int screenWidth = Math.round(graphics.guiWidth() / scale);
		int screenHeight = Math.round(graphics.guiHeight() / scale);

		int x = originX(config, player, screenWidth, width);
		int y = originY(config, screenHeight, height);

		graphics.pose().pushMatrix();
		graphics.pose().scale(scale, scale);

		drawBackground(graphics, config.armorHudStyle, x, y, slots, horizontal);
		drawPieces(graphics, minecraft, config, pieces, x, y, horizontal);

		graphics.pose().popMatrix();
	}

	/**
	 * The pieces worth drawing.
	 *
	 * <p>An empty list means "draw nothing at all", which is how the visibility
	 * modes that hide the widget entirely are expressed.
	 */
	private static List<ItemStack> collect(LocalPlayer player, VantageConfig config) {
		List<ItemStack> worn = new ArrayList<>(4);
		boolean anyPresent = false;

		for (EquipmentSlot slot : ARMOR) {
			ItemStack stack = player.getItemBySlot(slot);
			if (!stack.isEmpty()) {
				anyPresent = true;
			}
			if (!stack.isEmpty() || config.armorHudVisibility.drawsEmptySlots()) {
				worn.add(stack);
			}
		}

		return switch (config.armorHudVisibility) {
			case ALWAYS -> worn;
			case IF_ANY_PRESENT, NOT_EMPTY -> anyPresent ? worn : List.of();
		};
	}

	private static int originX(VantageConfig config, LocalPlayer player, int screenWidth, int width) {
		int centre = screenWidth / 2;
		int offhand = offhandSpace(config, player);

		return config.armorHudOffsetX + switch (config.armorHudAnchor) {
			// Tucked just outside the hotbar's own edge, past the offhand slot
			// when that slot is in the way.
			case HOTBAR -> switch (config.armorHudSide) {
				case LEFT -> centre - HOTBAR_HALF_WIDTH - width - offhand;
				case RIGHT -> centre + HOTBAR_HALF_WIDTH + offhand;
			};
			case TOP_LEFT, BOTTOM_LEFT -> 0;
			case TOP_RIGHT, BOTTOM_RIGHT -> screenWidth - width;
		};
	}

	/**
	 * Width to leave clear for the offhand slot, or zero when it is not a
	 * problem.
	 *
	 * <p>Vanilla puts the offhand slot beyond whichever end of the hotbar is
	 * opposite the main hand, and only while something is actually in it — so
	 * the widget has to move out of the way exactly when that happens, and move
	 * back when the offhand empties.
	 */
	private static int offhandSpace(VantageConfig config, LocalPlayer player) {
		if (config.armorHudAnchor != VantageConfig.ArmorAnchor.HOTBAR
				|| config.armorHudOffhand == VantageConfig.ArmorOffhand.IGNORE) {
			return 0;
		}

		boolean sameSide = (player.getMainArm().getOpposite() == HumanoidArm.LEFT)
				== (config.armorHudSide == VantageConfig.ArmorSide.LEFT);
		if (!sameSide) {
			return 0;
		}

		boolean occupied = !player.getOffhandItem().isEmpty();
		return config.armorHudOffhand == VantageConfig.ArmorOffhand.RESERVE || occupied
				? OFFHAND_SLOT_WIDTH
				: 0;
	}

	private static int originY(VantageConfig config, int screenHeight, int height) {
		return config.armorHudOffsetY + switch (config.armorHudAnchor) {
			// Bottom-aligned with the hotbar, whichever way it is oriented.
			case HOTBAR -> screenHeight - height;
			case TOP_LEFT, TOP_RIGHT -> 0;
			case BOTTOM_LEFT, BOTTOM_RIGHT -> screenHeight - height;
		};
	}

	/**
	 * Rebuilds a hotbar of the right length out of vanilla's own texture.
	 *
	 * <p>The texture is nine slots wide and cannot be stretched without smearing
	 * the borders, so the widget is assembled instead: the left border and as
	 * many slots as are needed, then the right border blitted separately.
	 */
	private static void drawBackground(GuiGraphics graphics, ArmorStyle style,
	                                   int x, int y, int slots, boolean horizontal) {
		switch (style) {
			case NONE -> {
			}
			case SLOTS -> {
				for (int i = 0; i < slots; i++) {
					int slotX = horizontal ? x + BORDER + i * SLOT : x + BORDER;
					int slotY = horizontal ? y + BORDER : y + BORDER + i * SLOT;
					graphics.fill(slotX, slotY, slotX + SLOT - 2, slotY + SLOT - 2, SLOT_BACKDROP);
				}
			}
			case HOTBAR -> {
				if (horizontal) {
					graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE,
							HOTBAR_TEXTURE_WIDTH, HOTBAR_TEXTURE_HEIGHT,
							0, 0, x, y, slots * SLOT + BORDER, HOTBAR_TEXTURE_HEIGHT);
					graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE,
							HOTBAR_TEXTURE_WIDTH, HOTBAR_TEXTURE_HEIGHT,
							HOTBAR_TEXTURE_WIDTH - BORDER, 0,
							x + slots * SLOT + BORDER, y, BORDER, HOTBAR_TEXTURE_HEIGHT);
				} else {
					// Stacked single slots; the shared borders overlap by design.
					for (int i = 0; i < slots; i++) {
						graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE,
								HOTBAR_TEXTURE_WIDTH, HOTBAR_TEXTURE_HEIGHT,
								0, 0, x, y + i * SLOT, SLOT + BORDER * 2, HOTBAR_TEXTURE_HEIGHT);
					}
				}
			}
		}
	}

	private static void drawPieces(GuiGraphics graphics, Minecraft minecraft, VantageConfig config,
	                               List<ItemStack> pieces, int x, int y, boolean horizontal) {
		for (int i = 0; i < pieces.size(); i++) {
			ItemStack stack = pieces.get(i);
			if (stack.isEmpty()) {
				continue;
			}

			int itemX = horizontal ? x + SLOT_PADDING + i * SLOT : x + SLOT_PADDING;
			int itemY = horizontal ? y + SLOT_PADDING : y + SLOT_PADDING + i * SLOT;

			graphics.renderItem(stack, itemX, itemY);

			drawWarning(graphics, config, stack, itemX, itemY, horizontal);

			// Vanilla's own decoration call places the label exactly where a
			// stack count would go, so the numeric modes line up for free.
			switch (config.armorHudDurability) {
				case NONE -> {
				}
				case BAR -> graphics.renderItemDecorations(minecraft.font, stack, itemX, itemY);
				case NUMERIC -> graphics.renderItemDecorations(minecraft.font, stack, itemX, itemY,
						stack.isDamageableItem() ? String.valueOf(stack.getMaxDamage() - stack.getDamageValue()) : "");
				case PERCENTAGE -> graphics.renderItemDecorations(minecraft.font, stack, itemX, itemY,
						stack.isDamageableItem() ? percent(stack) + "%" : "");
			}
		}
	}

	/**
	 * Warning icon for a piece about to break, jittering in place.
	 *
	 * <p>Placed outside the slot — above it when the widget runs across, beside
	 * it when it runs down — so it never covers the item it is warning about.
	 * The shake is a per-frame random offset rather than a smooth wave, which is
	 * what makes it read as an alarm instead of an animation.
	 */
	private static void drawWarning(GuiGraphics graphics, VantageConfig config, ItemStack stack,
	                                int itemX, int itemY, boolean horizontal) {
		if (!ArmorWarning.isCritical(stack)) {
			return;
		}

		int jitter = ArmorWarning.shakeOffset();

		int x = horizontal ? itemX + (16 - WARNING_SIZE) / 2 : itemX - WARNING_SIZE - 2;
		int y = horizontal ? itemY - WARNING_SIZE - 2 + jitter : itemY + (16 - WARNING_SIZE) / 2 + jitter;

		graphics.blit(RenderPipelines.GUI_TEXTURED, WARNING_TEXTURE, x, y,
				0, 0, WARNING_SIZE, WARNING_SIZE, WARNING_SIZE, WARNING_SIZE);
	}

	private static int percent(ItemStack stack) {
		float remaining = 1.0F - (float) stack.getDamageValue() / stack.getMaxDamage();
		return Math.round(remaining * 100.0F);
	}

}
