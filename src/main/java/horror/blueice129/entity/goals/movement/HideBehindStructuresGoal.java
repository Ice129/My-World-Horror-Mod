package horror.blueice129.entity.goals.movement;

import horror.blueice129.entity.Blueice129Entity;
import horror.blueice129.entity.goals.BaseBlueice129Goal;
import horror.blueice129.utils.ChunkLoader;
import horror.blueice129.utils.LineOfSightUtils;
import horror.blueice129.utils.PathVisibilityScorer;
import horror.blueice129.utils.PathVisualizer;
import horror.blueice129.utils.SurfaceFinder;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Goal that makes the entity hide behind structures on the surface.
 * Finds positions not visible to the player, preferring spots further away
 * with minimal visibility along the path. Uses glowstone trails for debug visualization.
 */
public class HideBehindStructuresGoal extends BaseBlueice129Goal {
    
    private PlayerEntity targetPlayer;
    private BlockPos currentHidingSpot;
    private BlockPos lastVisualizedSpot;
    private int searchCooldown = 0;
    private static final int SEARCH_RADIUS = 50;
    private static final int PLAYER_DETECTION_RANGE = 64;
    // private static final int MAX_CANDIDATES_TO_EVALUATE = 3;
    private static final int MAX_STALK_DISTANCE = 20; // Maximum distance from player while stalking
    
    public HideBehindStructuresGoal(Blueice129Entity entity) {
        super(entity);
        this.setControls(EnumSet.of(Goal.Control.MOVE));
    }
    
    @Override
    protected boolean shouldStart() {
        return isInState(Blueice129Entity.EntityState.SURFACE_HIDING);
    }
    
    @Override
    public void tick() {
        World world = entity.getWorld();
        
        // Cleanup expired glowstone blocks (debug visualization)
        if (world instanceof ServerWorld serverWorld) {
            PathVisualizer.cleanupExpiredBlocks(serverWorld, world.getTime());
        }
        
        // Refresh player and search for hiding spots periodically (every tick for responsive stalking)
        if (searchCooldown <= 0) {
            targetPlayer = entity.getWorld().getClosestPlayer(entity, PLAYER_DETECTION_RANGE);
            
            // Check if entity has drifted beyond max stalk distance - force re-search
            boolean beyondStalkDistance = false;
            if (targetPlayer != null) {
                double distanceToPlayer = entity.getBlockPos().getSquaredDistance(targetPlayer.getBlockPos());
                beyondStalkDistance = distanceToPlayer > (MAX_STALK_DISTANCE * MAX_STALK_DISTANCE);
            }
            
            // Search for new spot if: no spot, navigation idle, or beyond stalk distance
            if (currentHidingSpot == null || entity.getNavigation().isIdle() || beyondStalkDistance) {
                BlockPos newSpot = findValidHidingSpot();
                
                // Visualize the new path if it changed
                if (newSpot != null && !newSpot.equals(lastVisualizedSpot)) {
                    currentHidingSpot = newSpot;
                    lastVisualizedSpot = newSpot;
                    
                    if (world instanceof ServerWorld serverWorld) {
                        PathVisualizer.visualizePath(
                            serverWorld, 
                            entity.getBlockPos(), 
                            currentHidingSpot, 
                            world.getTime()
                        );
                    }
                }
            }

            searchCooldown = 1; // Reset cooldown (1 tick at 20 TPS)
        } else {
            searchCooldown--;
        }
        
        // Move to hiding spot if we have one
        if (currentHidingSpot != null) {
            // Check if entity is currently hidden from player view
            BlockPos entityPos = entity.getBlockPos();
            boolean isCurrentlyHidden = targetPlayer != null && 
                !PathVisibilityScorer.isPositionVisible(
                    targetPlayer, 
                    world, 
                    entityPos, 
                    PLAYER_DETECTION_RANGE
                );
            
            // Calculate distance to target hiding spot
            double distanceToTarget = Math.sqrt(entityPos.getSquaredDistance(currentHidingSpot));
            
            // Only stop navigation if BOTH conditions are met:
            // 1. Entity is currently hidden from player
            // 2. Entity is reasonably close to target (within 5 blocks) OR very close (within 2 blocks)
            boolean shouldStopMoving = isCurrentlyHidden && 
                                      !entity.getNavigation().isIdle() && 
                                      distanceToTarget <= 5.0;
            
            if (shouldStopMoving) {
                entity.getNavigation().stop();
                // Keep currentHidingSpot - entity is in a good hidden position
            } else {
                // Continue moving toward hiding spot
                entity.getNavigation().startMovingTo(
                    currentHidingSpot.getX(), 
                    currentHidingSpot.getY(), 
                    currentHidingSpot.getZ(), 
                    0.5 // Slower speed for testing. default is 1.3
                );
            }
        }
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        
        // Clear visualization when goal stops
        World world = entity.getWorld();
        if (world instanceof ServerWorld serverWorld) {
            PathVisualizer.clearAll(serverWorld);
        }
        
        lastVisualizedSpot = null;
    }

    private BlockPos findValidHidingSpot() {
        if (targetPlayer == null) {
            return null;
        }
        
        List<BlockPos> candidates = new ArrayList<>();
        BlockPos entityPos = entity.getBlockPos();
        BlockPos playerPos = targetPlayer.getBlockPos();
        World world = entity.getWorld();
        
        if (!(world instanceof ServerWorld serverWorld)) {
            return null;
        }
        
        // Calculate direction away from player
        Vec3d toPlayer = new Vec3d(
            playerPos.getX() - entityPos.getX(),
            0,
            playerPos.getZ() - entityPos.getZ()
        ).normalize();
        
        // Use circular sampling pattern - check positions in expanding rings
        for (int radius = 10; radius <= SEARCH_RADIUS; radius += 5) {
            for (int angle = 0; angle < 360; angle += 30) {
                double radian = Math.toRadians(angle);
                int x = entityPos.getX() + (int)(Math.cos(radian) * radius);
                int z = entityPos.getZ() + (int)(Math.sin(radian) * radius);
                
                BlockPos checkPos = new BlockPos(x, entityPos.getY(), z);
                
                // Load chunks first (required before accessing blocks)
                if (!ChunkLoader.loadChunksInRadius(serverWorld, checkPos, 1)) {
                    continue;
                }
                
                // Find surface Y coordinate
                int surfaceY = SurfaceFinder.findPointSurfaceY(
                    serverWorld, x, z, false, true, false);
                    
                if (surfaceY == -1) {
                    continue; // No valid surface found
                }
                
                BlockPos groundPos = new BlockPos(x, surfaceY, z);
                
                // Check distance from player - enforce max stalk distance
                double distanceFromPlayer = groundPos.getSquaredDistance(playerPos);
                if (distanceFromPlayer > (MAX_STALK_DISTANCE * MAX_STALK_DISTANCE)) {
                    continue; // Too far from player for stalking behavior
                }
                
                // Check if this is a valid hiding spot
                if (isValidHidingSpot(groundPos)) {
                    // Prefer positions away from player (dot product check)
                    Vec3d toCandidate = new Vec3d(
                        groundPos.getX() - entityPos.getX(),
                        0,
                        groundPos.getZ() - entityPos.getZ()
                    ).normalize();
                    
                    double dotProduct = toCandidate.dotProduct(toPlayer);
                    
                    // Only consider positions generally away from player (dot product < 0)
                    // or at least not directly toward player (dot product < 0.3)
                    if (dotProduct < 0.3) {
                        candidates.add(groundPos);
                    }
                }
            }
        }
        
        if (candidates.isEmpty()) {
            return null; // No valid hiding spot found
        }
        
        // Sort candidates by visibility score first (lowest = best hiding), 
        // then by distance from player (closer for stalking, but still hidden)
        // Pre-score all candidates for better sorting
        List<ScoredCandidate> scoredCandidates = new ArrayList<>();
        for (BlockPos candidate : candidates) {
            int visibilityScore = PathVisibilityScorer.scorePath(
                targetPlayer,
                world,
                entityPos, 
                candidate, 
                SEARCH_RADIUS
            );
            if (visibilityScore >= 0) {
                scoredCandidates.add(new ScoredCandidate(candidate, visibilityScore));
            }
        }
        
        if (scoredCandidates.isEmpty()) {
            return null;
        }
        
        // Sort by visibility score (lower = better), break ties with closer distance
        scoredCandidates.sort((a, b) -> {
            int scoreCompare = Integer.compare(a.score, b.score);
            if (scoreCompare != 0) {
                return scoreCompare;
            }
            // If scores equal, prefer closer to player (for stalking)
            return Double.compare(
                a.pos.getSquaredDistance(playerPos),
                b.pos.getSquaredDistance(playerPos)
            );
        });
        
        // Return the best candidate (already sorted by score, then distance)
        return scoredCandidates.get(0).pos;
    }
    
    private boolean isValidHidingSpot(BlockPos pos) {
        World world = entity.getWorld();
        
        // Check that the ground block is solid
        if (!world.getBlockState(pos).isSolidBlock(world, pos)) {
            return false;
        }
        
        // Check that there are 2 air blocks (or passable blocks) above the ground position
        BlockPos above1 = pos.up(1);
        BlockPos above2 = pos.up(2);
        
        // Allow air or non-collision blocks (like grass, torches, snow) above ground
        BlockState state1 = world.getBlockState(above1);
        BlockState state2 = world.getBlockState(above2);
        
        boolean space1Clear = state1.isAir() || !state1.isSolidBlock(world, above1);
        boolean space2Clear = state2.isAir() || !state2.isSolidBlock(world, above2);
        
        if (!space1Clear || !space2Clear) {
            return false;
        }
        
        // Check if all three positions (ground + 2 air blocks) are not visible by the player
        boolean groundVisible = LineOfSightUtils.isBlockRenderedOnScreen(
            targetPlayer, pos, SEARCH_RADIUS);
        boolean air1Visible = LineOfSightUtils.isBlockRenderedOnScreen(
            targetPlayer, above1, SEARCH_RADIUS);
        boolean air2Visible = LineOfSightUtils.isBlockRenderedOnScreen(
            targetPlayer, above2, SEARCH_RADIUS);
            
        // Valid if none of the positions are visible
        return !groundVisible && !air1Visible && !air2Visible;
    }
    
    /**
     * Helper class to store a candidate position with its visibility score
     */
    private static class ScoredCandidate {
        final BlockPos pos;
        final int score;
        
        ScoredCandidate(BlockPos pos, int score) {
            this.pos = pos;
            this.score = score;
        }
    }
}
