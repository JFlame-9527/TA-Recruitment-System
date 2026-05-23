package com.tars.controller;

import com.tars.entity.bean.Application;
import com.tars.entity.bean.TAProfile;
import com.tars.entity.QueryCondition;
import com.tars.entity.dto.ta.AppPosDTO;
import com.tars.entity.dto.ta.PosBriefDTO;
import com.tars.entity.dto.ta.PosDetailDTO;
import com.tars.entity.dto.ta.ProfileDTO;
import com.tars.entity.dto.user.UserDTO;
import com.tars.service.TAService;
import com.tars.util.BeanUtils;
import com.tars.util.FileUtils;
import com.tars.util.RespUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servlet for Technical Assistant (TA) candidate operations in the recruitment system.
 * <p>
 * This servlet provides comprehensive TA functionality:
 * <ul>
 *   <li><b>Application Management</b>: View applied positions, withdraw applications</li>
 *   <li><b>Position Browsing</b>: Browse available positions with filtering and AI-based recommendations</li>
 *   <li><b>Position Application</b>: Apply for positions with profile verification</li>
 *   <li><b>Profile Management</b>: Create, view, update TA profiles with resume upload</li>
 *   <li><b>Resume Operations</b>: Download resumes, extract skills using AI</li>
 * </ul>
 * </p>
 * <p>
 * <b>Access Control:</b> All operations require TA authentication (role=1).
 * The {@link #verifyUser(HttpServletRequest, HttpServletResponse, Object)} method validates:
 * <ol>
 *   <li>User is logged in (session contains "user" attribute)</li>
 *   <li>User object is valid UserDTO instance</li>
 *   <li>User has TA role (role=1)</li>
 * </ol>
 * Failed validation results in redirect to login page or HTTP 401/403 error.
 * </p>
 * <p>
 * <b>File Upload Configuration:</b> Supports PDF resume uploads up to 10MB per file,
 * with total request size limit of 50MB. Files are validated for:
 * <ul>
 *   <li>Extension must be .pdf</li>
 *   <li>Content type must be application/pdf</li>
 *   <li>Path traversal prevention</li>
 *   <li>UUID-based filename generation</li>
 * </ul>
 * </p>
 * <p>
 * <b>Request Mapping:</b> All operations are routed through {@link BaseServlet} using
 * the {@code action} parameter:
 * <pre>
 * GET  /taServlet?action=listApplied        → listApplied() [Full page]
 * POST /taServlet?action=withdraw           → withdraw() [AJAX]
 * GET  /taServlet?action=listPositions      → listPositions() [Full page]
 * GET  /taServlet?action=viewPosition       → viewPosition() [Full page]
 * POST /taServlet?action=apply              → apply() [AJAX]
 * GET  /taServlet?action=getProfile         → getProfile() [Full page]
 * POST /taServlet?action=updateProfile      → updateProfile() [Form submit]
 * POST /taServlet?action=createProfile      → createProfile() [Form submit]
 * GET  /taServlet?action=downloadResume     → downloadResume() [File download]
 * POST /taServlet?action=extractSkills      → extractSkills() [AJAX]
 * </pre>
 * </p>
 * <p>
 * <b>AI Integration:</b> Profile creation and updates trigger AI-powered skill extraction
 * from uploaded resumes via {@link TAService#extractSkills(Part)}. Extracted skills are
 * used to generate vector portraits for position matching.
 * </p>
 *
 * @author QiheSun Xiri04
 * @version 2.0.0
 * @since 2026/3/26
 * @see TAService
 * @see BaseServlet
 * @see FileUtils
 */
@Slf4j
@WebServlet(name = "TAServlet", value = "/taServlet")
@MultipartConfig(
        fileSizeThreshold = 0, // 0MB - files smaller than this are kept in memory
        maxFileSize = 1024 * 1024 * 10,      // 10MB - maximum file size allowed
        maxRequestSize = 1024 * 1024 * 50    // 50MB - maximum request size (files + form data)
)
public class TAServlet extends BaseServlet {

    private TAService taService;

    /**
     * Initializes the servlet and creates TAService instance.
     *
     * @throws ServletException if initialization fails
     */
    @Override
    public void init() throws ServletException {
        super.init();
        taService = new TAService();
    }

    /**
     * Lists TA's applied positions with full page rendering.
     * <p>
     * This method displays all positions the TA has applied for, with filtering and pagination:
     * <ol>
     *   <li>Verifies TA authentication</li>
     *   <li>Extracts query conditions from request (filter, order, page)</li>
     *   <li>Calls {@link TAService#getAppPosList(String, QueryCondition)}</li>
     *   <li>Calculates total pages for pagination</li>
     *   <li>Sets request attributes for JSP rendering</li>
     *   <li>Forwards to /views/ta/home.jsp</li>
     * </ol>
     * </p>
     * <p>
     * <b>Filter Options:</b>
     * <ul>
     *   <li>"all" - Show all applications</li>
     *   <li>"opened" - Applications awaiting review (status=0)</li>
     *   <li>"offered" - Applications with offers (status=1)</li>
     *   <li>"rejected" - Declined applications (status=2)</li>
     * </ul>
     * </p>
     * <p>
     * <b>Sort Options:</b>
     * <ul>
     *   <li>"applyAt" - By application date</li>
     *   <li>"postDate" - By position posting date</li>
     *   <li>"deadline" - By application deadline</li>
     * </ul>
     * </p>
     * <p>
     * <b>Response Data:</b> Each AppPosDTO includes:
     * <ul>
     *   <li>Application info: id, status, applyAt, feedback</li>
     *   <li>Position info: title, postUserId, requiredNum, offeredNum</li>
     * </ul>
     * </p>
     *
     * @param req  HttpServletRequest containing query parameters
     * @param resp HttpServletResponse for forwarding to JSP
     * @throws IOException      if I/O error occurs
     * @throws ServletException if servlet error occurs
     * @see TAService#getAppPosList(String, QueryCondition)
     * @see TAService#getAppPosPages(String, QueryCondition)
     */
    private void listApplied(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        QueryCondition condition = BeanUtils.mapFromReq(req, QueryCondition.class);

        Object userObj = req.getSession().getAttribute("user");
        if (!verifyUser(req, resp, userObj)) return;

        String userId = ((UserDTO) userObj).getId();
        log.info("TA user {} requesting applied positions list, page: {}", userId, condition.getPage());

        List<AppPosDTO> appPosList = taService.getAppPosList(userId, condition);
        long totalPages = taService.getAppPosPages(userId, condition);
        req.setAttribute("appliedList", appPosList);
        req.setAttribute("condition", condition);
        req.setAttribute("totalPages", totalPages);
        req.getRequestDispatcher("/views/ta/home.jsp").forward(req, resp);
    }

    /**
     * Withdraws a TA's application for a position.
     * <p>
     * This AJAX endpoint allows TAs to cancel their applications:
     * <ol>
     *   <li>Validates appId parameter is provided</li>
     *   <li>Verifies TA authentication</li>
     *   <li>Calls {@link TAService#withdrawApplication(String, String)}</li>
     *   <li>Returns success response with appId</li>
     * </ol>
     * </p>
     * <p>
     * <b>Withdrawal Effects:</b>
     * <ul>
     *   <li>Sets application status to 3 (withdrawn)</li>
     *   <li>Decrements position's appliedNum counter</li>
     *   <li>If previously offered: also decrements offeredNum</li>
     *   <li>If previously rejected: also decrements rejectedNum</li>
     * </ul>
     * </p>
     * <p>
     * <b>Re-application:</b> After withdrawal, TAs can re-apply for the same position.
     * The system will restore the withdrawn application instead of creating a duplicate.
     * </p>
     * <p>
     * <b>Response Format:</b>
     * <pre>{@code
     * {
     *   "success": true,
     *   "message": "Application withdrawn successfully",
     *   "data": {
     *     "appId": "uuid-string"
     *   }
     * }
     * }</pre>
     * </p>
     *
     * @param req  HttpServletRequest containing appId parameter
     * @param resp HttpServletResponse for sending JSON response
     * @throws IOException      if I/O error occurs
     * @throws ServletException if servlet error occurs
     * @see TAService#withdrawApplication(String, String)
     */
    private void withdraw(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String appId = req.getParameter("appId");
        if (appId == null || appId.trim().isEmpty()) {
            log.warn("Invalid appId: {}", appId);
            RespUtils.writeError(resp, "Invalid appId", HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        Object userObj = req.getSession().getAttribute("user");
        if (!verifyUser(req, resp, userObj)) return;

        String userId = ((UserDTO) userObj).getId();

        taService.withdrawApplication(appId, userId);

        Map<String, String> data = new HashMap<>();
        data.put("appId", appId);
        RespUtils.writeSuccess(resp, data, "Application withdrawn successfully");
    }

    /**
     * Lists available positions with full page rendering.
     * <p>
     * This method displays all open positions that TAs can apply for, with advanced filtering and AI recommendations:
     * <ol>
     *   <li>Verifies TA authentication</li>
     *   <li>Extracts query conditions from request</li>
     *   <li>Calls {@link TAService#getPositionList(String, QueryCondition)}</li>
     *   <li>Calculates total pages for pagination</li>
     *   <li>Sets request attributes for JSP rendering</li>
     *   <li>Forwards to /views/ta/positions.jsp</li>
     * </ol>
     * </p>
     * <p>
     * <b>Filter Options:</b>
     * <ul>
     *   <li>"all" - Show all opened positions</li>
     *   <li>"unapplied" - Exclude positions already applied by this TA</li>
     *   <li>"applied" - Show only positions already applied</li>
     * </ul>
     * </p>
     * <p>
     * <b>Search Keys:</b> Searches in position title, description, and requirements
     * </p>
     * <p>
     * <b>Sort Options:</b>
     * <ul>
     *   <li>"postDate" - By posting date (newest first)</li>
     *   <li>"deadline" - By deadline (earliest first)</li>
     *   <li>"vacancy" - By number of vacancies (most first)</li>
     *   <li>"workload" - By weekly workload (lowest first)</li>
     *   <li>"recommend" - AI-based recommendation sorted by match score</li>
     * </ul>
     * </p>
     * <p>
     * <b>AI Recommendation:</b> When sorting by "recommend", positions are ranked by
     * cosine similarity between TA portrait and position portrait vectors. Higher scores
     * indicate better skill/experience match.
     * </p>
     *
     * @param req  HttpServletRequest containing query parameters
     * @param resp HttpServletResponse for forwarding to JSP
     * @throws IOException      if I/O error occurs
     * @throws ServletException if servlet error occurs
     * @see TAService#getPositionList(String, QueryCondition)
     * @see TAService#getPositionPages(QueryCondition)
     */
    private void listPositions(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        QueryCondition condition = BeanUtils.mapFromReq(req, QueryCondition.class);

        Object userObj = req.getSession().getAttribute("user");
        if (!verifyUser(req, resp, userObj)) return;


        String userId = ((UserDTO) userObj).getId();
        log.info("TA user {} requesting positions list, page: {}", userId, condition.getPage());

        List<PosBriefDTO> positionList = taService.getPositionList(userId, condition);
        long totalPages = taService.getPositionPages(condition);
        req.setAttribute("positionList", positionList);
        req.setAttribute("condition", condition);
        req.setAttribute("totalPages", totalPages);
        req.getRequestDispatcher("/views/ta/positions.jsp").forward(req, resp);
    }

    /**
     * Verifies that the current user has TA privileges.
     * <p>
     * This security check is called at the beginning of every TA operation to ensure
     * only authenticated TAs can access their functions.
     * </p>
     * <p>
     * <b>Validation Steps:</b>
     * <ol>
     *   <li>Checks if user object exists in session (logged in)</li>
     *   <li>Validates user object is instance of UserDTO</li>
     *   <li>Verifies user role is 1 (TA)</li>
     * </ol>
     * </p>
     * <p>
     * <b>Failure Responses:</b>
     * <ul>
     *   <li>Not logged in: Redirects to login page (/views/user/login.jsp)</li>
     *   <li>Invalid session: Sends HTTP 401 Unauthorized error</li>
     *   <li>Not TA: Sends HTTP 403 Forbidden error</li>
     * </ul>
     * </p>
     *
     * @param req     HttpServletRequest for potential redirect
     * @param resp    HttpServletResponse for sending error responses
     * @param userObj User object from session (may be null)
     * @return true if user is authenticated TA, false otherwise (response already sent)
     * @throws IOException if I/O error occurs during redirect or error response
     */
    private boolean verifyUser(HttpServletRequest req, HttpServletResponse resp, Object userObj) throws IOException {
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

        if (userDTO.getRole() != 1) {
            log.warn("User {} does not have TA role, role: {}", userDTO.getId(), userDTO.getRole());
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
            return false;
        }

        return true;
    }

    /**
     * Displays detailed information for a specific position.
     * <p>
     * This method shows complete position details including application history:
     * <ol>
     *   <li>Extracts posId, appId, condition, and from parameters</li>
     *   <li>Calls {@link TAService#getPosition(String, String)} to get position details</li>
     *   <li>Sets request attributes for JSP rendering</li>
     *   <li>Forwards to /views/ta/positionDetail.jsp</li>
     * </ol>
     * </p>
     * <p>
     * <b>Parameters:</b>
     * <ul>
     *   <li>posId: Position ID to display (required)</li>
     *   <li>appId: Application ID if TA has applied (optional, for showing application status)</li>
     *   <li>from: Navigation source ("positions" or "applied"), default "positions"</li>
     *   <li>condition: Query conditions for returning to list with same filters</li>
     * </ul>
     * </p>
     * <p>
     * <b>Display Content:</b> PosDetailDTO includes:
     * <ul>
     *   <li>Position basics: title, description, requirements</li>
     *   <li>Dates: postDate, startDate, endDate, deadline</li>
     *   <li>Statistics: requiredNum, offeredNum, appliedNum, rejectedNum, vacancyNum</li>
     *   <li>Workload: weeklyWorkload</li>
     *   <li>Application status: hasApplied, applicationId, applicationStatus (if applicable)</li>
     * </ul>
     * </p>
     *
     * @param req  HttpServletRequest containing posId and optional appId
     * @param resp HttpServletResponse for forwarding to JSP
     * @throws IOException      if I/O error occurs
     * @throws ServletException if servlet error occurs
     * @see TAService#getPosition(String, String)
     */
    private void viewPosition(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String posId = req.getParameter("posId");
        String appId = req.getParameter("appId");
        QueryCondition condition = BeanUtils.mapFromReq(req, QueryCondition.class);
        String from = req.getParameter("from");

        PosDetailDTO pos = taService.getPosition(posId, appId);
        req.setAttribute("position", pos);
        req.setAttribute("condition", condition);
        req.setAttribute("from", from != null ? from : "positions");
        req.getRequestDispatcher("/views/ta/positionDetail.jsp").forward(req, resp);
    }

    /**
     * Submits an application for a position.
     * <p>
     * This AJAX endpoint handles TA position applications with validation:
     * <ol>
     *   <li>Verifies TA authentication</li>
     *   <li>Validates TA has completed profile ({@link TAService#verifyProfileExists(String)})</li>
     *   <li>Validates position is available ({@link TAService#verifyPosAvailable(String, String)}):
     *     <ul>
     *       <li>Position status is opened (0)</li>
     *       <li>TA hasn't already applied (or has withdrawn previous application)</li>
     *     </ul>
     *   </li>
     *   <li>Creates Application object with status=0 (applied)</li>
     *   <li>Calls {@link TAService#apply(Application)} which handles:
     *     <ul>
     *       <li>New applications: Creates new record</li>
     *       <li>Withdrawn applications: Restores existing record</li>
     *     </ul>
     *   </li>
     *   <li>Returns success with appId or error response</li>
     * </ol>
     * </p>
     * <p>
     * <b>Profile Requirement:</b> TAs must complete their profile before applying.
     * This ensures MOs have necessary information to evaluate candidates.
     * </p>
     * <p>
     * <b>Smart Re-application:</b> If TA previously withdrew an application for this position,
     * the system restores the withdrawn application instead of creating a duplicate.
     * This preserves application history while allowing re-application.
     * </p>
     * <p>
     * <b>Response Format (Success):</b>
     * <pre>{@code
     * {
     *   "success": true,
     *   "message": "Application submitted successfully",
     *   "data": {
     *     "appId": "uuid-string"
     *   }
     * }
     * }</pre>
     * </p>
     *
     * @param req  HttpServletRequest containing posId parameter
     * @param resp HttpServletResponse for sending JSON response
     * @throws IOException      if I/O error occurs
     * @throws ServletException if servlet error occurs
     * @see TAService#apply(Application)
     * @see TAService#verifyProfileExists(String)
     * @see TAService#verifyPosAvailable(String, String)
     */
    private void apply(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String posId = req.getParameter("posId");

        Object userObj = req.getSession().getAttribute("user");
        if (!verifyUser(req, resp, userObj)) return;

        String userId = ((UserDTO) userObj).getId();

        if (!taService.verifyProfileExists(userId)) {
            log.warn("User {} has not completed profile, cannot apply", userId);
            RespUtils.writeError(resp, "Please complete your profile before applying", HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if (!taService.verifyPosAvailable(posId, userId)) {
            log.warn("Position {} is not available for user {}", posId, userId);
            RespUtils.writeError(resp, "Position is not available", HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        Application application = new Application();
        application.setPositionId(posId);
        application.setUserId(userId);
        application.setStatus(0);
        
        // Use reapplyAfterWithdraw to handle withdrawn applications
        boolean applied = taService.apply(application);

        if (applied) {
            Map<String, String> data = new HashMap<>();
            data.put("appId", application.getId());
            RespUtils.writeSuccess(resp, data, "Application submitted successfully");
        } else {
            RespUtils.writeError(resp, "Failed to apply for position",
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Retrieves TA profile information for viewing/editing.
     * <p>
     * This method loads TA profile data and forwards to profile page:
     * <ol>
     *   <li>Verifies TA authentication</li>
     *   <li>Calls {@link TAService#getProfileDTO(String)}</li>
     *   <li>If profile not found: Sets warning message and forwards to profile.jsp</li>
     *   <li>If profile exists:
     *     <ul>
     *       <li>Normalizes resume path (backslashes to forward slashes)</li>
     *       <li>Sets safeResumePath attribute for URL safety</li>
     *       <li>Sets profile attribute</li>
     *       <li>Forwards to /views/ta/profile.jsp</li>
     *     </ul>
     *   </li>
     * </ol>
     * </p>
     * <p>
     * <b>Path Normalization:</b> Resume paths stored in database may contain backslashes
     * (Windows format). These are converted to forward slashes for safe URL usage in JSP.
     * </p>
     * <p>
     * <b>Profile Not Found:</b> If TA hasn't created a profile yet, displays a warning
     * message prompting them to create one. The profile.jsp page will show a creation form.
     * </p>
     *
     * @param req  HttpServletRequest for session access
     * @param resp HttpServletResponse for forwarding to JSP
     * @throws IOException      if I/O error occurs
     * @throws ServletException if servlet error occurs
     * @see TAService#getProfileDTO(String)
     */
    private void getProfile(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        Object userObj = req.getSession().getAttribute("user");
        if (!verifyUser(req, resp, userObj)) return;

        String userId = ((UserDTO) userObj).getId();
        ProfileDTO profile = taService.getProfileDTO(userId);

        if (profile == null) {
            log.warn("Profile not found for user {}", userId);
            req.setAttribute("warn", "Profile not found. Please create your profile first.");
            req.getRequestDispatcher("/views/ta/profile.jsp").forward(req, resp);
            return;
        }

        if (profile.getResumePath() != null) {
            String normalizedPath = profile.getResumePath().replace("\\", "/");
            req.setAttribute("safeResumePath", normalizedPath);
            log.debug("Normalized resume path: {}", normalizedPath);
        }

        req.setAttribute("profile", profile);
        req.getRequestDispatcher("/views/ta/profile.jsp").forward(req, resp);
    }

    /**
     * Serves resume file for download or inline viewing.
     * <p>
     * This method securely serves PDF resume files with path validation:
     * <ol>
     *   <li>Validates file parameter is provided</li>
     *   <li>Sanitizes path using {@link FileUtils#sanitizePath(String)} to prevent path traversal</li>
     *   <li>Calls {@link FileUtils#serveFile(HttpServletRequest, HttpServletResponse, String)}</li>
     *   <li>File is served with appropriate headers:
     *     <ul>
     *       <li>Content-Type: application/pdf</li>
     *       <li>Content-Disposition: inline or attachment (based on request parameter)</li>
     *       <li>X-Content-Type-Options: nosniff (MIME sniffing protection)</li>
     *     </ul>
     *   </li>
     * </ol>
     * </p>
     * <p>
     * <b>Security:</b> Path sanitization prevents directory traversal attacks by:
     * <ul>
     *   <li>Rejecting paths containing ".."</li>
     *   <li>Normalizing multiple slashes to single slash</li>
     *   <li>Ensuring resolved path stays within uploads directory</li>
     * </ul>
     * </p>
     * <p>
     * <b>Usage Modes:</b>
     * <ul>
     *   <li>Inline viewing: {@code downloadResume?file=path/to/resume.pdf}</li>
     *   <li>Force download: {@code downloadResume?file=path/to/resume.pdf&download=true}</li>
     * </ul>
     * </p>
     *
     * @param req  HttpServletRequest containing file parameter
     * @param resp HttpServletResponse for serving file
     * @throws IOException if I/O error occurs during file serving
     * @see FileUtils#sanitizePath(String)
     * @see FileUtils#serveFile(HttpServletRequest, HttpServletResponse, String)
     */
    private void downloadResume(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String fileName = req.getParameter("file");

        if (fileName == null || fileName.trim().isEmpty()) {
            log.warn("Empty file parameter in downloadResume");
            RespUtils.writeError(resp, "Invalid file path", HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String sanitizedPath = FileUtils.sanitizePath(fileName);
        if (sanitizedPath == null) {
            log.warn("Invalid file path requested: {}", fileName);
            RespUtils.writeError(resp, "Invalid file path", HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        log.info("Serving file: {} (download={})", sanitizedPath, req.getParameter("download"));

        try {
            FileUtils.serveFile(req, resp, sanitizedPath);
        } catch (IOException e) {
            log.error("Error serving file: {}", sanitizedPath, e);
            throw e;
        }
    }

    /**
     * Updates TA profile information with optional resume upload.
     * <p>
     * This complex method handles profile updates with file management:
     * <ol>
     *   <li>Verifies TA authentication</li>
     *   <li>Retrieves existing profile from database</li>
     *   <li>Maps request parameters to updatedProfile object</li>
     *   <li>Handles resume file:
     *     <ul>
     *       <li>If new file uploaded:
     *         <ul>
     *           <li>Saves new PDF file via {@link FileUtils#savePdfFile(Part, String)}</li>
     *           <li>Deletes old resume file if exists</li>
     *           <li>Sets new resumePath and resumeName</li>
     *           <li>Marks resumeUpdated flag</li>
     *         </ul>
     *       </li>
     *       <li>If no new file:
     *         <ul>
     *           <li>Preserves existing resumePath and resumeName</li>
     *           <li>Creates File object from existing path for AI processing</li>
     *         </ul>
     *       </li>
     *     </ul>
     *   </li>
     *   <li>Merges updatedProfile into existingProfile (ignoring id, userId, timestamps)</li>
     *   <li>Updates timestamp to current time</li>
     *   <li>Validates required fields (name)</li>
     *   <li>Calls {@link TAService#updateProfile(TAProfile, Part)} or
     *       {@link TAService#updateProfile(TAProfile, File)} based on resume status</li>
     *   <li>On success: Redirects to getProfile with success parameter</li>
     *   <li>On failure: Forwards to profile.jsp with error message</li>
     * </ol>
     * </p>
     * <p>
     * <b>File Management:</b>
     * <ul>
     *   <li>New uploads replace old files to avoid storage waste</li>
     *   <li>Old file deletion failures are logged but don't abort the update</li>
     *   <li>Existing files are wrapped as File objects for AI skill extraction</li>
     * </ul>
     * </p>
     * <p>
     * <b>AI Integration:</b> Whether uploading new resume or keeping existing one,
     * the service extracts skills and regenerates portrait vector for improved matching.
     * </p>
     * <p>
     * <b>Error Handling:</b> Catches three exception types:
     * <ul>
     *   <li>SecurityException: Path traversal or invalid file attempts</li>
     *   <li>IllegalArgumentException: Invalid data format</li>
     *   <li>Exception: General errors</li>
     * </ul>
     * All errors forward to profile.jsp with descriptive messages.
     * </p>
     *
     * @param req  HttpServletRequest containing profile data and optional resume file
     * @param resp HttpServletResponse for redirect or forward
     * @throws IOException      if I/O error occurs
     * @throws ServletException if servlet error occurs
     * @see TAService#updateProfile(TAProfile, Part)
     * @see TAService#updateProfile(TAProfile, File)
     * @see FileUtils#savePdfFile(Part, String)
     * @see FileUtils#deleteFile(String)
     */
    private void updateProfile(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        Object userObj = req.getSession().getAttribute("user");
        if (!verifyUser(req, resp, userObj)) return;

        String userId = ((UserDTO) userObj).getId();
        log.info("Updating profile for TA user {}", userId);

        try {
            // Step 1: Get existing profile from database
            TAProfile existingProfile = taService.getProfile(userId);
            if (existingProfile == null) {
                log.warn("Profile not found for user {}", userId);
                req.setAttribute("error", "Profile not found. Please create your profile first.");
                req.getRequestDispatcher("/views/ta/profile.jsp").forward(req, resp);
                return;
            }

            // Step 2: Map request parameters to new profile object
            TAProfile updatedProfile = BeanUtils.mapFromReq(req, TAProfile.class);

            // Step 3: Handle file upload (if any)
            Part resumePart = FileUtils.getFilePart(req, "resume");
            String webRootPath = getServletContext().getRealPath("");


            boolean resumeUpdated = false;
            File resumeFile = null;
            
            if (resumePart != null && resumePart.getSize() > 0) {
                // User uploaded a new resume
                log.info("New resume detected, processing upload...");

                // Save new file
                String newPath = FileUtils.savePdfFile(resumePart, "resumes");
                String originalFileName = Paths.get(resumePart.getSubmittedFileName()).getFileName().toString();

                // Set new file info
                updatedProfile.setResumePath(newPath);
                updatedProfile.setResumeName(originalFileName);

                // Delete old file if exists
                if (existingProfile.getResumePath() != null) {
                    boolean deleted = FileUtils.deleteFile(existingProfile.getResumePath());
                    if (deleted) {
                        log.info("Old resume deleted: {}", existingProfile.getResumePath());
                    } else {
                        log.warn("Failed to delete old resume: {}", existingProfile.getResumePath());
                    }
                }
                resumeUpdated = true;
            } else {
                // No new file, keep existing resume info and create File object
                updatedProfile.setResumePath(existingProfile.getResumePath());
                updatedProfile.setResumeName(existingProfile.getResumeName());
                
                // Create File object from existing resume path if it exists
                if (existingProfile.getResumePath() != null) {
                    resumeFile = FileUtils.getFileFromRelativePath(existingProfile.getResumePath());
                    if (resumeFile != null) {
                        log.info("Created File object from existing resume: {}", existingProfile.getResumePath());
                    } else {
                        log.warn("Resume file does not exist for path: {}", existingProfile.getResumePath());
                    }
                } else {
                    log.info("No resume file exists");
                }
            }

            // Step 4: Merge updated fields into existing profile
            // Ignore system fields that shouldn't be updated via form
            BeanUtils.merge(updatedProfile, existingProfile,
                    "id", "userId", "createAt", "updateAt");

            // Step 5: Update timestamp
            existingProfile.setUpdateAt(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));

            // Step 6: Validate required fields
            if (existingProfile.getName() == null || existingProfile.getName().trim().isEmpty()) {
                req.setAttribute("error", "Name is required");
                req.getRequestDispatcher("/views/ta/profile.jsp").forward(req, resp);
                return;
            }

            // Step 7: Save to database
            boolean success = false;
            if (resumeUpdated) {
                success = taService.updateProfile(existingProfile, resumePart);
            } else if (resumeFile != null) {
                success = taService.updateProfile(existingProfile, resumeFile);
            }

            if (success) {
                log.info("Profile updated successfully for user {}", userId);
                resp.sendRedirect(req.getContextPath() + "/taServlet?action=getProfile&success=updated");
            } else {
                log.error("Failed to update profile for user {}", userId);
                req.setAttribute("error", "Failed to update profile");
                req.getRequestDispatcher("/views/ta/profile.jsp").forward(req, resp);
            }

        } catch (SecurityException e) {
            log.error("Security violation during profile update: {}", e.getMessage(), e);
            req.setAttribute("error", "Security error: " + e.getMessage());
            req.getRequestDispatcher("/views/ta/profile.jsp").forward(req, resp);
        } catch (IllegalArgumentException e) {
            log.error("Invalid parameter format: {}", e.getMessage(), e);
            req.setAttribute("error", "Invalid data format: " + e.getMessage());
            req.getRequestDispatcher("/views/ta/profile.jsp").forward(req, resp);
        } catch (Exception e) {
            log.error("Error updating profile for user {}", userId, e);
            req.setAttribute("error", "Error updating profile: " + e.getMessage());
            req.getRequestDispatcher("/views/ta/profile.jsp").forward(req, resp);
        }
    }

    /**
     * Creates a new TA profile with required resume upload.
     * <p>
     * This method handles initial profile creation:
     * <ol>
     *   <li>Verifies TA authentication</li>
     *   <li>Checks if profile already exists (prevents duplicates)</li>
     *   <li>Maps request parameters to TAProfile object</li>
     *   <li>Sets userId from session</li>
     *   <li>Validates resume file is provided (required for creation)</li>
     *   <li>Saves PDF file via {@link FileUtils#savePdfFile(Part, String)}</li>
     *   <li>Extracts original filename and sets resumeName</li>
     *   <li>Validates required fields (name)</li>
     *   <li>Calls {@link TAService#createProfile(TAProfile, Part)} which:
     *     <ul>
     *       <li>Extracts skills from resume using AI</li>
     *       <li>Generates portrait vector from profile data</li>
     *       <li>Saves profile and portrait to repositories</li>
     *     </ul>
     *   </li>
     *   <li>On success: Redirects to getProfile with success=created</li>
     *   <li>On failure: Forwards to profile.jsp with error message</li>
     * </ol>
     * </p>
     * <p>
     * <b>Resume Requirement:</b> Unlike updates, profile creation REQUIRES a resume file.
     * This ensures all TAs have at least basic qualification information for MO review.
     * </p>
     * <p>
     * <b>AI Processing:</b> During creation, the system:
     * <ul>
     *   <li>Extracts technical skills from resume PDF using Qwen AI</li>
     *   <li>Generates 3-dimensional portrait vector (skills, experience, education)</li>
     *   <li>Stores portrait for future position matching</li>
     * </ul>
     * </p>
     * <p>
     * <b>Duplicate Prevention:</b> If profile already exists, returns HTTP 400 error.
     * Users should use updateProfile instead.
     * </p>
     *
     * @param req  HttpServletRequest containing profile data and resume file
     * @param resp HttpServletResponse for redirect or forward
     * @throws IOException      if I/O error occurs
     * @throws ServletException if servlet error occurs
     * @see TAService#createProfile(TAProfile, Part)
     * @see TAService#checkProfileExist(String)
     * @see FileUtils#savePdfFile(Part, String)
     */
    private void createProfile(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        Object userObj = req.getSession().getAttribute("user");
        if (!verifyUser(req, resp, userObj)) return;

        String userId = ((UserDTO) userObj).getId();

        boolean exist = taService.checkProfileExist(userId);
        if (exist) {
            log.warn("TA profile already exist, userId: {}", userId);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "TA profile already exist");
            return;
        }

        log.info("Creating profile for TA user {}", userId);

        try {
            TAProfile profile = BeanUtils.mapFromReq(req, TAProfile.class);

            profile.setUserId(userId);

            Part resumePart = FileUtils.getFilePart(req, "resume");

            if (resumePart == null || resumePart.getSize() == 0) {
                req.setAttribute("error", "Resume is required when creating profile");
                req.getRequestDispatcher("/views/ta/profile.jsp").forward(req, resp);
                return;
            }

            String originalFileName = Paths.get(resumePart.getSubmittedFileName()).getFileName().toString();
            profile.setResumeName(originalFileName);

            String resumePath = FileUtils.savePdfFile(resumePart, "resumes");
            profile.setResumePath(resumePath);

            log.info("Resume uploaded successfully - Original name: {}, Saved path: {}",
                    originalFileName, resumePath);

            if (profile.getName() == null || profile.getName().trim().isEmpty()) {
                req.setAttribute("error", "Name is required");
                req.getRequestDispatcher("/views/ta/profile.jsp").forward(req, resp);
                return;
            }

            boolean created = taService.createProfile(profile, resumePart);

            if (created) {
                log.info("Profile created successfully for user {}", userId);
                resp.sendRedirect(req.getContextPath() + "/taServlet?action=getProfile&success=created");
            } else {
                log.error("Failed to create profile for user {}", userId);
                req.setAttribute("error", "Failed to create profile");
                req.getRequestDispatcher("/views/ta/profile.jsp").forward(req, resp);
            }

        } catch (IllegalArgumentException e) {
            log.error("Invalid parameter format: {}", e.getMessage(), e);
            req.setAttribute("error", "Invalid data format: " + e.getMessage());
            req.getRequestDispatcher("/views/ta/profile.jsp").forward(req, resp);
        } catch (Exception e) {
            log.error("Error creating profile for user {}", userId, e);
            req.setAttribute("error", "Error creating profile: " + e.getMessage());
            req.getRequestDispatcher("/views/ta/profile.jsp").forward(req, resp);
        }
    }

    /**
     * Extracts technical skills from uploaded resume using AI.
     * <p>
     * This AJAX endpoint provides real-time skill extraction for profile forms:
     * <ol>
     *   <li>Verifies TA authentication</li>
     *   <li>Extracts resume file from request</li>
     *   <li>Calls {@link TAService#extractSkills(Part)}</li>
     *   <li>Returns extracted skills list as JSON</li>
     * </ol>
     * </p>
     * <p>
     * <b>AI Processing:</b> Uses Qwen AI to analyze resume PDF and extract:
     * <ul>
     *   <li>Programming languages (Java, Python, C++, etc.)</li>
     *   <li>Frameworks and libraries (Spring, React, TensorFlow, etc.)</li>
     *   <li>Tools and technologies (Docker, Git, AWS, etc.)</li>
     *   <li>Domain-specific skills (Machine Learning, Database Design, etc.)</li>
     * </ul>
     * </p>
     * <p>
     * <b>Use Case:</b> Frontend calls this endpoint when user uploads a resume during
     * profile creation/update. Returned skills are auto-populated into the skills field,
     * allowing users to review and edit before saving.
     * </p>
     * <p>
     * <b>Response Format (Success):</b>
     * <pre>{@code
     * {
     *   "success": true,
     *   "message": "Skills extracted successfully",
     *   "data": {
     *     "skills": ["Java", "Spring Boot", "MySQL", "Docker"]
     *   }
     * }
     * }</pre>
     * </p>
     * <p>
     * <b>Error Handling:</b>
     * <ul>
     *   <li>IllegalArgumentException: Invalid file format or empty file (400 Bad Request)</li>
     *   <li>Exception: AI service failure or processing error (500 Internal Server Error)</li>
     * </ul>
     * </p>
     *
     * @param req  HttpServletRequest containing resume file
     * @param resp HttpServletResponse for sending JSON response
     * @throws IOException if I/O error occurs
     * @see TAService#extractSkills(Part)
     */
    private void extractSkills(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Object userObj = req.getSession().getAttribute("user");
        if (!verifyUser(req, resp, userObj)) return;

        try {
            Part resumePart = req.getPart("resume");
            
            List<String> skills = taService.extractSkills(resumePart);
            
            Map<String, Object> data = new HashMap<>();
            data.put("skills", skills);
            
            RespUtils.writeSuccess(resp, data, "Skills extracted successfully");
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid input for skill extraction: {}", e.getMessage());
            RespUtils.writeError(resp, e.getMessage(), HttpServletResponse.SC_BAD_REQUEST);
        } catch (Exception e) {
            log.error("Failed to extract skills", e);
            RespUtils.writeError(resp, "Failed to extract skills: " + e.getMessage(), 
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
