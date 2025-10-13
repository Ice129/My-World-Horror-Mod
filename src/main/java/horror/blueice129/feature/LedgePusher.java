package horror.blueice129.feature;

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
        // get the block infront of the player, and the 2 diagonal forwrd blocks
        int directionX = (int) Math.round(-Math.sin(Math.toRadians(player.getYaw())));
        int directionZ = (int) Math.round(Math.cos(Math.toRadians(player.getYaw())));
        BlockPos frontPos = playerPos.add(directionX, 0, directionZ);
        


        for (BlockPos pos : BlockPos.iterate(playerPos.add(-1, 0, -1), playerPos.add(1, 0, 1))) {
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
