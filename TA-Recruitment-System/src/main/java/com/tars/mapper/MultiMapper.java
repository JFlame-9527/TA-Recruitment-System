package com.tars.mapper;

import com.tars.entity.bean.Application;
import com.tars.entity.bean.Position;
import com.tars.entity.dto.ta.AppPosDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * @author Xiri04
 * @version 1.0.0
 * @since 2026/3/26
 */
@Mapper
public interface MultiMapper {
    MultiMapper INSTANCE = Mappers.getMapper(MultiMapper.class);

    @Mapping(target = "appId", source = "application.id")
    @Mapping(target = "posId", source = "position.id")
    @Mapping(target = "status", source = "application.status")
    AppPosDTO toAppPosDTO(Application application, Position position);
}
