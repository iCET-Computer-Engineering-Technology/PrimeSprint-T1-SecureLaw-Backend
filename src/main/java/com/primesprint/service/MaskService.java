package com.primesprint.service;

import com.primesprint.model.dto.request.MaskRequest;
import com.primesprint.model.dto.response.MaskResponse;

public interface MaskService {
    public MaskResponse mask(MaskRequest maskRequest);
}
