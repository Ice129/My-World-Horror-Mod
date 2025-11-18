package horror.blueice129.entity.goals.basic;

import horror.blueice129.entity.Blueice129Entity;
import horror.blueice129.entity.goals.BaseBlueice129Goal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import java.util.EnumSet;

/**
 * Goal that makes the entity look at nearby players
 */
public class LookAtPlayerGoal extends BaseBlueice129Goal {
    
    private final float range;
    private PlayerEntity targetPlayer;
    private int lookTime;
    
    public LookAtPlayerGoal(Blueice129Entity entity, float range) {
        super(entity);
        this.range = range;
        this.setControls(EnumSet.of(Goal.Control.LOOK));
    }
    
    @Override
    protected boolean shouldStart() {
        if (entity.getRandom().nextFloat() >= 0.02F) {
            return false;
        }
        
        targetPlayer = entity.getWorld().getClosestPlayer(entity, range);
        return targetPlayer != null;
    }
    
    @Override
    protected boolean shouldKeepRunning() {
        if (!targetPlayer.isAlive()) {
            return false;
        }
        if (entity.squaredDistanceTo(targetPlayer) > (range * range)) {
            return false;
        }
        return lookTime > 0;
    }
    
    @Override
    protected void onStart() {
        lookTime = 40 + entity.getRandom().nextInt(40);
    }
    
    @Override
    protected void onStop() {
        targetPlayer = null;
    }
    
    @Override
    public void tick() {
        entity.getLookControl().lookAt(
            targetPlayer.getX(),
            targetPlayer.getEyeY(),
            targetPlayer.getZ()
        );
        lookTime--;
    }
}
