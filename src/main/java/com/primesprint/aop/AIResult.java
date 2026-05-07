package com.primesprint.aop;

import java.util.Map;

public interface AIResult {
    public String getTemplateId();
    public Map<String, Integer> getMaskCounts();
    public String getModelUsed();
}
