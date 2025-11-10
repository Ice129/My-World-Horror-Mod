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
 * Goal that applies a speed boost to the entity while active.
 * Can be configured to match player sprinting speed or custom speeds.
 * 
 * Player walking speed: 0.10
 * Player sprinting speed: 0.13 (1.3x walking)
 * Entity default speed: 0.25
 * 
 * This goal adds a speed modifier that can be adjusted based on state needs.
 */
public class SpeedBoostGoal extends BaseBlueice129Goal {
    
    // Unique UUID for this speed modifier (randomly generated, persistent)
    private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("fabbad50-68c0-4165-b67b-9d1c5d525821"); 
    // ^^^^ remember to have an actual uuid here, otherwise the entity wont summon and you will waste an entire morning figuring that out
    private static final String SPEED_MODIFIER_NAME = "blueice129_speed_boost";
    
    private final Blueice129Entity.EntityState[] activeStates;
    private final double speedMultiplier;
    private boolean modifierApplied = false;
    
    /**
     * Create a speed boost goal that activates in specific states
     * @param entity The entity to boost
     * @param speedMultiplier The multiplier to apply to base speed (1.5 = player sprinting speed)
     * @param activeStates The states where this boost should be active
     */
    public SpeedBoostGoal(Blueice129Entity entity, double speedMultiplier, Blueice129Entity.EntityState... activeStates) {
        super(entity, 1);
        this.speedMultiplier = speedMultiplier;
        this.activeStates = activeStates;
        this.setControls(EnumSet.noneOf(Goal.Control.class)); // Doesn't control anything, just modifies attributes
    }
    
    /**
     * Convenience constructor for matching player sprinting speed (1.3x base)
     */
    public static SpeedBoostGoal sprintSpeed(Blueice129Entity entity, Blueice129Entity.EntityState... activeStates) {
        return new SpeedBoostGoal(entity, 1.3, activeStates);
    }
    
    /**
     * Convenience constructor for matching player running speed (1.5x base)
     * Note: In Minecraft, "running" and "sprinting" are the same thing
     */
    public static SpeedBoostGoal runningSpeed(Blueice129Entity entity, Blueice129Entity.EntityState... activeStates) {
        return new SpeedBoostGoal(entity, 1.5, activeStates);
    }
    
    @Override
    protected boolean shouldStart() {
        // Check if entity is in any of the active states
        for (Blueice129Entity.EntityState state : activeStates) {
            if (isInState(state)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    protected boolean shouldKeepRunning() {
        return shouldStart(); // Keep running as long as we're in an active state
    }
    
    @Override
    protected void onStart() {
        applySpeedModifier();
    }
    
    @Override
    public void tick() {
        // Ensure modifier stays applied
        if (!modifierApplied) {
            applySpeedModifier();
        }
    }
    
    @Override
    protected void onStop() {
        removeSpeedModifier();
    }
    
    /**
     * Apply the speed modifier to the entity
     */
    private void applySpeedModifier() {
        EntityAttributeInstance speedAttribute = entity.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        
        if (speedAttribute != null) {
            // Remove any existing modifier with this UUID first
            EntityAttributeModifier existingModifier = speedAttribute.getModifier(SPEED_MODIFIER_UUID);
            if (existingModifier != null) {
                speedAttribute.removeModifier(SPEED_MODIFIER_UUID);
            }
            
            // Using MULTIPLY_BASE operation so it multiplies the base speed
            // for 1.5x speed, add 0.5 which becomes 1.0 + 0.5 = 1.5x
            EntityAttributeModifier modifier = new EntityAttributeModifier(
                SPEED_MODIFIER_UUID,
                SPEED_MODIFIER_NAME,
                speedMultiplier - 1.0,
                EntityAttributeModifier.Operation.MULTIPLY_BASE
            );
            
            speedAttribute.addTemporaryModifier(modifier);
            modifierApplied = true;
        }
    }
    
    /**
     * Remove the speed modifier from the entity
     */
    private void removeSpeedModifier() {
        EntityAttributeInstance speedAttribute = entity.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        
        if (speedAttribute != null) {
            EntityAttributeModifier existingModifier = speedAttribute.getModifier(SPEED_MODIFIER_UUID);
            if (existingModifier != null) {
                speedAttribute.removeModifier(SPEED_MODIFIER_UUID);
            }
            modifierApplied = false;
        }
    }
}
