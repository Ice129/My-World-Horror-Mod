package horror.blueice129.entity.goals.animation;

import horror.blueice129.entity.Blueice129Entity;
import horror.blueice129.entity.goals.BaseBlueice129Goal;

/**
 * Goal that makes the entity move its head erratically (for PANICED state)
 * Creates random look targets around the entity at intervals for a panicked appearance
 */
public class ErraticHeadMovementGoal extends BaseBlueice129Goal {
    
    private static final int LOOK_CHANGE_INTERVAL = 1; // Change look direction every tick
    private static final double LOOK_DISTANCE = 10.0; // Distance to look target
    private static final double MIN_ANGLE_DIFFERENCE = Math.PI * 0.6; // Minimum 108 degrees difference
    private int tickCounter = 0;
    private double lastYaw = 0;
    private double lastPitch = 0;

    public ErraticHeadMovementGoal(Blueice129Entity entity) {
        super(entity);
    }

    @Override
    protected boolean shouldStart() {
        return isInState(Blueice129Entity.EntityState.PANICED);
    }

    //IMPROVE: make this better, but its good enough for now
    @Override
    public void tick() {
        tickCounter++;
        
        // Change look direction at intervals for erratic but smooth movement
        if (tickCounter >= LOOK_CHANGE_INTERVAL) {
            tickCounter = 0;
            
            double yaw, pitch;
            int attempts = 0;
            
            // Keep generating random directions until we find one far from the previous
            do {
                yaw = entity.getRandom().nextDouble() * 2.0 * Math.PI;
                pitch = (entity.getRandom().nextDouble() - 0.5) * Math.PI;
                attempts++;
            } while (attempts < 10 && !isFarEnough(yaw, pitch));
            
            lastYaw = yaw;
            lastPitch = pitch;
            
            // Calculate look target position
            double x = entity.getX() + Math.cos(yaw) * Math.cos(pitch) * LOOK_DISTANCE;
            double y = entity.getEyeY() + Math.sin(pitch) * LOOK_DISTANCE;
            double z = entity.getZ() + Math.sin(yaw) * Math.cos(pitch) * LOOK_DISTANCE;
            
            // LookControl handles smooth interpolation automatically
            entity.getLookControl().lookAt(x, y, z);
        }
    }
    
    /**
     * Check if the new angles are far enough from the previous ones
     */
    private boolean isFarEnough(double yaw, double pitch) {
        // Calculate angular distance (simplified check using both yaw and pitch difference)
        double yawDiff = Math.abs(yaw - lastYaw);
        double pitchDiff = Math.abs(pitch - lastPitch);
        
        // Normalize yaw difference to be within [0, PI] (shortest angular distance)
        if (yawDiff > Math.PI) {
            yawDiff = 2.0 * Math.PI - yawDiff;
        }
        
        // Require significant change in at least one axis
        return yawDiff > MIN_ANGLE_DIFFERENCE || pitchDiff > MIN_ANGLE_DIFFERENCE * 0.5;
    }
}
