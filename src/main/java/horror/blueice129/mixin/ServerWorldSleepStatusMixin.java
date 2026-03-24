package horror.blueice129.mixin;

import horror.blueice129.feature.TwoPlayerSleep;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.SleepManager;
import net.minecraft.text.Text;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerWorld.class)
public abstract class ServerWorldSleepStatusMixin {
    @Shadow
    @Final
    private SleepManager sleepManager;

    @Inject(method = "sendSleepingStatus", at = @At("HEAD"), cancellable = true)
    private void horror$sendSleepStatusWithFakePlayer(CallbackInfo ci) {
        if (TwoPlayerSleep.getFakeSleeperMode() == TwoPlayerSleep.FakeSleeperMode.LOGGED_OUT) {
            return;
        }

        int percentage = ((ServerWorld) (Object) this).getGameRules().getInt(GameRules.PLAYERS_SLEEPING_PERCENTAGE);
        int sleeping = this.sleepManager.getSleeping();
        int required = this.sleepManager.getNightSkippingRequirement(percentage);

        Text message = this.sleepManager.canSkipNight(percentage)
                ? Text.translatable("sleep.skipping_night")
                : Text.translatable("sleep.players_sleeping", sleeping, required);

        for (ServerPlayerEntity player : ((ServerWorld) (Object) this).getPlayers()) {
            player.sendMessageToClient(message, true);
        }

        ci.cancel();
    }
}
