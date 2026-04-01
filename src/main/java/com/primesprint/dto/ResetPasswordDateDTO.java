package com.primesprint.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ResetPasswordDateDTO {
    private String email;
    private String token;
    private String newPassword;
}
