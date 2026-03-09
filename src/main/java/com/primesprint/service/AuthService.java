package com.primesprint.service;

import com.primesprint.model.User;
import com.primesprint.repository.UserRepository;
import com.primesprint.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public String login(String usernameOrEmail, String password) {

        User user = userRepository.findByUsernameOrEmail(usernameOrEmail);

        if(user == null){
            throw new RuntimeException("User not found");
        }

        if(!passwordEncoder.matches(password, user.getPassword())){
            throw new RuntimeException("Invalid password");
        }

        return jwtUtil.generateToken(user);
    }

}