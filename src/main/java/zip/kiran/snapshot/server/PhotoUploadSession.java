package zip.kiran.snapshot.server;

import zip.kiran.snapshot.CameraConstants;
import zip.kiran.snapshot.component.CameraSettings;

final class PhotoUploadSession {
    final int transferId;
    final CameraSettings settings;
    final byte[][] chunks;
    final int compressedLength;
    final long deadline;
    private int received;

    PhotoUploadSession(int transferId, int chunkCount, int compressedLength, CameraSettings settings, long now) {
        this.transferId = transferId;
        this.chunks = new byte[chunkCount][];
        this.compressedLength = compressedLength;
        this.settings = settings;
        this.deadline = now + CameraConstants.UPLOAD_TIMEOUT_MS;
    }

    boolean accept(int index, byte[] data) {
        if (index < 0 || index >= chunks.length || chunks[index] != null) {
            return false;
        }
        chunks[index] = data;
        received++;
        return true;
    }

    boolean isComplete() {
        return received == chunks.length;
    }

    boolean isExpired(long now) {
        return now > deadline;
    }

    byte[] assemble() {
        int total = 0;
        for (byte[] chunk : chunks) {
            total += chunk.length;
        }
        if (total != compressedLength) {
            return null;
        }
        byte[] out = new byte[total];
        int offset = 0;
        for (byte[] chunk : chunks) {
            System.arraycopy(chunk, 0, out, offset, chunk.length);
            offset += chunk.length;
        }
        return out;
    }
}
