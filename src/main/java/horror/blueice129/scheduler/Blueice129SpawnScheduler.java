package horror.blueice129.scheduler;

import horror.blueice129.HorrorMod129;
import horror.blueice129.data.HorrorModPersistentState;
import horror.blueice129.entity.Blueice129Entity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
// import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import net.minecraft.util.math.random.Random;

/**
 * Scheduler for natural Blueice129 entity spawning.
 * Spawns the entity in forest biomes with a chance based on the agro meter.
 * Occurs every 10-30 minutes of playtime.
 */
public class Blueice129SpawnScheduler {
    private static final String TIMER_ID = "blueice129_spawn_timer";
    private static final Random RANDOM = Random.create();

    private static final int MIN_DELAY = 20 * 60 * 10; // 10 minutes
    private static final int MAX_DELAY = 20 * 60 * 30; // 30 minutes

    private static final int MIN_SPAWN_DISTANCE = 40;
    private static final int MAX_SPAWN_DISTANCE = 100;

    /**
     * Gets a random delay for the next spawn attempt.
     * 
     * @return Random delay in ticks
     */
    private static int getRandomDelay() {
        return MIN_DELAY + RANDOM.nextInt(MAX_DELAY - MIN_DELAY);
    }

    /**
     * Calculates spawn chance based on agro meter (0-10).
     * 
     * @param agroMeter The current agro meter value
     * @return Spawn chance as a decimal (0.0 to 1.0)
     */
    private static double getSpawnChance(int agroMeter) {
        // 0% at agro 0, 10% per agro level, 100% at agro 10
        return Math.min(1.0, agroMeter * 0.1);
    }

    /**
     * Server tick handler to check and attempt entity spawning.
     * 
     * @param server The Minecraft server instance
     */
    private static void onServerTick(MinecraftServer server) {
        HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
        if (!state.hasTimer(TIMER_ID)) {
            return; // Timer not initialized yet
        }

        int timer = state.getTimer(TIMER_ID);
        if (timer > 0) {
            state.setTimer(TIMER_ID, timer - 1);
        } else {
            // Timer reached zero, attempt to spawn
            attemptSpawn(server);

            // Reset timer regardless of spawn success
            state.setTimer(TIMER_ID, getRandomDelay());
        }
    }

    /**
     * Attempts to spawn a Blueice129 entity near a random player in a forest biome.
     * 
     * @param server The Minecraft server instance
     */
    private static void attemptSpawn(MinecraftServer server) {
        // Check if there are any players online
        if (server.getPlayerManager().getPlayerList().isEmpty()) {
            HorrorMod129.LOGGER.info("Blueice129 spawn attempt: No players online");
            return;
        }

        ServerWorld world = server.getOverworld();

        // Check if an entity can spawn (only one at a time)
        if (!Blueice129Entity.canSpawn(world)) {
            HorrorMod129.LOGGER.info("Blueice129 spawn attempt: Entity already exists in world");
            return;
        }

        // Get agro meter and calculate spawn chance
        HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
        int agroMeter = state.getIntValue("agroMeter", 0);
        double spawnChance = getSpawnChance(agroMeter);

        // Roll for spawn chance
        if (RANDOM.nextDouble() > spawnChance) {
            HorrorMod129.LOGGER.info("Blueice129 spawn attempt: Failed spawn chance roll ({}% chance, agro: {})",
                    (int) (spawnChance * 100), agroMeter);
            return;
        }

        // Select a random player to spawn near
        // INFO: works with multiplayer
        ServerPlayerEntity player = server.getPlayerManager().getPlayerList()
                .get(RANDOM.nextInt(server.getPlayerManager().getPlayerList().size()));

        // Try to find a suitable spawn location in a forest biome
        BlockPos spawnPos = findForestSpawnLocation(world, player.getBlockPos());

        if (spawnPos == null) {
            HorrorMod129.LOGGER.info("Blueice129 spawn attempt: No suitable forest biome found near player {}",
                    player.getName().getString());
            return;
        }

        // Spawn the entity
        spawnEntity(world, spawnPos, server);
    }

    /**
     * Finds a suitable spawn location in a forest biome near the player.
     * 
     * @param world     The server world
     * @param playerPos The player's position
     * @return A safe spawn position, or null if none found
     */
    private static BlockPos findForestSpawnLocation(ServerWorld world, BlockPos playerPos) {
        // Try multiple random positions
        for (int attempt = 0; attempt < 20; attempt++) {
            // Random angle and distance
            double angle = RANDOM.nextDouble() * 2 * Math.PI;
            int distance = MIN_SPAWN_DISTANCE + RANDOM.nextInt(MAX_SPAWN_DISTANCE - MIN_SPAWN_DISTANCE);

            int offsetX = (int) (Math.cos(angle) * distance);
            int offsetZ = (int) (Math.sin(angle) * distance);

            BlockPos testPos = playerPos.add(offsetX, 0, offsetZ);

            // Check if this position is in a forest biome
            RegistryEntry<Biome> biomeEntry = world.getBiome(testPos);
            String biomeKey = biomeEntry.getKey().map(key -> key.getValue().toString()).orElse("");

            if (!isForestBiome(biomeKey)) {
                continue;
            }

            // Find the surface at this position
            BlockPos surfacePos = world.getTopPosition(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                    testPos);

            // Verify it's a safe spawn location
            if (isSafeSpawnLocation(world, surfacePos)) {
                return surfacePos;
            }
        }

        return null;
    }

    /**
     * Checks if a biome key indicates a forest biome.
     * 
     * @param biomeKey The biome registry key as a string
     * @return true if it's a forest biome
     */
    private static boolean isForestBiome(String biomeKey) {
        return biomeKey.contains("forest") ||
                biomeKey.contains("taiga") ||
                biomeKey.contains("grove") ||
                biomeKey.contains("jungle");
    }

    /**
     * Checks if a position is safe for entity spawning.
     * 
     * @param world The server world
     * @param pos   The position to check
     * @return true if it's safe to spawn
     */
    private static boolean isSafeSpawnLocation(ServerWorld world, BlockPos pos) {
        // Check if the block below is solid
        if (!world.getBlockState(pos.down()).isSolidBlock(world, pos.down())) {
            return false;
        }

        // Check if there's enough space (2 blocks high)
        if (!world.getBlockState(pos).isAir() || !world.getBlockState(pos.up()).isAir()) {
            return false;
        }

        // Don't spawn in liquid
        if (!world.getBlockState(pos).getFluidState().isEmpty() ||
                !world.getBlockState(pos.down()).getFluidState().isEmpty()) {
            return false;
        }

        return true;
    }

    /**
     * Spawns the Blueice129 entity at the specified location.
     * 
     * @param world  The server world
     * @param pos    The spawn position
     * @param server The Minecraft server instance
     */
    private static void spawnEntity(ServerWorld world, BlockPos pos, MinecraftServer server) {
        Blueice129Entity entity = HorrorMod129.BLUEICE129_ENTITY.create(world);

        if (entity == null) {
            HorrorMod129.LOGGER.error("Failed to create Blueice129Entity instance");
            return;
        }

        // Set position
        entity.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                RANDOM.nextFloat() * 360.0f, 0.0f);

        //TODO: make join message consistant with login and logout, so no double join without leave
        // Spawn the entity in the world
        if (world.spawnEntity(entity)) {
            // Broadcast join message
            server.getPlayerManager().broadcast(
                    net.minecraft.text.Text.literal("Blueice129 joined the game")
                            .styled(style -> style.withColor(0xFFFF55)),
                    false);

            HorrorMod129.LOGGER.info("Blueice129 entity spawned at {} {} {}",
                    pos.getX(), pos.getY(), pos.getZ());
        } else {
            HorrorMod129.LOGGER.warn("Failed to spawn Blueice129 entity at {} {} {}",
                    pos.getX(), pos.getY(), pos.getZ());
        }
    }

    /**
     * Registers the tick event to handle the spawn scheduling.
     * This should be called during mod initialization.
     */
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(Blueice129SpawnScheduler::onServerTick);

        // Register server world loading event to initialize timer if needed
        ServerWorldEvents.LOAD.register((server, world) -> {
            if (world.isClient())
                return;
            if (world.getRegistryKey() == World.OVERWORLD) {
                HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);

                // If the timer is not set, initialize it
                if (!state.hasTimer(TIMER_ID)) {
                    state.setTimer(TIMER_ID, getRandomDelay());
                    HorrorMod129.LOGGER.info("Blueice129SpawnScheduler initialized with timer: {} ticks",
                            state.getTimer(TIMER_ID));
                }
            }
        });

        HorrorMod129.LOGGER.info("Registered Blueice129SpawnScheduler");
    }

    /**
     * Debug helper to set the spawn timer directly (in ticks).
     * 
     * @param server The Minecraft server instance
     * @param ticks  The timer value in ticks
     */
    public static void setTimer(MinecraftServer server, int ticks) {
        HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
        state.setTimer(TIMER_ID, Math.max(ticks, 1));
        HorrorMod129.LOGGER.info("Blueice129SpawnScheduler timer set to {} ticks via debug command",
                state.getTimer(TIMER_ID));
    }

    /**
     * Debug helper to get the current spawn chance.
     * 
     * @param server The Minecraft server instance
     * @return The current spawn chance as a percentage string
     */
    public static String getSpawnChanceString(MinecraftServer server) {
        HorrorModPersistentState state = HorrorModPersistentState.getServerState(server);
        int agroMeter = state.getIntValue("agroMeter", 0);
        double spawnChance = getSpawnChance(agroMeter);
        return String.format("%d%% (agro: %d)", (int) (spawnChance * 100), agroMeter);
    }

    /**
     * Debug helper to force a spawn attempt.
     * 
     * @param server The Minecraft server instance
     * @param player The player to spawn near
     * @return The spawn position if successful, null otherwise
     */
    public static BlockPos forceSpawn(MinecraftServer server, ServerPlayerEntity player) {
        ServerWorld world = server.getOverworld();

        // Check if an entity can spawn
        if (!Blueice129Entity.canSpawn(world)) {
            return null;
        }

        // Find spawn location
        BlockPos spawnPos = findForestSpawnLocation(world, player.getBlockPos());

        if (spawnPos == null) {
            return null;
        }

        // Spawn the entity
        spawnEntity(world, spawnPos, server);
        return spawnPos;
    }
}
