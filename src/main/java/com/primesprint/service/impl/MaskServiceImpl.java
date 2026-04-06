package com.primesprint.service.impl;

import com.primesprint.custom_annotation.AIInteractionAuditable;
import com.primesprint.model.dto.request.MaskRequest;
import com.primesprint.model.dto.response.MaskResponse;
import com.primesprint.model.enums.ActionType;
import com.primesprint.service.MaskService;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MaskServiceImpl implements MaskService {
    @Override
    @AIInteractionAuditable(action = ActionType.MASK_APPLIED)
    public MaskResponse mask(MaskRequest maskRequest) {
        StringBuilder doc = new StringBuilder(maskRequest.getDocument());
        StringBuilder prompt = new StringBuilder(maskRequest.getPrompt());

        Map<String, String> tokenMap = new HashMap<>();
        Map<String, Integer> countMap = new HashMap<>();

        List<MaskRequest.SensitiveData> docData = new ArrayList<>();
        List<MaskRequest.SensitiveData> promptData = new ArrayList<>();

        for (MaskRequest.SensitiveData s : maskRequest.getSensitiveData()) {
            if ("document".equals(s.getSource())) {
                docData.add(s);
            } else {
                promptData.add(s);
            }
        }

        docData.sort((a, b) -> Integer.compare(b.getStart(), a.getStart()));
        promptData.sort((a, b) -> Integer.compare(b.getStart(), a.getStart()));

        processReplacements(docData, doc, tokenMap, countMap);

        processReplacements(promptData, prompt, tokenMap, countMap);

        return new MaskResponse(maskRequest.getRequestId(), doc.toString(), prompt.toString(),maskRequest.getRequestId(), tokenMap);
    }

    private void processReplacements(List<MaskRequest.SensitiveData> dataList, StringBuilder sb,
                                     Map<String, String> tokenMap, Map<String, Integer> countMap) {
        for (MaskRequest.SensitiveData s : dataList) {
            int currentCount = countMap.getOrDefault(s.getType(), 0) + 1;
            countMap.put(s.getType(), currentCount);

            String token = String.format("<<<SL_TOKEN_%s_%s_SEQ%d>>>",
                    UUID.randomUUID().toString().substring(0, 8), s.getType(), currentCount);

            tokenMap.put(token, sb.substring(s.getStart(), s.getEnd()));

            sb.replace(s.getStart(), s.getEnd(), token);
        }
    }
}
