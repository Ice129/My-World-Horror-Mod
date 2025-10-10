package horror.blueice129.client;

import net.fabricmc.api.ClientModInitializer;
import horror.blueice129.HorrorMod129;

/**
 * Client entry point for the horror mod.
 * Handles client-side initialization.
 */
public class HorrorMod129Client implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        HorrorMod129.LOGGER.info("Initializing HorrorMod129 client");
    }
}