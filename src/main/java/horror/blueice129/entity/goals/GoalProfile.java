package horror.blueice129.entity.goals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a set of goals with their priorities for a specific entity state.
 * Goals are stored as entries with their priority and can be added to an entity's goal selector.
 */
public class GoalProfile {
    
    private final List<GoalEntry> goals;
    
    public GoalProfile() {
        this.goals = new ArrayList<>();
    }
    
    /**
     * Add a goal to this profile with its priority
     * @param priority Lower values = higher priority (0 is highest)
     * @param goal The goal to add
     * @return This profile for chaining
     */
    public GoalProfile addGoal(int priority, BaseBlueice129Goal goal) {
        goals.add(new GoalEntry(priority, goal));
        return this;
    }
    
    /**
     * Get all goal entries in this profile as an unmodifiable list
     */
    public List<GoalEntry> getGoals() {
        return Collections.unmodifiableList(goals);
    }
    
    /**
     * Create a new empty goal profile
     */
    public static GoalProfile create() {
        return new GoalProfile();
    }
    
    /**
     * Represents a goal with its priority
     */
    public static class GoalEntry {
        private final int priority;
        private final BaseBlueice129Goal goal;
        
        public GoalEntry(int priority, BaseBlueice129Goal goal) {
            this.priority = priority;
            this.goal = goal;
        }
        
        public int getPriority() {
            return priority;
        }
        
        public BaseBlueice129Goal getGoal() {
            return goal;
        }
    }
}
