package com.tars.service;

import com.tars.entity.bean.User;
import com.tars.entity.dto.user.UserDTO;
import com.tars.mapper.UserMapper;
import com.tars.repository.JsonRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Jflame
 * @version 1.0.0
 * @since 2026/3/23
 */
@Slf4j
public class UserService {

    private final JsonRepository<User> userRepo = new JsonRepository<>(User.class);

    public boolean saveUser(User user) {
        try {
            userRepo.saveEntity(user);
            log.info("user save success, userId: {}", user.getId());
        } catch (IOException e) {
            log.error("user save failed, userId: {}, error message: {}", user.getId(), e.getMessage());
            return false;
        }
        return true;
    }

    public boolean updateUser(User source) {
        String userId = source.getId();
        if (userId == null) {
            log.error("update user failed, userId is null");
            return false;
        }
        
        User exist = getUserById(userId);
        if (exist == null) {
            log.error("update user failed, user not found, userId: {}", userId);
            return false;
        }
        
        User user = mergeUser(exist, source);
        
        try {
            userRepo.saveEntity(user);
            log.info("user update success, userId: {}", userId);
        } catch (IOException e) {
            log.error("user update failed, userId: {}, error message: {}", userId, e.getMessage());
            return false;
        }
        return true;
    }

    public boolean checkUserExist(String username) {
        try {
            List<User> users = userRepo.loadAllEntities();
            for (User user : users) {
                if (user.getName().equals(username)) {
                    return true;
                }
            }
        } catch (IOException e) {
            log.error("check user exist failed, username: {}, error message: {}", username, e.getMessage());
        }
        return false;
    }

    public UserDTO login(String username, String password) {
        try {
            List<User> users = userRepo.loadAllEntities();
            for (User user : users) {
                if (user.getName().equals(username) && user.getPassword().equals(password)) {
                    UserDTO dto = UserMapper.INSTANCE.toDTO(user);

                    user.setLastLoginAt(Timestamp.valueOf(LocalDateTime.now()));
                    userRepo.saveEntity(user);

                    return dto;
                }
            }
        } catch (IOException e) {
            log.error("login failed, username: {}, error message: {}", username, e.getMessage());
        }
        return null;
    }

    public User getUserById(String id) {
        try {
            return userRepo.getEntityById(id);
        } catch (IOException e) {
            log.error("get user by id failed, id: {}, error message: {}", id, e.getMessage());
        }
        return null;
    }

    public String encryptPassword(String password) {
        password = DigestUtils.md5Hex(password);
        return password;
    }

    private User mergeUser(User exist, User source) {
        if (source.getName() != null) {
            exist.setName(source.getName());
        }

        if (source.getPassword() != null) {
            exist.setPassword(source.getPassword());
        }

        exist.setRole(source.getRole());

        exist.setStatus(source.getStatus());
        
        exist.setUpdateAt(Timestamp.valueOf(LocalDateTime.now()));
        return exist;
    }
}
