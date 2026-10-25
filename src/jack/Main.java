package jack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Entry point: java -cp out jack.Main <File.jack | directory>
 * Each .jack file will produce a File.xml (syntax tree) next to it.
 * TODO: plug in your Tokenizer and CompilationEngine here.
 *
 * <p>Options:
 * <ul>
 *   <li>{@code -h}, {@code --help}: print usage information and exit.</li>
 * </ul>
 */
public class Main {
    private static void printHelp() {
        System.out.println("Jack Syntax Analyzer");
        System.out.println("Usage: java -cp out jack.Main <File.jack | directory>");
        System.out.println("  -h, --help   Show this message");
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 1 && (args[0].equals("-h") || args[0].equals("--help"))) {
            printHelp();
            return;
        }
        if (args.length != 1) {
            System.err.println("Usage: java -cp out jack.Main <File.jack | directory>");
            System.exit(1);
        }
        Path input = Path.of(args[0]);
        List<Path> files;
        if (Files.isDirectory(input)) {
            try (Stream<Path> s = Files.list(input)) {
                files = s.filter(p -> p.toString().endsWith(".jack")).sorted().toList();
            }
        } else {
            files = List.of(input);
        }
        for (Path f : files) {
            Path out = Path.of(f.toString().replaceAll("\\.jack$", ".txt"));
            System.out.println("Would parse " + f + " -> " + out)
            // TODO: new CompilationEngine(new Tokenizer(f), out).compileClass();
        }
    }
}
