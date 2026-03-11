package com.primesprint.service;

import com.primesprint.model.dto.Page;
import com.primesprint.model.dto.UserDto;
import com.primesprint.model.dto.request.PageRequest;
import com.primesprint.model.dto.request.UserCreateRequest;
import com.primesprint.model.dto.request.UserUpdateRequest;

import java.util.UUID;

public interface AdminUserService {

    UserDto createUser(UserCreateRequest request);

    UserDto updateUser(UUID id, UserUpdateRequest request);

    void deleteUser(UUID id);

    Page<UserDto> getUsers(PageRequest pageRequest, String search);
}
