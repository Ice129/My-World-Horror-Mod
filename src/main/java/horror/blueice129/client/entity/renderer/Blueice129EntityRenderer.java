package horror.blueice129.client.entity.renderer;

import horror.blueice129.HorrorMod129;
import horror.blueice129.entity.Blueice129Entity;
import horror.blueice129.client.entity.model.Blueice129EntityModel;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.model.ArmorEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.util.Identifier;

/**
 * Renderer for the Blueice129 entity.
 * Provides the model and texture for rendering the entity in the world.
 */
public class Blueice129EntityRenderer extends BipedEntityRenderer<Blueice129Entity, Blueice129EntityModel> {

    /**
     * Constructor for the Blueice129EntityRenderer.
     * 
     * @param context The entity renderer factory context
     */
    public Blueice129EntityRenderer(EntityRendererFactory.Context context) {
        // false = standard Steve arms (4px wide), true = Alex arms (3px wide)
        super(context, new Blueice129EntityModel(context.getPart(Blueice129EntityModel.LAYER), false), 0.5f);
        this.addFeature(
                new ArmorFeatureRenderer<>(
                        this, new ArmorEntityModel<>(context.getPart(EntityModelLayers.ARMOR_STAND_INNER_ARMOR)), new ArmorEntityModel<>(context.getPart(EntityModelLayers.ARMOR_STAND_OUTER_ARMOR)), context.getModelManager()
                )
        );
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
        return new Identifier(HorrorMod129.MOD_ID, "textures/entity/blueice129/blueice129.png");
    }
}
