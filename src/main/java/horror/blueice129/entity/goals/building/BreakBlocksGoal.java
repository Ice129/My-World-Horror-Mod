package horror.blueice129.entity.goals.building;

import horror.blueice129.entity.Blueice129Entity;
import horror.blueice129.entity.goals.BaseBlueice129Goal;
import net.minecraft.entity.ai.goal.Goal;
import java.util.EnumSet;

/**
 * Goal that makes the entity break blocks
 * TODO: Implement block breaking logic
 */
public class BreakBlocksGoal extends BaseBlueice129Goal {
    
    public BreakBlocksGoal(Blueice129Entity entity) {
        super(entity, 15);
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }
    
    @Override
    protected boolean shouldStart() {
        return isInState(Blueice129Entity.EntityState.UPGRADING_HOUSE);
    }
    
    @Override
    public void tick() {
        // TODO: Identify blocks to break (old/unwanted blocks)
        // TODO: Select appropriate tool from inventory
        // TODO: Break block with animation
        // TODO: Collect drops
    }
}
