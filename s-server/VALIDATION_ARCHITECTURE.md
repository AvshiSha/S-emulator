# Function Catalog Validation Architecture

## Overview

The S-Emulator server now enforces strict validation rules when uploading programs to ensure consistency and prevent conflicts in the function catalog.

## Validation Rules

When a program is uploaded via the `/api/upload` endpoint, the following validations are performed:

### 1. **Program Name Uniqueness** ✅

- Each program must have a unique name
- If a program with the same name already exists in the catalog, the upload is rejected
- **Error**: `VALIDATION_ERROR: Program with name 'X' already exists in the catalog. Please choose a unique name.`

### 2. **Function Reference Validation** ✅

- All functions referenced in QUOTE or JUMP_EQUAL_FUNCTION instructions must exist
- Functions can exist either:
  - In the global function catalog (from previously uploaded programs)
  - Or defined locally within the current program file
- **Error**: `VALIDATION_ERROR: Function 'FunctionName' is used in the program but not defined. Please ensure all referenced functions exist in the catalog or are defined in this file.`

### 3. **Function Conflict Prevention** ✅

- Programs cannot redefine functions that already exist in the catalog
- Each function name must be unique across all uploaded programs
- **Error**: `VALIDATION_ERROR: Function 'FunctionName' is already defined in program 'ExistingProgram'. Cannot redefine existing functions.`

## How It Works

### Validation Flow

```
Upload Request (XML)
     ↓
Parse & Load Program
     ↓
Check XML Schema Validity
     ↓
╔════════════════════════════════════╗
║  NEW: Function Catalog Validation  ║
╠════════════════════════════════════╣
║ 1. Check program name uniqueness   ║
║ 2. Check function conflicts        ║
║ 3. Validate all function references║
╚════════════════════════════════════╝
     ↓
Add to Catalog & Extract Functions
     ↓
Success Response
```

### Implementation Details

The validation is implemented in `ServerState.loadProgram()` and includes:

1. **`validateProgramFunctions(SProgram program)`**

   - Main validation method
   - Checks function conflicts and references
   - Returns error message or null if valid

2. **`collectReferencedFunctions(SProgramImpl program)`**

   - Recursively scans all instructions
   - Collects function names from QUOTE and JUMP_EQUAL_FUNCTION instructions
   - Handles nested function calls in arguments

3. **`collectFunctionsFromInstruction(SInstruction, Set<String>)`**

   - Extracts function names from individual instructions
   - Supports both QUOTE and JUMP_EQUAL_FUNCTION

4. **`collectFunctionsFromArguments(List<FunctionArgument>, Set<String>)`**
   - Recursively processes function arguments
   - Handles deeply nested function compositions like `(F1, (F2, x1), (F3, (F4, x2)))`

## Example Scenarios

### ❌ Scenario 1: Missing Function Reference

**Program XML:**

```xml
<S-Instruction type="synthetic" name="QUOTE">
  <S-Variable>z1</S-Variable>
  <S-Instruction-Arguments>
    <S-Instruction-Argument name="functionName" value="Smaller_Than"/>
    <S-Instruction-Argument name="functionArguments" value="x1,x2"/>
  </S-Instruction-Arguments>
</S-Instruction>
```

**Result:**

- ❌ Upload fails if `Smaller_Than` is not in the catalog and not defined in the file
- Error: `VALIDATION_ERROR: Function 'Smaller_Than' is used in the program but not defined.`

### ✅ Scenario 2: Function Defined Locally

**Program XML:**

```xml
<S-Instructions>
  <S-Instruction type="synthetic" name="QUOTE">
    <S-Variable>z1</S-Variable>
    <S-Instruction-Arguments>
      <S-Instruction-Argument name="functionName" value="MyFunction"/>
      <S-Instruction-Argument name="functionArguments" value="x1"/>
    </S-Instruction-Arguments>
  </S-Instruction>
</S-Instructions>

<S-Functions>
  <S-Function name="MyFunction" user-string="MF">
    <S-Instructions>
      <!-- function body -->
    </S-Instructions>
  </S-Function>
</S-Functions>
```

**Result:**

- ✅ Upload succeeds - function is defined locally
- `MyFunction` is added to the catalog

### ✅ Scenario 3: Function From Catalog

**Assume** `Add` function exists in catalog from previous upload.

**Program XML:**

```xml
<S-Instruction type="synthetic" name="QUOTE">
  <S-Variable>z1</S-Variable>
  <S-Instruction-Arguments>
    <S-Instruction-Argument name="functionName" value="Add"/>
    <S-Instruction-Argument name="functionArguments" value="x1,x2"/>
  </S-Instruction-Arguments>
</S-Instruction>
```

**Result:**

- ✅ Upload succeeds - `Add` exists in catalog
- Program can use existing functions

### ❌ Scenario 4: Function Name Conflict

**Assume** `Add` function exists in catalog from program "BasicMath".

**Program XML:**

```xml
<S-Functions>
  <S-Function name="Add" user-string="ADD">
    <S-Instructions>
      <!-- different implementation -->
    </S-Instructions>
  </S-Function>
</S-Functions>
```

**Result:**

- ❌ Upload fails - cannot redefine existing function
- Error: `VALIDATION_ERROR: Function 'Add' is already defined in program 'BasicMath'. Cannot redefine existing functions.`

## Nested Function Validation

The validation system handles complex nested function calls:

```xml
<S-Instruction-Argument name="functionArguments"
  value="(Add,x1,x2),(Multiply,(Subtract,x3,x4),x5)"/>
```

**Functions checked:**

- `Add`
- `Multiply`
- `Subtract`

All three must exist in the catalog or be defined locally.

## Benefits

1. **Prevents Runtime Errors**: Catches missing functions at upload time
2. **Ensures Consistency**: Function names are unique across all programs
3. **Clear Error Messages**: Users know exactly what's wrong and how to fix it
4. **Supports Reusability**: Programs can use functions from other programs in the catalog

## API Response Examples

### Success Response

```json
{
  "success": true,
  "programName": "MyProgram",
  "instructionCount": 15,
  "maxDegree": 3,
  "functions": ["MyFunction1", "MyFunction2"]
}
```

### Validation Error Response

```json
{
  "error": "VALIDATION_ERROR",
  "message": "Function 'Smaller_Than' is used in the program but not defined. Please ensure all referenced functions exist in the catalog or are defined in this file."
}
```

## Testing the Validation

You can test the validation by:

1. **Upload a program without defining required functions** → Should fail
2. **Upload a program with all functions defined** → Should succeed
3. **Upload a second program reusing functions from the first** → Should succeed
4. **Try to redefine an existing function** → Should fail
5. **Upload a program with duplicate name** → Should fail

## Debug Logging

When expansion debugging is enabled, you'll see:

```
=== EXPANSION DEBUG: Starting expansion to degree 1 ===
...
[expandQuote] Available functions: [CONST0, Add, Multiply, ...]
[expandQuote] ERROR: Function 'MissingFunc' not found!
```

This helps identify which functions are missing before upload validation catches them.
