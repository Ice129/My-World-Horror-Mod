package horror.blueice129.feature;

// import horror.blueice129.HorrorMod129;
// import horror.blueice129.utils.ChunkLoader;
import horror.blueice129.utils.TorchPlacer;
import horror.blueice129.utils.BlockModificationUtils;
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
import java.util.Collections;
import java.util.Comparator;
// import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
// import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
// import java.util.UUID;

public final class UndergroundTunnelEvent {
    private static final Random RANDOM = Random.create();

    private static final int AREA_STABLE_TICKS = 20 * 10;
    private static final int EVENT_TICKS = 20 * 30;
    private static final int LOOK_CANCEL_TICKS = 20 * 5;
    private static final int TORCH_INTERVAL = 7 * 3; // 
    private static final int MIN_CAVE_VOLUME = 20;
    private static final int SEARCH_RADIUS = 48;
    private static final double HEARING_RADIUS = 24.0;
    private static final double HEARING_RADIUS_SQ = HEARING_RADIUS * HEARING_RADIUS;
    private static final double CLOSE_CANCEL_RADIUS = 9.0;
    private static final double CLOSE_CANCEL_RADIUS_SQ = CLOSE_CANCEL_RADIUS * CLOSE_CANCEL_RADIUS;
    private static final int MAX_CAVE_FILL = 256;
    // private static final int MAX_PATH_NODES = 180;
    private static int listPosition = 0;
    private static BlockPos[] previousPlayerLocations = new BlockPos[AREA_STABLE_TICKS / 10];

    public static void shouldTriggerEvent(MinecraftServer server, ServerPlayerEntity player) {
        // needs previous player locations to ensure the player is still enough
        if (listPosition < previousPlayerLocations.length) {
            previousPlayerLocations[listPosition] = player.getBlockPos();
            listPosition++;
        } else {
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

    public static boolean triggerEvent(MinecraftServer server, ServerPlayerEntity player) {
        ServerWorld world = (ServerWorld) player.getWorld();
        BlockPos playerPos = player.getBlockPos();

        // Find tunnel start area
        BlockPos tunnelStart = findTunnelStart(world, playerPos);
        BlockPos caveWallPos = findCaveWall(world, tunnelStart, player);
        BlockPos[] tunnelPath = findTunnelPath(world, caveWallPos, playerPos);
        BlockPos[] cleanedTunnelPath = cleanTunnelPath(world, tunnelPath);
        boolean carveStage1 = carveTunnel(world, cleanedTunnelPath, player);
        // for (BlockPos pos : cleanedTunnelPath) {
            // world.setBlockState(pos, Blocks.AIR.getDefaultState());
        // }
        return true;
    }

    public static boolean carveTunnel(ServerWorld world, BlockPos[] tunnelPath, ServerPlayerEntity player) {
        // Carve out the tunnel path, making sure to place blocks on the floor of the path if there is air below the path, torches every 7 blocks, and remove gravel
        for (int i = 0; i < tunnelPath.length; i++) {
            BlockPos pos = tunnelPath[i];
            // check if block below is air, and also not within the tunnel path list
            BlockPos belowPos = pos.down();
            if ((world.getBlockState(belowPos).isAir() || world.getBlockState(belowPos).isOf(Blocks.CAVE_AIR)) && !List.of(tunnelPath).contains(belowPos)) {
                world.setBlockState(belowPos, Blocks.COBBLESTONE.getDefaultState());
            }
            
            BlockModificationUtils.placeAirBlock(world, pos, Blocks.COBBLESTONE);

            if (i % TORCH_INTERVAL == 0) {
                // place torches every TORCH_INTERVAL blocks
                world.setBlockState(pos, Blocks.AIR.getDefaultState());
                TorchPlacer.placeTorch(world, pos, RANDOM, player);
            }
        }
        return true;
    }
        

    public static BlockPos[] cleanTunnelPath(ServerWorld world, BlockPos[] tunnelPath) {
        // remove the end of the tunnel path until the final few blocks do not have any air conected on any side
        int endIndex = tunnelPath.length - 1;
        while (endIndex >= 0) {
            BlockPos pos = tunnelPath[endIndex];
            boolean hasAirNeighbor = false;
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = pos.offset(dir);
                BlockState neighborState = world.getBlockState(neighborPos);
                if (neighborState.isAir() || neighborState.isOf(Blocks.CAVE_AIR)) {
                    hasAirNeighbor = true;
                    break;
                }
            }
            if (!hasAirNeighbor) {
                break;
            }
            endIndex--;
        }
        // return the cleaned tunnel path
        BlockPos[] cleanedTunnelPath = new BlockPos[endIndex + 1];
        System.arraycopy(tunnelPath, 0, cleanedTunnelPath, 0, endIndex + 1);
        return cleanedTunnelPath;
    }

    public static BlockPos[] findTunnelPath(ServerWorld world, BlockPos start, BlockPos end) {
        Random random = world.getRandom();

        LinkedHashSet<BlockPos> visited = new LinkedHashSet<>();

        List<BlockPos> waypoints = new ArrayList<>();
        waypoints.add(start);

        Vec3d startVec = Vec3d.ofCenter(start);
        Vec3d endVec = Vec3d.ofCenter(end);

        Vec3d direction = endVec.subtract(startVec);
        double distance = direction.length();

        if (distance == 0) {
            visited.add(start);
            visited.add(start.up());
            return visited.toArray(new BlockPos[0]);
        }

        direction = direction.normalize();

        // Horizontal perpendicular vector
        Vec3d perpendicular = new Vec3d(-direction.z, 0, direction.x);
        if (perpendicular.lengthSquared() > 0.0001) {
            perpendicular = perpendicular.normalize();
        }

        // One waypoint roughly every 10 blocks
        int waypointCount = Math.max(1, (int) (distance / 10));

        for (int i = 1; i <= waypointCount; i++) {
            double t = i / (double) (waypointCount + 1);

            Vec3d point = startVec.add(direction.multiply(distance * t));

            // Up to 3 blocks sideways
            double sideways = random.nextDouble() * 6.0 - 3.0;

            // Up to 1 block vertically
            double vertical = random.nextDouble() * 2.0 - 1.0;

            point = point
                    .add(perpendicular.multiply(sideways))
                    .add(0, vertical, 0);

            waypoints.add(BlockPos.ofFloored(point));
        }

        waypoints.add(end);

        // Connect every waypoint pair
        for (int i = 0; i < waypoints.size() - 1; i++) {
            carveSegment(waypoints.get(i), waypoints.get(i + 1), visited);
        }

        return visited.toArray(new BlockPos[0]);
    }

    private static void carveSegment(BlockPos start, BlockPos end, Set<BlockPos> visited) {
        Vec3d startVec = Vec3d.ofCenter(start);
        Vec3d endVec = Vec3d.ofCenter(end);

        Vec3d direction = endVec.subtract(startVec);
        double distance = direction.length();

        if (distance == 0) {
            visited.add(start);
            visited.add(start.up());
            return;
        }

        direction = direction.normalize();

        BlockPos previous = BlockPos.ofFloored(startVec);

        for (double d = 0; d <= distance; d += 0.25) {
            Vec3d point = startVec.add(direction.multiply(d));
            BlockPos current = BlockPos.ofFloored(point);

            int x = previous.getX();
            int y = previous.getY();
            int z = previous.getZ();

            while (x != current.getX() || y != current.getY() || z != current.getZ()) {

                List<Integer> axes = new ArrayList<>(3);

                if (x != current.getX())
                    axes.add(0);
                if (y != current.getY())
                    axes.add(1);
                if (z != current.getZ())
                    axes.add(2);

                Collections.shuffle(axes);

                for (int axis : axes) {
                    switch (axis) {
                        case 0 -> x += Integer.signum(current.getX() - x);
                        case 1 -> y += Integer.signum(current.getY() - y);
                        case 2 -> z += Integer.signum(current.getZ() - z);
                    }

                    BlockPos p = new BlockPos(x, y, z);
                    visited.add(p);
                    visited.add(p.up());
                }
            }

            visited.add(current);
            visited.add(current.up());
            previous = current;
        }
    }

    public static BlockPos findCaveWall(ServerWorld world, BlockPos tunnelStart, ServerPlayerEntity player) {
        // find what cardinal direction the player is and search in that direction for a
        // wall
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
            int xOffset = RANDOM.nextInt((int) (SEARCH_RADIUS * 2.2)) - SEARCH_RADIUS;
            int zOffset = RANDOM.nextInt((int) (SEARCH_RADIUS * 2.2)) - SEARCH_RADIUS;
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
        PriorityQueue<BlockPos> queue = new PriorityQueue<>(
                Comparator.comparingInt(pos -> pos.getManhattanDistance(startPos)));
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

    public static void tick(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            // shouldTriggerEvent(server, player);
            int x = player.getBlockPos().getX();
        }
    }
}