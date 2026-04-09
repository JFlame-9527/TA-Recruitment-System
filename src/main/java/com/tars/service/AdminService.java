package com.tars.service;

import com.tars.entity.bean.Application;
import com.tars.entity.bean.MOProfile;
import com.tars.entity.bean.Position;
import com.tars.entity.bean.TAProfile;
import com.tars.entity.bean.User;
import com.tars.entity.dto.admin.MOProDTO;
import com.tars.entity.dto.admin.TAProDTO;
import com.tars.entity.dto.admin.UserDetailDTO;
import com.tars.mapper.ProfileMapper;
import com.tars.mapper.UserMapper;
import com.tars.repository.JsonRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Yue Wang
 * @version 1.0.0
 * @since 2026/3/24
 */
@Slf4j
public class AdminService {
    private final JsonRepository<User> userRepo = new JsonRepository<>(User.class);

    private final JsonRepository<TAProfile> taProfileRepo = new JsonRepository<>(TAProfile.class);

    private final JsonRepository<MOProfile> moProfileRepo = new JsonRepository<>(MOProfile.class);

    private final JsonRepository<Application> appRepo = new JsonRepository<>(Application.class);

    private final JsonRepository<Position> posRepo = new JsonRepository<>(Position.class);

    private static final int pageSize = 10;

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

    public boolean updateUser(User user) {
        try {
            userRepo.saveEntity(user);
            log.info("update user success, userId: {}", user.getId());
        } catch (IOException e) {
            log.error("update user failed, userId: {}, error message: {}", user.getId(), e.getMessage());
            return false;
        }
        return true;
    }

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
                        String proId = null;
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
}
