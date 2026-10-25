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
 */
public class Main {
    public static void main(String[] args) throws IOException {
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
            Path out = Path.of(f.toString().replaceAll("\\.jack$", ".xml"));
            System.out.println("Would parse " + f + " -> " + out);
            // TODO: new CompilationEngine(new Tokenizer(f), out).compileClass();
        }
    }
}
