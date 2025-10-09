package com.semulator.client.ui.temp;

import java.util.List;
import java.util.ArrayList;
import javafx.scene.Scene;

/**
 * Temporary classes until we have the real ones from the original UI package.
 * These provide the basic functionality needed for the Exercise-2 UI
 * components.
 */
public class TempClasses {

    /**
     * Represents the result of a program run
     */
    public static class RunResult {
        private final int runNumber;
        private final int level;
        private final List<Long> inputs;
        private final long yValue;
        private final int cycles;

        public RunResult(int runNumber, int level, List<Long> inputs, long yValue, int cycles) {
            this.runNumber = runNumber;
            this.level = level;
            this.inputs = inputs;
            this.yValue = yValue;
            this.cycles = cycles;
        }

        public int runNumber() {
            return runNumber;
        }

        public int level() {
            return level;
        }

        public List<Long> inputs() {
            return inputs;
        }

        public long yValue() {
            return yValue;
        }

        public int cycles() {
            return cycles;
        }

        public String inputsCsv() {
            return inputs.stream()
                    .map(String::valueOf)
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");
        }
    }

    /**
     * Manages run history
     */
    public static class RunHistory {
        private final List<RunResult> runs = new ArrayList<>();
        private int nextRunNumber = 1;

        public void addRun(int level, List<Long> inputs, long yValue, int cycles) {
            runs.add(new RunResult(nextRunNumber++, level, inputs, yValue, cycles));
        }

        public void clear() {
            runs.clear();
            nextRunNumber = 1;
        }

        public boolean isEmpty() {
            return runs.isEmpty();
        }

        public List<RunResult> getAllRuns() {
            return new ArrayList<>(runs);
        }
    }

    /**
     * Theme enumeration
     */
    public enum Theme {
        LIGHT, DARK
    }

    /**
     * Manages theme and styling
     */
    public static class ThemeManager {
        private static ThemeManager instance;
        private Theme currentTheme = Theme.LIGHT;
        private String currentFontSize = "14px";

        public static ThemeManager getInstance() {
            if (instance == null) {
                instance = new ThemeManager();
            }
            return instance;
        }

        public Theme getCurrentTheme() {
            return currentTheme;
        }

        public String getCurrentFontSize() {
            return currentFontSize;
        }

        public void setTheme(Theme theme) {
            this.currentTheme = theme;
        }

        public void setFontSize(String fontSize) {
            this.currentFontSize = fontSize;
        }

        public void applyCurrentTheme(Scene scene) {
            // TODO: Implement theme application
        }
    }

    /**
     * Manages animation settings
     */
    public static class Animations {
        private static boolean enabled = true;

        public static boolean isEnabled() {
            return enabled;
        }

        public static void setEnabled(boolean enabled) {
            Animations.enabled = enabled;
        }
    }
}
