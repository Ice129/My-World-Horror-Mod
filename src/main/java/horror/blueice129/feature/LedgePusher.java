package horror.blueice129.feature;

import horror.blueice129.utils.PlayerUtils;

import com.mojang.authlib.GameProfile;

import horror.blueice129.HorrorMod129;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;



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
    String direction = PlayerUtils.getPlayerCompassDirection(player);
    double pushStrength = 1.0;
    double[] directionVector = PlayerUtils.getDirectionVector(direction);
    double dx = directionVector[0] * pushStrength;
    double dz = directionVector[1] * pushStrength;
    player.addVelocity(dx, 0.5, dz);
    
    // Create a fake player with blueice129 name to damage the player
    if (world instanceof ServerWorld) {
        ServerWorld serverWorld = (ServerWorld) world;
        GameProfile profile = new GameProfile(java.util.UUID.nameUUIDFromBytes("blueice129".getBytes()), "blueice129");
        FakePlayer fakePlayer = FakePlayer.get(serverWorld, profile);
        // Position the fake player behind the real player (opposite of push direction)
        fakePlayer.setPosition(
            player.getX() - directionVector[0] * 2,
            player.getY(),
            player.getZ() - directionVector[1] * 2
        );
        // Damage the player as if attacked by the fake player
        player.damage(player.getDamageSources().playerAttack(fakePlayer), 2.0f);
    } else {
        // Fallback for client-side or if we can't create a fake player
        player.damage(player.getDamageSources().generic(), 2.0f);
    }
}







}
