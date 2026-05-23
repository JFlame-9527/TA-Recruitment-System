package com.tars.controller;

import com.tars.entity.bean.Position;
import com.tars.entity.QueryCondition;
import com.tars.entity.dto.mo.ApplicationDTO;
import com.tars.entity.dto.mo.PosBriefDTO;
import com.tars.entity.dto.mo.PosDetailDTO;
import com.tars.entity.dto.mo.ProfileDTO;
import com.tars.entity.dto.user.UserDTO;
import com.tars.service.MOService;
import com.tars.util.BeanUtils;
import com.tars.util.RespUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servlet for Module Owner (MO) operations in the TA recruitment system.
 * <p>
 * This servlet provides comprehensive MO functionality:
 * <ul>
 *   <li><b>Position Management</b>: Create, repost, view, and withdraw positions</li>
 *   <li><b>Application Review</b>: View applications with AI-based recommendation sorting</li>
 *   <li><b>Decision Making</b>: Offer or reject TA applications with feedback</li>
 *   <li><b>Profile Viewing</b>: Review TA profiles with application context</li>
 * </ul>
 * </p>
 * <p>
 * <b>Access Control:</b> All operations require MO authentication (role=2).
 * The {@link #verifyUser(HttpServletRequest, HttpServletResponse, Object)} method validates:
 * <ol>
 *   <li>User is logged in (session contains "user" attribute)</li>
 *   <li>User object is valid UserDTO instance</li>
 *   <li>User has MO role (role=2)</li>
 * </ol>
 * Failed validation results in redirect to login page or HTTP 401/403 error.
 * </p>
 * <p>
 * <b>Position Ownership:</b> Most position-related operations verify that the MO is the
 * owner of the position via {@link MOService#verifyPositionOwner(String, String)}.
 * This prevents unauthorized access to other MOs' positions and applications.
 * </p>
 * <p>
 * <b>Repost Feature:</b> Supports re-opening withdrawn positions while preserving:
 * <ul>
 *   <li>Original position ID (maintains application history linkage)</li>
 *   <li>Original creation timestamp</li>
 *   <li>All other fields can be updated (description, dates, requirements, etc.)</li>
 * </ul>
 * Repost workflow: repostPosition() → post() (with repostId) → postPosition()
 * </p>
 * <p>
 * <b>Request Mapping:</b> All operations are routed through {@link BaseServlet} using
 * the {@code action} parameter:
 * <pre>
 * GET  /moServlet?action=listPosition          → listPosition() [Full page]
 * GET  /moServlet?action=positionDetail         → positionDetail() [Full page]
 * GET  /moServlet?action=post                   → post() [Full page - form display]
 * POST /moServlet?action=postPosition           → postPosition() [AJAX - form submit]
 * POST /moServlet?action=withdrawnPosition      → withdrawnPosition() [AJAX]
 * POST /moServlet?action=repostPosition         → repostPosition() [Redirect to post form]
 * GET  /moServlet?action=listApp                → listApp() [AJAX pagination]
 * GET  /moServlet?action=getProfile             → getProfile() [AJAX]
 * POST /moServlet?action=offerApplication       → offerApplication() [AJAX]
 * POST /moServlet?action=rejectApplication      → rejectApplication() [AJAX]
 * </pre>
 * </p>
 * <p>
 * <b>AI Integration:</b> Position creation triggers AI-powered portrait generation
 * via {@link MOService#postPosition(Position, String)}. Application listing supports
 * AI-based recommendation sorting that considers skill match and workload constraints.
 * </p>
 *
 * @author 477996850
 * @version 1.0.0
 * @since 2026/3/29
 * @see MOService
 * @see BaseServlet
 * @see RespUtils
 */
@Slf4j
@WebServlet(name = "MOServlet", value = "/moServlet")
@MultipartConfig(
        fileSizeThreshold = 0,
        maxFileSize = 1024 * 1024 * 10,
        maxRequestSize = 1024 * 1024 * 50
)
public class MOServlet extends BaseServlet {

    private MOService moService;

    /**
     * Initializes the servlet and creates MOService instance.
     *
     * @throws ServletException if initialization fails
     */
    @Override
    public void init() throws ServletException {
        super.init();
        moService = new MOService();
    }


    /**
     * Lists MO's posted positions with full page rendering.
     * <p>
     * This method displays all positions posted by the MO, with filtering and pagination:
     * <ol>
     *   <li>Verifies MO authentication</li>
     *   <li>Extracts query conditions from request (filter, order, page)</li>
     *   <li>Calls {@link MOService#getPositionList(String, QueryCondition)}</li>
     *   <li>Calculates total pages for pagination</li>
     *   <li>Sets request attributes for JSP rendering</li>
     *   <li>Forwards to /views/mo/home.jsp</li>
     * </ol>
     * </p>
     * <p>
     * <b>Filter Options:</b>
     * <ul>
     *   <li>"all" - Show all positions</li>
     *   <li>"opened" - Positions accepting applications (status=0)</li>
     *   <li>"closed" - Positions no longer accepting (status=1)</li>
     *   <li>"filled" - All required TAs have been offered (status=2)</li>
     *   <li>"withdrawn" - Cancelled positions (status=3)</li>
     * </ul>
     * </p>
     * <p>
     * <b>Sort Options:</b>
     * <ul>
     *   <li>"postDate" - By posting date (newest first)</li>
     *   <li>"deadline" - By application deadline (earliest first)</li>
     * </ul>
     * </p>
     * <p>
     * <b>Display Content:</b> Each PosBriefDTO includes:
     * <ul>
     *   <li>Position basics: id, title, description</li>
     *   <li>Dates: postDate, deadline</li>
     *   <li>Statistics: requiredNum, offeredNum, appliedNum, rejectedNum, vacancyNum, pendingNum</li>
     *   <li>Status: numeric status code</li>
     * </ul>
     * </p>
     *
     * @param req  HttpServletRequest containing query parameters
     * @param resp HttpServletResponse for forwarding to JSP
     * @throws IOException      if I/O error occurs
     * @throws ServletException if servlet error occurs
     * @see MOService#getPositionList(String, QueryCondition)
     * @see MOService#getPositionPages(String, QueryCondition)
     */
    private void listPosition(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        QueryCondition condition = BeanUtils.mapFromReq(req, QueryCondition.class);

        Object userObj = req.getSession().getAttribute("user");
        if (!verifyUser(req, resp, userObj)) return;

        String userId = ((UserDTO) userObj).getId();
        log.info("MO user {} requesting positions list, page: {}", userId, condition.getPage());

        List<PosBriefDTO> positionList = moService.getPositionList(userId, condition);
        long totalPages = moService.getPositionPages(userId, condition);

        req.setAttribute("positionList", positionList);
        req.setAttribute("condition", condition);
        req.setAttribute("totalPages", totalPages);
        req.getRequestDispatcher("/views/mo/home.jsp").forward(req, resp);
    }

    /**
     * Displays detailed information for a specific position.
     * <p>
     * This method shows complete position details for MO management:
     * <ol>
     *   <li>Verifies MO authentication</li>
     *   <li>Validates posId parameter is provided</li>
     *   <li>Verifies MO owns the position ({@link MOService#verifyPositionOwner(String, String)})</li>
     *   <li>Calls {@link MOService#getPosition(String)} to get position details</li>
     *   <li>Validates position exists</li>
     *   <li>Sets request attributes for JSP rendering</li>
     *   <li>Forwards to /views/mo/position.jsp</li>
     * </ol>
     * </p>
     * <p>
     * <b>Security:</b> Position ownership verification ensures MOs can only view
     * their own positions, preventing unauthorized access to other MOs' data.
     * </p>
     * <p>
     * <b>Display Content:</b> PosDetailDTO includes all position fields plus:
     * <ul>
     *   <li>Calculated fields: vacancyNum (required - offered), pendingNum (applied - offered - rejected)</li>
     *   <li>Complete statistics for management dashboard</li>
     * </ul>
     * </p>
     * <p>
     * <b>Navigation Context:</b> Preserves query condition for returning to list
     * with same filters/sort order.
     * </p>
     *
     * @param req  HttpServletRequest containing posId parameter
     * @param resp HttpServletResponse for forwarding to JSP
     * @throws IOException      if I/O error occurs
     * @throws ServletException if servlet error occurs
     * @see MOService#getPosition(String)
     * @see MOService#verifyPositionOwner(String, String)
     */
    private void positionDetail(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Object userObj = req.getSession().getAttribute("user");
        if (!verifyUser(req, resp, userObj)) return;

        String userId = ((UserDTO) userObj).getId();
        String posId = req.getParameter("posId");
        QueryCondition condition = BeanUtils.mapFromReq(req, QueryCondition.class);

        if (posId == null || posId.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Position ID is required");
            return;
        }

        if (!moService.verifyPositionOwner(posId, userId)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
            return;
        }

        PosDetailDTO position = moService.getPosition(posId);
        if (position == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Position not found");
            return;
        }

        req.setAttribute("position", position);
        req.setAttribute("posId", posId);
        req.setAttribute("fromCondition", condition);
        req.getRequestDispatcher("/views/mo/position.jsp").forward(req, resp);
    }

    /**
     * Displays position creation/repost form.
     * <p>
     * This method prepares the post.jsp form for either new position creation or reposting:
     * <ol>
     *   <li>Verifies MO authentication</li>
     *   <li>Checks for repostId parameter:
     *     <ul>
     *       <li>If repostId provided:
     *         <ul>
     *           <li>Loads original position data</li>
     *           <li>Validates position is withdrawn (status=3)</li>
     *           <li>Sets repostData and isRepost attributes for form pre-fill</li>
     *           <li>Stores repostPositionId in session for later use</li>
     *         </ul>
     *       </li>
     *       <li>If no repostId: Clears repostPositionId from session</li>
     *     </ul>
     *   </li>
     *   <li>Forwards to /views/mo/post.jsp</li>
     * </ol>
     * </p>
     * <p>
     * <b>Repost Validation:</b> Only withdrawn positions (status=3) can be reposted.
     * This prevents accidental duplication of active positions.
     * </p>
     * <p>
     * <b>Session Management:</b> Stores repostPositionId in session to persist across
     * the form display (GET) and submission (POST) requests.
     * </p>
     * <p>
     * <b>Form Pre-fill:</b> When reposting, the form is pre-populated with original
     * position data, allowing MOs to modify fields before re-submitting.
     * </p>
     *
     * @param req  HttpServletRequest containing optional repostId parameter
     * @param resp HttpServletResponse for forwarding to JSP
     * @throws IOException      if I/O error occurs
     * @throws ServletException if servlet error occurs
     * @see MOService#getPosition(String)
     */
    private void post(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Object userObj = req.getSession().getAttribute("user");
        if (!verifyUser(req, resp, userObj)) return;

        String repostId = req.getParameter("repostId");
        
        if (repostId != null && !repostId.trim().isEmpty()) {
            PosDetailDTO originalPosition = moService.getPosition(repostId);
            if (originalPosition != null && originalPosition.getStatus() == 3) {
                req.setAttribute("repostData", originalPosition);
                req.setAttribute("isRepost", true);
                req.getSession().setAttribute("repostPositionId", repostId);
                log.info("Loading repost data for position {}", repostId);
            } else {
                log.warn("Invalid repost request for position {}", repostId);
            }
        } else {
            req.getSession().removeAttribute("repostPositionId");
        }

        req.getRequestDispatcher("/views/mo/post.jsp").forward(req, resp);
    }

    /**
     * Creates a new position or reposts an existing one.
     * <p>
     * This AJAX endpoint handles position creation/submission:
     * <ol>
     *   <li>Verifies MO authentication</li>
     *   <li>Maps request parameters to Position object</li>
     *   <li>Sets postUserId from session</li>
     *   <li>Sets postDate to current timestamp</li>
     *   <li>Retrieves repostPositionId from session (if reposting)</li>
     *   <li>Calls {@link MOService#postPosition(Position, String)}:
     *     <ul>
     *       <li>Normal mode: Creates new position with new UUID</li>
     *       <li>Repost mode: Updates existing position, preserving ID and createAt</li>
     *       <li>Generates AI portrait from position data</li>
     *     </ul>
     *   </li>
     *   <li>Clears repostPositionId from session after successful submission</li>
     *   <li>Returns success with posId or error response</li>
     * </ol>
     * </p>
     * <p>
     * <b>AI Integration:</b> Position creation triggers automatic portrait generation
     * using {@link com.tars.ai.PortraitGenerator}. The portrait vector is used for
     * matching with TA profiles during application review.
     * </p>
     * <p>
     * <b>Repost Workflow:</b>
     * <pre>
     * 1. MO clicks "Repost" on withdrawn position
     * 2. repostPosition() validates and redirects to post()?repostId=xxx
     * 3. post() loads original data and stores repostPositionId in session
     * 4. MO modifies fields and submits form
     * 5. postPosition() retrieves repostPositionId from session and calls service
     * 6. Service updates existing position (preserves ID) instead of creating new one
     * 7. Session attribute cleared to prevent accidental reuse
     * </pre>
     * </p>
     * <p>
     * <b>Response Format (Success):</b>
     * <pre>{@code
     * {
     *   "success": true,
     *   "message": "Position created successfully",
     *   "data": {
     *     "posId": "uuid-string"
     *   }
     * }
     * }</pre>
     * </p>
     *
     * @param req  HttpServletRequest containing position data
     * @param resp HttpServletResponse for sending JSON response
     * @throws IOException      if I/O error occurs
     * @throws ServletException if servlet error occurs
     * @see MOService#postPosition(Position, String)
     */
    private void postPosition(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Object userObj = req.getSession().getAttribute("user");
        if (!verifyUser(req, resp, userObj)) return;

        String userId = ((UserDTO) userObj).getId();
        log.info("MO user {} creating position", userId);

        try {
            Position position = BeanUtils.mapFromReq(req, Position.class);

            position.setPostUserId(userId);
            position.setPostDate(Timestamp.valueOf(LocalDateTime.now()));

            String repostId = (String) req.getSession().getAttribute("repostPositionId");

            if (repostId != null && !repostId.trim().isEmpty()) {
                log.info("Reposting position {}", repostId);
            }

            boolean success = moService.postPosition(position, repostId);

            if (success) {
                // Clear repost session attribute after successful submission
                req.getSession().removeAttribute("repostPositionId");
                
                Map<String, String> data = new HashMap<>();
                data.put("posId", position.getId());
                RespUtils.writeSuccess(resp, data, "Position created successfully");
            } else {
                RespUtils.writeError(resp, "Failed to create position", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }

        } catch (Exception e) {
            log.error("Error creating position for user {}", userId, e);
            RespUtils.writeError(resp, "Error creating position: " + e.getMessage(), HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    /**
     * Withdraws a position and cleans up all related data.
     * <p>
     * This AJAX endpoint allows MOs to cancel positions:
     * <ol>
     *   <li>Verifies MO authentication</li>
     *   <li>Validates posId parameter is provided</li>
     *   <li>Verifies MO owns the position</li>
     *   <li>Calls {@link MOService#withdrawPosition(String)} which:
     *     <ul>
     *       <li>Sets position status to 3 (withdrawn)</li>
     *       <li>Resets statistics (appliedNum=0, offeredNum=0, rejectedNum=0)</li>
     *       <li>Deletes all applications for this position</li>
     *       <li>Deletes position portrait</li>
     *     </ul>
     *   </li>
     *   <li>Returns success with posId and new status</li>
     * </ol>
     * </p>
     * <p>
     * <b>Warning:</b> Withdrawal is irreversible and removes all application history.
     * Use with caution. Consider keeping position open but marking as "not hiring" instead.
     * </p>
     * <p>
     * <b>Use Case:</b> Withdraw positions when:
     * <ul>
     *   <li>Project is cancelled</li>
     *   <li>Funding is removed</li>
     *   <li>Position requirements change significantly</li>
     * </ul>
     * </p>
     * <p>
     * <b>Response Format:</b>
     * <pre>{@code
     * {
     *   "success": true,
     *   "message": "Position withdrawn successfully",
     *   "data": {
     *     "posId": "uuid-string",
     *     "status": 3
     *   }
     * }
     * }</pre>
     * </p>
     *
     * @param req  HttpServletRequest containing posId parameter
     * @param resp HttpServletResponse for sending JSON response
     * @throws IOException      if I/O error occurs
     * @throws ServletException if servlet error occurs
     * @see MOService#withdrawPosition(String)
     * @see MOService#verifyPositionOwner(String, String)
     */
    private void withdrawnPosition(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Object userObj = req.getSession().getAttribute("user");
        if (!verifyUser(req, resp, userObj)) return;

        String userId = ((UserDTO) userObj).getId();
        String posId = req.getParameter("posId");

        if (posId == null || posId.trim().isEmpty()) {
            RespUtils.writeError(resp, "Position ID is required", HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if (!moService.verifyPositionOwner(posId, userId)) {
            RespUtils.writeError(resp, "You don't have permission to withdraw this position", HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        log.info("MO user {} withdrawing position {}", userId, posId);

        boolean success = moService.withdrawPosition(posId);

        if (success) {
            Map<String, Object> data = new HashMap<>();
            data.put("posId", posId);
            data.put("status", 3);
            RespUtils.writeSuccess(resp, data, "Position withdrawn successfully");
        } else {
            RespUtils.writeError(resp, "Failed to withdraw position", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Initiates repost workflow for a withdrawn position.
     * <p>
     * This method validates repost eligibility and redirects to the post form:
     * <ol>
     *   <li>Verifies MO authentication</li>
     *   <li>Validates posId parameter is provided</li>
     *   <li>Verifies MO owns the position</li>
     *   <li>Loads position details</li>
     *   <li>Validates position exists</li>
     *   <li>Validates position status is 3 (withdrawn)</li>
     *   <li>Redirects to post()?repostId={posId}</li>
     * </ol>
     * </p>
     * <p>
     * <b>Validation:</b> Only withdrawn positions (status=3) can be reposted.
     * This prevents:
     * <ul>
     *   <li>Duplicating active positions</li>
     *   <li>Accidentally overwriting positions with applications</li>
     *   <li>Confusion about position history</li>
     * </ul>
     * </p>
     * <p>
     * <b>Redirect Pattern:</b> Uses PRG (Post-Redirect-Get) pattern to prevent
     * duplicate submissions. The actual repost happens when MO submits the form
     * via postPosition().
     * </p>
     * <p>
     * <b>Workflow:</b>
     * <pre>
     * 1. MO clicks "Repost" button on withdrawn position
     * 2. Frontend calls: POST /moServlet?action=repostPosition&posId=xxx
     * 3. This method validates and redirects to: GET /moServlet?action=post&repostId=xxx
     * 4. post() method loads original data and displays form
     * 5. MO modifies fields and submits: POST /moServlet?action=postPosition
     * 6. postPosition() creates updated position with preserved ID
     * </pre>
     * </p>
     *
     * @param req  HttpServletRequest containing posId parameter
     * @param resp HttpServletResponse for redirect
     * @throws IOException if I/O error occurs during redirect
     * @see MOService#getPosition(String)
     * @see MOService#verifyPositionOwner(String, String)
     */
    private void repostPosition(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Object userObj = req.getSession().getAttribute("user");
        if (!verifyUser(req, resp, userObj)) return;

        String userId = ((UserDTO) userObj).getId();
        String posId = req.getParameter("posId");

        if (posId == null || posId.trim().isEmpty()) {
            log.warn("Repost failed: missing posId for user {}", userId);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Position ID is required");
            return;
        }

        if (!moService.verifyPositionOwner(posId, userId)) {
            log.warn("Repost failed: user {} unauthorized access to position {}", userId, posId);
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
            return;
        }

        PosDetailDTO position = moService.getPosition(posId);
        if (position == null) {
            log.warn("Repost failed: position {} not found", posId);
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Position not found");
            return;
        }

        if (position.getStatus() != 3) {
            log.warn("Repost failed: position {} status is {} (expected 3)", posId, position.getStatus());
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Only withdrawn positions can be reposted");
            return;
        }

        log.info("MO user {} reposting position {}", userId, posId);
        
        resp.sendRedirect(req.getContextPath() + "/moServlet?action=post&repostId=" + posId);
    }

    /**
     * Lists applications for a position with AJAX pagination.
     * <p>
     * This method provides dynamic application listing with advanced filtering and AI recommendations:
     * <ol>
     *   <li>Validates posId parameter is provided</li>
     *   <li>Verifies MO authentication</li>
     *   <li>Verifies MO owns the position</li>
     *   <li>Extracts query conditions from request</li>
     *   <li>Calls {@link MOService#getAppList(String, QueryCondition)}</li>
     *   <li>Calculates total pages for pagination</li>
     *   <li>Returns JSON response with applications and metadata</li>
     * </ol>
     * </p>
     * <p>
     * <b>Filter Options:</b>
     * <ul>
     *   <li>"all" - Show all applications except withdrawn</li>
     *   <li>"opened" - Applications awaiting review (status=0)</li>
     *   <li>"offered" - Applications with offers extended (status=1)</li>
     *   <li>"rejected" - Declined applications (status=2)</li>
     * </ul>
     * </p>
     * <p>
     * <b>Sort Options:</b>
     * <ul>
     *   <li>"applyAt" - By application submission time (oldest first)</li>
     *   <li>"recommend" - AI-based recommendation with workload validation:
     *     <ul>
     *       <li>Calculates match score between TA and position portraits</li>
     *       <li>Checks if adding position exceeds TA's max weekly workload</li>
     *       <li>Sorts non-exceeding TAs by score (descending)</li>
     *       <li>Appends exceeding TAs at end (sorted by score)</li>
     *     </ul>
     *   </li>
     * </ul>
     * </p>
     * <p>
     * <b>AI Recommendation Details:</b> When sorting by "recommend":
     * <ol>
     *   <li>Load position portrait vector</li>
     *   <li>Generate weekly periods from position start/end dates</li>
     *   <li>For each application:
     *     <ul>
     *       <li>Retrieve TA profile and max weekly workload</li>
     *       <li>Get all other positions where TA has been offered</li>
     *       <li>Check if adding this position would exceed workload in any week</li>
     *       <li>If exceeds: add to exceedList</li>
     *       <li>If not exceeds: calculate match score, add to notExceedList</li>
     *     </ul>
     *   </li>
     *   <li>Sort notExceedList by match score (descending)</li>
     *   <li>Append exceedList at the end (also sorted by score)</li>
     *   <li>Apply pagination</li>
     * </ol>
     * </p>
     * <p>
     * <b>Response Format:</b>
     * <pre>{@code
     * {
     *   "success": true,
     *   "message": "success",
     *   "data": {
     *     "appList": [...],
     *     "condition": {...},
     *     "totalPages": 3
     *   }
     * }
     * }</pre>
     * </p>
     * <p>
     * <b>Use Case:</b> Frontend JavaScript calls this endpoint when:
     * <ul>
     *   <li>MO opens position detail page (initial load)</li>
     *   <li>MO clicks pagination buttons</li>
     *   <li>MO changes filter options</li>
     *   <li>MO changes sort order (especially "recommend")</li>
     * </ul>
     * </p>
     *
     * @param req  HttpServletRequest containing posId and query parameters
     * @param resp HttpServletResponse for sending JSON response
     * @throws IOException      if I/O error occurs
     * @throws ServletException if servlet error occurs
     * @see MOService#getAppList(String, QueryCondition)
     * @see MOService#getAppPages(String, QueryCondition)
     * @see MOService#verifyPositionOwner(String, String)
     */
    private void listApp(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String posId = req.getParameter("posId");
        if (posId == null || posId.trim().isEmpty()) {
            RespUtils.writeError(resp, "Position ID is required", HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        Object userObj = req.getSession().getAttribute("user");
        if (!verifyUser(req, resp, userObj)) return;

        String userId = ((UserDTO) userObj).getId();

        if (!moService.verifyPositionOwner(posId, userId)) {
            RespUtils.writeError(resp, "You don't have permission to view applications for this position", HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        QueryCondition condition = BeanUtils.mapFromReq(req, QueryCondition.class);

        log.info("MO user {} requesting applications for position {}, page: {}, filter: {}, order: {}", 
                userId, posId, condition.getPage(), condition.getFilter(), condition.getOrder());

        List<ApplicationDTO> appList = moService.getAppList(posId, condition);
        long totalPages = moService.getAppPages(posId,  condition);

        Map<String, Object> data = new HashMap<>();
        data.put("appList", appList);
        data.put("condition", condition);
        data.put("totalPages", totalPages);

        RespUtils.writeSuccess(resp, data);
    }

    /**
     * Retrieves TA profile information for application review.
     * <p>
     * This AJAX endpoint fetches TA profile with application context:
     * <ol>
     *   <li>Verifies MO authentication</li>
     *   <li>Validates proId (profile ID) and appId (application ID) parameters</li>
     *   <li>Calls {@link MOService#getProfile(String, String)}</li>
     *   <li>Validates profile exists</li>
     *   <li>Normalizes resume path (backslashes to forward slashes)</li>
     *   <li>Returns ProfileDTO with feedback from application</li>
     * </ol>
     * </p>
     * <p>
     * <b>Profile Data:</b> ProfileDTO includes:
     * <ul>
     *   <li>Personal info: name, email, phone</li>
     *   <li>Academic info: major, GPA, year</li>
     *   <li>Skills: technical skills list</li>
     *   <li>Experience: work/project history</li>
     *   <li>Availability: max weekly workload</li>
     *   <li>Resume: resumePath, resumeName</li>
     *   <li>Feedback: Application feedback message (if any)</li>
     * </ul>
     * </p>
     * <p>
     * <b>Path Normalization:</b> Resume paths stored in database may contain backslashes
     * (Windows format). These are converted to forward slashes for safe URL usage in frontend.
     * </p>
     * <p>
     * <b>Use Case:</b> Called when MO clicks "View Profile" button in application list.
     * Opens a modal or new page displaying complete TA information for evaluation.
     * </p>
     *
     * @param req  HttpServletRequest containing proId and appId parameters
     * @param resp HttpServletResponse for sending JSON response
     * @throws IOException      if I/O error occurs
     * @throws ServletException if servlet error occurs
     * @see MOService#getProfile(String, String)
     */
    private void getProfile(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Object userObj = req.getSession().getAttribute("user");
        if (!verifyUser(req, resp, userObj)) return;

        String userId = ((UserDTO) userObj).getId();
        String proId = req.getParameter("proId");
        String appId = req.getParameter("appId");

        if (proId == null || proId.trim().isEmpty()) {
            RespUtils.writeError(resp, "Profile ID is required", HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if (appId == null || appId.trim().isEmpty()) {
            RespUtils.writeError(resp, "Application ID is required", HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        log.info("MO user {} viewing profile {} for application {}", userId, proId, appId);

        ProfileDTO profile = moService.getProfile(proId, appId);

        if (profile == null) {
            RespUtils.writeError(resp, "Profile not found", HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // Normalize resume path to use forward slashes for URL safety
        if (profile.getResumePath() != null) {
            String normalizedPath = profile.getResumePath().replace("\\", "/");
            profile.setResumePath(normalizedPath);
            log.debug("Normalized resume path for profile {}: {}", proId, normalizedPath);
        }

        RespUtils.writeSuccess(resp, profile);
    }

    /**
     * Extends an offer to a TA applicant.
     * <p>
     * This AJAX endpoint allows MOs to accept TA applications:
     * <ol>
     *   <li>Verifies MO authentication</li>
     *   <li>Validates appId and posId parameters</li>
     *   <li>Verifies MO owns the position</li>
     *   <li>Calls {@link MOService#offerApplication(String, String)} which:
     *     <ul>
     *       <li>Validates application status is 0 (applied)</li>
     *       <li>Sets application status to 1 (offered)</li>
     *       <li>Saves feedback message</li>
     *       <li>Increments position's offeredNum counter</li>
     *       <li>If offeredNum >= requiredNum: Sets position status to 1 (filled)</li>
     *     </ul>
     *   </li>
     *   <li>Returns success with appId, new status, and feedback</li>
     * </ol>
     * </p>
     * <p>
     * <b>Validation:</b> Only applications with status=0 (applied) can be offered.
     * Already offered/rejected/withdrawn applications cannot be re-offered.
     * </p>
     * <p>
     * <b>Auto-Fill Logic:</b> When offeredNum reaches requiredNum, the position is
     * automatically marked as filled (status=1). This prevents further applications
     * from being submitted.
     * </p>
     * <p>
     * <b>Feedback:</b> Optional message to TA explaining offer details (start date,
     * responsibilities, next steps, etc.). Can be null or empty.
     * </p>
     * <p>
     * <b>Response Format:</b>
     * <pre>{@code
     * {
     *   "success": true,
     *   "message": "Application offered successfully",
     *   "data": {
     *     "appId": "uuid-string",
     *     "status": 1,
     *     "feedback": "Congratulations! Please contact me to discuss details."
     *   }
     * }
     * }</pre>
     * </p>
     *
     * @param req  HttpServletRequest containing appId, posId, and optional feedback
     * @param resp HttpServletResponse for sending JSON response
     * @throws IOException      if I/O error occurs
     * @throws ServletException if servlet error occurs
     * @see MOService#offerApplication(String, String)
     * @see MOService#verifyPositionOwner(String, String)
     */
    private void offerApplication(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Object userObj = req.getSession().getAttribute("user");
        if (!verifyUser(req, resp, userObj)) return;

        String userId = ((UserDTO) userObj).getId();
        String appId = req.getParameter("appId");
        String posId = req.getParameter("posId");
        String feedback = req.getParameter("feedback");

        if (appId == null || appId.trim().isEmpty()) {
            RespUtils.writeError(resp, "Application ID is required", HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if (posId == null || posId.trim().isEmpty()) {
            RespUtils.writeError(resp, "Position ID is required", HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if (!moService.verifyPositionOwner(posId, userId)) {
            RespUtils.writeError(resp, "You don't have permission to manage this position", HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        log.info("MO user {} offering application {}", userId, appId);

        boolean success = moService.offerApplication(appId, feedback);

        if (success) {
            Map<String, Object> data = new HashMap<>();
            data.put("appId", appId);
            data.put("status", 1);
            data.put("feedback", feedback != null ? feedback : "");
            RespUtils.writeSuccess(resp, data, "Application offered successfully");
        } else {
            RespUtils.writeError(resp, "Failed to offer application", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Rejects a TA application.
     * <p>
     * This AJAX endpoint allows MOs to decline TA applications:
     * <ol>
     *   <li>Verifies MO authentication</li>
     *   <li>Validates appId and posId parameters</li>
     *   <li>Verifies MO owns the position</li>
     *   <li>Calls {@link MOService#rejectApplication(String, String)} which:
     *     <ul>
     *       <li>Validates application status is 0 (applied)</li>
     *       <li>Sets application status to 2 (rejected)</li>
     *       <li>Saves feedback message</li>
     *       <li>Increments position's rejectedNum counter</li>
     *     </ul>
     *   </li>
     *   <li>Returns success with appId, new status, and feedback</li>
     * </ol>
     * </p>
     * <p>
     * <b>Validation:</b> Only applications with status=0 (applied) can be rejected.
     * Already offered/rejected/withdrawn applications cannot be re-rejected.
     * </p>
     * <p>
     * <b>Feedback:</b> Recommended to provide constructive feedback explaining why
     * the application was rejected (lack of required skills, insufficient experience,
     * better candidates, etc.). Helps TAs improve future applications.
     * </p>
     * <p>
     * <b>Response Format:</b>
     * <pre>{@code
     * {
     *   "success": true,
     *   "message": "Application rejected successfully",
     *   "data": {
     *     "appId": "uuid-string",
     *     "status": 2,
     *     "feedback": "Thank you for your interest. We selected candidates with more relevant experience."
     *   }
     * }
     * }</pre>
     * </p>
     *
     * @param req  HttpServletRequest containing appId, posId, and optional feedback
     * @param resp HttpServletResponse for sending JSON response
     * @throws IOException      if I/O error occurs
     * @throws ServletException if servlet error occurs
     * @see MOService#rejectApplication(String, String)
     * @see MOService#verifyPositionOwner(String, String)
     */
    private void rejectApplication(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Object userObj = req.getSession().getAttribute("user");
        if (!verifyUser(req, resp, userObj)) return;

        String userId = ((UserDTO) userObj).getId();
        String appId = req.getParameter("appId");
        String posId = req.getParameter("posId");
        String feedback = req.getParameter("feedback");

        if (appId == null || appId.trim().isEmpty()) {
            RespUtils.writeError(resp, "Application ID is required", HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if (posId == null || posId.trim().isEmpty()) {
            RespUtils.writeError(resp, "Position ID is required", HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if (!moService.verifyPositionOwner(posId, userId)) {
            RespUtils.writeError(resp, "You don't have permission to manage this position", HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        log.info("MO user {} rejecting application {}", userId, appId);

        boolean success = moService.rejectApplication(appId, feedback);

        if (success) {
            Map<String, Object> data = new HashMap<>();
            data.put("appId", appId);
            data.put("status", 2);
            data.put("feedback", feedback != null ? feedback : "");
            RespUtils.writeSuccess(resp, data, "Application rejected successfully");
        } else {
            RespUtils.writeError(resp, "Failed to reject application", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Verifies that the current user has MO privileges.
     * <p>
     * This security check is called at the beginning of every MO operation to ensure
     * only authenticated MOs can access their functions.
     * </p>
     * <p>
     * <b>Validation Steps:</b>
     * <ol>
     *   <li>Checks if user object exists in session (logged in)</li>
     *   <li>Validates user object is instance of UserDTO</li>
     *   <li>Verifies user role is 2 (MO)</li>
     * </ol>
     * </p>
     * <p>
     * <b>Failure Responses:</b>
     * <ul>
     *   <li>Not logged in: Redirects to login page (/views/user/login.jsp)</li>
     *   <li>Invalid session: Sends HTTP 401 Unauthorized error</li>
     *   <li>Not MO: Sends HTTP 403 Forbidden error</li>
     * </ul>
     * </p>
     *
     * @param req     HttpServletRequest for potential redirect
     * @param resp    HttpServletResponse for sending error responses
     * @param userObj User object from session (may be null)
     * @return true if user is authenticated MO, false otherwise (response already sent)
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

        if (userDTO.getRole() != 2) {
            log.warn("User {} does not have MO role, role: {}", userDTO.getId(), userDTO.getRole());
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
            return false;
        }
        return true;
    }
}
