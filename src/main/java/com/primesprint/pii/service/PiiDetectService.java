package com.primesprint.pii.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.primesprint.pii.client.GroqLlmClient;
import com.primesprint.pii.dto.PiiDetectRequest;
import com.primesprint.pii.dto.SensitiveDataItem;
import com.primesprint.pii.util.AllowedTypes;
import com.primesprint.pii.util.Utf8ByteUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class PiiDetectService {
    private static final Logger log = LoggerFactory.getLogger(PiiDetectService.class);

    private static final String SYSTEM_PROMPT = """
            You are a PII extraction service.
            
            Your task is to identify sensitive entities in the provided input text and return their exact positions.
            
            Return ONLY a valid JSON array. Do not include explanations, markdown, or additional text.
            
            Output format:
            
            [
              { "type": "<CATEGORY>", "value": "<exact matched text>", "start": <start_index>, "end": <end_index> }
            ]
            
            Rules:
            
            1. <CATEGORY> must be one of the following:
            PERSON, ORGANIZATION, CASE_ID, ADDRESS, EMAIL, PHONE,
            NATIONAL_ID, PASSPORT, DRIVER_LICENSE, BANK_ACCOUNT,
            CARD_NUMBER, CONTRACT_REF, EVIDENCE_ID, AMOUNT.
            
            2. "value" must be the exact substring from the input text. Preserve casing, punctuation, and spacing exactly.
            
            3. "start" and "end" are CHARACTER indexes into the input text.
               - Indexing is 0-based.
               - "end" is exclusive.
               - substring = text[start:end].
            
            4. Return [] if no entities are found.
            
            5. If uncertain about a match, omit it. Do not guess.
            
            6. Do NOT return duplicate entries for the same span.
            
            7. If the same value appears multiple times in the input text,
               return a separate object for EACH occurrence with its correct start and end indexes.
            
            8. Entities must represent the exact contiguous span of the sensitive value.
               Do not include surrounding labels such as "Email:", "Phone:", etc.
            
            9. Do NOT merge multiple occurrences into one entry.
            
            10. Do not modify the input text, normalize values, or insert placeholders.
            
            11. The response MUST be valid JSON. If you cannot produce valid JSON, return [].
            
            Return only the JSON array.
            """;

    private final GroqLlmClient llmClient;
    private final ObjectMapper objectMapper;

    private final int maxRetries;
    private final Duration retryBackoff;

    public PiiDetectService(GroqLlmClient llmClient,
                            ObjectMapper objectMapper,
                            @Value("${llm.max-retries:2}") int maxRetries,
                            @Value("${llm.retry-backoff-ms:500}") long retryBackoffMs) {
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
        this.maxRetries = Math.max(0, maxRetries);
        this.retryBackoff = Duration.ofMillis(Math.max(0, retryBackoffMs));
    }

    public List<SensitiveDataItem> detect(PiiDetectRequest request) {
        String requestId = request.getRequestId();
        String doc = Optional.ofNullable(request.getDocumentExtractedContent()).orElse("");
        String prompt = Optional.ofNullable(request.getUserPrompt()).orElse("");

        // Per story: detect separately on each source and combine.
        // Safe fallback: if either fails validation -> []
        var docRes = callParseValidateWithRetry(requestId, "document", doc);
        if (docRes.failed) {
            log.debug("pii-detect requestId={} stage=document_parse_failed errorCode={} -> fallback=[]", requestId, docRes.errorCode);
            return List.of();
        }

        var promptRes = callParseValidateWithRetry(requestId, "prompt", prompt);
        if (promptRes.failed) {
            log.debug("pii-detect requestId={} stage=prompt_parse_failed errorCode={} -> fallback=[]", requestId, promptRes.errorCode);
            return List.of();
        }

        List<SensitiveDataItem> combined = new ArrayList<>(docRes.items.size() + promptRes.items.size());
        combined.addAll(docRes.items);
        combined.addAll(promptRes.items);

        log.info("pii-detect requestId={} docEntities={} promptEntities={} totalEntities={}",
                requestId, docRes.items.size(), promptRes.items.size(), combined.size());
        return combined;
    }

    private record ParseResult(boolean failed, String errorCode, List<SensitiveDataItem> items) {
    }

    private ParseResult callParseValidateWithRetry(String requestId, String source, String text) {
        if (text == null || text.isBlank()) {
            return new ParseResult(false, null, List.of());
        }

        Exception lastEx = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                String raw = llmClient.chatRaw(SYSTEM_PROMPT, text).block();
                if (raw == null) {
                    return new ParseResult(true, "LLM_EMPTY_RESPONSE", List.of());
                }

                List<SensitiveDataItem> items = parseGroqResponseToItems(raw, text, source, requestId);
                // parseGroqResponseToItems returns null to indicate "fallback"-worthy failure
                if (items == null) {
                    return new ParseResult(true, "LLM_INVALID_OR_VALIDATION_FAILED", List.of());
                }
                return new ParseResult(false, null, items);
            } catch (Exception ex) {
                lastEx = ex;
                log.warn("pii-detect requestId={} source={} llm_call_failed attempt={}/{} err={}", requestId, source, attempt + 1, maxRetries + 1, ex.toString());
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(retryBackoff.toMillis());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        log.warn("pii-detect requestId={} source={} llm_failed_after_retries lastErr={}", requestId, source, lastEx == null ? null : lastEx.toString());
        return new ParseResult(true, "LLM_CALL_FAILED", List.of());
    }

    private List<SensitiveDataItem> parseGroqResponseToItems(String groqResponseJson, String sourceText, String sourceLabel, String requestId) {
        // Groq returns OpenAI-like response shape; we must extract choices[0].message.content
        String content;
        try {
            JsonNode root = objectMapper.readTree(groqResponseJson);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) return null;
            JsonNode message = choices.get(0).path("message");
            content = message.path("content").asText(null);
            if (content == null) return null;
        } catch (JsonProcessingException e) {
            // Sometimes we might be directly returned the array (e.g. mocked WireMock). Support that too.
            content = groqResponseJson;
        }

        // Content should be JSON array; if wrapped, extract first array substring.
        JsonNode arrayNode;
        try {
            arrayNode = objectMapper.readTree(content);
            if (!arrayNode.isArray()) {
                String extracted = extractFirstJsonArray(content);
                if (extracted == null) return null;
                arrayNode = objectMapper.readTree(extracted);
                if (!arrayNode.isArray()) return null;
            }
        } catch (Exception e) {
            String extracted = extractFirstJsonArray(content);
            if (extracted == null) return null;
            try {
                arrayNode = objectMapper.readTree(extracted);
                if (!arrayNode.isArray()) return null;
            } catch (Exception ex) {
                return null;
            }
        }

        List<SensitiveDataItem> items = new ArrayList<>();
        for (JsonNode node : arrayNode) {
            // Must contain exactly type,value,start,end (model side). No extra fields allowed.
            if (!node.isObject()) return null;
            if (!(node.has("type") && node.has("value") && node.has("start") && node.has("end"))) return null;
            if (node.size() != 4) return null;

            String type = node.get("type").asText(null);
            String value = node.get("value").asText(null);
            int start = node.get("start").asInt(-1);
            int end = node.get("end").asInt(-1);

            if (!AllowedTypes.isAllowed(type)) return null;
            if (value == null) return null;
            if (start < 0 || end <= start) return null;

            // Validate value matches UTF-8 byte slice (and recover offsets deterministically if needed).
            int[] offsets = validateAndMaybeRecoverOffsets(requestId, sourceLabel, sourceText, type, value, start, end);
            if (offsets == null) return null;

            // Overlap handling later
            items.add(new SensitiveDataItem(type, value, sourceLabel, offsets[0], offsets[1]));
        }

        // Resolve overlaps deterministically per source (longest-first, then start asc)
        items = resolveOverlaps(items);
        return items;
    }

    /**
     * Returns validated byte offsets [start,end] or null (meaning: invalid -> fallback per story).
     * <p>
     * Note: Story says offsets are UTF-8 byte offsets. In practice, some LLMs return char offsets;
     * we attempt deterministic recovery to keep the endpoint useful.
     */
    private int[] validateAndMaybeRecoverOffsets(String requestId,
                                                 String sourceLabel,
                                                 String sourceText,
                                                 String type,
                                                 String value,
                                                 int start,
                                                 int end) {
        try {
            String slice = Utf8ByteUtils.sliceByUtf8ByteOffsets(sourceText, start, end);
            if (value.equals(slice)) return new int[]{start, end};

            // Recovery: some models return "character" offsets instead of bytes.
            // Convert char offsets to byte offsets and try again.
            int recoveredStart = sourceText.substring(0, Math.min(start, sourceText.length())).getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
            int recoveredEnd = sourceText.substring(0, Math.min(end, sourceText.length())).getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
            try {
                String recoveredSlice = Utf8ByteUtils.sliceByUtf8ByteOffsets(sourceText, recoveredStart, recoveredEnd);
                if (value.equals(recoveredSlice)) {
                    log.debug("pii-detect stage={} offsets_recovered_by_char_to_byte type={}", sourceLabel, type);
                    return new int[]{recoveredStart, recoveredEnd};
                }
            } catch (Exception ignored) {
            }

            // Recovery: search by value bytes (first occurrence). Only accept if aligns exactly.
            int idx = Utf8ByteUtils.indexOfUtf8Bytes(sourceText, value);
            if (idx >= 0) {
                int recoveredEndBySearch = idx + value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
                try {
                    String recoveredSlice = Utf8ByteUtils.sliceByUtf8ByteOffsets(sourceText, idx, recoveredEndBySearch);
                    if (value.equals(recoveredSlice)) {
                        log.debug("pii-detect stage={} offsets_recovered_by_search type={}", sourceLabel, type);
                        return new int[]{idx, recoveredEndBySearch};
                    }
                } catch (Exception ignored) {
                }
                log.debug("pii-detect stage={} offsets_recovered_by_search type={}", sourceLabel, type);
            }

            log.debug("pii-detect stage={} value_mismatch type={} start={} end={}", sourceLabel, type, start, end);
            return null;
        } catch (Exception e) {
            log.debug("pii-detect stage={} invalid_offsets type={} start={} end={} err={}", sourceLabel, type, start, end, e.toString());
            return null;
        }
    }

    private List<SensitiveDataItem> resolveOverlaps(List<SensitiveDataItem> items) {
        if (items.isEmpty()) return items;

        // longest-first then start asc -> deterministic
        List<SensitiveDataItem> sorted = new ArrayList<>(items);
        sorted.sort(Comparator
                .<SensitiveDataItem>comparingInt(i -> (i.getEnd() - i.getStart())).reversed()
                .thenComparingInt(SensitiveDataItem::getStart)
                .thenComparing(SensitiveDataItem::getType));

        List<SensitiveDataItem> chosen = new ArrayList<>();
        for (SensitiveDataItem cand : sorted) {
            boolean overlaps = false;
            for (SensitiveDataItem existing : chosen) {
                if (rangesOverlap(cand.getStart(), cand.getEnd(), existing.getStart(), existing.getEnd())) {
                    overlaps = true;
                    break;
                }
            }
            if (!overlaps) chosen.add(cand);
        }

        // Return in ascending start order (more natural)
        chosen.sort(Comparator.comparingInt(SensitiveDataItem::getStart));
        return chosen;
    }

    private boolean rangesOverlap(int aStart, int aEnd, int bStart, int bEnd) {
        return aStart < bEnd && bStart < aEnd;
    }

    private String extractFirstJsonArray(String raw) {
        int idx = raw.indexOf('[');
        if (idx < 0) return null;
        int depth = 0;
        for (int i = idx; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') {
                depth--;
                if (depth == 0) return raw.substring(idx, i + 1);
            }
        }
        return null;
    }
}



