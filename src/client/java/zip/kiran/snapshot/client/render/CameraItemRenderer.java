package zip.kiran.snapshot.client.render;

import zip.kiran.snapshot.CameraMod;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class CameraItemRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer {
    public static final Identifier BODY = new Identifier(CameraMod.MOD_ID, "item/camera_body");

    @Override
    public void render(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        MinecraftClient client = MinecraftClient.getInstance();
        BakedModelManager models = client.getBakedModelManager();
        BakedModel body = models.getModel(BODY);
        if (body == null) {
            return;
        }

        boolean leftHanded = mode == ModelTransformationMode.FIRST_PERSON_LEFT_HAND
                || mode == ModelTransformationMode.THIRD_PERSON_LEFT_HAND;

        matrices.push();
        matrices.translate(0.5F, 0.5F, 0.5F);
        client.getItemRenderer().renderItem(stack, mode, leftHanded, matrices, vertexConsumers, light, overlay, body);
        matrices.pop();
    }
}
