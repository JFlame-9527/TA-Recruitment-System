package com.tars.controller;

import com.tars.entity.bean.MOProfile;
import com.tars.entity.bean.User;
import com.tars.entity.dto.QueryCondition;
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
 * @author wangyue
 * @version 4.0.0
 * @since 2026/4/14
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

    @Override
    public void init() throws ServletException {
        super.init();
        adminService = new AdminService();
    }

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
