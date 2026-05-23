package com.tars.service;

import com.tars.ai.PortraitGenerator;
import com.tars.ai.PortraitMatcher;
import com.tars.ai.SkillExtractor;
import com.tars.entity.bean.Application;
import com.tars.entity.bean.Portrait;
import com.tars.entity.bean.Position;
import com.tars.entity.bean.TAProfile;
import com.tars.entity.QueryCondition;
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
 * Service layer for Technical Assistant (TA) operations in the recruitment system.
 * <p>
 * This service provides comprehensive functionality for TA candidates, including:
 * <ul>
 *   <li><b>Profile Management</b>: Create, update, and retrieve TA profiles with AI-powered resume parsing</li>
 *   <li><b>Application Management</b>: Apply for positions, withdraw applications, track application status</li>
 *   <li><b>Position Browsing</b>: Search, filter, sort, and view available positions with AI-based recommendations</li>
 *   <li><b>Skill Extraction</b>: Extract technical skills from PDF resumes using Qwen AI</li>
 * </ul>
 * </p>
 * <p>
 * <b>AI Integration:</b>
 * <ul>
 *   <li>{@link PortraitGenerator}: Generates vector embeddings from resumes and job descriptions</li>
 *   <li>{@link PortraitMatcher}: Calculates match scores between TA portraits and position portraits</li>
 *   <li>{@link SkillExtractor}: Extracts technical skills from PDF resumes</li>
 * </ul>
 * </p>
 * <p>
 * <b>Pagination:</b>
 * <ul>
 *   <li>Application history: 9 records per page</li>
 *   <li>Position listings: 10 records per page</li>
 * </ul>
 * </p>
 * <p>
 * <b>Recommendation System:</b> Positions can be sorted by AI-calculated match scores based on:
 * <ul>
 *   <li>Skills compatibility (technical skills vector similarity)</li>
 *   <li>Experience relevance (work experience vector similarity)</li>
 *   <li>Soft skills alignment (behavioral competencies vector similarity)</li>
 * </ul>
 * </p>
 *
 * @author QiheSun Xiri04
 * @version 4.0.0
 * @since 2026/5/12
 * @see PortraitGenerator
 * @see PortraitMatcher
 * @see SkillExtractor
 */
@Slf4j
public class TAService {

    private final JsonRepository<TAProfile> taProfileRepo = new JsonRepository<>(TAProfile.class);

    private final JsonRepository<Application> applicationRepo = new JsonRepository<>(Application.class);

    private final JsonRepository<Position> positionRepo = new JsonRepository<>(Position.class);

    private final JsonRepository<Portrait> portraitRepo = new JsonRepository<>(Portrait.class);

    private final PortraitGenerator portraitGenerator = new PortraitGenerator();

    private final SkillExtractor skillExtractor = new SkillExtractor();

    /** Page size for application history queries (9 records per page) */
    private final int applyPageSize = 9;

    /** Page size for position listing queries (10 records per page) */
    private final int posPageSize = 10;

    /**
     * Creates a new TA profile with AI-generated portrait from resume.
     * <p>
     * This method performs the following steps:
     * <ol>
     *   <li>Generates a vectorized portrait from the TA profile and resume using {@link PortraitGenerator}</li>
     *   <li>Links the portrait ID to the TA profile</li>
     *   <li>Saves both the profile and portrait to their respective repositories</li>
     * </ol>
     * </p>
     * <p>
     * <b>Portrait Generation:</b> The portrait contains three vector embeddings:
     * <ul>
     *   <li>Skills vector: Technical capabilities extracted from resume</li>
     *   <li>Experience vector: Work history and achievements</li>
     *   <li>Soft skills vector: Interpersonal and behavioral competencies</li>
     * </ul>
     * </p>
     *
     * @param taProfile TA profile object containing personal and academic information
     * @param resume    Uploaded resume file (PDF format) for AI parsing
     * @return true if creation succeeded, false if IO error occurred
     * @see PortraitGenerator#generatePortrait(TAProfile, Part)
     */
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

    /**
     * Checks if a TA profile exists for the specified user.
     * <p>
     * This method searches through all TA profiles to find one matching the given userId.
     * Used to determine if a user has completed their profile setup.
     * </p>
     *
     * @param userId ID of the user to check
     * @return true if profile exists, false otherwise
     */
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

    /**
     * Retrieves a TA's profile as a DTO for display/editing.
     * <p>
     * This method finds the TA profile by userId and converts it to a TA-facing DTO
     * using {@link ProfileMapper}. The DTO excludes internal fields like userId, grade,
     * and timestamps.
     * </p>
     *
     * @param userId ID of the TA user
     * @return ProfileDTO containing profile information, or null if not found
     * @see ProfileMapper#toTAProfileDTO(TAProfile)
     */
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

    /**
     * Retrieves a TA's complete profile entity.
     * <p>
     * Unlike {@link #getProfileDTO(String)}, this method returns the full TAProfile entity
     * including all fields (userId, grade, portraitId, timestamps, etc.).
     * Used internally for operations that require complete profile data.
     * </p>
     *
     * @param userId ID of the TA user
     * @return TAProfile entity, or null if not found
     */
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
     * Updates an existing TA profile with validation and regenerates portrait.
     * <p>
     * This method validates required fields, verifies profile existence, updates the timestamp,
     * regenerates the AI portrait, and saves both profile and portrait.
     * </p>
     * <p>
     * <b>Validation:</b>
     * <ul>
     *   <li>Name must be non-null and non-empty</li>
     *   <li>UserId must be non-null and non-empty</li>
     *   <li>Profile must exist for the given userId</li>
     * </ul>
     * </p>
     * <p>
     * <b>Update Process:</b>
     * <ol>
     *   <li>Set updateAt timestamp to current time</li>
     *   <li>Regenerate portrait from updated profile and new resume (if provided)</li>
     *   <li>Update portraitId in profile</li>
     *   <li>Save profile and portrait</li>
     * </ol>
     * </p>
     *
     * @param taProfile Updated profile data
     * @param resume    New resume file (can be null to keep existing resume)
     * @return true if update succeeded, false if validation failed or IO error occurred
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

    /**
     * Updates an existing TA profile using a File object instead of Part.
     * <p>
     * This overload is primarily used for testing purposes where Part objects
     * are not readily available. Functionally identical to {@link #updateProfile(TAProfile, Part)}.
     * </p>
     *
     * @param taProfile Updated profile data
     * @param resume    Resume file object
     * @return true if update succeeded, false if validation failed or IO error occurred
     * @see #updateProfile(TAProfile, Part)
     */
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

    /**
     * Creates a new application for a position and increments position statistics.
     * <p>
     * This method saves the application and automatically increments the position's
     * appliedNum counter to track total applications received.
     * </p>
     *
     * @param application Application object with positionId and userId set
     * @return true if creation succeeded, false if IO error occurred
     */
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

    /**
     * Handles re-application after withdrawal by deleting the withdrawn application first.
     * <p>
     * When a TA withdraws an application (status=3) and then wants to re-apply to the same
     * position, this method:
     * <ol>
     *   <li>Finds the existing withdrawn application for this user-position pair</li>
     *   <li>Deletes the withdrawn application from the repository</li>
     *   <li>Creates a new application via {@link #createApplication(Application)}</li>
     * </ol>
     * </p>
     * <p>
     * <b>Use Case:</b> Allows TAs to update their application or reconsider after withdrawal
     * without creating duplicate records.
     * </p>
     *
     * @param application New application to create
     * @return true if operation succeeded, false if IO error occurred
     * @see #createApplication(Application)
     */
    public boolean apply(Application application) {
        try {
            List<Application> applications = applicationRepo.loadAllEntities();
            
            // Find existing withdrawn application for this user and position
            Application withdrawnApp = applications.stream()
                    .filter(app -> app != null 
                            && application.getUserId().equals(app.getUserId())
                            && application.getPositionId().equals(app.getPositionId())
                            && app.getStatus() == 3) // status 3 = withdrawn
                    .findFirst()
                    .orElse(null);
            
            if (withdrawnApp != null) {
                log.info("Found withdrawn application {} for user {} and position {}, deleting it",
                        withdrawnApp.getId(), application.getUserId(), application.getPositionId());
                
                // Delete the withdrawn application from repository
                applicationRepo.deleteEntity(withdrawnApp.getId());
                log.info("Deleted withdrawn application: {}", withdrawnApp.getId());
            }
            
            // Create new application (this will increment appliedNum)
            return createApplication(application);
            
        } catch (IOException e) {
            log.error("reapply after withdraw failed, userId: {}, posId: {}, error message: {}",
                    application.getUserId(), application.getPositionId(), e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves a paginated list of TA's applications with position details.
     * <p>
     * This method combines application data with position information to provide
     * a comprehensive view of the TA's application history.
     * </p>
     * <p>
     * <b>Filtering:</b> Supports filtering by application status:
     * <ul>
     *   <li>"pending" - Applications awaiting review (status=0)</li>
     *   <li>"offered" - Applications with offers extended (status=1)</li>
     *   <li>"rejected" - Declined applications (status=2)</li>
     *   <li>"withdrawn" - Withdrawn applications (status=3)</li>
     *   <li>"all" - No filter (default)</li>
     * </ul>
     * </p>
     * <p>
     * <b>Sorting:</b> Results are sorted by applyAt in descending order (newest first).
     * </p>
     * <p>
     * <b>Pagination:</b> Uses applyPageSize (9 records per page).
     * </p>
     *
     * @param userId    ID of the TA user
     * @param condition Query conditions containing filter and page parameters
     * @return List of AppPosDTO for the requested page
     * @see MultiMapper#toAppPosDTO(Application, Position)
     */
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

    /**
     * Calculates the total number of pages for TA's application history.
     * <p>
     * This method counts applications matching the filter criteria and calculates
     * the number of pages needed based on applyPageSize.
     * </p>
     *
     * @param userId    ID of the TA user
     * @param condition Query conditions containing filter parameter
     * @return Total number of pages (0 if no applications found)
     */
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

    /**
     * Withdraws a TA's application and decrements position statistics.
     * <p>
     * This method sets the application status to 3 (withdrawn) and decrements the
     * position's appliedNum counter. Includes ownership verification to prevent
     * unauthorized withdrawals.
     * </p>
     *
     * @param appId  ID of the application to withdraw
     * @param userId ID of the TA user (for ownership verification)
     */
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
            if (position != null) {
                position.setAppliedNum(position.getAppliedNum() - 1);
                positionRepo.saveEntity(position);
            } else {
                log.warn("Position not found for positionId: {}", application.getPositionId());
            }

            log.info("withdraw application success, appId: {}, userId: {}", appId, userId);
        } catch (IOException e) {
            log.error("withdraw application failed, appId: {}, userId: {}, error message: {}", appId, userId, e.getMessage());
        }
    }

    /**
     * Retrieves a paginated list of available positions with filtering, searching, and sorting.
     * <p>
     * This enhanced method provides comprehensive position browsing capabilities:
     * <ul>
     *   <li><b>Filtering</b>: By position status (opened/closedFilled/all)</li>
     *   <li><b>Searching</b>: By title, moduleName, or moduleCode (case-insensitive)</li>
     *   <li><b>Sorting</b>: Multiple options including AI-based recommendation</li>
     *   <li><b>Application Status</b>: Shows whether TA has applied and current status</li>
     * </ul>
     * </p>
     * <p>
     * <b>Sort Options:</b>
     * <ul>
     *   <li>"postDate" - By posting date (descending, newest first)</li>
     *   <li>"deadline" - By application deadline (ascending, earliest first)</li>
     *   <li>"vacancy" - By remaining vacancies (descending, most openings first)</li>
     *   <li>"workload" - By total workload (weeklyWorkload × duration, ascending)</li>
     *   <li>"recommend" - By AI-calculated match score (descending, best match first)</li>
     * </ul>
     * </p>
     * <p>
     * <b>Recommendation Algorithm:</b> When sorting by "recommend":
     * <ol>
     *   <li>Retrieves TA's portrait from repository</li>
     *   <li>For each position, retrieves position's portrait</li>
     *   <li>Calculates match score using {@link PortraitMatcher#calculateMatchScore(Portrait, Portrait)}</li>
     *   <li>Sorts positions by score in descending order</li>
     * </ol>
     * If TA has no portrait, falls back to default ordering.
     * </p>
     * <p>
     * <b>Exclusions:</b> Withdrawn positions (status=3) are excluded from results.
     * </p>
     *
     * @param userId    ID of the TA user (for application status tracking)
     * @param condition Query conditions containing filter, search, key, order, and page parameters
     * @return List of PosBriefDTO for the requested page
     * @see PosMapper#toTAPosBriefDTO(Position, Application)
     */
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
                case "title" -> posStream.filter(pos -> pos.getTitle() != null && pos.getTitle().toLowerCase().contains(searchTerm));
                case "moduleName" -> posStream.filter(pos -> pos.getModuleName() != null && pos.getModuleName().toLowerCase().contains(searchTerm));
                case "moduleCode" -> posStream.filter(pos -> pos.getModuleCode() != null && pos.getModuleCode().toLowerCase().contains(searchTerm));
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
     * Calculates AI-based recommendation score for a position.
     * <p>
     * This helper method retrieves the position's portrait and calculates the match
     * score against the TA's portrait using {@link PortraitMatcher}.
     * </p>
     *
     * @param position   Position to score
     * @param taPortrait TA's portrait for comparison
     * @param matcher    PortraitMatcher instance for score calculation
     * @return Match score (0.0 to 1.0), or 0.0 if position has no portrait
     * @see PortraitMatcher#calculateMatchScore(Portrait, Portrait)
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
     * Retrieves a portrait by its ID.
     * <p>
     * Helper method for fetching portraits from the repository with error handling.
     * </p>
     *
     * @param portraitId ID of the portrait to retrieve
     * @return Portrait entity, or null if not found or IO error occurred
     */
    private Portrait getPortraitById(String portraitId) {
        try {
            return portraitRepo.getEntityById(portraitId);
        } catch (IOException e) {
            log.error("Failed to get portrait by id: {}", portraitId, e);
            return null;
        }
    }

    /**
     * Calculates the total number of pages for position listings with filtering and searching.
     * <p>
     * This method applies the same filters and search criteria as {@link #getPositionList(String, QueryCondition)}
     * before counting, ensuring accurate pagination.
     * </p>
     *
     * @param condition Query conditions containing filter, search, and key parameters
     * @return Total number of pages after applying filters
     */
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
                case "title" -> posStream.filter(pos -> pos.getTitle() != null && pos.getTitle().toLowerCase().contains(searchTerm));
                case "moduleName" -> posStream.filter(pos -> pos.getModuleName() != null && pos.getModuleName().toLowerCase().contains(searchTerm));
                case "moduleCode" -> posStream.filter(pos -> pos.getModuleCode() != null && pos.getModuleCode().toLowerCase().contains(searchTerm));
                default -> posStream;
            };

            long positionCount = posStream.count();

            return positionCount % posPageSize == 0 ? positionCount / posPageSize : positionCount / posPageSize + 1;
        } catch (IOException e) {
            log.error("get position pages failed, error message: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Calculates the total number of pages for all positions (no filtering).
     * <p>
     * Simple version that counts all non-withdrawn positions without any filters.
     * </p>
     *
     * @return Total number of pages for all positions
     */
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

    /**
     * Retrieves detailed information for a specific position.
     * <p>
     * This method combines position details with application information (if the TA has applied)
     * to provide a complete view including feedback and application timeline.
     * </p>
     *
     * @param posId ID of the position to retrieve
     * @param appId ID of the application (null or invalid if not applied)
     * @return PosDetailDTO with position and application details, or null if position not found
     * @see PosMapper#toTAPosDetailDTO(Position, Application)
     */
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

    /**
     * Verifies if a position is available for application by a specific user.
     * <p>
     * This method checks two conditions:
     * <ol>
     *   <li>Position exists and has status=0 (opened)</li>
     *   <li>User has no active applications for this position (excluding withdrawn applications)</li>
     * </ol>
     * </p>
     * <p>
     * <b>Use Case:</b> Prevents duplicate applications and ensures only open positions can be applied to.
     * </p>
     *
     * @param posId  ID of the position to verify
     * @param userId ID of the TA user
     * @return true if position is available for application, false otherwise
     */
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
     * Verifies if a TA has completed their profile before allowing application.
     * <p>
     * This method ensures that TAs cannot apply for positions without first creating
     * their profile (which includes resume upload and AI portrait generation).
     * </p>
     *
     * @param userId ID of the TA user to check
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

    /**
     * Extracts technical skills from a PDF resume using AI.
     * <p>
     * This method uses {@link SkillExtractor} to parse the resume and identify technical
     * skills mentioned in the document.
     * </p>
     * <p>
     * <b>Validation:</b>
     * <ul>
     *   <li>Resume part must be non-null and non-empty</li>
     *   <li>File must have .pdf extension</li>
     * </ul>
     * </p>
     * <p>
     * <b>Use Case:</b> Allows TAs to preview extracted skills before submitting their profile,
     * or for debugging skill extraction accuracy.
     * </p>
     *
     * @param resumePart Uploaded resume file (PDF format)
     * @return List of extracted skill strings
     * @throws IllegalArgumentException if resume is null, empty, or not PDF
     * @throws RuntimeException         if skill extraction fails
     * @see SkillExtractor#extract(Part)
     */
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
