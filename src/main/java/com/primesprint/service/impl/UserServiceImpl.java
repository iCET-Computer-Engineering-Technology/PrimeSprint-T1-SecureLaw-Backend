package com.primesprint.service.impl;

import com.primesprint.event.UserCreatedEvent;
import com.primesprint.model.dto.UserDto;
import com.primesprint.model.entity.User;
import com.primesprint.repository.UserRepository;
import com.primesprint.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor

public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    @Override
    @Transactional
    public void createUser(UserDto userDto) {
        log.info("Creating new user: {}", userDto.getEmail());
        User user = User.builder()
                .username(userDto.getUsername())
                .email(userDto.getEmail())
                .password(userDto.getPassword())
                .role(userDto.getRole())
                .status("ACTIVE")
                .build();
        String accessLink = "https://secureflow.com/access?token=generated-token-here";

        UserCreatedEvent event = new UserCreatedEvent(
                1L,
                userDto.getEmail(),
                userDto.getUsername(),
                accessLink,
                userDto.getPassword(),
                Instant.now().toString()
        );

        eventPublisher.publishEvent(event);
        log.info("UserCreatedEvent published for: {}", userDto.getEmail());
    }
}
