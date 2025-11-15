package horror.blueice129.entity.goals.building;

import horror.blueice129.entity.Blueice129Entity;
import horror.blueice129.entity.goals.BaseBlueice129Goal;
import net.minecraft.entity.ai.goal.Goal;
import java.util.EnumSet;

/**
 * Goal that makes the entity clear an area of blocks
 * TODO: Implement area clearing logic
 */
public class ClearAreaGoal extends BaseBlueice129Goal {
    
    public ClearAreaGoal(Blueice129Entity entity) {
        super(entity);
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }
    
    @Override
    protected boolean shouldStart() {
        return isInState(Blueice129Entity.EntityState.UPGRADING_HOUSE);
    }
    
    @Override
    public void tick() {
        // TODO: Define area to clear
        // TODO: Systematically break blocks in area
        // TODO: Store items in inventory or nearby chests
    }
}
