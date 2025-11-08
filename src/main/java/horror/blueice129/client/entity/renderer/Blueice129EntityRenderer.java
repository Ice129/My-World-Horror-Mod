package horror.blueice129.client.entity.renderer;

import horror.blueice129.entity.Blueice129Entity;
import horror.blueice129.client.entity.model.Blueice129EntityModel;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

/**
 * Renderer for the Blueice129 entity.
 * Provides the model and texture for rendering the entity in the world.
 */
public class Blueice129EntityRenderer extends MobEntityRenderer<Blueice129Entity, Blueice129EntityModel> {

    /**
     * Constructor for the Blueice129EntityRenderer.
     * 
     * @param context The entity renderer factory context
     */
    public Blueice129EntityRenderer(EntityRendererFactory.Context context) {
        super(context, new Blueice129EntityModel(context.getPart(Blueice129EntityModel.LAYER)), 0.5f);
    }

    /**
     * Returns the texture identifier for the entity.
     * This should point to the Blueice129 player skin.
     * 
     * @param entity The entity to get the texture for
     * @return The identifier pointing to the texture file
     */
    @Override
    public Identifier getTexture(Blueice129Entity entity) {
        return new Identifier("horror-mod-129", "textures/entity/blueice129/blueice129.png");
    }
}
