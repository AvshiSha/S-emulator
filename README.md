# Name: Avshalom Sharabani

# ID: 318799608

# Email: Avshalomsh@mta.ac.il

GitHub Link: https://github.com/AvshiSha/S-emulator

# S-Emulator: A Program Expansion and Execution System

## Bonus Feature: Saving and loading the system

As an additional enhancement to the core system, S-Emulator includes a comprehensive **Save/Load State** feature that allows users to persist their entire exercise session. This bonus functionality provides:

### **Save State Capabilities:**

State files are automatically named based on the loaded program (e.g., `programName.state`)

- **Directory Creation**: Automatically creates parent directories if they don't exist

### **Load State Capabilities:**

- **Full Session Restoration**: Restore the exact program and complete run history
- **Seamless Continuation**: Resume work exactly where you left off
- **State Validation**: Automatic validation of loaded state files

### **Implementation Details:**

- **Run History Tracking**: Preserves all execution history including inputs, outputs, and cycle counts
- **User-Friendly Interface**: Simple menu options (6 for Save, 7 for Load) with clear prompts

---

## Overview

S-Emulator is a Java-based system that implements a program expansion mechanism for S-Instructions, allowing users to load, expand, execute, and analyze programs written in a specialized instruction set. The system provides a comprehensive environment for understanding how high-level synthetic instructions can be expanded into basic instructions through multiple degrees of expansion.

## System Architecture

### Project Structure

```
S-emulator/
├── engine/                    # Core engine module
│   ├── src/semulator/
│   │   ├── execution/         # Program execution logic
│   │   ├── instructions/      # Instruction implementations
│   │   ├── label/            # Label management
│   │   ├── program/          # Program representation and expansion
│   │   ├── state/            # State management for save/load
│   │   └── variable/         # Variable system
│   └── engine.iml
├── UI/                       # User interface module
│   ├── src/ui/              # Console UI implementation
│   └── UI.iml
├── out/                      # Compiled output
└── README.md                # This file
```

### Key Components

#### 1. Engine Module (`engine/`)

The core computational engine that handles:

- **Program Loading**: XML-based program loading with validation
- **Instruction Expansion**: Multi-degree expansion of synthetic instructions
- **Execution**: Program execution with cycle counting
- **State Management**: Save/load functionality for exercise states

#### 2. UI Module (`UI/`)

The user interface that provides:

- **Console Interface**: Interactive menu-driven interface
- **Program Display**: Pretty-printing of programs and expansion results
- **Execution Control**: Input handling and result display
- **History Management**: Run history tracking and display

## Instruction System

### Instruction Types

#### Basic Instructions (Degree 0)

- **INCREASE**: `x <- x + 1`
- **DECREASE**: `x <- x - 1`
- **NEUTRAL**: `x <- x` (no-op)
- **JUMP_NOT_ZERO**: `IF x != 0 GOTO label`

#### Synthetic Instructions (Higher Degrees)

- **ZERO_VARIABLE** (Degree 1): `x <- 0`
- **GOTO_LABEL** (Degree 1): `GOTO label`
- **ASSIGNMENT** (Degree 2): `x <- y`
- **CONSTANT_ASSIGNMENT** (Degree 2): `x <- constant`
- **JUMP_ZERO** (Degree 2): `IF x == 0 GOTO label`
- **JUMP_EQUAL_CONSTANT** (Degree 3): `IF x == constant GOTO label`
- **JUMP_EQUAL_VARIABLE** (Degree 3): `IF x == y GOTO label`

### Expansion Mechanism

The system implements a sophisticated expansion mechanism where synthetic instructions are recursively expanded into basic instructions:

1. **Degree Calculation**: Each instruction has a predefined expansion degree
2. **Iterative Expansion**: Programs are expanded step-by-step through each degree
3. **Lineage Tracking**: Parent-child relationships are maintained for display purposes
4. **Fresh Name Generation**: New labels and variables are generated to avoid conflicts

## Key Features

### 1. Program Loading and Validation

- **XML Format**: Programs are defined in XML with structured instruction definitions
- **Comprehensive Validation**: Syntax, semantics, and reference validation
- **Error Reporting**: Detailed error messages for debugging

### 2. The Expansion mechanism

- **Flexible Expansion**: Choose expansion degree from 0 to maximum available
- **Creation Chain Display**: Visual representation of instruction lineage
- **Degree-Specific Row Numbering**: Each degree maintains its own instruction numbering

### 3. Program Execution

- **Input Handling**: Support for multiple input variables (x1, x2, etc.)
- **Cycle Counting**: Accurate cycle counting for performance analysis
- **Variable State Tracking**: Complete variable state monitoring
- **Result Display**: Formatted output of execution results

### 4. State Management

- **Save/Load Functionality**: Persistent storage of exercise states
- **Run History**: Complete history of program executions
- **Automatic File Naming**: State files named after loaded programs

### 5. Advanced Display Features

- **Pretty Printing**: Aligned, formatted program display
- **Creation Chains**: Visual representation of instruction expansion lineage
- **Grouped Display**: Instructions grouped by expansion degree
- **Row Numbering**: Consistent row numbering across all display modes

## Implementation Details

### Design Choices

#### 1. Modular Architecture

- **Separation of Concerns**: Engine logic separated from UI
- **Extensible Design**: Easy to add new instruction types
- **Clean Interfaces**: Well-defined contracts between modules

#### 2. Data Structures

- **Identity Maps**: Used for instruction tracking to handle object identity
- **Caching**: Depth calculations cached for performance
- **Immutable Design**: Core data structures are immutable where possible

#### 3. Error Handling

- **Graceful Degradation**: System continues operation despite errors
- **Comprehensive Validation**: Multiple validation layers
- **User-Friendly Messages**: Clear, actionable error messages

### Technical Implementation

#### Expansion Algorithm

#### Row Numbering Strategy

- **Degree-Specific**: Each expansion degree gets fresh row numbering (1, 2, 3, ...)
- **Lineage Preservation**: Original program row numbers preserved for degree 0
- **Display Consistency**: Consistent numbering across all display modes

#### State Serialization

- **Java Serialization**: Used for save/load functionality
- **Object Graph Preservation**: Complete state including run history
- **Error Recovery**: Graceful handling of serialization errors

## Usage Guide

### Main Menu Options

1. **Load Program**: Load an XML program file
2. **Show Program**: Display the loaded program
3. **Expand**: Expand program to chosen degree with creation chains
4. **Run**: Execute program with input values
5. **History**: View execution history
6. **Save State**: Save current exercise state
7. **Load State**: Load previously saved state
8. **Exit**: Exit the system

### Example Workflow

1. **Load a Program**: Choose option 1 and provide XML file path
2. **View Program**: Use option 2 to see the original program
3. **Expand**: Use option 3 to see expansion to degree 1 or 2
4. **Execute**: Use option 4 to run the program with inputs
5. **Save State**: Use option 6 to save your progress

### Optimization Strategies

- **Caching**: Depth calculations and width measurements cached
- **Efficient Data Structures**: Identity maps for O(1) lookups
- **Lazy Evaluation**: Computations performed only when needed

### Scalability

- **Memory Efficient**: Minimal object creation during expansion
- **Linear Complexity**: Expansion time scales linearly with degree
- **Bounded Growth**: Instruction count growth is predictable

### Extensibility Points

- **Instruction System**: Easy to add new instruction types
- **Display System**: Modular display components
- **Execution Engine**: Pluggable execution strategies
- **Storage Backend**: Configurable persistence layer

## Conclusion

S-Emulator provides a comprehensive environment for understanding program expansion and execution. Its modular design, comprehensive feature set, and user-friendly interface make it an excellent tool for educational and research purposes in the field of program transformation and execution.

The system successfully demonstrates how high-level synthetic instructions can be systematically expanded into basic instructions, providing insights into program compilation and optimization techniques.
