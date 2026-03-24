package horror.blueice129.mixin;

import horror.blueice129.feature.TwoPlayerSleep;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.SleepManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(SleepManager.class)
public abstract class SleepManagerMixin {
    @Shadow
    private int total;

    @Shadow
    private int sleeping;

    private static int horror$getRequiredSleeping(int totalPlayers, int percentage) {
        return Math.max(1, (int) Math.ceil(totalPlayers * (percentage / 100.0D)));
    }

    @Inject(method = "canSkipNight", at = @At("HEAD"), cancellable = true)
    private void horror$canSkipNight(int percentage, CallbackInfoReturnable<Boolean> cir) {
        int extraOnline = TwoPlayerSleep.getExtraOnlinePlayers();
        int extraSleeping = TwoPlayerSleep.getExtraSleepingPlayers();
        if (extraOnline == 0 && extraSleeping == 0) {
            return;
        }

        int adjustedTotal = this.total + extraOnline;
        int adjustedSleeping = Math.min(adjustedTotal, this.sleeping + extraSleeping);
        int requiredSleeping = horror$getRequiredSleeping(adjustedTotal, percentage);

        cir.setReturnValue(adjustedSleeping >= requiredSleeping);
    }

    @Inject(method = "canResetTime", at = @At("HEAD"), cancellable = true)
    private void horror$canResetTime(int percentage, List<ServerPlayerEntity> players, CallbackInfoReturnable<Boolean> cir) {
        int extraOnline = TwoPlayerSleep.getExtraOnlinePlayers();
        int extraSleeping = TwoPlayerSleep.getExtraSleepingPlayers();
        if (extraOnline == 0 && extraSleeping == 0) {
            return;
        }

        int adjustedTotal = this.total + extraOnline;
        int requiredSleeping = horror$getRequiredSleeping(adjustedTotal, percentage);

        int realLongEnoughSleepers = 0;
        for (ServerPlayerEntity player : players) {
            if (player.canResetTimeBySleeping()) {
                realLongEnoughSleepers++;
            }
        }

        int adjustedLongEnoughSleepers = realLongEnoughSleepers + extraSleeping;
        cir.setReturnValue(adjustedLongEnoughSleepers >= requiredSleeping);
    }

    @Inject(method = "getNightSkippingRequirement", at = @At("HEAD"), cancellable = true)
    private void horror$getNightSkippingRequirement(int percentage, CallbackInfoReturnable<Integer> cir) {
        int extraOnline = TwoPlayerSleep.getExtraOnlinePlayers();
        if (extraOnline == 0) {
            return;
        }

        int adjustedTotal = this.total + extraOnline;
        cir.setReturnValue(horror$getRequiredSleeping(adjustedTotal, percentage));
    }

    @Inject(method = "getSleeping", at = @At("HEAD"), cancellable = true)
    private void horror$getSleeping(CallbackInfoReturnable<Integer> cir) {
        int extraOnline = TwoPlayerSleep.getExtraOnlinePlayers();
        int extraSleeping = TwoPlayerSleep.getExtraSleepingPlayers();
        if (extraOnline == 0 && extraSleeping == 0) {
            return;
        }

        int adjustedTotal = this.total + extraOnline;
        int adjustedSleeping = Math.min(adjustedTotal, this.sleeping + extraSleeping);
        cir.setReturnValue(adjustedSleeping);
    }
}
