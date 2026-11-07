package zip.kiran.snapshot.photo;

import zip.kiran.snapshot.CameraConstants;
import zip.kiran.snapshot.config.RuntimeConfig;

import java.io.ByteArrayOutputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public final class PhotoCompression {
    private PhotoCompression() {
    }

    public static byte[] deflate(byte[] raw) {
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        try {
            deflater.setInput(raw);
            deflater.finish();
            ByteArrayOutputStream out = new ByteArrayOutputStream(raw.length / 2);
            byte[] buffer = new byte[8192];
            while (!deflater.finished()) {
                int written = deflater.deflate(buffer);
                if (written <= 0) {
                    break;
                }
                out.write(buffer, 0, written);
            }
            return out.toByteArray();
        } finally {
            deflater.end();
        }
    }

    public static byte[] inflateExact(byte[] compressed) throws DataFormatException {
        Inflater inflater = new Inflater();
        try {
            inflater.setInput(compressed);
            int size = RuntimeConfig.photoSize * RuntimeConfig.photoSize * 3;
            byte[] out = new byte[size];
            int offset = 0;
            while (offset < out.length && !inflater.finished()) {
                int written = inflater.inflate(out, offset, out.length - offset);
                if (written == 0) {
                    if (inflater.needsInput() || inflater.needsDictionary()) {
                        break;
                    }
                }
                offset += written;
            }
            if (offset != out.length) {
                throw new DataFormatException("expected " + out.length + " bytes, decoded " + offset);
            }
            byte[] overflow = new byte[1];
            if (!inflater.finished() && inflater.inflate(overflow) > 0) {
                throw new DataFormatException("trailing data after one frame");
            }
            return out;
        } finally {
            inflater.end();
        }
    }

    public static byte[][] chunk(byte[] data) {
        int count = Math.max(1, (data.length + CameraConstants.CHUNK_BYTES - 1) / CameraConstants.CHUNK_BYTES);
        byte[][] chunks = new byte[count][];
        for (int i = 0; i < count; i++) {
            int from = i * CameraConstants.CHUNK_BYTES;
            int to = Math.min(data.length, from + CameraConstants.CHUNK_BYTES);
            byte[] piece = new byte[to - from];
            System.arraycopy(data, from, piece, 0, piece.length);
            chunks[i] = piece;
        }
        return chunks;
    }
}
