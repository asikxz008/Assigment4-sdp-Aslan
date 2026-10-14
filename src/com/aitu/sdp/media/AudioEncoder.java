package com.aitu.sdp.media;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/** Encodes samples and publishes the file only after writing has completed. */
public final class AudioEncoder {
    public Path write(PcmAudio audio, EncodingProfile profile, Path output) throws IOException {
        Path destination = output.toAbsolutePath().normalize();
        Path temporary = Files.createTempFile(destination.getParent(), ".conversion-", ".tmp");
        try {
            writeContainer(audio, profile, temporary);
            publish(temporary, destination);
            return destination;
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private void writeContainer(PcmAudio audio, EncodingProfile profile, Path temporary)
            throws IOException {
        short[] samples = audio.samples();
        ByteOrder order = profile.bigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
        ByteBuffer bytes = ByteBuffer.allocate(samples.length * Short.BYTES).order(order);
        bytes.asShortBuffer().put(samples);
        AudioFormat format = new AudioFormat(audio.sampleRate(), 16, audio.channels(),
                true, profile.bigEndian());
        try (AudioInputStream stream = new AudioInputStream(
                new ByteArrayInputStream(bytes.array()), format, audio.frameCount())) {
            if (!AudioSystem.isFileTypeSupported(profile.fileType(), stream)) {
                throw new IOException("Writer does not support the selected audio profile");
            }
            int writtenBytes = AudioSystem.write(stream, profile.fileType(), temporary.toFile());
            if (writtenBytes <= 0) {
                throw new IOException("Audio writer produced no data");
            }
        }
    }

    private void publish(Path temporary, Path destination) throws IOException {
        try {
            Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
