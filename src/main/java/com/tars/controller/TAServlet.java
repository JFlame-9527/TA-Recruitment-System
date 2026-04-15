package com.tars.controller;

import com.tars.entity.bean.Application;
import com.tars.entity.bean.TAProfile;
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

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author QiheSun Xiri04
 * @version 2.0.0
 * @since 2026/3/26
 */
@Slf4j
@WebServlet(name = "TAServlet", value = "/taServlet")
@MultipartConfig(
        fileSizeThreshold = 0, // 0MB - files smaller than this are kept in memory
        maxFileSize = 1024 * 1024 * 10,      // 10MB - maximum file size allowed
        maxRequestSize = 1024 * 1024 * 50    // 50MB - maximum request size (files + form data)
)
public class TAServlet extends BaseServlet{

    private TAService taService;

    @Override
    public void init() throws ServletException {
        super.init();
        taService = new TAService();
    }

    private void listApplied(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
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
        log.info("TA user {} requesting applied positions list, page: {}", userId, page);
        
        List<AppPosDTO> appPosList = taService.getAppPosList(userId, page);
        long totalPages = taService.getAppPosPages(userId);
        req.setAttribute("appliedList", appPosList);
        req.setAttribute("currentPage", page);
        req.setAttribute("totalPages", totalPages);
        req.getRequestDispatcher("/views/ta/home.jsp").forward(req, resp);
    }

    private void withdraw(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String appId = req.getParameter("appId");
        if (appId == null || appId.trim().isEmpty()) {
            log.warn("Invalid appId: {}", appId);
            RespUtils.writeError(resp, "Invalid appId", HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        Object userObj = req.getSession().getAttribute("user");
        if (!verifyUser(req, resp, userObj)) return;

        String userId = ((UserDTO)userObj).getId();

        taService.withdrawApplication(appId, userId);

        Map<String, String> data = new HashMap<>();
        data.put("appId", appId);
        RespUtils.writeSuccess(resp, data, "Application withdrawn successfully");
    }

    private void listPositions(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
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
        log.info("TA user {} requesting positions list, page: {}", userId, page);

        List<PosBriefDTO> positionList = taService.getPositionList(userId, page);
        long totalPages = taService.getPositionPages();
        req.setAttribute("positionList", positionList);
        req.setAttribute("currentPage", page);
        req.setAttribute("totalPages", totalPages);
        req.getRequestDispatcher("/views/ta/positions.jsp").forward(req, resp);
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

        if (userDTO.getRole() != 1) {
            log.warn("User {} does not have TA role, role: {}", userDTO.getId(), userDTO.getRole());
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
            return false;
        }

        return true;
    }

    private void viewPosition(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String posId = req.getParameter("posId");
        String appId = req.getParameter("appId");
        String page = req.getParameter("page");
        String from = req.getParameter("from");

        PosDetailDTO pos = taService.getPosition(posId, appId);
        req.setAttribute("position", pos);
        req.setAttribute("page", page);
        req.setAttribute("from", from != null ? from : "positions");
        req.getRequestDispatcher("/views/ta/positionDetail.jsp").forward(req, resp);
    }

    private void apply(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String posId = req.getParameter("posId");

        Object userObj = req.getSession().getAttribute("user");
        if (!verifyUser(req, resp, userObj)) return;

        String userId = ((UserDTO)userObj).getId();

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
        boolean applied = taService.createApplication(application);
        
        if (applied) {
            Map<String, String> data = new HashMap<>();
            data.put("appId", application.getId());
            RespUtils.writeSuccess(resp, data, "Application submitted successfully");
        } else {
            RespUtils.writeError(resp, "Failed to apply for position",
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void getProfile(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        Object userObj = req.getSession().getAttribute("user");
        if (!verifyUser(req, resp, userObj)) return;

        String userId = ((UserDTO)userObj).getId();
        ProfileDTO profile = taService.getProfileDTO(userId);
        
        if (profile == null) {
            log.warn("Profile not found for user {}", userId);
            req.setAttribute("warn", "Profile not found. Please create your profile first.");
            req.getRequestDispatcher("/views/ta/profile.jsp").forward(req, resp);
            return;
        }

        // Generate web-accessible URL for resume if it exists
        if (profile.getResumePath() != null) {
            String resumeUrl = FileUtils.getFileUrl(req.getContextPath(), profile.getResumePath());
            req.setAttribute("resumeUrl", resumeUrl);
            log.debug("Resume URL generated: {}", resumeUrl);
        }

        req.setAttribute("profile", profile);
        req.getRequestDispatcher("/views/ta/profile.jsp").forward(req, resp);
    }

    private void downloadResume(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String fileName = req.getParameter("file");
        
        // Validate and sanitize the file path
        String sanitizedPath = FileUtils.sanitizePath(fileName);
        if (sanitizedPath == null) {
            log.warn("Invalid file path requested: {}", fileName);
            RespUtils.writeError(resp, "Invalid file path", HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String webRootPath = getServletContext().getRealPath("");
        
        // Serve the file securely using FileUtils
        try {
            FileUtils.serveFile(req, resp, webRootPath, sanitizedPath);
        } catch (IOException e) {
            log.error("Error serving file: {}", sanitizedPath, e);
            throw e;
        }
    }

    private void updateProfile(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        Object userObj = req.getSession().getAttribute("user");
        if (!verifyUser(req, resp, userObj)) return;

        String userId = ((UserDTO)userObj).getId();
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
            
            if (resumePart != null && resumePart.getSize() > 0) {
                // User uploaded a new resume
                log.info("New resume detected, processing upload...");
                
                // Save new file
                String newPath = FileUtils.savePdfFile(resumePart, webRootPath, "resumes");
                String originalFileName = Paths.get(resumePart.getSubmittedFileName()).getFileName().toString();
                
                // Set new file info
                updatedProfile.setResumePath(newPath);
                updatedProfile.setResumeName(originalFileName);
                
                // Delete old file if exists
                if (existingProfile.getResumePath() != null) {
                    boolean deleted = FileUtils.deleteFile(webRootPath, existingProfile.getResumePath());
                    if (deleted) {
                        log.info("Old resume deleted: {}", existingProfile.getResumePath());
                    } else {
                        log.warn("Failed to delete old resume: {}", existingProfile.getResumePath());
                    }
                }
            } else {
                // No new file, keep existing resume info
                updatedProfile.setResumePath(existingProfile.getResumePath());
                updatedProfile.setResumeName(existingProfile.getResumeName());
                log.info("No new resume uploaded, keeping existing file");
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
            boolean success = taService.updateProfile(existingProfile);

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

    private void createProfile(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        Object userObj = req.getSession().getAttribute("user");
        if (!verifyUser(req, resp, userObj)) return;

        String userId = ((UserDTO)userObj).getId();

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
            
            String webRootPath = getServletContext().getRealPath("");
            String resumePath = FileUtils.savePdfFile(resumePart, webRootPath, "resumes");
            profile.setResumePath(resumePath);
            
            log.info("Resume uploaded successfully - Original name: {}, Saved path: {}", 
                    originalFileName, resumePath);
            
            if (profile.getName() == null || profile.getName().trim().isEmpty()) {
                req.setAttribute("error", "Name is required");
                req.getRequestDispatcher("/views/ta/profile.jsp").forward(req, resp);
                return;
            }
            
            boolean created = taService.createProfile(profile);
            
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
}
