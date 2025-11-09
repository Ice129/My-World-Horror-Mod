package horror.blueice129.entity.goals.basic;

import horror.blueice129.entity.Blueice129Entity;
import horror.blueice129.entity.goals.BaseBlueice129Goal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.Vec3d;
import java.util.EnumSet;

/**
 * Goal that makes the entity wander around randomly
 */
public class WanderGoal extends BaseBlueice129Goal {
    
    private final double speed;
    
    public WanderGoal(Blueice129Entity entity, double speed) {
        super(entity, 120);
        this.speed = speed;
        this.setControls(EnumSet.of(Goal.Control.MOVE));
    }
    
    @Override
    protected boolean shouldStart() {
        return entity.getNavigation().isIdle() && entity.getRandom().nextInt(10) == 0;
    }
    
    @Override
    protected boolean shouldKeepRunning() {
        return !entity.getNavigation().isIdle();
    }
    
    @Override
    public void tick() {
        // Find random position and path to it
        Vec3d targetPos = findRandomPosition();
        if (targetPos != null) {
            entity.getNavigation().startMovingTo(targetPos.x, targetPos.y, targetPos.z, speed);
        }
    }
    
    private Vec3d findRandomPosition() {
        // Simple random position within 10 blocks
        double x = entity.getX() + (entity.getRandom().nextDouble() - 0.5) * 20.0;
        double y = entity.getY() + (entity.getRandom().nextDouble() - 0.5) * 6.0;
        double z = entity.getZ() + (entity.getRandom().nextDouble() - 0.5) * 20.0;
        
        return new Vec3d(x, y, z);
    }
}
