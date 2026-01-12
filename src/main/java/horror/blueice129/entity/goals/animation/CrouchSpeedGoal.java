package horror.blueice129.entity.goals.animation;

import horror.blueice129.entity.Blueice129Entity;
import horror.blueice129.entity.goals.BaseBlueice129Goal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import java.util.EnumSet;
import java.util.UUID;

/**
 * Goal that applies crouch speed reduction when entity is sneaking.
 * Player crouch speed is approximately 30% of walking speed (0.3x multiplier).
 * This ensures the entity moves realistically slow when crouched.
 */
public class CrouchSpeedGoal extends BaseBlueice129Goal {
    
    // Unique UUID for crouch speed modifier
    private static final UUID CROUCH_SPEED_UUID = UUID.fromString("8b3c9d2e-1f4a-4c5b-9e8d-7a6f5e4d3c2b");
    private static final String CROUCH_SPEED_NAME = "blueice129_crouch_speed";
    private static final double CROUCH_SPEED_MULTIPLIER = 0.3; // 30% of normal speed when crouching
    
    private boolean modifierApplied = false;
    
    public CrouchSpeedGoal(Blueice129Entity entity) {
        super(entity);
        this.setControls(EnumSet.noneOf(Goal.Control.class)); // Doesn't control movement, just modifies speed
    }
    
    @Override
    protected boolean shouldStart() {
        // Active when entity is sneaking
        return entity.isSneaking();
    }
    
    @Override
    protected boolean shouldKeepRunning() {
        return entity.isSneaking();
    }
    
    @Override
    protected void onStart() {
        applyCrouchSpeedModifier();
    }
    
    @Override
    public void tick() {
        // Ensure modifier stays applied while crouching
        if (entity.isSneaking() && !modifierApplied) {
            applyCrouchSpeedModifier();
        } else if (!entity.isSneaking() && modifierApplied) {
            removeCrouchSpeedModifier();
        }
    }
    
    @Override
    protected void onStop() {
        removeCrouchSpeedModifier();
    }
    
    /**
     * Apply the crouch speed modifier to the entity
     */
    private void applyCrouchSpeedModifier() {
        EntityAttributeInstance speedAttribute = entity.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        
        if (speedAttribute != null) {
            // Remove any existing crouch modifier first
            EntityAttributeModifier existingModifier = speedAttribute.getModifier(CROUCH_SPEED_UUID);
            if (existingModifier != null) {
                speedAttribute.removeModifier(CROUCH_SPEED_UUID);
            }
            
            // Apply crouch speed penalty (0.3x = -0.7 with MULTIPLY_BASE)
            EntityAttributeModifier modifier = new EntityAttributeModifier(
                CROUCH_SPEED_UUID,
                CROUCH_SPEED_NAME,
                CROUCH_SPEED_MULTIPLIER - 1.0, // -0.7 for 30% speed
                EntityAttributeModifier.Operation.MULTIPLY_BASE
            );
            
            speedAttribute.addTemporaryModifier(modifier);
            modifierApplied = true;
        }
    }
    
    /**
     * Remove the crouch speed modifier from the entity
     */
    private void removeCrouchSpeedModifier() {
        EntityAttributeInstance speedAttribute = entity.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        
        if (speedAttribute != null) {
            EntityAttributeModifier existingModifier = speedAttribute.getModifier(CROUCH_SPEED_UUID);
            if (existingModifier != null) {
                speedAttribute.removeModifier(CROUCH_SPEED_UUID);
            }
            modifierApplied = false;
        }
    }
}
