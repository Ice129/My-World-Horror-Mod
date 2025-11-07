package horror.blueice129.feature;

import horror.blueice129.utils.PlayerUtils;

import com.mojang.authlib.GameProfile;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.text.Text;

public class LedgePusher {
    private final int minLedgeHeight;
    private PlayerEntity player;
    private World world;
    private BlockPos lastPlayerPos = null;

    public LedgePusher(PlayerEntity player, int minLedgeHeight) {
        this.player = player;
        this.minLedgeHeight = minLedgeHeight;
        this.world = player.getWorld();
    }

    public boolean isPlayerOnLedge() {
        if (!PlayerUtils.isPlayerOnGround(player)) {
            return false; // Player is not on the ground
        }
        if (!PlayerUtils.isPlayerCrouching(player)) {
            return false; // Player is not crouching
        }

        BlockPos playerPos = this.player.getBlockPos();
        // check if the block below the player is air, indicating they are crouching at
        // the very edge of the block
        BlockPos belowPlayerPos = playerPos.down();
        if (!world.isAir(belowPlayerPos)) {
            return false; // Block below player is not air
        }

        // check blocks in front of the player and see if they are on a ledge
        String direction = PlayerUtils.getPlayerCompassDirection(player);
        BlockPos frontPos = PlayerUtils.getRelativeBlockPos(playerPos, direction);
        BlockPos leftBlockPos = PlayerUtils.getRelativeBlockPos(playerPos,
                PlayerUtils.getLeftRightDirection(direction, true));
        BlockPos rightBlockPos = PlayerUtils.getRelativeBlockPos(playerPos,
                PlayerUtils.getLeftRightDirection(direction, false));

        // log the direction and positions being checked
        // HorrorMod129.LOGGER.info("Checking ledge at direction: " + direction);
        // HorrorMod129.LOGGER.info("Front Pos: " + frontPos);
        // HorrorMod129.LOGGER.info("Left Pos: " + leftBlockPos);
        // HorrorMod129.LOGGER.info("Right Pos: " + rightBlockPos);

        BlockPos[] checkPositions = { frontPos, leftBlockPos, rightBlockPos };
        for (BlockPos pos : checkPositions) {
            for (int y = 2; y >= -minLedgeHeight; y--) {
                BlockPos checkPos = pos.add(0, y, 0);

                if (!world.isAir(checkPos)) {
                    return false; // Found a solid block within the ledge height range
                }
                // set block to diamond block for testing
                // world.setBlockState(checkPos,
                // net.minecraft.block.Blocks.DIAMOND_BLOCK.getDefaultState());
            }
        }
        lastPlayerPos = playerPos;
        return true; // No solid blocks found, player is on a ledge
    }

    public boolean didPlayerFall() {
        if (lastPlayerPos == null) {
            return false; // No previous position to compare
        }
        BlockPos currentPos = player.getBlockPos().down(5);
        return currentPos.getY() < lastPlayerPos.getY();
    }

    public void pushPlayer() {
        String direction = PlayerUtils.getPlayerCompassDirection(player);
        double pushStrength = 1.0;
        double[] directionVector = PlayerUtils.getDirectionVector(direction);
        double dx = directionVector[0] * pushStrength;
        double dz = directionVector[1] * pushStrength;
        player.addVelocity(dx, 0.5, dz);

        // Create a fake player with blueice129 name to damage the player
        if (world instanceof ServerWorld) {
            ServerWorld serverWorld = (ServerWorld) world;
            GameProfile profile = new GameProfile(java.util.UUID.nameUUIDFromBytes("Blueice129".getBytes()),
                    "Blueice129");
            FakePlayer fakePlayer = FakePlayer.get(serverWorld, profile);
            // Position the fake player behind the real player (opposite of push direction)
            fakePlayer.setPosition(
                    player.getX() - directionVector[0] * 2,
                    player.getY(),
                    player.getZ() - directionVector[1] * 2);
            // Damage the player as if attacked by the fake player
            player.damage(player.getDamageSources().playerAttack(fakePlayer), 2.0f);
            
            // // Spawn the fleeing entity after pushing the player
            // spawnFleeingEntity(serverWorld, fakePlayer.getPos(), directionVector);
        } else {
            // Fallback for client-side or if we can't create a fake player
            player.damage(player.getDamageSources().generic(), 2.0f);
        }
    }

    // /**
    //  * Spawns an armor stand entity that flees in the given direction and despawns after 5 seconds
    //  */
    // private void spawnFleeingEntity(ServerWorld world, Vec3d position, double[] fleeDirection) {
    //     spawnFleeingEntityStatic(world, position, fleeDirection);
    // }
    
    /**
     * Static version of spawnFleeingEntity that can be called from anywhere
     */
    public static void spawnFleeingEntityStatic(ServerWorld world, Vec3d position, double[] fleeDirection) {
        // Try the simplest possible approach - plain armorstand
        try {
            // HorrorMod129.LOGGER.info("Creating fleeing entity at " + position.toString());
            
            // Create an armor stand as the fleeing entity
            ArmorStandEntity entity = new ArmorStandEntity(world, position.x, position.y, position.z);
            
            // Set entity properties
            entity.setCustomName(Text.literal("Blueice129"));
            entity.setCustomNameVisible(true);
            entity.setInvisible(true);
            entity.setNoGravity(true);

            float speed = 0.15f;
            
            // Set the custom tag via entity UUID
            FLEEING_ENTITIES.put(entity.getUuid(), new FleeingEntityData(80, -fleeDirection[0] * speed, -fleeDirection[1] * speed));

            // Spawn the entity in the world
            boolean success = world.spawnEntity(entity);
            success =  success && entity.isAlive();
            // HorrorMod129.LOGGER.info("Entity spawn success: " + success);
            
            // Now manually set initial velocity
            entity.setVelocity(fleeDirection[0] * 0.25, 0, fleeDirection[1] * 0.25);
        } catch (Exception e) {
            // Log any errors for debugging
            // HorrorMod129.LOGGER.error("Error spawning fleeing entity", e);
        }
    }
    
    // Storage for fleeing entities
    private static final java.util.Map<java.util.UUID, FleeingEntityData> FLEEING_ENTITIES = new java.util.HashMap<>();
    
    // Data class for fleeing entities
    private static class FleeingEntityData {
        private int lifetime;
        private final double dirX;
        private final double dirZ;
        
        public FleeingEntityData(int lifetime, double dirX, double dirZ) {
            this.lifetime = lifetime;
            this.dirX = dirX;
            this.dirZ = dirZ;
        }
    }
    
    // Register this with the ServerTickEvents.START_SERVER_TICK event
    public static void onServerTick(net.minecraft.server.MinecraftServer server) {
        // Create a set of UUIDs to remove after processing
        java.util.Set<java.util.UUID> entitiesToRemove = new java.util.HashSet<>();
        
        // Process all fleeing entities
        for (java.util.Map.Entry<java.util.UUID, FleeingEntityData> entry : FLEEING_ENTITIES.entrySet()) {
            java.util.UUID entityId = entry.getKey();
            FleeingEntityData data = entry.getValue();
            
            // Look for entity in all server worlds
            boolean entityFound = false;
            for (ServerWorld world : server.getWorlds()) {
                ArmorStandEntity entity = (ArmorStandEntity) world.getEntity(entityId);
                if (entity != null) {
                    entityFound = true;
                    
                    // Check lifetime
                    data.lifetime--;
                    if (data.lifetime <= 0) {
                        // Time to despawn
                        entity.discard();
                        entitiesToRemove.add(entityId);
                        break;
                    }
                    
                    // Apply movement directly to position
                    entity.setPosition(
                        entity.getX() + data.dirX,
                        entity.getY(),
                        entity.getZ() + data.dirZ
                    );
                    
                    // Also set velocity to make it look more natural
                    entity.setVelocity(data.dirX, 0, data.dirZ);
                    break;
                }
            }
            
            // If entity wasn't found in any world, it may have been removed
            if (!entityFound) {
                entitiesToRemove.add(entityId);
            }
        }
        
        // Clean up entities that are no longer valid
        for (java.util.UUID id : entitiesToRemove) {
            FLEEING_ENTITIES.remove(id);
        }
    }

}
