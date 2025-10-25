package horror.blueice129.debug;

import horror.blueice129.utils.LineOfSightUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class LineOfSightChecker {

    /**
     * Fills all non-air blocks that are in line of sight with green stained glass blocks.
     * Optimized to only check blocks in front of the player within their FOV.
     * 
     * @param player The player to check from
     * @param maxDistance The maximum distance to check
     */
    public static void fillLineOfSightWithGlass(PlayerEntity player, double maxDistance) {
        World world = player.getWorld();
        Vec3d eyePos = player.getEyePos();
        BlockPos playerPos = player.getBlockPos();
        int range = (int) Math.ceil(maxDistance);

        // Get player's view direction
        float pitch = player.getPitch() * (MathHelper.PI / 180F);
        float yaw = player.getYaw() * (MathHelper.PI / 180F);
        Vec3d viewVector = new Vec3d(
                -MathHelper.sin(yaw) * MathHelper.cos(pitch),
                -MathHelper.sin(pitch),
                MathHelper.cos(yaw) * MathHelper.cos(pitch)
        ).normalize();

        // Only check blocks within a cone in front of the player
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos targetPos = playerPos.add(x, y, z);
                    
                    // Skip air blocks
                    if (world.isAir(targetPos)) {
                        continue;
                    }
                    
                    // Quick distance check
                    double distSquared = eyePos.squaredDistanceTo(
                            targetPos.getX() + 0.5,
                            targetPos.getY() + 0.5,
                            targetPos.getZ() + 0.5
                    );
                    if (distSquared > maxDistance * maxDistance) {
                        continue;
                    }
                    
                    // Only check blocks that are roughly in front of the player
                    Vec3d toBlock = new Vec3d(
                            targetPos.getX() + 0.5 - eyePos.getX(),
                            targetPos.getY() + 0.5 - eyePos.getY(),
                            targetPos.getZ() + 0.5 - eyePos.getZ()
                    ).normalize();
                    
                    // Skip blocks that are behind or far to the side
                    if (viewVector.dotProduct(toBlock) < 0.3) {
                        continue;
                    }
                    
                    // Perform the expensive LOS check only if quick checks pass
                    if (LineOfSightUtils.isBlockInLineOfSight(player, targetPos, maxDistance)) {
                        world.setBlockState(targetPos, net.minecraft.block.Blocks.GREEN_STAINED_GLASS.getDefaultState());
                    }
                }
            }
        }
    }

    /**
     * Fills all non-air blocks that are within the player's field of view with blue stained glass blocks.
     * Optimized to only check blocks within the FOV cone.
     * 
     * @param player The player to check from
     * @param maxDistance The maximum distance to check
     */
    public static void fillFieldOfViewWithGlass(PlayerEntity player, double maxDistance) {
        World world = player.getWorld();
        Vec3d eyePos = player.getEyePos();
        BlockPos playerPos = player.getBlockPos();
        int range = (int) Math.ceil(maxDistance);

        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos targetPos = playerPos.add(x, y, z);
                    
                    // Skip air blocks
                    if (world.isAir(targetPos)) {
                        continue;
                    }
                    
                    // Quick distance check
                    double distSquared = eyePos.squaredDistanceTo(
                            targetPos.getX() + 0.5,
                            targetPos.getY() + 0.5,
                            targetPos.getZ() + 0.5
                    );
                    if (distSquared > maxDistance * maxDistance) {
                        continue;
                    }
                    
                    // Calculate direction to the block
                    Vec3d direction = new Vec3d(
                            targetPos.getX() + 0.5 - eyePos.getX(),
                            targetPos.getY() + 0.5 - eyePos.getY(),
                            targetPos.getZ() + 0.5 - eyePos.getZ()
                    ).normalize();
                    
                    // Check if the block is within the player's field of view
                    if (LineOfSightUtils.isWithinFieldOfView(player, direction)) {
                        world.setBlockState(targetPos, net.minecraft.block.Blocks.BLUE_STAINED_GLASS.getDefaultState());
                    }
                }
            }
        }
    }

    /**
     * Fills all non-air blocks that are rendered on the player's screen with yellow stained glass blocks.
     * Uses isBlockRenderedOnScreen to determine if the block is rendered on the screen.
     * Optimized with early FOV filtering before expensive raycast checks.
     * 
     * @param player The player to check from
     * @param maxDistance The maximum distance to check
     */
    public static void fillRenderedBlocksWithGlass(PlayerEntity player, double maxDistance) {
        World world = player.getWorld();
        Vec3d eyePos = player.getEyePos();
        BlockPos playerPos = player.getBlockPos();
        int range = (int) Math.ceil(maxDistance);

        // Get player's view direction for early FOV filtering
        float pitch = player.getPitch() * (MathHelper.PI / 180F);
        float yaw = player.getYaw() * (MathHelper.PI / 180F);
        Vec3d viewVector = new Vec3d(
                -MathHelper.sin(yaw) * MathHelper.cos(pitch),
                -MathHelper.sin(pitch),
                MathHelper.cos(yaw) * MathHelper.cos(pitch)
        ).normalize();

        // Get FOV from client
        double fovDegrees = 70.0; // Default FOV, safe fallback
        try {
            net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
            if (client != null && client.options != null) {
                fovDegrees = client.options.getFov().getValue();
            }
        } catch (Exception e) {
            // Use default FOV if client is not available
        }
        double fovRadians = (fovDegrees / 2.0) * (MathHelper.PI / 180.0);
        double minDotProduct = Math.cos(fovRadians);

        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos targetPos = playerPos.add(x, y, z);
                    
                    // Skip air blocks
                    if (world.isAir(targetPos)) {
                        continue;
                    }
                    
                    // Quick distance check
                    double distSquared = eyePos.squaredDistanceTo(
                            targetPos.getX() + 0.5,
                            targetPos.getY() + 0.5,
                            targetPos.getZ() + 0.5
                    );
                    if (distSquared > maxDistance * maxDistance) {
                        continue;
                    }
                    
                    // Early FOV filtering to avoid expensive raycast for blocks clearly outside FOV
                    Vec3d toBlock = new Vec3d(
                            targetPos.getX() + 0.5 - eyePos.getX(),
                            targetPos.getY() + 0.5 - eyePos.getY(),
                            targetPos.getZ() + 0.5 - eyePos.getZ()
                    ).normalize();
                    
                    // Skip blocks outside the FOV cone before expensive check
                    if (viewVector.dotProduct(toBlock) < minDotProduct) {
                        continue;
                    }
                    
                    // Use the isBlockRenderedOnScreen method to check if the block is rendered
                    if (LineOfSightUtils.isBlockRenderedOnScreen(player, targetPos, maxDistance)) {
                        world.setBlockState(targetPos, net.minecraft.block.Blocks.BLACK_CONCRETE.getDefaultState());
                    }
                }
            }
        }
    }
}
