package zip.kiran.snapshot.client;

import zip.kiran.snapshot.component.CameraSettings;
import net.minecraft.util.math.MathHelper;

public final class ViewfinderState {
    public enum Dial {
        FOCUS("hud.kiranzip.dial.focus"),
        APERTURE("hud.kiranzip.dial.aperture"),
        FOCAL_LENGTH("hud.kiranzip.dial.focal_length"),
        ISO("hud.kiranzip.dial.iso");

        public final String translationKey;

        Dial(String translationKey) {
            this.translationKey = translationKey;
        }

        public Dial next() {
            Dial[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    private CameraSettings settings = CameraSettings.DEFAULT;
    private Dial dial = Dial.FOCUS;
    private boolean active;
    private String apertureStr;
    private String focalStr;
    private String focusStr;
    private String isoStr;

    private static final double SENSOR_HEIGHT_MM = 24.0D;

    private double smoothedFov = -1.0D;
    private boolean transitioning;
    private long lastFrameNanos;

    private long lastAdjustMillis;

    public CameraSettings settings() {
        return settings;
    }

    public String apertureString() { return apertureStr; }
    public String focalString() { return focalStr; }
    public String focusString() { return focusStr; }
    public String isoString() { return isoStr; }

    public void setSettings(CameraSettings settings) {
        this.settings = settings;
        updateCachedStrings();
    }

    private void updateCachedStrings() {
        apertureStr = settings.formatAperture();
        focalStr = settings.formatFocalLength();
        focusStr = settings.formatFocusDistance();
        isoStr = settings.formatIso();
    }

    public Dial dial() {
        return dial;
    }

    public void cycleDial() {
        dial = dial.next();
        touch();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        if (active != this.active) {
            transitioning = true;
        }
        this.active = active;
    }

    public long lastAdjustMillis() {
        return lastAdjustMillis;
    }

    public void touch() {
        lastAdjustMillis = System.currentTimeMillis();
    }

    public boolean adjust(int direction) {
        CameraSettings before = settings;
        settings = switch (dial) {
            case APERTURE -> settings.withAperture(CameraSettings.step(CameraSettings.APERTURES, settings.aperture(), direction));
            case FOCAL_LENGTH -> settings.withFocalLength(CameraSettings.step(CameraSettings.FOCAL_LENGTHS, settings.focalLength(), direction));
            case ISO -> settings.withIso(CameraSettings.step(CameraSettings.ISOS, settings.iso(), direction));
            case FOCUS -> settings.withFocusDistance(CameraSettings.stepFocus(settings.focusDistance(), direction));
        };
        if (!settings.equals(before)) {
            updateCachedStrings();
            touch();
            return true;
        }
        return false;
    }

    public String dialValue() {
        return switch (dial) {
            case APERTURE -> settings.formatAperture();
            case FOCAL_LENGTH -> settings.formatFocalLength();
            case FOCUS -> settings.formatFocusDistance();
            case ISO -> settings.formatIso();
        };
    }

    public int dialStops() {
        return switch (dial) {
            case APERTURE -> CameraSettings.APERTURES.length;
            case FOCAL_LENGTH -> CameraSettings.FOCAL_LENGTHS.length;
            case ISO -> CameraSettings.ISOS.length;
            case FOCUS -> 0;
        };
    }

    public float dialFraction() {
        return switch (dial) {
            case APERTURE -> stopFraction(CameraSettings.nearestIndex(CameraSettings.APERTURES, settings.aperture()),
                    CameraSettings.APERTURES.length);
            case FOCAL_LENGTH -> stopFraction(CameraSettings.nearestIndex(CameraSettings.FOCAL_LENGTHS, settings.focalLength()),
                    CameraSettings.FOCAL_LENGTHS.length);
            case ISO -> stopFraction(CameraSettings.nearestIndex(CameraSettings.ISOS, settings.iso()),
                    CameraSettings.ISOS.length);
            case FOCUS -> {
                float minLog = (float) Math.log(CameraSettings.MIN_FOCUS);
                float maxLog = (float) Math.log(CameraSettings.MAX_FOCUS);
                float here = (float) Math.log(MathHelper.clamp(settings.focusDistance(),
                        CameraSettings.MIN_FOCUS, CameraSettings.MAX_FOCUS));
                yield (here - minLog) / (maxLog - minLog);
            }
        };
    }

    private static float stopFraction(int index, int count) {
        return count <= 1 ? 0.0F : (float) index / (float) (count - 1);
    }

    public void setFocusDistance(float distance) {
        settings = settings.withFocusDistance(distance);
        touch();
    }

    public double fov(double vanillaFov) {
        if (!active && !transitioning) {
            smoothedFov = vanillaFov;
            lastFrameNanos = 0L;
            return vanillaFov;
        }

        double target = active ? cameraFov() : vanillaFov;

        long now = System.nanoTime();
        float delta = lastFrameNanos == 0L ? 0.016F : (now - lastFrameNanos) / 1.0E9F;
        lastFrameNanos = now;
        delta = MathHelper.clamp(delta, 0.0F, 0.25F);

        if (!Double.isFinite(smoothedFov) || smoothedFov <= 0.0D || target <= 0.0D) {
            smoothedFov = target;
        } else {
            double rate = 1.0D - Math.exp(-delta * 12.0D);
            smoothedFov *= Math.pow(target / smoothedFov, rate);
            if (!Double.isFinite(smoothedFov) || Math.abs(smoothedFov - target) < 0.05D) {
                smoothedFov = target;
            }
        }

        if (!active && smoothedFov == target) {
            transitioning = false;
        }

        return smoothedFov;
    }

    private double cameraFov() {
        return 2.0D * Math.toDegrees(Math.atan((SENSOR_HEIGHT_MM * 0.5D) / Math.max(settings.focalLength(), 1.0F)));
    }

    public float exposure() {
        return (float) Math.pow(2.0, (Math.log(settings.iso() / 200.0) / Math.log(2.0)) * 0.32);
    }

    public float grainAmount() {
        return MathHelper.clamp((settings.iso() - 100) / 6300.0F, 0.0F, 1.0F) * 0.075F;
    }
}
