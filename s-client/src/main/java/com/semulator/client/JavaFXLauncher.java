package com.semulator.client;

import javafx.application.Application;

/**
 * JavaFX Launcher for JavaFX 11+ compatibility
 * This is required when using modular JavaFX
 */
public class JavaFXLauncher {

    public static void main(String[] args) {
        // Launch the JavaFX application
        Application.launch(Main.class, args);
    }
}
