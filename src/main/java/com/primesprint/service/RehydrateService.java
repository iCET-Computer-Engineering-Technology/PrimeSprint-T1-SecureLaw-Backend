package com.primesprint.service;

import com.primesprint.model.dto.request.RehydrateRequest;
import com.primesprint.model.dto.response.RehydrateResponse;

public interface RehydrateService {
    RehydrateResponse rehydrate(RehydrateRequest rehydrateRequest);
}
