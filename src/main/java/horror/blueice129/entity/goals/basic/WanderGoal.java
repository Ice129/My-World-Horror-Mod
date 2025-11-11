package horror.blueice129.entity.goals.basic;

import horror.blueice129.entity.Blueice129Entity;
import horror.blueice129.entity.goals.BaseBlueice129Goal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;

/**
 * Goal that makes the entity wander around randomly using vanilla WanderAroundFarGoal
 */
public class WanderGoal extends BaseBlueice129Goal {
    
    private final WanderAroundFarGoal vanillaGoal;
    
    public WanderGoal(Blueice129Entity entity, double speed) {
        super(entity, 120);
        this.vanillaGoal = new WanderAroundFarGoal(entity, speed);
    }
    
    @Override
    protected boolean shouldStart() {
        return vanillaGoal.canStart();
    }
    
    @Override
    protected boolean shouldKeepRunning() {
        return vanillaGoal.shouldContinue();
    }
    
    @Override
    protected void onStart() {
        vanillaGoal.start();
    }
    
    @Override
    public void tick() {
        vanillaGoal.tick();
    }
    
    @Override
    protected void onStop() {
        vanillaGoal.stop();
    }
}
