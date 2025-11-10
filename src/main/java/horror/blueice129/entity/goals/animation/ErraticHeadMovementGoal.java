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
            // Generate random yaw and pitch offsets for erratic movement
            float yawOffset = (entity.getRandom().nextFloat() - 0.5f) * 100f; // -30 to +30 degrees
            float pitchOffset = (entity.getRandom().nextFloat() - 0.5f) * 80f; // -20 to +20 degrees
            
            // Apply the offsets to current rotation
            entity.headYaw += yawOffset;
            entity.prevHeadYaw = entity.headYaw;
            entity.setPitch(entity.getPitch() + pitchOffset);
            
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
