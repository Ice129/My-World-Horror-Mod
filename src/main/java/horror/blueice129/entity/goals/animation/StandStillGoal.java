package horror.blueice129.entity.goals.animation;

import horror.blueice129.entity.Blueice129Entity;
import horror.blueice129.entity.goals.BaseBlueice129Goal;

/**
 * Goal that makes the entity stand completely still (for IN_MENUS state)
 */
public class StandStillGoal extends BaseBlueice129Goal {
    
    public StandStillGoal(Blueice129Entity entity) {
        super(entity);
    }
    
    @Override
    protected boolean shouldStart() {
        return isInState(Blueice129Entity.EntityState.IN_MENUS);
    }
    
    @Override
    public void tick() {
        // Stop all movement
        entity.getNavigation().stop();
        entity.setVelocity(0, entity.getVelocity().y, 0);
    }
}
