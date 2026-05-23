package com.tars.service;

import com.tars.entity.bean.*;
import com.tars.entity.QueryCondition;
import com.tars.entity.dto.admin.MOProDTO;
import com.tars.entity.dto.admin.TAProDTO;
import com.tars.entity.dto.admin.UserDetailDTO;
import com.tars.mapper.ProfileMapper;
import com.tars.mapper.UserMapper;
import com.tars.repository.JsonRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Service layer for administrative operations in the TA Recruitment System.
 * <p>
 * This service provides comprehensive user and system management capabilities for administrators,
 * including:
 * <ul>
 *   <li><b>User Management</b>: Delete users, update status, reset passwords, modify accounts</li>
 *   <li><b>Profile Management</b>: View and update TA/MO profiles</li>
 *   <li><b>Account Creation</b>: Create new MO accounts with associated profiles</li>
 *   <li><b>Account Listing</b>: Paginated user lists with filtering and sorting</li>
 *   <li><b>System Maintenance</b>: Automated position closure for expired deadlines</li>
 * </ul>
 * </p>
 * <p>
 * <b>Cascading Deletion Logic:</b>
 * When deleting a user, this service ensures data consistency by removing all related records:
 * <ul>
 *   <li><b>TA User (role=1)</b>:
 *     <ul>
 *       <li>Delete all applications submitted by the TA</li>
 *       <li>Update position statistics (appliedNum, offeredNum, rejectedNum)</li>
 *       <li>Delete TA profile</li>
 *     </ul>
 *   </li>
 *   <li><b>MO User (role=2)</b>:
 *     <ul>
 *       <li>Delete all positions posted by the MO</li>
 *       <li>Delete all applications for those positions</li>
 *       <li>Delete MO profile</li>
 *     </ul>
 *   </li>
 * </ul>
 * </p>
 * <p>
 * <b>Password Security:</b> All passwords are encrypted using MD5 hashing via
 * {@link DigestUtils#md5Hex(String)} before storage. Plain text passwords are never persisted.
 * </p>
 * <p>
 * <b>Pagination:</b> Uses fixed page size of 10 records per page. Page numbers are 1-based.
 * Sorting supports multiple fields with null-safe comparison.
 * </p>
 * <p>
 * <b>Thread Safety:</b> This service is stateless and thread-safe. Each method operates
 * independently on repository instances.
 * </p>
 *
 * @author wangyue
 * @version 1.0.0
 * @since 2026/3/24
 * @see JsonRepository
 * @see UserMapper
 * @see ProfileMapper
 */
@Slf4j
public class AdminService {
    private final JsonRepository<User> userRepo = new JsonRepository<>(User.class);

    private final JsonRepository<TAProfile> taProfileRepo = new JsonRepository<>(TAProfile.class);

    private final JsonRepository<MOProfile> moProfileRepo = new JsonRepository<>(MOProfile.class);

    private final JsonRepository<Application> appRepo = new JsonRepository<>(Application.class);

    private final JsonRepository<Position> posRepo = new JsonRepository<>(Position.class);

    /** Fixed page size for paginated queries (10 records per page) */
    private static final int pageSize = 10;

    /**
     * Deletes a user account and all associated data with cascading cleanup.
     * <p>
     * This method performs role-specific cascading deletion to maintain data integrity:
     * </p>
     * <p>
     * <b>For TA Users (role=1):</b>
     * <ol>
     *   <li>Iterate through all applications to find those submitted by the TA</li>
     *   <li>For each application:
     *     <ul>
     *       <li>Decrement position's appliedNum (if status != withdrawn)</li>
     *       <li>Decrement position's offeredNum (if status = offered)</li>
     *       <li>Decrement position's rejectedNum (if status = rejected)</li>
     *       <li>Save updated position</li>
     *       <li>Delete the application</li>
     *     </ul>
     *   </li>
     *   <li>Delete TA profile</li>
     *   <li>Delete user account</li>
     * </ol>
     * </p>
     * <p>
     * <b>For MO Users (role=2):</b>
     * <ol>
     *   <li>Find all positions posted by the MO</li>
     *   <li>Delete all those positions</li>
     *   <li>Find and delete all applications for those positions</li>
     *   <li>Delete MO profile</li>
     *   <li>Delete user account</li>
     * </ol>
     * </p>
     * <p>
     * <b>For Admin Users (role=0):</b>
     * <ul>
     *   <li>Only delete the user account (no associated profile or cascade)</li>
     * </ul>
     * </p>
     *
     * @param userId ID of the user to delete
     * @return true if deletion succeeded, false if user not found or IO error occurred
     */
    public boolean deleteUser(String userId) {
        try {
            User user = userRepo.getEntityById(userId);
            if (user == null) {
                log.warn("User not found for deletion, userId: {}", userId);
                return false;
            }

            int role = user.getRole();

            if (role == 1) {
                List<Application> apps = appRepo.loadAllEntities();
                for (Application app : apps) {
                    if (app.getUserId().equals(userId)) {
                        Position position = posRepo.getEntityById(app.getPositionId());
                        if (app.getStatus() != 3) {
                            position.setAppliedNum(position.getAppliedNum() - 1);
                        }
                        if (app.getStatus() == 1) {
                            position.setOfferedNum(position.getOfferedNum() - 1);
                        }
                        if (app.getStatus() == 2) {
                            position.setRejectedNum(position.getRejectedNum() - 1);
                        }
                        posRepo.saveEntity(position);
                        appRepo.deleteEntity(app.getId());
                    }
                }

                List<TAProfile> taProfiles = taProfileRepo.loadAllEntities();
                for (TAProfile tp : taProfiles) {
                    if (tp.getUserId().equals(userId)) {
                        taProfileRepo.deleteEntity(tp.getId());
                        break;
                    }
                }
            } else if (role == 2) {
                List<Position> positions = posRepo.loadAllEntities();
                Set<String> posIdList = new HashSet<>();
                for (Position pos : positions) {
                    if (pos.getPostUserId().equals(userId)) {
                        posIdList.add(pos.getId());
                        posRepo.deleteEntity(pos.getId());
                    }
                }
                List<Application> applications = appRepo.loadAllEntities();
                for (Application app : applications) {
                    if (posIdList.contains(app.getPositionId())) {
                        appRepo.deleteEntity(app.getId());
                    }
                }

                List<MOProfile> moProfiles = moProfileRepo.loadAllEntities();
                for (MOProfile mp : moProfiles) {
                    if (mp.getUserId().equals(userId)) {
                        moProfileRepo.deleteEntity(mp.getId());
                        break;
                    }
                }
            }

            userRepo.deleteEntity(userId);

            log.info("delete user success, userId: {}, role: {}", userId, role);
        } catch (IOException e) {
            log.error("delete user failed, userId: {}, error message: {}", userId, e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Updates a user's account status (active/frozen).
     * <p>
     * This method allows administrators to enable or disable user accounts without
     * deleting them. Frozen accounts cannot log in but retain all their data.
     * </p>
     * <p>
     * <b>Status Values:</b>
     * <ul>
     *   <li>0 = Available (active, can log in)</li>
     *   <li>1 = Frozen (disabled, cannot log in)</li>
     * </ul>
     * </p>
     *
     * @param userId ID of the user to update
     * @param status New status value (0 or 1)
     * @return true if update succeeded, false if user not found or IO error occurred
     */
    public boolean updateUserStatus(String userId, int status) {
        try {
            User user = userRepo.getEntityById(userId);
            if (user == null) {
                log.warn("User not found for status update, userId: {}", userId);
                return false;
            }
            user.setStatus(status);
            userRepo.saveEntity(user);
            log.info("update user status success, userId: {}, status: {}", userId, status);
            return true;
        } catch (IOException e) {
            log.error("update user status failed, userId: {}, error: {}", userId, e.getMessage());
            return false;
        }
    }

    /**
     * Resets a user's password with MD5 encryption.
     * <p>
     * This method validates the new password length (minimum 6 characters), encrypts it
     * using MD5 hashing, and updates the user record.
     * </p>
     * <p>
     * <b>Security:</b> Passwords are hashed using {@link DigestUtils#md5Hex(String)}.
     * The original plain text password is never stored.
     * </p>
     * <p>
     * <b>Validation:</b> Rejects passwords shorter than 6 characters.
     * </p>
     *
     * @param userId      ID of the user whose password to reset
     * @param newPassword New password in plain text (will be encrypted)
     * @return true if reset succeeded, false if validation failed or IO error occurred
     */
    public boolean resetPassword(String userId, String newPassword) {
        try {
            User user = userRepo.getEntityById(userId);
            if (user == null) {
                log.warn("User not found for password reset, userId: {}", userId);
                return false;
            }

            if (newPassword == null || newPassword.length() < 6) {
                log.warn("Password too short, userId: {}", userId);
                return false;
            }

            String encryptedPassword = DigestUtils.md5Hex(newPassword);
            user.setPassword(encryptedPassword);
            userRepo.saveEntity(user);

            log.info("reset password success, userId: {}", userId);
            return true;
        } catch (IOException e) {
            log.error("reset password failed, userId: {}, error: {}", userId, e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves a TA's profile information for admin viewing.
     * <p>
     * This method searches for a TA profile by userId and converts it to an admin-facing
     * DTO format using {@link ProfileMapper}.
     * </p>
     *
     * @param userId ID of the TA user
     * @return TAProDTO containing profile information, or null if not found
     */
    public TAProDTO getTAProfile(String userId) {
        try {
            List<TAProfile> taProfiles = taProfileRepo.loadAllEntities();
            TAProfile profile = taProfiles.stream()
                    .filter(tp -> tp.getUserId().equals(userId))
                    .findFirst()
                    .orElse(null);

            if (profile == null) {
                log.warn("TA profile not found, userId: {}", userId);
                return null;
            }

            return ProfileMapper.INSTANCE.toProfileDTO(profile);
        } catch (IOException e) {
            log.error("get TA profile failed, userId: {}, error: {}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * Retrieves an MO's profile information for admin viewing.
     * <p>
     * This method searches for an MO profile by userId and converts it to an admin-facing
     * DTO format using {@link ProfileMapper}.
     * </p>
     *
     * @param userId ID of the MO user
     * @return MOProDTO containing profile information, or null if not found
     */
    public MOProDTO getMOProfile(String userId) {
        try {
            List<MOProfile> moProfiles = moProfileRepo.loadAllEntities();
            MOProfile profile = moProfiles.stream()
                    .filter(mp -> mp.getUserId().equals(userId))
                    .findFirst()
                    .orElse(null);

            if (profile == null) {
                log.warn("MO profile not found, userId: {}", userId);
                return null;
            }

            return ProfileMapper.INSTANCE.toMOProfileDTO(profile);
        } catch (IOException e) {
            log.error("get MO profile failed, userId: {}, error: {}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * Updates an MO's profile information.
     * <p>
     * This method saves the updated MO profile to the repository. The profile object
     * should contain all fields that need to be updated.
     * </p>
     *
     * @param moProfile Updated MO profile object
     * @return true if update succeeded, false if IO error occurred
     */
    public boolean updateMOProfile(MOProfile moProfile) {
        try {
            moProfileRepo.saveEntity(moProfile);
            log.info("update moProfile success, moProfileId: {}", moProfile.getId());
            return true;
        } catch (IOException e) {
            log.error("update moProfile failed, moProfileId: {}, error message: {}", moProfile.getId(), e.getMessage());
            return false;
        }
    }

    /**
     * Updates a user's basic account information (name and password).
     * <p>
     * This method performs a partial update, only modifying fields that are non-null
     * and non-empty in the provided user object. The updateAt timestamp is automatically
     * set to the current time.
     * </p>
     * <p>
     * <b>Updated Fields:</b>
     * <ul>
     *   <li>Name: Updated if non-null and non-empty</li>
     *   <li>Password: Updated if non-null and non-empty (should be pre-encrypted)</li>
     *   <li>UpdateAt: Always set to current timestamp</li>
     * </ul>
     * </p>
     * <p>
     * <b>Note:</b> This method does NOT re-encrypt the password. If updating password,
     * ensure it's already MD5-hashed before calling this method.
     * </p>
     *
     * @param updatedUser User object containing fields to update
     * @return true if update succeeded, false if user not found or IO error occurred
     */
    public boolean updateUser(User updatedUser) {
        try {
            User existingUser = userRepo.getEntityById(updatedUser.getId());
            if (existingUser == null) {
                log.warn("User not found for update, userId: {}", updatedUser.getId());
                return false;
            }

            if (updatedUser.getName() != null && !updatedUser.getName().trim().isEmpty()) {
                existingUser.setName(updatedUser.getName());
            }

            if (updatedUser.getPassword() != null && !updatedUser.getPassword().trim().isEmpty()) {
                existingUser.setPassword(updatedUser.getPassword());
            }

            existingUser.setUpdateAt(Timestamp.valueOf(LocalDateTime.now()));

            userRepo.saveEntity(existingUser);
            log.info("update user success, userId: {}", existingUser.getId());
            return true;
        } catch (IOException e) {
            log.error("update user failed, userId: {}, error message: {}", updatedUser.getId(), e.getMessage());
            return false;
        }
    }

    /**
     * Creates a new MO account with associated profile.
     * <p>
     * This method performs atomic creation of both user account and profile:
     * <ol>
     *   <li>Sets user role to 2 (MO)</li>
     *   <li>Links profile to user via userId</li>
     *   <li>Saves user account</li>
     *   <li>Saves MO profile</li>
     * </ol>
     * </p>
     * <p>
     * <b>Note:</b> If saving the profile fails after user is saved, the user record
     * will remain in the database. Consider implementing transaction rollback for
     * production use.
     * </p>
     *
     * @param mo        User object for the new MO account (password should be pre-encrypted)
     * @param moProfile MOProfile object with contact information
     * @return true if both saves succeeded, false if IO error occurred
     */
    public boolean createMOAccount(User mo, MOProfile moProfile) {
        try {
            mo.setRole(2);
            moProfile.setUserId(mo.getId());
            userRepo.saveEntity(mo);
            moProfileRepo.saveEntity(moProfile);
            log.info("create moAccount success, moId: {}, moProfileId: {}", mo.getId(), moProfile.getId());
        } catch (IOException e) {
            log.error("create moAccount failed, moId: {}, moProfileId: {}, error message: {}", mo.getId(), moProfile.getId(), e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Retrieves a paginated list of user accounts by role (basic version without filtering/sorting).
     * <p>
     * This method returns a single page of users with the specified role, excluding a
     * specific user ID (typically the currently logged-in admin).
     * </p>
     * <p>
     * <b>Pagination:</b>
     * <ul>
     *   <li>Page size: Fixed at 10 records per page</li>
     *   <li>Page numbering: 1-based (page 1 = first page)</li>
     *   <li>Invalid page numbers (&lt; 1) are clamped to 1</li>
     * </ul>
     * </p>
     *
     * @param role         User role to filter by (1 = TA, 2 = MO)
     * @param page         Page number (1-based)
     * @param excludeUserId User ID to exclude from results (typically current admin)
     * @return List of UserDetailDTO for the requested page
     */
    public List<UserDetailDTO> getAccountsByRole(int role, int page, String excludeUserId) {
        try {
            List<User> users = userRepo.loadAllEntities();
            List<TAProfile> taProfiles = taProfileRepo.loadAllEntities();
            List<MOProfile> moProfiles = moProfileRepo.loadAllEntities();

            int pageNum = Math.max(page, 1);
            return users.stream()
                    .filter(u -> u != null && u.getRole() == role && !u.getId().equals(excludeUserId))
                    .skip((long) (pageNum - 1) * pageSize)
                    .limit(pageSize)
                    .map(u -> {
                        String proId;
                        if (role == 1) {
                            proId = taProfiles.stream()
                                    .filter(tp -> tp.getUserId().equals(u.getId()))
                                    .map(TAProfile::getId)
                                    .findFirst()
                                    .orElse(null);
                        } else {
                            proId = moProfiles.stream()
                                    .filter(mp -> mp.getUserId().equals(u.getId()))
                                    .map(MOProfile::getId)
                                    .findFirst()
                                    .orElse(null);
                        }
                        return UserMapper.INSTANCE.toDetailDTO(u, proId);
                    })
                    .toList();
        } catch (IOException e) {
            log.error("get accounts by role failed, role: {}, error: {}", role, e.getMessage());
            return List.of();
        }
    }

    /**
     * Retrieves a paginated list of user accounts by role with filtering and sorting.
     * <p>
     * This enhanced version supports:
     * <ul>
     *   <li><b>Filtering</b>: By availability status (available/unavailable/all)</li>
     *   <li><b>Sorting</b>: By name, createAt, updateAt, or lastLoginAt</li>
     *   <li><b>Pagination</b>: Same as basic version</li>
     * </ul>
     * </p>
     * <p>
     * <b>Filter Options:</b>
     * <ul>
     *   <li>"available" - Only active users (status = 0)</li>
     *   <li>"unavailable" - Only frozen users (status = 1)</li>
     *   <li>"all" - No status filter</li>
     *   <li>Other values - No filter applied (default behavior)</li>
     * </ul>
     * </p>
     * <p>
     * <b>Sort Options:</b>
     * <ul>
     *   <li>"name" - Alphabetical by username (ascending)</li>
     *   <li>"createAt" - By account creation date (descending, newest first)</li>
     *   <li>"updateAt" - By last update date (descending, newest first)</li>
     *   <li>"lastLoginAt" - By last login date (descending, most recent first)</li>
     *   <li>Other values - Default to name sorting</li>
     * </ul>
     * </p>
     * <p>
     * <b>Null Handling:</b> Date comparisons use {@link Comparator#nullsLast(Comparator)}
     * to ensure users with null dates appear at the end of sorted results.
     * </p>
     *
     * @param role         User role to filter by (1 = TA, 2 = MO)
     * @param condition    Query conditions containing filter, order, and page parameters
     * @param excludeUserId User ID to exclude from results
     * @return List of UserDetailDTO for the requested page with applied filters and sorting
     * @see QueryCondition
     */
    public List<UserDetailDTO> getAccountsByRole(int role, QueryCondition condition, String excludeUserId) {
        try {
            List<User> users = userRepo.loadAllEntities();
            List<TAProfile> taProfiles = taProfileRepo.loadAllEntities();
            List<MOProfile> moProfiles = moProfileRepo.loadAllEntities();

            int pageNum = Math.max(condition.getPage(), 1);

            Stream<User> userStream = users.stream()
                    .filter(u -> u != null && u.getRole() == role && !u.getId().equals(excludeUserId));

            userStream = switch (condition.getFilter()) {
                case "available" -> userStream.filter(u -> u.getStatus() == 0);
                case "unavailable" -> userStream.filter(u -> u.getStatus() == 1);
                case "all" -> userStream;
                default -> userStream;
            };

            userStream = switch (condition.getOrder()) {
                case "name" -> userStream.sorted(Comparator.comparing(User::getName));
                case "createAt" -> userStream.sorted(Comparator.comparing(User::getCreateAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
                case "updateAt" -> userStream.sorted(Comparator.comparing(User::getUpdateAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
                case "lastLoginAt" -> userStream.sorted(Comparator.comparing(User::getLastLoginAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
                default -> userStream.sorted(Comparator.comparing(User::getName));
            };
            return userStream.skip((long) (pageNum - 1) * pageSize)
                    .limit(pageSize)
                    .map(u -> {
                        String proId;
                        if (role == 1) {
                            proId = taProfiles.stream()
                                    .filter(tp -> tp.getUserId().equals(u.getId()))
                                    .map(TAProfile::getId)
                                    .findFirst()
                                    .orElse(null);
                        } else {
                            proId = moProfiles.stream()
                                    .filter(mp -> mp.getUserId().equals(u.getId()))
                                    .map(MOProfile::getId)
                                    .findFirst()
                                    .orElse(null);
                        }
                        return UserMapper.INSTANCE.toDetailDTO(u, proId);
                    })
                    .toList();
        } catch (IOException e) {
            log.error("get accounts by role failed, role: {}, error: {}", role, e.getMessage());
            return List.of();
        }
    }

    /**
     * Calculates the total number of pages for user accounts by role (basic version).
     * <p>
     * This method counts users matching the role and exclusion criteria, then calculates
     * the number of pages needed based on the fixed page size.
     * </p>
     * <p>
     * <b>Calculation:</b>
     * <pre>
     * totalPages = (count == 0) ? 0 : ceil(count / pageSize)
     * </pre>
     * </p>
     *
     * @param role         User role to count (1 = TA, 2 = MO)
     * @param excludeUserId User ID to exclude from count
     * @return Total number of pages (0 if no users found)
     */
    public long getAccountPages(int role, String excludeUserId) {
        try {
            List<User> users = userRepo.loadAllEntities();
            long count = users.stream()
                    .filter(u -> u != null && u.getRole() == role && !u.getId().equals(excludeUserId))
                    .count();
            return count == 0 ? 0 : (count % pageSize == 0 ? count / pageSize : count / pageSize + 1);
        } catch (IOException e) {
            log.error("get account pages failed, role: {}, error: {}", role, e.getMessage());
            return 0;
        }
    }

    /**
     * Calculates the total number of pages for user accounts by role with filtering.
     * <p>
     * This enhanced version applies the same status filter as
     * {@link #getAccountsByRole(int, QueryCondition, String)} before counting.
     * </p>
     * <p>
     * <b>Filter Options:</b> Same as getAccountsByRole:
     * <ul>
     *   <li>"available" - Count only active users</li>
     *   <li>"unavailable" - Count only frozen users</li>
     *   <li>"all" or other - Count all users</li>
     * </ul>
     * </p>
     *
     * @param role         User role to count (1 = TA, 2 = MO)
     * @param condition    Query conditions containing filter parameter
     * @param excludeUserId User ID to exclude from count
     * @return Total number of pages after applying filter (0 if no users found)
     * @see QueryCondition
     */
    public long getAccountPages(int role, QueryCondition condition, String excludeUserId) {
        try {
            List<User> users = userRepo.loadAllEntities();

            Stream<User> userStream = users.stream()
                    .filter(u -> u != null && u.getRole() == role && !u.getId().equals(excludeUserId));

            userStream = switch (condition.getFilter()) {
                case "available" -> userStream.filter(u -> u.getStatus() == 0);
                case "unavailable" -> userStream.filter(u -> u.getStatus() == 1);
                case "all" -> userStream;
                default -> userStream;
            };

            long count = userStream.count();
            return count == 0 ? 0 : (count % pageSize == 0 ? count / pageSize : count / pageSize + 1);
        } catch (IOException e) {
            log.error("get account pages failed, role: {}, error: {}", role, e.getMessage());
            return 0;
        }
    }

    /**
     * Encrypts a password using MD5 hashing.
     * <p>
     * This utility method wraps {@link DigestUtils#md5Hex(String)} for consistent
     * password encryption across the application.
     * </p>
     * <p>
     * <b>Usage:</b>
     * <pre>{@code
     * String encrypted = adminService.encryptPassword("plainTextPassword");
     * user.setPassword(encrypted);
     * }</pre>
     * </p>
     *
     * @param password Plain text password to encrypt
     * @return MD5-hashed password string
     * @see DigestUtils#md5Hex(String)
     */
    public String encryptPassword(String password) {
        password = DigestUtils.md5Hex(password);
        return password;
    }

    /**
     * Closes all positions that have passed their application deadline.
     * <p>
     * This scheduled maintenance task runs daily (typically at midnight via
     * {@link com.tars.listener.ScheduledTaskListener}) to automatically close
     * expired positions.
     * </p>
     * <p>
     * <b>Closure Criteria:</b>
     * <ul>
     *   <li>Position status must be 0 (opened)</li>
     *   <li>Deadline must be set and before current time</li>
     * </ul>
     * </p>
     * <p>
     * <b>Actions Taken:</b>
     * <ul>
     *   <li>Set position status to 2 (closed)</li>
     *   <li>Update position's updateAt timestamp</li>
     *   <li>Save all modified positions in batch</li>
     * </ul>
     * </p>
     * <p>
     * <b>Logging:</b> Logs the number of positions closed for monitoring purposes.
     * </p>
     *
     * @return true if operation completed (even if no positions closed), false if IO error occurred
     * @see com.tars.listener.ScheduledTaskListener
     */
    public boolean closePositions() {
        try {
            List<Position> positions = posRepo.loadAllEntities();
            LocalDateTime now = LocalDateTime.now();
            int closedCount = 0;

            for (Position position : positions) {
                if (position.getStatus() == 0 && position.getDeadline() != null) {
                    LocalDateTime deadline = position.getDeadline().toLocalDateTime();
                    if (deadline.isBefore(now)) {
                        position.setStatus(2);
                        position.setUpdateAt(Timestamp.valueOf(now));
                        closedCount++;
                    }
                }
            }

            if (closedCount > 0) {
                posRepo.saveAllEntities(positions);
                log.info("Closed {} expired positions", closedCount);
            }

            return true;
        } catch (IOException e) {
            log.error("Failed to close expired positions, error: {}", e.getMessage());
            return false;
        }
    }
}
