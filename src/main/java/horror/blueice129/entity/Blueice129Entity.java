package horror.blueice129.entity;

import horror.blueice129.entity.goals.GoalProfileRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.world.World;

/**
 * Blueice129 Entity - A custom PathAwareEntity that takes the form of a player
 * character with Blueice129's skin.
 * 
 * PathAwareEntity extends MobEntity, which extends LivingEntity.
 * - LivingEntity has health and can deal damage.
 * - MobEntity has movement controls and AI capabilities.
 * - PathAwareEntity has pathfinding favor and slightly tweaked leash behavior.
 * 
 * This entity uses a state-based goal profile system where different behaviors
 * are activated based on the current EntityState.
 */
public class Blueice129Entity extends PathAwareEntity {

    private EntityState currentState = EntityState.PASSIVE;
    private final int costlyCheckTickCooldown = 10;
    private int costlyCheckTickCounter = 0;
    private GoalProfileRegistry goalRegistry;

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
     * This method uses the GoalProfileRegistry to apply the appropriate goal profile.
     */
    private void updateGoals() {
        if (goalRegistry != null) {
            goalRegistry.applyCurrentProfile();
        }
    }

    /**
     * will handle automatic state transitions and per-tick behavior
     */
    @Override
    public void tick() {
        super.tick();
        costlyCheckTickCounter++;
        
        // Perform costly checks less frequently
        if (costlyCheckTickCounter >= costlyCheckTickCooldown) {
            costlyCheckTickCounter = 0;
            performCostlyStateChecks();
        }

        switch (currentState) {
            case PASSIVE:
                // TODO: Implement transitions from PASSIVE to other states
                // check if player is within 64 blocks
                // check if player is within line of sight of the entity
                // specifically the block where the player's head is located
                // if the entitiy sees the player for over 5 ticks, transition to PANICED
                break;
            case PANICED:
                // TODO: Implement transitions from PANICED to other states
                // after a certain amount of time panicing, probably 10-20 ticks, transition to
                // IN_MENUS, passing a param to say to log out after 5-15 ticks
                // if agro meter high enough, increase chance to enter fleeing/ hiding /
                // aggravated states
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
    
    /**
     * Perform expensive state transition checks
     * This is called less frequently than tick() to optimize performance
     */
    private void performCostlyStateChecks() {
        // TODO: Implement costly state transition logic here
        // Examples: Line of sight checks, distance calculations, pathfinding validation
    }
    
    /**
     * Clear all goals from the goal selector
     * Helper method for the goal profile system
     */
    public void clearGoals() {
        this.goalSelector.clear(goal -> true); // Remove all goals
    }
    
    /**
     * Add a goal to the goal selector
     * Helper method for the goal profile system
     */
    public void addGoal(int priority, net.minecraft.entity.ai.goal.Goal goal) {
        this.goalSelector.add(priority, goal);
    }

    public Blueice129Entity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        // Initialize the goal profile registry
        this.goalRegistry = new GoalProfileRegistry(this);
    }

    /**
     * Initialize AI goals for the entity.
     * This method is called during entity construction to set up behavior.
     * The goal profile system handles goal initialization based on the current state.
     */
    @Override
    protected void initGoals() {
        // Apply the initial goal profile (PASSIVE state)
        if (goalRegistry != null) {
            goalRegistry.applyCurrentProfile();
        }
    }

    /**
     * Create default attributes for the Blueice129 entity.
     * This includes health, movement speed, attack damage, etc.
     */
    public static DefaultAttributeContainer.Builder createBlueice129Attributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0D) // Same as player
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.1D) // Same as player
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 35.0D); // How far they can detect entities
    }
}
