package com.primesprint.controller;

import com.primesprint.model.dto.UserDto;
import com.primesprint.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor

public class UserController {
    private final UserService userService;
    @PostMapping("/create")
    public ResponseEntity<String> createUser(@RequestBody UserDto userDto) {
        log.info("Received request to create user: {}", userDto.getEmail());

        userService.createUser(userDto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body("User account created successfully. Invitation email process initiated.");
    }
}
