package horror.blueice129.utils;

import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class FootstepPathUtils {

    private FootstepPathUtils() {
    }

    public static boolean isNearAnyPlayer(MinecraftServer server, BlockPos pos, double maxDistance) {
        double maxSq = maxDistance * maxDistance;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (player.getBlockPos().getSquaredDistance(pos) <= maxSq) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasOverheadCover(ServerWorld world, BlockPos pos, int minSolidOverhead) {
        int surfaceY = SurfaceFinder.findPointSurfaceY(world, pos.getX(), pos.getZ(), true, true, true);

        if (pos.getY() >= surfaceY) {
            return false;
        }

        int solidCount = 0;
        for (int checkY = pos.getY() + 1; checkY <= surfaceY; checkY++) {
            BlockPos checkPos = new BlockPos(pos.getX(), checkY, pos.getZ());
            BlockState state = world.getBlockState(checkPos);

            if (state.isSolidBlock(world, checkPos)) {
                solidCount++;
                if (solidCount >= minSolidOverhead) {
                    return true;
                }
            }
        }

        return false;
    }

    public static List<BlockPos> floodFillPath(
            ServerWorld world,
            BlockPos start,
            Direction behind,
            int minTargetDistance,
            int maxTargetDistance,
            int maxRadius) {

        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Map<BlockPos, BlockPos> parent = new HashMap<>();
        queue.add(start);
        parent.put(start, null);

        BlockPos best = null;
        double bestScore = -Double.MAX_VALUE;

        int bx = behind.getOffsetX();
        int bz = behind.getOffsetZ();

        int[][] neighbors = {
                {1, 0}, {-1, 0}, {0, 1}, {0, -1},
                {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
        };

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();

            int dx = current.getX() - start.getX();
            int dz = current.getZ() - start.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            double dotBehind = dx * bx + dz * bz;

            if (dist >= minTargetDistance && dist <= maxTargetDistance && dotBehind > 0) {
                double score = dotBehind;
                if (score > bestScore) {
                    bestScore = score;
                    best = current;
                }
            }

            if (dist > maxRadius) {
                continue;
            }

            for (int[] n : neighbors) {
                BlockPos candidateBase = current.add(n[0], 0, n[1]);
                BlockPos candidate = findWalkableNearby(world, candidateBase);
                if (candidate == null) {
                    continue;
                }

                if (!parent.containsKey(candidate)) {
                    parent.put(candidate, current);
                    queue.add(candidate);
                }
            }
        }

        if (best == null) {
            return null;
        }

        List<BlockPos> path = new ArrayList<>();
        BlockPos cursor = best;
        while (cursor != null) {
            path.add(cursor);
            cursor = parent.get(cursor);
        }

        return path;
    }

    private static BlockPos findWalkableNearby(ServerWorld world, BlockPos base) {
        BlockPos[] candidates = new BlockPos[] {
                base,
                base.down(1),
                base.up(1)
        };
        for (BlockPos pos : candidates) {
            if (isWalkable(world, pos)) {
                return pos;
            }
        }
        return null;
    }

    private static boolean isWalkable(ServerWorld world, BlockPos pos) {
        BlockState ground = world.getBlockState(pos);
        BlockState air = world.getBlockState(pos.up());
        BlockState head = world.getBlockState(pos.up(2));
        return !ground.isAir() && ground.isSolidBlock(world, pos) && air.isAir() && head.isAir();
    }

    /**
     * Greedily walks from {@code from} toward {@code to}, choosing the walkable
     * neighbour that minimises distance to {@code to} at each step.
     *
     * Stops when within {@code stopDistance} blocks of {@code to} or after
     * {@code maxSteps} steps. Visited positions are tracked to prevent loops.
     *
     * @return ordered list of positions walked (not including {@code from}),
     *         or {@code null} if no progress could be made at all.
     */
    public static List<BlockPos> walkToward(
            ServerWorld world,
            BlockPos from,
            BlockPos to,
            int stopDistance,
            int maxSteps) {

        List<BlockPos> path = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        BlockPos current = from;
        visited.add(current);

        int stopDistSq = stopDistance * stopDistance;
        int[][] offsets = {
                {1, 0}, {-1, 0}, {0, 1}, {0, -1},
                {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
        };

        for (int step = 0; step < maxSteps; step++) {
            if (current.getSquaredDistance(to) <= stopDistSq) {
                break;
            }

            BlockPos best = null;
            double bestDistSq = Double.MAX_VALUE;

            for (int[] offset : offsets) {
                BlockPos candidate = findWalkableNearby(world, current.add(offset[0], 0, offset[1]));
                if (candidate == null || visited.contains(candidate)) continue;

                double distSq = candidate.getSquaredDistance(to);
                if (distSq < bestDistSq) {
                    bestDistSq = distSq;
                    best = candidate;
                }
            }

            if (best == null) break; // Stuck — no unvisited walkable neighbour

            visited.add(best);
            current = best;
            path.add(current);
        }

        return path.isEmpty() ? null : path;
    }
}
