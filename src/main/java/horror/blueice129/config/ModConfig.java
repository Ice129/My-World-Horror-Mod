package horror.blueice129.config;

public class ModConfig {
    public enum SpeedOption {
        SLOWEST,
        SLOWER,
        NORMAL,
        FASTER,
        FASTEST;

        public int getDaysPerAggroLevel() {
            return switch (this) {
                case FASTEST -> 1;
                case FASTER -> 2;
                case NORMAL -> 3;
                case SLOWER -> 4;
                case SLOWEST -> 5;
            };
        }

        @Override
        public String toString() {
            String name = this.name();
            return name.charAt(0) + name.substring(1).toLowerCase();
        }
    }

    public boolean enableSettingsModifications = true;
    public boolean enableRenderDistanceChange = true;
    public boolean enableBrightnessChange = true;
    public boolean enableFpsChange = true;
    public boolean enableMouseSensitivityChange = true;
    public boolean enableSmoothLightingChange = true;
    
    public boolean enableMusicVolumeLocking = true;
    public SpeedOption speedOfProgression = SpeedOption.NORMAL;

    public ModConfig() {}

    public static ModConfig createDefault() {
        return new ModConfig();
    }
}
