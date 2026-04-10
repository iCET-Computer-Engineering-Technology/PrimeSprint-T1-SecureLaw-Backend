package com.primesprint.service.impl;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import com.primesprint.model.dto.request.ExternalAiRequest;
import com.primesprint.model.dto.response.ExternalAiResponse;
import com.primesprint.service.GeminiService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class GeminiServiceImpl implements GeminiService {

    private static final Pattern TOKEN_TYPE_PATTERN =
            Pattern.compile("<<<SL_TOKEN_[a-f0-9]+_([A-Z_]+)_SEQ\\d+>>>");

    private final String geminiApiKey;

    public GeminiServiceImpl(@Value("${gemini.api-key:${GEMINI_API_KEY:}}") String geminiApiKey) {
        this.geminiApiKey = geminiApiKey;
    }

    @Override
    public ExternalAiResponse process(ExternalAiRequest externalAiRequest) {

        String prompt = externalAiRequest.getMaskedPrompt();
        String doc = externalAiRequest.getMaskedDocument();

        String legend = "";
        String text = "";

        if(!externalAiRequest.getTokenMappings().isEmpty() && externalAiRequest.getMaskedDocument() != null){

            legend = externalAiRequest.getTokenMappings().keySet().stream()
                    .map(this::toLegendLine)
                    .collect(Collectors.joining("\n"));

            text = """
                You are a legal document assistant. You will receive:
                1) A masked document (context) containing ONLY tokens in the form <<<SL_TOKEN_xxx_TYPE_SEQn>>>.
                2) A masked user prompt that instructs what to do with the masked document.
                
                RULES (CRITICAL — enforce verbatim):
                1. NEVER modify, remove, rename, or split any <<<SL_TOKEN_...>>> placeholder.
                2. Copy each token EXACTLY as-is in your output whenever the value belongs there.
                3. Treat each token as the real value of its type and write naturally around it.
                4. If you are unsure whether a token should appear, prefer to include it where context indicates.
                5. Do not invent new tokens or make up values for tokens.
                6. If asked to redact or obscure data, keep tokens unchanged and follow the instruction with tokens intact.
                
                TOKEN LEGEND:
                ${legend}
                
                INPUT:
                DOCUMENT:
                ${maskedDocument}
                
                USER PROMPT:
                ${maskedPrompt}
                
                Produce a single text output — do not return JSON or metadata. The response must include any tokens required and remain natural and professional.
                
                """;

            text = text.replace("${maskedDocument}",doc).replace("${maskedPrompt}",prompt).replace("${legend}",legend);

        }//have sensetive and has doc
        else if (!externalAiRequest.getTokenMappings().isEmpty() && externalAiRequest.getMaskedDocument() == null){//have sensetive and has no doc

            legend = externalAiRequest.getTokenMappings().keySet().stream()
                    .map(this::toLegendLine)
                    .collect(Collectors.joining("\n"));

            text = """
                You are a legal document assistant. You will receive:
                1) A masked user prompt that may contain tokens in the form <<<SL_TOKEN_xxx_TYPE_SEQn>>>.
                
                RULES (CRITICAL — enforce verbatim):
                1. NEVER modify, remove, rename, or split any <<<SL_TOKEN_...>>> placeholder.
                2. Copy each token EXACTLY as-is in your output whenever the value belongs there.
                3. Treat each token as the real value of its type and write naturally around it.
                4. If you are unsure whether a token should appear, prefer to include it where context indicates.
                5. Do not invent new tokens or make up values for tokens.
                6. If asked to redact or obscure data, keep tokens unchanged and follow the instruction with tokens intact.
                
                TOKEN LEGEND:
                ${legend}
                
                INPUT:
                
                USER PROMPT:
                ${maskedPrompt}
                
                Produce a single text output — do not return JSON or metadata. The response must include any tokens required and remain natural and professional.
                
                """;

            text = text.replace("${maskedPrompt}",prompt).replace("${legend}",legend);

        }//have sensetive and has no doc
        else if(externalAiRequest.getTokenMappings().isEmpty() && externalAiRequest.getMaskedDocument() != null){//dont have sensetive and has doc
            text = """
                You are a legal document assistant. You will receive:
                1) A masked document (context).
                2) A masked user prompt that instructs what to do with the masked document.
                
                INPUT:
                DOCUMENT:
                ${maskedDocument}
                
                USER PROMPT:
                ${maskedPrompt}
                
                Produce a single text output — do not return JSON or metadata. The response must remain natural and professional.
                
                """;

            text = text.replace("${maskedDocument}",doc).replace("${maskedPrompt}",prompt);

        }//dont have sensetive and has doc
        else if(externalAiRequest.getTokenMappings().isEmpty() && externalAiRequest.getMaskedDocument() == null){//dont have sensetive and has no doc

            text = """
                You are a legal document assistant. You will receive:
                1) A masked user prompt.
                
                INPUT:
                
                USER PROMPT:
                ${maskedPrompt}
                
                Produce a single text output — do not return JSON or metadata. The response must remain natural and professional.
                
                """;

            text = text.replace("${maskedPrompt}",prompt);
        }//dont have sensetive and has  doc


        String apiKey = geminiApiKey == null ? "" : geminiApiKey.trim();
        if (apiKey.isEmpty()) {
            throw new IllegalStateException("Missing Gemini API key. Set 'gemini.api-key' or GEMINI_API_KEY.");
        }

        try (Client client = Client.builder().apiKey(apiKey).build()) {
            String model = "gemini-3-flash-preview";
            GenerateContentResponse response =
                    client.models.generateContent(
                            model,
                            text,
                            null);

            return new ExternalAiResponse(externalAiRequest.getRequestId(), externalAiRequest.getProvider(), model, response.text(), null);
        }
    }

    private String toLegendLine(String token) {
        Matcher matcher = TOKEN_TYPE_PATTERN.matcher(token);
        String readableType = "masked value";
        if (matcher.matches()) {
            readableType = matcher.group(1).toLowerCase(Locale.ROOT).replace('_', ' ');
        }
        return "• " + token + " — represents a " + readableType;
    }
}