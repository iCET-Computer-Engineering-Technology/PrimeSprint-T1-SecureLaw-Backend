package com.primesprint.service.impl;

import com.primesprint.mapper.UserMapper;
import com.primesprint.model.dto.UserDto;
import com.primesprint.model.dto.request.UserCreateRequest;
import com.primesprint.model.dto.request.UserUpdateRequest;
import com.primesprint.model.entity.Role;
import com.primesprint.model.entity.User;
import com.primesprint.repository.AdminUserRepository;
import com.primesprint.repository.RoleRepository;
import com.primesprint.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final AdminUserRepository adminUserRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    public UserDto createUser(UserCreateRequest request) {
        if (adminUserRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (adminUserRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (request.getSeniorId() != null && adminUserRepository.existsBySeniorId(request.getSeniorId())) {
            throw new IllegalArgumentException("Senior ID already exists");
        }

        Role role = roleRepository.findByName(request.getRole().name())
                .orElseThrow(() -> new IllegalArgumentException("Invalid role: " + request.getRole()));

        User user = User
                .builder()
                .id(UUID.randomUUID())
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(com.primesprint.model.enums.Role.valueOf(role.getName()))
                .roleId(role.getId())
                .status("ACTIVE")
                .seniorId(request.getSeniorId())
                .createdAt(Timestamp.from(Instant.now()))
                .updatedAt(Timestamp.from(Instant.now()))
                .build();
        User savedUser = adminUserRepository.save(user);
        return userMapper.toDto(savedUser);
    }

    @Override
    public UserDto updateUser(UUID id, UserUpdateRequest request) {
        return null;
    }

    @Override
    public void deleteUser(UUID id) {

    }
}
