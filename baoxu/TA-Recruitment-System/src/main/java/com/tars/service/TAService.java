package com.tars.service;

import com.tars.entity.bean.Application;
import com.tars.entity.bean.Position;
import com.tars.entity.bean.TAProfile;
import com.tars.entity.dto.ta.AppPosDTO;
import com.tars.entity.dto.ta.PosBriefDTO;
import com.tars.entity.dto.ta.PosDetailDTO;
import com.tars.entity.dto.ta.ProfileDTO;
import com.tars.mapper.MultiMapper;
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
 * @author QiheSun Xiri04
 * @version 2.0.0
 * @since 2026/3/25
 */
@Slf4j
public class TAService {

    private final JsonRepository<TAProfile> taProfileRepo = new JsonRepository<>(TAProfile.class);

    private final JsonRepository<Application> applicationRepo = new JsonRepository<>(Application.class);

    private final JsonRepository<Position> positionRepo = new JsonRepository<>(Position.class);

    private final int applyPageSize = 9;

    private final int posPageSize = 10;

    public boolean createProfile(TAProfile taProfile) {
        try {
            taProfileRepo.saveEntity(taProfile);
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
    public boolean updateProfile(TAProfile taProfile) {
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
            taProfileRepo.saveEntity(taProfile);
            log.info("update taProfile success, taProfileId: {}", taProfile.getId());
        } catch (IOException e) {
            log.error("update taProfile failed, taProfileId: {}, error message: {}",
                    taProfile.getId(), e.getMessage(), e);
            return false;
        }
        return true;
    }
}
