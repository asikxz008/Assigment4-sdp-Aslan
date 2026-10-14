package com.aitu.sdp.media;

import java.io.IOException;
import java.nio.file.Path;
import javax.sound.sampled.UnsupportedAudioFileException;

/** Simple entry point that coordinates the independent audio subsystem. */
public final class MediaConversionFacade {
    private final MetadataReader metadataReader = new MetadataReader();
    private final CodecFactory codecFactory = new CodecFactory();
    private final PcmDecoder decoder = new PcmDecoder();
    private final AudioMixer mixer = new AudioMixer();
    private final AudioEncoder encoder = new AudioEncoder();

    public Path convert(String fileName, String targetFormat) throws ConversionException {
        try {
            if (fileName == null || fileName.isBlank()) {
                throw new IllegalArgumentException("Input file name is required");
            }
            Path input = Path.of(fileName).toAbsolutePath().normalize();
            EncodingProfile profile = codecFactory.forFormat(targetFormat);
            AudioMetadata metadata = metadataReader.read(input);
            PcmAudio decoded = decoder.decode(input, metadata);
            PcmAudio mono = mixer.toMono(decoded);
            Path output = outputPath(input, profile.extension());
            return encoder.write(mono, profile, output);
        } catch (IOException | UnsupportedAudioFileException | IllegalArgumentException exception) {
            throw new ConversionException("Cannot convert '" + fileName + "' to '"
                    + targetFormat + "': " + exception.getMessage(), exception);
        }
    }

    private Path outputPath(Path input, String extension) {
        String name = input.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String stem = dot > 0 ? name.substring(0, dot) : name;
        return input.resolveSibling(stem + "-converted." + extension);
    }
}
