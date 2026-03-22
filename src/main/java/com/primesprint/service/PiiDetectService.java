package com.primesprint.service;

import com.primesprint.model.dto.SensitiveDataItem;
import com.primesprint.model.dto.request.PiiDetectRequest;

import java.util.List;

public interface PiiDetectService {
    List<SensitiveDataItem> detect(PiiDetectRequest request);
}
