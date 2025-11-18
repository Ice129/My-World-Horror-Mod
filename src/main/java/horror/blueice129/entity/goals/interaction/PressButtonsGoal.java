package horror.blueice129.entity.goals.interaction;

import horror.blueice129.entity.Blueice129Entity;
import horror.blueice129.entity.goals.BaseBlueice129Goal;
import net.minecraft.entity.ai.goal.Goal;
import java.util.EnumSet;

/**
 * Goal that makes the entity press buttons
 * TODO: Implement button pressing logic
 */
public class PressButtonsGoal extends BaseBlueice129Goal {
    
    public PressButtonsGoal(Blueice129Entity entity) {
        super(entity);
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }
    
    @Override
    protected boolean shouldStart() {
        return isInState(Blueice129Entity.EntityState.INVESTIGATING);
    }
    
    @Override
    public void tick() {
        // TODO: Find nearest button within range
        // TODO: Pathfind to button
        // TODO: Press button
    }
}
