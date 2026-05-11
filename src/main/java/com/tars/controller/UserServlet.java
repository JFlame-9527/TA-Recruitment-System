package com.tars.controller;

import com.google.code.kaptcha.Constants;
import com.tars.entity.bean.User;
import com.tars.entity.dto.user.UserDTO;
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
 * @author Jflame
 * @version 1.0.0
 * @since 2026/3/23
 */
@WebServlet(name = "UserServlet", urlPatterns = "/userServlet")
public class UserServlet extends BaseServlet {

    private static final Logger log = LoggerFactory.getLogger(UserServlet.class);

    private UserService userService;

    public static final String KAPTCHA_SESSION_KEY = "KAPTCHA_SESSION_KEY";
    public static final String KAPTCHA_SESSION_DATE = "KAPTCHA_SESSION_DATE";
    public static final String LOGIN_FAIL_COUNT = "LOGIN_FAIL_COUNT";
    public static final long CAPTCHA_EXPIRY_TIME = 60000; // 60 seconds

    @Override
    public void init() throws ServletException {
        super.init();
        userService = new UserService();
    }

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

    private void checkUsername(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String username = req.getParameter("username");
        boolean exists = userService.checkUserExist(username);
        
        if (exists) {
            RespUtils.writeError(resp, "Username already exists");
        } else {
            RespUtils.writeSuccess(resp, "Username available");
        }
    }

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

    private void logout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String userId = req.getParameter("userId");
        req.getSession().removeAttribute("user");

        log.info("logout user status success, userId: {}", userId);

        resp.sendRedirect(req.getContextPath() + "/");
    }

    private void getCaptchaStatus(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer failCount = (Integer) req.getSession().getAttribute(LOGIN_FAIL_COUNT);
        boolean requireCaptcha = failCount != null && failCount >= 1;
        
        Map<String, Object> data = new HashMap<>();
        data.put("requireCaptcha", requireCaptcha);
        data.put("failCount", failCount != null ? failCount : 0);
        
        RespUtils.writeSuccess(resp, data);
    }

}
