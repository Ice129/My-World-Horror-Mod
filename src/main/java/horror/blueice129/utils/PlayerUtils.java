package horror.blueice129.utils;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

public class PlayerUtils {
    public static boolean isPlayerCrouching(PlayerEntity player) {
        return player.isSneaking();
    }

    public static float getPlayerLookDirection(PlayerEntity player) {
        return player.getYaw();
    }

    /**
     * Get the compass direction (cardinal and intercardinal) the player is facing.
     * 
     * @return String 'N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW'
     */
    public static String getPlayerCompassDirection(PlayerEntity player) {
        float yaw = getPlayerLookDirection(player);
        if (yaw < 0) {
            yaw += 360;
        }
        if (yaw >= 337.5 || yaw < 22.5) {
            return "N";
        } else if (yaw >= 22.5 && yaw < 67.5) {
            return "NE";
        } else if (yaw >= 67.5 && yaw < 112.5) {
            return "E";
        } else if (yaw >= 112.5 && yaw < 157.5) {
            return "SE";
        } else if (yaw >= 157.5 && yaw < 202.5) {
            return "S";
        } else if (yaw >= 202.5 && yaw < 247.5) {
            return "SW";
        } else if (yaw >= 247.5 && yaw < 292.5) {
            return "W";
        } else if (yaw >= 292.5 && yaw < 337.5) {
            return "NW";
        }
        // Fallback: ensure the method always returns a String
        return "N";
    }

    /**
     * Returns the compass direction 45 degrees to the left or right of the given
     * direction.
     * "Left" means a counterclockwise rotation on the compass (e.g., from 'N' to
     * 'NW'),
     * while "right" means a clockwise rotation (e.g., from 'N' to 'NE').
     * If {@code isLeft} is true, returns the previous direction in the array order
     * (counter-clockwise).
     * If {@code isLeft} is false, returns the next direction in the array order
     * (clockwise).
     * If the input direction is invalid, returns the original direction.
     * 
     * @param direction The current compass direction ('N', 'NE', 'E', 'SE', 'S',
     *                  'SW', 'W', 'NW')
     * @param isLeft    Whether to get the direction to the left (counterclockwise,
     *                  true) or right (clockwise, false)
     * @return The new compass direction, or the original if input is invalid
     */
    public static String getLeftRightDirection(String compassDirection, boolean isLeft) {
        String[] directions = { "N", "NE", "E", "SE", "S", "SW", "W", "NW" };
        int index = -1;
        for (int i = 0; i < directions.length; i++) {
            if (directions[i].equals(compassDirection)) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            return compassDirection; // Invalid direction input
        }
        if (isLeft) {
            index = (index - 1 + directions.length) % directions.length;
        } else {
            index = (index + 1) % directions.length;
        }
        return directions[index];
    }

    /**
     * get block relative to initial block, with compass direction 
     * @param initialBlockPos The starting block position
     * @param compassDirection The compass direction ('N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW')
     * @return The new BlockPos
     */
    public static BlockPos getRelativeBlockPos(BlockPos initialBlockPos, String compassDirection) {
        switch (compassDirection) {
            case "N":
                return initialBlockPos.north();
            case "NE":
                return initialBlockPos.north().east();
            case "E":
                return initialBlockPos.east();
            case "SE":
                return initialBlockPos.south().east();
            case "S":
                return initialBlockPos.south();
            case "SW":
                return initialBlockPos.south().west();
            case "W":
                return initialBlockPos.west();
            case "NW":
                return initialBlockPos.north().west();
            default:
                return initialBlockPos; // Invalid direction input
        }
    }
}
