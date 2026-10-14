package com.aitu.sdp.media;

/** Downmixes each frame by averaging its channels; keeps rate and duration. */
public final class AudioMixer {
    public PcmAudio toMono(PcmAudio source) {
        if (source.channels() == 1) {
            return source;
        }
        short[] input = source.samples();
        short[] mono = new short[source.frameCount()];
        for (int frame = 0; frame < mono.length; frame++) {
            int sum = 0;
            for (int channel = 0; channel < source.channels(); channel++) {
                sum += input[frame * source.channels() + channel];
            }
            mono[frame] = (short) (sum / source.channels());
        }
        return new PcmAudio(source.sampleRate(), 1, mono);
    }
}
