package com.primesprint.model.entity;

import com.primesprint.model.enums.Role;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class User implements UserDetails {
    private UUID id;
    private String username;
    private String email;
    private String password;
    private Role role;
    private UUID roleId;
    private String status;
    private UUID seniorId;
    private Timestamp createdAt;
    private Timestamp updatedAt;


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}
