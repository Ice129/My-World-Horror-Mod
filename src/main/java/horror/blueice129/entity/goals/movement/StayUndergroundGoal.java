package horror.blueice129.entity.goals.movement;

import horror.blueice129.entity.Blueice129Entity;
import horror.blueice129.entity.goals.BaseBlueice129Goal;
import net.minecraft.entity.ai.goal.Goal;
import java.util.EnumSet;

/**
 * Goal that keeps the entity underground
 * TODO: Implement underground positioning logic
 */
public class StayUndergroundGoal extends BaseBlueice129Goal {
    
    public StayUndergroundGoal(Blueice129Entity entity) {
        super(entity, 10);
        this.setControls(EnumSet.of(Goal.Control.MOVE));
    }
    
    @Override
    protected boolean shouldStart() {
        return isInState(Blueice129Entity.EntityState.UNDERGROUND_BURROWING) && 
               !entity.getWorld().isSkyVisible(entity.getBlockPos());
    }
    
    @Override
    public void tick() {
        // TODO: If above ground, pathfind to nearest cave/underground area
        // TODO: Stay at similar Y level as player but underground
    }
}
