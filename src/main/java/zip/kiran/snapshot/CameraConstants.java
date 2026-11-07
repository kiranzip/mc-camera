package zip.kiran.snapshot;

public final class CameraConstants {
    private CameraConstants() {
    }

    public static final int PHOTO_SIZE = 128;

    public static final int PHOTO_BYTES = PHOTO_SIZE * PHOTO_SIZE * 3;

    public static final int CHUNK_BYTES = 16384;

    public static final int MAX_CHUNK_BYTES = 24576;

    public static final int MAX_CHUNKS = 64;

    public static final long SHUTTER_COOLDOWN_MS = 1200L;

    public static final long SERVE_COOLDOWN_MS = 100L;

    public static final long SETTINGS_COOLDOWN_MS = 50L;

    public static final long UPLOAD_TIMEOUT_MS = 20000L;

    public static final float DEVELOP_SECONDS = 30.0F;

    public static final int EJECT_DELAY_TICKS = 12;
}
