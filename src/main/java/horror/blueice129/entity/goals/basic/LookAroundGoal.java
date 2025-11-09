package horror.blueice129.entity.goals.basic;

import horror.blueice129.entity.Blueice129Entity;
import horror.blueice129.entity.goals.BaseBlueice129Goal;
import net.minecraft.entity.ai.goal.Goal;
import java.util.EnumSet;

/**
 * Goal that makes the entity look around randomly
 */
public class LookAroundGoal extends BaseBlueice129Goal {
    
    public LookAroundGoal(Blueice129Entity entity) {
        super(entity, 20);
        this.setControls(EnumSet.of(Goal.Control.LOOK));
    }
    
    @Override
    protected boolean shouldStart() {
        return entity.getRandom().nextFloat() < 0.02F;
    }
    
    @Override
    public void tick() {
        double yaw = entity.getRandom().nextDouble() * 2.0 * Math.PI;
        double pitch = (entity.getRandom().nextDouble() - 0.5) * Math.PI * 0.5;
        
        double x = entity.getX() + Math.cos(yaw) * 10.0;
        double y = entity.getEyeY() + Math.sin(pitch) * 10.0;
        double z = entity.getZ() + Math.sin(yaw) * 10.0;
        
        entity.getLookControl().lookAt(x, y, z);
    }
}
