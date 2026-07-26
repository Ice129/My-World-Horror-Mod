package horror.blueice129.mixin;

import horror.blueice129.entity.Blueice129Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ZombieEntity.class, AbstractSkeletonEntity.class})
public abstract class HostileEntitiesMixin extends HostileEntity {
    protected HostileEntitiesMixin(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "initGoals", at = @At("TAIL"))
    private void injectCustomGoals(CallbackInfo ci) {
        this.targetSelector.add(2, new ActiveTargetGoal(this, Blueice129Entity.class, true));
    }
}
