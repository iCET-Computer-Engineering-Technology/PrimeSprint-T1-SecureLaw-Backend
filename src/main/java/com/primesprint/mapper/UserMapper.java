package com.primesprint.mapper;

import com.primesprint.model.dto.UserDto;
import com.primesprint.model.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto toDto(User user);
}

