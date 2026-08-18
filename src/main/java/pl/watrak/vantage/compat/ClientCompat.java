package pl.watrak.vantage.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;

/**
 * Client lookups that moved between the Minecraft versions this mod targets.
 *
 * <p>Feature code asks these questions from several places, so answering them
 * here keeps the version conditionals in one file instead of scattering them
 * through the HUD and screen code. Only differences that change the shape of an
 * expression belong here — everything that is merely a rename is handled by a
 * replacement rule in the build script, which needs no code at all.
 */
public final class ClientCompat {

	private ClientCompat() {
	}

	/** Whether the player has hidden the HUD, normally with F1. */
	public static boolean hudHidden(Minecraft minecraft) {
		//? if >=26.2 {
		/*return minecraft.gui.hud.isHidden();
		*///?} else {
		return minecraft.options.hideGui;
		//?}
	}

	/**
	 * The sprite drawn for a status effect.
	 *
	 * <p>Named in full on both sides rather than imported: the class this lives
	 * on is called Gui on one version and Hud on the other, and both names are
	 * in use elsewhere in the mod for genuinely different classes.
	 */
	public static Identifier mobEffectSprite(Holder<MobEffect> effect) {
		//? if >=26.2 {
		/*return net.minecraft.client.gui.Hud.getMobEffectSprite(effect);
		*///?} else {
		return net.minecraft.client.gui.Gui.getMobEffectSprite(effect);
		//?}
	}

	/**
	 * Whether vanilla would draw player faces in the tab list.
	 *
	 * <p>Worth asking because it decides how far along a row the ping number
	 * belongs. 26.x settled on a single online-mode check where earlier versions
	 * asked whether the world was local or the connection encrypted; both answer
	 * the same underlying question of whether skins can be trusted.
	 */
	public static boolean vanillaShowsTabFaces(Minecraft minecraft) {
		if (minecraft.getConnection() == null) {
			return false;
		}
		//? if >=26.2 {
		/*return minecraft.getConnection().onlineMode();
		*///?} else {
		return minecraft.isLocalServer() || minecraft.getConnection().getConnection().isEncrypted();
		//?}
	}

	/** The screen currently open, or null while the player is in the world. */
	public static Screen currentScreen(Minecraft minecraft) {
		//? if >=26.2 {
		/*return minecraft.gui.screen();
		*///?} else {
		return minecraft.screen;
		//?}
	}

}
