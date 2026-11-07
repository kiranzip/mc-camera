package zip.kiran.snapshot.client.capture;

import net.minecraft.client.texture.NativeImage;
import zip.kiran.snapshot.config.RuntimeConfig;

public final class FilmLook {
    private FilmLook() {
    }

    public static void apply(NativeImage image, int iso, long seed) {
        int width = image.getWidth();
        int height = image.getHeight();
        float halfW = width / 2.0F;
        float halfH = height / 2.0F;
        float grainAmount = Math.min(1.0F, Math.max(0.0F, (iso - 100) / 6300.0F)) * 26.0F;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int packed = image.getColor(x, y);
                float r = packed & 0xFF;
                float g = (packed >> 8) & 0xFF;
                float b = (packed >> 16) & 0xFF;
                int a = (packed >>> 24) & 0xFF;

                if (RuntimeConfig.filmColorProfile) {
                    r = r * 0.90F + 15.0F;
                    g = g * 0.90F + 13.0F;
                    b = b * 0.90F + 17.0F;
                    r *= 1.05F;
                    b *= 0.95F;

                    float luma = 0.299F * r + 0.587F * g + 0.114F * b;
                    r += (luma - r) * 0.10F;
                    g += (luma - g) * 0.10F;
                    b += (luma - b) * 0.10F;
                }

                if (RuntimeConfig.filmVignette) {
                    float dx = (x - halfW) / halfW;
                    float dy = (y - halfH) / halfH;
                    float falloff = 1.0F - 0.30F * Math.min(1.0F, dx * dx + dy * dy);
                    r *= falloff;
                    g *= falloff;
                    b *= falloff;
                }

                if (grainAmount > 0.0F) {
                    float noise = hash(x, y, seed) - 0.5F;
                    float grain = noise * grainAmount;
                    r += grain;
                    g += grain;
                    b += grain;
                }

                image.setColor(x, y, (a << 24) | (clamp(b) << 16) | (clamp(g) << 8) | clamp(r));
            }
        }
    }

    private static int clamp(float value) {
        int i = Math.round(value);
        return i < 0 ? 0 : Math.min(i, 255);
    }

    private static float hash(int x, int y, long seed) {
        long h = x * 374761393L + y * 668265263L + seed * 2246822519L;
        h = (h ^ (h >> 13)) * 1274126177L;
        h = h ^ (h >> 16);
        return (h & 0xFFFF) / 65535.0F;
    }
}
