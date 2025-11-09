package horror.blueice129.entity.goals.interaction;

import horror.blueice129.entity.Blueice129Entity;
import horror.blueice129.entity.goals.BaseBlueice129Goal;
import net.minecraft.entity.ai.goal.Goal;
import java.util.EnumSet;

/**
 * Goal that makes the entity mimic the player's mining actions
 * TODO: Implement mining mimicry
 */
public class MimicPlayerMiningGoal extends BaseBlueice129Goal {
    
    public MimicPlayerMiningGoal(Blueice129Entity entity) {
        super(entity, 5);
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }
    
    @Override
    protected boolean shouldStart() {
        return isInState(Blueice129Entity.EntityState.UNDERGROUND_BURROWING);
    }
    
    @Override
    public void tick() {
        // TODO: Detect when player is mining nearby
        // TODO: Mine similar blocks in proximity
        // TODO: Use similar tools from inventory
    }
}
