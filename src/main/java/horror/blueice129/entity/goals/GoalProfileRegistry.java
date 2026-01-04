package horror.blueice129.entity.goals;

import horror.blueice129.entity.Blueice129Entity;
import horror.blueice129.entity.Blueice129Entity.EntityState;
import horror.blueice129.entity.goals.animation.*;
import horror.blueice129.entity.goals.basic.*;
import horror.blueice129.entity.goals.building.*;
import horror.blueice129.entity.goals.interaction.*;
import horror.blueice129.entity.goals.movement.*;
import java.util.EnumMap;
import java.util.Map;

/**
 * Registry that manages goal profiles for different entity states.
 * This class maps each EntityState to a GoalProfile containing the goals
 * that should be active when the entity is in that state.
 * 
 * USAGE:
 * 1. Create profiles for each state using createProfileForState()
 * 2. Register the profiles using registerProfile()
 * 3. Apply profiles to entity using applyProfile()
 */
public class GoalProfileRegistry {
    
    private final Map<EntityState, GoalProfile> profiles;
    private final Blueice129Entity entity;
    
    public GoalProfileRegistry(Blueice129Entity entity) {
        this.entity = entity;
        this.profiles = new EnumMap<>(EntityState.class);
        initializeProfiles();
    }
    
    /**
     * Initialize all state profiles.
     * This is where you define which goals are active in each state.
     */
    private void initializeProfiles() {
        // PASSIVE State Profile
        registerProfile(EntityState.PASSIVE, createPassiveProfile());
        
        // PANICED State Profile
        registerProfile(EntityState.PANICED, createPanicedProfile());

        // FLEEING State Profile
        registerProfile(EntityState.FLEEING, createFleeingProfile());
        
        // SURFACE_HIDING State Profile
        registerProfile(EntityState.SURFACE_HIDING, createSurfaceHidingProfile());
        
        // UNDERGROUND_BURROWING State Profile
        registerProfile(EntityState.UNDERGROUND_BURROWING, createUndergroundBurrowingProfile());
        
        // IN_MENUS State Profile
        registerProfile(EntityState.IN_MENUS, createInMenusProfile());
        
        // INVESTIGATING State Profile
        registerProfile(EntityState.INVESTIGATING, createInvestigatingProfile());
        
        // UPGRADING_HOUSE State Profile
        registerProfile(EntityState.UPGRADING_HOUSE, createUpgradingHouseProfile());
    }
    
    /**
     * Create the goal profile for PASSIVE state
     */
    private GoalProfile createPassiveProfile() {
        return GoalProfile.create()
            .addGoal(0, new SwimGoal(entity))
            .addGoal(1, new LookAtPlayerGoal(entity, 6.0F))
            .addGoal(2, new WanderGoal(entity, 1.0))
            .addGoal(3, new LookAroundGoal(entity));
    }
    
    /**
     * Create the goal profile for PANICED state
     */
    private GoalProfile createPanicedProfile() {
        return GoalProfile.create()
            .addGoal(0, new SwimGoal(entity))
            .addGoal(1, new SpeedBoostGoal(entity, 1.3, EntityState.PANICED))
            // .addGoal(2, new PanicedGoal(entity))
            .addGoal(2, new ErraticHeadMovementGoal(entity))
            // .addGoal(3, new RandomCrouchGoal(entity))
            .addGoal(3, new JumpOnceGoal(entity))
            .addGoal(4, new HotbarCycleGoal(entity));
    }

    /**
     * Create the goal profile for FLEEING state
     */
    private GoalProfile createFleeingProfile() {
        return GoalProfile.create()
            .addGoal(0, new SwimGoal(entity))
            .addGoal(1, new SpeedBoostGoal(entity, 1.5, EntityState.FLEEING))
            .addGoal(2, new FleeingGoal(entity));
    }
    
    /**
     * Create the goal profile for SURFACE_HIDING state
     */
    private GoalProfile createSurfaceHidingProfile() {
        return GoalProfile.create()
            .addGoal(0, new SwimGoal(entity))
            .addGoal(1, new HideBehindStructuresGoal(entity))
            .addGoal(2, new CrouchSpeedGoal(entity)); // Speed reduction when crouching (crouching is handled by HideBehindStructuresGoal)
    }
    
    /**
     * Create the goal profile for UNDERGROUND_BURROWING state
     */
    private GoalProfile createUndergroundBurrowingProfile() {
        return GoalProfile.create()
            .addGoal(0, new SwimGoal(entity))
            .addGoal(1, new StayUndergroundGoal(entity))
            .addGoal(2, new FollowPlayerUndergroundGoal(entity))
            .addGoal(3, new MimicPlayerMiningGoal(entity))
            .addGoal(4, new AlwaysCrouchGoal(entity))
            .addGoal(5, new CrouchSpeedGoal(entity));
    }
    
    /**
     * Create the goal profile for IN_MENUS state
     */
    private GoalProfile createInMenusProfile() {
        return GoalProfile.create()
            .addGoal(0, new StandStillGoal(entity));
    }
    
    /**
     * Create the goal profile for INVESTIGATING state
     */
    private GoalProfile createInvestigatingProfile() {
        return GoalProfile.create()
            .addGoal(0, new SwimGoal(entity))
            .addGoal(1, new OpenChestsGoal(entity))
            .addGoal(2, new OpenDoorsGoal(entity))
            .addGoal(3, new PressButtonsGoal(entity))
            .addGoal(4, new FlipLeversGoal(entity))
            .addGoal(5, new WanderGoal(entity, 1.0))
            .addGoal(6, new LookAroundGoal(entity));
    }
    
    /**
     * Create the goal profile for UPGRADING_HOUSE state
     */
    private GoalProfile createUpgradingHouseProfile() {
        return GoalProfile.create()
            .addGoal(0, new SwimGoal(entity))
            .addGoal(1, new PlaceBlocksGoal(entity))
            .addGoal(2, new BreakBlocksGoal(entity))
            .addGoal(3, new ClearAreaGoal(entity))
            .addGoal(4, new WanderGoal(entity, 0.8));
    }
    
    /**
     * Register a goal profile for a specific state
     */
    public void registerProfile(EntityState state, GoalProfile profile) {
        profiles.put(state, profile);
    }
    
    /**
     * Get the goal profile for a specific state
     */
    public GoalProfile getProfile(EntityState state) {
        return profiles.getOrDefault(state, GoalProfile.create());
    }
    
    /**
     * Apply the goal profile for the current entity state.
     * This clears all existing goals and adds the goals from the profile.
     */
    public void applyCurrentProfile() {
        applyProfile(entity.getState());
    }
    
    /**
     * Apply a specific state's goal profile to the entity.
     * This clears all existing goals and adds the goals from the profile.
     */
    public void applyProfile(EntityState state) {
        // Clear existing goals using the entity's method
        entity.clearGoals();
        
        // Get the profile for this state
        GoalProfile profile = getProfile(state);
        
        // Add all goals from the profile using the entity's method
        for (GoalProfile.GoalEntry entry : profile.getGoals()) {
            entity.addGoal(entry.getPriority(), entry.getGoal());
        }
    }
}
