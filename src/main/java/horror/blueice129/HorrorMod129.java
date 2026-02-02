package horror.blueice129;

import horror.blueice129.command.DebugCommands;
import horror.blueice129.entity.Blueice129Entity;
import horror.blueice129.network.ModNetworking;
import horror.blueice129.scheduler.AgroMeterScheduler;
import horror.blueice129.scheduler.Blueice129SpawnScheduler;
import horror.blueice129.scheduler.CaveMinerScheduler;
import horror.blueice129.scheduler.HomeEventScheduler;
import horror.blueice129.scheduler.PlayerDeathItemsScheduler;
import horror.blueice129.scheduler.SmallStructureScheduler;
import horror.blueice129.scheduler.LedgePusherScheduler;
import horror.blueice129.scheduler.SettingsScheduler;
import horror.blueice129.scheduler.OnWorldCreation;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HorrorMod129 implements ModInitializer {
	public static final String MOD_ID = "horror-mod-129";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/**
	 * Blueice129 Entity Type registration.
	 * This entity is a PathAwareEntity that looks like a player with Blueice129's skin.
	 */
	public static final EntityType<Blueice129Entity> BLUEICE129_ENTITY = Registry.register(
		Registries.ENTITY_TYPE,
		new Identifier(MOD_ID, "blueice129"),
		EntityType.Builder.create(Blueice129Entity::new, SpawnGroup.CREATURE)
			.setDimensions(0.6f, 1.8f) // Player dimensions
			.build("blueice129")
	);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.


		LOGGER.info("Initializing Horror Mod 129!");
		
		// Register networking packets
		ModNetworking.registerPackets();
		
		// Register entity attributes
		FabricDefaultAttributeRegistry.register(BLUEICE129_ENTITY, Blueice129Entity.createBlueice129Attributes());
		
		// Register schedulers
		OnWorldCreation.register();
		AgroMeterScheduler.register();
		Blueice129SpawnScheduler.register();
		CaveMinerScheduler.register();
		HomeEventScheduler.register();
		SmallStructureScheduler.register();
		LedgePusherScheduler.register();
		PlayerDeathItemsScheduler.register();
		SettingsScheduler.register(); // Now server-side
		
		// Register fleeing entity tick handler
		ServerTickEvents.START_SERVER_TICK.register(horror.blueice129.feature.LedgePusher::onServerTick);
		
		// Register debug commands
		DebugCommands.register();

		LOGGER.info("Horror Mod 129 initialization complete!");
	}
}