package horror.blueice129.config;

import horror.blueice129.scheduler.AgroMeterScheduler;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;

public class ConfigScreen {
    public static Screen createConfigScreen(Screen parent) {
        ModConfig config = ConfigManager.getConfig();
        
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.literal("ENGRAM Horror Mod Config"));
        
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        
        ConfigCategory settingsCategory = builder.getOrCreateCategory(Text.literal("Settings Modifications"));
        
        settingsCategory.addEntry(entryBuilder.startBooleanToggle(Text.literal("Enable All"), config.enableSettingsModifications)
                .setDefaultValue(true)
                .setTooltip(Text.literal("Master toggle for all client setting modifications below"))
                .setSaveConsumer(newValue -> config.enableSettingsModifications = newValue)
                .build());
        
        settingsCategory.addEntry(entryBuilder.startBooleanToggle(Text.literal("Enable Render Distance Changes"), config.enableRenderDistanceChange)
                .setDefaultValue(true)
                .setTooltip(Text.literal("Decreases render distance periodically"))
                .setSaveConsumer(newValue -> config.enableRenderDistanceChange = newValue)
                .build());
        
        settingsCategory.addEntry(entryBuilder.startBooleanToggle(Text.literal("Enable Brightness Changes"), config.enableBrightnessChange)
                .setDefaultValue(true)
                .setTooltip(Text.literal("Sets brightness to moody periodically"))
                .setSaveConsumer(newValue -> config.enableBrightnessChange = newValue)
                .build());
        
        settingsCategory.addEntry(entryBuilder.startBooleanToggle(Text.literal("Enable FPS Changes"), config.enableFpsChange)
                .setDefaultValue(true)
                .setTooltip(Text.literal("Caps FPS to 30 periodically"))
                .setSaveConsumer(newValue -> config.enableFpsChange = newValue)
                .build());
        
        settingsCategory.addEntry(entryBuilder.startBooleanToggle(Text.literal("Enable Mouse Sensitivity Changes"), config.enableMouseSensitivityChange)
                .setDefaultValue(true)
                .setTooltip(Text.literal("Decreases mouse sensitivity periodically"))
                .setSaveConsumer(newValue -> config.enableMouseSensitivityChange = newValue)
                .build());
        
        settingsCategory.addEntry(entryBuilder.startBooleanToggle(Text.literal("Enable Smooth Lighting Changes"), config.enableSmoothLightingChange)
                .setDefaultValue(true)
                .setTooltip(Text.literal("Disables smooth lighting periodically"))
                .setSaveConsumer(newValue -> config.enableSmoothLightingChange = newValue)
                .build());
        
        ConfigCategory audioCategory = builder.getOrCreateCategory(Text.literal("Audio"));
        
        audioCategory.addEntry(entryBuilder.startBooleanToggle(Text.literal("Enable Music Volume Locking"), config.enableMusicVolumeLocking)
                .setDefaultValue(true)
                .setTooltip(Text.literal("Locks music volume to minimum 50%"))
                .setSaveConsumer(newValue -> config.enableMusicVolumeLocking = newValue)
                .build());

        ConfigCategory multiplayerCategory = builder.getOrCreateCategory(Text.literal("Multiplayer"));

        multiplayerCategory.addEntry(entryBuilder.startIntField(
                        Text.literal("Auto LAN Port (change this to make LAN multiplayer work)"),
                        config.autoLanPort)
                .setDefaultValue(ModConfig.DEFAULT_AUTO_LAN_PORT)
                .setMin(ModConfig.MIN_AUTO_LAN_PORT)
                .setMax(ModConfig.MAX_AUTO_LAN_PORT)
                .setTooltip(Text.literal("Port used when auto-opening singleplayer to LAN"))
                .setSaveConsumer(newValue -> config.autoLanPort = newValue)
                .build());
        
        ConfigCategory speedCategory = builder.getOrCreateCategory(Text.literal("Progression"));
        // multiple string options: slowest, slower, normal, faster, fastest
        speedCategory.addEntry(entryBuilder.startEnumSelector(Text.literal("Speed"), ModConfig.SpeedOption.class, config.speedOfProgression)
                .setDefaultValue(ModConfig.SpeedOption.NORMAL)
                .setTooltip(Text.literal("Adjusts how quickly the mod progresses through stages"))
                .setSaveConsumer(newValue -> config.speedOfProgression = newValue)
                .build());
        
        builder.setSavingRunnable(() -> {
            ConfigManager.saveConfig(config);

            MinecraftServer server = MinecraftClient.getInstance().getServer();
            if (server != null) {
                server.execute(() -> AgroMeterScheduler.recalculateAggroForCurrentDay(server));
            }
        });
        
        return builder.build();
    }
}
