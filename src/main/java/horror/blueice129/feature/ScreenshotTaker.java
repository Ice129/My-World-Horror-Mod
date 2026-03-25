package horror.blueice129.feature;

import horror.blueice129.HorrorMod129;
import horror.blueice129.network.ModNetworking;
import horror.blueice129.utils.BlockTypes;
import horror.blueice129.utils.LineOfSightUtils;
// import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import java.util.HashMap;
import java.util.Map;

public class ScreenshotTaker {

    private static final Map<Entity, Integer> pendingDiscards = new HashMap<>();
    private static final Map<Entity, PendingScreenshot> pendingScreenshots = new HashMap<>();
    private static final int ARMOURSTAND_DISCARD_DELAY_TICKS = 10;
    private static final int SCREENSHOT_SEND_DELAY_TICKS = 3;

    private static final int MIN_DISTANCE = 10;
    private static final int MAX_DISTANCE = 20;
    private static final int MAX_ATTEMPTS = 100;
    private static final int VERTICAL_RANGE = 10;
    private static final Random RANDOM = Random.create();

    public static boolean takeScreenshotOfPlayer(ServerPlayerEntity player) {
        HorrorMod129.LOGGER
                .info("ScreenshotTaker: Starting screenshot attempt for player " + player.getName().getString());
        ServerWorld world = player.getServerWorld();
        BlockPos playerPos = player.getBlockPos();
        // HorrorMod129.LOGGER.info("ScreenshotTaker: Player position: " + playerPos);

        BlockPos cameraPos = findSuitableCameraPosition(world, player, playerPos);
        if (cameraPos == null) {
            HorrorMod129.LOGGER.warn(
                    "ScreenshotTaker: Failed to find suitable camera position after " + MAX_ATTEMPTS + " attempts");
            return false;
        }

        // HorrorMod129.LOGGER.info("ScreenshotTaker: Found camera position: " +
        // cameraPos);

        Entity cameraEntity = spawnCameraEntity(world, cameraPos, player);
        if (cameraEntity == null) {
            HorrorMod129.LOGGER.warn("ScreenshotTaker: Failed to spawn camera entity");
            return false;
        }

        // HorrorMod129.LOGGER.info("ScreenshotTaker: Spawned camera entity with ID " +
        // cameraEntity.getId() + " at " + cameraPos);

        pendingScreenshots.put(cameraEntity, new PendingScreenshot(player, SCREENSHOT_SEND_DELAY_TICKS));
        // HorrorMod129.LOGGER.info("ScreenshotTaker: Queued screenshot packet in " +
        // SCREENSHOT_SEND_DELAY_TICKS + " ticks, entity will be discarded in " +
        // BAT_DISCARD_DELAY_TICKS + " ticks");
        pendingDiscards.put(cameraEntity, ARMOURSTAND_DISCARD_DELAY_TICKS);
        return true;
    }

    public static void tick() {
        pendingScreenshots.entrySet().removeIf(entry -> {
            PendingScreenshot pending = entry.getValue();
            int remaining = pending.remainingTicks() - 1;
            if (remaining <= 0) {
                Entity cameraEntity = entry.getKey();
                if (!cameraEntity.isRemoved()) {
                    ModNetworking.sendEntityScreenshot(pending.player(), cameraEntity.getId());
                    // HorrorMod129.LOGGER.info("ScreenshotTaker: Sent screenshot packet to player
                    // for camera entity ID " + cameraEntity.getId());
                } else {
                    HorrorMod129.LOGGER
                            .warn("ScreenshotTaker: Camera entity was removed before screenshot packet could be sent");
                }
                return true;
            }
            pending.setRemainingTicks(remaining);
            return false;
        });

        pendingDiscards.entrySet().removeIf(entry -> {
            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                entry.getKey().discard();
                // HorrorMod129.LOGGER.info("ScreenshotTaker: Discarded camera entity " +
                // entry.getKey().getId());
                return true;
            }
            entry.setValue(remaining);
            return false;
        });
    }

    private static BlockPos findSuitableCameraPosition(ServerWorld world, ServerPlayerEntity player,
            BlockPos playerPos) {
        // HorrorMod129.LOGGER.info("ScreenshotTaker: Searching for camera
        // position...");
        int positionsChecked = 0;
        int airCheckFailures = 0;
        int losFailures = 0;

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            int distance = MIN_DISTANCE + RANDOM.nextInt(MAX_DISTANCE - MIN_DISTANCE);
            double angle = RANDOM.nextDouble() * 2 * Math.PI;
            int offsetX = (int) (Math.cos(angle) * distance);
            int offsetZ = (int) (Math.sin(angle) * distance);

            int x = playerPos.getX() + offsetX;
            int z = playerPos.getZ() + offsetZ;

            for (int yOffset = -VERTICAL_RANGE; yOffset <= VERTICAL_RANGE; yOffset++) {
                int y = playerPos.getY() + yOffset;
                BlockPos candidatePos = new BlockPos(x, y, z);
                positionsChecked++;

                boolean candidatePassable = world.isAir(candidatePos)
                        || BlockTypes.isFoliage(world.getBlockState(candidatePos).getBlock(), true);
                boolean candidateAbovePassable = world.isAir(candidatePos.up())
                        || BlockTypes.isFoliage(world.getBlockState(candidatePos.up()).getBlock(), true);
                boolean candidateBelowGrounded = world.getBlockState(candidatePos.down()).isSolidBlock(world,
                        candidatePos.down());

                if (!candidatePassable || !candidateAbovePassable || !candidateBelowGrounded) {
                    airCheckFailures++;
                    continue;
                }

                if (hasLineOfSightFromBatToPlayer(world, candidatePos, player)
                        && !LineOfSightUtils.hasLineOfSight(player, candidatePos, 30)
                        && !LineOfSightUtils.hasLineOfSight(player, candidatePos.up(), 30)) {
                    //
                    // HorrorMod129.LOGGER.info("ScreenshotTaker: Found position after checking " +
                    // positionsChecked + " positions (air check failures: " + airCheckFailures + ",
                    // LOS failures: " + losFailures + ")");
                    // world.setBlockState(candidatePos, Blocks.GLOWSTONE.getDefaultState());
                    return candidatePos;
                }
                losFailures++;
            }
        }

        HorrorMod129.LOGGER.warn("ScreenshotTaker: Position search failed - checked " + positionsChecked
                + " positions (air check failures: " + airCheckFailures + ", LOS failures: " + losFailures + ")");
        return null;
    }

    private static Entity spawnCameraEntity(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        // HorrorMod129.LOGGER.info("ScreenshotTaker: Creating marker armor stand camera
        // entity");
        ArmorStandEntity cameraEntity = EntityType.ARMOR_STAND.create(world);
        if (cameraEntity == null) {
            HorrorMod129.LOGGER.error("ScreenshotTaker: EntityType.ARMOR_STAND.create() returned null");
            return null;
        }

        // HorrorMod129.LOGGER.info("ScreenshotTaker: Setting camera entity position and
        // orientation");
        Vec3d cameraPos = new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        Vec3d dir = player.getEyePos().subtract(cameraPos);
        double hDist = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        float yaw = (float) (Math.atan2(-dir.x, dir.z) * (180.0 / Math.PI)) + (RANDOM.nextFloat() - 0.5f) * 20.0f;
        float pitch = (float) (Math.atan2(-dir.y, hDist) * (180.0 / Math.PI)) + (RANDOM.nextFloat() - 0.5f) * 20.0f;
        cameraEntity.refreshPositionAndAngles(cameraPos.x, cameraPos.y, cameraPos.z, yaw, pitch);
        applyRotation(cameraEntity, yaw, pitch);
        cameraEntity.setInvisible(true);
        cameraEntity.setInvulnerable(true);
        cameraEntity.setNoGravity(true);
        cameraEntity.setSilent(true);

        // HorrorMod129.LOGGER.info("ScreenshotTaker: Attempting to spawn camera entity
        // in world");
        if (!world.spawnEntity(cameraEntity)) {
            HorrorMod129.LOGGER.error("ScreenshotTaker: world.spawnEntity() returned false");
            return null;
        }

        applyRotation(cameraEntity, yaw, pitch);
        return cameraEntity;
    }

    private static void applyRotation(Entity entity, float yaw, float pitch) {
        entity.setYaw(yaw);
        entity.setPitch(pitch);
        entity.setHeadYaw(yaw);
    }

    private static boolean hasLineOfSightFromBatToPlayer(ServerWorld world, BlockPos batPos,
            ServerPlayerEntity player) {
        Vec3d batEyePos = new Vec3d(batPos.getX() + 0.5, batPos.getY() + 0.5, batPos.getZ() + 0.5);
        Vec3d dir = player.getEyePos().subtract(batEyePos);

        double hDist = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        float yaw = (float) (Math.atan2(-dir.x, dir.z) * (180.0 / Math.PI));
        float pitch = (float) (Math.atan2(-dir.y, hDist) * (180.0 / Math.PI));

        return LineOfSightUtils.hasLineOfSight(world, batEyePos, pitch, yaw, player.getBlockPos(), MAX_DISTANCE * 2,
                player);
    }

    private static final class PendingScreenshot {
        private final ServerPlayerEntity player;
        private int remainingTicks;

        private PendingScreenshot(ServerPlayerEntity player, int remainingTicks) {
            this.player = player;
            this.remainingTicks = remainingTicks;
        }

        private ServerPlayerEntity player() {
            return player;
        }

        private int remainingTicks() {
            return remainingTicks;
        }

        private void setRemainingTicks(int remainingTicks) {
            this.remainingTicks = remainingTicks;
        }
    }
}
