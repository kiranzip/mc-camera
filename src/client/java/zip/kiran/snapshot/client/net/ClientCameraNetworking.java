package zip.kiran.snapshot.client.net;

import zip.kiran.snapshot.net.payload.PhotoBeginS2C;
import zip.kiran.snapshot.net.payload.PhotoChunkS2C;
import zip.kiran.snapshot.net.payload.PhotoMissingS2C;
import zip.kiran.snapshot.client.photo.ClientPhotoCache;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class ClientCameraNetworking {
    private ClientCameraNetworking() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(PhotoBeginS2C.TYPE, (packet, player, sender) ->
                ClientPhotoCache.onDownloadBegin(packet.photoId(), packet.chunkCount(), packet.compressedLength()));

        ClientPlayNetworking.registerGlobalReceiver(PhotoChunkS2C.TYPE, (packet, player, sender) ->
                ClientPhotoCache.onDownloadChunk(packet.photoId(), packet.index(), packet.data()));

        ClientPlayNetworking.registerGlobalReceiver(PhotoMissingS2C.TYPE, (packet, player, sender) ->
                ClientPhotoCache.onMissing(packet.photoId()));
    }
}
