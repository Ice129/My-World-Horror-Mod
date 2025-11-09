package horror.blueice129.entity.goals.interaction;

import horror.blueice129.entity.Blueice129Entity;
import horror.blueice129.entity.goals.BaseBlueice129Goal;
import net.minecraft.entity.ai.goal.Goal;
import java.util.EnumSet;

/**
 * Goal that makes the entity flip levers
 * TODO: Implement lever flipping logic
 */
public class FlipLeversGoal extends BaseBlueice129Goal {
    
    public FlipLeversGoal(Blueice129Entity entity) {
        super(entity, 25);
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }
    
    @Override
    protected boolean shouldStart() {
        return isInState(Blueice129Entity.EntityState.INVESTIGATING);
    }
    
    @Override
    public void tick() {
        // TODO: Find nearest lever within range
        // TODO: Pathfind to lever
        // TODO: Flip lever
    }
}
