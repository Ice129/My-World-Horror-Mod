package horror.blueice129.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

/**
 * Blueice129 Entity - A custom PathAwareEntity that takes the form of a player
 * character
 * with Blueice129's skin.
 * 
 * PathAwareEntity extends MobEntity, which extends LivingEntity.
 * - LivingEntity has health and can deal damage.
 * - MobEntity has movement controls and AI capabilities.
 * - PathAwareEntity has pathfinding favor and slightly tweaked leash behavior.
 */
public class Blueice129Entity extends PathAwareEntity {

    // TODO: Implement state-based behavior using currentState
    // TODO: Implement PASSIVE goal
    // TODO: Implement PANICED goal
    // TODO: Implement SURFACE_HIDING goal
    // TODO: Implement UNDERGROUND_BURROWING goal
    // TODO: Implement IN_MENUS goal
    // TODO: Implement INVESTIGATING goal
    // TODO: Implement UPGRADING_HOUSE goal

    private EntityState currentState = EntityState.PASSIVE;
    private final int costlyCheckTickCooldown = 10;
    private int costlyCheckTickCounter = 0;

    public enum EntityState {
        PASSIVE, // Default state, does nothing really
        PANICED, // jumps away from player, cycles hotbar, head jerks around like mouse is being
                 // shaken, crouches randomly
        SURFACE_HIDING, // hides behind trees/other structures, peeks out occasionally, always crouched
        UNDERGROUND_BURROWING, // crouched in the ground near player, mines blocks when player does, attempts
                               // to follow player
        IN_MENUS, // stops moving, no actions taken.
        INVESTIGATING, // opens chests, furnaces, opens doors, presses buttons and levers, generally
                       // interacts with the environment
        UPGRADING_HOUSE // places blocks, breaks old blocks, clears out area, uses blocks and tools from
                        // inventory
    }

    /**
     * Change the entity's state and update AI goals accordingly
     */
    public void setState(EntityState newState) {
        if (this.currentState == newState)
            return;

        this.currentState = newState;
        updateGoals();
    }

    /**
     * Update AI goals based on the current state.
     * This method clears existing goals and adds new ones based on state.
     */
    private void updateGoals() {
        // TODO: Clear existing goals
        // TODO: finish this method
        // this.goalSelector.remove(null);
    }

    /**
     * will handle automatic state transitions and per-tick behavior
     */
    @Override
    public void tick() {
        super.tick();
        costlyCheckTickCounter++;
        boolean shouldPerformCostlyCheck = costlyCheckTickCounter >= costlyCheckTickCooldown;

        switch (currentState) {
            case PASSIVE:
                // TODO: Implement transitions from PASSIVE to other states
                break;
            case PANICED:
                // TODO: Implement transitions from PANICED to other states
                break;
            case SURFACE_HIDING:
                // TODO: Implement transitions from SURFACE_HIDING to other states
                break;
            case UNDERGROUND_BURROWING:
                // TODO: Implement transitions from UNDERGROUND_BURROWING to other states
                break;
            case IN_MENUS:
                // TODO: Implement transitions from IN_MENUS to other states
                break;
            case INVESTIGATING:
                // TODO: Implement transitions from INVESTIGATING to other states
                break;
            case UPGRADING_HOUSE:
                // TODO: Implement transitions from UPGRADING_HOUSE to other states
                break;
        }
    }

    /**
     * Get the current state of the entity
     */
    public EntityState getState() {
        return currentState;
    }

    public Blueice129Entity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    /**
     * Initialize AI goals for the entity.
     * This method is called during entity construction to set up behavior.
     */
    @Override
    protected void initGoals() {
        // test commit

        // Basic goals applicable in all states, can be overridden in state-specific updates
        // TODO: Add state-specific goals in updateGoals()
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(5, new LookAroundGoal(this));
    }

    /**
     * Create default attributes for the Blueice129 entity.
     * This includes health, movement speed, attack damage, etc.
     */
    public static DefaultAttributeContainer.Builder createBlueice129Attributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0D) // Same as player
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25D) // Slightly slower than player
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 35.0D); // How far they can detect entities
    }
}
