package com.primesprint.common;

import java.util.Map;

public interface AIResult {
    public String getTemplateId();
    public Map<String, Integer> getMaskCounts();
    public String getModelUsed();
}
