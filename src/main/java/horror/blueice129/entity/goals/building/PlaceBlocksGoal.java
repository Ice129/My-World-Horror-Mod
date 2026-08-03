package horror.blueice129.entity.goals.building;

import horror.blueice129.entity.Blueice129Entity;
import horror.blueice129.entity.Blueice129Entity.BlockPlacementTask;
import horror.blueice129.entity.goals.BaseBlueice129Goal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.block.BlockState;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;

/**
 * Goal that makes the entity place blocks from inventory
 * TODO: Implement block placement logic
 */
public class PlaceBlocksGoal extends BaseBlueice129Goal {

    private BlockPlacementTask activeTask;
    private int equipWaitTicks;
    private int placeWaitTicks = -1;

    public PlaceBlocksGoal(Blueice129Entity entity) {
        super(entity);
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    @Override
    protected boolean shouldStart() {
        return isInState(Blueice129Entity.EntityState.UPGRADING_HOUSE) && entity.hasQueuedBlockPlacements();
    }

    @Override
    protected boolean shouldKeepRunning() {
        return isInState(Blueice129Entity.EntityState.UPGRADING_HOUSE)
                && (this.activeTask != null || entity.hasQueuedBlockPlacements());
    }

    @Override
    protected void onStart() {
        this.activeTask = null;
        this.equipWaitTicks = 0;
        this.placeWaitTicks = -1;
    }

    @Override
    protected void onStop() {
        this.activeTask = null;
        this.equipWaitTicks = 0;
        this.placeWaitTicks = -1;
        entity.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (!isInState(Blueice129Entity.EntityState.UPGRADING_HOUSE)) {
            return;
        }

        if (this.activeTask == null) {
            this.activeTask = entity.peekNextBlockPlacementTask();
            this.equipWaitTicks = 0;
            this.placeWaitTicks = -1;
        }

        if (this.activeTask == null) {
            return;
        }

        BlockPos targetPos = this.activeTask.position();
        BlockState targetState = this.activeTask.blockState();
        Vec3d targetCenter = Vec3d.ofCenter(targetPos);

        if (entity.squaredDistanceTo(targetCenter) > 9.0D || !entity.hasLineOfSightTo(targetPos)) {
            entity.getNavigation().startMovingTo(targetCenter.x, targetCenter.y, targetCenter.z, 1.0D);
            this.equipWaitTicks = 0;
            this.placeWaitTicks = -1;
            return;
        }

        entity.getNavigation().stop();
        entity.getLookControl().lookAt(targetCenter.x, targetCenter.y, targetCenter.z);

        if (this.equipWaitTicks > 0) {
            this.equipWaitTicks--;
            return;
        }

        if (!entity.isHoldingBlockItem(targetState)) {
            if (!entity.equipBlockItemForPlacement(targetState)) {
                this.placeWaitTicks = -1;
                return;
            }

            this.equipWaitTicks = 5;
            this.placeWaitTicks = -1;
            return;
        }

        if (this.placeWaitTicks < 0) {
            this.placeWaitTicks = entity.getRandom().nextInt(5) + 3;
            return;
        }

        if (this.placeWaitTicks > 0) {
            this.placeWaitTicks--;
            return;
        }

        if (!entity.canPlaceBlockAt(this.activeTask)) {
            entity.pollNextBlockPlacementTask();
            this.activeTask = null;
            this.placeWaitTicks = -1;
            return;
        }

        entity.swingHand(Hand.MAIN_HAND);
        if (entity.getWorld().setBlockState(targetPos, targetState, 3)) {
            entity.playBlockPlacementSound(targetState, targetPos);
            entity.consumeMainHandItem();
        }

        entity.pollNextBlockPlacementTask();
        this.activeTask = null;
        this.equipWaitTicks = 0;
        this.placeWaitTicks = -1;
    }
}
