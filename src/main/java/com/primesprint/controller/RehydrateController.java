package com.primesprint.controller;

import com.primesprint.model.dto.request.RehydrateRequest;
import com.primesprint.model.dto.response.RehydrateResponse;
import com.primesprint.service.RehydrateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class RehydrateController {

    final RehydrateService rehydrater;

    @PostMapping("/rehydrate")
    public RehydrateResponse rehydrate(@RequestBody RehydrateRequest rehydrateRequest){
        return rehydrater.rehydrate(rehydrateRequest);
    }
}
