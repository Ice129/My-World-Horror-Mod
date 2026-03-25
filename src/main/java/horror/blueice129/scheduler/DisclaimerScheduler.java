package horror.blueice129.scheduler;

import horror.blueice129.HorrorMod129;
import horror.blueice129.client.screen.DisclaimerScreen;
import horror.blueice129.data.HorrorModPersistentState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.MinecraftServer;

@Environment(EnvType.CLIENT)
public class DisclaimerScheduler {
    private static boolean disclaimerShown = false;
    private static int delayCounter = 0;

    public static void initialize() {
        HorrorMod129.LOGGER.info("DisclaimerScheduler initialized");
        
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Only check if we're in a world
            if (client.world == null || client.player == null) return;

            // Only check for single player
            if (!client.isInSingleplayer()) return;
            
            checkAndShowDisclaimer(client);
        });
    }

    private static void checkAndShowDisclaimer(MinecraftClient client) {
        if (disclaimerShown) return;
        
        HorrorMod129.LOGGER.info("Checking for disclaimer...");
        
        // Get the integrated server for single-player
        MinecraftServer server = client.getServer();
        if (server == null) {
            HorrorMod129.LOGGER.warn("Server is null");
            return;
        }
        
        // Get persistent state from server
        HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
        if (state == null) {
            HorrorMod129.LOGGER.warn("Persistent state is null");
            return;
        }

        String disclaimerKey = HorrorMod129.MOD_ID + "_singleplayer_disclaimer_shown";
        int disclaimerValue = state.getIntValue(disclaimerKey, 0);
        HorrorMod129.LOGGER.info("Disclaimer shown flag: " + disclaimerValue);
        
        // Check if disclaimer has been shown
        if (disclaimerValue == 1) {
            disclaimerShown = true;
            return; // Already shown
        }

        // Add a small delay to ensure the world is fully loaded and no other screens are being set
        if (delayCounter < 5) {
            delayCounter++;
            return;
        }

        HorrorMod129.LOGGER.info("Opening disclaimer screen");
        // Show disclaimer
        client.setScreen(new DisclaimerScreen(null));
        
        // Mark as shown in persistent state
        state.setIntValue(disclaimerKey, 1);
        disclaimerShown = true;
    }
}
