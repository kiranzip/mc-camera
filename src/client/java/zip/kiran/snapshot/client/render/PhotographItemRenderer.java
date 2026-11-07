package zip.kiran.snapshot.client.render;

import zip.kiran.snapshot.CameraMod;
import zip.kiran.snapshot.client.photo.ClientPhotoCache;
import zip.kiran.snapshot.component.Components;
import zip.kiran.snapshot.component.PhotoRef;
import zip.kiran.snapshot.config.RuntimeConfig;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class PhotographItemRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer {
    private static final Identifier FRAME = new Identifier(CameraMod.MOD_ID, "textures/item/photograph_frame.png");
    private static final Identifier BLANK = new Identifier(CameraMod.MOD_ID, "textures/item/photograph_blank.png");

    private static final float CARD_X = 0.41F;
    private static final float CARD_Y = 0.50F;
    private static final float THICKNESS = 0.006F;

    private static final float IMAGE_LEFT = -0.34F;
    private static final float IMAGE_RIGHT = 0.34F;
    private static final float IMAGE_TOP = 0.43F;
    private static final float IMAGE_BOTTOM = -0.25F;

    @Override
    public void render(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        PhotoRef ref = PhotoRef.fromNbt(Components.PHOTO.raw(stack));
        Identifier photo = ref == null ? null : ClientPhotoCache.texture(ref.photoId());
        float developed = ref == null ? 1.0F : ref.developProgress(System.currentTimeMillis());

        matrices.push();
        matrices.translate(0.5F, 0.5F, 0.5F);
        drawSide(matrices, vertexConsumers, light, overlay, photo, developed, true);
        drawSide(matrices, vertexConsumers, light, overlay, photo, developed, false);
        matrices.pop();
    }

    private void drawSide(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay,
                          Identifier photo, float developed, boolean front) {
        MatrixStack.Entry entry = matrices.peek();
        float z = front ? THICKNESS : -THICKNESS;

        quad(vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(FRAME)), entry,
                -CARD_X, -CARD_Y, CARD_X, CARD_Y, z, front, light, overlay, 1.0F);

        if (photo != null) {
            float imageZ = front ? z + 0.001F : z - 0.001F;
            quad(vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(photo)), entry,
                    IMAGE_LEFT, IMAGE_BOTTOM, IMAGE_RIGHT, IMAGE_TOP, imageZ, front, light, overlay, 1.0F);

            if (RuntimeConfig.polaroidDevelopEffect && developed < 1.0F) {
                float milkZ = front ? z + 0.002F : z - 0.002F;
                quad(vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(BLANK)), entry,
                        IMAGE_LEFT, IMAGE_BOTTOM, IMAGE_RIGHT, IMAGE_TOP, milkZ, front, light, overlay, 1.0F - developed);
            }
        }
    }

    private static void quad(VertexConsumer consumer, MatrixStack.Entry entry,
                             float x0, float y0, float x1, float y1, float z,
                             boolean front, int light, int overlay, float alpha) {
        float normal = front ? 1.0F : -1.0F;
        if (front) {
            vertex(consumer, entry, x0, y0, z, 0.0F, 1.0F, light, overlay, alpha, normal);
            vertex(consumer, entry, x1, y0, z, 1.0F, 1.0F, light, overlay, alpha, normal);
            vertex(consumer, entry, x1, y1, z, 1.0F, 0.0F, light, overlay, alpha, normal);
            vertex(consumer, entry, x0, y1, z, 0.0F, 0.0F, light, overlay, alpha, normal);
        } else {
            vertex(consumer, entry, x1, y0, z, 0.0F, 1.0F, light, overlay, alpha, normal);
            vertex(consumer, entry, x0, y0, z, 1.0F, 1.0F, light, overlay, alpha, normal);
            vertex(consumer, entry, x0, y1, z, 1.0F, 0.0F, light, overlay, alpha, normal);
            vertex(consumer, entry, x1, y1, z, 0.0F, 0.0F, light, overlay, alpha, normal);
        }
    }

    private static void vertex(VertexConsumer consumer, MatrixStack.Entry entry, float x, float y, float z,
                               float u, float v, int light, int overlay, float alpha, float normalZ) {
        consumer.vertex(entry.getPositionMatrix(), x, y, z)
                .color(1.0F, 1.0F, 1.0F, alpha)
                .texture(u, v)
                .overlay(overlay)
                .light(light)
                .normal(entry.getNormalMatrix(), 0.0F, 0.0F, normalZ)
                .next();
    }
}
