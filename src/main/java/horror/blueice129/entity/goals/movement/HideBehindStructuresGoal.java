package horror.blueice129.entity.goals.movement;

import horror.blueice129.entity.Blueice129Entity;
import horror.blueice129.entity.goals.BaseBlueice129Goal;
import net.minecraft.entity.ai.goal.Goal;
import java.util.EnumSet;

/**
 * Goal that makes the entity hide behind structures on the surface
 * TODO: Implement structure hiding logic with pathfinding
 */
public class HideBehindStructuresGoal extends BaseBlueice129Goal {
    
    public HideBehindStructuresGoal(Blueice129Entity entity) {
        super(entity, 20);
        this.setControls(EnumSet.of(Goal.Control.MOVE));
    }
    
    @Override
    protected boolean shouldStart() {
        return isInState(Blueice129Entity.EntityState.SURFACE_HIDING);
    }
    
    @Override
    public void tick() {
        // TODO: Find nearest structure (tree, building, etc.)
        // TODO: Pathfind to position behind structure relative to player
        // TODO: Stay crouched while moving
    }
}
