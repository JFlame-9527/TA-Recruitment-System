package com.tars.service;

import com.tars.ai.PortraitGenerator;
import com.tars.ai.PortraitMatcher;
import com.tars.entity.bean.Application;
import com.tars.entity.bean.Portrait;
import com.tars.entity.bean.Position;
import com.tars.entity.bean.TAProfile;
import com.tars.entity.QueryCondition;
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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    private final JsonRepository<Portrait> portraitRepo = new JsonRepository<>(Portrait.class);

    private final PortraitGenerator portraitGenerator = new PortraitGenerator();

    private final PortraitMatcher portraitMatcher = new PortraitMatcher();

    private final int posPageSize = 9;

    private final int appPageSize = 10;

    public boolean postPosition(Position position, String repostId) {
        try {
            Portrait portrait = portraitGenerator.generatePortrait(position);
            position.setPortraitId(portrait.getId());

            if (repostId != null && !repostId.trim().isEmpty()) {
                // Repost mode: load original position and preserve id and createAt
                Position originalPosition = positionRepo.getEntityById(repostId);
                
                if (originalPosition == null) {
                    log.error("Original position not found for repost, reposId: {}", repostId);
                    return false;
                }

                // Preserve original id and createAt
                String originalId = originalPosition.getId();
                Timestamp originalCreateAt = originalPosition.getCreateAt();

                // Update the position with preserved fields
                position.setId(originalId);
                position.setCreateAt(originalCreateAt);
                position.setUpdateAt(Timestamp.valueOf(LocalDateTime.now()));

                // Save the reposted position (update existing record)
                positionRepo.saveEntity(position);
                
                log.info("repost position success, positionId: {}, originalCreateAt: {}", 
                        originalId, originalCreateAt);
            } else {
                // Normal create mode: generate new id and timestamps
                position.setUpdateAt(Timestamp.valueOf(LocalDateTime.now()));
                positionRepo.saveEntity(position);
                
                log.info("create position success, positionId: {}", position.getId());
            }

            // Always save/update the portrait
            portraitRepo.saveEntity(portrait);

        } catch (IOException e) {
            log.error("create/repost position failed, positionId: {}, error message: {}", 
                    position.getId(), e.getMessage());
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
                    .map(PosMapper.INSTANCE::toMOPosBriefDTO)
                    .filter(Objects::nonNull)
                    .toList();

        } catch (IOException e) {
            log.error("get position list failed, userId: {}, error message: {}", userId, e.getMessage());
            return List.of();
        }
    }

    public List<PosBriefDTO> getPositionList(String userId, QueryCondition condition) {
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("userId is null or empty");
            return List.of();
        }

        try {
            List<Position> positions = positionRepo.loadAllEntities();

            if (positions == null || positions.isEmpty()) {
                return List.of();
            }

            int pageNum = Math.max(condition.getPage(), 1);

            Stream<Position> posStream = positions.stream()
                    .filter(pos -> pos != null && userId.equals(pos.getPostUserId()));

            posStream = switch (condition.getFilter()) {
                case "opened" -> posStream.filter(pos -> pos.getStatus() == 0);
                case "closed" -> posStream.filter(pos -> pos.getStatus() == 1);
                case "filled" -> posStream.filter(pos -> pos.getStatus() == 2);
                case "withdrawn" -> posStream.filter(pos -> pos.getStatus() == 3);
                default -> posStream;
            };

            posStream = switch (condition.getOrder()) {
                case "postDate" -> posStream.sorted(Comparator.comparing(Position::getPostDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
                case "deadline" -> posStream.sorted(Comparator.comparing(Position::getDeadline, Comparator.nullsLast(Comparator.naturalOrder())));
                default -> posStream;
            };

            return posStream.skip((long) (pageNum - 1) * posPageSize)
                    .limit(posPageSize)
                    .map(PosMapper.INSTANCE::toMOPosBriefDTO)
                    .filter(Objects::nonNull)
                    .toList();

        } catch (IOException e) {
            log.error("get position list failed, userId: {}, error message: {}", userId, e.getMessage());
            return List.of();
        }
    }


    public long getPositionPages(String userId, QueryCondition condition) {
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("userId is null or empty");
            return 0;
        }

        try {
            List<Position> positions = positionRepo.loadAllEntities();

            if (positions == null || positions.isEmpty()) {
                return 0;
            }

            Stream<Position> posStream = positions.stream()
                    .filter(pos -> pos != null && userId.equals(pos.getPostUserId()));
            posStream = switch (condition.getFilter()) {
                case "opened" -> posStream.filter(pos -> pos.getStatus() == 0);
                case "closed" -> posStream.filter(pos -> pos.getStatus() == 1);
                case "filled" -> posStream.filter(pos -> pos.getStatus() == 2);
                case "withdrawn" -> posStream.filter(pos -> pos.getStatus() == 3);
                default -> posStream;
            };

            long positionCount = posStream.count();

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


    public List<ApplicationDTO> getAppList(String posId, QueryCondition condition) {
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

            int pageNum = Math.max(condition.getPage(), 1);

            Stream<Application> baseAppStream = applications.stream()
                    .filter(app -> app != null && posId.equals(app.getPositionId()) && app.getStatus() != 3);

            baseAppStream = switch (condition.getFilter()) {
                case "opened" -> baseAppStream.filter(app -> app.getStatus() == 0);
                case "offered" -> baseAppStream.filter(app -> app.getStatus() == 1);
                case "rejected" -> baseAppStream.filter(app -> app.getStatus() == 2);
                default -> baseAppStream;
            };


            List<Application> finalApplications;

            if ("recommend".equals(condition.getOrder())) {
                try {
                    Position position = positionRepo.getEntityById(posId);
                    if (position == null) {
                        log.warn("Position not found for recommendation, posId: {}", posId);
                        finalApplications = baseAppStream.toList();
                    } else {
                        List<Application> allApplications = applicationRepo.loadAllEntities();
                        Map<String, List<Application>> userOfferedAppsMap = allApplications.stream()
                                .filter(app -> app != null && app.getStatus() == 1)
                                .collect(Collectors.groupingBy(Application::getUserId));

                        List<LocalDate> weeklyPeriods = generateWeeklyPeriods(
                                position.getStartDate().toLocalDateTime().toLocalDate(),
                                position.getEndDate().toLocalDateTime().toLocalDate()
                        );

                        Portrait positionPortrait = portraitRepo.getEntityById(position.getPortraitId());

                        List<Application> appList = baseAppStream.toList();

                        List<Application> notExceedList = new java.util.ArrayList<>();
                        List<Application> exceedList = new java.util.ArrayList<>();

                        Map<String, Double> scoreCache = new java.util.HashMap<>();

                        for (Application app : appList) {
                            String userId = app.getUserId();
                            TAProfile profile = profileMap.get(userId);

                            if (profile == null) {
                                log.debug("Profile not found for userId: {}", userId);
                                continue;
                            }

                            float maxWeeklyWorkload = profile.getMaxWeeklyWorkload();

                            List<Application> userOfferedApps = userOfferedAppsMap.getOrDefault(userId, List.of());

                            boolean exceeds = checkWorkloadExceeds(
                                    userOfferedApps,
                                    position,
                                    weeklyPeriods,
                                    maxWeeklyWorkload);

                            if (exceeds) {
                                exceedList.add(app);
                            } else {
                                notExceedList.add(app);

                                if (!scoreCache.containsKey(userId)) {
                                    Portrait taPortrait = null;
                                    if (profile.getPortraitId() != null) {
                                        taPortrait = portraitRepo.getEntityById(profile.getPortraitId());
                                    }

                                    double score = 0.0;
                                    if (taPortrait != null && positionPortrait != null) {
                                        score = portraitMatcher.calculateMatchScore(taPortrait, positionPortrait);
                                    }
                                    scoreCache.put(userId, score);
                                }
                            }
                        }

                        notExceedList.sort((app1, app2) -> {
                            double score1 = scoreCache.getOrDefault(app1.getUserId(), 0.0);
                            double score2 = scoreCache.getOrDefault(app2.getUserId(), 0.0);
                            return Double.compare(score2, score1);
                        });

                        List<Application> sortedList = new ArrayList<>(notExceedList);
                        sortedList.addAll(exceedList);

                        finalApplications = sortedList;
                    }


                } catch (IOException e) {
                    log.error("Recommend sorting failed, error message: {}", e.getMessage());
                    finalApplications = baseAppStream.toList();
                }
            } else {
                Comparator<Application> comparator = switch (condition.getOrder()) {
                    case "applyAt" -> Comparator.comparing(Application::getApplyAt);
                    default -> (a1, a2) -> 0;
                };

                finalApplications = baseAppStream.sorted(comparator).toList();
            }

            return finalApplications.stream()
                    .map(app -> {
                        TAProfile profile = profileMap.get(app.getUserId());
                        if (profile == null) {
                            log.debug("TAProfile not found for userId: {}", app.getUserId());
                            return null;
                        }
                        return AppMapper.INSTANCE.toAppDTO(app, profile);
                    })
                    .filter(Objects::nonNull)
                    .skip((long) (pageNum - 1) * appPageSize)
                    .limit(appPageSize)
                    .toList();

        } catch (IOException e) {
            log.error("get application list failed, posId: {}, error message: {}", posId, e.getMessage());
            return List.of();
        }
    }


    public long getAppPages(String posId, QueryCondition condition) {
        if (posId == null || posId.trim().isEmpty()) {
            log.warn("posId is null or empty");
            return 0;
        }

        try {
            List<Application> applications = applicationRepo.loadAllEntities();

            if (applications == null || applications.isEmpty()) {
                return 0;
            }

            Stream<Application> appStream = applications.stream()
                    .filter(app -> app != null && posId.equals(app.getPositionId()) && app.getStatus() != 3);

            appStream = switch (condition.getFilter()) {
                case "opened" -> appStream.filter(app -> app.getStatus() == 0);
                case "offered" -> appStream.filter(app -> app.getStatus() == 1);
                case "rejected" -> appStream.filter(app -> app.getStatus() == 2);
                default -> appStream;
            };

            long appCount = appStream.count();

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

    private List<LocalDate> generateWeeklyPeriods(LocalDate startDate, LocalDate endDate) {
        List<LocalDate> periods = new ArrayList<>();
        
        LocalDate current = startDate;
        
        while (!current.isAfter(endDate)) {
            periods.add(current);
            
            DayOfWeek currentDayOfWeek = current.getDayOfWeek();
            int daysUntilSunday = DayOfWeek.SUNDAY.getValue() - currentDayOfWeek.getValue();
            
            if (daysUntilSunday == 0) {
                daysUntilSunday = 7;
            }
            
            current = current.plusDays(daysUntilSunday + 1);
        }

        return periods;
    }

    private boolean checkWorkloadExceeds(List<Application> offeredApps, 
                                         Position newPosition,
                                         List<LocalDate> weeklyPeriods,
                                         float maxWeeklyWorkload) {
        try {
            LocalDate positionEndDate = newPosition.getEndDate().toLocalDateTime().toLocalDate();

            for (LocalDate weekStart : weeklyPeriods) {
                DayOfWeek startDayOfWeek = weekStart.getDayOfWeek();
                int daysUntilSunday = DayOfWeek.SUNDAY.getValue() - startDayOfWeek.getValue();
                
                LocalDate weekEnd = weekStart.plusDays(daysUntilSunday);
                
                if (weekEnd.isAfter(positionEndDate)) {
                    weekEnd = positionEndDate;
                }

                float totalWorkload = newPosition.getWeeklyWorkload();

                for (Application offeredApp : offeredApps) {
                    Position offeredPosition = positionRepo.getEntityById(offeredApp.getPositionId());
                    if (offeredPosition == null) {
                        continue;
                    }

                    LocalDate posStart = offeredPosition.getStartDate().toLocalDateTime().toLocalDate();
                    LocalDate posEnd = offeredPosition.getEndDate().toLocalDateTime().toLocalDate();

                    if (!weekStart.isAfter(posEnd) && !weekEnd.isBefore(posStart)) {
                        totalWorkload += offeredPosition.getWeeklyWorkload();
                    }
                }

                if (totalWorkload > maxWeeklyWorkload) {
                    return true;
                }
            }
            
            return false;
        } catch (IOException e) {
            log.error("Check workload exceeds failed, error message: {}", e.getMessage());
            return false;
        }
    }

    public boolean withdrawPosition(String posId) {

        try {
            Position position = positionRepo.getEntityById(posId);
            if (position == null) {
                log.warn("position not found, posId: {}", posId);
                return false;
            }

            position.setStatus(3);
            position.setAppliedNum(0);
            position.setOfferedNum(0);
            position.setRejectedNum(0);
            position.setUpdateAt(Timestamp.valueOf(LocalDateTime.now()));
            positionRepo.saveEntity(position);

            // Delete all corresponding applications
            List<Application> applications = applicationRepo.loadAllEntities();
            if (applications != null && !applications.isEmpty()) {
                for (Application app : applications) {
                    if (app != null && posId.equals(app.getPositionId())) {
                        applicationRepo.deleteEntity(app.getId());
                    }
                }
            }

            portraitRepo.deleteEntity(position.getPortraitId());

            log.info("withdraw position success, posId: {}", posId);
            return true;
        } catch (IOException e) {
            log.error("withdraw position failed, posId: {}, error message: {}", posId, e.getMessage());
            return false;
        }
    }
}
