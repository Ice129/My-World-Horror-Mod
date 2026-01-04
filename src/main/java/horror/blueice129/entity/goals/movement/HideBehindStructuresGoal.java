package horror.blueice129.entity.goals.movement;

import horror.blueice129.entity.Blueice129Entity;
import horror.blueice129.entity.goals.BaseBlueice129Goal;
import horror.blueice129.utils.LineOfSightUtils;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
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
    
    private PlayerEntity targetPlayer;
    private BlockPos hidingSpot;
    
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
        
        BlockPos entityPos = entity.getBlockPos();
        boolean isHidden = isPositionHidden(entityPos);
        boolean isStationary = entity.getNavigation().isIdle();
        
        // Handle behavior based on current state
        if (isHidden && isStationary) {
            // Already hidden and not moving - crouch and watch player
            entity.setSneaking(true);
            lookAtPlayer();
        } else if (isHidden && !isStationary) {
            // Moving to hiding spot - stand up for natural movement
            entity.setSneaking(false);
        } else {
            // Not hidden - find a hiding spot and move there
            entity.setSneaking(false);
            
            if (hidingSpot == null || !isPositionHidden(hidingSpot)) {
                hidingSpot = findNearestHidingSpot();
            }
            
            if (hidingSpot != null) {
                moveToHidingSpot();
            }
        }
        
        // Re-evaluate hiding spot if player moved significantly
        if (hidingSpot != null && targetPlayer != null) {
            double distanceToPlayer = hidingSpot.getSquaredDistance(targetPlayer.getBlockPos());
            if (distanceToPlayer > STALK_RADIUS * STALK_RADIUS) {
                hidingSpot = null; // Too far from player, need new spot
            }
        }
    }
    
    /**
     * Check if a position would hide the entity from the player's view.
     * Checks both blocks the entity would occupy (feet and head level).
     */
    private boolean isPositionHidden(BlockPos groundPos) {
        if (targetPlayer == null) {
            return false;
        }
        
        BlockPos feetPos = groundPos.up();  // Entity stands ON groundPos, feet at groundPos+1
        BlockPos headPos = groundPos.up(2); // Head at groundPos+2
        
        // Both positions must be hidden from player view
        boolean feetVisible = LineOfSightUtils.isBlockRenderedOnScreen(
            targetPlayer, feetPos, STALK_RADIUS * 2);
        boolean headVisible = LineOfSightUtils.isBlockRenderedOnScreen(
            targetPlayer, headPos, STALK_RADIUS * 2);
        
        return !feetVisible && !headVisible;
    }
    
    /**
     * Find the nearest valid hiding spot.
     * Searches in expanding rings from entity position.
     */
    private BlockPos findNearestHidingSpot() {
        if (targetPlayer == null) {
            return null;
        }
        
        World world = entity.getWorld();
        BlockPos entityPos = entity.getBlockPos();
        BlockPos playerPos = targetPlayer.getBlockPos();
        
        BlockPos bestSpot = null;
        double bestDistance = Double.MAX_VALUE;
        
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
                    
                    // Check distance constraints
                    double distFromPlayer = groundPos.getSquaredDistance(playerPos);
                    if (distFromPlayer > STALK_RADIUS * STALK_RADIUS) {
                        continue; // Too far from player
                    }
                    if (distFromPlayer < MIN_DISTANCE_FROM_PLAYER * MIN_DISTANCE_FROM_PLAYER) {
                        continue; // Too close to player
                    }
                    
                    // Check if this is a valid hiding spot
                    if (!isValidStandingPosition(world, groundPos)) {
                        continue;
                    }
                    if (!isPositionHidden(groundPos)) {
                        continue;
                    }
                    
                    // Track the closest valid spot
                    double distFromEntity = groundPos.getSquaredDistance(entityPos);
                    if (distFromEntity < bestDistance) {
                        bestDistance = distFromEntity;
                        bestSpot = groundPos;
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
        
        // Navigate to the position above the ground (where entity stands)
        entity.getNavigation().startMovingTo(
            hidingSpot.getX() + 0.5,
            hidingSpot.getY() + 1,  // Stand on top of ground block
            hidingSpot.getZ() + 0.5,
            1.0  // Normal walking speed
        );
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
