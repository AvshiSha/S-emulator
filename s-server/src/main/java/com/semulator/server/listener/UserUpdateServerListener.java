package com.semulator.server.listener;

import com.semulator.server.realtime.UserUpdateServer;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * Listener to start/stop the user update server (includes chat) with the web
 * application
 * 
 * NOTE: Disabled in favor of HTTP polling-based real-time updates
 * All communication now goes through port 8080 (HTTP REST API)
 */
@WebListener
public class UserUpdateServerListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // TCP socket server disabled - using HTTP polling instead
        // UserUpdateServer.start();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // TCP socket server disabled - using HTTP polling instead
        // UserUpdateServer.stop();
    }
}
