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
 * Service layer for user authentication and account management.
 * <p>
 * This service provides core user operations including:
 * <ul>
 *   <li><b>User Registration</b>: Save new user accounts with encrypted passwords</li>
 *   <li><b>Authentication</b>: Login validation with password verification and last login tracking</li>
 *   <li><b>Account Updates</b>: Partial updates with field merging</li>
 *   <li><b>Password Security</b>: MD5 encryption for password storage</li>
 * </ul>
 * </p>
 * <p>
 * <b>Password Security:</b> All passwords must be encrypted using MD5 hashing via
 * {@link DigestUtils#md5Hex(String)} before calling save or update methods.
 * Plain text passwords should never be stored in the database.
 * </p>
 * <p>
 * <b>Login Process:</b>
 * <ol>
 *   <li>Search for user by username</li>
 *   <li>Verify password (must be pre-encrypted)</li>
 *   <li>Update lastLoginAt timestamp</li>
 *   <li>Return UserDTO (excludes sensitive fields like password)</li>
 * </ol>
 * </p>
 * <p>
 * <b>Thread Safety:</b> This service is stateless and thread-safe. Each method operates
 * independently on the repository instance.
 * </p>
 *
 * @author Jflame
 * @version 1.0.0
 * @since 2026/3/23
 * @see JsonRepository
 * @see UserMapper
 * @see UserDTO
 */
@Slf4j
public class UserService {

    private final JsonRepository<User> userRepo = new JsonRepository<>(User.class);

    /**
     * Saves a new user account to the repository.
     * <p>
     * This method performs an upsert operation:
     * <ul>
     *   <li>If user ID already exists → updates existing record</li>
     *   <li>If user ID is new → creates new record</li>
     * </ul>
     * </p>
     * <p>
     * <b>Important:</b> The password field must be pre-encrypted with MD5 before calling this method.
     * Use {@link #encryptPassword(String)} to encrypt plain text passwords.
     * </p>
     *
     * @param user User object with all required fields set (id, name, password, role, status)
     * @return true if save succeeded, false if IO error occurred
     * @see #encryptPassword(String)
     */
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

    /**
     * Updates an existing user account with partial field updates.
     * <p>
     * This method performs a merge operation where only non-null fields from the source
     * user are applied to the existing user. The update process:
     * <ol>
     *   <li>Validates that userId is not null</li>
     *   <li>Retrieves existing user from repository</li>
     *   <li>Merges source fields into existing user via {@link #mergeUser(User, User)}</li>
     *   <li>Saves merged user back to repository</li>
     * </ol>
     * </p>
     * <p>
     * <b>Merge Behavior:</b>
     * <ul>
     *   <li>Name: Updated if source.name is non-null</li>
     *   <li>Password: Updated if source.password is non-null (should be pre-encrypted)</li>
     *   <li>Role: Always updated from source (even if null)</li>
     *   <li>Status: Always updated from source (even if null)</li>
     *   <li>UpdateAt: Always set to current timestamp</li>
     * </ul>
     * </p>
     * <p>
     * <b>Note:</b> Unlike {@link com.tars.util.BeanUtils#merge(Object, Object, String...)},
     * this method always updates role and status regardless of null values.
     * </p>
     *
     * @param source User object containing fields to update
     * @return true if update succeeded, false if userId is null, user not found, or IO error occurred
     * @see #mergeUser(User, User)
     */
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

    /**
     * Checks if a username already exists in the system.
     * <p>
     * This method performs a case-sensitive search through all users to find
     * a matching username. Used during registration to prevent duplicate usernames.
     * </p>
     * <p>
     * <b>Performance Note:</b> This operation loads all users into memory and performs
     * a linear search. For large user bases, consider adding an index or using a database.
     * </p>
     *
     * @param username Username to check for existence
     * @return true if username exists, false otherwise or if IO error occurred
     */
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

    /**
     * Authenticates a user with username and password, returning user DTO on success.
     * <p>
     * This method performs the complete login process:
     * <ol>
     *   <li>Loads all users from repository</li>
     *   <li>Searches for user with matching username</li>
     *   <li>Verifies password matches (password must be pre-encrypted with MD5)</li>
     *   <li>Updates user's lastLoginAt timestamp to current time</li>
     *   <li>Saves updated user back to repository</li>
     *   <li>Converts user to UserDTO (excludes password and timestamps)</li>
     *   <li>Returns UserDTO for session storage</li>
     * </ol>
     * </p>
     * <p>
     * <b>Security:</b> Password comparison is done on encrypted values. The caller
     * must encrypt the input password using {@link #encryptPassword(String)} before
     * passing it to this method.
     * </p>
     * <p>
     * <b>Return Value:</b> Returns null if:
     * <ul>
     *   <li>Username not found</li>
     *   <li>Password doesn't match</li>
     *   <li>IO error occurs during loading or saving</li>
     * </ul>
     * </p>
     *
     * @param username User's login username
     * @param password User's encrypted password (MD5 hash)
     * @return UserDTO with basic user info (id, name, role, status), or null if authentication failed
     * @see UserMapper#toDTO(User)
     * @see #encryptPassword(String)
     */
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

    /**
     * Retrieves a user entity by their unique identifier.
     * <p>
     * This method returns the complete User entity including all fields (password, timestamps, etc.).
     * Use with caution as it exposes sensitive data. For display purposes, prefer using
     * DTOs that exclude sensitive fields.
     * </p>
     *
     * @param id Unique user identifier (UUID)
     * @return User entity, or null if not found or IO error occurred
     */
    public User getUserById(String id) {
        try {
            return userRepo.getEntityById(id);
        } catch (IOException e) {
            log.error("get user by id failed, id: {}, error message: {}", id, e.getMessage());
        }
        return null;
    }

    /**
     * Encrypts a plain text password using MD5 hashing.
     * <p>
     * This utility method wraps {@link DigestUtils#md5Hex(String)} to provide consistent
     * password encryption across the application.
     * </p>
     * <p>
     * <b>Usage:</b>
     * <pre>{@code
     * // During registration
     * String encrypted = userService.encryptPassword(plainPassword);
     * user.setPassword(encrypted);
     * userService.saveUser(user);
     * 
     * // During login
     * String encryptedInput = userService.encryptPassword(inputPassword);
     * UserDTO user = userService.login(username, encryptedInput);
     * }</pre>
     * </p>
     * <p>
     * <b>Security Note:</b> MD5 is used for compatibility with existing systems.
     * For new projects, consider using stronger algorithms like bcrypt or Argon2.
     * </p>
     *
     * @param password Plain text password to encrypt
     * @return MD5-hashed password string (32-character hexadecimal)
     * @see DigestUtils#md5Hex(String)
     */
    public String encryptPassword(String password) {
        password = DigestUtils.md5Hex(password);
        return password;
    }

    /**
     * Merges fields from a source user into an existing user.
     * <p>
     * This private helper method implements the merge logic for {@link #updateUser(User)}.
     * It selectively applies non-null fields from source to exist, with special handling:
     * </p>
     * <p>
     * <b>Merge Rules:</b>
     * <ul>
     *   <li><b>Name</b>: Only updated if source.name is non-null</li>
     *   <li><b>Password</b>: Only updated if source.password is non-null (preserves existing if null)</li>
     *   <li><b>Role</b>: Always updated from source (even if null/0)</li>
     *   <li><b>Status</b>: Always updated from source (even if null/0)</li>
     *   <li><b>UpdateAt</b>: Always set to current timestamp</li>
     *   <li><b>ID</b>: Never changed (uses existing user's ID)</li>
     *   <li><b>CreateAt</b>: Never changed (preserves original creation time)</li>
     *   <li><b>LastLoginAt</b>: Not modified by this method</li>
     * </ul>
     * </p>
     * <p>
     * <b>Rationale for Role/Status Always Updating:</b> These fields are controlled
     * by administrators and should reflect the exact values provided, even if setting to 0.
     * </p>
     *
     * @param exist  Existing user entity from repository (will be modified)
     * @param source Source user containing update values
     * @return Modified existing user with merged fields
     */
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
