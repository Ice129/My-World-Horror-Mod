package horror.blueice129.utils;

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
     * Optimized with early FOV filtering, backface culling, and face-based
     * raycasting to check if any part of the full block is visible.
     * Treats all blocks as full blocks for raycasting.
     *
     * @param player      The player to check from
     * @param blockPos    The position of the block to check
     * @param maxDistance The maximum distance to check
     * @return true if the block is rendered on the player's screen, false otherwise
     */
    public static boolean isBlockRenderedOnScreen(PlayerEntity player, BlockPos blockPos, double maxDistance) {
        World world = player.getWorld();
        Vec3d eyePos = player.getEyePos();
        Vec3d blockCenter = new Vec3d(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);

        double distance = eyePos.distanceTo(blockCenter);
        if (distance > maxDistance) {
            return false;
        }

        Vec3d direction = blockCenter.subtract(eyePos).normalize();
        if (!isWithinFieldOfView(player, direction)) {
            return false;
        }

        double epsilon = 0.001; // Small offset to avoid precision issues
        Vec3d[] corners = {
            // Bottom corners (with epsilon offset inward)
            new Vec3d(blockPos.getX() + epsilon, blockPos.getY() + epsilon, blockPos.getZ() + epsilon),         // 0,0,0
            new Vec3d(blockPos.getX() + 1 - epsilon, blockPos.getY() + epsilon, blockPos.getZ() + epsilon),     // 1,0,0
            new Vec3d(blockPos.getX() + epsilon, blockPos.getY() + epsilon, blockPos.getZ() + 1 - epsilon),     // 0,0,1
            new Vec3d(blockPos.getX() + 1 - epsilon, blockPos.getY() + epsilon, blockPos.getZ() + 1 - epsilon), // 1,0,1
            // Top corners (with epsilon offset inward)
            new Vec3d(blockPos.getX() + epsilon, blockPos.getY() + 1 - epsilon, blockPos.getZ() + epsilon),         // 0,1,0
            new Vec3d(blockPos.getX() + 1 - epsilon, blockPos.getY() + 1 - epsilon, blockPos.getZ() + epsilon),     // 1,1,0
            new Vec3d(blockPos.getX() + epsilon, blockPos.getY() + 1 - epsilon, blockPos.getZ() + 1 - epsilon),     // 0,1,1
            new Vec3d(blockPos.getX() + 1 - epsilon, blockPos.getY() + 1 - epsilon, blockPos.getZ() + 1 - epsilon)  // 1,1,1
        };

        for (Vec3d corner : corners) {

            Vec3d cornerDirection = corner.subtract(eyePos).normalize();
            double cornerDistance = eyePos.distanceTo(corner);

            BlockHitResult hitResult = world.raycast(new RaycastContext(
                    eyePos,
                    eyePos.add(cornerDirection.multiply(cornerDistance)),
                    RaycastContext.ShapeType.OUTLINE,
                    RaycastContext.FluidHandling.NONE,
                    player));

            if (hitResult.getType() == HitResult.Type.BLOCK &&
                    hitResult.getBlockPos().equals(blockPos)) {
                return true;
            }
        }

        return false;
    }
}