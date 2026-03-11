package com.primesprint.service.impl;

import com.primesprint.mapper.UserMapper;
import com.primesprint.model.dto.Page;
import com.primesprint.model.dto.UserDto;
import com.primesprint.model.dto.request.PageRequest;
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
import java.util.List;
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
        User user = adminUserRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));

        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())
                && adminUserRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (request.getEmail() != null && !request.getEmail().equalsIgnoreCase(user.getEmail())
                && adminUserRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (request.getRole() != null) {
            Role role = roleRepository.findByName(request.getRole().name())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid role: " + request.getRole()));
            user.setRoleId(role.getId());
        }
        if (request.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getSeniorId() != null) {
            user.setSeniorId(request.getSeniorId());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        } else {
            user.setStatus("ACTIVE");
        }

        user.setEmail(request.getEmail() != null ? request.getEmail() : user.getEmail());
        user.setUsername(request.getUsername() != null ? request.getUsername() : user.getUsername());
        user.setUpdatedAt(Timestamp.from(Instant.now()));

        User updatedUser = adminUserRepository.update(user);
        return userMapper.toDto(updatedUser);
    }

    @Override
    public void deleteUser(UUID id) {
        User user = adminUserRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));

        if (adminUserRepository.existsBySeniorId(user.getId())) {
            throw new IllegalStateException("Cannot delete user with assigned juniors");
        }

        adminUserRepository.delete(id);
    }

    @Override
    public Page<UserDto> getUsers(PageRequest pageRequest, String search) {
        int offset = (pageRequest.getPage() - 1) * pageRequest.getSize();
        List<User> users;
        long totalElements;
        if(search != null && !search.isEmpty()) {
            users = adminUserRepository.search(offset, pageRequest.getSize(),
                    pageRequest.getSort(), pageRequest.getDirection(), search);
            totalElements = adminUserRepository.count(search);
        }else{
            users = adminUserRepository.findAll(offset, pageRequest.getSize(),
                    pageRequest.getSort(), pageRequest.getDirection());
            totalElements = adminUserRepository.countAll();
        }

        int totalPages = (int) Math.ceil((double) totalElements / pageRequest.getSize());
        List<UserDto> userDtos = users.stream()
                .map(userMapper::toDto)
                .toList();
        return new Page<>(userDtos, totalPages, totalElements,pageRequest.getSize(), pageRequest.getPage());
    }
}
