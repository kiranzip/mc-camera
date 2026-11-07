package zip.kiran.snapshot.client.capture;

import zip.kiran.snapshot.CameraMod;
import zip.kiran.snapshot.CameraConstants;
import zip.kiran.snapshot.config.RuntimeConfig;
import zip.kiran.snapshot.net.payload.PhotoChunkC2S;
import zip.kiran.snapshot.net.payload.PhotoUploadBeginC2S;
import zip.kiran.snapshot.photo.PhotoCompression;
import zip.kiran.snapshot.component.CameraSettings;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.util.Util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

public final class PhotoCapture {
    private PhotoCapture() {
    }

    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");
    private static final AtomicInteger TRANSFERS = new AtomicInteger();

    private static volatile boolean pending;

    public static void request() {
        pending = true;
    }

    public static void onWorldFrameEnd(MinecraftClient client, CameraSettings settings) {
        if (!pending) {
            return;
        }
        pending = false;

        NativeImage square;
        try (NativeImage full = ScreenshotRecorder.takeScreenshot(client.getFramebuffer())) {
            square = cropCentreSquare(full);
        } catch (Exception e) {
            CameraMod.LOGGER.error("Could not read the framebuffer for a photograph", e);
            return;
        }

        Util.getIoWorkerExecutor().execute(() -> develop(client, square, settings));
    }

    private static void develop(MinecraftClient client, NativeImage square, CameraSettings settings) {
        try {
            FilmLook.apply(square, settings.iso(), System.nanoTime());
            byte[] rgb = downsample(square, RuntimeConfig.photoSize);
            saveOriginal(client, square);
            compressAndUpload(client, rgb, settings);
        } catch (Exception e) {
            CameraMod.LOGGER.error("Could not develop a photograph", e);
        } finally {
            square.close();
        }
    }

    private static void compressAndUpload(MinecraftClient client, byte[] rgb, CameraSettings settings) {
        byte[] compressed = PhotoCompression.deflate(rgb);
        byte[][] chunks = PhotoCompression.chunk(compressed);
        if (chunks.length > CameraConstants.MAX_CHUNKS) {
            CameraMod.LOGGER.error("Photograph did not compress small enough to send ({} bytes)", compressed.length);
            return;
        }

        int transferId = TRANSFERS.incrementAndGet();
        client.execute(() -> {
            if (client.getNetworkHandler() == null) {
                return;
            }
            ClientPlayNetworking.send(new PhotoUploadBeginC2S(transferId, chunks.length, compressed.length, settings));
            for (int i = 0; i < chunks.length; i++) {
                ClientPlayNetworking.send(new PhotoChunkC2S(transferId, i, chunks[i]));
            }
        });
    }

    private static NativeImage cropCentreSquare(NativeImage full) {
        int size = Math.min(full.getWidth(), full.getHeight());
        int offsetX = (full.getWidth() - size) / 2;
        int offsetY = (full.getHeight() - size) / 2;

        NativeImage square = new NativeImage(NativeImage.Format.RGBA, size, size, false);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                square.setColor(x, y, full.getColor(offsetX + x, offsetY + y));
            }
        }
        return square;
    }

    private static byte[] downsample(NativeImage source, int size) {
        int sourceSize = source.getWidth();
        byte[] out = new byte[size * size * 3];
        int index = 0;

        for (int outY = 0; outY < size; outY++) {
            int y0 = outY * sourceSize / size;
            int y1 = Math.max(y0 + 1, (outY + 1) * sourceSize / size);
            for (int outX = 0; outX < size; outX++) {
                int x0 = outX * sourceSize / size;
                int x1 = Math.max(x0 + 1, (outX + 1) * sourceSize / size);

                long r = 0;
                long g = 0;
                long b = 0;
                int samples = 0;
                for (int y = y0; y < y1; y++) {
                    for (int x = x0; x < x1; x++) {
                        int packed = source.getColor(x, y);
                        r += packed & 0xFF;
                        g += (packed >> 8) & 0xFF;
                        b += (packed >> 16) & 0xFF;
                        samples++;
                    }
                }
                out[index++] = (byte) (r / samples);
                out[index++] = (byte) (g / samples);
                out[index++] = (byte) (b / samples);
            }
        }
        return out;
    }

    private static void saveOriginal(MinecraftClient client, NativeImage square) {
        try {
            Path directory = client.runDirectory.toPath().resolve("photographs");
            Files.createDirectories(directory);
            Path file = directory.resolve(LocalDateTime.now().format(FILE_STAMP) + ".png");
            int suffix = 1;
            while (Files.exists(file)) {
                file = directory.resolve(LocalDateTime.now().format(FILE_STAMP) + "_" + suffix++ + ".png");
            }
            square.writeTo(file);
        } catch (Exception e) {
            CameraMod.LOGGER.warn("Could not save the full-resolution original", e);
        }
    }
}
