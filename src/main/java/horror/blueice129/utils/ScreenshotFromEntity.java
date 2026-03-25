package horror.blueice129.utils;

import horror.blueice129.mixin.client.GameRendererAccessor;
import horror.blueice129.mixin.client.MinecraftClientAccessor;
import horror.blueice129.mixin.client.PlayerEntityAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public class ScreenshotFromEntity {

    private static Entity pendingTarget = null;
    private static Entity activeTarget = null;
    private static Framebuffer offscreenFramebuffer = null;
    private static Entity originalCamera = null;
    private static Perspective originalPerspective = Perspective.FIRST_PERSON;
    private static boolean captureInProgress = false;

    public static void initialize() {
        WorldRenderEvents.AFTER_ENTITIES.register(ScreenshotFromEntity::onAfterEntities);
    }

    public static void scheduleScreenshot(Entity target) {
        if (target == null || pendingTarget != null || captureInProgress) return;
        pendingTarget = target;
        // horror.blueice129.HorrorMod129.LOGGER.info("ScreenshotFromEntity: Scheduled screenshot from entity " + target.getId() + " (" + target.getName().getString() + ")");
    }

    public static void captureOffscreen(GameRenderer gameRenderer, float tickDelta, long renderTime) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (pendingTarget == null || client.player == null || client.world == null || captureInProgress) return;
        if (!pendingTarget.isAlive() || pendingTarget.getWorld() != client.world) {
            horror.blueice129.HorrorMod129.LOGGER.warn("ScreenshotFromEntity: Pending target is not alive or in different world");
            pendingTarget = null;
            return;
        }

        // horror.blueice129.HorrorMod129.LOGGER.info("ScreenshotFromEntity: Starting offscreen capture from entity " + pendingTarget.getId());

        ensureOffscreenFramebuffer(client);

        activeTarget = pendingTarget;
        pendingTarget = null;
        captureInProgress = true;

        Framebuffer mainFramebuffer = client.getFramebuffer();
        originalCamera = client.cameraEntity;
        originalPerspective = client.options.getPerspective();
        boolean originalRenderHand = ((GameRendererAccessor) gameRenderer).horrorMod129$getRenderHand();

        try {
            client.options.setPerspective(Perspective.FIRST_PERSON);
            client.setCameraEntity(activeTarget);
            ((MinecraftClientAccessor) client).horrorMod129$setFramebuffer(offscreenFramebuffer);
            gameRenderer.setRenderHand(false);

            offscreenFramebuffer.beginWrite(true);
            gameRenderer.renderWorld(tickDelta, renderTime, new MatrixStack());
            ScreenshotRecorder.saveScreenshot(client.runDirectory, offscreenFramebuffer, msg -> {
                horror.blueice129.HorrorMod129.LOGGER.info("ScreenshotFromEntity: Screenshot saved");
                if (client.player != null) {
                    client.player.sendMessage(msg, false);
                }
            });
        } finally {
            offscreenFramebuffer.endWrite();
            ((MinecraftClientAccessor) client).horrorMod129$setFramebuffer(mainFramebuffer);
            gameRenderer.setRenderHand(originalRenderHand);
            mainFramebuffer.beginWrite(true);

            Entity restore = originalCamera != null ? originalCamera : client.player;
            if (restore != null) {
                client.setCameraEntity(restore);
            }

            client.options.setPerspective(originalPerspective);
            restoreMainCamera(gameRenderer, client, restore, tickDelta);
            originalCamera = null;
            activeTarget = null;
            captureInProgress = false;
        }
    }

    private static void restoreMainCamera(GameRenderer gameRenderer, MinecraftClient client, Entity cameraEntity, float tickDelta) {
        if (client.world == null || cameraEntity == null) return;

        boolean thirdPerson = originalPerspective != Perspective.FIRST_PERSON;
        boolean inverseView = originalPerspective == Perspective.THIRD_PERSON_FRONT;
        Camera camera = gameRenderer.getCamera();
        camera.update(client.world, cameraEntity, thirdPerson, inverseView, tickDelta);
        client.getSoundManager().updateListenerPosition(camera);
    }

    private static void ensureOffscreenFramebuffer(MinecraftClient client) {
        int width = client.getWindow().getFramebufferWidth();
        int height = client.getWindow().getFramebufferHeight();

        if (offscreenFramebuffer == null) {
            offscreenFramebuffer = new SimpleFramebuffer(width, height, true, MinecraftClient.IS_SYSTEM_MAC);
            return;
        }

        if (offscreenFramebuffer.textureWidth != width || offscreenFramebuffer.textureHeight != height) {
            offscreenFramebuffer.resize(width, height, MinecraftClient.IS_SYSTEM_MAC);
        }
    }

    private static void onAfterEntities(WorldRenderContext context) {
        if (!captureInProgress) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || activeTarget == null) return;

        renderLocalPlayerForCapture(client, context);
    }

    private static void renderLocalPlayerForCapture(MinecraftClient client, WorldRenderContext context) {
        if (client.player == null) return;

        Camera camera = context.camera();
        if (camera == null) return;

        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) return;

        float tickDelta = context.tickDelta();

        // TODO: make this work with multiple players in multiplayer

        Vec3d cameraPos = camera.getPos();
        // fake player since the real player won't be rendered in first person
        OtherClientPlayerEntity fakePlayer = new OtherClientPlayerEntity(client.world, client.player.getGameProfile());
        fakePlayer.copyPositionAndRotation(client.player);
        fakePlayer.prevX = client.player.prevX;
        fakePlayer.prevY = client.player.prevY;
        fakePlayer.prevZ = client.player.prevZ;
        fakePlayer.prevYaw = client.player.prevYaw;
        fakePlayer.prevPitch = client.player.prevPitch;
        fakePlayer.prevBodyYaw = client.player.prevBodyYaw;
        fakePlayer.bodyYaw = client.player.bodyYaw;
        fakePlayer.prevHeadYaw = client.player.prevHeadYaw;
        fakePlayer.headYaw = client.player.headYaw;
        fakePlayer.age = client.player.age;
        fakePlayer.setSneaking(client.player.isSneaking());
        fakePlayer.setSprinting(client.player.isSprinting());
        fakePlayer.setPose(client.player.getPose());
        copyEquippedItems(client, fakePlayer);

        Vec3d playerPos = fakePlayer.getLerpedPos(tickDelta);

        int light = client.getEntityRenderDispatcher().getLight(fakePlayer, tickDelta);
        MatrixStack matrices = context.matrixStack();
        matrices.push();
        client.getEntityRenderDispatcher().render(
                fakePlayer,
                playerPos.x - cameraPos.x,
                playerPos.y - cameraPos.y,
                playerPos.z - cameraPos.z,
                fakePlayer.getYaw(tickDelta),
                tickDelta,
                matrices,
                consumers,
                light);
        matrices.pop();

        if (consumers instanceof VertexConsumerProvider.Immediate immediateConsumers) {
            immediateConsumers.draw();
        }
    }

    private static void copyEquippedItems(MinecraftClient client, OtherClientPlayerEntity fakePlayer) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            fakePlayer.equipStack(slot, client.player.getEquippedStack(slot).copy());
        }

        var playerModelParts = PlayerEntityAccessor.horrorMod129$getPlayerModelParts();
        byte visibleParts = client.player.getDataTracker().get(playerModelParts);
        fakePlayer.getDataTracker().set(playerModelParts, visibleParts);
    }
}

