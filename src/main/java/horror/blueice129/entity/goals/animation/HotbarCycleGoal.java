package horror.blueice129.entity.goals.animation;

import horror.blueice129.entity.Blueice129Entity;
import horror.blueice129.entity.goals.BaseBlueice129Goal;

/**
 * Goal that makes the entity cycle through hotbar slots (for PANICED state)
 * TODO: Implement hotbar cycling behavior
 */
public class HotbarCycleGoal extends BaseBlueice129Goal {
    
    public HotbarCycleGoal(Blueice129Entity entity) {
        super(entity);
    }
    
    @Override
    protected boolean shouldStart() {
        return isInState(Blueice129Entity.EntityState.PANICED);
    }
    
    @Override
    public void tick() {
        // TODO: Implement hotbar cycling
        // Simulate player rapidly scrolling through hotbar
    }
}
