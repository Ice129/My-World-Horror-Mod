package horror.blueice129.entity.goals.basic;

import horror.blueice129.entity.Blueice129Entity;
import horror.blueice129.entity.goals.BaseBlueice129Goal;
import net.minecraft.entity.ai.goal.Goal;
import java.util.EnumSet;

/**
 * Goal that makes the entity look around randomly
 */
public class LookAroundGoal extends BaseBlueice129Goal {
    private double targetX;
    private double targetY;
    private double targetZ;
    private int lookTicksRemaining = 0;
    
    public LookAroundGoal(Blueice129Entity entity) {
        super(entity);
        this.setControls(EnumSet.of(Goal.Control.LOOK));
    }
    
    @Override
    protected boolean shouldStart() {
        return entity.getRandom().nextFloat() < 0.015F;
    }
    
    @Override
    public void tick() {
        // If we have an active target, keep looking at it each tick until the timer expires
        if (this.lookTicksRemaining > 0) {
            entity.getLookControl().lookAt(this.targetX, this.targetY, this.targetZ);
            this.lookTicksRemaining--;
            return;
        }

        // Otherwise, only pick a new target once per second
        int currentTick = entity.age;
        if (currentTick % 20 != 0) {
            return; // wait until the next second to pick a new angle
        }

        double yaw = entity.getRandom().nextDouble() * 2.0 * Math.PI;
        double pitch = (entity.getRandom().nextDouble() - 0.5) * Math.PI * 0.5;

        double x = entity.getX() + Math.cos(yaw) * 10.0;
        double y = entity.getEyeY() + Math.sin(pitch) * 10.0;
        double z = entity.getZ() + Math.sin(yaw) * 10.0;

        this.targetX = x;
        this.targetY = y / 2.0; // y is halved to reduce vertical looking
        this.targetZ = z;
        this.lookTicksRemaining = 20; // maintain this look for 20 ticks (1 second)
        entity.getLookControl().lookAt(this.targetX, this.targetY, this.targetZ);
    }
}
