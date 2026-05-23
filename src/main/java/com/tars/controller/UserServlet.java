package com.tars.controller;

import com.google.code.kaptcha.Constants;
import com.tars.entity.bean.User;
import com.tars.entity.dto.user.UserDTO;
import com.tars.service.AdminService;
import com.tars.service.UserService;
import com.tars.util.RespUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Servlet for handling user authentication and account management operations.
 * <p>
 * This servlet provides endpoints for:
 * <ul>
 *   <li><b>Login</b>: User authentication with progressive CAPTCHA protection</li>
 *   <li><b>Registration</b>: New TA account creation with username uniqueness check</li>
 *   <li><b>Username Check</b>: AJAX endpoint for real-time username availability validation</li>
 *   <li><b>Profile Update</b>: Modify username and/or password</li>
 *   <li><b>Logout</b>: Session cleanup and redirect to home page</li>
 *   <li><b>CAPTCHA Status</b>: Check if CAPTCHA is required for login</li>
 * </ul>
 * </p>
 * <p>
 * <b>Progressive CAPTCHA Strategy:</b>
 * To balance security and user experience, CAPTCHA is only required after failed login attempts:
 * <ul>
 *   <li>First attempt: No CAPTCHA required</li>
 *   <li>After 1+ failures: CAPTCHA required for all subsequent attempts</li>
 *   <li>After successful login: Failure count reset, CAPTCHA not required next time</li>
 * </ul>
 * This prevents brute-force attacks while avoiding unnecessary friction for legitimate users.
 * </p>
 * <p>
 * <b>CAPTCHA Validation:</b> When required, validates:
 * <ul>
 *   <li>CAPTCHA is provided (not null/empty)</li>
 *   <li>CAPTCHA exists in session</li>
 *   <li>CAPTCHA hasn't expired (60-second timeout)</li>
 *   <li>CAPTCHA matches (case-insensitive comparison)</li>
 * </ul>
 * </p>
 * <p>
 * <b>Session Management:</b>
 * <ul>
 *   <li>On successful login: Stores UserDTO in session attribute "user"</li>
 *   <li>On logout: Removes "user" attribute and redirects to home</li>
 *   <li>Failure tracking: Stores LOGIN_FAIL_COUNT in session</li>
 *   <li>CAPTCHA storage: Uses KAPTCHA_SESSION_KEY and KAPTCHA_SESSION_DATE</li>
 * </ul>
 * </p>
 * <p>
 * <b>Role-Based Redirection:</b> After successful login, users are redirected based on role:
 * <ul>
 *   <li>Admin (role=0): → Admin account list page</li>
 *   <li>TA (role=1): → TA application history page</li>
 *   <li>MO (role=2): → MO position management page</li>
 * </ul>
 * </p>
 * <p>
 * <b>Request Mapping:</b> All operations are routed through {@link BaseServlet} using
 * the {@code action} parameter:
 * <pre>
 * POST /userServlet?action=login        → login()
 * POST /userServlet?action=register     → register()
 * GET  /userServlet?action=checkUsername → checkUsername()
 * POST /userServlet?action=modifyUser   → modifyUser()
 * GET  /userServlet?action=logout       → logout()
 * GET  /userServlet?action=getCaptchaStatus → getCaptchaStatus()
 * </pre>
 * </p>
 *
 * @author Jflame
 * @version 1.0.0
 * @since 2026/3/23
 * @see UserService
 * @see BaseServlet
 * @see RespUtils
 */
@WebServlet(name = "UserServlet", urlPatterns = "/userServlet")
public class UserServlet extends BaseServlet {

    private static final Logger log = LoggerFactory.getLogger(UserServlet.class);

    private UserService userService;

    /** Session key for storing CAPTCHA text */
    public static final String KAPTCHA_SESSION_KEY = "KAPTCHA_SESSION_KEY";

    /** Session key for storing CAPTCHA generation timestamp */
    public static final String KAPTCHA_SESSION_DATE = "KAPTCHA_SESSION_DATE";

    /** Session key for tracking consecutive login failures */
    public static final String LOGIN_FAIL_COUNT = "LOGIN_FAIL_COUNT";

    /** CAPTCHA expiration time in milliseconds (60 seconds) */
    public static final long CAPTCHA_EXPIRY_TIME = 60000; // 60 seconds

    /**
     * Initializes the servlet and creates UserService instance.
     * <p>
     * Called once when the servlet is first loaded by the container.
     * </p>
     *
     * @throws ServletException if initialization fails
     */
    @Override
    public void init() throws ServletException {
        super.init();
        userService = new UserService();
    }

    /**
     * Authenticates user with username, password, and optional CAPTCHA.
     * <p>
     * This method implements progressive CAPTCHA protection:
     * <ol>
     *   <li>Retrieves login failure count from session (defaults to 0)</li>
     *   <li>If failCount >= 1, requires and validates CAPTCHA:
     *     <ul>
     *       <li>Checks CAPTCHA is provided</li>
     *       <li>Verifies CAPTCHA exists in session</li>
     *       <li>Validates CAPTCHA hasn't expired (60s timeout)</li>
     *       <li>Compares CAPTCHA case-insensitively</li>
     *     </ul>
     *   </li>
     *   <li>Encrypts password with MD5</li>
     *   <li>Calls UserService.login() for authentication</li>
     *   <li>If login succeeds:
     *     <ul>
     *       <li>Checks if account is frozen (status=1)</li>
     *       <li>Stores UserDTO in session</li>
     *       <li>Clears failure count and CAPTCHA data</li>
     *       <li>Returns redirect URL based on user role</li>
     *     </ul>
     *   </li>
     *   <li>If login fails:
     *     <ul>
     *       <li>Increments failure count</li>
     *       <li>Returns error with requireCaptcha flag if failCount >= 1</li>
     *     </ul>
     *   </li>
     * </ol>
     * </p>
     * <p>
     * <b>Response Format (Success):</b>
     * <pre>{@code
     * {
     *   "success": true,
     *   "message": "Login successful",
     *   "data": {
     *     "role": 1,
     *     "redirectUrl": "taServlet?action=listApplied&page=1&filter=all&order=applyAt",
     *     "requireCaptcha": false
     *   }
     * }
     * }</pre>
     * </p>
     * <p>
     * <b>Response Format (Failure):</b>
     * <pre>{@code
     * {
     *   "success": false,
     *   "message": "Invalid username or password. Verification code required.",
     *   "data": {
     *     "requireCaptcha": true,
     *     "failCount": 2
     *   }
     * }
     * }</pre>
     * </p>
     * <p>
     * <b>HTTP Status Codes:</b>
     * <ul>
     *   <li>200 OK - Login successful</li>
     *   <li>400 Bad Request - CAPTCHA validation failed</li>
     *   <li>401 Unauthorized - Invalid credentials</li>
     *   <li>403 Forbidden - Account is frozen</li>
     * </ul>
     * </p>
     *
     * @param req  HttpServletRequest containing username, password, and optional captcha
     * @param resp HttpServletResponse for sending JSON response
     * @throws IOException if I/O error occurs
     * @see UserService#login(String, String)
     * @see #getRedirectUrl(int)
     */
    private void login(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        String captcha = req.getParameter("captcha");

        Integer failCount = (Integer) req.getSession().getAttribute(LOGIN_FAIL_COUNT);
        if (failCount == null) {
            failCount = 0;
        }

        boolean requireCaptcha = failCount >= 1;

        if (requireCaptcha) {
            if (captcha == null || captcha.trim().isEmpty()) {
                RespUtils.writeError(resp, "Please enter verification code", HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            String sessionCaptcha = (String) req.getSession().getAttribute(KAPTCHA_SESSION_KEY);
            Long captchaTime = (Long) req.getSession().getAttribute(KAPTCHA_SESSION_DATE);

            if (sessionCaptcha == null || captchaTime == null) {
                RespUtils.writeError(resp, "Verification code expired, please refresh", HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            long currentTime = System.currentTimeMillis();
            if (currentTime - captchaTime > CAPTCHA_EXPIRY_TIME) {
                req.getSession().removeAttribute(KAPTCHA_SESSION_KEY);
                req.getSession().removeAttribute(KAPTCHA_SESSION_DATE);
                RespUtils.writeError(resp, "Verification code expired, please refresh", HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            if (!sessionCaptcha.equalsIgnoreCase(captcha.trim())) {
                RespUtils.writeError(resp, "Invalid verification code", HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
        }

        password = userService.encryptPassword(password);

        UserDTO user = userService.login(username, password);
        
        if (user != null) {
            if (user.getStatus() == 1) {
                RespUtils.writeError(resp, "Account is frozen. Please contact administrator.", HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            
            req.getSession().setAttribute("user", user);
            req.getSession().removeAttribute(LOGIN_FAIL_COUNT);
            req.getSession().removeAttribute(KAPTCHA_SESSION_KEY);
            req.getSession().removeAttribute(KAPTCHA_SESSION_DATE);
            
            Map<String, Object> data = new HashMap<>();
            data.put("role", user.getRole());
            data.put("redirectUrl", getRedirectUrl(user.getRole()));
            data.put("requireCaptcha", false);
            
            RespUtils.writeSuccess(resp, data, "Login successful");
        } else {
            failCount++;
            req.getSession().setAttribute(LOGIN_FAIL_COUNT, failCount);
            
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("requireCaptcha", failCount >= 1);
            errorData.put("failCount", failCount);
            
            String message = "Invalid username or password";
            if (failCount >= 1) {
                message = "Invalid username or password. Verification code required.";
            }
            
            RespUtils.writeError(resp, errorData, message, HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    /**
     * Determines the redirect URL based on user role after successful login.
     * <p>
     * Each role is directed to their respective module's default page with appropriate
     * query parameters for initial view state.
     * </p>
     *
     * @param role User role (0=Admin, 1=TA, 2=MO)
     * @return Redirect URL string for the role's default page
     */
    private String getRedirectUrl(int role) {
        switch (role) {
            case 0:
                return "adminServlet?action=listAccounts&filter=all&order=name";
            case 1:
                return "taServlet?action=listApplied&page=1&filter=all&order=applyAt";
            case 2:
                return "moServlet?action=listPosition&page=1&filter=all&order=postDate";
            default:
                return "views/user/login.jsp";
        }
    }

    /**
     * Checks if a username is available for registration (AJAX endpoint).
     * <p>
     * This method is called via AJAX during registration form input to provide
     * real-time feedback on username availability.
     * </p>
     * <p>
     * <b>Response Format:</b>
     * <pre>{@code
     * // Available
     * {"success": true, "message": "Username available", "data": null}
     * 
     * // Taken
     * {"success": false, "message": "Username already exists", "data": null}
     * }</pre>
     * </p>
     *
     * @param req  HttpServletRequest containing username parameter
     * @param resp HttpServletResponse for sending JSON response
     * @throws IOException if I/O error occurs
     * @see UserService#checkUserExist(String)
     */
    private void checkUsername(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String username = req.getParameter("username");
        boolean exists = userService.checkUserExist(username);
        
        if (exists) {
            RespUtils.writeError(resp, "Username already exists");
        } else {
            RespUtils.writeSuccess(resp, "Username available");
        }
    }

    /**
     * Registers a new TA user account.
     * <p>
     * This method performs the following steps:
     * <ol>
     *   <li>Validates username uniqueness via {@link UserService#checkUserExist(String)}</li>
     *   <li>Creates new User object with:
     *     <ul>
     *       <li>Username from request</li>
     *       <li>MD5-encrypted password</li>
     *       <li>Default role=1 (TA)</li>
     *       <li>Auto-generated UUID and timestamps (via User constructor)</li>
     *     </ul>
     *   </li>
     *   <li>Saves user via {@link UserService#saveUser(User)}</li>
     *   <li>Returns success or error response</li>
     * </ol>
     * </p>
     * <p>
     * <b>Default Role:</b> All registrations default to TA role (role=1).
     * MO accounts must be created by administrators via {@link AdminService#createMOAccount(User, com.tars.entity.bean.MOProfile)}.
     * </p>
     * <p>
     * <b>Response Format (Success):</b>
     * <pre>{@code
     * {"success": true, "message": "Registration successful", "data": null}
     * }</pre>
     * </p>
     * <p>
     * <b>Response Format (Failure):</b>
     * <pre>{@code
     * // Username taken
     * {"success": false, "message": "Username already exists", "data": null}
     * 
     * // Server error
     * {"success": false, "message": "Registration failed", "data": null}
     * }</pre>
     * </p>
     *
     * @param req  HttpServletRequest containing username and password parameters
     * @param resp HttpServletResponse for sending JSON response
     * @throws IOException if I/O error occurs
     * @see UserService#saveUser(User)
     */
    private void register(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        
        // Check if username already exists
        if (userService.checkUserExist(username)) {
            RespUtils.writeError(resp, "Username already exists");
            return;
        }
        
        // Create new user
        User user = new User();
        user.setName(username);
        user.setPassword(userService.encryptPassword(password));
        user.setRole(1); // Default to TA role
        
        // Save user
        boolean saved = userService.saveUser(user);
        
        if (saved) {
            RespUtils.writeSuccess(resp, "Registration successful");
        } else {
            RespUtils.writeError(resp, "Registration failed", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }


    /**
     * Updates user profile information (username and/or password).
     * <p>
     * This method allows users to modify their account details:
     * <ol>
     *   <li>Retrieves existing user by userId</li>
     *   <li>If username is changing, validates uniqueness</li>
     *   <li>Encrypts new password if provided (null if not changing)</li>
     *   <li>Creates source User object with updates</li>
     *   <li>Calls {@link UserService#updateUser(User)} for partial update</li>
     *   <li>Returns success with userId or error response</li>
     * </ol>
     * </p>
     * <p>
     * <b>Update Behavior:</b>
     * <ul>
     *   <li>Username: Updated if different from current, must be unique</li>
     *   <li>Password: Only updated if provided (non-null and non-empty), otherwise preserved</li>
     *   <li>Role/Status: Not modifiable through this endpoint</li>
     * </ul>
     * </p>
     * <p>
     * <b>Validation:</b> If changing username, checks for conflicts with existing users
     * to prevent duplicate usernames.
     * </p>
     *
     * @param req  HttpServletRequest containing userId, username, and optional password
     * @param resp HttpServletResponse for sending JSON response
     * @throws IOException if I/O error occurs
     * @see UserService#updateUser(User)
     */
    private void modifyUser(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String userId = req.getParameter("userId");
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        User exit = userService.getUserById(userId);
        if (exit == null) {
            log.warn("User not found, userId: {}", userId);
            RespUtils.writeError(resp, "User not found", HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (!exit.getName().equals(username)) {
            if (userService.checkUserExist(username)) {
                log.warn("username already exist, username: {}", username);
                RespUtils.writeError(resp, "Username already exists");
                return;
            }
        }

        password = password != null && !password.isEmpty() ? userService.encryptPassword(password) : null;

        User source = new User(userId, username, password);

        boolean updated = userService.updateUser(source);
        log.info("update user status success, userId: {}", userId);

        if (updated) {
            Map<String, String> data = new HashMap<>();
            data.put("userId", userId);
            RespUtils.writeSuccess(resp, data, "Update successful");
        } else {
            RespUtils.writeError(resp, "Update failed", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Logs out the current user by clearing session and redirecting to home page.
     * <p>
     * This method performs session cleanup:
     * <ol>
     *   <li>Removes "user" attribute from session</li>
     *   <li>Logs logout event with userId</li>
     *   <li>Redirects to application root (context path + "/")</li>
     * </ol>
     * </p>
     * <p>
     * <b>Note:</b> Other session attributes (like LOGIN_FAIL_COUNT) are not explicitly
     * cleared. They will expire naturally or be overwritten on next login.
     * </p>
     *
     * @param req  HttpServletRequest containing userId parameter
     * @param resp HttpServletResponse for redirect
     * @throws IOException if I/O error occurs during redirect
     */
    private void logout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String userId = req.getParameter("userId");
        req.getSession().removeAttribute("user");

        log.info("logout user status success, userId: {}", userId);

        resp.sendRedirect(req.getContextPath() + "/");
    }

    /**
     * Returns the current CAPTCHA requirement status based on login failure count.
     * <p>
     * This AJAX endpoint is called by the frontend to determine whether to display
     * the CAPTCHA input field on the login form.
     * </p>
     * <p>
     * <b>Logic:</b>
     * <ul>
     *   <li>If failCount is null or 0 → requireCaptcha = false</li>
     *   <li>If failCount >= 1 → requireCaptcha = true</li>
     * </ul>
     * </p>
     * <p>
     * <b>Response Format:</b>
     * <pre>{@code
     * {
     *   "success": true,
     *   "message": "success",
     *   "data": {
     *     "requireCaptcha": true,
     *     "failCount": 2
     *   }
     * }
     * }</pre>
     * </p>
     * <p>
     * <b>Use Case:</b> Frontend calls this on page load to decide whether to show
     * CAPTCHA field initially, then calls again after failed login to update UI.
     * </p>
     *
     * @param req  HttpServletRequest for accessing session
     * @param resp HttpServletResponse for sending JSON response
     * @throws IOException if I/O error occurs
     */
    private void getCaptchaStatus(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer failCount = (Integer) req.getSession().getAttribute(LOGIN_FAIL_COUNT);
        boolean requireCaptcha = failCount != null && failCount >= 1;
        
        Map<String, Object> data = new HashMap<>();
        data.put("requireCaptcha", requireCaptcha);
        data.put("failCount", failCount != null ? failCount : 0);
        
        RespUtils.writeSuccess(resp, data);
    }

}
