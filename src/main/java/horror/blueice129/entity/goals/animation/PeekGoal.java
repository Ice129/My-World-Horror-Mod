package horror.blueice129.entity.goals.animation;

import horror.blueice129.entity.Blueice129Entity;
import horror.blueice129.entity.goals.BaseBlueice129Goal;

/**
 * Goal that makes the entity peek out from hiding occasionally
 * TODO: Implement peeking behavior
 */
public class PeekGoal extends BaseBlueice129Goal {
    
    private int peekTimer;
    
    public PeekGoal(Blueice129Entity entity) {
        super(entity, 20);
        this.peekTimer = 0;
    }
    
    @Override
    protected boolean shouldStart() {
        return isInState(Blueice129Entity.EntityState.SURFACE_HIDING) && 
               entity.getRandom().nextInt(100) < 5;
    }
    
    @Override
    protected void onStart() {
        peekTimer = 20 + entity.getRandom().nextInt(40);
    }
    
    @Override
    protected boolean shouldKeepRunning() {
        return peekTimer > 0;
    }
    
    @Override
    public void tick() {
        // TODO: Implement peeking animation
        // Look towards player briefly, then look away
        peekTimer--;
    }
    
    @Override
    protected void onStop() {
        // Cleanup if needed
    }
}
