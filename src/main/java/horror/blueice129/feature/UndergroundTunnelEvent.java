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
    private static final Map<UUID, WatchState> WATCH_STATES = new HashMap<>();
    private static final Map<UUID, TunnelRun> ACTIVE_RUNS = new HashMap<>();

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

    private UndergroundTunnelEvent() {
    }

    public static void tick(MinecraftServer server) {
        if (server.getPlayerManager().getPlayerList().isEmpty()) {
            WATCH_STATES.clear();
            ACTIVE_RUNS.clear();
            return;
        }

        List<ServerPlayerEntity> players = new ArrayList<>(server.getPlayerManager().getPlayerList());
        Set<UUID> seen = new HashSet<>();

        for (ServerPlayerEntity player : players) {
            seen.add(player.getUuid());

            TunnelRun run = ACTIVE_RUNS.get(player.getUuid());
            if (run != null) {
                if (!run.tick(player)) {
                    ACTIVE_RUNS.remove(player.getUuid());
                }
                continue;
            }

            watchPlayer(player);
        }

        WATCH_STATES.keySet().removeIf(uuid -> !seen.contains(uuid));
        ACTIVE_RUNS.keySet().removeIf(uuid -> !seen.contains(uuid));
    }

    public static boolean triggerDebug(ServerPlayerEntity player) {
        return startRun(player.getServerWorld(), player, true);
    }

    private static void watchPlayer(ServerPlayerEntity player) {
        if (!isUnderground(player.getServerWorld(), player.getBlockPos())) {
            WATCH_STATES.remove(player.getUuid());
            return;
        }

        BlockPos pos = player.getBlockPos();
        int cellX = Math.floorDiv(pos.getX(), 3);
        int cellZ = Math.floorDiv(pos.getZ(), 3);

        WatchState state = WATCH_STATES.get(player.getUuid());
        if (state == null || state.cellX != cellX || state.cellZ != cellZ) {
            state = new WatchState(cellX, cellZ);
            WATCH_STATES.put(player.getUuid(), state);
        }

        state.ticksInCell++;
        if (state.ticksInCell < AREA_STABLE_TICKS) {
            return;
        }

        if (startRun(player.getServerWorld(), player, false)) {
            WATCH_STATES.remove(player.getUuid());
            HorrorMod129.LOGGER.info("Underground tunnel event started for {}", player.getName().getString());
        }
    }

    private static boolean startRun(ServerWorld world, ServerPlayerEntity player, boolean debug) {
        UUID uuid = player.getUuid();
        if (ACTIVE_RUNS.containsKey(uuid)) {
            return false;
        }

        CaveCandidate cave = findNearbyCave(world, player);
        if (cave == null) {
            if (debug) {
                HorrorMod129.LOGGER.info("Underground tunnel debug start failed for {}: no cave found", player.getName().getString());
            }
            return false;
        }

        BlockPos target = findTargetNearPlayer(player, cave.anchor);
        List<BlockPos> path = buildTunnelPath(world, player, cave.anchor, target);
        if (path.size() < 8) {
            if (debug) {
                HorrorMod129.LOGGER.info("Underground tunnel debug start failed for {}: path too short", player.getName().getString());
            }
            return false;
        }

        ACTIVE_RUNS.put(uuid, new TunnelRun(uuid, path));
        return true;
    }

    private static boolean isUnderground(ServerWorld world, BlockPos pos) {
        return pos.getY() < 60 && !world.isSkyVisible(pos);
    }

    private static CaveCandidate findNearbyCave(ServerWorld world, ServerPlayerEntity player) {
        BlockPos playerPos = player.getBlockPos();
        Set<BlockPos> tested = new HashSet<>();

        for (int radius = 20; radius <= SEARCH_RADIUS; radius += 4) {
            int steps = Math.max(16, radius * 3);
            for (int step = 0; step < steps; step++) {
                double angle = (Math.PI * 2.0 * step) / steps + RANDOM.nextDouble() * 0.05;
                int x = playerPos.getX() + (int) Math.round(Math.cos(angle) * radius);
                int z = playerPos.getZ() + (int) Math.round(Math.sin(angle) * radius);

                for (int yOffset = -10; yOffset <= 10; yOffset += 2) {
                    BlockPos candidate = new BlockPos(x, playerPos.getY() + yOffset, z);
                    if (!tested.add(candidate)) {
                        continue;
                    }

                    if (candidate.getY() <= world.getBottomY() + 1 || candidate.getY() >= world.getTopY() - 2) {
                        continue;
                    }

                    if (!ChunkLoader.loadChunksInRadius(world, candidate, 1)) {
                        continue;
                    }

                    BlockState state = world.getBlockState(candidate);
                    if (!isCaveAir(state)) {
                        continue;
                    }

                    CaveCandidate cave = floodFillCave(world, candidate, playerPos);
                    if (cave != null && cave.volume >= MIN_CAVE_VOLUME && cave.anchor.getSquaredDistance(playerPos) >= HEARING_RADIUS_SQ) {
                        return cave;
                    }
                }
            }
        }

        return null;
    }

    private static CaveCandidate floodFillCave(ServerWorld world, BlockPos start, BlockPos playerPos) {
        Set<BlockPos> visited = new HashSet<>();
        ArrayList<BlockPos> queue = new ArrayList<>();
        queue.add(start);
        visited.add(start);

        int index = 0;
        int volume = 0;
        long sumX = 0;
        long sumY = 0;
        long sumZ = 0;
        BlockPos closest = start;
        double closestDistance = start.getSquaredDistance(playerPos);

        while (index < queue.size() && volume < MAX_CAVE_FILL) {
            BlockPos current = queue.get(index++);
            BlockState state = world.getBlockState(current);
            if (!isCaveAir(state)) {
                continue;
            }

            volume++;
            sumX += current.getX();
            sumY += current.getY();
            sumZ += current.getZ();

            double distance = current.getSquaredDistance(playerPos);
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = current;
            }

            for (Direction direction : Direction.values()) {
                BlockPos next = current.offset(direction);
                if (visited.contains(next)) {
                    continue;
                }

                if (next.getY() <= world.getBottomY() + 1 || next.getY() >= world.getTopY() - 2) {
                    continue;
                }

                if (!ChunkLoader.loadChunksInRadius(world, next, 1)) {
                    continue;
                }

                if (isCaveAir(world.getBlockState(next))) {
                    visited.add(next);
                    queue.add(next);
                }
            }
        }

        if (volume < MIN_CAVE_VOLUME) {
            return null;
        }

        BlockPos center = new BlockPos((int) (sumX / volume), (int) (sumY / volume), (int) (sumZ / volume));
        return new CaveCandidate(closest, center, volume);
    }

    private static BlockPos findTargetNearPlayer(ServerPlayerEntity player, BlockPos caveAnchor) {
        BlockPos playerPos = player.getBlockPos();
        Vec3d direction = new Vec3d(
                caveAnchor.getX() - playerPos.getX(),
                0.0,
                caveAnchor.getZ() - playerPos.getZ()).normalize();

        if (Double.isNaN(direction.x) || Double.isNaN(direction.z) || direction.lengthSquared() == 0.0) {
            Vec3d look = player.getRotationVector();
            direction = new Vec3d(-look.x, 0.0, -look.z).normalize();
        }

        int x = playerPos.getX() + (int) Math.round(direction.x * HEARING_RADIUS);
        int z = playerPos.getZ() + (int) Math.round(direction.z * HEARING_RADIUS);
        return new BlockPos(x, caveAnchor.getY(), z);
    }

    private static List<BlockPos> buildTunnelPath(ServerWorld world, ServerPlayerEntity player, BlockPos start, BlockPos end) {
        int minX = Math.min(start.getX(), end.getX()) - 14;
        int maxX = Math.max(start.getX(), end.getX()) + 14;
        int minY = Math.max(world.getBottomY() + 2, Math.min(start.getY(), end.getY()) - 10);
        int maxY = Math.min(world.getTopY() - 3, Math.max(start.getY(), end.getY()) + 10);
        int minZ = Math.min(start.getZ(), end.getZ()) - 14;
        int maxZ = Math.max(start.getZ(), end.getZ()) + 14;

        PriorityQueue<PathNode> open = new PriorityQueue<>(Comparator.comparingDouble(node -> node.fScore));
        Map<BlockPos, Double> bestCost = new HashMap<>();
        Set<BlockPos> closed = new HashSet<>();

        PathNode startNode = new PathNode(start, null, null, 0.0, heuristic(start, end));
        open.add(startNode);
        bestCost.put(start, 0.0);

        while (!open.isEmpty()) {
            PathNode current = open.poll();
            if (!closed.add(current.pos)) {
                continue;
            }

            if (current.pos.equals(end)) {
                return reconstructPath(current);
            }

            if (closed.size() > 4096) {
                break;
            }

            for (Direction direction : Direction.values()) {
                BlockPos next = current.pos.offset(direction);

                if (next.getX() < minX || next.getX() > maxX || next.getY() < minY || next.getY() > maxY || next.getZ() < minZ || next.getZ() > maxZ) {
                    continue;
                }

                if (next.getSquaredDistance(player.getBlockPos()) < HEARING_RADIUS_SQ && !next.equals(end)) {
                    continue;
                }

                if (!ChunkLoader.loadChunksInRadius(world, next, 1)) {
                    continue;
                }

                BlockState state = world.getBlockState(next);
                if (state.getHardness(world, next) < 0.0f) {
                    continue;
                }

                boolean passable = state.getCollisionShape(world, next).isEmpty();
                double stepCost = 1.0;
                stepCost += passable ? 0.1 : 1.5 + Math.max(0.0, state.getHardness(world, next));

                if (current.direction == direction) {
                    stepCost += 0.2;
                }

                if (direction == Direction.UP || direction == Direction.DOWN) {
                    stepCost += 0.35;
                }

                stepCost += RANDOM.nextDouble() * 0.05;

                double newCost = current.costSoFar + stepCost;
                Double known = bestCost.get(next);
                if (known != null && known <= newCost) {
                    continue;
                }

                bestCost.put(next, newCost);
                open.add(new PathNode(next, current, direction, newCost, newCost + heuristic(next, end)));
            }
        }

        return List.of();
    }

    private static List<BlockPos> reconstructPath(PathNode node) {
        ArrayList<BlockPos> path = new ArrayList<>();
        PathNode current = node;
        while (current != null) {
            path.add(0, current.pos);
            current = current.previous;
        }

        if (path.size() > MAX_PATH_NODES) {
            return new ArrayList<>(path.subList(0, MAX_PATH_NODES));
        }

        return path;
    }

    private static double heuristic(BlockPos a, BlockPos b) {
        return Math.sqrt(a.getSquaredDistance(b));
    }

    private static boolean isCaveAir(BlockState state) {
        return state.isAir() || state.isOf(Blocks.CAVE_AIR);
    }

    private static boolean isLookingAt(ServerPlayerEntity player, BlockPos target) {
        Vec3d eye = player.getEyePos();
        Vec3d toTarget = new Vec3d(
                target.getX() + 0.5 - eye.x,
                target.getY() + 0.5 - eye.y,
                target.getZ() + 0.5 - eye.z).normalize();

        if (Double.isNaN(toTarget.x) || Double.isNaN(toTarget.y) || Double.isNaN(toTarget.z)) {
            return false;
        }

        return player.getRotationVector().dotProduct(toTarget) > 0.82;
    }

    private static boolean carveTunnelBlock(ServerWorld world, ServerPlayerEntity player, BlockPos pos) {
        if (pos.getY() <= world.getBottomY() + 1 || pos.getY() >= world.getTopY() - 2) {
            return false;
        }

        if (!ChunkLoader.loadChunksInRadius(world, pos, 1)) {
            return false;
        }

        boolean changed = false;
        for (BlockPos target : List.of(pos, pos.up())) {
            BlockState state = world.getBlockState(target);
            if (!state.getCollisionShape(world, target).isEmpty()) {
                if (state.getHardness(world, target) >= 0.0f) {
                    world.breakBlock(target, false, player);
                    changed = true;
                }
            }
        }

        return changed;
    }

    private static int getMiningDelay(ServerWorld world, BlockPos pos) {
        int delay = 2;
        for (BlockPos target : List.of(pos, pos.up())) {
            BlockState state = world.getBlockState(target);
            if (state.getCollisionShape(world, target).isEmpty()) {
                continue;
            }

            float hardness = state.getHardness(world, target);
            if (hardness < 0.0f) {
                continue;
            }

            int blockDelay = (int) Math.ceil(hardness * 30.0f / 8.0f);
            delay = Math.max(delay, blockDelay);
        }

        return Math.min(delay, 12);
    }

    private static void placeTorchIfNeeded(ServerWorld world, ServerPlayerEntity player, BlockPos pos, int pathIndex) {
        if (pathIndex <= 0 || pathIndex % TORCH_INTERVAL != 0) {
            return;
        }

        if (pos.getY() <= world.getBottomY() + 1 || pos.getY() >= world.getTopY() - 2) {
            return;
        }

        if (!world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()) {
            return;
        }

        TorchPlacer.placeTorch(world, pos, RANDOM, player);
    }

    private record CaveCandidate(BlockPos anchor, BlockPos center, int volume) {
    }

    private static final class WatchState {
        private final int cellX;
        private final int cellZ;
        private int ticksInCell;

        private WatchState(int cellX, int cellZ) {
            this.cellX = cellX;
            this.cellZ = cellZ;
        }
    }

    private static final class PathNode {
        private final BlockPos pos;
        private final PathNode previous;
        private final Direction direction;
        private final double costSoFar;
        private final double fScore;

        private PathNode(BlockPos pos, PathNode previous, Direction direction, double costSoFar, double fScore) {
            this.pos = pos;
            this.previous = previous;
            this.direction = direction;
            this.costSoFar = costSoFar;
            this.fScore = fScore;
        }
    }

    private static final class TunnelRun {
        private final UUID playerId;
        private final List<BlockPos> path;
        private final int halfPoint;
        private int pathIndex;
        private int cooldown;
        private int elapsedTicks;
        private int lookTicks;

        private TunnelRun(UUID playerId, List<BlockPos> path) {
            this.playerId = playerId;
            this.path = path;
            this.halfPoint = Math.max(1, path.size() / 2);
        }

        private boolean tick(ServerPlayerEntity player) {
            if (player == null || !player.isAlive() || !player.getUuid().equals(playerId)) {
                return false;
            }

            elapsedTicks++;
            if (elapsedTicks >= EVENT_TICKS) {
                return false;
            }

            BlockPos playerPos = player.getBlockPos();
            BlockPos currentTarget = path.get(Math.min(pathIndex, path.size() - 1));

            if (playerPos.getSquaredDistance(currentTarget) <= CLOSE_CANCEL_RADIUS_SQ) {
                return false;
            }

            if (isLookingAt(player, currentTarget)) {
                lookTicks++;
                if (lookTicks >= LOOK_CANCEL_TICKS) {
                    return false;
                }
            } else if (lookTicks > 0) {
                lookTicks--;
            }

            if (cooldown > 0) {
                cooldown--;
                return true;
            }

            if (pathIndex >= path.size()) {
                return false;
            }

            BlockPos node = path.get(pathIndex);
            if (node.getSquaredDistance(playerPos) < HEARING_RADIUS_SQ) {
                return false;
            }

            if (carveTunnelBlock(player.getServerWorld(), player, node)) {
                placeTorchIfNeeded(player.getServerWorld(), player, node, pathIndex);
            }

            pathIndex++;
            cooldown = pathIndex < halfPoint ? 1 : getMiningDelay(player.getServerWorld(), node);
            return true;
        }
    }
}