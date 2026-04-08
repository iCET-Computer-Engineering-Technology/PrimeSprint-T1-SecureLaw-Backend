package com.primesprint.service.impl;

import com.primesprint.event.UserRegisteredEvent;
import com.primesprint.mapper.UserMapper;
import com.primesprint.model.dto.Page;
import com.primesprint.model.dto.UserDto;
import com.primesprint.model.dto.request.PageRequest;
import com.primesprint.model.dto.request.UserCreateRequest;
import com.primesprint.model.dto.request.UserUpdateRequest;
import com.primesprint.model.entity.Role;
import com.primesprint.model.entity.User;
import com.primesprint.repository.AdminUserRepository;
import com.primesprint.repository.ProfileRepository;
import com.primesprint.repository.RoleRepository;
import com.primesprint.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final AdminUserRepository adminUserRepository;
    private final RoleRepository roleRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.access-link-base:https://secureflow.com/access}")
    private String accessLinkBase = "https://secureflow.com/access";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    @Transactional
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

        profileRepository.createProfile(
                savedUser.getId(),
                savedUser.getUsername()
        );

        String setupToken = generateSetupToken();
        String accessLink = accessLinkBase
                + "?email=" + URLEncoder.encode(savedUser.getEmail(), StandardCharsets.UTF_8)
                + "&setupToken=" + URLEncoder.encode(setupToken, StandardCharsets.UTF_8);
        eventPublisher.publishEvent(new UserRegisteredEvent(
                null,
                savedUser.getEmail(),
                savedUser.getUsername(),
                accessLink,
                savedUser.getCreatedAt().toInstant().toString()
        ));

        return userMapper.toDto(savedUser);
    }

    private String generateSetupToken() {
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
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
            user.setRole(com.primesprint.model.enums.Role.valueOf(role.getName()));
        }
        if (request.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getSeniorId() != null) {
            user.setSeniorId(request.getSeniorId());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
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
        int page = pageRequest.getPage();
        int size = pageRequest.getSize();
        if (page < 1) {
            throw new IllegalArgumentException("Page index must be greater than or equal to 1");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Page size must be greater than 0");
        }

        int offset = (page - 1) * size;
        List<User> users;
        long totalElements;
        if (search != null && !search.isEmpty()) {
            users = adminUserRepository.search(offset, size,
                    pageRequest.getSort(), pageRequest.getDirection(), search);
            totalElements = adminUserRepository.count(search);
        } else {
            users = adminUserRepository.findAll(offset, size,
                    pageRequest.getSort(), pageRequest.getDirection());
            totalElements = adminUserRepository.countAll();
        }

        int totalPages = (int) Math.ceil((double) totalElements / size);
        List<UserDto> userDtos = users.stream()
                .map(userMapper::toDto)
                .toList();
        return new Page<>(userDtos, totalPages, totalElements, size, page);
    }
}
