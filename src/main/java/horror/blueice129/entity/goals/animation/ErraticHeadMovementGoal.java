package horror.blueice129.entity.goals.animation;

import horror.blueice129.entity.Blueice129Entity;
import horror.blueice129.entity.goals.BaseBlueice129Goal;

/**
 * Goal that makes the entity move its head erratically (for PANICED state)
 * TODO: Implement erratic head movement behavior
 */
public class ErraticHeadMovementGoal extends BaseBlueice129Goal {
    
    public ErraticHeadMovementGoal(Blueice129Entity entity) {
        super(entity, 1);
    }
    
    @Override
    protected boolean shouldStart() {
        return isInState(Blueice129Entity.EntityState.PANICED);
    }
    
    @Override
    public void tick() {
        // TODO: Implement erratic head movement
        // Rapidly change look direction to simulate panic
    }
}
