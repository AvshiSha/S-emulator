package semulator.program;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import semulator.instructions.SInstruction;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class SProgramImpl implements SProgram {

    private final String name;
    private final List<SInstruction> instructions;
    private Path xmlPath;

    public SProgramImpl(String name) {
        this.name = name;
        instructions = new ArrayList<>();
    }

    // Allowed instruction names (case-sensitive)
    private static final Set<String> ALLOWED_NAMES = Set.of(
            "NEUTRAL", "INCREASE", "DECREASE", "JUMP_NOT_ZERO",
            "ZERO_VARIABLE", "ASSIGNMENT", "GOTO_LABEL", "CONSTANT_ASSIGNMENT",
            "JUMP_ZERO", "JUMP_EQUAL_CONSTANT", "JUMP_EQUAL_VARIABLE", "QUOTE_PROGRAM", "JUMP_EQUAL_FUNCTION"
    );

    // Classification for the optional cross-check
    private static final Set<String> BASIC = Set.of(
            "NEUTRAL", "INCREASE", "DECREASE", "JUMP_NOT_ZERO"
    );
    private static final Set<String> SYNTHETIC = Set.of(
            "ZERO_VARIABLE", "ASSIGNMENT", "GOTO_LABEL", "CONSTANT_ASSIGNMENT",
            "JUMP_ZERO", "JUMP_EQUAL_CONSTANT", "JUMP_EQUAL_VARIABLE"
    );

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void addInstruction(SInstruction instruction) {
        instructions.add(instruction);
    }

    @Override
    public List<SInstruction> getInstructions() {
        return instructions;
    }

    @Override
    public boolean validate() {
        System.out.println("Please enter XML Path: ");
        java.util.Scanner sc = new java.util.Scanner(System.in);
        String line = sc.hasNextLine() ? sc.nextLine() : "";
        Path xmlPath = Path.of(line);

        // Accept spaces; just strip accidental wrapping quotes without touching internal spaces.
        String raw = xmlPath.toString().strip();
        if ((raw.startsWith("\"") && raw.endsWith("\"")) || (raw.startsWith("'") && raw.endsWith("'"))) {
            raw = raw.substring(1, raw.length() - 1);
        }
        Path p = Path.of(raw).normalize();

        if (!p.isAbsolute()) {
            System.err.println("Please provide a full (absolute) path to the XML file.");
            return false;
        }

        // Check that it exists, is a regular file, and is readable
        if (!Files.exists(p)) {
            System.err.println("XML file does not exist: " + p);
            return false;
        } else {
            if (!Files.isRegularFile(p)) {
                System.err.println("Path is not a regular file: " + p);
                return false;
            }
            if (!Files.isReadable(p)) {
                System.err.println("XML file is not readable: " + p);
                return false;
            }

            // Must be .xml (case-insensitive)
            String fn = (p.getFileName() != null) ? p.getFileName().toString() : "";
            if (!fn.toLowerCase(Locale.ROOT).endsWith(".xml")) {
                System.err.println("File must have a .xml extension. Got: " + fn);
                return false;
            }
            this.xmlPath = p;
            return true;
        }
    }


    @Override
    public int calculateMaxDegree() {
        // traverse all commands and find the maximum degree
        return 0;
    }

    @Override
    public int calculateCycles() {
        // traverse all commands and calculate cycles
        return 0;
    }

    @Override
    public Object load() throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        boolean bool;
        try (InputStream xmlFileInputStream = new FileInputStream(this.xmlPath.toFile())) {
            Document doc = builder.parse(xmlFileInputStream);
            doc.getDocumentElement().normalize();
            bool = validatexmlFile(doc);
        }
        if (bool) {
            return xmlPath;
        } else {
            return null;
        }
    }

    private boolean validatexmlFile(Document doc) {
        Element root = doc.getDocumentElement();
        if (root == null) {
            System.out.println("Empty XML document.");
            return false;
        }

        // checking if the name of the program is "S-program" mandatory
        if (!"S-Program".equals(root.getTagName())) {
            System.out.println("Root element must be <S-Program>, found <" + root.getTagName() + ">.");
            return false; // no point continuing without a valid root
        }

        // avoids a NullPointerException when you try to call .trim() on null
        //and always gives you a trimmed string (leading and trailing spaces removed).
        String progName = trimOrNull(root.getAttribute("name"));
        if (progName == null || progName.isEmpty()) {
            System.out.println("Attribute 'name' on <S-program> is mandatory and must not be empty.");
            return false;
        }

        // checking if <S-instructions> is nested under <S-program> only one time (mandatory)
        Element sInstructions = getSingleChild(root);
        if (sInstructions == null) {
            System.out.println("Missing mandatory <S-instructions> element under <S-program>.");
            return false;
        }

        // Validate each <S-Instruction> in <S-instructions>
        // Collect all <S-Instruction> children
        List<Element> all = childElements(sInstructions, "S-Instruction");
        // Validate each one
        boolean inst_valid;
        for (int i = 0; i < all.size(); i++) {
            inst_valid = validateInstructionShallow(all.get(i), i);
            if (!inst_valid) {
                return false;
            }
        }

        return true;
    }

    private static String trimOrNull(String s) {
        if (s == null) return null;
        return s.trim();
    }

    private static Element getSingleChild(Element parent) {
        NodeList nl = parent.getElementsByTagName("S-Instructions");
        // Ensure it's a direct child, not a deep descendant
        List<Element> direct = new ArrayList<>();
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (n.getParentNode() == parent && n.getNodeType() == Node.ELEMENT_NODE) {
                direct.add((Element) n);
            }
        }
        if (direct.isEmpty()) return null;
        if (direct.size() > 1) {
            // Multiple same-name direct children where only one is expected
            // The caller will decide if that's an error (we usually expect singletons)
            // Return the first to continue validation.
        }
        return direct.get(0);
    }

    private static List<Element> childElements(Element parent, String tagName) {
        NodeList nl = parent.getElementsByTagName(tagName);
        List<Element> elems = new ArrayList<>();
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                elems.add((Element) n);
            }
        }
        return elems;
    }

    private boolean validateInstructionShallow(Element instEl, int index) {
        boolean ok = true;
        String where = "S-Instruction[" + index + "]: ";

        // (1) Ensure this element does not contain nested S-Instruction elements
        List<Element> nested = childElements(instEl, "S-Instruction");
        if (!nested.isEmpty()) {
            System.out.println(where + "Must define exactly one instruction; nested <S-Instruction> found.");
            ok = false;
        }

        // (2) type attribute: mandatory, case-sensitive
        String type = instEl.hasAttribute("type") ? instEl.getAttribute("type").trim() : null;
        if (type == null || type.isEmpty()) {
            System.out.println(where + "Missing mandatory attribute 'type' (must be 'basic' or 'synthetic').");
            ok = false;
        } else if (!type.equals("basic") && !type.equals("synthetic")) {
            System.out.println(where + "Invalid type='" + type + "'. Allowed: 'basic' | 'synthetic' (case-sensitive).");
            ok = false;
        }

        // (3) name attribute: mandatory.
        String instrName = instEl.hasAttribute("name") ? instEl.getAttribute("name").trim() : null;
        if (instrName == null || instrName.isEmpty()) {
            System.out.println(where + "Missing mandatory attribute 'name'.");
            ok = false;
        } else if (!ALLOWED_NAMES.contains(instrName)) {
            System.out.println(where + "Unknown instruction name '" + instrName + "'. Allowed: " + ALLOWED_NAMES + ".");
            ok = false;
        }

        // (4) Optional: cross-check type matches classification (helps catch mismatches)
        if (ok && type != null && instrName != null && !type.isEmpty() && !instrName.isEmpty()) {
            if (type.equals("basic") && !BASIC.contains(instrName)) {
                System.out.println(where + "Instruction '" + instrName + "' is synthetic but type='basic' given.");
                ok = false;
            }
            if (type.equals("synthetic") && !SYNTHETIC.contains(instrName)) {
                System.out.println(where + "Instruction '" + instrName + "' is basic but type='synthetic' given.");
                ok = false;
            }
        }

        ok = validateVariableForInstruction(instEl, index) && ok;
        ok = validateLabelForInstruction(instEl, index) && ok;
        ok = validateArgsForInstruction(instEl, index, instrName) && ok;


        return ok;

    }

    private boolean validateVariableForInstruction(Element instEl, int index) {
        boolean ok = true;
        String where = "S-Instruction[" + index + "]: ";

        // Exactly one <S-Variable> expected
        List<Element> vars = childElements(instEl, "S-Variable");
        if (vars.isEmpty()) {
            System.out.println(where + "Missing mandatory <S-Variable> element.");
            return false;
        }
        if (vars.size() > 1) {
            System.out.println(where + "Multiple <S-Variable> elements found; expected exactly one.");
            ok = false;
        }

        // Validate content of the first one (continue even if there are extras to report all issues)
        Element varEl = vars.get(0);
        String raw = varEl.getTextContent();
        String val = (raw == null) ? "" : raw.trim(); // ignore leading/trailing spaces

        if (val.isEmpty()) {
            // Empty string is explicitly allowed
            return ok;
        }

        // No spaces allowed anywhere inside (they would have to be internal now)
        if (val.chars().anyMatch(Character::isWhitespace)) {
            System.out.println(where + "<S-Variable> must not contain spaces. Got: '" + val + "'");
            ok = false;
        }

        // Case-sensitive: only x|y|z + digits (e.g., x1, y23, z999)
        if (!val.matches("^[xyz][0-9]+$")) {
            System.out.println(where + "<S-Variable> must match ^[xyz][0-9]+$ (case-sensitive, no spaces). Got: '" + val + "'");
            ok = false;
        }

        return ok;
    }

    /**
     * Validate the optional <S-Label> child of a given <S-Instruction>.
     */
    private boolean validateLabelForInstruction(Element instEl, int index) {
        boolean ok = true;
        String where = "S-Instruction[" + index + "]: ";

        // Gather direct <S-Label> children
        List<Element> labels = childElements(instEl, "S-Label");
        if (labels.isEmpty()) {
            // Optional and absent → valid
            return true;
        }
        if (labels.size() > 1) {
            System.out.println(where + "Multiple <S-Label> elements found; expected at most one.");
            ok = false;
        }

        // Validate the first one (continue to report all errors if needed)
        Element labelEl = labels.get(0);
        String raw = labelEl.getTextContent();
        String val = (raw == null) ? "" : raw.trim(); // ignore leading/trailing spaces

        if (val.isEmpty()) {
            System.out.println(where + "<S-Label> must not be empty when present.");
            ok = false;
        } else {
            // No whitespace anywhere inside
            if (val.chars().anyMatch(Character::isWhitespace)) {
                System.out.println(where + "<S-Label> must not contain spaces. Got: '" + val + "'");
                ok = false;
            }
            // Must be 'L' + digits, case-sensitive
            if (!val.matches("^L[0-9]+$")) {
                System.out.println(where + "<S-Label> must match ^L[0-9]+$ (uppercase L followed by digits). Got: '" + val + "'");
                ok = false;
            }
        }

        return ok;
    }

    /**
     * Validate the <S-Instruction-Arguments> block for a given <S-Instruction> by opcode.
     */
    private boolean validateArgsForInstruction(Element instEl, int index, String instrName) {
        boolean ok = true;
        String where = "S-Instruction[" + index + "]: ";

        // Find direct containers
        List<Element> containers = childElements(instEl, "S-Instruction-Arguments");
        if (containers.size() > 1) {
            System.out.println(where + "Multiple <S-Instruction-Arguments> blocks found; expected at most one.");
            ok = false;
        }
        Element container = containers.isEmpty() ? null : containers.get(0);

        // Which instructions use an arguments block?
        boolean usesArgs =
                instrName.equals("GOTO_LABEL") ||
                        instrName.equals("ASSIGNMENT") ||
                        instrName.equals("CONSTANT_ASSIGNMENT") ||
                        instrName.equals("JUMP_ZERO") ||
                        instrName.equals("JUMP_EQUAL_CONSTANT") ||
                        instrName.equals("JUMP_EQUAL_VARIABLE") ||
                        instrName.equals("QUOTE_PROGRAM") ||          // add to ALLOWED_NAMES if not yet there
                        instrName.equals("JUMP_EQUAL_FUNCTION");      // add to ALLOWED_NAMES if not yet there

        if (!usesArgs && container != null) {
            System.out.println(where + "Unexpected <S-Instruction-Arguments> for instruction '" + instrName + "'.");
            ok = false;
            // keep validating to surface more issues
        }
        if (usesArgs && container == null) {
            System.out.println(where + "Missing <S-Instruction-Arguments> for instruction '" + instrName + "'.");
            return false;
        }
        if (container == null) return ok; // nothing more to check

        // Collect arguments
        List<Element> args = childElements(container, "S-Instruction-Argument");
        if (args.isEmpty()) {
            System.out.println(where + "<S-Instruction-Arguments> must contain at least one <S-Instruction-Argument>.");
            ok = false;
            return ok;
        }

        // Build a map name->value (after trimming)
        Map<String, String> argMap = new LinkedHashMap<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < args.size(); i++) {
            Element a = args.get(i);
            String whereArg = where + "Argument[" + i + "]: ";

            if (!a.hasAttribute("name")) {
                System.out.println(whereArg + "Missing mandatory attribute 'name'.");
                ok = false;
                continue;
            }
            if (!a.hasAttribute("value")) {
                System.out.println(whereArg + "Missing mandatory attribute 'value'.");
                ok = false;
                continue;
            }
            String name = a.getAttribute("name").trim();
            String value = a.getAttribute("value").trim();

            if (name.isEmpty()) {
                System.out.println(whereArg + "Attribute 'name' must not be empty.");
                ok = false;
            }
            if (value.isEmpty()) {
                System.out.println(whereArg + "Attribute 'value' must not be empty.");
                ok = false;
            }
            if (!seen.add(name)) {
                System.out.println(whereArg + "Duplicate argument name '" + name + "' in the same instruction.");
                ok = false;
            }
            if (!name.isEmpty()) {
                argMap.put(name, value);
            }
        }

        // Now enforce exact argument sets per opcode, and validate values
        switch (instrName) {

            case "GOTO_LABEL": {
                // name: gotoLabel → label
                ok &= requireOnly(where, argMap, Set.of("gotoLabel"));
                String v = argMap.get("gotoLabel");
                ok &= checkLabel(where, "gotoLabel", v);
                break;
            }

            case "ASSIGNMENT": {
                // name: assignedVariable → variable
                ok &= requireOnly(where, argMap, Set.of("assignedVariable"));
                String v = argMap.get("assignedVariable");
                ok &= checkVariable(where, "assignedVariable", v);
                break;
            }

            case "CONSTANT_ASSIGNMENT": {
                // name: constantValue → integer
                ok &= requireOnly(where, argMap, Set.of("constantValue"));
                String v = argMap.get("constantValue");
                ok &= checkInteger(where, "constantValue", v);
                break;
            }

            case "JUMP_ZERO": {
                // name: JZLabel → label
                ok &= requireOnly(where, argMap, Set.of("JZLabel"));
                String v = argMap.get("JZLabel");
                ok &= checkLabel(where, "JZLabel", v);
                break;
            }

            case "JUMP_EQUAL_CONSTANT": {
                // names: JEConstantLabel → label, constantValue → integer
                ok &= requireOnly(where, argMap, Set.of("JEConstantLabel", "constantValue"));
                ok &= checkLabel(where, "JEConstantLabel", argMap.get("JEConstantLabel"));
                ok &= checkInteger(where, "constantValue", argMap.get("constantValue"));
                break;
            }

            case "JUMP_EQUAL_VARIABLE": {
                // names: JEVariableLabel → label, variableName → variable
                ok &= requireOnly(where, argMap, Set.of("JEVariableLabel", "variableName"));
                ok &= checkLabel(where, "JEVariableLabel", argMap.get("JEVariableLabel"));
                ok &= checkVariable(where, "variableName", argMap.get("variableName"));
                break;
            }

            case "QUOTE_PROGRAM": {
                // names: functionName → identifier, functionArguments → non-empty string (commas allowed)
                ok &= requireOnly(where, argMap, Set.of("functionName", "functionArguments"));
                ok &= checkIdentifier(where, "functionName", argMap.get("functionName"));
                ok &= checkNonEmpty(where, "functionArguments", argMap.get("functionArguments"));
                break;
            }

            case "JUMP_EQUAL_FUNCTION": {
                // names: JEFunctionLabel → label, functionName → identifier, functionArguments → non-empty string
                ok &= requireOnly(where, argMap, Set.of("JEFunctionLabel", "functionName", "functionArguments"));
                ok &= checkLabel(where, "JEFunctionLabel", argMap.get("JEFunctionLabel"));
                ok &= checkIdentifier(where, "functionName", argMap.get("functionName"));
                ok &= checkNonEmpty(where, "functionArguments", argMap.get("functionArguments"));
                break;
            }

            default:
                // For any other instruction, having args was already flagged above.
                break;
        }

        return ok;
    }


    private boolean requireOnly(String where, Map<String, String> map, Set<String> expected) {
        boolean ok = true;
        // missing
        for (String need : expected) {
            if (!map.containsKey(need)) {
                System.out.println(where + "Missing required argument '" + need + "'.");
                ok = false;
            }
        }
        // extras
        for (String got : map.keySet()) {
            if (!expected.contains(got)) {
                System.out.println(where + "Unexpected argument '" + got + "'. Allowed: " + expected + ".");
                ok = false;
            }
        }
        return ok;
    }

    private boolean checkLabel(String where, String argName, String v) {
        if (v == null || v.isEmpty()) {
            System.out.println(where + argName + " must not be empty.");
            return false;
        }
        if (v.chars().anyMatch(Character::isWhitespace)) {
            System.out.println(where + argName + " must not contain spaces. Got: '" + v + "'");
            return false;
        }
        if (!v.matches("^L[0-9]+$")) {
            System.out.println(where + argName + " must match ^L[0-9]+$ (uppercase L + digits). Got: '" + v + "'");
            return false;
        }
        return true;
    }

    private boolean checkVariable(String where, String argName, String v) {
        if (v == null || v.isEmpty()) {
            System.out.println(where + argName + " must not be empty.");
            return false;
        }
        if (v.chars().anyMatch(Character::isWhitespace)) {
            System.out.println(where + argName + " must not contain spaces. Got: '" + v + "'");
            return false;
        }
        if (!v.matches("^[xyz][0-9]+$")) {
            System.out.println(where + argName + " must match ^[xyz][0-9]+$ (x|y|z + digits). Got: '" + v + "'");
            return false;
        }
        return true;
    }

    private boolean checkInteger(String where, String argName, String v) {
        if (v == null || v.isEmpty()) {
            System.out.println(where + argName + " must not be empty.");
            return false;
        }
        if (!v.matches("^-?\\d+$")) {
            System.out.println(where + argName + " must be an integer (e.g., -3, 0, 17). Got: '" + v + "'");
            return false;
        }
        return true;
    }

    private boolean checkIdentifier(String where, String argName, String v) {
        if (v == null || v.isEmpty()) {
            System.out.println(where + argName + " must not be empty.");
            return false;
        }
        if (!v.matches("^[A-Za-z_][A-Za-z0-9_]*$")) {
            System.out.println(where + argName + " must be an identifier ([A-Za-z_][A-Za-z0-9_]*). Got: '" + v + "'");
            return false;
        }
        return true;
    }

    private boolean checkNonEmpty(String where, String argName, String v) {
        if (v == null || v.trim().isEmpty()) {
            System.out.println(where + argName + " must not be empty.");
            return false;
        }
        return true;
    }


}
