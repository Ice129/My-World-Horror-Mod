package horror.blueice129.entity.goals.interaction;

import horror.blueice129.entity.Blueice129Entity;
import horror.blueice129.entity.goals.BaseBlueice129Goal;
import net.minecraft.entity.ai.goal.Goal;
import java.util.EnumSet;

/**
 * Goal that makes the entity open doors
 * TODO: Implement door opening logic
 */
public class OpenDoorsGoal extends BaseBlueice129Goal {
    
    public OpenDoorsGoal(Blueice129Entity entity) {
        super(entity, 15);
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }
    
    @Override
    protected boolean shouldStart() {
        return isInState(Blueice129Entity.EntityState.INVESTIGATING);
    }
    
    @Override
    public void tick() {
        // TODO: Find nearest door within range
        // TODO: Pathfind to door
        // TODO: Open/close door
    }
}
