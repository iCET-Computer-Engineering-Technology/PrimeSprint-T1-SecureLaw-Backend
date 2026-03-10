package com.primesprint.model.dto;

import com.primesprint.model.enums.Role;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class UserDto {
    private UUID id;
    private String username;
    private String email;
    private String password;
    private Role role;
    private String status;
    private UUID seniorId;
}
