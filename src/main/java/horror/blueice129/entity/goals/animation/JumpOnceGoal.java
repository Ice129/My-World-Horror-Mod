package horror.blueice129.entity.goals.animation;

import horror.blueice129.entity.Blueice129Entity;
import horror.blueice129.entity.goals.BaseBlueice129Goal;
import net.minecraft.entity.ai.goal.Goal;
import java.util.EnumSet;

/**
 * Goal that makes the entity perform a single jump once when entering the
 * PANICED state. The goal will only trigger once per profile application.
 */
public class JumpOnceGoal extends BaseBlueice129Goal {
    private boolean jumped;

    public JumpOnceGoal(Blueice129Entity entity) {
        super(entity);
        this.setControls(EnumSet.of(Goal.Control.JUMP));
        this.jumped = false;
    }

    @Override
    protected boolean shouldStart() {
        // Start only if in PANICED and haven't jumped yet and are on ground
        return isInState(Blueice129Entity.EntityState.PANICED) && !jumped && entity.isOnGround();
    }

    @Override
    protected boolean shouldKeepRunning() {
        // We only need to run long enough to perform the jump once
        return false;
    }

    @Override
    protected void onStart() {
        // Trigger a jump immediately when the goal starts
        entity.getJumpControl().setActive();
        jumped = true;
    }

    @Override
    public void tick() {
        // No continuous behavior required
    }
}
