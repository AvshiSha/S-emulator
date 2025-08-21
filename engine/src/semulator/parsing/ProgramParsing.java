package semulator.parsing;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import semulator.program.SProgramImpl;

import javax.xml.parsers.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;

public final class ProgramParsing {
    private ProgramParsing() {
    }

    /**
     * @param path non-null only when ok == true
     */ // ---------- RESULT TYPES (siblings, not nested inside each other) ----------
    public record PathCheckResult(boolean ok, Path path, List<String> errors) {
        public PathCheckResult(boolean ok, Path path, List<String> errors) {
            this.ok = ok;
            this.path = path;
            this.errors = errors == null ? List.of() : List.copyOf(errors);
        }
    }

    /**
     * @param program non-null only when success == true
     */
    public record ParseResult(boolean success, SProgramImpl program, List<String> errors) {
        public ParseResult(boolean success, SProgramImpl program, List<String> errors) {
            this.success = success;
            this.program = program;
            this.errors = errors == null ? List.of() : List.copyOf(errors);
        }
    }

    // ---------- CONSTANTS ----------
    // Printable ASCII only (no control chars).Blocks Hebrew/non-ASCII letters.
    private static final Pattern ASCII_PATH = Pattern.compile("^[\\p{ASCII}&&[^\\p{Cntrl}]]+$");

    // (You can keep these for later parser/validation phases if you want)
//    static final Set<String> ALLOWED_TYPES = Set.of("basic", "synthetic");
//    static final Set<String> ALLOWED_NAMES = Set.of(
//            "NEUTRAL", "INCREASE", "DECREASE", "JUMP_NOT_ZERO",
//            "ZERO_VARIABLE", "ASSIGNMENT", "GOTO_LABEL", "CONSTANT_ASSIGNMENT",
//            "JUMP_ZERO", "JUMP_EQUAL_CONSTANT", "JUMP_EQUAL_VARIABLE"
//    );

    // ---------- PATH METHODS (now truly on ProgramParsing) ----------
    public static PathCheckResult askPathAndCheckFromConsole() {
        System.out.print("Enter full path to XML: ");
        java.util.Scanner sc = new java.util.Scanner(System.in);
        String line = sc.hasNextLine() ? sc.nextLine() : "";
        return checkPath(line);
    }

//    public static PathCheckResult askPathAndCheck(java.util.function.Supplier<String> lineReader) {
//        String line = (lineReader == null) ? "" : lineReader.get();
//        if (line == null) line = "";
//        return checkPath(line);
//    }

    public static PathCheckResult checkPath(String rawPathLine) {
        List<String> errors = new ArrayList<>();
        String s = (rawPathLine == null) ? "" : rawPathLine.trim();

        if (s.isEmpty()) {
            errors.add("Path is empty after trimming.");
            return new PathCheckResult(false, null, errors);
        }
        if (!ASCII_PATH.matcher(s).matches()) {
            errors.add("Path must contain only English/ASCII characters (no Hebrew or non-ASCII).");
            return new PathCheckResult(false, null, errors);
        }
        if (!s.toLowerCase(Locale.ROOT).endsWith(".xml")) {
            errors.add("File must end with .xml");
            return new PathCheckResult(false, null, errors);
        }

        final Path p;
        try {
            p = Paths.get(s).normalize();
        } catch (Exception ex) {
            errors.add("Invalid path: " + ex.getMessage());
            return new PathCheckResult(false, null, errors);
        }

        if (!Files.exists(p)) {
            errors.add("File does not exist: " + p);
            return new PathCheckResult(false, null, errors);
        }

        return new PathCheckResult(true, p, List.of());
    }

    // ---------- SIMPLE SAX PARSE (returns ParseResult; never throws) ----------
    public static Document parseXml(Path xmlFile) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputStream xmlFileInputStream = new FileInputStream(xmlFile.toFile());
        return builder.parse(xmlFileInputStream);

}