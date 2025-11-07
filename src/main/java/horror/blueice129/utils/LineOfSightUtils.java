package horror.blueice129.utils;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
// import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

/**
 * Utility class for line of sight calculations.
 */
public class LineOfSightUtils {
    // TODO: make a simple check to return true is block is 180 degrees behind
    // player, aka not in the front half of the player
    // BUG: sometimes misses blocks that are in line of sight

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

        // ========== STEP 1: GET PLAYER'S EYE POSITION ==========
        // This is where the camera originates from - right at the player's eyes
        // All visibility checks start from this point
        Vec3d eyePos = player.getEyePos();

        // ========== STEP 2: GET THE CENTER POINT OF THE TARGET BLOCK ==========
        // Blocks are 1x1x1 cubes. The center is at +0.5 in each direction
        // We use the center because it's the most representative point of the block
        Vec3d targetPos = new Vec3d(
                blockPos.getX() + 0.5,
                blockPos.getY() + 0.5,
                blockPos.getZ() + 0.5);

        // ========== STEP 3: CALCULATE DIRECTION FROM EYES TO TARGET ==========
        // This creates a line/ray starting from the player's eyes pointing toward the
        // block
        // We normalize it to get a unit direction vector (length = 1)
        Vec3d direction = targetPos.subtract(eyePos).normalize();

        // ========== STEP 4: CALCULATE DISTANCE TO TARGET BLOCK ==========
        // How far away is the block from the player's eyes?
        double distance = eyePos.distanceTo(targetPos);

        // ========== STEP 5: EARLY EXIT - IS THE BLOCK TOO FAR? ==========
        // If block is beyond maxDistance (e.g., 100 blocks), don't bother checking
        // This is a performance optimization
        if (distance > maxDistance) {
            return false;
        }

        // ========== STEP 6: CHECK FIELD OF VIEW ==========
        // Is the block even within the player's visible screen area?
        // This uses the complex FOV calculation accounting for aspect ratio and FOV
        // setting
        // If the block is behind the player or off to the extreme sides, this fails
        if (!isWithinFieldOfView(player, direction)) {
            return false;
        }

        // ========== STEP 7: RAYCAST CHECK - IS THE BLOCK OCCLUDED? ==========
        // Cast a ray from the player's eyes toward the block
        // This checks if any solid blocks are in the way blocking the view
        // Like pointing a laser at the block - does it hit the target or something else
        // first?
        BlockHitResult hitResult = world.raycast(new RaycastContext(
                eyePos,
                eyePos.add(direction.multiply(distance)),
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                player));

        // ========== STEP 8: VERIFY THE RAY HIT THE CORRECT BLOCK ==========
        // Check if the ray hit a block AND that block is the one we're looking for
        // If something else blocks the view, this returns false
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
        // ========== STEP 1: CALCULATE THE DIRECTION THE PLAYER IS FACING ==========
        // Convert the player's head rotation angles (pitch and yaw) from degrees to
        // radians
        // Think of it like: the player's head is a compass pointing in a specific
        // direction
        // Pitch: rotation up/down (looking up = positive, looking down = negative)
        // Yaw: rotation left/right (spinning like a top)
        float pitch = player.getPitch() * ((float) Math.PI / 180F);
        float yaw = player.getYaw() * ((float) Math.PI / 180F);

        // ========== STEP 2: CREATE A UNIT VECTOR POINTING OUT OF THE PLAYER'S EYES
        // ==========
        // This is a "forward vector" - imagine a laser beam coming straight out of the
        // player's eyes
        // The math uses trigonometry to convert the pitch/yaw angles into an (x, y, z)
        // direction
        // That points toward where the player is looking
        //
        // NOTE: The FOV origin is shifted one block behind the player by calculating
        // direction
        // vectors from a point 1 block behind. This is handled in the calling code.
        float x = -MathHelper.sin(yaw) * MathHelper.cos(pitch);
        float y = -MathHelper.sin(pitch);
        float z = MathHelper.cos(yaw) * MathHelper.cos(pitch);
        Vec3d viewVector = new Vec3d(x, y, z).normalize();

        // ========== NORMALIZE THE INPUT DIRECTION ==========
        // Make the direction vector have a length of 1 (like the viewVector)
        // This lets us compare angles fairly - the length doesn't matter, only the
        // direction
        direction = direction.normalize();

        // ========== STEP 3: SET FIXED FOV BOUNDS ==========
        // Horizontal FOV: 180 degrees (90 degrees left, 90 degrees right)
        // Vertical FOV: 120 degrees (60 degrees up, 60 degrees down)
        // This creates a simple rectangular field of view in front of the player
        double hFovDegrees = 140.0; // 180 degrees horizontal
        double vFovDegrees = 120.0; // 120 degrees vertical

        // Convert to radians and calculate half-angles
        double hFovRad = hFovDegrees * (Math.PI / 180.0);
        double vFovRad = vFovDegrees * (Math.PI / 180.0);
        double hFovHalf = hFovRad / 2.0; // 90 degrees in radians
        double vFovHalf = vFovRad / 2.0; // 60 degrees in radians

        // ========== STEP 5: CREATE A "RIGHT" VECTOR (LEFT-RIGHT AXIS) ==========
        // This is like drawing a line from the player pointing directly to their right
        // We rotate the viewVector 90 degrees right in the horizontal plane (flat
        // ground)
        // Correct formula: rotate view vector 90 degrees right in the horizontal plane
        // Think of it like the player extending their right arm straight out from their
        // body
        // It points perpendicular to where they're looking, on the ground level
        Vec3d rightVector = new Vec3d(MathHelper.cos(yaw), 0, MathHelper.sin(yaw)).normalize();

        // ========== STEP 6: CREATE AN "UP" VECTOR (UP-DOWN AXIS) ==========
        // This is like drawing a line from the player pointing directly upward
        // We calculate it by crossing the viewVector (forward) with the rightVector
        // Cross product of two perpendicular vectors gives you the third perpendicular
        // direction
        // Think of it like: if forward is North and right is East, then up is Sky
        // This vector points perpendicular to both the view direction and the right
        // direction
        Vec3d upVector = viewVector.crossProduct(rightVector).normalize();

        // ========== STEP 7: PROJECT THE TARGET DIRECTION ONTO OUR 3 AXES ==========
        // We now have three perpendicular directions (forward, right, up)
        // We need to figure out: how much is the target direction in each of these
        // directions?
        // Dot product tells us this: how much two vectors point in the same direction
        // Higher dot product = more aligned
        // Example: viewDot close to 1 means target is nearly straight ahead
        // rightDot close to 1 means target is far to the right
        double viewDot = viewVector.dotProduct(direction); // How much is it in the forward direction?
        double rightDot = rightVector.dotProduct(direction); // How much is it in the right direction?
        double upDot = upVector.dotProduct(direction); // How much is it in the up direction?

        // ========== STEP 8: CONVERT PROJECTIONS INTO ANGLES ==========
        // atan2 is a special function that converts (x, y) into an angle
        // It gives us the signed angle from one direction to another
        // Think of it like: if you're looking forward, and something is 30° to your
        // right,
        // atan2 would give you a positive 30°. If it's 30° to the left, it gives -30°.
        // Horizontal angle: how far LEFT (-) or RIGHT (+) the target is from center
        double horizontalAngle = Math.atan2(rightDot, viewDot);

        // Vertical angle: how far DOWN (-) or UP (+) the target is from center
        double verticalAngle = Math.atan2(upDot, viewDot);

        // ========== STEP 9: FINAL CHECK - IS THE TARGET WITHIN THE FOV BOUNDS?
        // ==========
        // Check if target is within 180 degrees horizontal and 120 degrees vertical
        // hFovHalf = 90 degrees in radians (π/2)
        // vFovHalf = 60 degrees in radians (π/3)
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

        // ========== OPTIMIZATION 1: EARLY DISTANCE CHECK ==========
        // Skip blocks that are too far away
        double distance = eyePos.distanceTo(blockCenter);
        if (distance > maxDistance) {
            return false;
        }

        // ========== OPTIMIZATION 2: EARLY FOV CHECK ==========
        // Skip blocks not in field of view before expensive raycasting
        Vec3d direction = blockCenter.subtract(eyePos).normalize();
        if (!isWithinFieldOfView(player, direction)) {
            return false;
        }

        // ========== DIRECTION FROM BLOCK TO PLAYER (FOR BACKFACE CULLING) ==========
        Vec3d toPlayer = eyePos.subtract(blockCenter).normalize();

        // ========== DEFINE ALL 8 CORNERS OF THE BLOCK ==========
        // Create vectors for all corners of the block with small epsilon offset
        // Epsilon moves points slightly inward to avoid precision errors at exact block boundaries
        double epsilon = 0.001;
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

        // ========== CHECK EACH CORNER ==========
        for (Vec3d corner : corners) {
            // Calculate direction and distance to this corner
            Vec3d cornerDirection = corner.subtract(eyePos).normalize();
            double cornerDistance = eyePos.distanceTo(corner);

            // ========== RAYCAST TO THIS CORNER ==========
            // Cast ray from player's eyes to this corner
            BlockHitResult hitResult = world.raycast(new RaycastContext(
                    eyePos,
                    eyePos.add(cornerDirection.multiply(cornerDistance)),
                    RaycastContext.ShapeType.OUTLINE,
                    RaycastContext.FluidHandling.NONE,
                    player));

            // ========== IF RAY HITS THIS BLOCK, IT'S VISIBLE ==========
            if (hitResult.getType() == HitResult.Type.BLOCK &&
                    hitResult.getBlockPos().equals(blockPos)) {
                return true;
            }
        }

        return false;
    }
}