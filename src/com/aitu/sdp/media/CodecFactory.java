package com.aitu.sdp.media;

import java.util.Locale;
import javax.sound.sampled.AudioFileFormat;

/** Resolves the target container and PCM byte order in one place. */
public final class CodecFactory {
    public EncodingProfile forFormat(String targetFormat) {
        if (targetFormat == null || targetFormat.isBlank()) {
            throw new IllegalArgumentException("Target format is required: wav or au");
        }
        return switch (targetFormat.strip().toLowerCase(Locale.ROOT)) {
            case "wav" -> new EncodingProfile(AudioFileFormat.Type.WAVE, false, "wav");
            case "au" -> new EncodingProfile(AudioFileFormat.Type.AU, true, "au");
            default -> throw new IllegalArgumentException("Unsupported target format: "
                    + targetFormat + "; supported formats: wav, au");
        };
    }
}
