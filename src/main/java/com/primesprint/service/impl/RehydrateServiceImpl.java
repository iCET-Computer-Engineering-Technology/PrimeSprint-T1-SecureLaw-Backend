package com.primesprint.service.impl;

import com.primesprint.custom_annotation.AIInteractionAuditable;
import com.primesprint.model.dto.request.RehydrateRequest;
import com.primesprint.model.dto.response.RehydrateResponse;
import com.primesprint.model.enums.ActionType;
import com.primesprint.service.RehydrateService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Map;

@Service
public class RehydrateServiceImpl implements RehydrateService {
    @Override
    @AIInteractionAuditable(action = ActionType.REHYDRATE_REQUESTED)
    public RehydrateResponse rehydrate(RehydrateRequest rehydrateRequest) {
        ArrayList<RehydrateResponse.Warning> warnings = new ArrayList<>();
        String tokenizedResponse = rehydrateRequest.getTokenizedResponse();

        Map<String,String> map = rehydrateRequest.getTokenMappings();

        for (Map.Entry<String, String> entry : map.entrySet()) {

            String token = entry.getKey();
            String value = entry.getValue();

            if(tokenizedResponse.contains(token)){
                tokenizedResponse = tokenizedResponse.replace(token, value);
            }else{
                warnings.add(new RehydrateResponse.Warning("UNKNOWN_TOKEN",token,"No mapping found"));
            }

        }

        return new RehydrateResponse(rehydrateRequest.getMappingId(),
                tokenizedResponse,
                warnings
        );
    }
}
