package zip.kiran.snapshot.client.render;

import zip.kiran.snapshot.client.mixin.PostEffectProcessorAccessor;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectPass;
import net.minecraft.client.gl.PostEffectProcessor;

public final class PostChains {
    private PostChains() {
    }

    public static void setUniform(PostEffectProcessor processor, String name, float value) {
        for (PostEffectPass pass : ((PostEffectProcessorAccessor) processor).kiranzip$getPasses()) {
            pass.getProgram().getUniformByNameOrDummy(name).set(value);
        }
    }

    public static void setFilter(PostEffectProcessor processor, Framebuffer main, int filter, String... targets) {
        main.setTexFilter(filter);
        for (String name : targets) {
            Framebuffer target = processor.getSecondaryTarget(name);
            if (target != null) {
                target.setTexFilter(filter);
            }
        }
    }
}
