package horror.blueice129.entity.goals.movement;

import horror.blueice129.entity.Blueice129Entity;
import horror.blueice129.entity.goals.BaseBlueice129Goal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import java.util.EnumSet;

/**
 * Goal that makes the entity follow the player while staying underground
 * TODO: Implement underground following with pathfinding
 */
public class FollowPlayerUndergroundGoal extends BaseBlueice129Goal {
    
    private PlayerEntity targetPlayer;
    private final double followDistance = 16.0;
    
    public FollowPlayerUndergroundGoal(Blueice129Entity entity) {
        super(entity, 10);
        this.setControls(EnumSet.of(Goal.Control.MOVE));
    }
    
    @Override
    protected boolean shouldStart() {
        if (!isInState(Blueice129Entity.EntityState.UNDERGROUND_BURROWING)) {
            return false;
        }
        
        targetPlayer = entity.getWorld().getClosestPlayer(entity, 64.0);
        return targetPlayer != null && entity.squaredDistanceTo(targetPlayer) > followDistance * followDistance;
    }
    
    @Override
    protected boolean shouldKeepRunning() {
        return targetPlayer != null && targetPlayer.isAlive() && 
               entity.squaredDistanceTo(targetPlayer) > followDistance * followDistance;
    }
    
    @Override
    public void tick() {
        // TODO: Pathfind towards player while staying underground
        // TODO: Maintain distance, dig through blocks if necessary
    }
}
