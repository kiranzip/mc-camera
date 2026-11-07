package zip.kiran.snapshot.net;

import zip.kiran.snapshot.net.payload.CameraSettingsC2S;
import zip.kiran.snapshot.net.payload.PhotoChunkC2S;
import zip.kiran.snapshot.net.payload.PhotoRequestC2S;
import zip.kiran.snapshot.net.payload.PhotoUploadBeginC2S;
import zip.kiran.snapshot.server.ServerPhotoStore;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public final class CameraNetworking {
    private CameraNetworking() {
    }

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(PhotoUploadBeginC2S.TYPE, (packet, player, sender) ->
                onServerThread(player, store ->
                        store.beginUpload(player, packet.transferId(), packet.chunkCount(),
                                packet.compressedLength(), packet.settings())));

        ServerPlayNetworking.registerGlobalReceiver(PhotoChunkC2S.TYPE, (packet, player, sender) ->
                onServerThread(player, store ->
                        store.acceptChunk(player, packet.transferId(), packet.index(), packet.data())));

        ServerPlayNetworking.registerGlobalReceiver(PhotoRequestC2S.TYPE, (packet, player, sender) ->
                onServerThread(player, store ->
                        store.serve(player, packet.photoId())));

        ServerPlayNetworking.registerGlobalReceiver(CameraSettingsC2S.TYPE, (packet, player, sender) ->
                onServerThread(player, store ->
                        store.updateSettings(player, packet.settings())));
    }

    private interface StoreAction {
        void run(ServerPhotoStore store);
    }

    private static void onServerThread(ServerPlayerEntity player, StoreAction action) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        server.execute(() -> {
            ServerPhotoStore store = ServerPhotoStore.get();
            if (store != null && !player.isRemoved()) {
                action.run(store);
            }
        });
    }
}
