package horror.blueice129.client.sound;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

import java.util.HashSet;
import java.util.Set;

public final class LilyRainSoundManager {

    private static final Set<BlockPos> ACTIVE_FLOWERS = new HashSet<>();
    private static final float BASE_VOLUME = 0.55f;
    private static final float PITCH = 0.5f;
    private static final double MAX_DISTANCE = 3.5;

    private static LilyRainSoundInstance activeSound;

    private LilyRainSoundManager() {
    }

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(LilyRainSoundManager::onClientTick);
    }

    public static void start(BlockPos pos) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return;
        }

        ACTIVE_FLOWERS.add(pos.toImmutable());
        ensureSound(client);
    }

    public static void stop(BlockPos pos) {
        MinecraftClient client = MinecraftClient.getInstance();
        ACTIVE_FLOWERS.remove(pos);
        if (ACTIVE_FLOWERS.isEmpty()) {
            stopActiveSound(client.getSoundManager());
        } else {
            ensureSound(client);
            updateSoundSource(client);
        }
    }

    private static void onClientTick(MinecraftClient client) {
        if (client.world == null) {
            clearAll(client.getSoundManager());
            return;
        }

        if (ACTIVE_FLOWERS.isEmpty()) {
            stopActiveSound(client.getSoundManager());
            return;
        }

        ACTIVE_FLOWERS.removeIf(pos -> !client.world.getBlockState(pos).isOf(Blocks.LILY_OF_THE_VALLEY));
        if (ACTIVE_FLOWERS.isEmpty()) {
            stopActiveSound(client.getSoundManager());
            return;
        }

        ensureSound(client);
        updateSoundSource(client);
    }

    private static void clearAll(SoundManager soundManager) {
        ACTIVE_FLOWERS.clear();
        stopActiveSound(soundManager);
    }

    private static void ensureSound(MinecraftClient client) {
        if (activeSound != null) {
            if (activeSound.isDone()) {
                activeSound = null;
            } else {
                return;
            }
        }

        if (client.world == null) {
            return;
        }

        BlockPos sourcePos = getNearestFlower(client);
        if (sourcePos == null) {
            return;
        }

        activeSound = new LilyRainSoundInstance(sourcePos);
        client.getSoundManager().play(activeSound);
    }

    private static void updateSoundSource(MinecraftClient client) {
        if (activeSound == null) {
            return;
        }

        BlockPos sourcePos = getNearestFlower(client);
        if (sourcePos == null) {
            stopActiveSound(client.getSoundManager());
            return;
        }

        activeSound.setSource(client, sourcePos);
    }

    private static BlockPos getNearestFlower(MinecraftClient client) {
        if (client.player == null || ACTIVE_FLOWERS.isEmpty()) {
            return null;
        }

        BlockPos playerPos = client.player.getBlockPos();
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (BlockPos flowerPos : ACTIVE_FLOWERS) {
            double distance = flowerPos.getSquaredDistance(playerPos);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = flowerPos;
            }
        }

        return nearest;
    }

    private static void stopActiveSound(SoundManager soundManager) {
        if (activeSound == null) {
            return;
        }

        if (soundManager != null) {
            soundManager.stop(activeSound);
        }

        activeSound = null;
    }

    private static final class LilyRainSoundInstance extends MovingSoundInstance {
        private BlockPos pos;

        private LilyRainSoundInstance(BlockPos pos) {
            super(SoundEvents.ITEM_ELYTRA_FLYING, SoundCategory.AMBIENT, Random.create());
            this.pos = pos.toImmutable();
            this.x = this.pos.getX() + 0.5;
            this.y = this.pos.getY() + 0.5;
            this.z = this.pos.getZ() + 0.5;
            this.volume = BASE_VOLUME;
            this.pitch = PITCH;
            this.repeat = true;
            this.repeatDelay = 0;
            this.attenuationType = AttenuationType.LINEAR;
            this.relative = false;
        }

        private void setSource(MinecraftClient client, BlockPos sourcePos) {
            this.pos = sourcePos.toImmutable();
            this.x = this.pos.getX() + 0.5;
            this.y = this.pos.getY() + 0.5;
            this.z = this.pos.getZ() + 0.5;
            if (client.player != null) {
                double dx = client.player.getX() - this.x;
                double dy = client.player.getY() - this.y;
                double dz = client.player.getZ() - this.z;
                double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                float factor = (float) Math.max(0.0, 1.0 - (distance / MAX_DISTANCE));
                this.volume = BASE_VOLUME * factor;
            } else {
                this.volume = 0.0f;
            }
            this.pitch = PITCH;
        }

        @Override
        public void tick() {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null || !client.world.getBlockState(pos).isOf(Blocks.LILY_OF_THE_VALLEY)) {
                setDone();
            }
        }
    }
}
