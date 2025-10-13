package com.semulator.server.listener;

import com.semulator.server.realtime.UserUpdateServer;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * Listener to start/stop the user update server with the web application
 */
@WebListener
public class UserUpdateServerListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // Starting user update server
        UserUpdateServer.start();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Stopping user update server
        UserUpdateServer.stop();
    }
}
