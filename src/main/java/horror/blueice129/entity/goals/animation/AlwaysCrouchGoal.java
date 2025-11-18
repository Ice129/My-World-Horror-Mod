package horror.blueice129.entity.goals.animation;

import horror.blueice129.entity.Blueice129Entity;
import horror.blueice129.entity.goals.BaseBlueice129Goal;

/**
 * Goal that crouches the entity when stationary in hiding states.
 * Entity only crouches when not moving between spots to maintain realistic behavior.
 */
public class AlwaysCrouchGoal extends BaseBlueice129Goal {
    
    private static final double MOVEMENT_THRESHOLD = 0.01; // Velocity threshold to determine if entity is moving
    
    public AlwaysCrouchGoal(Blueice129Entity entity) {
        super(entity);
    }
    
    @Override
    protected boolean shouldStart() {
        return isInState(Blueice129Entity.EntityState.SURFACE_HIDING) || 
               isInState(Blueice129Entity.EntityState.UNDERGROUND_BURROWING);
    }
    
    @Override
    public void tick() {
        // Check if entity is currently moving
        boolean isMoving = entity.getVelocity().lengthSquared() > MOVEMENT_THRESHOLD;
        
        // Only crouch when stationary (not moving between hiding spots)
        if (!isMoving && entity.getNavigation().isIdle()) {
            entity.setSneaking(true);
        } else {
            // Stand up when moving to next hiding spot
            entity.setSneaking(false);
        }
    }
}
