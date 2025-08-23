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

    private static final Set<String> LABEL_TARGET_ARGS = Set.of(
            "JNZLabel",          // JUMP_NOT_ZERO
            "JZLabel",           // JUMP_ZERO
            "JEConstantLabel",   // JUMP_EQUAL_CONSTANT
            "JEVariableLabel",   // JUMP_EQUAL_VARIABLE
            "gotoLabel"          // GOTO_LABEL
            // Add more here if new jump types are introduced
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
    }


    @Override
    public int calculateMaxDegree() {
        // Fixed degrees under the current expansion definitions
        final Map<String, Integer> DEGREE_BY_OPCODE = Map.ofEntries(
                Map.entry("NEUTRAL", 0),
                Map.entry("INCREASE", 0),
                Map.entry("DECREASE", 0),
                Map.entry("JUMP_NOT_ZERO", 0),
                Map.entry("ZERO_VARIABLE", 1),
                Map.entry("GOTO_LABEL", 1),
                Map.entry("CONSTANT_ASSIGNMENT", 1),
                Map.entry("JUMP_ZERO", 1),
                Map.entry("JUMP_EQUAL_CONSTANT", 1),
                Map.entry("JUMP_EQUAL_VARIABLE", 1),

                Map.entry("ASSIGNMENT", 2)
                // "QUOTE_PROGRAM", "JUMP_EQUAL_FUNCTION" are intentionally not included here.
        );

        int max = 0;
        for (SInstruction ins : instructions) {
            Integer d = DEGREE_BY_OPCODE.get(ins.getName());
            if (d != null && d > max) {
                max = d;
            }
        }
        return max;
    }

    @Override
    public int calculateCycles() {
        // TODO: לפי דרישות המטלה בהמשך
        return 0;
    }

    @Override
    public ExpansionResult expandToDegree(int degree) {
        // Start from the current program as generation 0
        List<SInstruction> cur = new ArrayList<>(this.instructions);

        // We’ll accumulate lineage across steps:
        // parentMap links a newly created instruction to the instruction it replaced/expanded from.
        Map<SInstruction, SInstruction> parentMap = new IdentityHashMap<>();

        for (int step = 0; step < degree; step++) {
            // Expand once
            List<SInstruction> next = new ArrayList<>(cur.size() * 2);

            for (SInstruction ins : cur) {
                if (isBasic(ins)) {
                    // basic stays as-is for this step
                    next.add(ins);
                } else {
                    // Replace ins with its expansion children.
                    // TODO: call your real expander here:
                    List<SInstruction> children = expandOne(ins);
                    // Track immediate parent for lineage printing
                    for (SInstruction ch : children) {
                        parentMap.put(ch, ins);
                        next.add(ch);
                    }
                }
            }
            cur = next;
        }

        // Number the final snapshot 1..N for PrettyPrinter
        Map<SInstruction, Integer> lineNo = new IdentityHashMap<>();
        for (int i = 0; i < cur.size(); i++) {
            lineNo.put(cur.get(i), i + 1);
        }

        return new ExpansionResult(cur, parentMap, lineNo);
    }

    private boolean isBasic(SInstruction in) {
        String name = in.getName();
        // You already keep BASIC/SYNTHETIC sets in this class:
        return BASIC.contains(name);
    }

    // Expand one instruction by your exact rules (ZERO_VARIABLE, ASSIGNMENT, etc.)
    private List<SInstruction> expandOne(SInstruction in) {
        // Replace the switch below with your actual expansion code
        // that builds the DEC/INC/JNZ sequences and fresh labels/temps as needed.
        switch (in.getName()) {
            case "ZERO_VARIABLE":
                return List.of(new DecreaseInstruction(in.getVariable()), new JumpNotZeroInstruction(in.getVariable(), in.getLabel()));
            case "ASSIGNMENT":
                AssignVariableInstruction a = (AssignVariableInstruction) in;
                var V = a.getVariable();   // target
                var Vp = a.getSource();     // source
                var z1 = new VariableImpl(VariableType.WORK, 1);
                // loop labels
                var AL1 = new LabelImpl("A_L1"); // drain V' -> z1
                var AL2 = new LabelImpl("A_L2"); // restore from z1 -> V', V
                var AL3 = new LabelImpl("A_L3");

                var out = new ArrayList<SInstruction>();

                out.add(new ZeroVariableInstruction(V)); // V --> 0
                out.add(new JumpNotZeroInstruction(Vp, AL1)); // if Vp != 0 goto AL1
                out.add(new GotoLabelInstruction(AL3)); // goto AL3
                out.add(new DecreaseInstruction(Vp, AL1)); // Vp --> Vp - 1
                out.add(new IncreaseInstruction(z1)); // z1 --> z1 - 1
                out.add(new JumpNotZeroInstruction(Vp, AL1));  // IF Vp != 0 goto AL1
                out.add(new JumpNotZeroInstruction(z1, AL2)); // IF z1 != 0 goto AL2
                out.add(new DecreaseInstruction(z1, AL2)); // z1 --> z1 - 1
                out.add(new IncreaseInstruction(V)); // V --> V + 1
                out.add(new IncreaseInstruction(Vp)); // Vp --> Vp + 1
                out.add(new JumpNotZeroInstruction(z1, AL2)); // IF z1 != 0 goto AL2
                out.add(new NoOpInstruction(V)); // V --> V ??
                return out;
            case "GOTO_LABEL":
                var z2 = new VariableImpl(VariableType.WORK, 2);
                var out2 = new ArrayList<SInstruction>();
                out2.add(new IncreaseInstruction(z2)); // z2 --> z2 + 1
                out2.add(new JumpNotZeroInstruction(z2, in.getLabel())); // IF z2 != 0 goto in.getLabel()
                return out2;
            case "CONSTANT_ASSIGNMENT":
                AssignConstantInstruction a2 = (AssignConstantInstruction) in;
                var out3 = new ArrayList<SInstruction>();

                out3.add(new ZeroVariableInstruction(in.getVariable())); // V --> 0
                for (int i = 0; i < a2.getConstant(); i++) {
                    out3.add(new IncreaseInstruction(in.getVariable())); // V --> V + 1 K times
                }
                return out3;
            case "JUMP_ZERO":
            case "JUMP_EQUAL_CONSTANT":
            case "JUMP_EQUAL_VARIABLE":
                // TODO: return expansion sequence for each case
                return List.of(in); // TEMP: remove when you plug real expansions
            default:
                return List.of(in);
        }
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

    /**
     * ממיר XML מאומת לרשימת הוראות בזיכרון.
     */
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
                    if (selfLabel != FixedLabel.EMPTY)
                        instructions.add(new JumpNotZeroInstruction(var, selfLabel, target));
                    else instructions.add(new JumpNotZeroInstruction(var, target));
                }

                // ===== synthetic.xml cases =====
                case "ZERO_VARIABLE" -> {
                    if (selfLabel != FixedLabel.EMPTY)
                        instructions.add(new ZeroVariableInstruction(var, selfLabel));
                    else instructions.add(new ZeroVariableInstruction(var));
                }
                case "ASSIGNMENT" -> {                                        // <... name="assignedVariable" value="x2"/>
                    Variable src = parseVariable(args.get("assignedVariable"));
                    if (selfLabel != FixedLabel.EMPTY)
                        instructions.add(new AssignVariableInstruction(var, src, selfLabel));
                    else instructions.add(new AssignVariableInstruction(var, src));
                }
                case "CONSTANT_ASSIGNMENT" -> {                                // <... name="constantValue" value="5"/>
                    int c = Integer.parseInt(args.get("constantValue"));
                    if (selfLabel != FixedLabel.EMPTY)
                        instructions.add(new AssignConstantInstruction(var, c, selfLabel));
                    else instructions.add(new AssignConstantInstruction(var, c));
                }
                case "JUMP_ZERO" -> {                                          // <... name="JZLabel" value="EXIT"/>
                    Label target = parseLabel(args.get("JZLabel"), labelPool);
                    if (selfLabel != FixedLabel.EMPTY)
                        instructions.add(new JumpZeroInstruction(var, selfLabel, target));
                    else instructions.add(new JumpZeroInstruction(var, target));
                }
                case "JUMP_EQUAL_CONSTANT" -> {                                // JEConstantLabel + constantValue
                    Label target = parseLabel(args.get("JEConstantLabel"), labelPool);
                    int c = Integer.parseInt(args.get("constantValue"));
                    if (selfLabel != FixedLabel.EMPTY)
                        instructions.add(new JumpEqualConstantInstruction(var, selfLabel, c, target));
                    else instructions.add(new JumpEqualConstantInstruction(var, c, target));
                }
                case "JUMP_EQUAL_VARIABLE" -> {                                // JEVariableLabel + variableName
                    Label target = parseLabel(args.get("JEVariableLabel"), labelPool);
                    Variable other = parseVariable(args.get("variableName"));
                    if (selfLabel != FixedLabel.EMPTY)
                        instructions.add(new JumpEqualVariableInstruction(var, selfLabel, other, target));
                    else instructions.add(new JumpEqualVariableInstruction(var, other, target));
                }
                case "GOTO_LABEL" -> {                                         // gotoLabel
                    Label target = parseLabel(args.get("gotoLabel"), labelPool);
                    if (selfLabel != FixedLabel.EMPTY)
                        instructions.add(new GotoLabelInstruction(selfLabel, target));
                    else instructions.add(new GotoLabelInstruction(target));
                }

                default -> { /* לא בונים הוראה לא מוכרת */ }
            }

        }
    }

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

        return validateLabelReferences(sInstructions);
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
        String where = "S-Instruction[" + (index + 1) + "]: ";

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
        String where = "S-Instruction[" + (index + 1) + "]: ";

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
        String where = "S-Instruction[" + (index + 1) + "]: ";

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
        String where = "S-Instruction[" + (index + 1) + "]: ";

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
        }
        return ok;
    }

    private static Label parseLabel(String name, Map<String, Label> pool) {
        if (name == null || name.isBlank()) return FixedLabel.EMPTY;
        if ("EXIT".equals(name)) return FixedLabel.EXIT;
        // מצפה ל־L\d+
        return pool.computeIfAbsent(name, n -> new LabelImpl(Integer.parseInt(n.substring(1)), 0));
    }

    private boolean validateLabelReferences(Element sInstructions) {
        // Collect declared labels (from <S-Label> on lines)
        Set<String> declared = new HashSet<>();
        List<Element> all = childElements(sInstructions, "S-Instruction");
        for (Element instEl : all) {
            List<Element> lbls = childElements(instEl, "S-Label");
            if (!lbls.isEmpty()) {
                String raw = lbls.get(0).getTextContent();
                String val = (raw == null) ? "" : raw.trim();
                if (!val.isEmpty()) {
                    declared.add(val);
                }
            }
        }

        boolean ok = true;

        // Collect and check referenced labels from arguments
        for (int i = 0; i < all.size(); i++) {
            Element instEl = all.get(i);
            String where = "S-Instruction[" + (i + 1) + "]: ";

            String instrName = instEl.hasAttribute("name") ? instEl.getAttribute("name").trim() : "";

            // Find the (optional) <S-Instruction-Arguments> block
            Element argsBlock = getSingleChild(instEl, "S-Instruction-Arguments");
            if (argsBlock == null) continue; // nothing to check

            List<Element> args = childElements(argsBlock, "S-Instruction-Argument");
            for (Element arg : args) {
                String n = arg.getAttribute("name");
                String v = arg.getAttribute("value");
                if (n == null) n = "";
                if (v == null) v = "";
                n = n.trim();
                v = v.trim();

                if (!LABEL_TARGET_ARGS.contains(n)) {
                    continue; // not a label target argument
                }
                if (v.isEmpty()) {
                    // Already covered by other checks usually; still helpful to surface
                    System.out.println(where + "Label argument '" + n + "' for instruction '" + instrName + "' must not be empty.");
                    ok = false;
                    continue;
                }

                if ("EXIT".equals(v)) {
                    // EXIT is a virtual sink, not a real line label.
                    continue;
                }

                // Ensure it is syntactically an L# label (you already enforce this on <S-Label>).
                if (!v.matches("^L[0-9]+$")) {
                    System.out.println(where + "Label argument '" + n + "' must match ^L[0-9]+$ (got '" + v + "').");
                    ok = false;
                    continue;
                }

                // Cross-reference: does this referenced label appear anywhere as a declared line label?
                if (!declared.contains(v)) {
                    System.out.println(where + "References label '" + v + "' via argument '" + n +
                            "' but no such <S-Label> exists in the program.");
                    ok = false;
                }
            }
        }

        return ok;
    }

}
