package horror.blueice129.entity.goals.basic;

import horror.blueice129.entity.Blueice129Entity;
import horror.blueice129.entity.goals.BaseBlueice129Goal;
import net.minecraft.entity.ai.goal.Goal;
import java.util.EnumSet;

/**
 * Basic swimming goal - keeps the entity afloat in water
 */
public class SwimGoal extends BaseBlueice129Goal {
    
    public SwimGoal(Blueice129Entity entity) {
        super(entity);
        this.setControls(EnumSet.of(Goal.Control.JUMP));
    }
    
    @Override
    protected boolean shouldStart() {
        return entity.isTouchingWater() || entity.isInLava();
    }
    
    @Override
    protected boolean shouldKeepRunning() {
        return shouldStart();
    }
    
    @Override
    public void tick() {
        if (entity.getRandom().nextFloat() < 0.8F) {
            entity.getJumpControl().setActive();
        }
    }
}
