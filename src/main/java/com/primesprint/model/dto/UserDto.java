package com.primesprint.model.dto;

import com.primesprint.model.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    private UUID id;
    private String username;
    private String email;
    private Role role;
    private String status;
    private UUID seniorId;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private String password;
}
