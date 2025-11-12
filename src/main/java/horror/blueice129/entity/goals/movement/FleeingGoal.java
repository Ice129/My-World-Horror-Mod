package horror.blueice129.entity.goals.movement;
import horror.blueice129.entity.Blueice129Entity;
import horror.blueice129.entity.goals.BaseBlueice129Goal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import java.util.EnumSet;


/**
 * Goal that makes the entity flee from the player, occasionally looking back
 */
public class FleeingGoal extends BaseBlueice129Goal {
    
    private static final double PLAYER_DETECTION_RANGE = 64.0;
    private static final double FLEE_DISTANCE_SQUARED = 256.0; // 16 blocks squared
    private static final double FLEE_TARGET_DISTANCE = 8.0;
    private static final float LOOK_BACK_CHANCE = 0.05F;
    private static final int PLAYER_REFRESH_INTERVAL = 20; // Once per second
    private static final int BASE_FLEE_TIME = 200; // 10 seconds
    private static final int RANDOM_FLEE_TIME = 200; // 0-10 seconds random
    
    private PlayerEntity targetPlayer;
    private int fleeTime;
    private int playerRefreshCooldown = 0;
    
    public FleeingGoal(Blueice129Entity entity) {
        super(entity, 5);
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK, Goal.Control.JUMP));
    }
    
    @Override
    protected boolean shouldStart() {
        return isInState(Blueice129Entity.EntityState.FLEEING);
    }

    @Override
    protected boolean shouldKeepRunning() {
        // Refresh the closest player only at intervals to reduce overhead
        if (playerRefreshCooldown <= 0) {
            targetPlayer = entity.getWorld().getClosestPlayer(entity, PLAYER_DETECTION_RANGE);
            playerRefreshCooldown = PLAYER_REFRESH_INTERVAL;
        } else {
            playerRefreshCooldown--;
        }
        return fleeTime > 0 && isInState(Blueice129Entity.EntityState.FLEEING) && targetPlayer != null;
    }
    
    @Override
    public void tick() {
        fleeTime--;
        
        // Flee from player if nearby
        if (targetPlayer != null && entity.squaredDistanceTo(targetPlayer) < FLEE_DISTANCE_SQUARED) {
            // Calculate flee direction (away from player)
            double dx = entity.getX() - targetPlayer.getX();
            double dz = entity.getZ() - targetPlayer.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            
            if (distance > 0) {
                double fleeX = entity.getX() + (dx / distance) * FLEE_TARGET_DISTANCE;
                double fleeZ = entity.getZ() + (dz / distance) * FLEE_TARGET_DISTANCE;
                
                // Use speed multiplier of 1.0 since SpeedBoostGoal handles the speed increase
                entity.getNavigation().startMovingTo(fleeX, entity.getY(), fleeZ, 1.0);
            }
        }
        
        // Occasionally look back at player
        if (entity.getRandom().nextFloat() < LOOK_BACK_CHANCE && targetPlayer != null) {
            entity.getLookControl().lookAt(targetPlayer, 30.0F, 30.0F);
        }
    }

    @Override
    protected void onStart() {
        fleeTime = BASE_FLEE_TIME + entity.getRandom().nextInt(RANDOM_FLEE_TIME);
        targetPlayer = entity.getWorld().getClosestPlayer(entity, PLAYER_DETECTION_RANGE);
        playerRefreshCooldown = 0;
    }
}