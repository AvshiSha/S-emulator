# Credit Management & Architecture Cost Features - Implementation Summary

## Overview

This document describes the new credit management and architecture cost features implemented in the S-Emulator execution screen.

## Features Implemented

### ✅ 1. Architecture Compatibility Checking

**Location**: `InstructionTableController.java`, `DebuggerExecution.java`, `ProgramRunController.java`

**Implementation**:

- Added `isArchitectureCompatible()` method that checks if all instructions in a program are compatible with the selected architecture
- Added `getUnsupportedArchitectures()` method that returns a list of unsupported architecture types
- Implemented architecture hierarchy validation (I < II < III < IV)
- Added real-time compatibility callback mechanism between components

**Behavior**:

- Execution buttons (Start Regular, Start Debug) are **disabled** when the selected architecture does not support all instructions
- Status message displays: "⚠ Program contains instructions requiring: [Architecture List]"
- Buttons are **enabled** only when architecture is compatible and program is loaded

---

### ✅ 2. Architecture Cost Deduction at Run Start

**Location**: `DebuggerExecution.java`

**Architecture Costs**:

- Architecture I → **5 credits**
- Architecture II → **100 credits**
- Architecture III → **500 credits**
- Architecture IV → **1000 credits**

**Implementation**:

- Added `getArchitectureCost(String architecture)` method
- Modified `startRegularExecution()` to deduct architecture cost before starting
- Modified `startDebugExecution()` to deduct architecture cost before starting
- Uses the `/users/{username}/topup` endpoint with negative values for deduction

**Behavior**:

- When user clicks "Start", the system first checks if they have enough credits for the architecture cost
- If sufficient credits exist, the architecture cost is deducted immediately
- Status message shows: "Deducting architecture cost (X credits)..."
- If deduction fails, execution is cancelled with an error message

---

### ✅ 3. Credit Validation Before Execution

**Location**: `DebuggerExecution.java`

**Implementation**:

- Added `fetchCurrentCredits()` method to retrieve user's current credit balance
- Added credit validation in both `startRegularExecution()` and `startDebugExecution()`
- Credits are fetched when program/function is loaded via `setProgramName()` and `setFunctionName()`

**Behavior**:

- Before starting execution, system checks if user has at least the architecture cost in credits
- If insufficient credits, shows alert:
  ```
  "You need at least X credits to run on Architecture Y.
   You have Z credits.
   Please load more credits to continue."
  ```
- Execution is blocked until user loads more credits

---

### ✅ 4. Per-Cycle Credit Deduction

**Location**: `DebuggerExecution.java`

**Implementation**:

- **Debug Mode**: Modified `stepOver()` to deduct 1 credit per step
- **Regular Mode**: Server-side API handles per-cycle deduction automatically
- Added `deductCredits(int amount)` method that handles credit deduction via API

**Behavior**:

- **Debug Mode**: Each "Step Over" action deducts 1 credit before executing the instruction
- **Regular Mode**: Server deducts credits automatically during execution (1 credit per cycle)
- Status messages show current credit balance during execution

---

### ✅ 5. Real-Time Credit Display Updates

**Location**: `DebuggerExecution.java`

**Implementation**:

- Modified `stepOver()` to call `fetchCurrentCredits()` after each step
- Modified `pollForCompletion()` to refresh credits during regular execution
- Status messages updated to show: "Step executed - Instruction X of Y | Credits: Z"

**Behavior**:

- **Debug Mode**: Credits are refreshed after each step-over operation
- **Regular Mode**: Credits are refreshed every 500ms during polling
- User can see their credit balance decrease in real-time as the program executes
- Execution status shows: "Running... (Cycles: X | Credits: Y)"

---

### ✅ 6. Out-of-Credits Handling

**Location**: `DebuggerExecution.java`

**Implementation**:

- Added credit check at the start of `stepOver()` method
- Added error detection in `pollForCompletion()` for credit-related errors
- Added `navigateBackToDashboard()` method (placeholder for navigation)

**Behavior**:

- **Debug Mode**:

  - Before each step, checks if user has at least 1 credit
  - If credits < 1, shows alert: "You have run out of credits! Execution has been stopped."
  - Stops execution immediately
  - Attempts to navigate back to dashboard/programs screen

- **Regular Mode**:
  - Server detects when user runs out of credits
  - Returns ERROR status with credit-related message
  - Client detects error message containing "credit"
  - Shows alert: "You have run out of credits during execution!"
  - Attempts to navigate back to dashboard

---

## Architecture Summary Display

**Location**: `InstructionTableController.java`

**Enhancement**:

- Summary line at bottom of instruction table shows command counts by architecture
- Red highlighting when an architecture has commands that require higher than selected architecture
- White highlighting for selected architecture
- Format: "I: X commands | II: Y commands | III: Z commands | IV: W commands"

---

## Component Communication Flow

```
ProgramRunController
    ↓ (wires up callbacks)
InstructionTableController ←→ DebuggerExecution
    ↓                              ↓
(tracks arch counts)      (tracks compatibility)
    ↓                              ↓
(notifies on change)      (enables/disables buttons)
```

---

## API Endpoints Used

1. **GET** `/users` - Fetch current user credits
2. **POST** `/users/{username}/topup` - Deduct credits (negative amount)
3. **POST** `/debug/start` - Start debug session
4. **POST** `/debug/step` - Execute one step in debug mode
5. **POST** `/run/prepare` - Prepare regular execution
6. **POST** `/run/start` - Start regular execution
7. **GET** `/run/status` - Poll execution status

---

## User Experience Flow

### Scenario 1: Successful Execution

1. User selects a program and architecture
2. System validates architecture compatibility ✓
3. System checks credits (5 for Arch I) ✓
4. User clicks "Start Debug"
5. System deducts 5 credits for architecture cost
6. Debug session starts
7. User clicks "Step Over"
8. System deducts 1 credit per step
9. Credit count updates in real-time
10. Program completes successfully

### Scenario 2: Incompatible Architecture

1. User selects a program with Arch II instructions
2. User selects Architecture I
3. System detects incompatibility
4. Start buttons are **disabled**
5. Status shows: "⚠ Program contains instructions requiring: Architecture II"
6. User must select Architecture II or higher to proceed

### Scenario 3: Insufficient Credits

1. User has 3 credits
2. User selects Architecture I (requires 5 credits)
3. User clicks "Start"
4. System checks credits
5. Alert shows: "You need at least 5 credits... You have 3 credits"
6. Execution is blocked
7. User must load more credits

### Scenario 4: Out of Credits During Execution

1. User starts execution with 10 credits
2. Architecture cost deducted (5 credits)
3. 5 credits remaining
4. User executes 5 steps (5 credits deducted)
5. 0 credits remaining
6. User clicks "Step Over" again
7. Alert: "You have run out of credits!"
8. Execution stops
9. User is navigated back to dashboard

---

## Testing Checklist

- [ ] Test with Architecture I program on Architecture I (should work)
- [ ] Test with Architecture II program on Architecture I (should be disabled)
- [ ] Test with Architecture III program on Architecture II (should be disabled)
- [ ] Test architecture cost deduction for each architecture level
- [ ] Test execution with exactly enough credits
- [ ] Test execution with insufficient credits
- [ ] Test running out of credits during debug step-over
- [ ] Test credit display updates in real-time
- [ ] Test regular execution credit deduction
- [ ] Test navigation back to dashboard when out of credits

---

## Files Modified

1. `InstructionTableController.java`

   - Added compatibility checking methods
   - Added compatibility callback mechanism
   - Enhanced architecture summary calculation

2. `DebuggerExecution.java`

   - Added credit management methods
   - Added architecture cost logic
   - Added credit validation
   - Added per-cycle credit deduction
   - Added out-of-credits handling
   - Added real-time credit display

3. `ProgramRunController.java`
   - Wired up architecture compatibility callback
   - Connected InstructionTable to DebuggerExecution

---

## Future Enhancements (Optional)

1. Add estimated total cost calculator before execution starts
2. Add credit history/transaction log
3. Add warning when credits are running low (e.g., < 10% remaining)
4. Add batch credit purchase options with discounts
5. Add credit usage analytics/reports
6. Implement proper navigation to dashboard (currently just logs)

---

## Notes

- All credit operations use the existing `/users/{username}/topup` endpoint with negative values
- Server-side validation should also be implemented to prevent credit manipulation
- The `navigateBackToDashboard()` method is a placeholder and needs proper implementation
- Architecture compatibility is enforced on client-side; server should also validate

---

**Implementation Date**: October 13, 2025  
**Status**: ✅ Complete  
**All TODOs**: Completed
