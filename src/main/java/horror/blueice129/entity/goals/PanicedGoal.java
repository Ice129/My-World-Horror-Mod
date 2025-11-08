package horror.blueice129.entity.goals;

import net.minecraft.entity.ai.goal.Goal;

public class PanicedGoal extends Goal {

    @Override
    public boolean canStart() {
        return false;
    }

    @Override
    public void start() {
    }

    @Override
    public boolean shouldContinue() {
        return false;
    }

    @Override
    public void stop() {
    }

    @Override
    public void tick() {
    }


}
