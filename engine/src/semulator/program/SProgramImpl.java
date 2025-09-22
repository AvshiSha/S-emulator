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
    private final Map<String, List<SInstruction>> functions = new HashMap<>();

    private final Set<String> baseUsedLabelNames = new HashSet<>();
    private final Set<String> baseUsedVarNames = new HashSet<>();

    public SProgramImpl(String name) {
        this.name = name;
        this.instructions = new ArrayList<>();
    }

    private static final Set<String> ALLOWED_NAMES = Set.of(
            "NEUTRAL", "INCREASE", "DECREASE", "JUMP_NOT_ZERO",
            "ZERO_VARIABLE", "ASSIGNMENT", "GOTO_LABEL", "CONSTANT_ASSIGNMENT",
            "JUMP_ZERO", "JUMP_EQUAL_CONSTANT", "JUMP_EQUAL_VARIABLE", "QUOTE_PROGRAM", "JUMP_EQUAL_FUNCTION", "QUOTE");

    private static final Set<String> BASIC = Set.of(
            "NEUTRAL", "INCREASE", "DECREASE", "JUMP_NOT_ZERO");
    private static final Set<String> SYNTHETIC = Set.of(
            "ZERO_VARIABLE", "ASSIGNMENT", "GOTO_LABEL", "CONSTANT_ASSIGNMENT",
            "JUMP_EQUAL_CONSTANT", "JUMP_EQUAL_VARIABLE", "QUOTE", "JUMP_EQUAL_FUNCTION", "IFZ", "JUMP_ZERO");

    private static final Set<String> LABEL_TARGET_ARGS = Set.of(
            "JNZLabel",
            "JZLabel",
            "JEConstantLabel",
            "JEVariableLabel",
            "gotoLabel");

    // Fixed degrees under the current expansion definitions
    private static final Map<String, Integer> DEGREE_BY_OPCODE = Map.ofEntries(
            Map.entry("NEUTRAL", 0),
            Map.entry("INCREASE", 0),
            Map.entry("DECREASE", 0),
            Map.entry("JUMP_NOT_ZERO", 0),
            Map.entry("ZERO", 1),
            Map.entry("GOTO", 1),
            Map.entry("ASSIGN", 2),
            Map.entry("ASSIGNC", 2),
            Map.entry("IFZ", 2),
            Map.entry("IFEQC", 3),
            Map.entry("IFEQV", 3),
            Map.entry("QUOTE", 2),
            Map.entry("JUMP_EQUAL_FUNCTION", 2));

    private static final class InstrNode {
        final SInstruction ins;
        final int rowNumber;

        InstrNode(SInstruction ins, int rowNumber) {
            this.ins = ins;
            this.rowNumber = rowNumber;
        }
    }

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

    public Map<String, List<SInstruction>> getFunctions() {
        return functions;
    }

    @Override
    public String validate(Path xmlPath) {

        // Accept spaces; just strip accidental wrapping quotes without touching
        // internal spaces.
        String raw = xmlPath.toString().strip();
        if ((raw.startsWith("\"") && raw.endsWith("\"")) || (raw.startsWith("'") && raw.endsWith("'"))) {
            raw = raw.substring(1, raw.length() - 1);
        }
        Path p = Path.of(raw).normalize();

        if (!p.isAbsolute()) {
            return "Please provide a full (absolute) path to the XML file.";
        }

        // Check that it exists, is a regular file, and is readable
        if (!Files.exists(p)) {
            return "XML file does not exist: " + p;
        } else {
            if (!Files.isRegularFile(p)) {
                return "Path is not a regular file: " + p;
            }
            if (!Files.isReadable(p)) {
                return "XML file is not readable: " + p;
            }

            String fn = (p.getFileName() != null) ? p.getFileName().toString() : "";
            if (!fn.toLowerCase(Locale.ROOT).endsWith(".xml")) {
                return "File must have a .xml extension. Got: " + fn;
            }

            this.xmlPath = p;
            return "Valid";
        }
    }

    @Override
    public int calculateMaxDegree() {
        // Use the new expression-based approach
        return deg_prog();
    }

    /**
     * Calculate the maximum degree of a program using the new expression-based
     * approach.
     * deg_prog(P) = max degree over all instructions in program P.
     */
    private int deg_prog() {
        int maxDegree = 0;

        // Calculate degree for main program instructions
        for (SInstruction instruction : instructions) {
            int instructionDegree = deg_inst(instruction);
            if (instructionDegree > maxDegree) {
                maxDegree = instructionDegree;
            }
        }

        // Calculate degree for all function instructions
        for (Map.Entry<String, List<SInstruction>> functionEntry : functions.entrySet()) {
            String functionName = functionEntry.getKey();
            List<SInstruction> functionInstructions = functionEntry.getValue();

            for (SInstruction instruction : functionInstructions) {
                int instructionDegree = deg_inst(instruction);
                if (instructionDegree > maxDegree) {
                    maxDegree = instructionDegree;
                }
            }
        }

        return maxDegree;
    }

    /**
     * Calculate the degree of a function.
     * deg_func(F) = max degree over all instructions in function F.
     */
    private int deg_func(String functionName, Set<String> visitedFunctions) {
        // Prevent infinite recursion
        if (visitedFunctions.contains(functionName)) {
            return 1; // Return safe default for circular dependencies
        }

        if (!functions.containsKey(functionName)) {
            return 1; // Return safe default for missing functions
        }

        visitedFunctions.add(functionName);

        List<SInstruction> functionInstructions = functions.get(functionName);
        int maxDegree = 0;

        for (SInstruction instruction : functionInstructions) {
            int instructionDegree = deg_inst(instruction);
            if (instructionDegree > maxDegree) {
                maxDegree = instructionDegree;
            }
        }

        visitedFunctions.remove(functionName);
        return maxDegree;
    }

    /**
     * Calculate the degree of an instruction.
     * Basic instructions: degree = 0
     * QUOTE: degree = deg_expr(Q)
     * JUMP_EQUAL_FUNCTION: degree = deg_expr(Q)
     * Other synthetic instructions: use their known fixed degrees
     */
    private int deg_inst(SInstruction instruction) {
        String instructionName = instruction.getName();

        // Basic instructions have degree 0
        if (BASIC.contains(instructionName)) {
            return 0;
        }

        // Handle QUOTE instructions
        if (instruction instanceof QuoteInstruction) {
            QuoteInstruction quote = (QuoteInstruction) instruction;
            return deg_expr(quote.getFunctionName(), quote.getFunctionArguments(), new HashSet<>());
        }

        // Handle JUMP_EQUAL_FUNCTION instructions
        if (instruction instanceof JumpEqualFunctionInstruction) {
            JumpEqualFunctionInstruction jef = (JumpEqualFunctionInstruction) instruction;
            return deg_expr(jef.getFunctionName(), jef.getFunctionArguments(), new HashSet<>()) + 1;
        }

        // For other synthetic instructions, use their known fixed degrees
        Integer fixedDegree = DEGREE_BY_OPCODE.get(instructionName);
        return (fixedDegree != null) ? fixedDegree : 0;
    }

    /**
     * Calculate the degree of an expression.
     * If E is a variable/constant → deg_expr(E) = 0
     * If E = (f, a1, a2, ..., ak) → deg_expr(E) = 1 + max(deg_func(f), max_i
     * deg_expr(ai))
     */
    private int deg_expr(String functionName, List<FunctionArgument> arguments, Set<String> visitedFunctions) {
        // Calculate degree of the function body
        int functionDegree = deg_func(functionName, visitedFunctions);

        // Calculate maximum degree of arguments
        int maxArgumentDegree = 0;
        for (FunctionArgument arg : arguments) {
            int argDegree = deg_expr(arg, visitedFunctions);
            if (argDegree > maxArgumentDegree) {
                maxArgumentDegree = argDegree;
            }
        }

        // Expression degree = 1 + max(function degree, max argument degree)
        return 1 + Math.max(functionDegree, maxArgumentDegree);
    }

    /**
     * Calculate the degree of a function argument (which can be a variable or
     * function call).
     */
    private int deg_expr(FunctionArgument argument, Set<String> visitedFunctions) {
        if (argument.isFunctionCall()) {
            // This is a nested function call - calculate its degree
            FunctionCall call = argument.asFunctionCall();
            return deg_expr(call.getFunctionName(), call.getArguments(), visitedFunctions);
        } else {
            // This is a simple variable or constant - degree is 0
            return 0;
        }
    }

    @Override
    public int calculateCycles() {
        int totalCycles = 0;
        for (SInstruction instruction : instructions) {
            totalCycles += instruction.cycles();
        }
        return totalCycles;
    }

    @Override
    public ExpansionResult expandToDegree(int degree) {
        // Start from the current program as generation 0
        List<InstrNode> cur = new ArrayList<>(this.instructions.size());

        for (int i = 0; i < this.instructions.size(); i++) {
            cur.add(new InstrNode(this.instructions.get(i), i + 1)); // Row numbers start from 1
        }

        // We’ll accumulate lineage across steps:
        // parentMap links a newly created instruction to the instruction it
        // replaced/expanded from.
        Map<SInstruction, SInstruction> parentMap = new IdentityHashMap<>();

        // expose row mapping for the final snapshot (useful for later
        // policies/printing)
        Map<SInstruction, Integer> rowOf = new IdentityHashMap<>();

        NameSession names = new NameSession(baseUsedLabelNames, baseUsedVarNames);

        for (int step = 0; step < degree; step++) {
            // Expand once
            List<InstrNode> next = new ArrayList<>(cur.size() * 2);
            int rowCounter = 1; // Fresh row numbering for this degree

            for (InstrNode node : cur) {
                SInstruction in = node.ins;
                if (isBasic(in)) {
                    // Basic instructions stay as-is, but we need to track them in parent map
                    // for history chain tracing - they "come from" themselves in the previous
                    // degree
                    next.add(new InstrNode(in, rowCounter++));
                    // For history chain purposes, basic instructions are their own parent
                    // when copied across degrees
                } else {
                    // Expand synthetic instructions
                    List<SInstruction> children = expandOne(in, names);
                    for (SInstruction ch : children) {
                        parentMap.put(ch, in); // Track parent-child relationship
                        next.add(new InstrNode(ch, rowCounter++)); // Assign fresh row number
                    }
                }
            }
            cur = next;
        }

        // Build the final flattened snapshot in order
        List<SInstruction> finalProgram = new ArrayList<>(cur.size());
        Map<SInstruction, Integer> lineNo = new IdentityHashMap<>();
        for (int i = 0; i < cur.size(); i++) {
            SInstruction ins = cur.get(i).ins;
            finalProgram.add(ins);
            lineNo.put(ins, cur.get(i).rowNumber); // Use the row number from current degree
            rowOf.put(ins, i); // Store original position for lineage
        }

        return new ExpansionResult(finalProgram, parentMap, lineNo, rowOf);
    }

    private boolean isBasic(SInstruction in) {
        String name = in.getName();
        // You already keep BASIC/SYNTHETIC sets in this class:
        return BASIC.contains(name);
    }

    // Expand one instruction by your exact rules (ZERO_VARIABLE, ASSIGNMENT, etc.)
    private List<SInstruction> expandOne(SInstruction in, NameSession names) {
        // Replace the switch below with your actual expansion code

        switch (in.getName()) {
            case "ZERO":
                var L8 = names.freshLabel();
                return List.of(new DecreaseInstruction(in.getVariable(), L8),
                        new JumpNotZeroInstruction(in.getVariable(), L8));
            case "ASSIGN":
                AssignVariableInstruction a = (AssignVariableInstruction) in;
                var V = a.getVariable(); // target
                var Vp = a.getSource(); // source
                var z1 = names.freshZ();
                // loop labels
                var AL1 = names.freshLabel(); // drain V' -> z1
                var AL2 = names.freshLabel(); // restore from z1 -> V', V
                var AL3 = names.freshLabel();

                var out = new ArrayList<SInstruction>();

                out.add(new ZeroVariableInstruction(V)); // V --> 0
                out.add(new JumpNotZeroInstruction(Vp, AL1)); // if Vp != 0 goto AL1
                out.add(new GotoLabelInstruction(AL3)); // goto AL3
                out.add(new DecreaseInstruction(Vp, AL1)); // Vp --> Vp - 1
                out.add(new IncreaseInstruction(z1)); // z1 --> z1 + 1
                out.add(new JumpNotZeroInstruction(Vp, AL1)); // IF Vp != 0 goto AL1
                out.add(new DecreaseInstruction(z1, AL2)); // z1 --> z1 - 1
                out.add(new IncreaseInstruction(V)); // V --> V + 1
                out.add(new IncreaseInstruction(Vp)); // Vp --> Vp + 1
                out.add(new JumpNotZeroInstruction(z1, AL2)); // IF z1 != 0 goto AL2
                out.add(new NoOpInstruction(V)); // V --> V
                return out;
            case "GOTO":
                GotoLabelInstruction gl = (GotoLabelInstruction) in;
                var z2 = names.freshZ();
                var out2 = new ArrayList<SInstruction>();
                out2.add(new IncreaseInstruction(z2)); // z2 --> z2 + 1
                out2.add(new JumpNotZeroInstruction(z2, gl.getTarget())); // IF z2 != 0 goto in.getLabel()
                return out2;
            case "ASSIGNC":
                AssignConstantInstruction a2 = (AssignConstantInstruction) in;
                var out3 = new ArrayList<SInstruction>();

                out3.add(new ZeroVariableInstruction(a2.getVariable())); // V --> 0
                for (int i = 0; i < a2.getConstant(); i++) {
                    out3.add(new IncreaseInstruction(a2.getVariable())); // V --> V + 1 K times
                }
                return out3;
            case "IFZ":
                JumpZeroInstruction jz = (JumpZeroInstruction) in;
                var out4 = new ArrayList<SInstruction>();
                Label BL1 = names.freshLabel();
                out4.add(new JumpNotZeroInstruction(jz.getVariable(), BL1));
                out4.add(new GotoLabelInstruction(jz.getTarget()));
                out4.add(new NoOpInstruction(Variable.RESULT, BL1));
                return out4;
            case "IFEQC":
                JumpEqualConstantInstruction jec = (JumpEqualConstantInstruction) in;
                var out5 = new ArrayList<SInstruction>();
                Label BL2 = names.freshLabel();
                Variable z5 = names.freshZ();
                out5.add(new AssignVariableInstruction(z5, jec.getVariable())); // z5 <-- V
                for (int i = 0; i < jec.getConstant(); i++) { // K times
                    out5.add(new JumpZeroInstruction(z5, BL2)); // IF z5 == 0 goto BL2
                    out5.add(new DecreaseInstruction(z5)); // z5 --> z5 - 1
                }
                out5.add(new JumpNotZeroInstruction(z5, BL2)); // IF z5 != 0 goto BL2
                out5.add(new GotoLabelInstruction(jec.getTarget())); // goto original target
                out5.add(new NoOpInstruction(Variable.RESULT, BL2)); // BL2: V --> V
                // Add the original target label as a NoOp instruction so jumps can find it
                out5.add(new NoOpInstruction(Variable.RESULT, jec.getTarget()));
                return out5;
            case "IFEQV":
                JumpEqualVariableInstruction jev = (JumpEqualVariableInstruction) in;
                var Source = jev.getVariable();
                var Target = jev.getOther();
                var Target2 = jev.getTarget();
                var out6 = new ArrayList<SInstruction>();
                Label BL3 = names.freshLabel();
                Label BL4 = names.freshLabel();
                Label BL5 = names.freshLabel();
                Variable z6 = names.freshZ();
                Variable z7 = names.freshZ();
                out6.add(new AssignVariableInstruction(z6, Source));
                out6.add(new AssignVariableInstruction(z7, Target));
                out6.add(new JumpZeroInstruction(z6, BL4, BL5));
                out6.add(new JumpZeroInstruction(z7, BL3));
                out6.add(new DecreaseInstruction(z6));
                out6.add(new DecreaseInstruction(z7));
                out6.add(new GotoLabelInstruction(BL4));
                out6.add(new JumpZeroInstruction(z7, BL5, Target2));
                out6.add(new NoOpInstruction(Variable.RESULT, BL3));
                return out6;
            case "QUOTE":
                return expandQuote((QuoteInstruction) in, names);
            case "JUMP_EQUAL_FUNCTION":
                return expandJumpEqualFunction((JumpEqualFunctionInstruction) in, names);
            default:
                return List.of(in);
        }
    }

    private List<SInstruction> expandQuote(QuoteInstruction quote, NameSession names) {
        String functionName = quote.getFunctionName();
        List<FunctionArgument> arguments = quote.getFunctionArguments();
        Variable target = quote.getVariable();

        // Get the function body
        List<SInstruction> functionBody = functions.get(functionName);
        if (functionBody == null) {
            throw new IllegalArgumentException("Function '" + functionName + "' not found");
        }

        List<SInstruction> expanded = new ArrayList<>();

        // Process each argument - this is where composition happens
        // For each argument, if it's a function call, we create a QUOTE instruction
        // that will be expanded in the next degree, not fully expanded now.
        List<Variable> processedArguments = new ArrayList<>();
        for (FunctionArgument arg : arguments) {
            if (arg.isFunctionCall()) {
                // This is a function call - create a QUOTE instruction for it
                // Example: (+, x1, y) becomes z2 <- (+, x1, y) which will be expanded later
                FunctionCall call = arg.asFunctionCall();
                Variable resultVar = names.freshZ();

                // Create a QUOTE instruction for the nested function call
                // This will be expanded in the next degree, not now
                QuoteInstruction nestedQuote = new QuoteInstruction(resultVar, call.getFunctionName(),
                        call.getArguments(), functions.get(call.getFunctionName()));
                expanded.add(nestedQuote);

                // The result of the nested function becomes an input to the parent
                processedArguments.add(resultVar);
            } else {
                // Simple variable - use as before
                processedArguments.add(arg.asVariable());
            }
        }

        // Create fresh variables for inputs, outputs, and working variables
        Map<Variable, Variable> variableMap = new HashMap<>();
        Map<Label, Label> labelMap = new HashMap<>();

        // Map input variables (x1, x2, ...) to the processed arguments
        for (int i = 0; i < processedArguments.size(); i++) {
            Variable inputVar = new VariableImpl(VariableType.INPUT, i + 1);
            Variable freshVar = processedArguments.get(i);
            variableMap.put(inputVar, freshVar);
        }

        // Map output variable (y) to a fresh working variable
        Variable outputVar = Variable.RESULT;
        Variable freshOutputVar = names.freshZ();
        variableMap.put(outputVar, freshOutputVar);

        // Map ALL variables used in the function body to fresh variables
        for (SInstruction inst : functionBody) {
            Variable var = inst.getVariable();
            if (var != null && !variableMap.containsKey(var)) {
                // This is a working variable or other variable used in the function
                Variable freshVar = names.freshZ();
                variableMap.put(var, freshVar);
            }

            // Also check for variables in instruction arguments
            if (inst instanceof AssignVariableInstruction assign) {
                Variable sourceVar = assign.getSource();
                if (sourceVar != null && !variableMap.containsKey(sourceVar)) {
                    Variable freshVar = names.freshZ();
                    variableMap.put(sourceVar, freshVar);
                }
            } else if (inst instanceof JumpEqualVariableInstruction jev) {
                Variable otherVar = jev.getOther();
                if (otherVar != null && !variableMap.containsKey(otherVar)) {
                    Variable freshVar = names.freshZ();
                    variableMap.put(otherVar, freshVar);
                }
            }
        }

        // Create fresh labels for all labels in the function
        for (SInstruction inst : functionBody) {
            if (inst.getLabel() != FixedLabel.EMPTY) {
                Label originalLabel = inst.getLabel();
                if (!labelMap.containsKey(originalLabel)) {
                    labelMap.put(originalLabel, names.freshLabel());
                }
            }
        }

        // Create a fresh label for the end of the quoted function
        Label endLabel = names.freshLabel();

        expanded.add(new NoOpInstruction(Variable.RESULT, FixedLabel.EMPTY));

        // Input variables are already initialized through processedArguments
        // No need for additional assignment since we're using processedArguments
        // directly

        // // Add initialization instructions: zi <- Vi for each argument
        // for (int i = 0; i < arguments.size(); i++) {
        // Variable inputVar = new VariableImpl(VariableType.INPUT, i + 1);
        // Variable freshVar = variableMap.get(inputVar);
        // Variable argument = arguments.get(i);

        // if (argument.getType() == VariableType.Constant) {
        // // For constants, assign the constant value directly
        // expanded.add(new AssignConstantInstruction(freshVar, (int)
        // argument.getNumber()));
        // } else {
        // // For variables, assign the variable value
        // expanded.add(new AssignVariableInstruction(freshVar, argument));
        // }
        // }

        // Add the expanded function body
        for (SInstruction inst : functionBody) {
            SInstruction expandedInst = expandInstruction(inst, variableMap, labelMap, endLabel);
            if (expandedInst != null) {
                // If the expanded instruction is synthetic, we need to expand it further
                if (!isBasic(expandedInst)) {
                    // This is a synthetic instruction that needs expansion
                    List<SInstruction> furtherExpanded = expandOne(expandedInst, names);
                    expanded.addAll(furtherExpanded);
                } else {
                    expanded.add(expandedInst);
                }
            }
        }

        expanded.add(new AssignVariableInstruction(target, freshOutputVar, endLabel));

        return expanded;
    }

    private List<SInstruction> expandJumpEqualFunction(JumpEqualFunctionInstruction jef, NameSession names) {
        String functionName = jef.getFunctionName();
        List<FunctionArgument> arguments = jef.getFunctionArguments();
        Variable compareVar = jef.getVariable();
        Label targetLabel = jef.getTarget();

        // Get the function body
        List<SInstruction> functionBody = functions.get(functionName);
        if (functionBody == null) {
            throw new IllegalArgumentException("Function '" + functionName + "' not found");
        }

        // Degree 1 expansion: Split into QUOTE call + comparison
        // z1 = Q(x1, ...) (as a QUOTE instruction)
        // IF V = z1 GOTO L

        Variable freshOutputVar = names.freshZ();

        // Convert arguments to FunctionArgument (they're already FunctionArgument)
        List<FunctionArgument> functionArguments = arguments;

        // Create a QUOTE instruction: z1 <- (Q, x1, ...)
        QuoteInstruction quoteInst = new QuoteInstruction(freshOutputVar, functionName, functionArguments,
                functionBody);

        // Create the comparison: IF V == z1 GOTO L
        JumpEqualVariableInstruction comparison = new JumpEqualVariableInstruction(compareVar, freshOutputVar,
                targetLabel);

        return List.of(quoteInst, comparison);
    }

    private SInstruction expandInstruction(SInstruction inst, Map<Variable, Variable> variableMap,
            Map<Label, Label> labelMap, Label endLabel) {
        // Map the variable - all variables should be in the map now
        Variable newVar = variableMap.get(inst.getVariable());
        if (newVar == null) {
            // This should not happen if we properly mapped all variables
            throw new IllegalStateException("Variable " + inst.getVariable() + " not found in variable map");
        }
        Label newLabel = labelMap.getOrDefault(inst.getLabel(), inst.getLabel());

        // Handle EXIT label specially
        if (inst.getLabel() == FixedLabel.EXIT) {
            newLabel = endLabel;
        }

        return switch (inst.getName()) {
            case "INCREASE" -> new IncreaseInstruction(newVar, newLabel);
            case "DECREASE" -> new DecreaseInstruction(newVar, newLabel);
            case "NEUTRAL" -> new NoOpInstruction(newVar, newLabel);
            case "JUMP_NOT_ZERO" -> {
                JumpNotZeroInstruction jnz = (JumpNotZeroInstruction) inst;
                Label targetLabel = labelMap.getOrDefault(jnz.getTarget(), jnz.getTarget());
                if (jnz.getTarget() == FixedLabel.EXIT) {
                    targetLabel = endLabel;
                }
                yield new JumpNotZeroInstruction(newVar, newLabel, targetLabel);
            }
            case "ZERO" -> new ZeroVariableInstruction(newVar, newLabel);
            case "ASSIGN" -> {
                AssignVariableInstruction assign = (AssignVariableInstruction) inst;
                Variable newSource = variableMap.get(assign.getSource());
                if (newSource == null) {
                    throw new IllegalStateException(
                            "Source variable " + assign.getSource() + " not found in variable map");
                }
                yield new AssignVariableInstruction(newVar, newSource, newLabel);
            }
            case "ASSIGNC" -> {
                AssignConstantInstruction assign = (AssignConstantInstruction) inst;
                yield new AssignConstantInstruction(newVar, assign.getConstant(), newLabel);
            }
            case "IFZ" -> {
                JumpZeroInstruction jz = (JumpZeroInstruction) inst;
                Label targetLabel = labelMap.getOrDefault(jz.getTarget(), jz.getTarget());
                if (jz.getTarget() == FixedLabel.EXIT) {
                    targetLabel = endLabel;
                }
                yield new JumpZeroInstruction(newVar, newLabel, targetLabel);
            }
            case "IFEQC" -> {
                JumpEqualConstantInstruction jec = (JumpEqualConstantInstruction) inst;
                Label targetLabel = labelMap.getOrDefault(jec.getTarget(), jec.getTarget());
                if (jec.getTarget() == FixedLabel.EXIT) {
                    targetLabel = endLabel;
                }
                yield new JumpEqualConstantInstruction(newVar, newLabel, jec.getConstant(), targetLabel);
            }
            case "JUMP_EQUAL_VARIABLE" -> {
                JumpEqualVariableInstruction jev = (JumpEqualVariableInstruction) inst;
                Variable newOther = variableMap.get(jev.getOther());
                if (newOther == null) {
                    throw new IllegalStateException("Other variable " + jev.getOther() + " not found in variable map");
                }
                Label targetLabel = labelMap.getOrDefault(jev.getTarget(), jev.getTarget());
                if (jev.getTarget() == FixedLabel.EXIT) {
                    targetLabel = endLabel;
                }
                yield new JumpEqualVariableInstruction(newVar, newLabel, newOther, targetLabel);
            }
            case "GOTO" -> {
                GotoLabelInstruction gotoInst = (GotoLabelInstruction) inst;
                Label targetLabel = labelMap.getOrDefault(gotoInst.getTarget(), gotoInst.getTarget());
                if (gotoInst.getTarget() == FixedLabel.EXIT) {
                    targetLabel = endLabel;
                }
                yield new GotoLabelInstruction(newLabel, targetLabel);
            }
            case "QUOTE" -> {
                QuoteInstruction quote = (QuoteInstruction) inst;
                List<SInstruction> functionInstructions = functions.get(quote.getFunctionName());
                if (functionInstructions == null) {
                    functionInstructions = new ArrayList<>(); // fallback
                }
                yield new QuoteInstruction(newVar, quote.getFunctionName(), quote.getFunctionArguments(),
                        functionInstructions, newLabel);
            }
            case "JUMP_EQUAL_FUNCTION" -> {
                JumpEqualFunctionInstruction jef = (JumpEqualFunctionInstruction) inst;
                List<SInstruction> functionInstructions = functions.get(jef.getFunctionName());
                if (functionInstructions == null) {
                    functionInstructions = new ArrayList<>(); // fallback
                }
                yield new JumpEqualFunctionInstruction(newVar, jef.getFunctionName(), jef.getFunctionArguments(),
                        functionInstructions, jef.getTarget(), newLabel);
            }
            default -> null; // Unknown instruction
        };
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
            ok = validateXmlFile(doc);
        }
        if (!ok) {
            return null;
        }
        buildInMemory(doc);
        reseedNameRegistryFromProgram();
        return xmlPath;
    }

    private void buildInMemory(Document doc) {
        instructions.clear();
        functions.clear();

        Element root = doc.getDocumentElement();

        // Parse functions FIRST before main instructions
        Element sFunctions = getSingleChild(root, "S-Functions");
        if (sFunctions != null) {
            parseFunctions(sFunctions, new HashMap<>());
        }

        Element sInstructions = getSingleChild(root, "S-Instructions");
        if (sInstructions == null)
            return; // הגנה נוספת

        // מאגר לייבלים כדי לייצר מופע יחיד לכל שם (L1, L2,...)
        Map<String, Label> labelPool = new HashMap<>();

        List<Element> all = childElements(sInstructions, "S-Instruction");
        for (Element instEl : all) {
            String name = instEl.getAttribute("name").trim(); // e.g. DECREASE / INCREASE / JUMP_NOT_ZERO / NEUTRAL
            String varText = textOfSingle(instEl, "S-Variable"); // e.g. x1 / y / z3
            Variable var = parseVariable(varText);

            // לייבל שמוגדר על ההוראה (אופציונלי)
            String lblText = textOfOptional(instEl, "S-Label"); // e.g. L1
            Label selfLabel = (lblText == null || lblText.isBlank())
                    ? FixedLabel.EMPTY
                    : getOrCreateLabel(lblText, labelPool);

            // ארגומנטים (למשל יעד קפיצה ב-JNZ)
            Map<String, String> args = readArgs(instEl);

            switch (name) {
                case "INCREASE" -> {
                    if (selfLabel != FixedLabel.EMPTY)
                        instructions.add(new IncreaseInstruction(var, selfLabel));
                    else
                        instructions.add(new IncreaseInstruction(var));
                }
                case "DECREASE" -> {
                    if (selfLabel != FixedLabel.EMPTY)
                        instructions.add(new DecreaseInstruction(var, selfLabel));
                    else
                        instructions.add(new DecreaseInstruction(var));
                }
                case "NEUTRAL" -> {
                    instructions.add(new NoOpInstruction(var));
                }
                case "JUMP_NOT_ZERO" -> {
                    String targetName = args.get("JNZLabel"); // badic.xml
                    Label target = parseLabel(targetName, labelPool);
                    if (selfLabel != FixedLabel.EMPTY)
                        instructions.add(new JumpNotZeroInstruction(var, selfLabel, target));
                    else
                        instructions.add(new JumpNotZeroInstruction(var, target));
                }

                // ===== synthetic.xml cases =====
                case "ZERO_VARIABLE" -> {
                    if (selfLabel != FixedLabel.EMPTY)
                        instructions.add(new ZeroVariableInstruction(var, selfLabel));
                    else
                        instructions.add(new ZeroVariableInstruction(var));
                }
                case "ASSIGNMENT" -> { // <... name="assignedVariable" value="x2"/>
                    Variable src = parseVariable(args.get("assignedVariable"));
                    if (selfLabel != FixedLabel.EMPTY)
                        instructions.add(new AssignVariableInstruction(var, src, selfLabel));
                    else
                        instructions.add(new AssignVariableInstruction(var, src));
                }
                case "CONSTANT_ASSIGNMENT" -> { // <... name="constantValue" value="5"/>
                    int c = Integer.parseInt(args.get("constantValue"));
                    if (selfLabel != FixedLabel.EMPTY)
                        instructions.add(new AssignConstantInstruction(var, c, selfLabel));
                    else
                        instructions.add(new AssignConstantInstruction(var, c));
                }
                case "JUMP_ZERO" -> { // <... name="JZLabel" value="EXIT"/>
                    Label target = parseLabel(args.get("JZLabel"), labelPool);
                    if (selfLabel != FixedLabel.EMPTY)
                        instructions.add(new JumpZeroInstruction(var, selfLabel, target));
                    else
                        instructions.add(new JumpZeroInstruction(var, target));
                }
                case "JUMP_EQUAL_CONSTANT" -> { // JEConstantLabel + constantValue
                    Label target = parseLabel(args.get("JEConstantLabel"), labelPool);
                    int c = Integer.parseInt(args.get("constantValue"));
                    if (selfLabel != FixedLabel.EMPTY)
                        instructions.add(new JumpEqualConstantInstruction(var, selfLabel, c, target));
                    else
                        instructions.add(new JumpEqualConstantInstruction(var, c, target));
                }
                case "JUMP_EQUAL_VARIABLE" -> { // JEVariableLabel + variableName
                    Label target = parseLabel(args.get("JEVariableLabel"), labelPool);
                    Variable other = parseVariable(args.get("variableName"));
                    if (selfLabel != FixedLabel.EMPTY)
                        instructions.add(new JumpEqualVariableInstruction(var, selfLabel, other, target));
                    else
                        instructions.add(new JumpEqualVariableInstruction(var, other, target));
                }
                case "GOTO_LABEL" -> { // gotoLabel
                    Label target = parseLabel(args.get("gotoLabel"), labelPool);
                    if (selfLabel != FixedLabel.EMPTY)
                        instructions.add(new GotoLabelInstruction(selfLabel, target));
                    else
                        instructions.add(new GotoLabelInstruction(target));
                }

                case "QUOTE" -> {
                    String functionName = args.get("functionName");
                    String functionArguments = args.get("functionArguments");
                    List<FunctionArgument> argVars = new ArrayList<>();

                    if (functionArguments != null && !functionArguments.trim().isEmpty()) {
                        // Use the new parser that handles nested function calls
                        String[] argStrings = splitFunctionArguments(functionArguments);
                        for (String arg : argStrings) {
                            argVars.add(FunctionArgumentParser.parseFunctionArgument(arg));
                        }
                    }

                    List<SInstruction> functionInstructions = functions.get(functionName);
                    if (functionInstructions == null) {
                        functionInstructions = new ArrayList<>(); // fallback
                    }
                    if (selfLabel != FixedLabel.EMPTY)
                        instructions
                                .add(new QuoteInstruction(var, functionName, argVars, functionInstructions, selfLabel));
                    else
                        instructions.add(new QuoteInstruction(var, functionName, argVars, functionInstructions));
                }

                case "JUMP_EQUAL_FUNCTION" -> {
                    String functionName = args.get("functionName");
                    String functionArguments = args.get("functionArguments");
                    String targetLabel = args.get("JEFunctionLabel");
                    List<FunctionArgument> argVars = new ArrayList<>();

                    if (functionArguments != null && !functionArguments.trim().isEmpty()) {
                        // Use the new parser that handles nested function calls
                        String[] argStrings = splitFunctionArguments(functionArguments);
                        for (String arg : argStrings) {
                            argVars.add(FunctionArgumentParser.parseFunctionArgument(arg));
                        }
                    }

                    Label target = parseLabel(targetLabel, labelPool);

                    List<SInstruction> functionInstructions = functions.get(functionName);
                    if (functionInstructions == null) {
                        functionInstructions = new ArrayList<>(); // fallback
                    }
                    if (selfLabel != FixedLabel.EMPTY)
                        instructions
                                .add(new JumpEqualFunctionInstruction(var, functionName, argVars, functionInstructions,
                                        target, selfLabel));
                    else
                        instructions.add(new JumpEqualFunctionInstruction(var, functionName, argVars,
                                functionInstructions, target));
                }

                default -> {
                    /* לא בונים הוראה לא מוכרת */ }
            }

        }
    }

    private void parseFunctions(Element sFunctions, Map<String, Label> labelPool) {
        List<Element> functionElements = childElements(sFunctions, "S-Function");
        for (Element functionEl : functionElements) {
            String functionName = functionEl.getAttribute("name").trim();
            if (functionName.isEmpty()) {
                System.out.println("S-Function missing name attribute");
                continue;
            }

            Element functionInstructions = getSingleChild(functionEl, "S-Instructions");
            if (functionInstructions == null) {
                System.out.println("S-Function '" + functionName + "' missing S-Instructions");
                continue;
            }

            List<SInstruction> functionBody = new ArrayList<>();
            List<Element> all = childElements(functionInstructions, "S-Instruction");
            for (Element instEl : all) {
                SInstruction instruction = parseInstruction(instEl, labelPool);
                if (instruction != null) {
                    functionBody.add(instruction);
                }
            }

            functions.put(functionName, functionBody);
        }
    }

    private SInstruction parseInstruction(Element instEl, Map<String, Label> labelPool) {
        String name = instEl.getAttribute("name").trim();
        String varText = textOfSingle(instEl, "S-Variable");
        Variable var = parseVariable(varText);

        String lblText = textOfOptional(instEl, "S-Label");
        Label selfLabel = (lblText == null || lblText.isBlank())
                ? FixedLabel.EMPTY
                : getOrCreateLabel(lblText, labelPool);

        Map<String, String> args = readArgs(instEl);

        return switch (name) {
            case "INCREASE" -> {
                if (selfLabel != FixedLabel.EMPTY)
                    yield new IncreaseInstruction(var, selfLabel);
                else
                    yield new IncreaseInstruction(var);
            }
            case "DECREASE" -> {
                if (selfLabel != FixedLabel.EMPTY)
                    yield new DecreaseInstruction(var, selfLabel);
                else
                    yield new DecreaseInstruction(var);
            }
            case "NEUTRAL" -> new NoOpInstruction(var);
            case "JUMP_NOT_ZERO" -> {
                String targetName = args.get("JNZLabel");
                Label target = parseLabel(targetName, labelPool);
                if (selfLabel != FixedLabel.EMPTY)
                    yield new JumpNotZeroInstruction(var, selfLabel, target);
                else
                    yield new JumpNotZeroInstruction(var, target);
            }
            case "ZERO_VARIABLE" -> {
                if (selfLabel != FixedLabel.EMPTY)
                    yield new ZeroVariableInstruction(var, selfLabel);
                else
                    yield new ZeroVariableInstruction(var);
            }
            case "ASSIGNMENT" -> {
                String assignedVar = args.get("assignedVariable");
                Variable sourceVar = parseVariable(assignedVar);
                if (selfLabel != FixedLabel.EMPTY)
                    yield new AssignVariableInstruction(var, sourceVar, selfLabel);
                else
                    yield new AssignVariableInstruction(var, sourceVar);
            }
            case "CONSTANT_ASSIGNMENT" -> {
                String constantValue = args.get("constantValue");
                long value = Long.parseLong(constantValue);
                if (selfLabel != FixedLabel.EMPTY)
                    yield new AssignConstantInstruction(var, value, selfLabel);
                else
                    yield new AssignConstantInstruction(var, value);
            }
            case "JUMP_ZERO" -> {
                String targetName = args.get("JZLabel");
                Label target = parseLabel(targetName, labelPool);
                if (selfLabel != FixedLabel.EMPTY)
                    yield new JumpZeroInstruction(var, selfLabel, target);
                else
                    yield new JumpZeroInstruction(var, target);
            }
            case "JUMP_EQUAL_CONSTANT" -> {
                String targetName = args.get("JEConstantLabel");
                String constantValue = args.get("constantValue");
                Label target = parseLabel(targetName, labelPool);
                long value = Long.parseLong(constantValue);
                if (selfLabel != FixedLabel.EMPTY)
                    yield new JumpEqualConstantInstruction(var, selfLabel, value, target);
                else
                    yield new JumpEqualConstantInstruction(var, value, target);
            }
            case "JUMP_EQUAL_VARIABLE" -> {
                String targetName = args.get("JEVariableLabel");
                String otherVar = args.get("otherVariable");
                Label target = parseLabel(targetName, labelPool);
                Variable other = parseVariable(otherVar);
                if (selfLabel != FixedLabel.EMPTY)
                    yield new JumpEqualVariableInstruction(var, selfLabel, other, target);
                else
                    yield new JumpEqualVariableInstruction(var, other, target);
            }
            case "GOTO_LABEL" -> {
                String targetName = args.get("gotoLabel");
                Label target = parseLabel(targetName, labelPool);
                if (selfLabel != FixedLabel.EMPTY)
                    yield new GotoLabelInstruction(selfLabel, target);
                else
                    yield new GotoLabelInstruction(target);
            }
            case "QUOTE" -> {
                String functionName = args.get("functionName");
                String functionArguments = args.get("functionArguments");
                List<FunctionArgument> parsedArguments = new ArrayList<>();
                if (functionArguments != null && !functionArguments.trim().isEmpty()) {
                    String[] argStrings = splitFunctionArguments(functionArguments);
                    for (String arg : argStrings) {
                        parsedArguments.add(FunctionArgumentParser.parseFunctionArgument(arg));
                    }
                }
                List<SInstruction> functionInstructions = functions.get(functionName);
                if (functionInstructions == null) {
                    functionInstructions = new ArrayList<>(); // fallback
                }
                if (selfLabel != FixedLabel.EMPTY)
                    yield new QuoteInstruction(var, functionName, parsedArguments, functionInstructions, selfLabel);
                else
                    yield new QuoteInstruction(var, functionName, parsedArguments, functionInstructions);
            }
            case "JUMP_EQUAL_FUNCTION" -> {
                String functionName = args.get("functionName");
                String functionArguments = args.get("functionArguments");
                String targetName = args.get("JEFunctionLabel");
                Label target = parseLabel(targetName, labelPool);
                List<FunctionArgument> parsedArguments = new ArrayList<>();
                if (functionArguments != null && !functionArguments.trim().isEmpty()) {
                    String[] argStrings = splitFunctionArguments(functionArguments);
                    for (String arg : argStrings) {
                        parsedArguments.add(FunctionArgumentParser.parseFunctionArgument(arg));
                    }
                }
                List<SInstruction> functionInstructions = functions.get(functionName);
                if (functionInstructions == null) {
                    functionInstructions = new ArrayList<>(); // fallback
                }
                if (selfLabel != FixedLabel.EMPTY)
                    yield new JumpEqualFunctionInstruction(var, functionName, parsedArguments, functionInstructions,
                            target, selfLabel);
                else
                    yield new JumpEqualFunctionInstruction(var, functionName, parsedArguments, functionInstructions,
                            target);
            }
            default -> null; // Unknown instruction
        };
    }

    private static String textOfSingle(Element parent, String tag) {
        List<Element> lst = childElements(parent, tag);
        if (lst.isEmpty())
            return "";
        return trimOrNull(lst.get(0).getTextContent());
    }

    private static String textOfOptional(Element parent, String tag) {
        List<Element> lst = childElements(parent, tag);
        if (lst.isEmpty())
            return null;
        return trimOrNull(lst.get(0).getTextContent());
    }

    private static Map<String, String> readArgs(Element instEl) {
        Map<String, String> map = new HashMap<>();
        Element args = getSingleChild(instEl, "S-Instruction-Arguments");
        if (args == null)
            return map;
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

    private static Variable parseVariableOrConstant(String txt) {
        if (txt == null || txt.trim().isEmpty()) {
            return null;
        }
        String t = txt.trim();

        // Try to parse as a constant number first
        try {
            int constantValue = Integer.parseInt(t);
            return new VariableImpl(VariableType.Constant, constantValue);
        } catch (NumberFormatException e) {
            // Not a number, try parsing as variable
            return parseVariable(txt);
        }
    }

    private static Label getOrCreateLabel(String name, Map<String, Label> pool) {
        return pool.computeIfAbsent(name, n -> {
            if (n.equals("EXIT")) {
                return FixedLabel.EXIT;
            }
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
        // and always gives you a trimmed string (leading and trailing spaces removed).
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
        if (s == null)
            return null;
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
        if (direct.isEmpty())
            return null;
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

        // (4) Optional: cross-check type matches classification (helps catch
        // mismatches)
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
            System.out.println(where
                    + "<S-Variable> must be 'y' or match ^[xz][0-9]+$ (case-sensitive, no spaces). Got: '" + val + "'");
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
                System.out.println(
                        where + "<S-Label> must match ^L[0-9]+$ (uppercase L followed by digits). Got: '" + val + "'");
                ok = false;
            }
        }
        return ok;
    }

    /**
     * Validate the <S-Instruction-Arguments> block for a given <S-Instruction> by
     * opcode.
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
        boolean usesArgs = instrName.equals("GOTO_LABEL") ||
                instrName.equals("JUMP_NOT_ZERO") ||
                instrName.equals("ASSIGNMENT") ||
                instrName.equals("CONSTANT_ASSIGNMENT") ||
                instrName.equals("JUMP_ZERO") ||
                instrName.equals("JUMP_EQUAL_CONSTANT") ||
                instrName.equals("JUMP_EQUAL_VARIABLE") ||
                instrName.equals("QUOTE_PROGRAM") ||
                instrName.equals("JUMP_EQUAL_FUNCTION") ||
                instrName.equals("QUOTE");
        if (!usesArgs && container != null) {
            System.out.println(where + "Unexpected <S-Instruction-Arguments> for instruction '" + instrName + "'.");
            ok = false;
            // keep validating to surface more issues
        }
        if (usesArgs && container == null) {
            System.out.println(where + "Missing <S-Instruction-Arguments> for instruction '" + instrName + "'.");
            return false;
        }
        if (container == null)
            return ok; // nothing more to check

        // Collect arguments
        List<Element> args = childElements(container, "S-Instruction-Argument");
        if (args.isEmpty()) {
            System.out.println(where + "<S-Instruction-Arguments> must contain at least one <S-Instruction-Argument>.");
            ok = false;
        }
        return ok;
    }

    private static Label parseLabel(String name, Map<String, Label> pool) {
        if (name == null || name.isBlank())
            return FixedLabel.EMPTY;
        if ("EXIT".equals(name))
            return FixedLabel.EXIT;
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
            if (argsBlock == null)
                continue; // nothing to check

            List<Element> args = childElements(argsBlock, "S-Instruction-Argument");
            for (Element arg : args) {
                String n = arg.getAttribute("name");
                String v = arg.getAttribute("value");
                if (n == null)
                    n = "";
                if (v == null)
                    v = "";
                n = n.trim();
                v = v.trim();

                if (!LABEL_TARGET_ARGS.contains(n)) {
                    continue; // not a label target argument
                }
                if (v.isEmpty()) {
                    // Already covered by other checks usually; still helpful to surface
                    System.out.println(where + "Label argument '" + n + "' for instruction '" + instrName
                            + "' must not be empty.");
                    ok = false;
                    continue;
                }

                if ("EXIT".equals(v)) {
                    // EXIT is a virtual sink, not a real line label.
                    continue;
                }

                // Ensure it is syntactically an L# label (you already enforce this on
                // <S-Label>).
                if (!v.matches("^L[0-9]+$")) {
                    System.out.println(where + "Label argument '" + n + "' must match ^L[0-9]+$ (got '" + v + "').");
                    ok = false;
                    continue;
                }

                // Cross-reference: does this referenced label appear anywhere as a declared
                // line label?
                if (!declared.contains(v)) {
                    System.out.println(where + "References label '" + v + "' via argument '" + n +
                            "' but no such <S-Label> exists in the program.");
                    ok = false;
                }
            }
        }

        return ok;
    }

    private void reseedNameRegistryFromProgram() {
        baseUsedLabelNames.clear();
        baseUsedVarNames.clear();

        // helpers
        java.util.function.Consumer<semulator.label.Label> recordLabel = lbl -> {
            if (lbl == null || lbl.isExit())
                return;
            String s = lbl.getLabel();
            if (s != null && !s.isBlank() && s.charAt(0) == 'L') {
                baseUsedLabelNames.add(s);
            }
        };
        java.util.function.Consumer<semulator.variable.Variable> recordVar = v -> {
            if (v == null)
                return;
            String s = v.toString();
            if (s != null && !s.isBlank() && s.charAt(0) == 'z') {
                baseUsedVarNames.add(s);
            }
        };

        // Scan main program instructions
        for (SInstruction in : instructions) {
            // labels: self + jump targets
            recordLabel.accept(in.getLabel());
            if (in instanceof GotoLabelInstruction g)
                recordLabel.accept(g.getTarget());
            if (in instanceof JumpNotZeroInstruction j)
                recordLabel.accept(j.getTarget());
            if (in instanceof JumpZeroInstruction j)
                recordLabel.accept(j.getTarget());
            if (in instanceof JumpEqualConstantInstruction j)
                recordLabel.accept(j.getTarget());
            if (in instanceof JumpEqualVariableInstruction j)
                recordLabel.accept(j.getTarget());

            // variables: main + sources/others
            recordVar.accept(in.getVariable());
            if (in instanceof AssignVariableInstruction a)
                recordVar.accept(a.getSource());
            if (in instanceof JumpEqualVariableInstruction j)
                recordVar.accept(j.getOther());
        }

        // Scan function instructions
        for (Map.Entry<String, List<SInstruction>> functionEntry : functions.entrySet()) {
            List<SInstruction> functionInstructions = functionEntry.getValue();
            for (SInstruction in : functionInstructions) {
                // labels: self + jump targets
                recordLabel.accept(in.getLabel());
                if (in instanceof GotoLabelInstruction g)
                    recordLabel.accept(g.getTarget());
                if (in instanceof JumpNotZeroInstruction j)
                    recordLabel.accept(j.getTarget());
                if (in instanceof JumpZeroInstruction j)
                    recordLabel.accept(j.getTarget());
                if (in instanceof JumpEqualConstantInstruction j)
                    recordLabel.accept(j.getTarget());
                if (in instanceof JumpEqualVariableInstruction j)
                    recordLabel.accept(j.getTarget());

                // variables: main + sources/others
                recordVar.accept(in.getVariable());
                if (in instanceof AssignVariableInstruction a)
                    recordVar.accept(a.getSource());
                if (in instanceof JumpEqualVariableInstruction j)
                    recordVar.accept(j.getOther());
            }
        }
    }

    /**
     * Split function arguments by commas while respecting nested parentheses.
     * This handles the composition syntax where arguments can be nested function
     * calls.
     */
    private String[] splitFunctionArguments(String args) {
        List<String> result = new ArrayList<>();
        int depth = 0;
        int start = 0;

        for (int i = 0; i < args.length(); i++) {
            char c = args.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (c == ',' && depth == 0) {
                // Only split on commas when we're at the top level (depth 0)
                result.add(args.substring(start, i).trim());
                start = i + 1;
            }
        }

        // Add the last argument
        result.add(args.substring(start).trim());

        return result.toArray(new String[0]);
    }

}
