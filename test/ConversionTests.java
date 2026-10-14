import com.aitu.sdp.media.AudioEncoder;
import com.aitu.sdp.media.AudioMetadata;
import com.aitu.sdp.media.AudioMixer;
import com.aitu.sdp.media.CodecFactory;
import com.aitu.sdp.media.ConversionException;
import com.aitu.sdp.media.EncodingProfile;
import com.aitu.sdp.media.MediaConversionFacade;
import com.aitu.sdp.media.MetadataReader;
import com.aitu.sdp.media.PcmAudio;
import com.aitu.sdp.media.PcmDecoder;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/** Independent checks of written files; not the application client. No libraries needed. */
public final class ConversionTests {
    private static final float SAMPLE_RATE = 8000;
    private static final short[] STEREO = {-12000, 4000, 16000, 8000, -32768, 32767, 6, 2};
    private static final short[] EXPECTED_MONO = {-4000, 12000, 0, 4};
    private static int passed;

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("facade-tests-");
        try {
            Path source = directory.resolve("stereo.wav");
            createWave(source, STEREO, 2);
            byte[] original = Files.readAllBytes(source);
            MediaConversionFacade converter = new MediaConversionFacade();

            check("T1 stereo WAV -> mono WAV", () -> verifyAudio(
                    converter.convert(source.toString(), "wav"), AudioFileFormat.Type.WAVE,
                    EXPECTED_MONO));
            Path au = converter.convert(source.toString(), "au");
            check("T2 stereo WAV -> mono AU and big-endian samples", () ->
                    verifyAudio(au, AudioFileFormat.Type.AU, EXPECTED_MONO));
            check("T3 AU -> WAV preserves mono samples", () -> verifyAudio(
                    converter.convert(au.toString(), "wav"), AudioFileFormat.Type.WAVE,
                    EXPECTED_MONO));
            check("T4 original input bytes unchanged", () -> require(
                    Arrays.equals(original, Files.readAllBytes(source)), "Original was modified"));
            check("T5 target format is case-insensitive", () -> verifyAudio(
                    converter.convert(source.toString(), " AU "), AudioFileFormat.Type.AU,
                    EXPECTED_MONO));
            check("T6 unsupported target retains cause and creates no output", () -> {
                expectFailure(() -> converter.convert(source.toString(), "mp3"));
                require(!Files.exists(directory.resolve("stereo-converted.mp3")), "Output exists");
            });
            check("T7 missing input fails", () -> expectFailure(() ->
                    converter.convert(directory.resolve("missing.wav").toString(), "wav")));
            check("T8 malformed input fails", () -> {
                Path invalid = directory.resolve("invalid.wav");
                Files.writeString(invalid, "not an audio file");
                expectFailure(() -> converter.convert(invalid.toString(), "wav"));
                require(!Files.exists(directory.resolve("invalid-converted.wav")), "Output exists");
            });
            check("T9 truncated PCM is rejected", () -> {
                Path truncated = directory.resolve("truncated.wav");
                Files.write(truncated, Arrays.copyOf(original, original.length - 4));
                expectFailure(() -> converter.convert(truncated.toString(), "au"));
            });
            check("T10 advanced client uses subsystem directly", () -> {
                AudioMetadata metadata = new MetadataReader().read(source);
                PcmAudio decoded = new PcmDecoder().decode(source, metadata);
                PcmAudio mono = new AudioMixer().toMono(decoded);
                EncodingProfile profile = new CodecFactory().forFormat("au");
                Path direct = new AudioEncoder().write(mono, profile, directory.resolve("direct.au"));
                verifyAudio(direct, AudioFileFormat.Type.AU, EXPECTED_MONO);
            });
            check("T11 write failure leaves existing destination and no temporary file", () -> {
                Path blockedSource = directory.resolve("blocked.wav");
                Files.copy(source, blockedSource);
                Path blockedOutput = directory.resolve("blocked-converted.au");
                Files.createDirectory(blockedOutput);
                Path sentinel = blockedOutput.resolve("keep.txt");
                Files.writeString(sentinel, "keep");
                expectFailure(() -> converter.convert(blockedSource.toString(), "au"));
                require("keep".equals(Files.readString(sentinel)), "Existing data changed");
                try (var paths = Files.list(directory)) {
                    require(paths.noneMatch(path -> path.getFileName().toString()
                            .startsWith(".conversion-")), "Temporary file leaked");
                }
            });
            check("T12 mono input and repeated conversion preserve samples", () -> {
                Path mono = directory.resolve("mono.wav");
                createWave(mono, EXPECTED_MONO, 1);
                Path first = converter.convert(mono.toString(), "wav");
                Path repeated = converter.convert(mono.toString(), "wav");
                require(first.equals(repeated), "Output path changed");
                verifyAudio(repeated, AudioFileFormat.Type.WAVE, EXPECTED_MONO);
            });
            check("T13 blank and null arguments fail through the public API", () -> {
                expectFailure(() -> converter.convert("", "wav"));
                expectFailure(() -> converter.convert(null, "wav"));
                expectFailure(() -> converter.convert(source.toString(), null));
            });
            check("T14 audio model cannot be mutated through arrays", () -> {
                short[] samples = {100, 200};
                PcmAudio audio = new PcmAudio(SAMPLE_RATE, 1, samples);
                samples[0] = 999;
                short[] exposed = audio.samples();
                exposed[1] = 999;
                require(Arrays.equals(audio.samples(), new short[] {100, 200}), "State leaked");
            });
            System.out.println("SUMMARY: " + passed + "/14 PASS");
        } finally {
            try (var paths = Files.walk(directory)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            }
        }
    }

    private static void verifyAudio(Path output, AudioFileFormat.Type type, short[] expected)
            throws Exception {
        AudioFileFormat file = AudioSystem.getAudioFileFormat(output.toFile());
        require(type.equals(file.getType()), "Wrong container");
        AudioFormat format = file.getFormat();
        require(format.getChannels() == 1, "Not mono");
        require(format.getSampleRate() == SAMPLE_RATE, "Sample rate changed");
        require(format.getSampleSizeInBits() == 16, "Wrong sample width");
        require(file.getFrameLength() == expected.length, "Duration or frame count changed");
        try (AudioInputStream stream = AudioSystem.getAudioInputStream(output.toFile())) {
            byte[] bytes = stream.readAllBytes();
            require(bytes.length == expected.length * Short.BYTES, "Wrong PCM length");
            ByteOrder order = format.isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
            short[] actual = new short[expected.length];
            ByteBuffer.wrap(bytes).order(order).asShortBuffer().get(actual);
            require(Arrays.equals(actual, expected), "Wrong samples: " + Arrays.toString(actual));
        }
    }

    private static void createWave(Path output, short[] samples, int channels) throws Exception {
        ByteBuffer bytes = ByteBuffer.allocate(samples.length * Short.BYTES)
                .order(ByteOrder.LITTLE_ENDIAN);
        bytes.asShortBuffer().put(samples);
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, channels, true, false);
        try (AudioInputStream stream = new AudioInputStream(new ByteArrayInputStream(bytes.array()),
                format, samples.length / channels)) {
            AudioSystem.write(stream, AudioFileFormat.Type.WAVE, output.toFile());
        }
    }

    private static void expectFailure(CheckedAction action) throws Exception {
        try {
            action.run();
        } catch (ConversionException expected) {
            require(expected.getCause() != null, "Original failure cause missing");
            return;
        }
        throw new AssertionError("Expected ConversionException");
    }

    private static void check(String name, CheckedAction action) throws Exception {
        action.run();
        passed++;
        System.out.println(name + " PASS");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }
}
