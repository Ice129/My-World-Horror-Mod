package horror.blueice129.feature;

import horror.blueice129.HorrorMod129;
import horror.blueice129.utils.ChunkLoader;
import horror.blueice129.utils.TorchPlacer;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;

public final class UndergroundTunnelEvent {
    private static final Random RANDOM = Random.create();

    private static final int AREA_STABLE_TICKS = 20 * 10;
    private static final int EVENT_TICKS = 20 * 30;
    private static final int LOOK_CANCEL_TICKS = 20 * 5;
    private static final int TORCH_INTERVAL = 7;
    private static final int MIN_CAVE_VOLUME = 20;
    private static final int SEARCH_RADIUS = 48;
    private static final double HEARING_RADIUS = 24.0;
    private static final double HEARING_RADIUS_SQ = HEARING_RADIUS * HEARING_RADIUS;
    private static final double CLOSE_CANCEL_RADIUS = 9.0;
    private static final double CLOSE_CANCEL_RADIUS_SQ = CLOSE_CANCEL_RADIUS * CLOSE_CANCEL_RADIUS;
    private static final int MAX_CAVE_FILL = 256;
    private static final int MAX_PATH_NODES = 180;
    private static int listPosition = 0;
    private static BlockPos[] previousPlayerLocations = new BlockPos[AREA_STABLE_TICKS / 10];

    public static void shouldTriggerEvent(MinecraftServer server, ServerPlayerEntity player) {
        // needs previous player locations to ensure the player is still enough
        if (listPosition < previousPlayerLocations.length) {
            previousPlayerLocations[listPosition] = player.getBlockPos();
            listPosition++;
        }
        else {
            listPosition = 0;
            previousPlayerLocations[listPosition] = player.getBlockPos();
        }

        // check if all previous player locations are within 3 x 3 x 3 area
        // done by checking the biggest difference in x, y, and z coordinates
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BlockPos pos : previousPlayerLocations) {
            if (pos == null) {
                return;
            }
            minX = Math.min(minX, pos.getX());
            maxX = Math.max(maxX, pos.getX());
            minY = Math.min(minY, pos.getY());
            maxY = Math.max(maxY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        // check if the difference in each dimension is within the 3 x 3 x 3 area
        if (maxX - minX <= 3 && maxY - minY <= 3 && maxZ - minZ <= 3) {
            triggerEvent(server, player);
        }
    }

    public static void triggerEvent(MinecraftServer server, ServerPlayerEntity player) {
        ServerWorld world = (ServerWorld) player.getWorld();
        BlockPos playerPos = player.getBlockPos();

        // Find tunnel start area
        BlockPos tunnelStart = findTunnelStart(world, playerPos);
        BlockPos caveWallPos = findCaveWall(world, tunnelStart);
    }

    public static BlockPos findCaveWall(ServerWorld world, BlockPos tunnelStart) {
        // find what cardinal direction the player is and search in that direction for a wall
        Direction playerDirection = Direction.fromRotation(world.getPlayerByUuid(player.getUuid()).getYaw());
        BlockPos currentPos = tunnelStart;
        while (true) {
            currentPos = currentPos.offset(playerDirection);
            BlockState state = world.getBlockState(currentPos);
            if (!state.isAir() && !state.isOf(Blocks.CAVE_AIR)) {
                return currentPos;
            }
        }
    }

    public static BlockPos findTunnelStart(ServerWorld world, BlockPos playerPos) {
        // 20 attempts
        for (int i = 0; i < 20; i++) {
            int xOffset = RANDOM.nextInt((int) (SEARCH_RADIUS * 2.5)) - SEARCH_RADIUS;
            int zOffset = RANDOM.nextInt((int) (SEARCH_RADIUS * 2.5)) - SEARCH_RADIUS;
            BlockPos randomPos = playerPos.add(xOffset, 0, zOffset);
            // look 5 blocks up and down to find air/cave air
            for (int yOffset = -5; yOffset <= 5; yOffset++) {
                BlockPos checkPos = randomPos.add(0, yOffset, 0);
                BlockState state = world.getBlockState(checkPos);
                if (state.isAir() || state.isOf(Blocks.CAVE_AIR)) {
                    // check if the cave volume is large enough
                    int caveVolume = calculateCaveVolume(world, checkPos);
                    if (caveVolume >= MIN_CAVE_VOLUME) {
                        return checkPos;
                    }
                }
            }            
        }
        return null;
    }

    public static int calculateCaveVolume(ServerWorld world, BlockPos startPos) {
        Set<BlockPos> visited = new HashSet<>();
        PriorityQueue<BlockPos> queue = new PriorityQueue<>(Comparator.comparingInt(pos -> pos.getManhattanDistance(startPos)));
        queue.add(startPos);
        visited.add(startPos);
        int volume = 0;

        while (!queue.isEmpty() && volume < MAX_CAVE_FILL) {
            BlockPos current = queue.poll();
            BlockState state = world.getBlockState(current);
            if (state.isAir() || state.isOf(Blocks.CAVE_AIR)) {
                volume++;
                for (Direction dir : Direction.values()) {
                    BlockPos neighbor = current.offset(dir);
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        }

        return volume;
    }
}