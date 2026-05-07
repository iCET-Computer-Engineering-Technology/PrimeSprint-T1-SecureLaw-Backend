package com.primesprint.model.dto;

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
    private String source; // "document" | "prompt"
    private int start;     // UTF-8 byte offset (0-based)
    private int end;       // exclusive
}

