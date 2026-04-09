package com.tars.mapper;

import com.tars.entity.bean.MOProfile;
import com.tars.entity.bean.TAProfile;
import com.tars.entity.dto.admin.MOProDTO;
import com.tars.entity.dto.admin.TAProDTO;
import com.tars.entity.dto.ta.ProfileDTO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author QiheSun
 * @version 1.0.0
 * @since 2026/3/30
 */
@Mapper
public interface ProfileMapper {

    ProfileMapper INSTANCE = Mappers.getMapper(ProfileMapper.class);

    ProfileDTO toTAProfileDTO(TAProfile taProfile);
}
