package horror.blueice129.mixin;

import horror.blueice129.entity.Blueice129Entity;
import net.minecraft.entity.ai.control.BodyControl;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BodyControl.class)
public abstract class BodyControlMixin {
    @Shadow
    @Final
    private MobEntity entity;

    @Shadow
    protected abstract void slowlyAdjustBody();

    @Redirect(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ai/control/BodyControl;slowlyAdjustBody()V")
    )
    private void redirectSlowBodyAdjustment(BodyControl instance) {
        if (!(entity instanceof Blueice129Entity)) {
            slowlyAdjustBody();
        }
    }
}
