package horror.blueice129.entity.goals.animation;

import horror.blueice129.entity.Blueice129Entity;
import horror.blueice129.entity.goals.BaseBlueice129Goal;

/**
 * Goal that keeps the entity always crouched (for SURFACE_HIDING and UNDERGROUND_BURROWING states)
 */
public class AlwaysCrouchGoal extends BaseBlueice129Goal {
    
    public AlwaysCrouchGoal(Blueice129Entity entity) {
        super(entity, 1);
    }
    
    @Override
    protected boolean shouldStart() {
        return isInState(Blueice129Entity.EntityState.SURFACE_HIDING) || 
               isInState(Blueice129Entity.EntityState.UNDERGROUND_BURROWING);
    }
    
    @Override
    public void tick() {
        // TODO: Keep entity crouched
        // entity.setSneaking(true);
    }
}
