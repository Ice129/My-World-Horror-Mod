package horror.blueice129.entity.goals;

import horror.blueice129.entity.Blueice129Entity;
import net.minecraft.entity.ai.goal.Goal;

/**
 * Base class for all Blueice129-specific goals.
 * Provides common functionality and access to the entity.
 * 
 * All custom goals should extend this class to ensure consistency
 * and access to entity-specific methods.
 */
public abstract class BaseBlueice129Goal extends Goal {
    
    protected final Blueice129Entity entity;
    protected final int checkInterval;
    protected int tickCounter;
    
    /**
     * Create a new base goal with default check interval of 1 (every tick)
     * @param entity The Blueice129Entity this goal belongs to
     */
    public BaseBlueice129Goal(Blueice129Entity entity) {
        this(entity, 1);
    }
    
    /**
     * Create a new base goal with custom check interval
     * @param entity The Blueice129Entity this goal belongs to
     * @param checkInterval How often to check conditions (in ticks)
     */
    public BaseBlueice129Goal(Blueice129Entity entity, int checkInterval) {
        this.entity = entity;
        this.checkInterval = checkInterval;
        this.tickCounter = 0;
    }
    
    /**
     * Check if this goal should begin execution.
     * Subclasses should override shouldStart() instead.
     */
    @Override
    public final boolean canStart() {
        // Only check conditions at the specified interval
        if (++tickCounter < checkInterval) {
            return false;
        }
        tickCounter = 0;
        return shouldStart();
    }
    
    /**
     * Check if this goal should continue execution.
     * Subclasses should override shouldContinue() instead.
     */
    @Override
    public final boolean shouldContinue() {
        return shouldKeepRunning();
    }
    
    /**
     * Called when the goal starts.
     * Subclasses can override this for initialization logic.
     */
    @Override
    public void start() {
        onStart();
    }
    
    /**
     * Called when the goal stops.
     * Subclasses can override this for cleanup logic.
     */
    @Override
    public void stop() {
        onStop();
    }
    
    /**
     * Called every tick while the goal is active.
     * Subclasses must implement this.
     */
    @Override
    public abstract void tick();
    
    /**
     * Determine if this goal should start.
     * Subclasses must implement this.
     */
    protected abstract boolean shouldStart();
    
    /**
     * Determine if this goal should continue running.
     * Default implementation returns true (run until manually stopped).
     */
    protected boolean shouldKeepRunning() {
        return true;
    }
    
    /**
     * Called when the goal starts.
     * Override this for initialization logic.
     */
    protected void onStart() {
        // Override in subclasses
    }
    
    /**
     * Called when the goal stops.
     * Override this for cleanup logic.
     */
    protected void onStop() {
        // Override in subclasses
    }
    
    /**
     * Check if the entity is in a specific state
     */
    protected boolean isInState(Blueice129Entity.EntityState state) {
        return entity.getState() == state;
    }
    
    /**
     * Get the entity's current state
     */
    protected Blueice129Entity.EntityState getCurrentState() {
        return entity.getState();
    }
}
