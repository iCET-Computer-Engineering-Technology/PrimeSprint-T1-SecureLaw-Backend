package com.primesprint.dto;

import com.primesprint.enums.ActionType;
import lombok.*;
import java.time.LocalDateTime;
import java.util.Map;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class AuditLogDto {

    private String userId;
    private LocalDateTime timestamp;
    private String target; //templateId,userId or annotated method name etc.
    private ActionType action;//ActionType Enum
    private String templateId;
    private Map<String, Integer> maskCounts;
    private String modelUsed;
    private Long responseTime;
    private String details;

}
