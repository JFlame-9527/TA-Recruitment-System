package com.tars.service;

import com.tars.entity.bean.Application;
import com.tars.entity.bean.Position;
import com.tars.entity.bean.TAProfile;
import com.tars.entity.dto.mo.ApplicationDTO;
import com.tars.entity.dto.mo.PosBriefDTO;
import com.tars.entity.dto.mo.PosDetailDTO;
import com.tars.entity.dto.mo.ProfileDTO;
import com.tars.mapper.AppMapper;
import com.tars.mapper.PosMapper;
import com.tars.mapper.ProfileMapper;
import com.tars.repository.JsonRepository;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author 477996850
 * @version 1.0.0
 * @since 2026/3/24
 */
@Slf4j
public class MOService {
    private final JsonRepository<Position> positionRepo = new JsonRepository<>(Position.class);

    private final JsonRepository<Application> applicationRepo = new JsonRepository<>(Application.class);

    private final JsonRepository<TAProfile> taProfileRepo = new JsonRepository<>(TAProfile.class);

    private final int posPageSize = 9;

    private final int appPageSize = 10;

    public boolean createPosition(Position position) {
        try {
            positionRepo.saveEntity(position);
            log.info("create position success, positionId: {}", position.getId());
        } catch (IOException e) {
            log.error("create position failed, positionId: {}, error message: {}", position.getId(), e.getMessage());
            return false;
        }
        return true;
    }

    public boolean updatePosition(Position position) {
        try {
            position.setUpdateAt(Timestamp.valueOf(LocalDateTime.now()));
            positionRepo.saveEntity(position);
            log.info("update position success, positionId: {}", position.getId());
        } catch (IOException e) {
            log.error("update position failed, positionId: {}, error message: {}", position.getId(), e.getMessage());
            return false;
        }
        return true;
    }
    
    public List<PosBriefDTO> getPositionList(String userId, int page) {
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("userId is null or empty");
            return List.of();
        }

        try {
            List<Position> positions = positionRepo.loadAllEntities();

            if (positions == null || positions.isEmpty()) {
                return List.of();
            }

            int pageNum = Math.max(page, 1);

            return positions.stream()
                    .filter(pos -> pos != null && userId.equals(pos.getPostUserId()))
                    .skip((long) (pageNum - 1) * posPageSize)
                    .limit(posPageSize)
                    .map(pos -> PosMapper.INSTANCE.toMOPosBriefDTO(pos))
                    .filter(dto -> dto != null)
                    .toList();

        } catch (IOException e) {
            log.error("get position list failed, userId: {}, error message: {}", userId, e.getMessage());
            return List.of();
        }
    }

    public long getPositionPages(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("userId is null or empty");
            return 0;
        }

        try {
            List<Position> positions = positionRepo.loadAllEntities();

            if (positions == null || positions.isEmpty()) {
                return 0;
            }

            long positionCount = positions.stream()
                    .filter(pos -> pos != null && userId.equals(pos.getPostUserId()))
                    .count();

            return positionCount % posPageSize == 0 ? positionCount / posPageSize : positionCount / posPageSize + 1;
        } catch (IOException e) {
            log.error("get position pages failed, userId: {}, error message: {}", userId, e.getMessage());
            return 0;
        }
    }

    public PosDetailDTO getPosition(String posId) {
        if (posId == null || posId.trim().isEmpty()) {
            log.warn("posId is null or empty");
            return null;
        }

        try {
            Position position = positionRepo.getEntityById(posId);
            if (position == null) {
                log.warn("position not found, posId: {}", posId);
                return null;
            }
            return PosMapper.INSTANCE.toMOPosDetailDTO(position);
        } catch (IOException e) {
            log.error("get position failed, posId: {}, error message: {}", posId, e.getMessage());
            return null;
        }
    }

    public List<ApplicationDTO> getAppList(String posId, int page) {
        if (posId == null || posId.trim().isEmpty()) {
            log.warn("posId is null or empty");
            return List.of();
        }

        try {
            List<Application> applications = applicationRepo.loadAllEntities();
            List<TAProfile> taProfiles = taProfileRepo.loadAllEntities();

            if (applications == null || applications.isEmpty()) {
                return List.of();
            }

            Map<String, TAProfile> profileMap = taProfiles.stream()
                    .filter(profile -> profile != null && profile.getUserId() != null)
                    .collect(java.util.stream.Collectors.toMap(
                            TAProfile::getUserId,
                            profile -> profile,
                            (existing, replacement) -> existing
                    ));

            int pageNum = Math.max(page, 1);

            return applications.stream()
                    .filter(app -> app != null && posId.equals(app.getPositionId()) && app.getStatus() != 3)
                    .map(app -> {
                        TAProfile profile = profileMap.get(app.getUserId());
                        if (profile == null) {
                            log.debug("TAProfile not found for userId: {}", app.getUserId());
                            return null;
                        }
                        return AppMapper.INSTANCE.toAppDTO(app, profile);
                    })
                    .filter(dto -> dto != null)
                    .skip((long) (pageNum - 1) * appPageSize)
                    .limit(appPageSize)
                    .toList();

        } catch (IOException e) {
            log.error("get application list failed, posId: {}, error message: {}", posId, e.getMessage());
            return List.of();
        }
    }

    public long getAppPages(String posId) {
        if (posId == null || posId.trim().isEmpty()) {
            log.warn("posId is null or empty");
            return 0;
        }

        try {
            List<Application> applications = applicationRepo.loadAllEntities();

            if (applications == null || applications.isEmpty()) {
                return 0;
            }

            long appCount = applications.stream()
                    .filter(app -> app != null && posId.equals(app.getPositionId()) && app.getStatus() != 3)
                    .count();

            return appCount % appPageSize == 0 ? appCount / appPageSize : appCount / appPageSize + 1;
        } catch (IOException e) {
            log.error("get application pages failed, posId: {}, error message: {}", posId, e.getMessage());
            return 0;
        }
    }

    public boolean verifyPositionOwner(String posId, String userId) {
        if (posId == null || posId.trim().isEmpty() || userId == null || userId.trim().isEmpty()) {
            log.warn("posId or userId is null or empty");
            return false;
        }

        try {
            Position position = positionRepo.getEntityById(posId);
            if (position == null) {
                log.warn("position not found, posId: {}", posId);
                return false;
            }
            return userId.equals(position.getPostUserId());
        } catch (IOException e) {
            log.error("verify position owner failed, posId: {}, userId: {}, error message: {}", 
                     posId, userId, e.getMessage());
            return false;
        }
    }

    public ProfileDTO getProfile(String profileId, String applicationId) {
        if (profileId == null || profileId.trim().isEmpty()) {
            log.warn("profile is null or empty");
            return null;
        }

        try {
            TAProfile profile = taProfileRepo.getEntityById(profileId);
            if (profile == null) {
                log.warn("profile not found, profileId: {}", profileId);
                return null;
            }
            Application application = applicationRepo.getEntityById(applicationId);
            String feedback = application != null ? application.getFeedback() : "";
            return ProfileMapper.INSTANCE.toMOProfileDTO(profile, feedback);
        } catch (IOException e) {
            log.error("get ta profile failed, profileId: {}, error message: {}", profileId, e.getMessage());
            return null;
        }
    }

    public boolean offerApplication(String appId, String feedback) {
        if (appId == null || appId.trim().isEmpty()) {
            log.warn("appId is null or empty");
            return false;
        }

        try {
            Application application = applicationRepo.getEntityById(appId);
            if (application == null) {
                log.warn("Application not found, appId: {}", appId);
                return false;
            }

            if (application.getStatus() != 0) {
                log.warn("Application status is not 'applied', appId: {}, status: {}", appId, application.getStatus());
                return false;
            }

            application.setStatus(1);
            application.setFeedback(feedback != null ? feedback : "");
            applicationRepo.saveEntity(application);

            Position position = positionRepo.getEntityById(application.getPositionId());
            if (position != null) {
                position.setOfferedNum(position.getOfferedNum() + 1);

                if (position.getOfferedNum() >= position.getRequiredNum()) {
                    position.setStatus(1);
                }

                positionRepo.saveEntity(position);
            }

            log.info("Offer application success, appId: {}", appId);
            return true;
        } catch (IOException e) {
            log.error("Offer application failed, appId: {}, error: {}", appId, e.getMessage());
            return false;
        }
    }

    public boolean rejectApplication(String appId, String feedback) {
        if (appId == null || appId.trim().isEmpty()) {
            log.warn("appId is null or empty");
            return false;
        }

        try {
            Application application = applicationRepo.getEntityById(appId);
            if (application == null) {
                log.warn("Application not found, appId: {}", appId);
                return false;
            }

            if (application.getStatus() != 0) {
                log.warn("Application status is not 'applied', appId: {}, status: {}", appId, application.getStatus());
                return false;
            }

            application.setStatus(2);
            application.setFeedback(feedback != null ? feedback : "");
            applicationRepo.saveEntity(application);

            Position position = positionRepo.getEntityById(application.getPositionId());
            if (position != null) {
                position.setRejectedNum(position.getRejectedNum() + 1);
                positionRepo.saveEntity(position);
            }

            log.info("Reject application success, appId: {}", appId);
            return true;
        } catch (IOException e) {
            log.error("Reject application failed, appId: {}, error: {}", appId, e.getMessage());
            return false;
        }
    }
}
