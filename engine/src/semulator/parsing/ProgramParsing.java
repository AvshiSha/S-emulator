package semulator.parsing;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;
import semulator.execution.ExecutionContext;
import semulator.instructions.SInstruction;
import semulator.label.Label;
import semulator.program.SProgramImpl;
import semulator.variable.Variable;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
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
    public static ParseResult parseXml(Path xmlFile) {
        List<String> errors = new ArrayList<>();

        // DTO for instruction
        final class InstructionDTO {
            String type;
            String name;
            String variable = "";   // maybe empty
            String label = null;    // optional
            final Map<String, String> args = new LinkedHashMap<>();
            int idx;
        }

        // Minimal adapter for SInstruction
        final class ParsedInstructionAdapter implements SInstruction {
            private final InstructionDTO dto;

            ParsedInstructionAdapter(InstructionDTO dto) {
                this.dto = dto;
            }

            @Override
            public String getName() {
                return dto.name;
            }

            @Override
            public Label execute(ExecutionContext context) {
                return null;
            }

            @Override
            public int cycles() {
                return 0;
            }

            @Override
            public Label getLabel() {
                return null;
            }

            @Override
            public Variable getVariable() {
                return null;
            }
        }

        try (InputStream in = Files.newInputStream(xmlFile)) {
            SAXParserFactory f = SAXParserFactory.newInstance();
            f.setNamespaceAware(true);
            SAXParser sax = f.newSAXParser();

            final List<InstructionDTO> instructions = new ArrayList<>();
            final StringBuilder[] textBuf = new StringBuilder[1];
            final InstructionDTO[] cur = new InstructionDTO[1];
            final boolean[] sawSProgram = {false};
            final boolean[] sawSInstructions = {false};
            final String[] programName = {null};
            final int[] instrCounter = {0};

            DefaultHandler handler = new DefaultHandler() {
                @Override
                public void startElement(String uri, String local, String qName, Attributes atts) {
                    switch (qName) {
                        case "S-Program" -> {
                            sawSProgram[0] = true;
                            programName[0] = Optional.ofNullable(atts.getValue("name")).orElse("").trim();
                            if (programName[0].isEmpty()) {
                                errors.add("S-Program 'name' attribute is mandatory and must be non-empty.");
                            }
                        }
                        case "S-Instructions" -> sawSInstructions[0] = true;
                        case "S-Instruction" -> {
                            cur[0] = new InstructionDTO();
                            cur[0].idx = ++instrCounter[0];
                            cur[0].type = Optional.ofNullable(atts.getValue("type")).orElse("");
                            cur[0].name = Optional.ofNullable(atts.getValue("name")).orElse("");
                            if (cur[0].type.isEmpty()) errors.add(where() + "Missing 'type' on <S-Instruction>.");
                            if (cur[0].name.isEmpty()) errors.add(where() + "Missing 'name' on <S-Instruction>.");
                        }
                        case "S-Variable", "S-Label" -> textBuf[0] = new StringBuilder();
                        case "S-Instruction-Argument" -> {
                            if (cur[0] == null) {
                                errors.add("S-Instruction-Argument appears outside of an <S-Instruction>.");
                            } else {
                                String n = Optional.ofNullable(atts.getValue("name")).orElse("");
                                String v = Optional.ofNullable(atts.getValue("value")).orElse("");
                                if (n.isEmpty()) {
                                    errors.add(where() + "S-Instruction-Argument missing 'name' attribute.");
                                } else {
                                    cur[0].args.put(n, v);
                                }
                            }
                        }
                    }
                }

                @Override
                public void characters(char[] ch, int start, int length) {
                    if (textBuf[0] != null) textBuf[0].append(ch, start, length);
                }

                @Override
                public void endElement(String uri, String local, String qName) {
                    switch (qName) {
                        case "S-Variable" -> {
                            if (cur[0] != null) {
                                cur[0].variable = textBuf[0] == null ? "" : textBuf[0].toString().trim(); // maybe empty
                            }
                            textBuf[0] = null;
                        }
                        case "S-Label" -> {
                            if (cur[0] != null) {
                                String v = textBuf[0] == null ? "" : textBuf[0].toString().trim();
                                cur[0].label = v.isEmpty() ? null : v; // optional
                            }
                            textBuf[0] = null;
                        }
                        case "S-Instruction" -> {
                            if (cur[0] != null) {
                                instructions.add(cur[0]);
                                cur[0] = null;
                            }
                        }
                    }
                }

                @Override
                public void endDocument() {
                    if (!sawSProgram[0]) errors.add("Missing root <S-Program> element.");
                    if (!sawSInstructions[0]) errors.add("Missing <S-Instructions> element.");
                    if (instructions.isEmpty()) errors.add("No <S-Instruction> elements found.");
                }

                private String where() {
                    return (cur[0] == null) ? "" : "[Instruction#" + cur[0].idx + "] ";
                }
            };

            sax.parse(new InputSource(in), handler);

            if (!errors.isEmpty()) return new ParseResult(false, null, errors);

            SProgramImpl program = new SProgramImpl(programName[0]);
            for (InstructionDTO dto : instructions) {
                program.addInstruction(new ParsedInstructionAdapter(dto));
            }
            return new ParseResult(true, program, List.of());

        } catch (Exception e) {
            errors.add("XML parse error: " + e.getMessage());
            return new ParseResult(false, null, errors);
        }
    }
}
