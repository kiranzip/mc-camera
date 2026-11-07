package zip.kiran.snapshot;

import zip.kiran.snapshot.net.CameraNetworking;
import zip.kiran.snapshot.server.ServerPhotoStore;
import zip.kiran.snapshot.component.Components;
import zip.kiran.snapshot.item.Items;
import zip.kiran.snapshot.sound.Sounds;
import zip.kiran.snapshot.config.RuntimeConfig;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CameraMod implements ModInitializer {
    public static final String MOD_ID = "kiranzip";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        java.util.Properties props = new java.util.Properties();
        try (java.io.InputStream is = CameraMod.class.getResourceAsStream("/assets/kiranzip/config/photo.properties")) {
            if (is != null) {
                props.load(is);
                RuntimeConfig.load(props);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load photo config", e);
        }
        Components.init();
        Items.init();
        Sounds.init();
        CameraNetworking.registerServerReceivers();
        ServerPhotoStore.registerLifecycle();
    }
}
