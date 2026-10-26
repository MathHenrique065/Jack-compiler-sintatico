package jack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Compares generated XML with the official nand2tetris reference files.
 * Like the book's test, each line is stripped and empty lines are ignored,
 * so indentation and line endings do not matter.
 *
 * Usage:
 *   java -cp out jack.Validate <gerado.xml> <referencia.xml>
 *   java -cp out jack.Validate <pasta_gerada> <pasta_referencia>   (compares every reference *.xml)
 */
public class Validate {

    static List<String> normalize(Path file) throws IOException {
        return Files.readAllLines(file).stream()
                .map(String::strip)
                .filter(l -> !l.isEmpty())
                .toList();
    }

    /** Returns null when both files are equivalent, otherwise a description of the first difference. */
    static String diff(Path actual, Path expected) throws IOException {
        List<String> a = normalize(actual);
        List<String> e = normalize(expected);
        int n = Math.min(a.size(), e.size());
        for (int i = 0; i < n; i++) {
            if (!a.get(i).equals(e.get(i))) {
                return "linha " + (i + 1) + ": obtido '" + a.get(i) + "' esperado '" + e.get(i) + "'";
            }
        }
        if (a.size() != e.size()) {
            return "tamanhos diferentes: obtido " + a.size() + " linhas, esperado " + e.size();
        }
        return null;
    }

    private static boolean compare(Path actual, Path expected) throws IOException {
        if (!Files.exists(actual)) {
            System.out.println("FALTA " + actual);
            return false;
        }
        String d = diff(actual, expected);
        if (d == null) {
            System.out.println("OK    " + actual.getFileName());
            return true;
        }
        System.out.println("DIFF  " + actual + " -> " + d);
        return false;
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Uso: java -cp out jack.Validate <gerado | pasta> <referencia | pasta>");
            System.exit(2);
        }
        Path actual = Path.of(args[0]);
        Path expected = Path.of(args[1]);
        boolean allOk = true;
        if (Files.isDirectory(expected)) {
            List<Path> refs;
            try (Stream<Path> s = Files.list(expected)) {
                refs = s.filter(p -> p.toString().endsWith(".xml")).sorted().toList();
            }
            for (Path ref : refs) {
                allOk &= compare(actual.resolve(ref.getFileName()), ref);
            }
        } else {
            allOk = compare(actual, expected);
        }
        System.exit(allOk ? 0 : 1);
    }
}
