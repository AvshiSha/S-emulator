package semulator.program;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import semulator.instructions.*;
import semulator.label.FixedLabel;
import semulator.label.Label;
import semulator.label.LabelImpl;
import semulator.variable.Variable;
import semulator.variable.VariableImpl;
import semulator.variable.VariableType;

import semulator.instructions.ZeroVariableInstruction;
import semulator.instructions.JumpZeroInstruction;
import semulator.instructions.JumpEqualConstantInstruction;
import semulator.instructions.JumpEqualVariableInstruction;
import semulator.instructions.GotoLabelInstruction;
import semulator.instructions.AssignConstantInstruction;
import semulator.instructions.AssignVariableInstruction;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * בניית תכנית מ־XML אל תוך List<SInstruction>.
 * בשלב זה מכוסות הפקודות הבסיסיות: NEUTRAL, INCREASE, DECREASE, JUMP_NOT_ZERO.
 */
public class SProgramImpl implements SProgram {

    private final String name;
    private final List<SInstruction> instructions;
    private Path xmlPath;

    public SProgramImpl(String name) {
        this.name = name;
        this.instructions = new ArrayList<>();
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

        String fn = (p.getFileName() != null) ? p.getFileName().toString() : "";
        if (!fn.toLowerCase(Locale.ROOT).endsWith(".xml")) {
            System.err.println("File must have a .xml extension. Got: " + fn);
            return false;
        }

        this.xmlPath = p;
        return true;
    }


    @Override
    public int calculateMaxDegree() {
        // TODO: לפי דרישות המטלה בהמשך
        return 0;
    }

    @Override
    public int calculateCycles() {
        // TODO: לפי דרישות המטלה בהמשך
        return 0;
    }

    @Override
    public String expand(int level){
        return "";        // TODO: לפי דרישות המטלה בהמשך
    }

    @Override
    public Object load() throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc;
        boolean ok;
        try (InputStream xmlFileInputStream = new FileInputStream(this.xmlPath.toFile())) {
            doc = builder.parse(xmlFileInputStream);
            doc.getDocumentElement().normalize();
            ok = validateXmlFile(doc); // שומר את שם המתודה המקורי אצלך
        }
        if (!ok) {
            return null;
        }

        // חדש: בניית ההוראות אל תוך הזיכרון
        buildInMemory(doc);
        return xmlPath;
    }

    /** ממיר XML מאומת לרשימת הוראות בזיכרון. */
    private void buildInMemory(Document doc) {
        instructions.clear();

        Element root = doc.getDocumentElement();
        Element sInstructions = getSingleChild(root, "S-Instructions");
        if (sInstructions == null) return; // הגנה נוספת

        // מאגר לייבלים כדי לייצר מופע יחיד לכל שם (L1, L2,...)
        Map<String, Label> labelPool = new HashMap<>();

        List<Element> all = childElements(sInstructions, "S-Instruction");
        for (Element instEl : all) {
            String name = instEl.getAttribute("name").trim();     // e.g. DECREASE / INCREASE / JUMP_NOT_ZERO / NEUTRAL
            String varText = textOfSingle(instEl, "S-Variable");  // e.g. x1 / y / z3
            Variable var = parseVariable(varText);

            // לייבל שמוגדר על ההוראה (אופציונלי)
            String lblText = textOfOptional(instEl, "S-Label");   // e.g. L1
            Label selfLabel = (lblText == null || lblText.isBlank())
                    ? FixedLabel.EMPTY
                    : getOrCreateLabel(lblText, labelPool);

            // ארגומנטים (למשל יעד קפיצה ב-JNZ)
            Map<String, String> args = readArgs(instEl);

            switch (name) {
                case "INCREASE" -> {
                    if (selfLabel != FixedLabel.EMPTY) instructions.add(new IncreaseInstruction(var, selfLabel));
                    else instructions.add(new IncreaseInstruction(var));
                }
                case "DECREASE" -> {
                    if (selfLabel != FixedLabel.EMPTY) instructions.add(new DecreaseInstruction(var, selfLabel));
                    else instructions.add(new DecreaseInstruction(var));
                }
                case "NEUTRAL" -> {
                    instructions.add(new NoOpInstruction(var));
                }
                case "JUMP_NOT_ZERO" -> {
                    String targetName = args.get("JNZLabel");                 // badic.xml
                    Label target = parseLabel(targetName, labelPool);
                    if (selfLabel != FixedLabel.EMPTY) instructions.add(new JumpNotZeroInstruction(var, selfLabel, target));
                    else instructions.add(new JumpNotZeroInstruction(var, target));
                }

                // ===== synthetic.xml cases =====
                case "ZERO_VARIABLE" -> {
                    if (selfLabel != FixedLabel.EMPTY) instructions.add(new ZeroVariableInstruction(var, selfLabel));
                    else instructions.add(new ZeroVariableInstruction(var));
                }
                case "ASSIGNMENT" -> {                                        // <... name="assignedVariable" value="x2"/>
                    Variable src = parseVariable(args.get("assignedVariable"));
                    if (selfLabel != FixedLabel.EMPTY) instructions.add(new AssignVariableInstruction(var, src, selfLabel));
                    else instructions.add(new AssignVariableInstruction(var, src));
                }
                case "CONSTANT_ASSIGNMENT" -> {                                // <... name="constantValue" value="5"/>
                    int c = Integer.parseInt(args.get("constantValue"));
                    if (selfLabel != FixedLabel.EMPTY) instructions.add(new AssignConstantInstruction(var, c, selfLabel));
                    else instructions.add(new AssignConstantInstruction(var, c));
                }
                case "JUMP_ZERO" -> {                                          // <... name="JZLabel" value="EXIT"/>
                    Label target = parseLabel(args.get("JZLabel"), labelPool);
                    if (selfLabel != FixedLabel.EMPTY) instructions.add(new JumpZeroInstruction(var, selfLabel, target));
                    else instructions.add(new JumpZeroInstruction(var, target));
                }
                case "JUMP_EQUAL_CONSTANT" -> {                                // JEConstantLabel + constantValue
                    Label target = parseLabel(args.get("JEConstantLabel"), labelPool);
                    int c = Integer.parseInt(args.get("constantValue"));
                    if (selfLabel != FixedLabel.EMPTY) instructions.add(new JumpEqualConstantInstruction(var, selfLabel, c, target));
                    else instructions.add(new JumpEqualConstantInstruction(var, c, target));
                }
                case "JUMP_EQUAL_VARIABLE" -> {                                // JEVariableLabel + variableName
                    Label target = parseLabel(args.get("JEVariableLabel"), labelPool);
                    Variable other = parseVariable(args.get("variableName"));
                    if (selfLabel != FixedLabel.EMPTY) instructions.add(new JumpEqualVariableInstruction(var, selfLabel, other, target));
                    else instructions.add(new JumpEqualVariableInstruction(var, other, target));
                }
                case "GOTO_LABEL" -> {                                         // gotoLabel
                    Label target = parseLabel(args.get("gotoLabel"), labelPool);
                    if (selfLabel != FixedLabel.EMPTY) instructions.add(new GotoLabelInstruction(selfLabel, target));
                    else instructions.add(new GotoLabelInstruction(target));
                }

                default -> { /* לא בונים הוראה לא מוכרת */ }
            }

        }
    }


    /* =====================  עזרי XML וכללי  ===================== */

    private static String textOfSingle(Element parent, String tag) {
        List<Element> lst = childElements(parent, tag);
        if (lst.isEmpty()) return "";
        return trimOrNull(lst.get(0).getTextContent());
    }

    private static String textOfOptional(Element parent, String tag) {
        List<Element> lst = childElements(parent, tag);
        if (lst.isEmpty()) return null;
        return trimOrNull(lst.get(0).getTextContent());
    }

    private static Map<String, String> readArgs(Element instEl) {
        Map<String, String> map = new HashMap<>();
        Element args = getSingleChild(instEl, "S-Instruction-Arguments");
        if (args == null) return map;
        List<Element> items = childElements(args, "S-Instruction-Argument");
        for (Element e : items) {
            String n = e.getAttribute("name");
            String v = e.getAttribute("value");
            if (n != null && !n.isBlank()) {
                map.put(n, v == null ? "" : v.trim());
            }
        }
        return map;
    }

    private static Variable parseVariable(String txt) {
        String s = (txt == null) ? "" : txt.trim();
        if (s.isEmpty() || s.equals("y")) {
            return Variable.RESULT;
        }
        // xN / zN
        char kind = s.charAt(0);
        int idx = Integer.parseInt(s.substring(1));
        if (kind == 'x') {
            return new VariableImpl(VariableType.INPUT, idx);
        } else if (kind == 'z') {
            return new VariableImpl(VariableType.WORK, idx);
        } else {
            return null;
        }
    }

    private static Label getOrCreateLabel(String name, Map<String, Label> pool) {
        return pool.computeIfAbsent(name, n -> {
            // n = "L12" → id=12, address=0 (placeholder)
            int id = Integer.parseInt(n.substring(1));
            return new LabelImpl(id, 0);
        });
    }

    /* =====================  הוולידציה המקורית שלך  ===================== */

    private boolean validateXmlFile(Document doc) {
        Element root = doc.getDocumentElement();
        if (root == null) {
            System.out.println("Empty XML document.");
            return false;
        }

        // checking if the name of the program is "S-program" mandatory
        if (!"S-Program".equals(root.getTagName())) {
            System.out.println("Root element must be <S-Program>, found <" + root.getTagName() + ">.");
            return false;
        }

        // avoids a NullPointerException when you try to call .trim() on null
        //and always gives you a trimmed string (leading and trailing spaces removed).
        String progName = trimOrNull(root.getAttribute("name"));
        if (progName == null || progName.isEmpty()) {
            System.out.println("Attribute 'name' on <S-program> is mandatory and must not be empty.");
            return false;
        }

        Element sInstructions = getSingleChild(root, "S-Instructions");
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
            if (!validateInstructionShallow(all.get(i), i)) {
                return false;
            }
        }

        return true;
    }

    private static String trimOrNull(String s) {
        if (s == null) return null;
        return s.trim();
    }

    private static Element getSingleChild(Element parent, String tagName) {
        NodeList nl = parent.getElementsByTagName(tagName);
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
            } else if (type.equals("synthetic") && !SYNTHETIC.contains(instrName)) {
                System.out.println(where + "Instruction '" + instrName + "' is basic but type='synthetic' given.");
                ok = false;
            }
        }

        ok = validateVariableForInstruction(instEl, index) && ok;
        ok = validateLabelForInstruction(instEl, index) && ok;

        return ok;
    }

    private boolean validateVariableForInstruction(Element instEl, int index) {
        boolean ok = true;
        String where = "S-Instruction[" + index + "]: ";

        List<Element> vars = childElements(instEl, "S-Variable");
        if (vars.isEmpty()) {
            System.out.println(where + "Missing mandatory <S-Variable> element.");
            return false;
        }
        if (vars.size() > 1) {
            System.out.println(where + "Multiple <S-Variable> elements found; expected exactly one.");
            ok = false;
        }

        Element varEl = vars.get(0);
        String raw = varEl.getTextContent();
        String val = (raw == null) ? "" : raw.trim();

        if (val.isEmpty()) {
            return ok; // ריק מותר (לפי ההערות שלך)
        }
        if (val.chars().anyMatch(Character::isWhitespace)) {
            System.out.println(where + "<S-Variable> must not contain spaces. Got: '" + val + "'");
            ok = false;
        }
        if (!(val.equals("y") || val.matches("^[xz][0-9]+$"))) {
            System.out.println(where + "<S-Variable> must be 'y' or match ^[xz][0-9]+$ (case-sensitive, no spaces). Got: '" + val + "'");
            ok = false;
        }

        return ok;
    }

    private boolean validateLabelForInstruction(Element instEl, int index) {
        boolean ok = true;
        String where = "S-Instruction[" + index + "]: ";

        List<Element> labels = childElements(instEl, "S-Label");
        if (labels.isEmpty()) {
            return true; // אופציונלי
        }
        if (labels.size() > 1) {
            System.out.println(where + "Multiple <S-Label> elements found; expected at most one.");
            ok = false;
        }

        Element labelEl = labels.get(0);
        String raw = labelEl.getTextContent();
        String val = (raw == null) ? "" : raw.trim();

        if (val.isEmpty()) {
            System.out.println(where + "<S-Label> must not be empty when present.");
            ok = false;
        } else {
            if (val.chars().anyMatch(Character::isWhitespace)) {
                System.out.println(where + "<S-Label> must not contain spaces. Got: '" + val + "'");
                ok = false;
            }
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
                        instrName.equals("JUMP_NOT_ZERO") ||
                        instrName.equals("ASSIGNMENT") ||
                        instrName.equals("CONSTANT_ASSIGNMENT") ||
                        instrName.equals("JUMP_ZERO") ||
                        instrName.equals("JUMP_EQUAL_CONSTANT") ||
                        instrName.equals("JUMP_EQUAL_VARIABLE") ||
                        instrName.equals("QUOTE_PROGRAM") ||
                        instrName.equals("JUMP_EQUAL_FUNCTION");
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

    private static Label parseLabel(String name, Map<String, Label> pool) {
        if (name == null || name.isBlank()) return FixedLabel.EMPTY;
        if ("EXIT".equals(name)) return FixedLabel.EXIT;
        // מצפה ל־L\d+
        return pool.computeIfAbsent(name, n -> new LabelImpl(Integer.parseInt(n.substring(1)), 0));
    }

}
