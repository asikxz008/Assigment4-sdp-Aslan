package com.aitu.sdp.media;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

/** Decodes PCM bytes according to the header's byte order and frame layout. */
public final class PcmDecoder {
    private static final int MAX_PCM_BYTES = 20 * 1024 * 1024;

    public PcmAudio decode(Path input, AudioMetadata metadata)
            throws IOException, UnsupportedAudioFileException {
        AudioFormat format = metadata.format();
        long expectedBytes = metadata.frameCount() * format.getFrameSize();
        if (expectedBytes > MAX_PCM_BYTES) {
            throw new IllegalArgumentException("PCM data exceeds the 20 MiB demo limit");
        }
        try (AudioInputStream stream = AudioSystem.getAudioInputStream(input.toFile())) {
            byte[] bytes = stream.readNBytes(MAX_PCM_BYTES + format.getFrameSize());
            if (bytes.length != expectedBytes) {
                throw new IOException("PCM length does not match the header; input may be truncated");
            }
            ByteOrder order = format.isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
            ByteBuffer buffer = ByteBuffer.wrap(bytes).order(order);
            short[] samples = new short[bytes.length / Short.BYTES];
            buffer.asShortBuffer().get(samples);
            return new PcmAudio(format.getSampleRate(), format.getChannels(), samples);
        }
    }
}
