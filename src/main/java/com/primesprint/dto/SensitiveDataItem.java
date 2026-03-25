package com.primesprint.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SensitiveDataItem {

    private String type;
    private String value;
    private String source;
    private int start;
    private int end;
}

