package zip.kiran.snapshot.client;

import zip.kiran.snapshot.client.CameraClient;
import net.fabricmc.api.ClientModInitializer;

public class CameraModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CameraClient.init();
    }
}
