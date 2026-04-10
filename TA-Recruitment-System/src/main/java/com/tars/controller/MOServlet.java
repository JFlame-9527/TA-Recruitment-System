package com.tars.controller;

import com.tars.entity.bean.Position;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jflame
 * @version 1.0.0
 * @since 2026/3/29
 */
@Slf4j
@WebServlet(name = "MOServlet", value = "/moServlet")
@MultipartConfig(
        fileSizeThreshold = 0,
        maxFileSize = 1024 * 1024 * 10,
        maxRequestSize = 1024 * 1024 * 50
)
public class MOServlet extends BaseServlet{

    private MOService moService;

    @Override
    public void init() throws ServletException {
        super.init();
        moService = new MOService();
    }


    private void listPosition(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pageParam = req.getParameter("page");
        if (pageParam == null || pageParam.trim().isEmpty()) {
            pageParam = "1";
        }

        int page;
        try {
            page = Integer.parseInt(pageParam);
            if (page < 1) {
                page = 1;
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid page number: {}", pageParam);
            page = 1;
        }

        Object userObj = req.getSession().getAttribute("user");
        if (!verifyUser(req, resp, userObj)) return;

        String userId = ((UserDTO)userObj).getId();
        log.info("MO user {} requesting positions list, page: {}", userId, page);

        List<PosBriefDTO> positionList = moService.getPositionList(userId, page);
        long totalPages = moService.getPositionPages(userId);

        req.setAttribute("positionList", positionList);
        req.setAttribute("currentPage", page);
        req.setAttribute("totalPages", totalPages);
        req.getRequestDispatcher("/views/mo/home.jsp").forward(req, resp);
    }

    private void positionDetail(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Object userObj = req.getSession().getAttribute("user");
        if (!verifyUser(req, resp, userObj)) return;

        String userId = ((UserDTO)userObj).getId();
        String posId = req.getParameter("posId");
        String fromPage = req.getParameter("page");

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
        req.setAttribute("fromPage", fromPage != null ? fromPage : "1");
        req.getRequestDispatcher("/views/mo/position.jsp").forward(req, resp);
    }

    private void postPosition(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Object userObj = req.getSession().getAttribute("user");
        if (!verifyUser(req, resp, userObj)) return;

        req.getRequestDispatcher("/views/mo/post.jsp").forward(req, resp);
    }

    private void createPosition(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Object userObj = req.getSession().getAttribute("user");
        if (!verifyUser(req, resp, userObj)) return;

        String userId = ((UserDTO)userObj).getId();
        log.info("MO user {} creating position", userId);

        try {
            Position position = BeanUtils.mapFromReq(req, Position.class);
            
            position.setPostUserId(userId);
            position.setStatus(0);
            position.setAppliedNum(0);
            position.setOfferedNum(0);
            position.setRejectedNum(0);

            boolean success = moService.createPosition(position);

            if (success) {
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

    private void updatePosition(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Object userObj = req.getSession().getAttribute("user");
        if (!verifyUser(req, resp, userObj)) return;

        String userId = ((UserDTO)userObj).getId();
        String posId = req.getParameter("posId");

        if (posId == null || posId.trim().isEmpty()) {
            RespUtils.writeError(resp, "Position ID is required", HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if (!moService.verifyPositionOwner(posId, userId)) {
            RespUtils.writeError(resp, "You don't have permission to update this position", HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        log.info("MO user {} updating position {}", userId, posId);

        try {
            Position updatedFields = BeanUtils.mapFromReq(req, Position.class);
            
            updatedFields.setId(posId);
            updatedFields.setPostUserId(userId);

            boolean success = moService.updatePosition(updatedFields);

            if (success) {
                RespUtils.writeSuccess(resp, "Position updated successfully");
            } else {
                RespUtils.writeError(resp, "Failed to update position", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }

        } catch (Exception e) {
            log.error("Error updating position {} for user {}", posId, userId, e);
            RespUtils.writeError(resp, "Error updating position: " + e.getMessage(), HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private void listApp(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String posId = req.getParameter("posId");
        if (posId == null || posId.trim().isEmpty()) {
            RespUtils.writeError(resp, "Position ID is required", HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        Object userObj = req.getSession().getAttribute("user");
        if (!verifyUser(req, resp, userObj)) return;

        String userId = ((UserDTO)userObj).getId();

        if (!moService.verifyPositionOwner(posId, userId)) {
            RespUtils.writeError(resp, "You don't have permission to view applications for this position", HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String pageParam = req.getParameter("page");
        if (pageParam == null || pageParam.trim().isEmpty()) {
            pageParam = "1";
        }

        int page;
        try {
            page = Integer.parseInt(pageParam);
            if (page < 1) {
                page = 1;
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid page number: {}", pageParam);
            page = 1;
        }

        log.info("MO user {} requesting applications for position {}, page: {}", userId, posId, page);

        List<ApplicationDTO> appList = moService.getAppList(posId, page);
        long totalPages = moService.getAppPages(posId);

        Map<String, Object> data = new HashMap<>();
        data.put("appList", appList);
        data.put("currentPage", page);
        data.put("totalPages", totalPages);

        RespUtils.writeSuccess(resp, data);
    }

    private void getProfile(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Object userObj = req.getSession().getAttribute("user");
        if (!verifyUser(req, resp, userObj)) return;

        String userId = ((UserDTO)userObj).getId();
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

        RespUtils.writeSuccess(resp, profile);
    }

    private void offerApplication(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Object userObj = req.getSession().getAttribute("user");
        if (!verifyUser(req, resp, userObj)) return;

        String userId = ((UserDTO)userObj).getId();
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

    private void rejectApplication(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Object userObj = req.getSession().getAttribute("user");
        if (!verifyUser(req, resp, userObj)) return;

        String userId = ((UserDTO)userObj).getId();
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
