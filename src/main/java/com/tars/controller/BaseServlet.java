package com.tars.controller;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * Base servlet that provides action-based request routing using reflection.
 * <p>
 * This abstract class implements a front controller pattern where all HTTP requests
 * (both GET and POST) are routed to specific handler methods based on the {@code action}
 * request parameter. This eliminates the need for multiple servlet mappings and provides
 * a clean separation of concerns within each servlet.
 * </p>
 * <p>
 * <b>Routing Mechanism:</b>
 * <ol>
 *   <li>Extracts {@code action} parameter from request</li>
 *   <li>Uses reflection to find a method with matching name in the subclass</li>
 *   <li>Method signature must be: {@code private void methodName(HttpServletRequest, HttpServletResponse)}</li>
 *   <li>Sets method accessible (allows calling private methods)</li>
 *   <li>Invokes the method with request and response objects</li>
 * </ol>
 * </p>
 * <p>
 * <b>Usage Example:</b>
 * <pre>{@code
 * @WebServlet("/userServlet")
 * public class UserServlet extends BaseServlet {
 *     private void login(HttpServletRequest req, HttpServletResponse resp) throws IOException {
 *         // Handle login logic
 *     }
 *     
 *     private void register(HttpServletRequest req, HttpServletResponse resp) throws IOException {
 *         // Handle registration logic
 *     }
 * }
 * 
 * // Requests:
 * // POST /userServlet?action=login    → calls login()
 * // POST /userServlet?action=register → calls register()
 * }</pre>
 * </p>
 * <p>
 * <b>Important Notes:</b>
 * <ul>
 *   <li>All handler methods should be {@code private} to prevent direct external access</li>
 *   <li>Handler methods must accept HttpServletRequest and HttpServletResponse parameters</li>
 *   <li>Both GET and POST requests are handled by the same routing logic (doGet delegates to doPost)</li>
 *   <li>Character encoding is set to UTF-8 for proper internationalization support</li>
 *   <li>If action parameter doesn't match any method, RuntimeException is thrown</li>
 * </ul>
 * </p>
 * <p>
 * <b>Error Handling:</b> If the requested action method doesn't exist or cannot be invoked,
 * a RuntimeException wrapping the underlying exception is thrown. Subclasses should handle
 * exceptions within their handler methods and return appropriate error responses.
 * </p>
 *
 * @author Jflame
 * @version 1.0.0
 * @since 2026/3/20
 * @see AdminServlet
 * @see UserServlet
 * @see TAServlet
 * @see MOServlet
 */
public abstract class BaseServlet extends HttpServlet {
    
    /**
     * Routes POST requests to handler methods based on the action parameter.
     * <p>
     * This method implements the core routing logic:
     * <ol>
     *   <li>Sets request character encoding to UTF-8</li>
     *   <li>Extracts "action" parameter from request</li>
     *   <li>Finds matching method in subclass using reflection</li>
     *   <li>Makes method accessible (bypasses private modifier)</li>
     *   <li>Invokes method with request and response objects</li>
     * </ol>
     * </p>
     * <p>
     * <b>Method Resolution:</b> Uses {@link Class#getDeclaredMethod(String, Class...)}
     * to find the exact method signature: {@code void methodName(HttpServletRequest, HttpServletResponse)}.
     * Only declared methods in the immediate subclass are considered (not inherited methods).
     * </p>
     * <p>
     * <b>Exception Handling:</b> Wraps reflection exceptions in RuntimeException:
     * <ul>
     *   <li>NoSuchMethodException - Action parameter doesn't match any method</li>
     *   <li>IllegalAccessException - Method cannot be accessed (shouldn't occur with setAccessible)</li>
     *   <li>InvocationTargetException - Handler method threw an exception</li>
     * </ul>
     * </p>
     *
     * @param req  HttpServletRequest containing action parameter and request data
     * @param resp HttpServletResponse for sending response
     * @throws IOException if I/O error occurs during request processing
     * @throws RuntimeException if action method not found or invocation fails
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        String action = req.getParameter("action");
        Method declaredMethod;
        try {
            declaredMethod = this.getClass().getDeclaredMethod(action, HttpServletRequest.class, HttpServletResponse.class);
            declaredMethod.setAccessible(true);
            declaredMethod.invoke(this, req, resp);
        } catch (NoSuchMethodException | IllegalAccessException | java.lang.reflect.InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Delegates GET requests to POST handler for unified routing.
     * <p>
     * This method simply calls {@link #doPost(HttpServletRequest, HttpServletResponse)}
     * to ensure both GET and POST requests use the same action-based routing logic.
     * This allows flexible URL patterns while maintaining consistent behavior.
     * </p>
     * <p>
     * <b>Rationale:</b> Many actions (like viewing lists or checking status) are naturally
     * GET operations but should follow the same routing mechanism as POST operations.
     * This design simplifies the routing logic and reduces code duplication.
     * </p>
     *
     * @param req  HttpServletRequest containing action parameter
     * @param resp HttpServletResponse for sending response
     * @throws IOException if I/O error occurs during request processing
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doPost(req, resp);
    }
}
