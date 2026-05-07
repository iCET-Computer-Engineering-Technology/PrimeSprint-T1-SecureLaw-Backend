package com.primesprint.dto;

import lombok.*;

import java.time.LocalDate;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class PIIDailyCount {
    private LocalDate day;
    private Integer totalBlocked;

}
