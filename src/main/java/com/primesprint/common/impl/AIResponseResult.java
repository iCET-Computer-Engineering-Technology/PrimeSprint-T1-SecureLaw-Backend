package com.primesprint.common.impl;

import com.primesprint.common.AIResult;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class AIResponseResult implements AIResult {

    private String templateId;
    private Map<String, Integer> maskCounts;
    private String modelUsed;

}
