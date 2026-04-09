package com.tars.mapper;

import com.tars.entity.bean.Application;
import com.tars.entity.bean.TAProfile;
import com.tars.entity.dto.mo.ApplicationDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * @author Jflame
 * @version 1.0.0
 * @since 2026/4/5
 */
@Mapper
public interface AppMapper {
    AppMapper INSTANCE = Mappers.getMapper(AppMapper.class);

    @Mapping(target = "appId", source = "application.id")
    @Mapping(target = "proId", source = "taProfile.id")
    @Mapping(target = "name", source = "taProfile.name")
    ApplicationDTO toAppDTO(Application application, TAProfile taProfile);
}
