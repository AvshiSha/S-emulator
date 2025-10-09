package com.semulator.client;

import com.semulator.client.service.ApiClient;
import javafx.stage.Stage;

/**
 * Global application context for managing state and navigation
 */
public class AppContext {
    private static final AppContext INSTANCE = new AppContext();

    private Stage mainStage;
    private ApiClient apiClient;
    private String currentUser;

    private AppContext() {
        this.apiClient = new ApiClient();
    }

    public static AppContext getInstance() {
        return INSTANCE;
    }

    public Stage getMainStage() {
        return mainStage;
    }

    public void setMainStage(Stage mainStage) {
        this.mainStage = mainStage;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public String getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(String currentUser) {
        this.currentUser = currentUser;
    }

    public void setAuthToken(String token) {
        apiClient.setAuthToken(token);
    }
}
