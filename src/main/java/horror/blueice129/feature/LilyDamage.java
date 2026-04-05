package horror.blueice129.feature;

import horror.blueice129.network.ModNetworking;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class LilyDamage {

    private static final int H_RADIUS = 3;
    private static final int V_RADIUS = 1;
    private static final int DURABILITY_DRAIN_PER_FLOWER = 2;

    public static void applyToPlayer(ServerWorld world, ServerPlayerEntity player) {
        List<BlockPos> flowers = findNearbyFlowers(world, player.getBlockPos());
        if (flowers.isEmpty()) return;

        for (BlockPos flowerPos : flowers) {
            ModNetworking.sendLilyRainStart(player, flowerPos);
        }

        int drain = flowers.size() * DURABILITY_DRAIN_PER_FLOWER;
        PlayerInventory inventory = player.getInventory();
        ItemStack[] candidates = new ItemStack[inventory.size()];
        int candidateCount = 0;

        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty() && stack.isDamageable()) {
                candidates[candidateCount++] = stack;
            }
        }

        if (candidateCount == 0) return;

        ItemStack chosenStack = candidates[world.getRandom().nextInt(candidateCount)];
        chosenStack.damage(drain, world.getRandom(), player);
    }

    private static List<BlockPos> findNearbyFlowers(ServerWorld world, BlockPos center) {
        List<BlockPos> flowers = new ArrayList<>();
        for (int x = -H_RADIUS; x <= H_RADIUS; x++) {
            for (int z = -H_RADIUS; z <= H_RADIUS; z++) {
                for (int y = -V_RADIUS; y <= V_RADIUS; y++) {
                    BlockPos flowerPos = center.add(x, y, z);
                    if (world.getBlockState(flowerPos).isOf(Blocks.LILY_OF_THE_VALLEY)) {
                        flowers.add(flowerPos.toImmutable());
                    }
                }
            }
        }
        return flowers;
    }
}
