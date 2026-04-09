package com.tars.mapper;

import com.tars.entity.bean.User;
import com.tars.entity.dto.admin.UserDetailDTO;
import com.tars.entity.dto.user.UserDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * @author Jflame
 * @version 1.0.0
 * @since 2026/3/24
 */
@Mapper
public interface UserMapper {

    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    UserDTO toDTO(User user);

    @Mapping(target = "userId", source = "user.id")
    UserDetailDTO toDetailDTO(User user, String proId);
}
