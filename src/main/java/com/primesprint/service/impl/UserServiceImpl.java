package com.primesprint.service.impl;

import com.primesprint.event.UserRegisteredEvent;
import com.primesprint.model.dto.UserDto;
import com.primesprint.repository.RoleRepository;
import com.primesprint.repository.UserRepository;
import com.primesprint.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.access-link-base:https://secureflow.com/access}")
    private String accessLinkBase = "https://secureflow.com/access";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    @Transactional
    public void createUser(UserDto userDto) {
        log.info("Creating new user: {}", userDto.getEmail());

        UUID roleId = roleRepository.findRoleIdByName(userDto.getRole().name());
        userRepository.saveUser(
                userDto.getUsername(),
                userDto.getEmail(),
                passwordEncoder.encode(userDto.getPassword()),
                roleId
        );

        String setupToken = generateSetupToken();
        String accessLink = accessLinkBase
                + "?email=" + java.net.URLEncoder.encode(userDto.getEmail(), StandardCharsets.UTF_8)
                + "&setupToken=" + java.net.URLEncoder.encode(setupToken, StandardCharsets.UTF_8);

        UserRegisteredEvent event = new UserRegisteredEvent(
                null,
                userDto.getEmail(),
                userDto.getUsername(),
                accessLink,
                Instant.now().toString()
        );

        eventPublisher.publishEvent(event);
        log.info("UserRegisteredEvent published for: {}", userDto.getEmail());
    }

    private String generateSetupToken() {
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }
}
