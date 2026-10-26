package jack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Entry point: java -cp out jack.Main <File.jack | directory> [output.xml | output directory]
 * Each .jack file produces a File.xml (syntax tree) and a FileT.xml (tokens). By default they are
 * written next to the source; with a second argument they go to that file (single input) or
 * directory (directory input).
 *
 * <p>Options:
 * <ul>
 *   <li>{@code -h}, {@code --help}: print usage information and exit.</li>
 * </ul>
 */
public class Main {
    private static void printHelp() {
        System.out.println("Jack Syntax Analyzer");
        System.out.println("Usage: java -cp out jack.Main <File.jack | directory> [output.xml | output directory]");
        System.out.println("  -h, --help   Show this message");
    }

    /** Where the parse tree of {@code jackFile} goes (null target = next to the source file). */
    private static Path outputFor(Path jackFile, Path input, Path target) throws IOException {
        String name = jackFile.getFileName().toString().replaceAll("\\.jack$", ".xml");
        if (target == null) {
            return jackFile.resolveSibling(name);
        }
        if (Files.isDirectory(input)) {
            Files.createDirectories(target);
            return target.resolve(name);
        }
        Path parent = target.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        return target;
    }

    /** Token file for a parse-tree file: Name.xml -> NameT.xml, in the same folder. */
    private static Path tokensFor(Path xml) {
        return xml.resolveSibling(xml.getFileName().toString().replaceAll("\\.xml$", "") + "T.xml");
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 1 && (args[0].equals("-h") || args[0].equals("--help"))) {
            printHelp();
            return;
        }
        if (args.length < 1 || args.length > 2) {
            System.err.println("Usage: java -cp out jack.Main <File.jack | directory> [output.xml | output directory]");
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
        Path target = args.length == 2 ? Path.of(args[1]) : null;
        boolean failed = false;
        for (Path f : files) {
            Path out = outputFor(f, input, target);
            Path tokensOut = tokensFor(out);
            try {
                List<Token> tokens = Tokenizer.fromFile(f).tokenize();
                List<String> lines = new java.util.ArrayList<>();
                lines.add("<tokens>");
                tokens.forEach(t -> lines.add(t.toXml()));
                lines.add("</tokens>");
                Files.write(tokensOut, lines);
                System.out.println("Tokenized " + f + " -> " + tokensOut + " (" + tokens.size() + " tokens)");
                Parser parser = new Parser(tokens);
                parser.parse();
                Files.write(out, parser.getOutput());
                System.out.println("Parsed    " + f + " -> " + out);
            } catch (IllegalStateException e) {
                System.err.println(f + ": " + e.getMessage());
                failed = true;
            }
        }
        if (failed) {
            System.exit(1);
        }
    }
}
