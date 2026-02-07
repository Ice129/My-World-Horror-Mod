package horror.blueice129.config;

public class ModConfig {
    public boolean enableSettingsModifications = true;
    public boolean enableRenderDistanceChange = true;
    public boolean enableBrightnessChange = true;
    public boolean enableFpsChange = true;
    public boolean enableMouseSensitivityChange = true;
    public boolean enableSmoothLightingChange = true;
    
    public boolean enableMusicVolumeLocking = true;

    public ModConfig() {}

    public static ModConfig createDefault() {
        return new ModConfig();
    }
}
