import com.aitu.sdp.media.ConversionException;
import com.aitu.sdp.media.MediaConversionFacade;
import java.nio.file.Path;

/** Client: knows only the facade and its public failure type. */
public final class Main {
    public static void main(String[] args) {
        MediaConversionFacade converter = new MediaConversionFacade();
        try {
            if (args.length == 1 && "--demo".equals(args[0])) {
                demonstrate(converter);
            } else if (args.length == 2) {
                Path output = converter.convert(args[0], args[1]);
                System.out.println("Created: " + output);
            } else {
                System.err.println("Usage: java -cp out Main <input.wav|input.au> <wav|au>");
                System.err.println("Demo:  java -cp out Main --demo");
                System.exit(1);
            }
        } catch (ConversionException exception) {
            System.err.println("Conversion failed: " + exception.getMessage());
            System.exit(1);
        }
    }

    private static void demonstrate(MediaConversionFacade converter) throws ConversionException {
        System.out.println("Facade demo: real stereo PCM -> mono WAV and AU");
        for (String format : new String[] {"wav", "au"}) {
            Path output = converter.convert("samples/stereo.wav", format);
            System.out.println("SUCCESS: " + output.getFileName());
        }
        try {
            converter.convert("samples/stereo.wav", "mp3");
            throw new IllegalStateException("Unsupported target should have failed");
        } catch (ConversionException expected) {
            System.out.println("EXPECTED ERROR: " + expected.getMessage());
        }
    }
}
