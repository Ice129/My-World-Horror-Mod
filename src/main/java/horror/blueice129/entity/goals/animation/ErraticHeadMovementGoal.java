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
        // Implement erratic head movement
        // Rapidly change look direction to simulate panic
        if (entity.getRandom().nextInt(2) == 0) {
            // random yaw and pitch in any direction 360 degrees
            float yaw = entity.getRandom().nextFloat() * 360f - 180f;
            float pitch = entity.getRandom().nextFloat() * 180f - 90f;
            
            // Apply the offsets to current rotation
            entity.headYaw += yaw;
            entity.prevHeadYaw = entity.headYaw;
            entity.setPitch(entity.getPitch() + pitch);
            
            // Clamp pitch to valid range (-90 to 90 degrees)
            float currentPitch = entity.getPitch();
            if (currentPitch > 90f) {
                entity.setPitch(90f);
            } else if (currentPitch < -90f) {
                entity.setPitch(-90f);
            }
        }
    }
}
