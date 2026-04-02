package com.primesprint.service;

import com.primesprint.model.dto.UserDto;
import com.primesprint.model.dto.request.LoginRequest;
import com.primesprint.model.dto.request.RegisterRequest;
import com.primesprint.model.entity.User;
import com.primesprint.repository.RoleRepository;
import com.primesprint.repository.UserRepository;
import com.primesprint.mapper.UserMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;

    //AUTHENTICATE (LOGIN VALIDATION ONLY)
    @Override
    public User authenticate(LoginRequest request) {

        User user = userRepository
                .findByUsernameOrEmail(request.getUsernameOrEmail());

        if (user == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid credentials"
            );
        }

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid credentials"
            );
        }

        return user;
    }

    // REGISTER (UNCHANGED
    @Override
    public void register(RegisterRequest request) {

        String hashedPassword = passwordEncoder.encode(request.getPassword());

        UUID roleId = roleRepository
                .findRoleIdByName(request.getRole().name());

        userRepository.saveUser(
                request.getUsername(),
                request.getEmail(),
                hashedPassword,
                roleId
        );
    }

    // GET USER FROM TOKEN (USED IN /me
    @Override
    public UserDto getUserFromToken(String token) {

        throw new UnsupportedOperationException(
                "Use JwtFilter + SecurityContext instead"
        );
    }
    // GET USER BY USERNAM
    @Override
    public UserDto getUserByUsername(String username) {

        User user = userRepository.findByUsernameOrEmail(username);

        if (user == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "User not found"
            );
        }

        return userMapper.toDto(user);
    }
    // GET USER BY ID (FOR REFRESH FLOW
    @Override
    public User getById(UUID id) {

        User user = userRepository.findById(id);

        if (user == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "User not found"
            );
        }

        return user;
    }
    // PASSWORD CHANGE SUPPOR
    @Override
    public void updatePasswordChangedAt(UUID userId) {

        userRepository.updatePasswordChangedAt(userId);
    }

    @Override
    public boolean validateToken(String token) {
        throw new UnsupportedOperationException(
                "Use JwtFilter for validation"
        );
    }

}