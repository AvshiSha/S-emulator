package semulator.instructions;

import semulator.variable.Variable;
import semulator.variable.VariableImpl;
import semulator.variable.VariableType;

import java.util.ArrayList;
import java.util.List;

/**
 * Parser for function arguments that can be either simple variables or nested
 * function calls.
 * Handles the composition syntax like (AND,(NOT,(EQUAL,x1,x2)),(CONST0))
 */
public class FunctionArgumentParser {

    /**
     * Parse a function argument string that can contain nested function calls.
     * 
     * @param argString The argument string to parse
     * @return A FunctionArgument representing either a variable or function call
     */
    public static FunctionArgument parseFunctionArgument(String argString) {
        if (argString == null || argString.trim().isEmpty()) {
            throw new IllegalArgumentException("Argument string cannot be null or empty");
        }

        String trimmed = argString.trim();

        if (trimmed.startsWith("(") && trimmed.endsWith(")")) {
            // Parse function call: (functionName,arg1,arg2,...)
            return parseFunctionCall(trimmed);
        } else {
            // Parse simple variable or constant
            return new VariableArgument(parseVariableOrConstant(trimmed));
        }
    }

    /**
     * Parse a function call string like (AND,(NOT,(EQUAL,x1,x2)),(CONST0))
     */
    private static FunctionCallArgument parseFunctionCall(String functionCallString) {
        // Remove outer parentheses
        String inner = functionCallString.substring(1, functionCallString.length() - 1);

        // Split arguments respecting nested parentheses
        String[] parts = splitFunctionArguments(inner);

        if (parts.length == 0) {
            throw new IllegalArgumentException("Function call must have a function name: " + functionCallString);
        }

        String functionName = parts[0].trim();
        List<FunctionArgument> arguments = new ArrayList<>();

        // Parse each argument recursively
        for (int i = 1; i < parts.length; i++) {
            arguments.add(parseFunctionArgument(parts[i]));
        }

        return new FunctionCallArgument(functionName, arguments);
    }

    /**
     * Split function arguments by commas while respecting nested parentheses.
     * This is the tricky part - we need to split on commas but only when we're
     * not inside nested parentheses.
     */
    private static String[] splitFunctionArguments(String args) {
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

    /**
     * Parse a simple variable or constant.
     * This handles the existing variable parsing logic.
     */
    private static Variable parseVariableOrConstant(String varString) {
        // This is a simplified version - you might want to use the existing
        // parseVariableOrConstant method from SProgramImpl
        if (varString.matches("\\d+")) {
            // It's a constant
            int value = Integer.parseInt(varString);
            return new VariableImpl(VariableType.Constant, value);
        } else {
            // It's a variable - determine type based on prefix
            if (varString.startsWith("x")) {
                int number = extractNumber(varString, 1); // Extract number from x1, x2, etc.
                return new VariableImpl(VariableType.INPUT, number);
            } else if (varString.startsWith("y")) {
                return new VariableImpl(VariableType.RESULT, 0);
            } else {
                int number = extractNumber(varString, 1); // Extract number from z1, z2, etc.
                return new VariableImpl(VariableType.WORK, number);
            }
        }
    }

    /**
     * Extract the numeric part from a variable name like x1, z2, etc.
     */
    private static int extractNumber(String varString, int startIndex) {
        try {
            return Integer.parseInt(varString.substring(startIndex));
        } catch (NumberFormatException e) {
            return 1; // Default fallback
        }
    }
}
