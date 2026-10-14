package com.aitu.sdp.media;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

/** Reads and validates the input header without processing samples. */
public final class MetadataReader {
    public AudioMetadata read(Path input) throws IOException, UnsupportedAudioFileException {
        if (!Files.isRegularFile(input)) {
            throw new IOException("Input is not a regular file: " + input);
        }
        AudioFileFormat file = AudioSystem.getAudioFileFormat(input.toFile());
        if (!AudioFileFormat.Type.WAVE.equals(file.getType())
                && !AudioFileFormat.Type.AU.equals(file.getType())) {
            throw new IllegalArgumentException("Input must be a WAV or AU file");
        }
        AudioFormat format = file.getFormat();
        if (!AudioFormat.Encoding.PCM_SIGNED.equals(format.getEncoding())
                || format.getSampleSizeInBits() != 16) {
            throw new IllegalArgumentException("Input must contain signed 16-bit PCM audio");
        }
        if (format.getChannels() < 1 || format.getChannels() > 2
                || format.getFrameSize() != format.getChannels() * Short.BYTES) {
            throw new IllegalArgumentException("Input must contain complete mono or stereo frames");
        }
        if (!Float.isFinite(format.getSampleRate()) || format.getSampleRate() <= 0
                || file.getFrameLength() <= 0) {
            throw new IllegalArgumentException("Input must have a known sample rate and frame count");
        }
        return new AudioMetadata(format, file.getFrameLength());
    }
}
