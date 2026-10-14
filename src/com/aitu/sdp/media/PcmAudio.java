package com.aitu.sdp.media;

/** Immutable interleaved signed 16-bit PCM samples. */
public final class PcmAudio {
    private final float sampleRate;
    private final int channels;
    private final short[] samples;

    public PcmAudio(float sampleRate, int channels, short[] samples) {
        if (!Float.isFinite(sampleRate) || sampleRate <= 0) {
            throw new IllegalArgumentException("Sample rate must be positive and finite");
        }
        if (channels < 1 || channels > 2) {
            throw new IllegalArgumentException("Only mono and stereo audio are supported");
        }
        if (samples == null || samples.length == 0 || samples.length % channels != 0) {
            throw new IllegalArgumentException("Samples must contain complete non-empty frames");
        }
        this.sampleRate = sampleRate;
        this.channels = channels;
        this.samples = samples.clone();
    }

    public float sampleRate() {
        return sampleRate;
    }

    public int channels() {
        return channels;
    }

    public int frameCount() {
        return samples.length / channels;
    }

    public short[] samples() {
        return samples.clone();
    }
}
