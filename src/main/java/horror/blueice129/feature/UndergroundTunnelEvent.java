package horror.blueice129.feature;

// import horror.blueice129.utils.ChunkLoader;
import horror.blueice129.utils.TorchPlacer;
import horror.blueice129.utils.BlockModificationUtils;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.sound.SoundCategory;
// import net.minecraft.text.Text;

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
import java.util.UUID;
import java.util.Arrays;
// import java.util.UUID;

public final class UndergroundTunnelEvent {
    private static final Random RANDOM = Random.create();

    private static final int AREA_STABLE_TICKS = 20 * 10;
    // private static final int EVENT_TICKS = 20 * 30;
    // private static final int LOOK_CANCEL_TICKS = 20 * 5;
    private static final int TORCH_INTERVAL = 7 * 3; //
    private static final int MIN_CAVE_VOLUME = 20;
    private static final int SEARCH_RADIUS = 48;
    private static final double HEARING_RADIUS = 12.0;
    private static final double HEARING_RADIUS_SQ = HEARING_RADIUS * HEARING_RADIUS;
    // private static final double CLOSE_CANCEL_RADIUS = 9.0;
    // private static final double CLOSE_CANCEL_RADIUS_SQ = CLOSE_CANCEL_RADIUS * CLOSE_CANCEL_RADIUS;
    private static final int MAX_CAVE_FILL = 256;
    // private static final float DYNAMIC_PICKAXE_SPEED = 8.0f;
    // private static final int MAX_PATH_NODES = 180;
    private static int listPosition = 0;
    private static BlockPos[] previousPlayerLocations = new BlockPos[AREA_STABLE_TICKS / 10];
    private static ActiveTunnelEvent activeTunnelEvent;

    public static void recordPlayerLocation(ServerPlayerEntity player) {
        if (listPosition < previousPlayerLocations.length) {
            previousPlayerLocations[listPosition] = player.getBlockPos();
            listPosition++;
            return;
        }

        listPosition = 0;
        previousPlayerLocations[listPosition] = player.getBlockPos();
        listPosition++;
    }

    public static void resetPlayerLocationHistory() {
        Arrays.fill(previousPlayerLocations, null);
        listPosition = 0;
    }

    private static final class ActiveTunnelEvent {
        private final UUID playerUuid;
        private final ServerWorld world;
        private final BlockPos[] tunnelPath;

        private int nextIndex;
        private int mineTicksRemaining;
        private BlockPos currentMinePos;
        private int totalMineTicks;
        private int hitSoundCooldown;

        private ActiveTunnelEvent(UUID playerUuid, ServerWorld world, BlockPos[] tunnelPath) {
            this.playerUuid = playerUuid;
            this.world = world;
            this.tunnelPath = tunnelPath;
        }

        private ServerPlayerEntity getPlayer(MinecraftServer server) {
            return server.getPlayerManager().getPlayer(playerUuid);
        }

        private boolean playerBrokeNearTunnel(BlockPos brokenPos) {
            for (int i = 0; i < nextIndex; i++) {
                if (tunnelPath[i].getSquaredDistance(brokenPos) <= 4) {
                    return true;
                }
            }

            return false;
        }

        private boolean shouldMineInstantly(ServerPlayerEntity player, BlockPos pos) {
            return player.getBlockPos().getSquaredDistance(Vec3d.ofCenter(pos)) > HEARING_RADIUS_SQ;
        }

        private boolean tick(MinecraftServer server) {
            ServerPlayerEntity player = getPlayer(server);
            if (player == null || player.getWorld() != world) {
                return false;
            }

            while (nextIndex < tunnelPath.length) {
                BlockPos nextPos = tunnelPath[nextIndex];
                if (!shouldMineInstantly(player, nextPos)) {
                    break;
                }

                carveTunnelBlock(world, tunnelPath, nextPos, player, false, nextIndex);
                nextIndex++;
            }

            if (nextIndex >= tunnelPath.length) {
                return false;
            }

            if (mineTicksRemaining <= 0) {
                BlockPos nextPos = tunnelPath[nextIndex];
                BlockState state = world.getBlockState(nextPos);
                if (state.isAir() || state.isOf(Blocks.CAVE_AIR)) {
                    nextIndex++;
                    return true;
                }

                mineTicksRemaining = getMineTicks(world, nextPos, state);
                totalMineTicks = mineTicksRemaining;
                currentMinePos = nextPos;
                hitSoundCooldown = 0;
                // world.playSound(null, nextPos, state.getSoundGroup().getHitSound(),
                // SoundCategory.BLOCKS, 1.0f, 1.0f);
                return true;
            }

            mineTicksRemaining--;

            int progress = (int) ((1.0 - (double) mineTicksRemaining / totalMineTicks) * 10);
            progress = Math.max(0, Math.min(9, progress));

            world.setBlockBreakingInfo(
                    currentMinePos.hashCode(),
                    currentMinePos,
                    progress);

            if (hitSoundCooldown > 0) {
                hitSoundCooldown--;
            }

            if (hitSoundCooldown <= 0) {
                BlockState state = world.getBlockState(currentMinePos);

                world.playSound(
                        null,
                        currentMinePos,
                        state.getSoundGroup().getHitSound(),
                        SoundCategory.BLOCKS,
                        0.25F,
                        0.5F + (world.random.nextFloat() * 0.15F));

                hitSoundCooldown = 4;
            }

            if (mineTicksRemaining > 0) {
                return true;
            }

            BlockPos minedPos = currentMinePos;
            if (minedPos == null) {
                minedPos = tunnelPath[nextIndex];
            }

            BlockState minedState = world.getBlockState(minedPos);

            carveTunnelBlock(world, tunnelPath, minedPos, player, false, nextIndex);

            world.syncWorldEvent(
                    2001,
                    minedPos,
                    Block.getRawIdFromState(minedState));

            world.setBlockBreakingInfo(
                    minedPos.hashCode(),
                    minedPos,
                    -1);

            currentMinePos = null;
            mineTicksRemaining = 0;
            totalMineTicks = 0;
            hitSoundCooldown = 0;
            nextIndex++;
            return true;
        }
    }

    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (activeTunnelEvent == null) {
                return true;
            }

            if (!(world instanceof ServerWorld serverWorld)) {
                return true;
            }

            if (!player.getUuid().equals(activeTunnelEvent.playerUuid)) {
                return true;
            }

            if (serverWorld != activeTunnelEvent.world) {
                return true;
            }

            if (activeTunnelEvent.playerBrokeNearTunnel(pos)) {
                activeTunnelEvent = null;
            }

            return true;
        });
    }

    public static boolean shouldTriggerEvent(MinecraftServer server, ServerPlayerEntity player) {
        recordPlayerLocation(player);

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
                return false;
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
            return true;
        }
        return false;
    }

    public static boolean triggerEvent(MinecraftServer server, ServerPlayerEntity player) {
        ServerWorld world = (ServerWorld) player.getWorld();
        BlockPos playerPos = player.getBlockPos();

        if (activeTunnelEvent != null) {
            return false;
        }

        // Find tunnel start area
        BlockPos tunnelStart = findTunnelStart(world, playerPos);
        if (tunnelStart == null) {
            return false;
        }

        BlockPos caveWallPos = findCaveWall(world, tunnelStart, player);
        if (caveWallPos == null) {
            return false;
        }

        BlockPos[] tunnelPath = findTunnelPath(world, caveWallPos, playerPos);
        BlockPos[] cleanedTunnelPath = cleanTunnelPath(world, tunnelPath);

        if (cleanedTunnelPath.length == 0) {
            return false;
        }

        activeTunnelEvent = new ActiveTunnelEvent(player.getUuid(), world, cleanedTunnelPath);
        return true;
    }

    public static boolean carveTunnel(ServerWorld world, BlockPos[] tunnelPath, ServerPlayerEntity player) {
        for (int i = 0; i < tunnelPath.length; i++) {
            carveTunnelBlock(world, tunnelPath, tunnelPath[i], player, false, i);
        }
        return true;
    }

    private static void carveTunnelBlock(ServerWorld world, BlockPos[] tunnelPath, BlockPos pos,
            ServerPlayerEntity player, boolean playBreakSound, int index) {
        BlockPos belowPos = pos.down();
        if ((world.getBlockState(belowPos).isAir() || world.getBlockState(belowPos).isOf(Blocks.CAVE_AIR))
                && !isTunnelPathPos(tunnelPath, belowPos)) {
            world.setBlockState(belowPos, Blocks.COBBLESTONE.getDefaultState(), 3);
        }

        BlockState state = world.getBlockState(pos);
        if (!state.isAir() && !state.isOf(Blocks.CAVE_AIR)) {
            BlockModificationUtils.placeAirBlock(world, pos, Blocks.COBBLESTONE);
            if (playBreakSound) {
                world.syncWorldEvent(2001, pos, Block.getRawIdFromState(state));
            }
        }

        if (index % TORCH_INTERVAL == 0) {
            world.setBlockState(pos, Blocks.AIR.getDefaultState());
            TorchPlacer.placeTorch(world, pos, RANDOM, player);
        }
    }

    private static int getMineTicks(ServerWorld world, BlockPos pos, BlockState state) {
        float hardness = state.getHardness(world, pos);

        if (hardness < 0) {
            return 1;
        }

        return Math.max(1, Math.round(hardness * 30.0f / 8.0f));
    }

    private static boolean isTunnelPathPos(BlockPos[] tunnelPath, BlockPos pos) {
        for (BlockPos tunnelPos : tunnelPath) {
            if (tunnelPos.equals(pos)) {
                return true;
            }
        }
        return false;
    }

    public static BlockPos[] cleanTunnelPath(ServerWorld world, BlockPos[] tunnelPath) {
        // remove the end of the tunnel path until the final few blocks do not have any
        // air conected on any side
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
        if (activeTunnelEvent == null) {
            return;
        }

        if (!activeTunnelEvent.tick(server)) {
            activeTunnelEvent = null;
        }
    }
}