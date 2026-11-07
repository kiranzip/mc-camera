package zip.kiran.snapshot.photo;

import zip.kiran.snapshot.CameraMod;
import zip.kiran.snapshot.CameraConstants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PhotoFile {
    private PhotoFile() {
    }

    public static void write(Path path, byte[] rgb) throws IOException {
        Files.createDirectories(path.getParent());
        Path temp = path.resolveSibling(path.getFileName().toString() + ".tmp");
        try {
            Files.write(temp, rgb);
            Files.move(temp, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    public static byte[] read(Path path) {
        try {
            if (!Files.isRegularFile(path)) {
                return null;
            }
            byte[] data = Files.readAllBytes(path);
            if (data.length != CameraConstants.PHOTO_BYTES) {
                CameraMod.LOGGER.warn("Photograph file {} has invalid size", path);
                return null;
            }
            return data;
        } catch (java.io.IOException e) {
            CameraMod.LOGGER.error("Could not read photograph file {}", path, e);
            return null;
        } catch (RuntimeException e) {
            CameraMod.LOGGER.warn("Could not read photograph file {}", path, e);
            return null;
        }
    }
}
