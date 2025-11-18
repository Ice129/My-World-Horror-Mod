package horror.blueice129.entity.goals.building;

import horror.blueice129.entity.Blueice129Entity;
import horror.blueice129.entity.goals.BaseBlueice129Goal;
import net.minecraft.entity.ai.goal.Goal;
import java.util.EnumSet;

/**
 * Goal that makes the entity place blocks from inventory
 * TODO: Implement block placement logic
 */
public class PlaceBlocksGoal extends BaseBlueice129Goal {
    
    public PlaceBlocksGoal(Blueice129Entity entity) {
        super(entity);
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }
    
    @Override
    protected boolean shouldStart() {
        return isInState(Blueice129Entity.EntityState.UPGRADING_HOUSE);
    }
    
    @Override
    public void tick() {
        // TODO: Select block from inventory
        // TODO: Find suitable placement location
        // TODO: Place block at location
        // TODO: Play placement animation/sound
    }
}
