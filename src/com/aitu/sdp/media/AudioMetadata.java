package com.aitu.sdp.media;

import javax.sound.sampled.AudioFormat;

/** Header information shared by the reader and decoder. */
public record AudioMetadata(AudioFormat format, long frameCount) {
}
