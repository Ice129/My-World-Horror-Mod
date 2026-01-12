package horror.blueice129.entity.goals.animation;

import horror.blueice129.entity.Blueice129Entity;
import horror.blueice129.entity.goals.BaseBlueice129Goal;

/**
 * Goal that makes the entity crouch randomly (for PANICED state)
 * TODO: Implement random crouching behavior
 */
public class RandomCrouchGoal extends BaseBlueice129Goal {
    
    public RandomCrouchGoal(Blueice129Entity entity) {
        super(entity);
    }
    
    @Override
    protected boolean shouldStart() {
        return isInState(Blueice129Entity.EntityState.PANICED) && entity.getRandom().nextFloat() < 0.1F;
    }
    
    @Override
    public void tick() {
        // TODO: Implement random crouching
        // entity.setSneaking(true/false) at random intervals
    }
}
