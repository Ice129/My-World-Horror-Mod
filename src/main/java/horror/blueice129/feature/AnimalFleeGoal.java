package horror.blueice129.feature;

import java.util.EnumSet;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public class AnimalFleeGoal extends Goal {
    private static final double FLEE_DISTANCE = 12.0D;

    private final AnimalEntity animal;
    private final PlayerEntity targetPlayer;
    private final double speed;

    public AnimalFleeGoal(AnimalEntity animal, PlayerEntity targetPlayer) {
        this(animal, targetPlayer, 1.4D);
    }

    public AnimalFleeGoal(AnimalEntity animal, PlayerEntity targetPlayer, double speed) {
        this.animal = animal;
        this.targetPlayer = targetPlayer;
        this.speed = speed;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        return animal.isAlive() && targetPlayer.isAlive();
    }

    @Override
    public boolean shouldContinue() {
        return animal.isAlive() && targetPlayer.isAlive();
    }

    @Override
    public void tick() {
        Vec3d away = animal.getPos().subtract(targetPlayer.getPos());

        if (away.lengthSquared() < 0.0001D) {
            away = new Vec3d(animal.getRandom().nextDouble() - 0.5D, 0.0D, animal.getRandom().nextDouble() - 0.5D);
        }

        Vec3d direction = away.normalize();
        Vec3d targetPos = animal.getPos().add(direction.multiply(FLEE_DISTANCE));

        animal.getNavigation().startMovingTo(targetPos.x, targetPos.y, targetPos.z, speed);
        animal.getLookControl().lookAt(targetPlayer, 30.0F, 30.0F);
    }

    @Override
    public void stop() {
        animal.getNavigation().stop();
    }
}