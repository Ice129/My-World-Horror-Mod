package horror.blueice129.utils;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.util.math.MathHelper;

/**
 * Utility class for line of sight calculations.
 */
public class LineOfSightUtils {
    
    private static final int MAX_TRANSPARENT_ITERATIONS = 20; // Safety limit for raycast continuation
    private static final int MAX_LEAVES_BEFORE_OPAQUE = 3; // Leaves become opaque after 3 blocks

    /**
     * Check if a block should stop the raycast (is opaque/blocks view).
     * Treats air, transparent blocks (glass, leaves), and non-collision blocks as see-through.
     * Special handling: leaves block view after passing through more than MAX_LEAVES_BEFORE_OPAQUE.
     * 
     * @param world The world
     * @param pos The block position
     * @param leafCount Current count of leaves passed through
     * @return true if this block blocks line of sight
     */
    private static boolean blocksLineOfSight(World world, BlockPos pos, int leafCount) {
        BlockState state = world.getBlockState(pos);
        
        // Air never blocks
        if (state.isAir()) {
            return false;
        }
        
        // Non-collision blocks (tall grass, flowers, etc.) never block - treat like air
        if (state.getCollisionShape(world, pos).isEmpty()) {
            return false;
        }
        
        // Check if it's a leaf block - block if we've hit too many
        String blockId = state.getBlock().toString();
        boolean isLeaf = blockId.contains("leaves") || blockId.contains("leaf");
        if (isLeaf && leafCount >= MAX_LEAVES_BEFORE_OPAQUE) {
            return true; // Too many leaves, now opaque
        }
        
        // Block only if it's opaque (not transparent like glass)
        return state.isOpaque();
    }

    /**
     * Checks if a direction is within the player's field of view.
     * Takes into account the rectangular screen shape and aspect ratio.
     * 
     * @param player    The player
     * @param direction The direction vector to check
     * @return true if the direction is within the player's field of view
     */
    public static boolean isWithinFieldOfView(PlayerEntity player, Vec3d direction) {
        float pitch = player.getPitch() * ((float) Math.PI / 180F);
        float yaw = player.getYaw() * ((float) Math.PI / 180F);

       
        float x = -MathHelper.sin(yaw) * MathHelper.cos(pitch);
        float y = -MathHelper.sin(pitch);
        float z = MathHelper.cos(yaw) * MathHelper.cos(pitch);
        Vec3d viewVector = new Vec3d(x, y, z).normalize();

        direction = direction.normalize();

        double hFovDegrees = 150.0; // 150 degrees horizontal
        double vFovDegrees = 130.0; // 130 degrees vertical

        // Convert to radians and calculate half-angles
        double hFovRad = hFovDegrees * (Math.PI / 180.0);
        double vFovRad = vFovDegrees * (Math.PI / 180.0);
        double hFovHalf = hFovRad / 2.0; 
        double vFovHalf = vFovRad / 2.0; 

        Vec3d rightVector = new Vec3d(MathHelper.cos(yaw), 0, MathHelper.sin(yaw)).normalize();
        Vec3d upVector = viewVector.crossProduct(rightVector).normalize();

        
        double viewDot = viewVector.dotProduct(direction); 
        double rightDot = rightVector.dotProduct(direction); 
        double upDot = upVector.dotProduct(direction);
        double horizontalAngle = Math.atan2(rightDot, viewDot);
        double verticalAngle = Math.atan2(upDot, viewDot);
        return Math.abs(horizontalAngle) <= hFovHalf && Math.abs(verticalAngle) <= vFovHalf;
    }

    /**
     * Checks a single block if it's rendered on the player's screen.
     * Now delegates to hasLineOfSight() for consistent visibility checking.
     * 
     * This method now properly handles:
     * - Transparent blocks (glass, ice) - see through them
     * - Leaves - become opaque after 3 blocks
     * - Non-collision blocks (grass, flowers) - automatically ignored
     * 
     * @param player      The player to check from
     * @param blockPos    The position of the block to check
     * @param maxDistance The maximum distance to check
     * @return true if the block has line of sight from the player
     */
    public static boolean isBlockRenderedOnScreen(PlayerEntity player, BlockPos blockPos, double maxDistance) {
        // Delegate to hasLineOfSight - they check the same thing!
        return hasLineOfSight(player, blockPos, maxDistance);
    }
    
    /**
     * Checks if a position has a clear line of sight from the player.
     * This is different from isBlockRenderedOnScreen - it checks if the player
     * can "see" a position (useful for checking if an entity would be visible).
     * 
     * Checks all 8 corners of the block with early stopping when any corner is visible.
     * Treats transparent blocks (glass, leaves, grass) as see-through by continuing raycast.
     * Leaves become opaque after passing through 3+ leaf blocks (realistic foliage density).
     * 
     * Optimizations: FOV check on center first, then corner checks with early return.
     * 
     * For AIR blocks: Returns true if nothing blocks the view to that position
     * For SOLID blocks: Returns true if the block itself is visible
     * 
     * @param player The player to check from
     * @param targetPos The position to check
     * @param maxDistance The maximum distance to check
     * @return true if the position has clear line of sight from player
     */
    public static boolean hasLineOfSight(PlayerEntity player, BlockPos targetPos, double maxDistance) {
        World world = player.getWorld();
        Vec3d eyePos = player.getEyePos();
        Vec3d targetCenter = new Vec3d(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5);
        
        // Early distance check (cheap)
        double distance = eyePos.distanceTo(targetCenter);
        if (distance > maxDistance) {
            return false;
        }
        
        // Early FOV check on center (cheap, filters out ~75% of blocks behind player)
        Vec3d directionToCenter = targetCenter.subtract(eyePos).normalize();
        if (!isWithinFieldOfView(player, directionToCenter)) {
            return false;
        }
        
        // Check if target is air/passable (cache this check)
        boolean isTargetPassable = world.isAir(targetPos) || !world.getBlockState(targetPos).isSolidBlock(world, targetPos);
        
        // Pre-calculate corner positions with epsilon offset (small value inward for precision)
        double epsilon = 0.001;
        Vec3d[] corners = {
            // Bottom corners (with epsilon offset inward)
            new Vec3d(targetPos.getX() + epsilon, targetPos.getY() + epsilon, targetPos.getZ() + epsilon),         // 0,0,0
            new Vec3d(targetPos.getX() + 1 - epsilon, targetPos.getY() + epsilon, targetPos.getZ() + epsilon),     // 1,0,0
            new Vec3d(targetPos.getX() + epsilon, targetPos.getY() + epsilon, targetPos.getZ() + 1 - epsilon),     // 0,0,1
            new Vec3d(targetPos.getX() + 1 - epsilon, targetPos.getY() + epsilon, targetPos.getZ() + 1 - epsilon), // 1,0,1
            // Top corners (with epsilon offset inward)
            new Vec3d(targetPos.getX() + epsilon, targetPos.getY() + 1 - epsilon, targetPos.getZ() + epsilon),         // 0,1,0
            new Vec3d(targetPos.getX() + 1 - epsilon, targetPos.getY() + 1 - epsilon, targetPos.getZ() + epsilon),     // 1,1,0
            new Vec3d(targetPos.getX() + epsilon, targetPos.getY() + 1 - epsilon, targetPos.getZ() + 1 - epsilon),     // 0,1,1
            new Vec3d(targetPos.getX() + 1 - epsilon, targetPos.getY() + 1 - epsilon, targetPos.getZ() + 1 - epsilon)  // 1,1,1
        };
        
        // Check each corner - early stop if ANY corner is visible
        for (Vec3d corner : corners) {
            if (hasLineOfSightToPoint(player, world, eyePos, corner, targetPos, isTargetPassable)) {
                return true; // EARLY RETURN - at least one corner is visible
            }
        }
        
        // None of the corners were visible
        return false;
    }
    
    /**
     * Helper method to check line of sight to a specific point, continuing through transparent blocks.
     * Handles transparent blocks (glass, leaves, grass) by continuing the raycast.
     * Tracks leaf count - leaves become opaque after 3 blocks.
     * 
     * Uses COLLIDER shape type so non-collision blocks (tall grass, flowers) are automatically skipped.
     * 
     * @param player The player
     * @param world The world
     * @param start Starting position (player eye)
     * @param end Target point (corner)
     * @param targetPos The target block position we're checking
     * @param isTargetPassable Whether the target block is air/passable
     * @return true if there's line of sight to the point
     */
    private static boolean hasLineOfSightToPoint(PlayerEntity player, World world, Vec3d start, Vec3d end, 
                                                   BlockPos targetPos, boolean isTargetPassable) {
        Vec3d currentStart = start;
        int leafCount = 0;
        double targetDistance = start.distanceTo(end);
        
        // Iteratively raycast, continuing through transparent blocks
        for (int iteration = 0; iteration < MAX_TRANSPARENT_ITERATIONS; iteration++) {
            // Raycast from current position to target point
            // Use COLLIDER shape type - this automatically skips non-collision blocks like tall grass!
            BlockHitResult hitResult = world.raycast(new RaycastContext(
                currentStart,
                end,
                RaycastContext.ShapeType.COLLIDER,  // CHANGED: Use COLLIDER instead of OUTLINE
                RaycastContext.FluidHandling.NONE,
                player
            ));
            
            // If we hit the target block itself, it's visible
            if (hitResult.getType() == HitResult.Type.BLOCK && hitResult.getBlockPos().equals(targetPos)) {
                return true;
            }
            
            // If we didn't hit anything (MISS), clear line of sight
            if (hitResult.getType() == HitResult.Type.MISS) {
                return true;
            }
            
            // Check what we hit
            BlockPos hitPos = hitResult.getBlockPos();
            
            // Update leaf count if we hit a leaf
            BlockState hitState = world.getBlockState(hitPos);
            String blockId = hitState.getBlock().toString();
            if (blockId.contains("leaves") || blockId.contains("leaf")) {
                leafCount++;
            }
            
            // Check if this block blocks line of sight
            if (blocksLineOfSight(world, hitPos, leafCount)) {
                // Hit an opaque block before reaching target
                // For passable targets, check if we went past the target
                if (isTargetPassable) {
                    double distToHit = start.distanceTo(hitResult.getPos());
                    return distToHit >= targetDistance; // Went past target = nothing blocked it
                }
                return false; // Solid block blocked view
            }
            
            // Hit a transparent block - continue from slightly past the hit point
            Vec3d hitPoint = hitResult.getPos();
            Vec3d direction = end.subtract(hitPoint).normalize();
            currentStart = hitPoint.add(direction.multiply(0.01)); // Small offset to avoid re-hitting same block
            
            // Safety check: if we've passed the target distance, we have line of sight
            double currentDistance = start.distanceTo(currentStart);
            if (currentDistance >= targetDistance) {
                return true;
            }
        }
        
        // Exceeded iteration limit (safety) - assume blocked
        return false;
    }
}