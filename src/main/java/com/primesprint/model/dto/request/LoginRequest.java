package com.primesprint.model.dto.request;

import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class LoginRequest {
    private String usernameOrEmail;
    private String password;
}
