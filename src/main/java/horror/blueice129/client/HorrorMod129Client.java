package horror.blueice129.client;

import net.fabricmc.api.ClientModInitializer;
import horror.blueice129.HorrorMod129;
import horror.blueice129.network.ClientPacketHandler;
import horror.blueice129.scheduler.MinMusicSetter;
import horror.blueice129.client.entity.model.Blueice129EntityModel;
import horror.blueice129.client.entity.renderer.Blueice129EntityRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/**
 * Client entry point for the horror mod.
 * Handles client-side initialization.
 */
public class HorrorMod129Client implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        HorrorMod129.LOGGER.info("Initializing HorrorMod129 client");
        
        // Register client packet receivers
        ClientPacketHandler.registerClientReceivers();
        
        // Register client-side features
        MinMusicSetter.initialize();

        // entity rendering needed to do on client side, as its visual and the server side logic are separate
        
        // Register entity renderer
        EntityRendererRegistry.register(HorrorMod129.BLUEICE129_ENTITY, Blueice129EntityRenderer::new);
        
        // Register entity model layer using built-in player model
        // false = standard Steve arms (4px wide), true = Alex arms (3px wide)
        EntityModelLayerRegistry.registerModelLayer(Blueice129EntityModel.LAYER, 
            () -> net.minecraft.client.model.TexturedModelData.of(
                net.minecraft.client.render.entity.model.PlayerEntityModel.getTexturedModelData(
                    new net.minecraft.client.model.Dilation(0.0f), false), 64, 64));
        
        HorrorMod129.LOGGER.info("HorrorMod129 client initialization complete");
    }
}