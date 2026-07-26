package horror.blueice129.client.entity.model;

import horror.blueice129.HorrorMod129;
import horror.blueice129.entity.Blueice129Entity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

public class Blueice129EntityModel extends PlayerEntityModel<Blueice129Entity> {
    public static final EntityModelLayer LAYER = new EntityModelLayer(new Identifier(HorrorMod129.MOD_ID, Blueice129Entity.NAME.toLowerCase()), "main");

    public Blueice129EntityModel(ModelPart root, boolean thinArms) {
        super(root, thinArms);
    }
}