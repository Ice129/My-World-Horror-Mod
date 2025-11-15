package horror.blueice129.entity.goals.movement;

import horror.blueice129.entity.Blueice129Entity;
import horror.blueice129.entity.goals.BaseBlueice129Goal;
import horror.blueice129.utils.ChunkLoader;
import horror.blueice129.utils.LineOfSightUtils;
import horror.blueice129.utils.SurfaceFinder;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Goal that makes the entity hide behind structures on the surface
 * Finds positions not visible to the player and pathfinds to them
 */
public class HideBehindStructuresGoal extends BaseBlueice129Goal {
    
    private PlayerEntity targetPlayer;
    private BlockPos currentHidingSpot;
    private int searchCooldown = 0;
    private static final int SEARCH_RADIUS = 30;
    private static final int PLAYER_DETECTION_RANGE = 64;
    
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
        // Refresh player and search for hiding spots periodically (every 20 ticks)
        if (searchCooldown <= 0) {
            targetPlayer = entity.getWorld().getClosestPlayer(entity, PLAYER_DETECTION_RANGE);
            
            // Only search for new spot if we don't have one or navigation is idle/completed
            if (currentHidingSpot == null || entity.getNavigation().isIdle()) {
                currentHidingSpot = findValidHidingSpot();
            }
            
            searchCooldown = 20; // Reset cooldown (1 second at 20 TPS)
        } else {
            searchCooldown--;
        }
        
        // Move to hiding spot if we have one
        if (currentHidingSpot != null) {
            entity.getNavigation().startMovingTo(
                currentHidingSpot.getX(), 
                currentHidingSpot.getY(), 
                currentHidingSpot.getZ(), 
                1.0
            );
        }
    }

    private BlockPos findValidHidingSpot() {
        if (targetPlayer == null) {
            return null;
        }
        
        List<BlockPos> candidates = new ArrayList<>();
        BlockPos entityPos = entity.getBlockPos();
        World world = entity.getWorld();
        
        if (!(world instanceof ServerWorld serverWorld)) {
            return null;
        }
        
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
                
                // Check if this is a valid hiding spot
                if (isValidHidingSpot(groundPos)) {
                    candidates.add(groundPos);
                }
            }
        }
        
        // Sort candidates by distance to entity and return nearest
        if (!candidates.isEmpty()) {
            candidates.sort((a, b) -> 
                Double.compare(
                    a.getSquaredDistance(entityPos),
                    b.getSquaredDistance(entityPos)
                )
            );
            return candidates.get(0);
        }
        
        return null; // No valid hiding spot found
    }
    
    private boolean isValidHidingSpot(BlockPos pos) {
        World world = entity.getWorld();
        
        // Check that there are 2 air blocks above the ground position
        BlockPos above1 = pos.up(1);
        BlockPos above2 = pos.up(2);
        
        if (!world.isAir(above1) || !world.isAir(above2)) {
            return false;
        }
        
        // Check that the ground block is solid
        if (!world.getBlockState(pos).isSolidBlock(world, pos)) {
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
}
