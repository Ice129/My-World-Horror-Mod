package horror.blueice129.client;

import net.fabricmc.api.ClientModInitializer;
import horror.blueice129.HorrorMod129;
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

        // entity rendering needed to do on client side, as its visual and the server side logic are separate
        
        // Register entity renderer
        EntityRendererRegistry.register(HorrorMod129.BLUEICE129_ENTITY, Blueice129EntityRenderer::new);
        
        // Register entity model layer
        EntityModelLayerRegistry.registerModelLayer(Blueice129EntityModel.LAYER, Blueice129EntityModel::getTexturedModelData);
        
        HorrorMod129.LOGGER.info("HorrorMod129 client initialization complete");
    }
}