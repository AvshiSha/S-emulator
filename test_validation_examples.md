# Validation Test Examples

## Your Original Problem

Your "Reminder" program references these functions:

- `Smaller_Than`
- `Minus`
- `AND`
- `NOT`
- `EQUAL`
- `Bigger_Equal_Than`
- `CONST0`

But only `CONST0` was defined in the file!

## What Happens Now

### Before the Fix ❌

```
1. Upload Reminder.xml
2. System accepts the upload
3. Try to expand → CRASH!
   Error: Function 'Smaller_Than' not found
```

### After the Fix ✅

```
1. Upload Reminder.xml
2. System validates → REJECTS!
   Error: "VALIDATION_ERROR: Function 'Smaller_Than' is used
          in the program but not defined. Please ensure all
          referenced functions exist in the catalog or are
          defined in this file."
3. User fixes the XML by either:
   a) Defining missing functions in the file, OR
   b) Uploading programs with those functions first
4. Upload succeeds
5. Expansion works perfectly!
```

## How to Fix Your XML

### Option 1: Define All Functions Locally

Add to `<S-Functions>` section:

```xml
<S-Functions>
  <!-- Your existing function -->
  <S-Function name="CONST0" user-string="C0">
    <S-Instructions>
      <S-Instruction type="synthetic" name="CONSTANT_ASSIGNMENT">
        <S-Variable>y</S-Variable>
        <S-Instruction-Arguments>
          <S-Instruction-Argument name="constantValue" value="0"/>
        </S-Instruction-Arguments>
      </S-Instruction>
    </S-Instructions>
  </S-Function>

  <!-- Add these missing functions -->
  <S-Function name="Smaller_Than" user-string="ST">
    <S-Instructions>
      <!-- Implementation here -->
    </S-Instructions>
  </S-Function>

  <S-Function name="Minus" user-string="MIN">
    <S-Instructions>
      <!-- Implementation here -->
    </S-Instructions>
  </S-Function>

  <S-Function name="AND" user-string="AND">
    <S-Instructions>
      <!-- Implementation here -->
    </S-Instructions>
  </S-Function>

  <S-Function name="NOT" user-string="NOT">
    <S-Instructions>
      <!-- Implementation here -->
    </S-Instructions>
  </S-Function>

  <S-Function name="EQUAL" user-string="EQ">
    <S-Instructions>
      <!-- Implementation here -->
    </S-Instructions>
  </S-Function>

  <S-Function name="Bigger_Equal_Than" user-string="BET">
    <S-Instructions>
      <!-- Implementation here -->
    </S-Instructions>
  </S-Function>
</S-Functions>
```

### Option 2: Use Functions From Catalog

1. First upload a "Library" program with common functions:

```xml
<S-Program name="MathLibrary">
  <S-Instructions>
    <!-- Empty or minimal main program -->
    <S-Instruction type="basic" name="INCREASE">
      <S-Variable>y</S-Variable>
    </S-Instruction>
  </S-Instructions>
  <S-Functions>
    <S-Function name="Smaller_Than" user-string="ST">...</S-Function>
    <S-Function name="Minus" user-string="MIN">...</S-Function>
    <S-Function name="AND" user-string="AND">...</S-Function>
    <S-Function name="NOT" user-string="NOT">...</S-Function>
    <S-Function name="EQUAL" user-string="EQ">...</S-Function>
    <S-Function name="Bigger_Equal_Than" user-string="BET">...</S-Function>
  </S-Functions>
</S-Program>
```

2. Then upload your "Reminder" program - it will find the functions in the catalog!

## Test Sequence

### Test 1: Missing Functions (Should FAIL)

```bash
POST /api/upload
Body: Your original Reminder.xml

Expected Response:
{
  "error": "VALIDATION_ERROR",
  "message": "Function 'Smaller_Than' is used in the program but not defined. Please ensure all referenced functions exist in the catalog or are defined in this file."
}
```

### Test 2: With Functions Defined (Should SUCCEED)

```bash
POST /api/upload
Body: Reminder.xml with all functions defined

Expected Response:
{
  "success": true,
  "programName": "Reminder",
  "instructionCount": 9,
  "maxDegree": ...,
  "functions": ["CONST0", "Smaller_Than", "Minus", "AND", "NOT", "EQUAL", "Bigger_Equal_Than"]
}
```

### Test 3: Program Name Conflict (Should FAIL)

```bash
POST /api/upload
Body: Another program also named "Reminder"

Expected Response:
{
  "error": "VALIDATION_ERROR",
  "message": "Program with name 'Reminder' already exists in the catalog. Please choose a unique name."
}
```

### Test 4: Function Redefinition (Should FAIL)

```bash
# Assume "Smaller_Than" already in catalog
POST /api/upload
Body: New program trying to define "Smaller_Than" again

Expected Response:
{
  "error": "VALIDATION_ERROR",
  "message": "Function 'Smaller_Than' is already defined in program 'Reminder'. Cannot redefine existing functions."
}
```

## Debug Output

With debug logging enabled, you'll see detailed output when trying to expand:

```
=== EXPANSION DEBUG: Starting expansion to degree 1 ===
Initial program has 9 instructions
  [1] IFZ x2
  [2] ASSIGN z3
  [3] QUOTE z2
  [4] JUMP_NOT_ZERO z2
  [5] QUOTE z3
  [6] INCREASE z4
  [7] QUOTE z1
  [8] JUMP_NOT_ZERO z1
  [9] ASSIGN y

--- EXPANSION STEP 1 of 1 ---
  Processing: QUOTE z2 (Label: EMPTY)
    -> Synthetic instruction, expanding...
      [expandOne] Instruction name: 'QUOTE'
      [expandOne] Expanding QUOTE instruction
      [expandOne] QUOTE function: Smaller_Than
      [expandOne] QUOTE arguments: [x1, x2]
        [expandQuote] Expanding function: Smaller_Than
        [expandQuote] Available functions: [CONST0]
        [expandQuote] Target variable: z2
        [expandQuote] Number of arguments: 2
        [expandQuote] Function body has X instructions
        ...
```

## Summary

The validation system now catches these errors **before** they cause runtime problems:

✅ **Catches**: Missing function references  
✅ **Catches**: Duplicate program names  
✅ **Catches**: Function redefinition conflicts  
✅ **Provides**: Clear, actionable error messages  
✅ **Supports**: Function reuse across programs  
✅ **Prevents**: Runtime expansion failures

Your programs will now fail fast with clear messages about what's missing, instead of crashing during expansion!
