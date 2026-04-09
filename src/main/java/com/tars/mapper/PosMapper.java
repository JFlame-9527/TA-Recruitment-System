package com.tars.mapper;

import com.tars.entity.bean.Application;
import com.tars.entity.bean.Position;
import com.tars.entity.dto.ta.PosBriefDTO;
import com.tars.entity.dto.ta.PosDetailDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * @author Xiri04
 * @version 1.0.0
 * @since 2026/3/30
 */
@Mapper
public interface PosMapper {
    PosMapper INSTANCE = Mappers.getMapper(PosMapper.class);

    @Mapping(target = "posId", source = "position.id")
    @Mapping(target = "posStatus", source = "position.status")
    @Mapping(target = "appId", source = "application.id")
    @Mapping(target = "appStatus", source = "application.status")
    PosBriefDTO toTAPosBriefDTO(Position position, Application application);

    @Mapping(target = "posId", source = "position.id")
    @Mapping(target = "posStatus", source = "position.status")
    @Mapping(target = "appId", source = "application.id")
    @Mapping(target = "appStatus", source = "application.status")
    PosDetailDTO toTAPosDetailDTO(Position position, Application application);
}
