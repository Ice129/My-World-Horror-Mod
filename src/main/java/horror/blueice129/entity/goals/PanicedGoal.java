package horror.blueice129.entity.goals;

import horror.blueice129.entity.Blueice129Entity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import java.util.EnumSet;

/**
 * Goal that makes the entity panic and flee from the player
 * Includes erratic movement and behavior
 */
public class PanicedGoal extends BaseBlueice129Goal {
    
    private PlayerEntity targetPlayer;
    private int panicTime;
    
    public PanicedGoal(Blueice129Entity entity) {
        super(entity, 1);
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.JUMP));
    }
    
    @Override
    protected boolean shouldStart() {
        return isInState(Blueice129Entity.EntityState.PANICED);
    }
    
    @Override
    protected boolean shouldKeepRunning() {
        return panicTime > 0 && isInState(Blueice129Entity.EntityState.PANICED);
    }
    
    @Override
    protected void onStart() {
        panicTime = 100 + entity.getRandom().nextInt(100);
        targetPlayer = entity.getWorld().getClosestPlayer(entity, 64.0);
    }
    
    @Override
    public void tick() {
        panicTime--;
        
        // Flee from player if nearby
        if (targetPlayer != null && entity.squaredDistanceTo(targetPlayer) < 256.0) {
            // Calculate flee direction (away from player)
            double dx = entity.getX() - targetPlayer.getX();
            double dz = entity.getZ() - targetPlayer.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            
            if (distance > 0) {
                double fleeX = entity.getX() + (dx / distance) * 8.0;
                double fleeZ = entity.getZ() + (dz / distance) * 8.0;
                
                entity.getNavigation().startMovingTo(fleeX, entity.getY(), fleeZ, 1.5);
            }
        }
        
        // Random jumps during panic
        if (entity.getRandom().nextFloat() < 0.1F) {
            entity.getJumpControl().setActive();
        }
    }
    
    @Override
    protected void onStop() {
        targetPlayer = null;
        entity.getNavigation().stop();
    }
}
