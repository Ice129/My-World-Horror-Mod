package horror.blueice129.utils;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

/**
 * Utility class for line of sight calculations.
 */
public class LineOfSightUtils {
    // TODO: make a way to visualize line of sight in-game for debugging
    //TODO: make a simple check to return true is block is 180 degrees behind player, aka not in the front half of the player
    //BUG: sometimes misses blocks tht are in line of sight

    /**
     * Checks if a block is within line of sight of a player.
     * 
     * @param player      The player to check from
     * @param blockPos    The position of the block to check
     * @param maxDistance The maximum distance to check
     * @return true if the block is within line of sight, false otherwise
     */
    public static boolean isBlockInLineOfSight(PlayerEntity player, BlockPos blockPos, double maxDistance) {
        World world = player.getWorld();

        // Get player's eye position
        Vec3d eyePos = player.getEyePos();

        // Get the center of the target block
        Vec3d targetPos = new Vec3d(
                blockPos.getX() + 0.5,
                blockPos.getY() + 0.5,
                blockPos.getZ() + 0.5);

        // Calculate direction vector from eye to target
        Vec3d direction = targetPos.subtract(eyePos).normalize();

        // Calculate the distance to the target
        double distance = eyePos.distanceTo(targetPos);

        // If target is too far, return false
        if (distance > maxDistance) {
            return false;
        }

        // Check if the block is within the player's field of view
        if (!isWithinFieldOfView(player, direction)) {
            return false;
        }

        // Perform the ray trace
        BlockHitResult hitResult = world.raycast(new RaycastContext(
                eyePos,
                eyePos.add(direction.multiply(distance)),
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                player));

        // Check if the hit block is the target block
        return hitResult.getType() == HitResult.Type.BLOCK &&
                hitResult.getBlockPos().equals(blockPos);
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
        // Get player's looking direction
        float pitch = player.getPitch() * ((float) Math.PI / 180F);
        float yaw = player.getYaw() * ((float) Math.PI / 180F);

        // Calculate player's view vector
        float x = -MathHelper.sin(yaw) * MathHelper.cos(pitch);
        float y = -MathHelper.sin(pitch);
        float z = MathHelper.cos(yaw) * MathHelper.cos(pitch);
        Vec3d viewVector = new Vec3d(x, y, z).normalize();

        // Normalize the direction
        direction = direction.normalize();

        // Get the player's vertical FOV (in degrees) and convert to radians
        MinecraftClient client = MinecraftClient.getInstance();
        double vFovDegrees = client.options.getFov().getValue();
        double vFovRad = vFovDegrees * (Math.PI / 180.0);

        // Calculate horizontal FOV based on aspect ratio
        // hFov = 2 * atan(tan(vFov/2) * aspectRatio)
        int screenWidth = client.getWindow().getWidth();
        int screenHeight = client.getWindow().getHeight();
        double aspectRatio = (double) screenWidth / screenHeight;
        double hFovRad = 2.0 * Math.atan(Math.tan(vFovRad / 2.0) * aspectRatio);

        // Calculate right vector (horizontal axis perpendicular to view)
        // Correct formula: rotate view vector 90 degrees right in the horizontal plane
        Vec3d rightVector = new Vec3d(MathHelper.cos(yaw), 0, MathHelper.sin(yaw)).normalize();

        // Calculate up vector (vertical axis perpendicular to view)
        Vec3d upVector = viewVector.crossProduct(rightVector).normalize();

        // Calculate horizontal and vertical deviations FROM the center view direction
        // using atan2 to get signed angles in each plane
        double viewDot = viewVector.dotProduct(direction);
        double rightDot = rightVector.dotProduct(direction);
        double upDot = upVector.dotProduct(direction);
        
        // Horizontal angle: deviation left/right from center
        double horizontalAngle = Math.atan2(rightDot, viewDot);
        
        // Vertical angle: deviation up/down from center
        double verticalAngle = Math.atan2(upDot, viewDot);

        // Add buffer factor to match Minecraft's actual FOV (Minecraft renders slightly beyond the strict bounds)
        double hFovBuffer = hFovRad / 2.0 * 1.1;
        double vFovBuffer = vFovRad / 2.0 * 1.1;

        // Must be within both horizontal and vertical FOV bounds
        return Math.abs(horizontalAngle) <= hFovBuffer && Math.abs(verticalAngle) <= vFovBuffer;
    }

    /**
     * if block rendered on the players screen
     * it will take into account the players FOV, eye position,
     * ray casting to see if the block is occluded by other blocks, check for the faces of the blocks
     * instead of just the center of the block, and the dimensions of the screen
     * @param player      The player to check from
     * @param blockPos    The position of the block to check
     * @param maxDistance The maximum distance to check
     * @return true if the block is rendered on the player's screen, false otherwise
     */
    public static boolean isBlockRenderedOnScreen(PlayerEntity player, BlockPos blockPos, double maxDistance) {
        World world = player.getWorld();
        Vec3d eyePos = player.getEyePos();

        // Calculate the block center and direction from block to player
        Vec3d blockCenter = new Vec3d(
                blockPos.getX() + 0.5,
                blockPos.getY() + 0.5,
                blockPos.getZ() + 0.5);
        double distance = eyePos.distanceTo(blockCenter);

        // If target is too far, return false
        if (distance > maxDistance) {
            return false;
        }

        // Direction from block center to player eye (for face culling)
        Vec3d toPlayer = eyePos.subtract(blockCenter).normalize();

        // Define block faces with their centers and normals
        Vec3d[] faceCenters = {
            blockCenter, // Check center first (helps with diagonal visibility)
            new Vec3d(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ()), // front face (Z=0)
            new Vec3d(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 1), // back face (Z=1)
            new Vec3d(blockPos.getX(), blockPos.getY() + 0.5, blockPos.getZ() + 0.5), // left face (X=0)
            new Vec3d(blockPos.getX() + 1, blockPos.getY() + 0.5, blockPos.getZ() + 0.5), // right face (X=1)
            new Vec3d(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5), // bottom face (Y=0)
            new Vec3d(blockPos.getX() + 0.5, blockPos.getY() + 1, blockPos.getZ() + 0.5) // top face (Y=1)
        };

        Vec3d[] faceNormals = {
            new Vec3d(0, 0, 0), // center has no specific normal, won't be culled
            new Vec3d(0, 0, -1), // front face normal
            new Vec3d(0, 0, 1),  // back face normal
            new Vec3d(-1, 0, 0), // left face normal
            new Vec3d(1, 0, 0),  // right face normal
            new Vec3d(0, -1, 0), // bottom face normal
            new Vec3d(0, 1, 0)   // top face normal
        };

        // Check only front-facing faces (optimization)
        for (int i = 0; i < faceCenters.length; i++) {
            Vec3d faceCenter = faceCenters[i];
            Vec3d faceNormal = faceNormals[i];

            // Skip back-facing faces (skip if normal points away from player)
            // Center point (i=0) has no normal, so always check it
            if (i > 0 && faceNormal.dotProduct(toPlayer) < 0.01) {
                continue;
            }

            Vec3d direction = faceCenter.subtract(eyePos).normalize();
            double faceDistance = eyePos.distanceTo(faceCenter);

            // Check if the point is within the player's field of view
            if (!isWithinFieldOfView(player, direction)) {
                continue;
            }

            // Perform the ray trace
            BlockHitResult hitResult = world.raycast(new RaycastContext(
                    eyePos,
                    eyePos.add(direction.multiply(faceDistance)),
                    RaycastContext.ShapeType.OUTLINE,
                    RaycastContext.FluidHandling.NONE,
                    player));

            // If the ray hits this block without being occluded, return true
            if (hitResult.getType() == HitResult.Type.BLOCK &&
                    hitResult.getBlockPos().equals(blockPos)) {
                return true;
            }
        }

        return false;
    }
    
}
