package com.primesprint.model;

import lombok.*;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class User {
    private UUID id;
    private String username;
    private String email;
    private String password;
    private UUID roleId;
    private String status;
    private UUID seniorId;
}
