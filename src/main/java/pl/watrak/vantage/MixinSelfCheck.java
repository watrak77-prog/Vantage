package pl.watrak.vantage;

import net.fabricmc.loader.api.FabricLoader;

import java.util.List;

/**
 * Development-time check that every mixin actually applies.
 *
 * <p>Mixins are woven when their target class is first loaded, so a wrong
 * method name or descriptor stays silent until something in the game happens to
 * touch that class — a fog mixin might not fail until the player swims into
 * lava. This forces every target to load during startup instead, turning a
 * latent breakage into an immediate, named error.
 *
 * <p>That matters most when porting: the same source has to hit moving targets
 * across 1.21.1 to 26.2, and this reports which ones missed on a given version
 * without needing anyone to go looking for them in-game.
 *
 * <p>Runs only in a development environment, so shipped builds pay nothing.
 */
final class MixinSelfCheck {

	/** Every class this mod mixes into. Keep in step with vantage.mixins.json. */
	private static final List<String> TARGETS = List.of(
			"net.minecraft.client.gui.Gui",
			"net.minecraft.client.gui.components.BossHealthOverlay",
			"net.minecraft.client.gui.components.PlayerTabOverlay",
			"net.minecraft.client.gui.contextualbar.LocatorBarRenderer",
			"net.minecraft.client.multiplayer.ClientExplosionTracker",
			"net.minecraft.client.multiplayer.ClientLevel",
			"net.minecraft.client.multiplayer.ClientPacketListener",
			"net.minecraft.client.player.LocalPlayer",
			"net.minecraft.client.renderer.entity.LivingEntityRenderer",
			"net.minecraft.client.renderer.entity.layers.ItemInHandLayer",
			"net.minecraft.client.renderer.entity.state.LivingEntityRenderState",
			"net.minecraft.client.renderer.ItemInHandRenderer",
			"net.minecraft.client.renderer.entity.ItemEntityRenderer",
			"net.minecraft.client.renderer.item.properties.conditional.IsUsingItem",
			"net.minecraft.world.entity.LivingEntity",
			"net.minecraft.client.renderer.entity.state.ItemEntityRenderState",
			"net.minecraft.world.item.ItemStack",
			"net.minecraft.client.gui.screens.DeathScreen",
			"net.minecraft.client.particle.ParticleEngine",
			"net.minecraft.client.sounds.SoundEngine",
			"net.minecraft.server.packs.repository.PackCompatibility",
			"net.minecraft.world.item.component.ItemContainerContents",
			"net.minecraft.client.renderer.LightTexture",
			"net.minecraft.client.renderer.ScreenEffectRenderer",
			"net.minecraft.client.renderer.fog.FogRenderer",
			"net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment",
			"net.minecraft.client.renderer.fog.environment.BlindnessFogEnvironment",
			"net.minecraft.client.renderer.fog.environment.DarknessFogEnvironment",
			"net.minecraft.client.renderer.fog.environment.LavaFogEnvironment",
			"net.minecraft.client.renderer.fog.environment.PowderedSnowFogEnvironment",
			"net.minecraft.client.renderer.fog.environment.WaterFogEnvironment"
	);

	private MixinSelfCheck() {
	}

	static void runIfDevelopment() {
		if (!FabricLoader.getInstance().isDevelopmentEnvironment()) {
			return;
		}

		int failed = 0;
		for (String target : TARGETS) {
			try {
				// initialize=false: defining the class is what triggers the mixin
				// transformer, and skipping the static initialiser avoids touching
				// game state that does not exist this early in startup.
				Class.forName(target, false, MixinSelfCheck.class.getClassLoader());
			} catch (Throwable t) {
				failed++;
				VantageClient.LOGGER.error("Mixin target failed to load: {}", target, t);
			}
		}

		if (failed == 0) {
			VantageClient.LOGGER.info("Mixin self-check passed: {} targets loaded cleanly.", TARGETS.size());
		} else {
			VantageClient.LOGGER.error("Mixin self-check: {} of {} targets failed.", failed, TARGETS.size());
		}
	}
}
