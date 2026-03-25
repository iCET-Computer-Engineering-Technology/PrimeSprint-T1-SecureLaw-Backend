package com.primesprint.controller;

import com.primesprint.model.dto.request.MaskRequest;
import com.primesprint.model.dto.response.MaskResponse;
import com.primesprint.service.MaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class MaskController {

    final MaskService maskService;

    @PostMapping("/mask")
    public MaskResponse mask(@RequestBody MaskRequest maskRequest){
        return maskService.mask(maskRequest);
    }

}
