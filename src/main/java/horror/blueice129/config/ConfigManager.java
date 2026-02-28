package horror.blueice129.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import horror.blueice129.HorrorMod129;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("ENGRAM.json");
    private static ModConfig instance = null;

    public static ModConfig getConfig() {
        if (instance == null) {
            loadConfig();
        }
        return instance;
    }

    public static void loadConfig() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                String json = Files.readString(CONFIG_PATH);
                instance = GSON.fromJson(json, ModConfig.class);
                HorrorMod129.LOGGER.info("Loaded config from ENGRAM.json");
            } else {
                HorrorMod129.LOGGER.info("Config file not found, creating default ENGRAM.json");
                instance = ModConfig.createDefault();
                saveConfig(instance);
            }
        } catch (Exception e) {
            HorrorMod129.LOGGER.error("Failed to load config, using defaults", e);
            instance = ModConfig.createDefault();
        }
    }

    public static void saveConfig(ModConfig config) {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            String json = GSON.toJson(config);
            Files.writeString(CONFIG_PATH, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            instance = config;
            HorrorMod129.LOGGER.info("Saved config to ENGRAM.json");
        } catch (Exception e) {
            HorrorMod129.LOGGER.error("Failed to save config", e);
        }
    }
}
