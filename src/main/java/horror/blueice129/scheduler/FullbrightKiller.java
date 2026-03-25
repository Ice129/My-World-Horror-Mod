package horror.blueice129.scheduler;

import horror.blueice129.HorrorMod129;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Enforces a maximum gamma value every tick
 * This ensures the player cannot set gamma above 100% during gameplay
 * The check runs every second (20 ticks)
 */
@Environment(EnvType.CLIENT)
public class FullbrightKiller {
    private static final double MAX_ALLOWED_GAMMA = 0.7;
    private static final double ENFORCED_GAMMA = 0.6;
    private static final int TICK_CHECK_INTERVAL = 20; // Check every second
    // private static final int HEARTBEAT_INTERVAL_TICKS = 20 * 30; // Log every 30 seconds
    private static final String GREENMAN_FULLBRIGHT_MOD_ID = "fullbright";
    private static final String GREENMAN_FULLBRIGHT_CONFIG_CLASS = "de.greenman999.fullbright.FullbrightConfig";
    private static final String N8M4_FULLBRIGHT_CLIENT_CLASS = "de.n8M4.fullbright.client.FullbrightClient";
    private static final String GAMMA_UTILS_MOD_ID = "gammautils";
    private static final String GAMMA_UTILS_CLASS = "io.github.sjouwer.gammautils.GammaUtils";
    private static final String GAMMA_UTILS_OPTIONS_CLASS = "io.github.sjouwer.gammautils.GammaOptions";
    private static final String GJEB_MOD_ID = "gjeb";

    private static boolean isInitialized = false;
    private static int tickCounter = 0;
    // private static int heartbeatCounter = 0;

    // Greenman fullbright integration
    private static Method isToggledMethod;
    private static Method toggleMethod;
    private static boolean greenmanReflectionInitFailed;

    // n8M4 fullbright integration
    private static Field n8m4FullbrightField;
    private static boolean n8m4ReflectionInitFailed;

    // Gamma Utils integration
    private static Method gammaUtilsGetConfigMethod;
    private static Method gammaUtilsSaveConfigMethod;
    private static Method gammaUtilsSetGammaMethod;
    private static Method gammaUtilsGetGammaMethod;
    private static Method gammaUtilsIsNightVisionEnabledMethod;
    private static Method gammaUtilsSetNightVisionMethod;
    private static Field gammaUtilsBrightEffectField;
    private static Field gammaUtilsDimEffectField;
    private static boolean gammaUtilsReflectionInitFailed;

    // GJEB integration
    private static boolean gjebDetected;

    /**
     * Registers the tick event that enforces gamma limits
     * Should be called during client initialization
     */
    public static void initialize() {
        if (isInitialized) {
            HorrorMod129.LOGGER.warn("FullbrightKiller already initialized, skipping...");
            return;
        }

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Only run if in-game and not paused
            if (client.world == null) {
                return;
            }

            if (tickCounter > 0) {
                tickCounter--;
                return;
            }

            disableGreenmanFullbright();
            disableN8m4Fullbright();
            disableGammaUtilsFullbright(client);
            disableGjebFullbright(client);

            double currentGamma = client.options.getGamma().getValue();

            // HorrorMod129.LOGGER.debug("FullbrightKiller: Current gamma: " + currentGamma);
            if (currentGamma > MAX_ALLOWED_GAMMA) {
                client.options.getGamma().setValue(ENFORCED_GAMMA);
                client.options.write();
                HorrorMod129.LOGGER.info("FullbrightKiller: Detected gamma above 100%, resetting to 70%" + " (was " + currentGamma + ")");
            }

            tickCounter = TICK_CHECK_INTERVAL;
        });

        isInitialized = true;
        HorrorMod129.LOGGER.info("FullbrightKiller initialized - enforcing gamma cap every second");
    }

    private static void disableGreenmanFullbright() {
        if (!FabricLoader.getInstance().isModLoaded(GREENMAN_FULLBRIGHT_MOD_ID)) {
            return;
        }

        if (greenmanReflectionInitFailed) {
            return;
        }

        try {
            if (isToggledMethod == null || toggleMethod == null) {
                Class<?> configClass = Class.forName(GREENMAN_FULLBRIGHT_CONFIG_CLASS);
                isToggledMethod = configClass.getMethod("isToggled");
                toggleMethod = configClass.getMethod("toggle");
            }

            boolean toggled = (boolean) isToggledMethod.invoke(null);
            if (toggled) {
                toggleMethod.invoke(null);
                HorrorMod129.LOGGER.warn("FullbrightKiller: Disabled external fullbright toggle from mod 'fullbright'");
            }
        } catch (ReflectiveOperationException e) {
            greenmanReflectionInitFailed = true;
            HorrorMod129.LOGGER.warn("FullbrightKiller: Failed to control external fullbright mod state", e);
        }
    }

    private static void disableN8m4Fullbright() {
        if (!FabricLoader.getInstance().isModLoaded(GREENMAN_FULLBRIGHT_MOD_ID)) {
            return;
        }

        if (n8m4ReflectionInitFailed) {
            return;
        }

        try {
            if (n8m4FullbrightField == null) {
                Class<?> clientClass = Class.forName(N8M4_FULLBRIGHT_CLIENT_CLASS);
                n8m4FullbrightField = clientClass.getField("fullbright");
            }

            boolean enabled = (boolean) n8m4FullbrightField.get(null);
            if (enabled) {
                n8m4FullbrightField.setBoolean(null, false);
                HorrorMod129.LOGGER.warn("FullbrightKiller: Disabled external fullbright toggle from mod 'de.n8M4.fullbright'");
            }
        } catch (ReflectiveOperationException e) {
            n8m4ReflectionInitFailed = true;
            HorrorMod129.LOGGER.warn("FullbrightKiller: Failed to control de.n8M4 fullbright state", e);
        }
    }

    private static void disableGammaUtilsFullbright(MinecraftClient client) {
        if (!FabricLoader.getInstance().isModLoaded(GAMMA_UTILS_MOD_ID)) {
            return;
        }

        if (gammaUtilsReflectionInitFailed) {
            return;
        }

        try {
            if (gammaUtilsGetConfigMethod == null) {
                Class<?> gammaUtilsClass = Class.forName(GAMMA_UTILS_CLASS);
                Class<?> gammaOptionsClass = Class.forName(GAMMA_UTILS_OPTIONS_CLASS);

                gammaUtilsGetConfigMethod = gammaUtilsClass.getMethod("getConfig");
                gammaUtilsSaveConfigMethod = gammaUtilsClass.getMethod("saveConfig");
                gammaUtilsBrightEffectField = gammaUtilsClass.getField("BRIGHT");
                gammaUtilsDimEffectField = gammaUtilsClass.getField("DIM");

                gammaUtilsSetGammaMethod = gammaOptionsClass.getMethod("setGamma", double.class, boolean.class);
                gammaUtilsGetGammaMethod = gammaOptionsClass.getMethod("getGamma");
            }

            Object config = gammaUtilsGetConfigMethod.invoke(null);
            if (gammaUtilsSetNightVisionMethod == null) {
                gammaUtilsIsNightVisionEnabledMethod = config.getClass().getMethod("isNightVisionEnabled");
                gammaUtilsSetNightVisionMethod = config.getClass().getMethod("setNightVision", boolean.class);
            }

            double gammaUtilsGamma = (double) gammaUtilsGetGammaMethod.invoke(null);
            if (gammaUtilsGamma > MAX_ALLOWED_GAMMA) {
                gammaUtilsSetGammaMethod.invoke(null, ENFORCED_GAMMA, false);
                HorrorMod129.LOGGER.warn("FullbrightKiller: Clamped Gamma Utils gamma from " + gammaUtilsGamma + " to " + ENFORCED_GAMMA);
            }

            boolean nightVisionEnabled = (boolean) gammaUtilsIsNightVisionEnabledMethod.invoke(config);
            if (nightVisionEnabled) {
                gammaUtilsSetNightVisionMethod.invoke(config, false);
                gammaUtilsSaveConfigMethod.invoke(null);
                HorrorMod129.LOGGER.warn("FullbrightKiller: Disabled Gamma Utils night vision mode");
            }

            if (client.player != null) {
                boolean removedAny = false;

                removedAny |= client.player.removeStatusEffect(StatusEffects.NIGHT_VISION);

                StatusEffect brightEffect = (StatusEffect) gammaUtilsBrightEffectField.get(null);
                StatusEffect dimEffect = (StatusEffect) gammaUtilsDimEffectField.get(null);
                removedAny |= client.player.removeStatusEffect(brightEffect);
                removedAny |= client.player.removeStatusEffect(dimEffect);

                if (removedAny) {
                    HorrorMod129.LOGGER.warn("FullbrightKiller: Removed Gamma Utils status-effect based brightness boost");
                }
            }
        } catch (ReflectiveOperationException e) {
            gammaUtilsReflectionInitFailed = true;
            HorrorMod129.LOGGER.warn("FullbrightKiller: Failed to control Gamma Utils state", e);
        }
    }

    private static void disableGjebFullbright(MinecraftClient client) {
        if (!FabricLoader.getInstance().isModLoaded(GJEB_MOD_ID)) {
            return;
        }

        if (!gjebDetected) {
            gjebDetected = true;
            HorrorMod129.LOGGER.info("FullbrightKiller: Detected GJEB mod - enforcing vanilla gamma limits");
        }

        double gamma = client.options.getGamma().getValue();
        if (gamma > MAX_ALLOWED_GAMMA) {
            client.options.getGamma().setValue(ENFORCED_GAMMA);
            client.options.write();
            HorrorMod129.LOGGER.warn("FullbrightKiller: Clamped GJEB gamma from " + gamma + " to " + ENFORCED_GAMMA);
        }
    }
}
