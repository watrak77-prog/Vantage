package pl.watrak.vantage.feature;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.system.APIUtil;
import org.lwjgl.system.JNI;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Platform;
import org.lwjgl.system.SharedLibrary;
import org.jspecify.annotations.Nullable;
import pl.watrak.vantage.VantageClient;
import pl.watrak.vantage.config.ConfigManager;

import java.nio.IntBuffer;

/**
 * Darkens the window's own title bar on Windows.
 *
 * <p>The bar belongs to the operating system rather than the game, so no amount
 * of rendering reaches it — it has to be asked for through the desktop window
 * manager. Everywhere other than Windows this does nothing at all.
 */
public final class WindowTheme {

	/**
	 * Documented attribute for a dark title bar. Windows 10 builds before 20H1
	 * used 19 for the same thing and ignore 20, so both are tried.
	 */
	private static final int DWMWA_USE_IMMERSIVE_DARK_MODE = 20;
	private static final int DWMWA_USE_IMMERSIVE_DARK_MODE_PRE_20H1 = 19;

	/**
	 * Declared as a long on purpose. Several {@code invokePPPI} overloads accept
	 * these arguments once int-to-long widening is allowed, and an int here
	 * leaves the call ambiguous; a long selects the intended one outright.
	 */
	private static final long ATTRIBUTE_SIZE = 4L;
	private static final int S_OK = 0;

	private static @Nullable SharedLibrary dwmapi;
	private static long setWindowAttribute;
	private static boolean lookedUp;
	private static @Nullable Boolean applied;

	private WindowTheme() {
	}

	/**
	 * Reapplies only when the setting actually changed, since this is a system
	 * call that resizes the window frame.
	 *
	 * <p>Two rules learned the hard way. First, the window is never touched
	 * while the feature is off — the previous version still made one call at
	 * startup to set the attribute to "light", which is a frame change nobody
	 * asked for. Second, the framebuffer is resynced afterwards: changing the
	 * title bar changes the height of the non-client area, so the client area
	 * shrinks or grows, and if the game does not notice it keeps drawing at the
	 * old size — which shows up as the picture sitting in a corner of a black
	 * window.
	 */
	public static void sync() {
		if (Platform.get() != Platform.WINDOWS) {
			return;
		}

		boolean dark = ConfigManager.get().darkWindowTitleBar;

		if (applied == null) {
			// Nothing has been applied yet; leaving it alone is already correct
			// unless the player actually wants a dark bar.
			if (!dark) {
				applied = false;
				return;
			}
		} else if (applied == dark) {
			return;
		}

		if (apply(dark)) {
			applied = dark;
			// Re-reads the real framebuffer size and rebuilds the GUI scale, so
			// the frame change cannot leave the viewport behind.
			Minecraft.getInstance().resizeDisplay();
		}
	}

	private static boolean apply(boolean dark) {
		long function = function();
		if (function == 0L) {
			return false;
		}

		long window = GLFWNativeWin32.glfwGetWin32Window(Minecraft.getInstance().getWindow().handle());
		if (window == 0L) {
			return false;
		}

		try (MemoryStack stack = MemoryStack.stackPush()) {
			IntBuffer value = stack.ints(dark ? 1 : 0);
			long pointer = MemoryUtil.memAddress(value);

			int result = JNI.invokePPPI(window, DWMWA_USE_IMMERSIVE_DARK_MODE,
					pointer, ATTRIBUTE_SIZE, function);
			if (result != S_OK) {
				result = JNI.invokePPPI(window, DWMWA_USE_IMMERSIVE_DARK_MODE_PRE_20H1,
						pointer, ATTRIBUTE_SIZE, function);
			}

			if (result != S_OK) {
				VantageClient.LOGGER.warn("Windows refused the dark title bar request (0x{})",
						Integer.toHexString(result));
				return false;
			}
			return true;
		}
	}

	/** Looked up once; a missing dwmapi simply disables the feature. */
	private static long function() {
		if (!lookedUp) {
			lookedUp = true;
			try {
				dwmapi = APIUtil.apiCreateLibrary("dwmapi");
				setWindowAttribute = APIUtil.apiGetFunctionAddressOptional(dwmapi, "DwmSetWindowAttribute");
			} catch (Throwable t) {
				VantageClient.LOGGER.warn("Could not reach dwmapi; dark title bar unavailable", t);
				setWindowAttribute = 0L;
			}
		}
		return setWindowAttribute;
	}
}
