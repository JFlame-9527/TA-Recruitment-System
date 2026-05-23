package com.tars.controller;

import com.tars.entity.bean.MOProfile;
import com.tars.entity.bean.User;
import com.tars.entity.QueryCondition;
import com.tars.entity.dto.admin.MOProDTO;
import com.tars.entity.dto.admin.TAProDTO;
import com.tars.entity.dto.admin.UserDetailDTO;
import com.tars.entity.dto.user.UserDTO;
import com.tars.service.AdminService;
import com.tars.util.BeanUtils;
import com.tars.util.RespUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servlet for administrator operations including user management and profile administration.
 * <p>
 * This servlet provides comprehensive admin functionality:
 * <ul>
 *   <li><b>Account Listing</b>: View TA and MO accounts with filtering, sorting, and pagination</li>
 *   <li><b>Account Management</b>: Delete users, update account status (active/frozen)</li>
 *   <li><b>Profile Viewing</b>: View detailed TA and MO profiles</li>
 *   <li><b>Profile Editing</b>: Update MO profile information</li>
 *   <li><b>User Updates</b>: Modify user credentials (username, password) with validation</li>
 *   <li><b>MO Account Creation</b>: Create new MO accounts with associated profiles</li>
 * </ul>
 * </p>
 * <p>
 * <b>Access Control:</b> All operations require admin authentication (role=0).
 * The {@link #verifyAdmin(HttpServletRequest, HttpServletResponse, Object)} method validates:
 * <ol>
 *   <li>User is logged in (session contains "user" attribute)</li>
 *   <li>User object is valid UserDTO instance</li>
 *   <li>User has admin role (role=0)</li>
 * </ol>
 * Failed validation results in redirect to login page or HTTP 401/403 error.
 * </p>
 * <p>
 * <b>File Upload Configuration:</b> Supports file uploads up to 10MB per file,
 * with total request size limit of 50MB. Used for profile picture uploads.
 * </p>
 * <p>
 * <b>Request Mapping:</b> All operations are routed through {@link BaseServlet} using
 * the {@code action} parameter:
 * <pre>
 * GET  /adminServlet?action=listAccounts      → listAccounts() [Full page load]
 * GET  /adminServlet?action=loadAccountsPage  → loadAccountsPage() [AJAX pagination]
 * POST /adminServlet?action=deleteUser        → deleteUser()
 * POST /adminServlet?action=updateStatus      → updateStatus()
 * GET  /adminServlet?action=getTAProfile      → getTAProfile()
 * GET  /adminServlet?action=getMOProfile      → getMOProfile()
 * POST /adminServlet?action=updateMOProfile   → updateMOProfile()
 * POST /adminServlet?action=updateUser        → updateUser()
 * POST /adminServlet?action=createMOAccount   → createMOAccount()
 * </pre>
 * </p>
 * <p>
 * <b>Pagination Strategy:</b> Two modes supported:
 * <ul>
 *   <li><b>Full Page Load</b> (listAccounts): Returns complete JSP view with TA and MO lists side-by-side</li>
 *   <li><b>AJAX Pagination</b> (loadAccountsPage): Returns JSON data for dynamic table updates without page reload</li>
 * </ul>
 * Both modes support filtering by status and sorting by various fields.
 * </p>
 *
 * @author wangyue
 * @version 2.0.0
 * @since 2026/4/14
 * @see AdminService
 * @see BaseServlet
 * @see RespUtils
 */
@Slf4j
@WebServlet(name = "AdminServlet", value = "/adminServlet")
@MultipartConfig(
        fileSizeThreshold = 0,
        maxFileSize = 1024 * 1024 * 10,
        maxRequestSize = 1024 * 1024 * 50
)
public class AdminServlet extends BaseServlet {

    private AdminService adminService;

    /**
     * Initializes the servlet and creates AdminService instance.
     *
     * @throws ServletException if initialization fails
     */
    @Override
    public void init() throws ServletException {
        super.init();
        adminService = new AdminService();
    }

    /**
     * Lists TA and MO accounts with full page rendering.
     * <p>
     * This method loads both TA and MO account lists simultaneously and forwards
     * to the admin home page for display. Used for initial page load.
     * </p>
     * <p>
     * <b>Process:</b>
     * <ol>
     *   <li>Verifies admin authentication</li>
     *   <li>Extracts query conditions from request (filter, order, page)</li>
     *   <li>Fetches TA accounts (role=1) with pagination</li>
     *   <li>Fetches MO accounts (role=2) with pagination</li>
     *   <li>Sets request attributes for JSP rendering:
     *     <ul>
     *       <li>taList, taCondition, taTotalPages - TA data</li>
     *       <li>moList, moCondition, moTotalPages - MO data</li>
     *       <li>activeRole - Currently selected tab (default: 1 for TA)</li>
     *     </ul>
     *   </li>
     *   <li>Forwards to /views/admin/home.jsp</li>
     * </ol>
     * </p>
     * <p>
     * <b>Query Conditions:</b> Supports filtering by account status and sorting by
     * username, creation date, etc. via {@link QueryCondition}.
     * </p>
     *
     * @param req  HttpServletRequest containing query parameters
     * @param resp HttpServletResponse for forwarding to JSP
     * @throws ServletException if servlet error occurs
     * @throws IOException      if I/O error occurs
     * @see AdminService#getAccountsByRole(int, QueryCondition, String)
     * @see AdminService#getAccountPages(int, QueryCondition, String)
     */
    private void listAccounts(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Object userObj = req.getSession().getAttribute("user");
        if (!verifyAdmin(req, resp, userObj)) return;

        String adminId = ((UserDTO)userObj).getId();
        QueryCondition condition = BeanUtils.mapFromReq(req, QueryCondition.class);

        List<UserDetailDTO> taList = adminService.getAccountsByRole(1, condition, adminId);
        long taTotalPages = adminService.getAccountPages(1, condition, adminId);

        List<UserDetailDTO> moList = adminService.getAccountsByRole(2, condition, adminId);
        long moTotalPages = adminService.getAccountPages(2, condition, adminId);

        req.setAttribute("taList", taList);
        req.setAttribute("taCondition", condition);
        req.setAttribute("taTotalPages", taTotalPages);

        req.setAttribute("moList", moList);
        req.setAttribute("moCondition", condition);
        req.setAttribute("moTotalPages", moTotalPages);

        req.setAttribute("activeRole", 1);
        req.getRequestDispatcher("/views/admin/home.jsp").forward(req, resp);
    }

    /**
     * Loads a paginated page of accounts via AJAX (JSON response).
     * <p>
     * This method provides dynamic pagination without full page reload. Called when
     * user switches pages, changes filters, or modifies sort order.
     * </p>
     * <p>
     * <b>Process:</b>
     * <ol>
     *   <li>Verifies admin authentication</li>
     *   <li>Extracts role parameter (1=TA, 2=MO)</li>
     *   <li>Extracts query conditions from request</li>
     *   <li>Fetches accounts for specified role with pagination</li>
     *   <li>Returns JSON response with accounts, condition, and totalPages</li>
     * </ol>
     * </p>
     * <p>
     * <b>Response Format:</b>
     * <pre>{@code
     * {
     *   "success": true,
     *   "message": "success",
     *   "data": {
     *     "accounts": [...],
     *     "condition": {...},
     *     "totalPages": 5
     *   }
     * }
     * }</pre>
     * </p>
     * <p>
     * <b>Use Case:</b> Frontend JavaScript calls this endpoint when user:
     * <ul>
     *   <li>Clicks pagination buttons</li>
     *   <li>Selects different filter options</li>
     *   <li>Changes sort column/order</li>
     *   <li>Switches between TA/MO tabs</li>
     * </ul>
     * The returned JSON is used to update the account table dynamically.
     * </p>
     *
     * @param req  HttpServletRequest containing role and query parameters
     * @param resp HttpServletResponse for sending JSON response
     * @throws IOException if I/O error occurs
     * @see AdminService#getAccountsByRole(int, QueryCondition, String)
     */
    private void loadAccountsPage(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Object userObj = req.getSession().getAttribute("user");
        if (!verifyAdmin(req, resp, userObj)) return;

        String adminId = ((UserDTO)userObj).getId();

        String roleParam = req.getParameter("role");
        int role = Integer.parseInt(roleParam);

        QueryCondition condition = BeanUtils.mapFromReq(req, QueryCondition.class);

        List<UserDetailDTO> accounts = adminService.getAccountsByRole(role, condition, adminId);
        long totalPages = adminService.getAccountPages(role, condition, adminId);

        Map<String, Object> data = new HashMap<>();
        data.put("accounts", accounts);
        data.put("condition", condition);
        data.put("totalPages", totalPages);

        RespUtils.writeSuccess(resp, data);
    }

    /**
     * Deletes a user account and all associated data.
     * <p>
     * This method performs cascading deletion:
     * <ol>
     *   <li>Verifies admin authentication</li>
     *   <li>Validates userId parameter is provided</li>
     *   <li>Prevents admin from deleting their own account</li>
     *   <li>Calls {@link AdminService#deleteUser(String)} for cascading cleanup:
     *     <ul>
     *       <li>For TA: Deletes applications, decrements position stats, deletes profile, deletes user</li>
     *       <li>For MO: Deletes positions, deletes profile, deletes user</li>
     *     </ul>
     *   </li>
     *   <li>Returns success or error response</li>
     * </ol>
     * </p>
     * <p>
     * <b>Security:</b> Self-deletion prevention ensures admins cannot accidentally
     * lock themselves out of the system.
     * </p>
     * <p>
     * <b>Warning:</b> This operation is irreversible. All associated data including
     * applications, positions, and profiles will be permanently deleted.
     * </p>
     *
     * @param req  HttpServletRequest containing userId parameter
     * @param resp HttpServletResponse for sending JSON response
     * @throws IOException if I/O error occurs
     * @see AdminService#deleteUser(String)
     */
    private void deleteUser(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Object userObj = req.getSession().getAttribute("user");
        if (!verifyAdmin(req, resp, userObj)) return;

        String adminId = ((UserDTO)userObj).getId();
        String userId = req.getParameter("userId");
        
        if (userId == null || userId.trim().isEmpty()) {
            RespUtils.writeError(resp, "User ID is required", HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if (userId.equals(adminId)) {
            RespUtils.writeError(resp, "Cannot delete yourself", HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        boolean success = adminService.deleteUser(userId);
        if (success) {
            RespUtils.writeSuccess(resp, "User deleted successfully");
        } else {
            RespUtils.writeError(resp, "Failed to delete user", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Updates a user's account status (active/frozen).
     * <p>
     * This method allows admins to freeze or unfreeze user accounts:
     * <ol>
     *   <li>Verifies admin authentication</li>
     *   <li>Validates userId and status parameters</li>
     *   <li>Parses status parameter to integer</li>
     *   <li>Calls {@link AdminService#updateUserStatus(String, int)}</li>
     *   <li>Returns success or error response</li>
     * </ol>
     * </p>
     * <p>
     * <b>Status Values:</b>
     * <ul>
     *   <li>0 - Active: User can log in and use the system</li>
     *   <li>1 - Frozen: User cannot log in, existing sessions remain active until logout</li>
     * </ul>
     * </p>
     * <p>
     * <b>Use Case:</b> Freeze accounts for policy violations, suspicious activity,
     * or temporary suspension. Unfreeze after resolution.
     * </p>
     *
     * @param req  HttpServletRequest containing userId and status parameters
     * @param resp HttpServletResponse for sending JSON response
     * @throws IOException if I/O error occurs
     * @see AdminService#updateUserStatus(String, int)
     */
    private void updateStatus(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Object userObj = req.getSession().getAttribute("user");
        if (!verifyAdmin(req, resp, userObj)) return;

        String userId = req.getParameter("userId");
        String statusParam = req.getParameter("status");
        if (userId == null || statusParam == null) {
            RespUtils.writeError(resp, "User ID and status are required", HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        int status = Integer.parseInt(statusParam);
        boolean success = adminService.updateUserStatus(userId, status);
        if (success) {
            RespUtils.writeSuccess(resp, "User status updated successfully");
        } else {
            RespUtils.writeError(resp, "Failed to update user status", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Retrieves detailed TA profile information.
     * <p>
     * This AJAX endpoint fetches complete TA profile data for admin review:
     * <ol>
     *   <li>Verifies admin authentication</li>
     *   <li>Validates userId parameter</li>
     *   <li>Calls {@link AdminService#getTAProfile(String)}</li>
     *   <li>Returns TAProDTO with profile details or 404 if not found</li>
     * </ol>
     * </p>
     * <p>
     * <b>Response Data:</b> Includes all TA profile fields:
     * <ul>
     *   <li>Personal info: name, email, phone</li>
     *   <li>Academic info: major, GPA, year</li>
     *   <li>Skills: technical skills list</li>
     *   <li>Experience: work/project history</li>
     *   <li>Availability: max weekly workload, preferred positions</li>
     * </ul>
     * </p>
     *
     * @param req  HttpServletRequest containing userId parameter
     * @param resp HttpServletResponse for sending JSON response
     * @throws IOException if I/O error occurs
     * @see AdminService#getTAProfile(String)
     * @see TAProDTO
     */
    private void getTAProfile(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Object userObj = req.getSession().getAttribute("user");
        if (!verifyAdmin(req, resp, userObj)) return;

        String userId = req.getParameter("userId");
        if (userId == null || userId.trim().isEmpty()) {
            RespUtils.writeError(resp, "User ID is required", HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        TAProDTO profile = adminService.getTAProfile(userId);
        if (profile == null) {
            RespUtils.writeError(resp, "TA profile not found", HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        RespUtils.writeSuccess(resp, profile);
    }

    /**
     * Retrieves detailed MO profile information.
     * <p>
     * This AJAX endpoint fetches complete MO profile data for admin review:
     * <ol>
     *   <li>Verifies admin authentication</li>
     *   <li>Validates userId parameter</li>
     *   <li>Calls {@link AdminService#getMOProfile(String)}</li>
     *   <li>Returns MOProDTO with profile details or 404 if not found</li>
     * </ol>
     * </p>
     * <p>
     * <b>Response Data:</b> Includes all MO profile fields:
     * <ul>
     *   <li>Personal info: name, email, phone</li>
     *   <li>Department info: department, title</li>
     *   <li>Research interests</li>
     *   <li>Posted positions summary</li>
     * </ul>
     * </p>
     *
     * @param req  HttpServletRequest containing userId parameter
     * @param resp HttpServletResponse for sending JSON response
     * @throws IOException if I/O error occurs
     * @see AdminService#getMOProfile(String)
     * @see MOProDTO
     */
    private void getMOProfile(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Object userObj = req.getSession().getAttribute("user");
        if (!verifyAdmin(req, resp, userObj)) return;

        String userId = req.getParameter("userId");
        if (userId == null || userId.trim().isEmpty()) {
            RespUtils.writeError(resp, "User ID is required", HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        MOProDTO profile = adminService.getMOProfile(userId);
        if (profile == null) {
            RespUtils.writeError(resp, "MO profile not found", HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        RespUtils.writeSuccess(resp, profile);
    }

    /**
     * Updates MO profile information.
     * <p>
     * This method allows admins to modify MO profile data:
     * <ol>
     *   <li>Verifies admin authentication</li>
     *   <li>Maps request parameters to MOProfile object</li>
     *   <li>Validates profile ID is provided</li>
     *   <li>Calls {@link AdminService#updateMOProfile(MOProfile)}</li>
     *   <li>Returns success or error response</li>
     * </ol>
     * </p>
     * <p>
     * <b>Updatable Fields:</b> All MO profile fields except ID can be modified:
     * <ul>
     *   <li>Contact information</li>
     *   <li>Department and title</li>
     *   <li>Research interests</li>
     *   <li>Other profile metadata</li>
     * </ul>
     * </p>
     *
     * @param req  HttpServletRequest containing MOProfile data
     * @param resp HttpServletResponse for sending JSON response
     * @throws IOException if I/O error occurs
     * @see AdminService#updateMOProfile(MOProfile)
     */
    private void updateMOProfile(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Object userObj = req.getSession().getAttribute("user");
        if (!verifyAdmin(req, resp, userObj)) return;

        try {
            MOProfile profile = BeanUtils.mapFromReq(req, MOProfile.class);
            
            if (profile.getId() == null || profile.getId().trim().isEmpty()) {
                RespUtils.writeError(resp, "Profile ID is required", HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            boolean success = adminService.updateMOProfile(profile);
            if (success) {
                RespUtils.writeSuccess(resp, "MO profile updated successfully");
            } else {
                RespUtils.writeError(resp, "Failed to update MO profile", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } catch (Exception e) {
            log.error("Error updating MO profile", e);
            RespUtils.writeError(resp, "Error updating MO profile: " + e.getMessage(), HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    /**
     * Updates user account information (username and/or password).
     * <p>
     * This method allows admins to modify user credentials:
     * <ol>
     *   <li>Verifies admin authentication</li>
     *   <li>Validates user ID is provided</li>
     *   <li>Maps request parameters to User object</li>
     *   <li>If password is provided:
     *     <ul>
     *       <li>Validates minimum length (6 characters)</li>
     *       <li>Encrypts password with MD5</li>
     *     </ul>
     *   </li>
     *   <li>If password is empty/null: Password field not updated (preserves existing)</li>
     *   <li>Calls {@link AdminService#updateUser(User)}</li>
     *   <li>Returns success or error response</li>
     * </ol>
     * </p>
     * <p>
     * <b>Password Policy:</b>
     * <ul>
     *   <li>Minimum length: 6 characters</li>
     *   <li>Stored as MD5 hash (never plain text)</li>
     *   <li>Optional update: Only changed if explicitly provided</li>
     * </ul>
     * </p>
     * <p>
     * <b>Updatable Fields:</b>
     * <ul>
     *   <li>Name (username): Must be unique across all users</li>
     *   <li>Password: Optional, encrypted before storage</li>
     *   <li>Role/Status: Not modifiable through this endpoint (use dedicated methods)</li>
     * </ul>
     * </p>
     *
     * @param req  HttpServletRequest containing user data (id, name, optional password)
     * @param resp HttpServletResponse for sending JSON response
     * @throws IOException if I/O error occurs
     * @see AdminService#updateUser(User)
     * @see AdminService#encryptPassword(String)
     */
    private void updateUser(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Object userObj = req.getSession().getAttribute("user");
        if (!verifyAdmin(req, resp, userObj)) return;

        try {
            String userId = req.getParameter("id");
            if (userId == null || userId.trim().isEmpty()) {
                RespUtils.writeError(resp, "User ID is required", HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            
            User updatedUser = BeanUtils.mapFromReq(req, User.class);

            String password = updatedUser.getPassword();
            if (password != null && !password.trim().isEmpty()) {
                if (password.length() < 6) {
                    RespUtils.writeError(resp, "Password must be at least 6 characters", HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
                updatedUser.setPassword(adminService.encryptPassword(password));
            }

            boolean success = adminService.updateUser(updatedUser);
            if (success) {
                RespUtils.writeSuccess(resp, "User updated successfully");
            } else {
                RespUtils.writeError(resp, "Failed to update user", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } catch (Exception e) {
            log.error("Error updating user", e);
            RespUtils.writeError(resp, "Error updating user: " + e.getMessage(), HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    /**
     * Creates a new MO account with associated profile.
     * <p>
     * This method performs atomic creation of both User and MOProfile:
     * <ol>
     *   <li>Verifies admin authentication</li>
     *   <li>Maps request parameters to User object with custom field mapping:
     *     <ul>
     *       <li>"username" parameter → "name" field</li>
     *     </ul>
     *   </li>
     *   <li>Validates username is provided</li>
     *   <li>Validates password meets requirements (min 6 chars)</li>
     *   <li>Encrypts password with MD5</li>
     *   <li>Sets initial status to 0 (active)</li>
     *   <li>Maps request parameters to MOProfile object</li>
     *   <li>Calls {@link AdminService#createMOAccount(User, MOProfile)}</li>
     *   <li>Returns success or error response</li>
     * </ol>
     * </p>
     * <p>
     * <b>Atomic Operation:</b> Both User and MOProfile are created in a single transaction.
     * If either creation fails, both are rolled back to maintain data consistency.
     * </p>
     * <p>
     * <b>Default Values:</b>
     * <ul>
     *   <li>Role: Automatically set to 2 (MO)</li>
     *   <li>Status: Set to 0 (active)</li>
     *   <li>ID: Auto-generated UUID</li>
     *   <li>Timestamps: Auto-set to current time</li>
     * </ul>
     * </p>
     * <p>
     * <b>Validation:</b>
     * <ul>
     *   <li>Username: Required, must be unique</li>
     *   <li>Password: Required, minimum 6 characters</li>
     *   <li>Profile fields: Validated by AdminService</li>
     * </ul>
     * </p>
     *
     * @param req  HttpServletRequest containing user and profile data
     * @param resp HttpServletResponse for sending JSON response
     * @throws IOException if I/O error occurs
     * @see AdminService#createMOAccount(User, MOProfile)
     */
    private void createMOAccount(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Object userObj = req.getSession().getAttribute("user");
        if (!verifyAdmin(req, resp, userObj)) return;

        try {
            Map<String, String> userParamMapping = new HashMap<>();
            userParamMapping.put("username", "name");
            
            User mo = BeanUtils.mapFromReq(req, User.class, userParamMapping);
            
            if (mo.getName() == null || mo.getName().trim().isEmpty()) {
                RespUtils.writeError(resp, "Username is required", HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            String password = req.getParameter("password");
            if (password == null || password.length() < 6) {
                RespUtils.writeError(resp, "Password must be at least 6 characters", HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            
            mo.setPassword(adminService.encryptPassword(password));
            mo.setStatus(0);

            MOProfile moProfile = BeanUtils.mapFromReq(req, MOProfile.class);

            boolean success = adminService.createMOAccount(mo, moProfile);
            if (success) {
                RespUtils.writeSuccess(resp, "MO account created successfully");
            } else {
                RespUtils.writeError(resp, "Failed to create MO account", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } catch (Exception e) {
            log.error("Error creating MO account", e);
            RespUtils.writeError(resp, "Error creating MO account: " + e.getMessage(), HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    /**
     * Verifies that the current user has administrator privileges.
     * <p>
     * This security check is called at the beginning of every admin operation to ensure
     * only authorized administrators can access sensitive functions.
     * </p>
     * <p>
     * <b>Validation Steps:</b>
     * <ol>
     *   <li>Checks if user object exists in session (logged in)</li>
     *   <li>Validates user object is instance of UserDTO</li>
     *   <li>Verifies user role is 0 (admin)</li>
     * </ol>
     * </p>
     * <p>
     * <b>Failure Responses:</b>
     * <ul>
     *   <li>Not logged in: Redirects to login page (/views/user/login.jsp)</li>
     *   <li>Invalid session: Sends HTTP 401 Unauthorized error</li>
     *   <li>Not admin: Sends HTTP 403 Forbidden error</li>
     * </ul>
     * </p>
     * <p>
     * <b>Usage Pattern:</b>
     * <pre>{@code
     * private void someAdminOperation(HttpServletRequest req, HttpServletResponse resp) throws IOException {
     *     Object userObj = req.getSession().getAttribute("user");
     *     if (!verifyAdmin(req, resp, userObj)) return;
     *     
     *     // Proceed with admin operation...
     * }
     * }</pre>
     * </p>
     *
     * @param req     HttpServletRequest for potential redirect
     * @param resp    HttpServletResponse for sending error responses
     * @param userObj User object from session (may be null)
     * @return true if user is authenticated admin, false otherwise (response already sent)
     * @throws IOException if I/O error occurs during redirect or error response
     */
    private boolean verifyAdmin(HttpServletRequest req, HttpServletResponse resp, Object userObj) throws IOException {
        if (userObj == null) {
            log.warn("User not logged in, redirecting to login");
            resp.sendRedirect(req.getContextPath() + "/views/user/login.jsp");
            return false;
        }

        if (!(userObj instanceof UserDTO userDTO)) {
            log.error("Invalid user object type in session");
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid user session");
            return false;
        }

        if (userDTO.getRole() != 0) {
            log.warn("User {} does not have Admin role, role: {}", userDTO.getId(), userDTO.getRole());
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
            return false;
        }

        return true;
    }
}
