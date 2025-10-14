package horror.blueice129.feature;

import horror.blueice129.utils.PlayerUtils;
import horror.blueice129.HorrorMod129;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;

public class LedgePusher {
    private final int minLedgeHeight;
    private PlayerEntity player;
    private World world;
    private BlockPos lastPlayerPos = null;

    public LedgePusher(PlayerEntity player, int minLedgeHeight) {
        this.player = player;
        this.minLedgeHeight = minLedgeHeight;
        this.world = player.getWorld();
    }

    public boolean isPlayerOnLedge() {
        if (!PlayerUtils.isPlayerOnGround(player)) {
            return false; // Player is not on the ground
        }
        if (!PlayerUtils.isPlayerCrouching(player)) {
            return false; // Player is not crouching
        }

        BlockPos playerPos = this.player.getBlockPos();
        // check if the block below the player is air, indicating they are crouching at the very edge of the block
        BlockPos belowPlayerPos = playerPos.down();
        if (!world.isAir(belowPlayerPos)) {
            return false; // Block below player is not air
        }

        // check blocks in front of the player and see if they are on a ledge
        String direction = PlayerUtils.getPlayerCompassDirection(player);
        BlockPos frontPos = PlayerUtils.getRelativeBlockPos(playerPos, direction);
        BlockPos leftBlockPos = PlayerUtils.getRelativeBlockPos(playerPos, PlayerUtils.getLeftRightDirection(direction, true));
        BlockPos rightBlockPos = PlayerUtils.getRelativeBlockPos(playerPos, PlayerUtils.getLeftRightDirection(direction, false));

        // log the direction and positions being checked
        HorrorMod129.LOGGER.info("Checking ledge at direction: " + direction);
        HorrorMod129.LOGGER.info("Front Pos: " + frontPos);
        HorrorMod129.LOGGER.info("Left Pos: " + leftBlockPos);
        HorrorMod129.LOGGER.info("Right Pos: " + rightBlockPos);

        BlockPos[] checkPositions = { frontPos, leftBlockPos, rightBlockPos };
        for (BlockPos pos : checkPositions) {
            for (int y = 2; y >= -minLedgeHeight; y--) {
                BlockPos checkPos = pos.add(0, y, 0);

                if (!world.isAir(checkPos)) {
                    return false; // Found a solid block within the ledge height range
                }
                // set block to diamond block for testing
                // world.setBlockState(checkPos, net.minecraft.block.Blocks.DIAMOND_BLOCK.getDefaultState());
            }
        }
        lastPlayerPos = playerPos;
        return true; // No solid blocks found, player is on a ledge
    }

    public boolean didPlayerFall() {
        if (lastPlayerPos == null) {
            return false; // No previous position to compare
        }
        BlockPos currentPos = player.getBlockPos().down(5);
        return currentPos.getY() < lastPlayerPos.getY();
    }

    public void pushPlayer() {
        // punch player in the direction they are facing
        // deal the damage too
        // attribute damage source to "BlueIce129", so if they die from falling, it shows "____ was doomed to fall by BlueIce129"
        String direction = PlayerUtils.getPlayerCompassDirection(player);
        double pushStrength = 1.0;
        






}
