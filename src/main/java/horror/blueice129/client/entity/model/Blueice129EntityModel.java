package horror.blueice129.client.entity.model;

import horror.blueice129.entity.Blueice129Entity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.EntityModelPartNames;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/**
 * Model for the Blueice129 entity.
 * This model mimics the player model structure with head, body, arms, and legs.
 */
public class Blueice129EntityModel extends EntityModel<Blueice129Entity> {
    public static final EntityModelLayer LAYER = new EntityModelLayer(new Identifier("horror-mod-129", "blueice129"), "main");
    
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    
    // Secondary layer (outer layer) parts
    private final ModelPart hat;
    private final ModelPart jacket;
    private final ModelPart rightSleeve;
    private final ModelPart leftSleeve;
    private final ModelPart rightPants;
    private final ModelPart leftPants;

    public Blueice129EntityModel(ModelPart root) {
        this.head = root.getChild(EntityModelPartNames.HEAD);
        this.body = root.getChild(EntityModelPartNames.BODY);
        this.rightArm = root.getChild(EntityModelPartNames.RIGHT_ARM);
        this.leftArm = root.getChild(EntityModelPartNames.LEFT_ARM);
        this.rightLeg = root.getChild(EntityModelPartNames.RIGHT_LEG);
        this.leftLeg = root.getChild(EntityModelPartNames.LEFT_LEG);
        
        // Get secondary layer parts
        this.hat = root.getChild(EntityModelPartNames.HAT);
        this.jacket = root.getChild("jacket");
        this.rightSleeve = root.getChild("right_sleeve");
        this.leftSleeve = root.getChild("left_sleeve");
        this.rightPants = root.getChild("right_pants");
        this.leftPants = root.getChild("left_pants");
    }

    /**
     * Creates the textured model data for the Blueice129 entity.
     * This defines the structure of the entity model (player-like).
     */
    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();

        // Head - 8x8x8 cube at position (0, 24, 0) offset by -4 in each direction to center
        modelPartData.addChild(
            EntityModelPartNames.HEAD,
            ModelPartBuilder.create()
                .uv(0, 0)
                .cuboid(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F),
            ModelTransform.pivot(0.0F, 0.0F, 0.0F)
        );

        // Body - 8x12x4 cube
        modelPartData.addChild(
            EntityModelPartNames.BODY,
            ModelPartBuilder.create()
                .uv(16, 16)
                .cuboid(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F),
            ModelTransform.pivot(0.0F, 0.0F, 0.0F)
        );

        // Right Arm - 4x12x4 cube
        modelPartData.addChild(
            EntityModelPartNames.RIGHT_ARM,
            ModelPartBuilder.create()
                .uv(40, 16)
                .cuboid(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
            ModelTransform.pivot(-5.0F, 2.0F, 0.0F)
        );

        // Left Arm - 4x12x4 cube
        modelPartData.addChild(
            EntityModelPartNames.LEFT_ARM,
            ModelPartBuilder.create()
                .uv(40, 16)
                .mirrored()
                .cuboid(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
            ModelTransform.pivot(5.0F, 2.0F, 0.0F)
        );

        // Right Leg - 4x12x4 cube
        modelPartData.addChild(
            EntityModelPartNames.RIGHT_LEG,
            ModelPartBuilder.create()
                .uv(0, 16)
                .cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
            ModelTransform.pivot(-1.9F, 12.0F, 0.0F)
        );

        // Left Leg - 4x12x4 cube
        modelPartData.addChild(
            EntityModelPartNames.LEFT_LEG,
            ModelPartBuilder.create()
                .uv(0, 16)
                .mirrored()
                .cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
            ModelTransform.pivot(1.9F, 12.0F, 0.0F)
        );

        // Secondary layer (outer layer) parts - slightly larger than base layer
        // Hat - outer layer for head
        modelPartData.addChild(
            EntityModelPartNames.HAT,
            ModelPartBuilder.create()
                .uv(32, 0)
                .cuboid(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new Dilation(0.5F)),
            ModelTransform.pivot(0.0F, 0.0F, 0.0F)
        );

        // Jacket - outer layer for body
        modelPartData.addChild(
            "jacket",
            ModelPartBuilder.create()
                .uv(16, 32)
                .cuboid(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new Dilation(0.25F)),
            ModelTransform.pivot(0.0F, 0.0F, 0.0F)
        );

        // Right Sleeve - outer layer for right arm
        modelPartData.addChild(
            "right_sleeve",
            ModelPartBuilder.create()
                .uv(40, 32)
                .cuboid(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new Dilation(0.25F)),
            ModelTransform.pivot(-5.0F, 2.0F, 0.0F)
        );

        // Left Sleeve - outer layer for left arm
        modelPartData.addChild(
            "left_sleeve",
            ModelPartBuilder.create()
                .uv(48, 48)
                .cuboid(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new Dilation(0.25F)),
            ModelTransform.pivot(5.0F, 2.0F, 0.0F)
        );

        // Right Pants - outer layer for right leg
        modelPartData.addChild(
            "right_pants",
            ModelPartBuilder.create()
                .uv(0, 32)
                .cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new Dilation(0.25F)),
            ModelTransform.pivot(-1.9F, 12.0F, 0.0F)
        );

        // Left Pants - outer layer for left leg
        modelPartData.addChild(
            "left_pants",
            ModelPartBuilder.create()
                .uv(0, 48)
                .cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new Dilation(0.25F)),
            ModelTransform.pivot(1.9F, 12.0F, 0.0F)
        );

        return TexturedModelData.of(modelData, 64, 64);
    }

    /**
     * Animates the model based on the entity's state.
     * This includes walking animation, head rotation, etc.
     */
    @Override
    public void setAngles(Blueice129Entity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        // Head rotation
        this.head.yaw = headYaw * 0.017453292F;
        this.head.pitch = headPitch * 0.017453292F;
        
        // Hat follows head rotation
        this.hat.yaw = this.head.yaw;
        this.hat.pitch = this.head.pitch;

        // Walking animation for legs
        this.rightLeg.pitch = MathHelper.cos(limbAngle * 0.6662F) * 1.4F * limbDistance;
        this.leftLeg.pitch = MathHelper.cos(limbAngle * 0.6662F + (float)Math.PI) * 1.4F * limbDistance;
        
        // Pants follow leg rotation
        this.rightPants.pitch = this.rightLeg.pitch;
        this.leftPants.pitch = this.leftLeg.pitch;

        // Walking animation for arms (opposite to legs)
        this.rightArm.pitch = MathHelper.cos(limbAngle * 0.6662F + (float)Math.PI) * 1.4F * limbDistance;
        this.leftArm.pitch = MathHelper.cos(limbAngle * 0.6662F) * 1.4F * limbDistance;
        
        // Idle arm sway animation (like vanilla player)
        this.rightArm.pitch += MathHelper.sin(animationProgress * 0.067F) * 0.07F;
        this.leftArm.pitch -= MathHelper.sin(animationProgress * 0.067F) * 0.07F;
        
        // Add subtle z-axis rotation for more natural arm sway
        this.rightArm.roll = 0.0F;
        this.leftArm.roll = 0.0F;
        this.rightArm.yaw = 0.0F;
        this.leftArm.yaw = 0.0F;
        
        // Sleeves follow arm rotation
        this.rightSleeve.pitch = this.rightArm.pitch;
        this.leftSleeve.pitch = this.leftArm.pitch;
        this.rightSleeve.roll = this.rightArm.roll;
        this.leftSleeve.roll = this.leftArm.roll;
        this.rightSleeve.yaw = this.rightArm.yaw;
        this.leftSleeve.yaw = this.leftArm.yaw;
        
        // Jacket follows body (body doesn't rotate, so jacket stays static too)
    }

    /**
     * Renders the model.
     */
    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
        // Render base layer
        this.head.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        this.body.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        this.rightArm.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        this.leftArm.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        this.rightLeg.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        this.leftLeg.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        
        // Render secondary layer (outer layer)
        this.hat.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        this.jacket.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        this.rightSleeve.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        this.leftSleeve.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        this.rightPants.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        this.leftPants.render(matrices, vertices, light, overlay, red, green, blue, alpha);
    }
}
