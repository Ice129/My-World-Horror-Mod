package horror.blueice129.feature;

import horror.blueice129.utils.PlayerUtils;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.entity.player.PlayerEntity;

public class LedgePusher {
    private final int minLedgeHeight;
    private PlayerEntity player;
    private World world;

    public LedgePusher(PlayerEntity player, int minLedgeHeight) {
        this.player = player;
        this.minLedgeHeight = minLedgeHeight;
        this.world = player.getWorld();
    }

    public boolean isPlayerOnLedge() {
        BlockPos playerPos = this.player.getBlockPos();
        // check blocks in front of the player and see if they are on a ledge
        String direction = PlayerUtils.getPlayerCompassDirection(player);
        BlockPos frontPos = PlayerUtils.getRelativeBlockPos(playerPos, direction);
        BlockPos leftBlockPos = PlayerUtils.getRelativeBlockPos(frontPos, PlayerUtils.getLeftRightDirection(direction, true));
        BlockPos rightBlockPos = PlayerUtils.getRelativeBlockPos(frontPos, PlayerUtils.getLeftRightDirection(direction, false));

        BlockPos[] checkPositions = { frontPos, leftBlockPos, rightBlockPos };
        for (BlockPos pos : checkPositions) {
            for (int y = -2; y <= minLedgeHeight; y++) {
                BlockPos checkPos = pos.add(0, y, 0);
                if (!world.isAir(checkPos)) {
                    return false; // Found a solid block within the ledge height range
                }
            }
        }
        return true; // No solid blocks found, player is on a ledge
    }



}
