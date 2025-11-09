package horror.blueice129.entity.goals.interaction;

import horror.blueice129.entity.Blueice129Entity;
import horror.blueice129.entity.goals.BaseBlueice129Goal;
import net.minecraft.entity.ai.goal.Goal;
import java.util.EnumSet;

/**
 * Goal that makes the entity open nearby chests
 * TODO: Implement chest opening logic with pathfinding
 */
public class OpenChestsGoal extends BaseBlueice129Goal {
    
    public OpenChestsGoal(Blueice129Entity entity) {
        super(entity, 20);
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }
    
    @Override
    protected boolean shouldStart() {
        return isInState(Blueice129Entity.EntityState.INVESTIGATING);
    }
    
    @Override
    public void tick() {
        // TODO: Find nearest chest within range
        // TODO: Pathfind to chest
        // TODO: Open chest (play animation/sound)
        // TODO: Wait briefly, then close
    }
}
