# Assignment 4 Facade Pattern

**Student:** Sabyr Aslan | **Group:** SE-2528 | **Option A:** Media/File Conversion  
**Repository:** https://github.com/asikxz008/Assigment4-sdp-Aslan

`MediaConversionFacade.convert(fileName, targetFormat)` converts real signed 16-bit
PCM WAV/AU audio to a mono WAV or AU file. It preserves the sample rate and frame
count. No external libraries, Maven or audio hardware are needed.

## Build and demonstrate

Run from this repository's root with JDK 17 or later:

```sh
javac --release 17 -encoding UTF-8 -Xlint:all -Werror -d out "@sources.txt" "@test-sources.txt"
java -cp out Main --demo
java -cp out ConversionTests
```

For a specific file:

```sh
java -cp out Main samples/stereo.wav au
```

The supplied sample is a generated one-second stereo tone at 8,000 Hz.
The demo creates `samples/stereo-converted.wav` and `samples/stereo-converted.au`,
then demonstrates an unsupported `mp3` target. Tests should end with
`SUMMARY: 14/14 PASS`; any failed check throws an error and exits unsuccessfully.

In IntelliJ IDEA, open the repository, choose JDK 17, mark `src` as Sources Root
and `test` as Test Sources Root. Run `Main` with program arguments `--demo` and the
repository root as working directory. Run `ConversionTests` without arguments.

## Design and structure

| Role | Class | Responsibility |
|---|---|---|
| Client | Main | Uses only the facade and its failure type |
| Facade | MediaConversionFacade | Coordinates steps and names the output |
| Subsystem | MetadataReader | Reads and validates the input header |
| Subsystem | CodecFactory | Resolves container, extension and byte order |
| Subsystem | PcmDecoder | Turns PCM bytes into sample values |
| Subsystem | AudioMixer | Averages each stereo frame into mono |
| Subsystem | AudioEncoder | Writes a complete container and publishes it |

`src/com/aitu/sdp/media` also contains `AudioMetadata`, `PcmAudio`,
`EncodingProfile` and `ConversionException`. `test/ConversionTests.java` verifies
real file contents and uses subsystem classes directly in T10. It is an advanced
test client; the application client `Main` never calls those classes.

Each subsystem is public and has no reference to the facade. The facade exposes
only filenames, target format, a standard `Path` result and `ConversionException`.
Its internal pipeline is: profile selection -> header validation -> PCM decoding
-> mono mixing -> container writing. The report explains six Clean Code principles
with excerpts from the actual implementation.

## Scope and error handling

Supported inputs are mono/stereo signed 16-bit PCM WAV/AU with known positive frame
count and sample rate. The PCM payload limit is 20 MiB. MP3/video, compression,
resampling and streaming are outside this example's scope. Mixing uses integer
averages truncated toward zero; mono input passes through without alteration.

The output is a sibling named `<stem>-converted.<format>`; repeating a conversion
replaces that generated destination after successful writing. The input is kept.
The writer uses a temporary file in the destination directory, attempts atomic
replacement and falls back to a regular move if atomic moves are unavailable.
That fallback does not guarantee atomic visibility. Failed writes remove the
temporary file. Failures reach the client as `ConversionException`, with the
original exception retained as the cause.

`report.pdf` contains the introduction, UML, Clean Code evidence, verification,
conclusion and repository link. `docs/class-diagram.svg` and `.puml` provide the
diagram separately. `defense-notes.txt` contains preparation notes in Russian.
`demo-output.txt` and `tests-output.txt` record execution on Java 17.0.16.
