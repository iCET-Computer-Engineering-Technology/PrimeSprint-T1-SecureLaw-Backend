package com.primesprint.pii.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.primesprint.pii.client.GroqLlmClient;
import com.primesprint.pii.dto.PiiDetectRequest;
import com.primesprint.pii.dto.SensitiveDataItem;
import com.primesprint.pii.util.AllowedTypes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

@Service
public class PiiDetectService {
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
               Amounts may include currency labels as part of the sensitive span (e.g., "450,000 LKR", "LKR 450,000").
               Alphanumeric IDs may include letters, digits, and separators (e.g., "DC-2026-7781", "AGR-2024-4451").
            
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
        String doc = Optional.ofNullable(request.getDocumentExtractedContent()).orElse("");
        String prompt = Optional.ofNullable(request.getUserPrompt()).orElse("");

        var docRes = callParseValidateWithRetry("document", doc);
        var promptRes = callParseValidateWithRetry("prompt", prompt);

        List<SensitiveDataItem> combined = new ArrayList<>();
        if (!docRes.failed) combined.addAll(docRes.items);
        if (!promptRes.failed) combined.addAll(promptRes.items);

        combined = dedupeBySpanKey(combined);
        combined = resolveOverlaps(combined);
        return combined;
    }

    private ParseResult callParseValidateWithRetry(String source, String text) {
        if (text == null || text.isBlank()) {
            return new ParseResult(false, null, List.of());
        }

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                String raw = llmClient.chatRaw(SYSTEM_PROMPT, text).block();
                if (raw == null) {
                    return new ParseResult(true, "LLM_EMPTY_RESPONSE", List.of());
                }

                List<SensitiveDataItem> items = parseGroqResponseToItems(raw, text, source);
                if (items == null) {
                    return new ParseResult(true, "LLM_INVALID_OR_VALIDATION_FAILED", List.of());
                }
                return new ParseResult(false, null, items);
            } catch (Exception ex) {
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
        return new ParseResult(true, "LLM_CALL_FAILED", List.of());
    }

    private List<SensitiveDataItem> parseGroqResponseToItems(String groqResponseJson,
                                                             String sourceText,
                                                             String sourceLabel) {
        String content;
        try {
            JsonNode root = objectMapper.readTree(groqResponseJson);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) return null;
            JsonNode message = choices.get(0).path("message");
            content = message.path("content").asText(null);
            if (content == null) return null;
        } catch (JsonProcessingException e) {
            content = groqResponseJson;
        }

        JsonNode arrayNode;
        try {
            arrayNode = objectMapper.readTree(content);
            if (!arrayNode.isArray()) {
                String extracted = extractFirstJsonArray(stripMarkdownCodeFences(content));
                if (extracted == null) return null;
                arrayNode = objectMapper.readTree(extracted);
                if (!arrayNode.isArray()) return null;
            }
        } catch (Exception e) {
            String extracted = extractFirstJsonArray(stripMarkdownCodeFences(content));
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
            if (!node.isObject()) {
                continue;
            }
            if (!(node.has("type") && node.has("value") && node.has("start") && node.has("end"))) {
                continue;
            }
            String type = node.get("type").asText(null);
            if (type != null) type = type.trim().toUpperCase(Locale.ROOT);

            String value = node.get("value").asText(null);
            if (value != null) value = value.trim();
            int start = node.get("start").asInt(-1);
            int end = node.get("end").asInt(-1);

            if (!AllowedTypes.isAllowed(type)) {
                continue;
            }
            if (value == null) {
                continue;
            }
            if (start < 0 || end <= start) {
                continue;
            }

            int[] offsets = validateCharacterOffsets(sourceText, value, start, end);

            if (offsets == null) {
                List<int[]> recoveredSpans = recoverSpansBySearchingValue(sourceText, value);
                if (!recoveredSpans.isEmpty()) {
                    for (int[] span : recoveredSpans) {
                        items.add(new SensitiveDataItem(type, value, sourceLabel, span[0], span[1]));
                    }
                    continue;
                }

                int[] recovered = tryRecoverCommonTruncatedSpan(sourceText, value, start, end);
                if (recovered != null) {
                    items.add(new SensitiveDataItem(type, sourceText.substring(recovered[0], recovered[1]), sourceLabel, recovered[0], recovered[1]));
                    continue;
                }
                continue;
            }

            items.add(new SensitiveDataItem(type, value, sourceLabel, offsets[0], offsets[1]));
        }
        items = expandDuplicateSpans(sourceText, items);
        items = dedupeBySpanKey(items);
        return resolveOverlaps(items);
    }

    private String stripMarkdownCodeFences(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.startsWith("```")) {
            int firstLineEnd = t.indexOf('\n');
            if (firstLineEnd > 0) {
                t = t.substring(firstLineEnd + 1);
            } else {
                t = t.substring(3);
            }
            int endFence = t.lastIndexOf("```");
            if (endFence >= 0) {
                t = t.substring(0, endFence);
            }
            return t.trim();
        }
        return s;
    }

    private List<int[]> recoverSpansBySearchingValue(String sourceText, String value) {
        if (sourceText == null || sourceText.isEmpty()) return List.of();
        if (value == null || value.isEmpty()) return List.of();
        List<int[]> spans = new ArrayList<>();
        int from = 0;
        while (from <= sourceText.length()) {
            int idx = sourceText.indexOf(value, from);
            if (idx < 0) break;
            spans.add(new int[]{idx, idx + value.length()});
            from = idx + 1;
        }
        return spans;
    }

    private List<SensitiveDataItem> expandDuplicateSpans(String sourceText, List<SensitiveDataItem> items) {
        if (items == null || items.isEmpty()) return items;
        List<SensitiveDataItem> expanded = new ArrayList<>();
        for (SensitiveDataItem item : items) {
            String value = item.getValue();
            if (value == null || value.isEmpty() || sourceText == null || sourceText.isEmpty()) {
                expanded.add(item);
                continue;
            }

            int from = 0;
            while (from <= sourceText.length()) {
                int idx = sourceText.indexOf(value, from);
                if (idx < 0) break;

                int end = idx + value.length();
                expanded.add(new SensitiveDataItem(item.getType(), value, item.getSource(), idx, end));
                from = idx + 1;
            }
        }
        return expanded;
    }

    private List<SensitiveDataItem> dedupeBySpanKey(List<SensitiveDataItem> items) {
        if (items == null || items.isEmpty()) return items;
        Set<String> seen = new HashSet<>(items.size() * 2);
        List<SensitiveDataItem> out = new ArrayList<>(items.size());
        for (SensitiveDataItem i : items) {
            String key = i.getType() + "|" + i.getSource() + "|" + i.getStart() + "|" + i.getEnd();
            if (seen.add(key)) out.add(i);
        }
        return out;
    }

    private int[] validateCharacterOffsets(String sourceText, String value, int start, int end) {
        try {
            if (sourceText == null) return null;
            if (start < 0 || end < 0 || start >= end) return null;
            if (start > sourceText.length() || end > sourceText.length()) return null;

            String extracted = sourceText.substring(start, end);
            if (!extracted.equals(value)) {
                return null;
            }
            return new int[]{start, end};
        } catch (Exception e) {
            return null;
        }
    }

    private int[] tryRecoverCommonTruncatedSpan(String sourceText, String value, int start, int end) {
        if (sourceText == null || value == null) return null;

        if (!sourceText.contains(value)) return null;

        String safeValue = java.util.regex.Pattern.quote(value);

        boolean isNumericAmount = value.matches("^[0-9]{2,}(?:[.,][0-9]{2,})?$");
        if (isNumericAmount) {
            java.util.regex.Pattern amountPattern = java.util.regex.Pattern.compile("^(?:\\s*([A-Z]{2,4})\\s+)?" + safeValue + "(?:\\s+([A-Z]{2,4}))?\\b");

            int windowStart = Math.max(0, start - 8);
            int windowEnd = Math.min(sourceText.length(), end + 8);
            if (windowStart < windowEnd) {
                String window = sourceText.substring(windowStart, windowEnd);
                java.util.regex.Matcher m = amountPattern.matcher(window);
                if (m.find()) {
                    int candStart = windowStart + m.start();
                    int candEnd = windowStart + m.end();
                    if (candStart <= start && candEnd >= end) {
                        return new int[]{candStart, candEnd};
                    }
                }
            }
        }

        if (value.matches(".*[A-Za-z].*") || value.contains("-") || value.contains("_") || value.contains("/") || value.contains(":") || value.contains(".")) {
            int len = sourceText.length();
            int left = Math.min(Math.max(0, start), len);
            int right = Math.min(Math.max(0, end), len);

            while (left > 0) {
                char c = sourceText.charAt(left - 1);
                if (Character.isLetterOrDigit(c) || c == '-' || c == '_' || c == '/' || c == ':' || c == '.') {
                    left--;
                } else {
                    break;
                }
            }
            while (right < len) {
                char c = sourceText.charAt(right);
                if (Character.isLetterOrDigit(c) || c == '-' || c == '_' || c == '/' || c == ':' || c == '.') {
                    right++;
                } else {
                    break;
                }
            }

            if (left < right) {
                String candidate = sourceText.substring(left, right);
                if (candidate.contains(value) && candidate.length() > value.length()) {
                    return new int[]{left, right};
                }
            }
        }

        return null;
    }

    private List<SensitiveDataItem> resolveOverlaps(List<SensitiveDataItem> items) {
        if (items.isEmpty()) return items;

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

    private record ParseResult(boolean failed, String errorCode, List<SensitiveDataItem> items) {
    }
}
