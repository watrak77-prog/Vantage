package pl.watrak.vantage.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Loads and stores the single {@link VantageConfig} instance backed by
 * {@code config/vantage.json}.
 */
public final class ConfigManager {

	private static final Logger LOGGER = LoggerFactory.getLogger("Vantage/Config");
	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.disableHtmlEscaping()
			.create();

	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("vantage.json");

	private static VantageConfig config = new VantageConfig();

	private ConfigManager() {
	}

	public static VantageConfig get() {
		return config;
	}

	public static void load() {
		if (!Files.isRegularFile(FILE)) {
			LOGGER.info("No config file yet, writing defaults to {}", FILE);
			config = new VantageConfig();
			save();
			return;
		}

		try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
			VantageConfig loaded = GSON.fromJson(reader, VantageConfig.class);
			// Gson hands back null for an empty or literally-"null" file.
			config = loaded != null ? loaded : new VantageConfig();
		} catch (IOException | JsonSyntaxException e) {
			// A corrupt config must never stop the game from starting. Keep the
			// broken file around so the user can rescue hand-made edits.
			LOGGER.error("Could not read {}, falling back to defaults", FILE, e);
			backupCorruptFile();
			config = new VantageConfig();
		}

		config.sanitise();
	}

	public static void save() {
		config.sanitise();

		try {
			Files.createDirectories(FILE.getParent());
			// Write to a sibling first so a crash mid-write cannot leave a
			// truncated config behind.
			Path tmp = FILE.resolveSibling(FILE.getFileName() + ".tmp");

			try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
				GSON.toJson(config, writer);
			}

			try {
				Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			LOGGER.error("Could not write {}", FILE, e);
		}
	}

	private static void backupCorruptFile() {
		Path broken = FILE.resolveSibling("vantage.json.broken");
		try {
			Files.move(FILE, broken, StandardCopyOption.REPLACE_EXISTING);
			LOGGER.error("Moved the unreadable config to {}", broken);
		} catch (IOException e) {
			LOGGER.error("Could not preserve the unreadable config", e);
		}
	}
}
