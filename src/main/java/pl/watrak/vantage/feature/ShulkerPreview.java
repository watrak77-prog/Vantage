package pl.watrak.vantage.feature;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.jspecify.annotations.Nullable;
//? if >=26.3 {
/*import org.lwjgl.sdl.SDLMouse;
import java.nio.FloatBuffer;
*///?} else {
import org.lwjgl.glfw.GLFW;
//?}
import pl.watrak.vantage.VantageClient;
import pl.watrak.vantage.config.ConfigManager;

import java.util.List;

/**
 * Swaps a shulker box's written contents for a grid while a key is held.
 *
 * <p>No list is added here: vanilla already writes one, and duplicating it was
 * the whole of the bug this replaced. The only thing this contributes is the
 * grid, and the key that trades one for the other.
 */
public final class ShulkerPreview {

	private static @Nullable KeyMapping key;

	private ShulkerPreview() {
	}

	public static void register() {
		key = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.vantage.shulker_preview",
				InputConstants.Type.KEYSYM,
				InputConstants.KEY_LALT,
				VantageClient.CATEGORY
		));
	}

	/**
	 * Whether the preview key is held right now.
	 *
	 * <p>Polled from the window rather than read off the key mapping, because a
	 * mapping only tracks presses while no screen is open — and a tooltip is
	 * almost always shown with an inventory in the way.
	 */
	public static boolean isPreviewHeld() {
		if (key == null || key.isUnbound()) {
			return false;
		}

		InputConstants.Key bound = KeyBindingHelper.getBoundKeyOf(key);

		//? if >=26.3 {
		/*// SDL keeps one keyboard state for the whole process, so no window is
		// named. Its mouse buttons are numbered from one rather than from zero,
		// and their state arrives as a bitmask.
		return switch (bound.getType()) {
			case KEYBOARD -> InputConstants.isKeyDown(bound.getValue());
			case MOUSE -> (SDLMouse.SDL_GetMouseState((FloatBuffer) null, (FloatBuffer) null)
					& (1 << (bound.getValue() - 1))) != 0;
		};
		*///?} else {
		Window window = Minecraft.getInstance().getWindow();

		return switch (bound.getType()) {
			case KEYSYM -> InputConstants.isKeyDown(window, bound.getValue());
			case MOUSE -> GLFW.glfwGetMouseButton(window.handle(), bound.getValue()) == GLFW.GLFW_PRESS;
			case SCANCODE -> false;
		};
		//?}
	}

	/** Non-empty contents of a shulker box, or an empty list for anything else. */
	public static List<ItemStack> contentsOf(ItemStack stack) {
		if (!(stack.getItem() instanceof BlockItem blockItem)
				|| !(blockItem.getBlock() instanceof ShulkerBoxBlock)) {
			return List.of();
		}

		ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
		return contents == null ? List.of() : contents.nonEmptyStream().toList();
	}
}
