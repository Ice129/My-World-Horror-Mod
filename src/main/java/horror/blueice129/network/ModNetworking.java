package horror.blueice129.network;

import horror.blueice129.HorrorMod129;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/**
 * Handles packet registration for client-server communication.
 */
public class ModNetworking {
    
    /**
     * Registers all custom packets for the mod.
     * Should be called during mod initialization.
     */
    public static void registerPackets() {
        // Register the settings trigger payload for client-bound packets
        PayloadTypeRegistry.playS2C().register(SettingsTriggerPayload.ID, SettingsTriggerPayload.CODEC);
        
        HorrorMod129.LOGGER.info("Registered mod networking packets");
    }
    
    /**
     * Sends a settings trigger packet to a specific player.
     * 
     * @param player The player to send the packet to
     * @param settingType The type of setting to modify
     */
    public static void sendSettingsTrigger(net.minecraft.server.network.ServerPlayerEntity player, 
                                          SettingsTriggerPayload.SettingType settingType) {
        ServerPlayNetworking.send(player, new SettingsTriggerPayload(settingType));
        HorrorMod129.LOGGER.info("Sent settings trigger packet to " + player.getName().getString() + 
                                " for setting: " + settingType);
    }
}
