package horror.blueice129;

import horror.blueice129.command.DebugCommands;
import horror.blueice129.scheduler.CaveMinerScheduler;
import horror.blueice129.scheduler.HomeEventScheduler;
import horror.blueice129.scheduler.PlayerDeathItemsScheduler;
import horror.blueice129.scheduler.SmallStructureScheduler;
import horror.blueice129.scheduler.LedgePusherScheduler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HorrorMod129 implements ModInitializer {
	public static final String MOD_ID = "horror-mod-129";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.


		LOGGER.info("Initializing Horror Mod 129!");
		
		// Register schedulers
		CaveMinerScheduler.register();
		HomeEventScheduler.register();
		SmallStructureScheduler.register();
		LedgePusherScheduler.register();
		PlayerDeathItemsScheduler.register();
		// Note: SettingsScheduler is registered in HorrorMod129Client as it runs on client side
		
		// Register fleeing entity tick handler
		ServerTickEvents.START_SERVER_TICK.register(horror.blueice129.feature.LedgePusher::onServerTick);
		
		// Register debug commands
		DebugCommands.register();

		LOGGER.info("Horror Mod 129 initialization complete!");
	}
}