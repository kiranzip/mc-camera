package zip.kiran.snapshot;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import zip.kiran.snapshot.photo.PhotoCompression;
import zip.kiran.snapshot.component.CameraSettings;

public class TestPhotoCompression {
    @Test
    void deflateInflateRoundTrip() throws Exception {
        byte[] raw = new byte[CameraConstants.PHOTO_BYTES];
        for (int i = 0; i < raw.length; i++) raw[i] = (byte)(i % 256);
        byte[] compressed = PhotoCompression.deflate(raw);
        byte[] inflated = PhotoCompression.inflateExact(compressed);
        assertArrayEquals(raw, inflated);
    }

    @Test
    void sanitiseClampsSettings() {
        CameraSettings s = new CameraSettings(3.0F, 30.0F, 300.0F, 999);
        CameraSettings clean = s.sanitised();
        assertEquals(CameraSettings.APERTURES[2], clean.aperture(), 0.001);
        assertEquals(CameraSettings.FOCAL_LENGTHS[2], clean.focalLength(), 0.001);
        assertEquals(CameraSettings.MAX_FOCUS, clean.focusDistance(), 0.001);
        assertEquals(800, clean.iso());
    }
}
