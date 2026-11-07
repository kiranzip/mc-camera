package zip.kiran.snapshot.config;

import java.util.Properties;

public final class RuntimeConfig {
    private RuntimeConfig() {}
    public static int photoSize = 128;
    public static boolean polaroidDevelopEffect = true;
    public static float developSeconds = 30.0F;
    public static boolean filmVignette = true;
    public static boolean filmColorProfile = true;
    public static void load(Properties props) {
        String v = props.getProperty("photo_size");
        if (v != null) {
            try {
                int s = Integer.parseInt(v);
                if (s > 0) photoSize = s;
            } catch (NumberFormatException ignored) {}
        }
        String b = props.getProperty("polaroid_develop_effect");
        if (b != null) {
            polaroidDevelopEffect = Boolean.parseBoolean(b);
        }
        String d = props.getProperty("develop_seconds");
        if (d != null) {
            try {
                float f = Float.parseFloat(d);
                if (f > 0) developSeconds = f;
            } catch (NumberFormatException ignored) {}
        }
        String v2 = props.getProperty("film_vignette");
        if (v2 != null) {
            filmVignette = Boolean.parseBoolean(v2);
        }
        String c = props.getProperty("film_color_profile");
        if (c != null) {
            filmColorProfile = Boolean.parseBoolean(c);
        }
    }
}
