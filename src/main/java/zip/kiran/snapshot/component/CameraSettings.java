package zip.kiran.snapshot.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.MathHelper;

public record CameraSettings(float aperture, float focalLength, float focusDistance, int iso) {
    public static final float[] APERTURES = {1.4F, 2.0F, 2.8F, 4.0F, 5.6F, 8.0F, 11.0F, 16.0F, 22.0F};
    public static final float[] FOCAL_LENGTHS = {8.0F, 14.0F, 18.0F, 24.0F, 28.0F, 35.0F, 50.0F, 85.0F, 135.0F, 200.0F, 300.0F, 600.0F};
    public static final int[] ISOS = {50, 100, 200, 400, 800, 1600, 3200, 6400};

    public static final float MIN_FOCUS = 0.4F;
    public static final float MAX_FOCUS = 256.0F;

    public static final CameraSettings DEFAULT = new CameraSettings(2.8F, 24.0F, 8.0F, 100);

    public static final Codec<CameraSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("aperture", DEFAULT.aperture()).forGetter(CameraSettings::aperture),
            Codec.FLOAT.optionalFieldOf("focal_length", DEFAULT.focalLength()).forGetter(CameraSettings::focalLength),
            Codec.FLOAT.optionalFieldOf("focus_distance", DEFAULT.focusDistance()).forGetter(CameraSettings::focusDistance),
            Codec.INT.optionalFieldOf("iso", DEFAULT.iso()).forGetter(CameraSettings::iso)
    ).apply(instance, CameraSettings::new));

    public static CameraSettings read(PacketByteBuf buf) {
        return new CameraSettings(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readVarInt());
    }

    public void write(PacketByteBuf buf) {
        buf.writeFloat(aperture);
        buf.writeFloat(focalLength);
        buf.writeFloat(focusDistance);
        buf.writeVarInt(iso);
    }

    public static CameraSettings fromNbt(NbtCompound tag) {
        try {
            float aperture = tag.contains("aperture") ? tag.getFloat("aperture") : DEFAULT.aperture();
            float focalLength = tag.contains("focal_length") ? tag.getFloat("focal_length") : DEFAULT.focalLength();
            float focusDistance = tag.contains("focus_distance") ? tag.getFloat("focus_distance") : DEFAULT.focusDistance();
            int iso = tag.contains("iso") ? tag.getInt("iso") : DEFAULT.iso();
            return new CameraSettings(aperture, focalLength, focusDistance, iso);
        } catch (RuntimeException e) {
            return DEFAULT;
        }
    }

    public CameraSettings sanitised() {
        return new CameraSettings(
                nearestStop(APERTURES, aperture),
                nearestStop(FOCAL_LENGTHS, focalLength),
                MathHelper.clamp(Float.isFinite(focusDistance) ? focusDistance : DEFAULT.focusDistance(), MIN_FOCUS, MAX_FOCUS),
                nearestStop(ISOS, iso));
    }

    public CameraSettings withAperture(float value) {
        return new CameraSettings(value, focalLength, focusDistance, iso);
    }

    public CameraSettings withFocalLength(float value) {
        return new CameraSettings(aperture, value, focusDistance, iso);
    }

    public CameraSettings withFocusDistance(float value) {
        return new CameraSettings(aperture, focalLength, MathHelper.clamp(value, MIN_FOCUS, MAX_FOCUS), iso);
    }

    public CameraSettings withIso(int value) {
        return new CameraSettings(aperture, focalLength, focusDistance, value);
    }

    private static int clampIndex(int index, int direction, int length) {
        return MathHelper.clamp(index + direction, 0, length - 1);
    }

    public static float step(float[] table, float current, int direction) {
        int index = nearestIndex(table, current);
        return table[clampIndex(index, direction, table.length)];
    }

    public static int step(int[] table, int current, int direction) {
        int index = nearestIndex(table, current);
        return table[clampIndex(index, direction, table.length)];
    }

    public static float stepFocus(float current, int direction) {
        float scaled = (float) Math.exp(Math.log(MathHelper.clamp(current, MIN_FOCUS, MAX_FOCUS)) + direction * 0.12F);
        return MathHelper.clamp(scaled, MIN_FOCUS, MAX_FOCUS);
    }

    private static int findBestIndex(double[] table, double value) {
        int best = 0;
        double bestDelta = Double.MAX_VALUE;
        for (int i = 0; i < table.length; i++) {
            double delta = Math.abs(table[i] - value);
            if (delta < bestDelta) {
                bestDelta = delta;
                best = i;
            }
        }
        return best;
    }

    public static int nearestIndex(float[] table, float value) {
        double[] d = new double[table.length];
        for (int i = 0; i < table.length; i++) d[i] = table[i];
        return findBestIndex(d, value);
    }

    public static int nearestIndex(int[] table, int value) {
        double[] d = new double[table.length];
        for (int i = 0; i < table.length; i++) d[i] = table[i];
        return findBestIndex(d, value);
    }

    private static float nearestStop(float[] table, float value) {
        return Float.isFinite(value) ? table[nearestIndex(table, value)] : table[0];
    }

    private static int nearestStop(int[] table, int value) {
        return table[nearestIndex(table, value)];
    }

    public String formatAperture() {
        return aperture < 10.0F ? String.format("f/%.1f", aperture) : String.format("f/%.0f", aperture);
    }

    public String formatFocalLength() {
        return String.format("%.0fmm", focalLength);
    }

    public String formatFocusDistance() {
        return focusDistance >= 200.0F ? "\u221e" : String.format("%.1fm", focusDistance);
    }

    public String formatIso() {
        return "ISO " + iso;
    }
}
