package horror.blueice129.entity;

import horror.blueice129.entity.goals.GoalProfileRegistry;
import horror.blueice129.HorrorMod129;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
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

    // Constants for entity behavior configuration
    private static final int COSTLY_CHECK_TICK_COOLDOWN = 10;
    private static final double PLAYER_DETECTION_RANGE = 64.0;
    private static final double PLAYER_FLEE_RANGE = 50.0;
    private static final double FIELD_OF_VIEW_THRESHOLD = 0.7; // ~45 degree FOV
    private static final double VELOCITY_STOPPED_THRESHOLD = 0.01;
    private static final int PANIC_DURATION_TICKS = 10; // 0.5 seconds (20 ticks = 1 second)
    private static final int FLEEING_DURATION_TICKS = 100; // 5 seconds
    private static final int FLEEING_STUCK_CHECK_TICKS = 40; // 2 seconds
    private static final int LOGOUT_DELAY_TICKS = 20; // 1 second

    private EntityState currentState;
    private int costlyCheckTickCounter = 0;
    private int ticksInCurrentState = 0;
    private boolean shouldLogoutAfterMenu = false;
    private GoalProfileRegistry goalRegistry;

    public enum EntityState {
        PASSIVE, // Default state, does nothing really
        PANICED, // jumps, cycles hotbar, head jerks around like mouse is being
                 // shaken, crouches randomly
        FLEEING, // runs away from player, attempts to break line of sight, occasionally looks
                 // back
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
        this.ticksInCurrentState = 0; // Reset the timer when changing states
        updateGoals();
    }

    /**
     * Update AI goals based on the current state.
     * This method uses the GoalProfileRegistry to apply the appropriate goal
     * profile.
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
        ticksInCurrentState++;

        boolean doCostlyChecks = costlyCheckTickCounter >= COSTLY_CHECK_TICK_COOLDOWN;
        boolean seesPlayer = false;
        if (doCostlyChecks) {
            seesPlayer = checkEntitySeesPlayer();
            costlyCheckTickCounter = 0;
        }

        switch (currentState) {
            case PASSIVE:
                // Check if player is within 64 blocks and within line of sight
                // If the entity sees the player, transition to PANICED
                if (doCostlyChecks && seesPlayer) {
                    setState(EntityState.PANICED);
                }
                // check if the player damages the entity
                if (doCostlyChecks && checkPlayerDamagesEntity()) {
                    setState(EntityState.PANICED);
                }
                shouldLogoutAfterMenu = false;
                break;
            case PANICED:
                // after a certain amount of time panicing, probably 10-20 ticks, transition to
                // IN_MENUS, passing a param to say to log out after 5-15 ticks
                // if agro meter high enough, increase chance to enter fleeing/ hiding /
                // aggravated states
                if (ticksInCurrentState > PANIC_DURATION_TICKS) {
                    setState(EntityState.FLEEING);
                    shouldLogoutAfterMenu = true; // done here so it doesn't log out when implementing higher aggro
                                                     // actions
                }

                break;
            case FLEEING:
                if (ticksInCurrentState > FLEEING_DURATION_TICKS) {
                    setState(EntityState.IN_MENUS);
                }
                // if player not within 50 blocks, go back to passive
                if (doCostlyChecks) {
                    PlayerEntity nearestPlayer = this.getWorld().getClosestPlayer(this, PLAYER_FLEE_RANGE);
                    if (nearestPlayer == null) {
                        setState(EntityState.PASSIVE);
                    }
                }
                // if the entity is no longer moving (stuck), go to IN_MENUS
                if (this.getVelocity().lengthSquared() < VELOCITY_STOPPED_THRESHOLD && ticksInCurrentState > FLEEING_STUCK_CHECK_TICKS) {
                    setState(EntityState.IN_MENUS);
                    HorrorMod129.LOGGER.info("Blueice129Entity: FLEEING state - entity stuck, transitioning to IN_MENUS");
                    shouldLogoutAfterMenu = true;
                }
                if (doCostlyChecks && checkPlayerDamagesEntity()) {
                    setState(EntityState.PANICED);
                    shouldLogoutAfterMenu = true;
                }


                break;
            case SURFACE_HIDING:
                // TODO: Implement transitions from SURFACE_HIDING to other states
                break;
            case UNDERGROUND_BURROWING:
                // TODO: Implement transitions from UNDERGROUND_BURROWING to other states
                break;
            case IN_MENUS:
                // TODO: Implement transitions from IN_MENUS to other states
                if (ticksInCurrentState > LOGOUT_DELAY_TICKS && shouldLogoutAfterMenu) {
                    // despawn this entity and send a logout message to the chat
                    if (this.getWorld().getServer() != null) {
                        this.getWorld().getServer().getPlayerManager().broadcast(net.minecraft.text.Text
                                .literal("Blueice129 left the game").styled(style -> style.withColor(0xFFFF55)), false);
                    }
                    this.remove(RemovalReason.DISCARDED);
                }
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
     * Check if the player has damaged this entity recently
     */
    private boolean checkPlayerDamagesEntity() {
        // Check if the entity has been recently damaged by a player
        return this.getRecentDamageSource() != null
                && this.getRecentDamageSource().getAttacker() instanceof PlayerEntity;
    }

    /**
     * Check if the entity can see the player within detection range
     * Performs a raycast from the entity's eye position to the player's position
     * and checks if the player is within the entity's field of view
     */
    private boolean checkEntitySeesPlayer() {
        // Find the nearest player within detection range
        PlayerEntity nearestPlayer = this.getWorld().getClosestPlayer(this, PLAYER_DETECTION_RANGE);

        if (nearestPlayer == null) {
            return false;
        }

        // Check if the entity has line of sight to the player
        if (!this.canSee(nearestPlayer)) {
            return false;
        }

        // Check if the player is within the entity's field of view
        // Get the direction vector from entity to player
        double dx = nearestPlayer.getX() - this.getX();
        double dz = nearestPlayer.getZ() - this.getZ();

        // Normalize the direction to player
        double distanceToPlayer = Math.sqrt(dx * dx + dz * dz);
        if (distanceToPlayer < VELOCITY_STOPPED_THRESHOLD) {
            return true; // Player is extremely close, can definitely see them
        }

        dx /= distanceToPlayer;
        dz /= distanceToPlayer;

        // Get the entity's look direction (yaw is in degrees)
        double yawRadians = Math.toRadians(this.getYaw());
        double lookDirX = -Math.sin(yawRadians);
        double lookDirZ = Math.cos(yawRadians);

        // Calculate dot product to determine if player is in front of entity
        // Dot product > 0 means player is in front (within 180 degree arc)
        // For a narrower field of view, use a higher threshold (e.g., 0.5 for ~60
        // degrees)
        double dotProduct = dx * lookDirX + dz * lookDirZ;

        // Use configured threshold for field of view
        return dotProduct > FIELD_OF_VIEW_THRESHOLD;
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
        this.currentState = EntityState.PASSIVE;
    }

    /**
     * Initialize AI goals for the entity.
     * This method is called during entity construction to set up behavior.
     * The goal profile system handles goal initialization based on the current
     * state.
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
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25D) // Same as player
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 35.0D); // How far they can detect entities
    }
}
