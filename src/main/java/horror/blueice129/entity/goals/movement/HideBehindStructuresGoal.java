package horror.blueice129.entity.goals.movement;

import horror.blueice129.HorrorMod129;
import horror.blueice129.entity.Blueice129Entity;
import horror.blueice129.entity.goals.BaseBlueice129Goal;
import horror.blueice129.utils.LineOfSightUtils;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.EnumSet;

/**
 * Goal that makes the entity hide from the player's sight while staying close enough to stalk.
 * 
 * Behavior:
 * - Finds the nearest position where the entity's 2-block-tall body is hidden from player view
 * - Stays within stalking distance of the player
 * - Crouches when hidden and stationary
 * - Looks at the player when hidden
 */
public class HideBehindStructuresGoal extends BaseBlueice129Goal {
    
    private static final int STALK_RADIUS = 30;
    private static final int SEARCH_RADIUS = 25;
    private static final int MIN_DISTANCE_FROM_PLAYER = 8;
    private static final int CROUCH_UPDATE_INTERVAL = 6;
    
    private PlayerEntity targetPlayer;
    private BlockPos hidingSpot;
    private int tickCounter = 0;
    
    public HideBehindStructuresGoal(Blueice129Entity entity) {
        super(entity);
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }
    
    @Override
    protected boolean shouldStart() {
        return isInState(Blueice129Entity.EntityState.SURFACE_HIDING);
    }
    
    @Override
    protected boolean shouldKeepRunning() {
        return isInState(Blueice129Entity.EntityState.SURFACE_HIDING);
    }
    
    @Override
    protected void onStart() {
        targetPlayer = null;
        hidingSpot = null;
        tickCounter = 0;
    }
    
    @Override
    protected void onStop() {
        entity.setSneaking(false);
        entity.getNavigation().stop();
        targetPlayer = null;
        hidingSpot = null;
    }
    
    @Override
    public void tick() {
        // Find nearest player
        targetPlayer = entity.getWorld().getClosestPlayer(entity, STALK_RADIUS * 2);
        if (targetPlayer == null) {
            return;
        }
        
        // Increment tick counter
        tickCounter++;
        
        // If player is crouching, only update visibility every 15 ticks
        boolean shouldUpdate = !targetPlayer.isSneaking() || (tickCounter % CROUCH_UPDATE_INTERVAL == 0);
        
        BlockPos entityPos = entity.getBlockPos();
        boolean isStationary = entity.getNavigation().isIdle();
        
        // Always look at player when stationary (every tick)
        if (isStationary) {
            lookAtPlayer();
        }
        
        // Only perform visibility checks and pathfinding updates when shouldUpdate is true
        if (shouldUpdate) {
            boolean isHidden = isPositionHidden(entityPos);
            
            // HorrorMod129.LOGGER.info(String.format(
            //     "[HideBehindStructures] EntityPos: %s | HidingSpot: %s | IsHidden: %b | IsStationary: %b | PlayerPos: %s",
            //     entityPos.toShortString(), 
            //     hidingSpot != null ? hidingSpot.toShortString() : "null",
            //     isHidden, 
            //     isStationary,
            //     targetPlayer.getBlockPos().toShortString()
            // ));
            
            // Handle behavior based on current state
            if (isHidden && isStationary) {
                // Already hidden and not moving - crouch
                entity.setSneaking(true);
                HorrorMod129.LOGGER.info("[HideBehindStructures] Action: Crouching and watching player");
            } else if (isHidden && !isStationary) {
                // Moving to hiding spot - stand up for natural movement
                entity.setSneaking(false);
                HorrorMod129.LOGGER.info("[HideBehindStructures] Action: Moving to hiding spot (standing)");
            } else {
                // Not hidden - find a hiding spot and move there
                entity.setSneaking(false);
                
                if (hidingSpot == null || !isPositionHidden(hidingSpot)) {
                    HorrorMod129.LOGGER.info("[HideBehindStructures] Searching for new hiding spot...");
                    hidingSpot = findNearestHidingSpot();
                    if (hidingSpot != null) {
                        HorrorMod129.LOGGER.info(String.format(
                            "[HideBehindStructures] Found hiding spot at %s (distance from player: %.1f blocks)",
                            hidingSpot.toShortString(),
                            Math.sqrt(hidingSpot.getSquaredDistance(targetPlayer.getBlockPos()))
                        ));
                    } else {
                        HorrorMod129.LOGGER.info("[HideBehindStructures] No valid hiding spot found!");
                    }
                }
                
                if (hidingSpot != null) {
                    moveToHidingSpot();
                }
            }
        }
        
        // Re-evaluate hiding spot if player moved significantly (only if we should update)
        if (shouldUpdate && hidingSpot != null && targetPlayer != null) {
            double distanceToPlayer = hidingSpot.getSquaredDistance(targetPlayer.getBlockPos());
            if (distanceToPlayer > STALK_RADIUS * STALK_RADIUS) {
                HorrorMod129.LOGGER.info("[HideBehindStructures] Hiding spot too far from player, clearing");
                hidingSpot = null; // Too far from player, need new spot
            }
        }
    }
    
    /**
     * Check if a position would hide the entity from the player's view.
     * Checks both blocks the entity would occupy (feet and head level).
     * 
     * @param feetPos The position where the entity's feet are (same as entity.getBlockPos())
     * @return true if both feet and head blocks are hidden from player view
     */
    private boolean isPositionHidden(BlockPos feetPos) {
        if (targetPlayer == null) {
            return false;
        }
        
        BlockPos headPos = feetPos.up(); // Head is one block above feet
        
        // Both positions must be hidden from player view
        // Use hasLineOfSight which properly handles air blocks
        boolean feetVisible = LineOfSightUtils.hasLineOfSight(
            targetPlayer, feetPos, STALK_RADIUS * 2);
        boolean headVisible = LineOfSightUtils.hasLineOfSight(
            targetPlayer, headPos, STALK_RADIUS * 2);
        
        boolean isHidden = !feetVisible && !headVisible;
        
        // HorrorMod129.LOGGER.info(String.format(
        //     "[HideBehindStructures] Visibility check at %s: FeetVisible=%b, HeadVisible=%b, IsHidden=%b",
        //     feetPos.toShortString(), feetVisible, headVisible, isHidden
        // ));
        
        return isHidden;
    }
    
    /**
     * Check if the player is looking towards the entity (within 120-degree cone).
     * @return true if player's look direction is within 60 degrees of the direction to the entity
     */
    private boolean isPlayerLookingAtEntity() {
        if (targetPlayer == null) {
            return false;
        }
        
        // Get player's look direction
        Vec3d playerLook = targetPlayer.getRotationVec(1.0f).normalize();
        
        // Get direction from player to entity
        Vec3d toEntity = entity.getPos().subtract(targetPlayer.getPos()).normalize();
        
        // Calculate dot product (cosine of angle between vectors)
        double dotProduct = playerLook.dotProduct(toEntity);
        
        // cos(60°) = 0.5, so if dot product > 0.5, angle is less than 60° (within 120° cone)
        return dotProduct > 0.5;
    }
    
    /**
     * Find the best valid hiding spot.
     * Searches in expanding rings from entity position.
     * Prefers closer spots normally, but further spots when player is looking at entity.
     */
    private BlockPos findNearestHidingSpot() {
        if (targetPlayer == null) {
            return null;
        }
        
        World world = entity.getWorld();
        BlockPos entityPos = entity.getBlockPos();
        BlockPos playerPos = targetPlayer.getBlockPos();
        
        boolean playerLooking = isPlayerLookingAtEntity();
        
        BlockPos bestSpot = null;
        double bestDistance = playerLooking ? Double.MIN_VALUE : Double.MAX_VALUE;
        
        // Search in expanding squares (simpler than circles, equally effective)
        for (int radius = 3; radius <= SEARCH_RADIUS; radius += 2) {
            for (int dx = -radius; dx <= radius; dx += 2) {
                for (int dz = -radius; dz <= radius; dz += 2) {
                    // Only check perimeter of each ring for efficiency
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue;
                    }
                    
                    int x = entityPos.getX() + dx;
                    int z = entityPos.getZ() + dz;
                    
                    // Find ground level at this position
                    BlockPos groundPos = findGround(world, x, entityPos.getY(), z);
                    if (groundPos == null) {
                        continue;
                    }
                    
                    // Convert ground position to feet position (one block above ground)
                    BlockPos feetPos = groundPos.up();
                    
                    // Check distance constraints using feet position
                    double distFromPlayer = feetPos.getSquaredDistance(playerPos);
                    if (distFromPlayer > STALK_RADIUS * STALK_RADIUS) {
                        continue; // Too far from player
                    }
                    if (distFromPlayer < MIN_DISTANCE_FROM_PLAYER * MIN_DISTANCE_FROM_PLAYER) {
                        continue; // Too close to player
                    }
                    
                    // Check if this is a valid hiding spot (still checks ground solidity)
                    if (!isValidStandingPosition(world, groundPos)) {
                        continue;
                    }
                    if (!isPositionHidden(feetPos)) {
                        continue;
                    }
                    
                    // Track the best valid spot based on whether player is looking
                    // If player is looking: prefer further spots from player
                    // If player is not looking: prefer closer spots to entity (current position)
                    distFromPlayer = feetPos.getSquaredDistance(playerPos);
                    
                    if (playerLooking) {
                        // Prefer spots further from player when being watched
                        if (distFromPlayer > bestDistance) {
                            bestDistance = distFromPlayer;
                            bestSpot = feetPos;
                        }
                    } else {
                        // Prefer spots closer to entity when not being watched
                        double distFromEntity = feetPos.getSquaredDistance(entityPos);
                        if (distFromEntity < bestDistance) {
                            bestDistance = distFromEntity;
                            bestSpot = feetPos;
                        }
                    }
                }
            }
            
            // Return early if we found a spot (prioritize nearest)
            if (bestSpot != null) {
                return bestSpot;
            }
        }
        
        return bestSpot;
    }
    
    /**
     * Find the ground level at a given X, Z starting from a Y hint.
     * Returns the block position the entity would stand ON.
     */
    private BlockPos findGround(World world, int x, int startY, int z) {
        // Search up and down from startY
        for (int yOffset = 0; yOffset <= 10; yOffset++) {
            // Check above
            BlockPos upPos = new BlockPos(x, startY + yOffset, z);
            if (isValidStandingPosition(world, upPos)) {
                return upPos;
            }
            
            // Check below
            BlockPos downPos = new BlockPos(x, startY - yOffset, z);
            if (isValidStandingPosition(world, downPos)) {
                return downPos;
            }
        }
        return null;
    }
    
    /**
     * Check if an entity can stand at a position.
     * Ground must be solid, with 2 passable blocks above.
     */
    private boolean isValidStandingPosition(World world, BlockPos groundPos) {
        // Ground must be solid
        BlockState ground = world.getBlockState(groundPos);
        if (!ground.isSolidBlock(world, groundPos)) {
            return false;
        }
        
        // Two blocks above must be passable (air or non-solid)
        BlockPos above1 = groundPos.up();
        BlockPos above2 = groundPos.up(2);
        
        BlockState state1 = world.getBlockState(above1);
        BlockState state2 = world.getBlockState(above2);
        
        boolean passable1 = state1.isAir() || state1.getCollisionShape(world, above1).isEmpty();
        boolean passable2 = state2.isAir() || state2.getCollisionShape(world, above2).isEmpty();
        
        return passable1 && passable2;
    }
    
    /**
     * Move the entity towards the hiding spot.
     */
    private void moveToHidingSpot() {
        if (hidingSpot == null) {
            return;
        }

        // Navigate to the hiding spot (which is now the feet position)
        entity.getNavigation().startMovingTo(
            hidingSpot.getX() + 0.5,
            hidingSpot.getY(),  // hidingSpot is already feet position
            hidingSpot.getZ() + 0.5,
            1.5  // Increased speed for running pace
        );

        HorrorMod129.LOGGER.info(String.format(
            "[HideBehindStructures] Navigating to hiding spot: %s",
            hidingSpot.toShortString()
        ));
    }
    
    /**
     * Make the entity look at the player.
     */
    private void lookAtPlayer() {
        if (targetPlayer == null) {
            return;
        }
        
        entity.getLookControl().lookAt(
            targetPlayer.getX(),
            targetPlayer.getEyeY(),
            targetPlayer.getZ()
        );
    }
}
