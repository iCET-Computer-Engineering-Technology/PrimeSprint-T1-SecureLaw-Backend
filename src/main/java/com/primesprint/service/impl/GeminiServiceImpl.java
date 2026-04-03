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

        boolean isChatMode = doc == null || doc.trim().isEmpty() ||
                externalAiRequest.getTokenMappings() == null || externalAiRequest.getTokenMappings().isEmpty();

        String text;
        if (isChatMode) {
            String chatPromptTemplate = """
                    You are SecureFlow, a professional AI assistant integrated into a secure document processing system.
                    
                    Your role:
                    - Assist users with questions and tasks
                    - Respond naturally and helpfully
                    - Maintain a professional tone
                    
                    Rules:
                    - Do NOT mention Google, Gemini, or any underlying model
                    - Do NOT describe yourself as a language model
                    - Always present yourself as SecureFlow
                    
                    USER MESSAGE:
                    %s
                    """;
            text = chatPromptTemplate.formatted(prompt != null ? prompt : "");
        } else {
            String legend = externalAiRequest.getTokenMappings().keySet().stream()
                    .map(this::toLegendLine)
                    .collect(Collectors.joining("\n"));

            String securePromptTemplate = """
                    You are a legal document assistant.
                    
                    Task:
                    Execute the user's instruction using the provided document.
                    
                    Constraints:
                    - Preserve all tokens exactly as they appear (<<<SL_TOKEN_...>>>).
                    - Never modify, rename, split, or remove tokens.
                    - Do not invent new tokens.
                    - Write naturally around tokens.
                    
                    Token Legend:
                    %s
                    
                    Document:
                    %s
                    
                    User Instruction:
                    %s
                    
                    Return only the final processed text.
                    """;
            text = securePromptTemplate.formatted(legend, doc, prompt != null ? prompt : "");
        }

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

            String responseText = response.text();
            return new ExternalAiResponse(externalAiRequest.getRequestId(), externalAiRequest.getProvider(), model, responseText != null ? responseText.trim() : "", null);
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