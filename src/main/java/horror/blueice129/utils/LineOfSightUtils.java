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
        // This creates a line/ray starting from the player's eyes pointing toward the block
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
        // This uses the complex FOV calculation accounting for aspect ratio and FOV setting
        // If the block is behind the player or off to the extreme sides, this fails
        if (!isWithinFieldOfView(player, direction)) {
            return false;
        }

        // ========== STEP 7: RAYCAST CHECK - IS THE BLOCK OCCLUDED? ==========
        // Cast a ray from the player's eyes toward the block
        // This checks if any solid blocks are in the way blocking the view
        // Like pointing a laser at the block - does it hit the target or something else first?
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
        // Convert the player's head rotation angles (pitch and yaw) from degrees to radians
        // Think of it like: the player's head is a compass pointing in a specific direction
        // Pitch: rotation up/down (looking up = positive, looking down = negative)
        // Yaw: rotation left/right (spinning like a top)
        float pitch = player.getPitch() * ((float) Math.PI / 130F);
        float yaw = player.getYaw() * ((float) Math.PI / 140F);

        // ========== STEP 2: CREATE A UNIT VECTOR POINTING OUT OF THE PLAYER'S EYES ==========
        // This is a "forward vector" - imagine a laser beam coming straight out of the player's eyes
        // The math uses trigonometry to convert the pitch/yaw angles into an (x, y, z) direction
        // That points toward where the player is looking
        // 
        // NOTE: The FOV origin is shifted one block behind the player by calculating direction
        // vectors from a point 1 block behind. This is handled in the calling code.
        float x = -MathHelper.sin(yaw) * MathHelper.cos(pitch);
        float y = -MathHelper.sin(pitch);
        float z = MathHelper.cos(yaw) * MathHelper.cos(pitch);
        Vec3d viewVector = new Vec3d(x, y, z).normalize();

        // ========== NORMALIZE THE INPUT DIRECTION ==========
        // Make the direction vector have a length of 1 (like the viewVector)
        // This lets us compare angles fairly - the length doesn't matter, only the direction
        direction = direction.normalize();

        // ========== STEP 3: SET FIXED FOV BOUNDS ==========
        // Horizontal FOV: 180 degrees (90 degrees left, 90 degrees right)
        // Vertical FOV: 120 degrees (60 degrees up, 60 degrees down)
        // This creates a simple rectangular field of view in front of the player
        double hFovDegrees = 180.0;  // 180 degrees horizontal
        double vFovDegrees = 120.0;  // 120 degrees vertical
        
        // Convert to radians and calculate half-angles
        double hFovRad = hFovDegrees * (Math.PI / 180.0);
        double vFovRad = vFovDegrees * (Math.PI / 180.0);
        double hFovHalf = hFovRad / 2.0;  // 90 degrees in radians
        double vFovHalf = vFovRad / 2.0;  // 60 degrees in radians

        // ========== STEP 5: CREATE A "RIGHT" VECTOR (LEFT-RIGHT AXIS) ==========
        // This is like drawing a line from the player pointing directly to their right
        // We rotate the viewVector 90 degrees right in the horizontal plane (flat ground)
        // Correct formula: rotate view vector 90 degrees right in the horizontal plane
        // Think of it like the player extending their right arm straight out from their body
        // It points perpendicular to where they're looking, on the ground level
        Vec3d rightVector = new Vec3d(MathHelper.cos(yaw), 0, MathHelper.sin(yaw)).normalize();

        // ========== STEP 6: CREATE AN "UP" VECTOR (UP-DOWN AXIS) ==========
        // This is like drawing a line from the player pointing directly upward
        // We calculate it by crossing the viewVector (forward) with the rightVector
        // Cross product of two perpendicular vectors gives you the third perpendicular direction
        // Think of it like: if forward is North and right is East, then up is Sky
        // This vector points perpendicular to both the view direction and the right direction
        Vec3d upVector = viewVector.crossProduct(rightVector).normalize();

        // ========== STEP 7: PROJECT THE TARGET DIRECTION ONTO OUR 3 AXES ==========
        // We now have three perpendicular directions (forward, right, up)
        // We need to figure out: how much is the target direction in each of these directions?
        // Dot product tells us this: how much two vectors point in the same direction
        // Higher dot product = more aligned
        // Example: viewDot close to 1 means target is nearly straight ahead
        //          rightDot close to 1 means target is far to the right
        double viewDot = viewVector.dotProduct(direction);   // How much is it in the forward direction?
        double rightDot = rightVector.dotProduct(direction); // How much is it in the right direction?
        double upDot = upVector.dotProduct(direction);       // How much is it in the up direction?
        
        // ========== STEP 8: CONVERT PROJECTIONS INTO ANGLES ==========
        // atan2 is a special function that converts (x, y) into an angle
        // It gives us the signed angle from one direction to another
        // Think of it like: if you're looking forward, and something is 30° to your right,
        // atan2 would give you a positive 30°. If it's 30° to the left, it gives -30°.
        // Horizontal angle: how far LEFT (-) or RIGHT (+) the target is from center
        double horizontalAngle = Math.atan2(rightDot, viewDot);
        
        // Vertical angle: how far DOWN (-) or UP (+) the target is from center
        double verticalAngle = Math.atan2(upDot, viewDot);

        // ========== STEP 9: FINAL CHECK - IS THE TARGET WITHIN THE FOV BOUNDS? ==========
        // Check if target is within 180 degrees horizontal and 120 degrees vertical
        // hFovHalf = 90 degrees in radians (π/2)
        // vFovHalf = 60 degrees in radians (π/3)
        return Math.abs(horizontalAngle) <= hFovHalf && Math.abs(verticalAngle) <= vFovHalf;
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

        // ========== STEP 1: GET BLOCK CENTER AND CHECK DISTANCE ==========
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

        // ========== STEP 2: CALCULATE DIRECTION FROM BLOCK TO PLAYER ==========
        // Direction from block center to player eye (for face culling)
        // This helps us figure out which faces of the block are facing toward the player
        // Think of it like shining a light from the block toward the player - which faces light up?
        Vec3d toPlayer = eyePos.subtract(blockCenter).normalize();

        // ========== STEP 3: DEFINE ALL 6 FACES OF THE BLOCK + CENTER POINT ==========
        // Blocks are cubes with 6 faces + a center point (7 total check points)
        // We check the center first (helps catch blocks that are diagonally visible)
        // Then we check each face: front, back, left, right, top, bottom
        // Coordinates for each face: +0.5 means the center of that axis, 0/+1 means the edge
        // Example: (X, Y, Z+1) is the BACK face of the block (when looking north)
        Vec3d[] faceCenters = {
            blockCenter,                                                 // Face 0: Center of block
            new Vec3d(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ()),     // Face 1: Front face (Z=0 edge)
            new Vec3d(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 1), // Face 2: Back face (Z=1 edge)
            new Vec3d(blockPos.getX(), blockPos.getY() + 0.5, blockPos.getZ() + 0.5),     // Face 3: Left face (X=0 edge)
            new Vec3d(blockPos.getX() + 1, blockPos.getY() + 0.5, blockPos.getZ() + 0.5), // Face 4: Right face (X=1 edge)
            new Vec3d(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5),     // Face 5: Bottom face (Y=0 edge)
            new Vec3d(blockPos.getX() + 0.5, blockPos.getY() + 1, blockPos.getZ() + 0.5)  // Face 6: Top face (Y=1 edge)
        };

        // ========== STEP 4: DEFINE THE NORMAL DIRECTION FOR EACH FACE ==========
        // A "normal" is a vector pointing outward from the face, perpendicular to it
        // It tells us which way the face is "facing"
        // Example: Front face's normal points in the -Z direction (outward from front)
        // The center point (Face 0) has no normal, so it won't be culled (always checked)
        Vec3d[] faceNormals = {
            new Vec3d(0, 0, 0),  // Face 0: Center - no normal, always include
            new Vec3d(0, 0, -1), // Face 1: Front face normal (points outward in -Z)
            new Vec3d(0, 0, 1),  // Face 2: Back face normal (points outward in +Z)
            new Vec3d(-1, 0, 0), // Face 3: Left face normal (points outward in -X)
            new Vec3d(1, 0, 0),  // Face 4: Right face normal (points outward in +X)
            new Vec3d(0, -1, 0), // Face 5: Bottom face normal (points outward in -Y)
            new Vec3d(0, 1, 0)   // Face 6: Top face normal (points outward in +Y)
        };

        // ========== STEP 5: CHECK EACH FACE TO SEE IF IT'S VISIBLE ==========
        // Check only front-facing faces (optimization)
        for (int i = 0; i < faceCenters.length; i++) {
            Vec3d faceCenter = faceCenters[i];
            Vec3d faceNormal = faceNormals[i];

            // ========== FACE CULLING: SKIP BACK-FACING FACES ==========
            // If a face is pointing away from the player, we can skip checking it
            // This is "backface culling" - like not looking at the back of a wall
            // How do we know if it's pointing away? Take the dot product of the normal with toPlayer
            // If dot product < 0.01, the face is roughly facing away, so skip it
            // We use 0.01 instead of 0 to allow faces that are nearly perpendicular to be visible
            // The center point (i=0) has no normal, so we always check it (the if condition is false)
            if (i > 0 && faceNormal.dotProduct(toPlayer) < 0.01) {
                continue;
            }

            // ========== CHECK FIELD OF VIEW FOR THIS FACE POINT ==========
            // Is this face point within the player's screen area?
            Vec3d direction = faceCenter.subtract(eyePos).normalize();
            double faceDistance = eyePos.distanceTo(faceCenter);

            if (!isWithinFieldOfView(player, direction)) {
                continue;
            }

            // ========== RAYCAST FROM EYES TO THIS FACE POINT ==========
            // Shoot a ray from the player's eyes toward this face point
            // Does it hit the target block or get blocked by something else?
            BlockHitResult hitResult = world.raycast(new RaycastContext(
                    eyePos,
                    eyePos.add(direction.multiply(faceDistance)),
                    RaycastContext.ShapeType.OUTLINE,
                    RaycastContext.FluidHandling.NONE,
                    player));

            // ========== IF RAY HITS THE TARGET BLOCK, IT'S VISIBLE! ==========
            // If the ray hit a block AND that block is the one we're checking
            // Return true immediately - the block is rendered on screen
            // If the ray hits something else first, continue to the next face
            if (hitResult.getType() == HitResult.Type.BLOCK &&
                    hitResult.getBlockPos().equals(blockPos)) {
                return true;
            }
        }

        // If no face was visible, the block is not rendered on the player's screen
        return false;
    }
    
}
