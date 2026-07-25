package horror.blueice129.entity.goals.interaction;

import horror.blueice129.HorrorMod129;
import horror.blueice129.entity.Blueice129Entity;
import horror.blueice129.entity.goals.BaseBlueice129Goal;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNode;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.EnumSet;

/**
 * Goal that makes the entity open doors
 */
public class OpenDoorsGoal extends BaseBlueice129Goal {

    protected BlockPos doorPos = BlockPos.ORIGIN;
    protected boolean doorValid;
    private boolean shouldStop;

    public OpenDoorsGoal(Blueice129Entity entity) {
        super(entity);
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    protected boolean isDoorOpen() {
        if (!this.doorValid) {
            return false;
        } else {
            BlockState blockState = this.entity.getWorld().getBlockState(this.doorPos);
            if (!(blockState.getBlock() instanceof DoorBlock)) {
                this.doorValid = false;
                return false;
            } else {
                return (Boolean)blockState.get(DoorBlock.OPEN);
            }
        }
    }

    protected void setDoorOpen(boolean open) {
        if (this.doorValid) {
            BlockState blockState = this.entity.getWorld().getBlockState(this.doorPos);
            if (blockState.getBlock() instanceof DoorBlock) {
                ((DoorBlock)blockState.getBlock()).setOpen(this.entity, this.entity.getWorld(), blockState, this.doorPos, open);
            }
        }
    }

    @Override
    public boolean shouldKeepRunning() {
        return !this.shouldStop;
    }
    
    @Override
    protected boolean shouldStart() {
            HorrorMod129.LOGGER.debug("Looking for door");
            Box box = entity.getBoundingBox().expand(entity.getAttributeValue(EntityAttributes.GENERIC_FOLLOW_RANGE));
            for (BlockPos pos : BlockPos.iterate((int) Math.round(box.minX), (int) Math.round(box.minY), (int) Math.round(box.minZ),
                    (int) Math.round(box.maxX), (int) Math.round(box.maxY), (int) Math.round(box.maxZ))) {
                this.doorPos = pos;
                this.doorValid = DoorBlock.canOpenByHand(this.entity.getWorld(), this.doorPos);
                if (this.doorValid && !isDoorOpen()) {
                    HorrorMod129.LOGGER.debug("Found valid door");
                    return true;
                }
                this.doorPos = this.doorPos.up();
                this.doorValid = DoorBlock.canOpenByHand(this.entity.getWorld(), this.doorPos);
                if (this.doorValid && !isDoorOpen()) {
                    HorrorMod129.LOGGER.debug("Found valid door");
                    return true;
                }
            }
        return false;
    }

    @Override
    protected void onStart() {
        HorrorMod129.LOGGER.debug("Started going to door at: "+doorPos.toString());
        this.shouldStop = false;
        entity.getNavigation().startMovingTo(doorPos.getX(), doorPos.getY(), doorPos.getZ(), 1.0);
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        // Stop once the entity has passed through the door plane.
        Box box = new Box(doorPos).expand(0.5, 1, 0.5);
        if (box.contains(entity.getPos()) || isDoorOpen()) {
            this.shouldStop = true;
        }
    }

    @Override
    public void onStop() {
        HorrorMod129.LOGGER.debug("Passed door at: "+doorPos.toString());
        this.setDoorOpen(true);
        entity.getNavigation().stop();
    }
}
