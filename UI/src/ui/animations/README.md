# S-Emulator Animation System

This directory contains the animation system for the S-Emulator, providing visual feedback during program execution.

## Overview

The animation system consists of three main animations:

1. **Executed Row Pulse** - Highlights instruction table rows when they are executed
2. **Data Flow Trace** - Shows data flow between UI components with animated dots
3. **Register/Variable Blip** - Animates variable cells when their values change

## Components

### Core Classes

- `Animations.java` - Global animation control with enable/disable functionality
- `RowPulseAnimation.java` - Row pulse animation implementation
- `DataFlowTraceAnimation.java` - Data flow trace animation implementation
- `VariableBlipAnimation.java` - Variable blip animation implementation
- `animations.css` - CSS styles for animation effects

### Integration Points

The animations are integrated into the existing UI components:

- **Header Component** - Contains the animation toggle checkbox
- **DebuggerExecution Component** - Triggers animations during step execution
- **InstructionTable Component** - Handles row pulse animations
- **Main View** - Loads the animation CSS styles

## Usage

### Enabling/Disabling Animations

Users can toggle animations using the "Animations" checkbox in the header component. The setting is persisted across application restarts.

### Programmatic Control

```java
// Check if animations are enabled
boolean enabled = Animations.isEnabled();

// Enable/disable animations
Animations.setEnabled(true);

// Toggle animations
boolean newState = Animations.toggle();

// Execute animation only if enabled
Animations.playIfEnabled(animation);
```

### Animation Triggers

#### Row Pulse Animation

Triggered automatically when instructions are executed in debug mode:

```java
// In InstructionTable component
RowPulseAnimation.pulseRow(tableRow);
```

#### Variable Blip Animation

Triggered when variable values change:

```java
// In DebuggerExecution component
VariableBlipAnimation.blipCell(variableCell);
```

#### Data Flow Trace Animation

Triggered for QUOTE and JUMP_EQUAL_FUNCTION instructions:

```java
// In DebuggerExecution component
DataFlowTraceAnimation.traceFlow(startNode, endNode, overlayLayer);
```

## Performance Considerations

- All animations respect the global enable/disable setting
- Animations are capped at 2 seconds maximum duration
- CSS includes performance optimizations with `-fx-cache: true`
- Accessibility support with `prefers-reduced-motion` media query

## Customization

### Animation Durations

Default durations can be modified in each animation class:

- Row Pulse: 600ms
- Data Flow Trace: 900ms
- Variable Blip: 160ms scale + 220ms fade

### Visual Styling

Animation colors and effects can be customized in `animations.css`:

- Row pulse: Warm amber background and border
- Data flow: Cyan token and light cyan path
- Variable blip: Scale and fade effects

### Theme Support

The CSS includes support for:

- Dark theme adjustments
- High contrast theme adjustments
- Screen reader friendly mode

## Requirements

- JavaFX 11+ (for animation classes)
- Java 11+ (for the application)
- CSS3 support (for styling)

## Notes

The animation system is designed to be:

- **Non-intrusive** - Can be completely disabled
- **Performant** - Minimal CPU/GPU impact
- **Accessible** - Respects system accessibility preferences
- **Extensible** - Easy to add new animation types
