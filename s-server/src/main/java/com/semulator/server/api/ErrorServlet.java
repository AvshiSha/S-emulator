package com.semulator.server.api;

import com.semulator.server.util.ServletUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Error handling servlet
 * Handles: /error
 */
@WebServlet(name = "ErrorServlet", urlPatterns = { "/error" })
public class ErrorServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleError(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleError(req, resp);
    }

    private void handleError(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer statusCode = (Integer) req.getAttribute("jakarta.servlet.error.status_code");
        String errorMessage = (String) req.getAttribute("jakarta.servlet.error.message");
        Throwable throwable = (Throwable) req.getAttribute("jakarta.servlet.error.exception");

        String code = "INTERNAL";
        String message = "Unknown error occurred";

        if (statusCode != null) {
            switch (statusCode) {
                case 404:
                    code = "NOT_FOUND";
                    message = "Resource not found";
                    break;
                case 400:
                    code = "VALIDATION_ERROR";
                    message = "Bad request";
                    break;
                case 401:
                    code = "UNAUTHORIZED";
                    message = "Authentication required";
                    break;
                case 403:
                    code = "FORBIDDEN";
                    message = "Access forbidden";
                    break;
                case 500:
                    code = "INTERNAL";
                    message = "Internal server error";
                    break;
                default:
                    code = "HTTP_" + statusCode;
                    message = "HTTP " + statusCode + " error";
            }
        }

        if (errorMessage != null && !errorMessage.isEmpty()) {
            message = errorMessage;
        }

        if (throwable != null) {
            message = throwable.getMessage();
        }

        ServletUtils.writeError(resp, statusCode != null ? statusCode : 500, code, message);
    }
}
