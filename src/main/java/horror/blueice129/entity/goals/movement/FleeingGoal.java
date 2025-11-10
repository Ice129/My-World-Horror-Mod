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
        // Refresh the closest player only every 20 ticks (once per second)
        if (playerRefreshCooldown <= 0) {
            targetPlayer = entity.getWorld().getClosestPlayer(entity, 64.0);
            playerRefreshCooldown = 20;
        } else {
            playerRefreshCooldown--;
        }
        return fleeTime > 0 && isInState(Blueice129Entity.EntityState.FLEEING) && targetPlayer != null;
    }
    
    @Override
    public void tick() {
        fleeTime--;
        
        // Flee from player if nearby
        if (targetPlayer != null && entity.squaredDistanceTo(targetPlayer) < 256.0) {
            // Calculate flee direction (away from player)
            double dx = entity.getX() - targetPlayer.getX();
            double dz = entity.getZ() - targetPlayer.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            
            if (distance > 0) {
                double fleeX = entity.getX() + (dx / distance) * 8.0;
                double fleeZ = entity.getZ() + (dz / distance) * 8.0;
                
                // Use speed multiplier of 1.0 since SpeedBoostGoal handles the speed increase
                entity.getNavigation().startMovingTo(fleeX, entity.getY(), fleeZ, 1.0);
            }
        }
        
        // Occasionally look back at player
        if (entity.getRandom().nextFloat() < 0.05F && targetPlayer != null) {
            entity.getLookControl().lookAt(targetPlayer, 30.0F, 30.0F);
        }
    }

    @Override
    protected void onStart() {
        fleeTime = 200 + entity.getRandom().nextInt(200);
        targetPlayer = entity.getWorld().getClosestPlayer(entity, 64.0);
        playerRefreshCooldown = 0;
    }
}