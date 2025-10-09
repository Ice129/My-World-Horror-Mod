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

        // Calculate the angle between the player's view and the direction
        double dot = viewVector.dotProduct(direction);
        double angle = Math.acos(dot);

        // Convert angle to degrees
        double angleDegrees = angle * (180.0 / Math.PI);

        // Get the player's FOV
        MinecraftClient client = MinecraftClient.getInstance();
        double fov = client.options.getFov().getValue();

        // Check if the angle is within half the FOV
        return angleDegrees <= fov / 2.0;
    }
}
