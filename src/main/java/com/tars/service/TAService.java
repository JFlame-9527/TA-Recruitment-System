package com.tars.service;

import com.tars.ai.PortraitGenerator;
import com.tars.ai.PortraitMatcher;
import com.tars.ai.SkillExtractor;
import com.tars.entity.bean.Application;
import com.tars.entity.bean.Portrait;
import com.tars.entity.bean.Position;
import com.tars.entity.bean.TAProfile;
import com.tars.entity.dto.QueryCondition;
import com.tars.entity.dto.ta.AppPosDTO;
import com.tars.entity.dto.ta.PosBriefDTO;
import com.tars.entity.dto.ta.PosDetailDTO;
import com.tars.entity.dto.ta.ProfileDTO;
import com.tars.mapper.MultiMapper;
import com.tars.mapper.PosMapper;
import com.tars.mapper.ProfileMapper;
import com.tars.repository.JsonRepository;
import jakarta.servlet.http.Part;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * @author QiheSun Xiri04
 * @version 4.0.0
 * @since 2026/5/12
 */
@Slf4j
public class TAService {

    private final JsonRepository<TAProfile> taProfileRepo = new JsonRepository<>(TAProfile.class);

    private final JsonRepository<Application> applicationRepo = new JsonRepository<>(Application.class);

    private final JsonRepository<Position> positionRepo = new JsonRepository<>(Position.class);

    private final JsonRepository<Portrait> portraitRepo = new JsonRepository<>(Portrait.class);

    private final PortraitGenerator portraitGenerator = new PortraitGenerator();

    private final SkillExtractor skillExtractor = new SkillExtractor();

    private final int applyPageSize = 9;

    private final int posPageSize = 10;

    public boolean createProfile(TAProfile taProfile, Part resume) {
        try {
            Portrait portrait = portraitGenerator.generatePortrait(taProfile, resume);
            taProfile.setPortraitId(portrait.getId());

            taProfileRepo.saveEntity(taProfile);
            portraitRepo.saveEntity(portrait);

            log.info("create ta Profile success, profileId: {}", taProfile.getId());
        } catch (IOException e) {
            log.error("create ta Profile failed, profileId: {}, error message: {}", taProfile.getId(), e.getMessage());
            return false;
        }
        return true;
    }

    public boolean checkProfileExist(String userId) {
        try {
            List<TAProfile> taProfiles = taProfileRepo.loadAllEntities();
            for (TAProfile taProfile : taProfiles) {
                if (taProfile != null && userId.equals(taProfile.getUserId())) {
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            log.error("check taProfile exists failed, userId: {}, error message: {}", userId, e.getMessage());
            return false;
        }
    }

    public ProfileDTO getProfileDTO(String userId) {
        try {
            List<TAProfile> taProfiles = taProfileRepo.loadAllEntities();
            for (TAProfile taProfile : taProfiles) {
                if (taProfile != null && userId.equals(taProfile.getUserId())) {
                    return ProfileMapper.INSTANCE.toTAProfileDTO(taProfile);
                }
            }
            return null;
        } catch (IOException e) {
            log.error("get taProfile failed, userId: {}, error message: {}", userId, e.getMessage());
            return null;
        }
    }

    public TAProfile getProfile(String userId) {
        try {
            List<TAProfile> taProfiles = taProfileRepo.loadAllEntities();
            for (TAProfile taProfile : taProfiles) {
                if (taProfile != null && userId.equals(taProfile.getUserId())) {
                    return taProfile;
                }
            }
            return null;
        } catch (IOException e) {
            log.error("get taProfile failed, userId: {}, error message: {}", userId, e.getMessage());
            return null;
        }
    }


    /**
     * Update profile with validation
     *
     * @param taProfile Profile to update
     * @return true if update successful, false otherwise
     */
    public boolean updateProfile(TAProfile taProfile, Part resume) {
        // Validate required fields
        if (taProfile.getName() == null || taProfile.getName().trim().isEmpty()) {
            log.warn("Profile name is required");
            return false;
        }

        if (taProfile.getUserId() == null || taProfile.getUserId().trim().isEmpty()) {
            log.warn("User ID is required");
            return false;
        }

        // Verify profile exists for this user
        if (!checkProfileExist(taProfile.getUserId())) {
            log.warn("Profile not found for user: {}", taProfile.getUserId());
            return false;
        }

        try {
            taProfile.setUpdateAt(Timestamp.valueOf(LocalDateTime.now()));

            Portrait portrait = portraitGenerator.generatePortrait(taProfile, resume);
            taProfile.setPortraitId(portrait.getId());

            taProfileRepo.saveEntity(taProfile);
            portraitRepo.saveEntity(portrait);

            log.info("update taProfile success, taProfileId: {}", taProfile.getId());
        } catch (IOException e) {
            log.error("update taProfile failed, taProfileId: {}, error message: {}",
                    taProfile.getId(), e.getMessage(), e);
            return false;
        }
        return true;
    }

    public boolean updateProfile(TAProfile taProfile, File resume) {
        // Validate required fields
        if (taProfile.getName() == null || taProfile.getName().trim().isEmpty()) {
            log.warn("Profile name is required");
            return false;
        }

        if (taProfile.getUserId() == null || taProfile.getUserId().trim().isEmpty()) {
            log.warn("User ID is required");
            return false;
        }

        // Verify profile exists for this user
        if (!checkProfileExist(taProfile.getUserId())) {
            log.warn("Profile not found for user: {}", taProfile.getUserId());
            return false;
        }

        try {
            taProfile.setUpdateAt(Timestamp.valueOf(LocalDateTime.now()));

            Portrait portrait = portraitGenerator.generatePortrait(taProfile, resume);
            taProfile.setPortraitId(portrait.getId());

            taProfileRepo.saveEntity(taProfile);
            portraitRepo.saveEntity(portrait);

            log.info("update taProfile success, taProfileId: {}", taProfile.getId());
        } catch (IOException e) {
            log.error("update taProfile failed, taProfileId: {}, error message: {}",
                    taProfile.getId(), e.getMessage(), e);
            return false;
        }
        return true;
    }

    public boolean createApplication(Application application) {
        try {
            applicationRepo.saveEntity(application);
            Position position = positionRepo.getEntityById(application.getPositionId());
            position.setAppliedNum(position.getAppliedNum() + 1);
            positionRepo.saveEntity(position);
            log.info("create application success, applicationId: {}", application.getId());
        } catch (IOException e) {
            log.error("create application failed, applicationId: {}, error message: {}", application.getId(),
                    e.getMessage());
            return false;
        }
        return true;
    }

    public List<AppPosDTO> getAppPosList(String userId, QueryCondition condition) {
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("userId is null or empty");
            return List.of();
        }

        try {
            List<Application> applications = applicationRepo.loadAllEntities();
            List<Position> positions = positionRepo.loadAllEntities();

            if (applications == null || applications.isEmpty()) {
                return List.of();
            }

            Map<String, Position> positionMap = positions.stream()
                    .filter(pos -> pos != null && pos.getId() != null)
                    .collect(java.util.stream.Collectors.toMap(
                            Position::getId,
                            pos -> pos,
                            (existing, replacement) -> existing
                    ));

            int pageNum = Math.max(condition.getPage(), 1);

            Stream<AppPosDTO> appPosStream = applications.stream()
                    .filter(app -> app != null && userId.equals(app.getUserId()))
                    .map(application -> {
                        Position position = positionMap.get(application.getPositionId());
                        if (position == null) {
                            log.debug("Position not found for positionId: {}", application.getPositionId());
                            return null;
                        }
                        return MultiMapper.INSTANCE.toAppPosDTO(application, position);
                    });

            switch (condition.getFilter()) {
                case "pending":
                    appPosStream = appPosStream.filter(dto -> dto.getStatus() == 0);
                    break;
                case "offered":
                    appPosStream = appPosStream.filter(dto -> dto.getStatus() == 1);
                    break;
                case "rejected":
                    appPosStream = appPosStream.filter(dto -> dto.getStatus() == 2);
                    break;
                case "withdrawn":
                    appPosStream = appPosStream.filter(dto -> dto.getStatus() == 3);
                    break;
                case "all":
                default:
                    break;

            }

            List<AppPosDTO> appPosList = appPosStream.sorted(Comparator.comparing(AppPosDTO::getApplyAt).reversed())
                    .skip((long) (pageNum - 1) * applyPageSize)
                    .limit(applyPageSize)
                    .toList();

            return appPosList;
        } catch (IOException e) {
            log.error("get appPosList failed, userId: {}, error message: {}", userId, e.getMessage());
            return List.of();
        }
    }

    public long getAppPosPages(String userId, QueryCondition condition) {
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("userId is null or empty");
            return 0;
        }

        try {
            List<Application> applications = applicationRepo.loadAllEntities();

            if (applications == null || applications.isEmpty()) {
                return 0;
            }

            Stream<Application> appStream = applications.stream()
                    .filter(app -> app != null && userId.equals(app.getUserId()));

            switch (condition.getFilter()) {
                case "applied":
                    appStream = appStream.filter(app -> app.getStatus() == 0);
                    break;
                case "offered":
                    appStream = appStream.filter(app -> app.getStatus() == 1);
                    break;
                case "rejected":
                    appStream = appStream.filter(app -> app.getStatus() == 2);
                    break;
                case "withdrawn":
                    appStream = appStream.filter(app -> app.getStatus() == 3);
                    break;
                case "all":
                default:
                    break;
            }
            long appPosCount = appStream.count();
            return appPosCount % applyPageSize == 0 ? appPosCount / applyPageSize : appPosCount / applyPageSize + 1;
        } catch (IOException e) {
            log.error("get appPosCount failed, userId: {}, error message: {}", userId, e.getMessage());
            return 0;
        }
    }

    public void withdrawApplication(String appId, String userId) {
        try {
            Application application = applicationRepo.getEntityById(appId);
            if (application == null || !userId.equals(application.getUserId())) {
                log.warn("application not found or userId not match, appId: {}, userId: {}", appId, userId);
                return;
            }
            application.setStatus(3);
            applicationRepo.saveEntity(application);

            Position position = positionRepo.getEntityById(application.getPositionId());
            position.setAppliedNum(position.getAppliedNum() - 1);
            positionRepo.saveEntity(position);

            log.info("withdraw application success, appId: {}, userId: {}", appId, userId);
        } catch (IOException e) {
            log.error("withdraw application failed, appId: {}, userId: {}, error message: {}", appId, userId, e.getMessage());
        }
    }

    public List<PosBriefDTO> getPositionList(String userId, QueryCondition condition) {
        try {
            List<Position> positions = positionRepo.loadAllEntities();
            List<Application> applications = applicationRepo.loadAllEntities();

            Map<String, Application> posAppMap = applications.stream()
                    .filter(app -> app != null && userId.equals(app.getUserId()))
                    .collect(java.util.stream.Collectors.toMap(
                            Application::getPositionId,
                            app -> app,
                            (existing, replacement) -> existing
                    ));

            int pageNum = Math.max(condition.getPage(), 1);

            Stream<Position> posStream = positions.stream()
                    .filter(pos -> pos != null && pos.getStatus() != 3);

            posStream = switch (condition.getFilter()) {
                case "opened" -> posStream.filter(pos -> pos.getStatus() == 0);
                case "closedFilled" -> posStream.filter(pos -> pos.getStatus() == 1 || pos.getStatus() == 2);
                case "all" -> posStream;
                default -> posStream;
            };

            String search = condition.getSearch();
            if (search != null && !search.isEmpty()) {
                search = search.toLowerCase();
            }
            String searchTerm = search;
            posStream = switch (condition.getKey()) {
                case "title" -> posStream.filter(pos -> pos.getTitle().toLowerCase().contains(searchTerm));
                case "moduleName" -> posStream.filter(pos -> pos.getModuleName().toLowerCase().contains(searchTerm));
                case "moduleCode" -> posStream.filter(pos -> pos.getModuleCode().toLowerCase().contains(searchTerm));
                default -> posStream;
            };

            posStream = switch (condition.getOrder()) {
                case "postDate" -> posStream.sorted(Comparator.comparing(Position::getPostDate).reversed());
                case "deadline" -> posStream.sorted(Comparator.comparing(Position::getDeadline));
                case "vacancy" ->
                        posStream.sorted(Comparator.comparing((Position pos) -> pos.getRequiredNum() - pos.getOfferedNum()).reversed());
                case "workload" ->
                        posStream.sorted(Comparator.comparing((Position pos) -> pos.getWeeklyWorkload() * pos.getDuration()));
                case "recommend" -> {
                    // Get TA profile and portrait
                    TAProfile taProfile = getProfile(userId);
                    if (taProfile == null || taProfile.getPortraitId() == null) {
                        log.warn("TA profile or portrait not found for user: {}, using default order", userId);
                        yield posStream;
                    }

                    Portrait taPortrait = getPortraitById(taProfile.getPortraitId());
                    if (taPortrait == null) {
                        log.warn("TA portrait not found for portraitId: {}, using default order", taProfile.getPortraitId());
                        yield posStream;
                    }

                    PortraitMatcher matcher = new PortraitMatcher();
                    
                    // Sort positions by recommendation score
                    yield posStream.sorted((pos1, pos2) -> {
                        double score1 = calculatePositionScore(pos1, taPortrait, matcher);
                        double score2 = calculatePositionScore(pos2, taPortrait, matcher);
                        return Double.compare(score2, score1); // Descending order
                    });
                }
                default -> posStream;
            };

            return posStream.skip((long) (pageNum - 1) * posPageSize)
                    .limit(posPageSize)
                    .map(pos -> {
                        Application app = posAppMap.get(pos.getId());
                        if (app == null) {
                            app = new Application(-1);
                        }
                        return PosMapper.INSTANCE.toTAPosBriefDTO(pos, app);
                    })
                    .filter(Objects::nonNull)
                    .toList();

        } catch (IOException e) {
            log.error("get positions failed, error message: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Calculate recommendation score for a position
     */
    private double calculatePositionScore(Position position, Portrait taPortrait, PortraitMatcher matcher) {
        if (position.getPortraitId() == null) {
            log.debug("Position {} has no portrait, using default score 0", position.getId());
            return 0.0;
        }

        Portrait posPortrait = getPortraitById(position.getPortraitId());
        if (posPortrait == null) {
            log.debug("Portrait not found for position {}, using default score 0", position.getId());
            return 0.0;
        }

        return matcher.calculateMatchScore(taPortrait, posPortrait);
    }

    /**
     * Get portrait by ID
     */
    private Portrait getPortraitById(String portraitId) {
        try {
            return portraitRepo.getEntityById(portraitId);
        } catch (IOException e) {
            log.error("Failed to get portrait by id: {}", portraitId, e);
            return null;
        }
    }

    public long getPositionPages(QueryCondition condition) {
        try {
            List<Position> positions = positionRepo.loadAllEntities();

            if (positions == null || positions.isEmpty()) {
                return 0;
            }

            Stream<Position> posStream = positions.stream()
                    .filter(pos -> pos != null && pos.getStatus() != 3);

            posStream = switch (condition.getFilter()) {
                case "opened" -> posStream.filter(pos -> pos.getStatus() == 0);
                case "closedFilled" -> posStream.filter(pos -> pos.getStatus() == 1 || pos.getStatus() == 2);
                case "all" -> posStream;
                default -> posStream;
            };

            String search = condition.getSearch();
            if (search != null && !search.isEmpty()) {
                search = search.toLowerCase();
            }
            String searchTerm = search;
            posStream = switch (condition.getKey()) {
                case "title" -> posStream.filter(pos -> pos.getTitle().toLowerCase().contains(searchTerm));
                case "moduleName" -> posStream.filter(pos -> pos.getModuleName().toLowerCase().contains(searchTerm));
                case "moduleCode" -> posStream.filter(pos -> pos.getModuleCode().toLowerCase().contains(searchTerm));
                default -> posStream;
            };

            long positionCount = posStream.count();

            return positionCount % posPageSize == 0 ? positionCount / posPageSize : positionCount / posPageSize + 1;
        } catch (IOException e) {
            log.error("get position pages failed, error message: {}", e.getMessage());
            return 0;
        }
    }

    public long getPositionPages() {
        try {
            List<Position> positions = positionRepo.loadAllEntities();

            if (positions == null || positions.isEmpty()) {
                return 0;
            }

            return positions.size() % posPageSize == 0 ? positions.size() / posPageSize : positions.size() / posPageSize + 1;
        } catch (IOException e) {
            log.error("get position pages failed, error message: {}", e.getMessage());
            return 0;
        }
    }

    public PosDetailDTO getPosition(String posId, String appId) {
        try {
            Position pos = positionRepo.getEntityById(posId);
            Application app = applicationRepo.getEntityById(appId);
            app = app != null ? app : new Application(-1);
            return PosMapper.INSTANCE.toTAPosDetailDTO(pos, app);
        } catch (IOException e) {
            log.error("get position failed, error message: {}", e.getMessage());
            return null;
        }
    }

    public boolean verifyPosAvailable(String posId, String userId) {
        try {
            Position pos = positionRepo.getEntityById(posId);
            if (pos == null) {
                log.warn("position not found, posId: {}", posId);
                return false;
            }

            if (pos.getStatus() != 0) return false;

            List<Application> applications = applicationRepo.loadAllEntities();
            if (applications == null || applications.isEmpty()) {
                return true;
            }

            long count = applications.stream()
                    .filter(app -> app != null && app.getPositionId().equals(posId) && app.getUserId().equals(userId))
                    .filter(app -> app.getStatus() != 3)
                    .count();
            return count == 0;
        } catch (IOException e) {
            log.error("verify position avaliable failed, posId: {}, userId: {}, error message: {}", posId, userId, e.getMessage());
            return false;
        }
    }

    /**
     * Verify if user has completed profile before applying
     *
     * @param userId User ID to check
     * @return true if profile exists, false otherwise
     */
    public boolean verifyProfileExists(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("userId is null or empty");
            return false;
        }

        try {
            List<TAProfile> taProfiles = taProfileRepo.loadAllEntities();
            if (taProfiles == null || taProfiles.isEmpty()) {
                return false;
            }

            boolean exists = taProfiles.stream()
                    .anyMatch(profile -> profile != null && userId.equals(profile.getUserId()));

            if (!exists) {
                log.warn("Profile not found for userId: {}", userId);
            }

            return exists;
        } catch (IOException e) {
            log.error("verify profile exists failed, userId: {}, error message: {}", userId, e.getMessage());
            return false;
        }
    }

    public List<String> extractSkills(Part resumePart) {
        if (resumePart == null || resumePart.getSize() == 0) {
            log.warn("No resume file provided for skill extraction");
            throw new IllegalArgumentException("No resume file provided");
        }

        String fileName = resumePart.getSubmittedFileName();
        if (fileName == null || !fileName.toLowerCase().endsWith(".pdf")) {
            log.warn("Invalid file type for skill extraction: {}", fileName);
            throw new IllegalArgumentException("Only PDF files are supported");
        }

        log.info("Starting skill extraction from resume: {}", fileName);

        try {
            List<String> skills = skillExtractor.extract(resumePart);
            log.info("Successfully extracted {} skills from {}", skills.size(), fileName);
            return skills;
        } catch (Exception e) {
            log.error("Failed to extract skills from resume: {}", fileName, e);
            throw new RuntimeException("Failed to extract skills: " + e.getMessage(), e);
        }
    }
}
