package com.aitu.sdp.media;

import javax.sound.sampled.AudioFileFormat;

/** Container and byte-order choice used by the encoder. */
public record EncodingProfile(AudioFileFormat.Type fileType, boolean bigEndian, String extension) {
}
