package zip.kiran.snapshot.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import zip.kiran.snapshot.CameraMod;
import zip.kiran.snapshot.client.ViewfinderState;
import zip.kiran.snapshot.client.render.PostChains;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public final class DofPostProcessor {
    private static final Identifier CHAIN = new Identifier(CameraMod.MOD_ID, "shaders/post/camera_dof.json");
    private static final String[] BLUR_TARGETS = {"dof_horizontal", "dof_vertical"};
    private static final float[] PLANES = new float[2];

    private PostEffectProcessor processor;
    private int width = -1;
    private int height = -1;
    private boolean failed;

    public void render(MinecraftClient client, ViewfinderState state) {
        if (failed) {
            return;
        }
        Framebuffer target = client.getFramebuffer();
        try {
            if (processor == null) {
                processor = new PostEffectProcessor(client.getTextureManager(), client.getResourceManager(), target, CHAIN);
                width = -1;
                height = -1;
            }
            if (target.textureWidth != width || target.textureHeight != height) {
                width = target.textureWidth;
                height = target.textureHeight;
                processor.setupDimensions(width, height);
            }

            float[] planes = extractNearFar();
            float pixelScale = Math.max(0.35F, height / 1080.0F);

            PostChains.setUniform(processor, "NearPlane", planes[0]);
            PostChains.setUniform(processor, "FarPlane", planes[1]);
            PostChains.setUniform(processor, "FocusDistance", state.settings().focusDistance());
            PostChains.setUniform(processor, "FocalLength", state.settings().focalLength());
            PostChains.setUniform(processor, "Aperture", state.settings().aperture());
            PostChains.setUniform(processor, "MaxBlur", 36.0F * pixelScale);
            PostChains.setUniform(processor, "Exposure", state.exposure());
            PostChains.setUniform(processor, "GrainAmount", state.grainAmount());
            PostChains.setUniform(processor, "TimeSeed", (System.nanoTime() % 1000000000L) / 1.0E9F);

            PostChains.setFilter(processor, target, GL11.GL_LINEAR, BLUR_TARGETS);
            try {
                processor.render(0.05F);
            } finally {
                PostChains.setFilter(processor, target, GL11.GL_NEAREST, BLUR_TARGETS);
            }
        } catch (Exception e) {
            CameraMod.LOGGER.error("Depth-of-field chain failed; disabling viewfinder blur", e);
            failed = true;
            close();
        } finally {
            target.beginWrite(false);
        }
    }

    private static float[] extractNearFar() {
        try {
            Matrix4f projection = RenderSystem.getProjectionMatrix();
            if (projection != null) {
                float m22 = projection.m22();
                float m32 = projection.m32();
                float near = m32 / (m22 - 1.0F);
                float far = m32 / (m22 + 1.0F);
                if (Float.isFinite(near) && Float.isFinite(far) && near > 0.0F && far > near) {
                    PLANES[0] = near;
                    PLANES[1] = far;
                    return PLANES;
                }
            }
        } catch (Exception ignored) {
        }
        PLANES[0] = 0.05F;
        PLANES[1] = 1024.0F;
        return PLANES;
    }

    public void close() {
        if (processor != null) {
            try {
                processor.close();
            } catch (Exception ignored) {
            }
            processor = null;
        }
        width = -1;
        height = -1;
    }

    public void invalidate() {
        close();
        failed = false;
    }
}
