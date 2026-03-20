package com.primesprint.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.primesprint.client.GroqLlmClient;
import com.primesprint.model.dto.SensitiveDataItem;
import com.primesprint.model.dto.request.PiiDetectRequest;
import com.primesprint.service.PiiDetectService;
import com.primesprint.util.AllowedTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PiiDetectServiceImpl implements PiiDetectService {
    private static final Logger log = LoggerFactory.getLogger(PiiDetectServiceImpl.class);

    private static final String SYSTEM_PROMPT = """
            You are a strict PII extraction engine.
            
            Your task is to extract sensitive entities from input text with exact character positions.
            
            Return ONLY a valid JSON array. No explanations, markdown, or additional text.
            
            Output format:
            [
              { "type": "<CATEGORY>", "value": "<exact substring>", "start": <int>, "end": <int> }
            ]
            
            CRITICAL RULES:
            
            1. Allowed types ONLY:
            PERSON, ORGANIZATION, CASE_ID, ADDRESS, EMAIL, PHONE,
            NATIONAL_ID, PASSPORT, DRIVER_LICENSE, BANK_ACCOUNT,
            CARD_NUMBER, CONTRACT_REF, EVIDENCE_ID, AMOUNT.
            
            2. VALUE MUST BE EXACT:
            - Must exactly match substring from input
            - No trimming, no normalization
            - start/end must match exactly
            
            3. INDEXING:
            - 0-based
            - end is exclusive
            - substring = text[start:end]
            
            4. NO DUPLICATES:
            - Same span → only once
            - Same value in different positions → separate entries
            
            5. TYPE CLASSIFICATION RULES (STRICT):
            
            CASE_ID:
            - Legal or court case references
            - Prefixes: DC, CR, HC
            - Examples: "DC-2026-0042", "CR-2024-8891"
            
            CONTRACT_REF:
            - Agreements, contracts, procurement references
            - Prefixes: AGR, CNT
            - Examples: "AGR-2025-7788", "CNT-2023-1122"
            
            PERSON:
            - Extract ONLY the person's full name
            - DO NOT include titles or prefixes (Mr., Mrs., Ms., Dr., Prof., Hon.)
            - Example: "John Silva" ✅, "Mr. John Silva" ❌
            
            AMOUNT:
            - Include currency if present: "450000 LKR", "LKR 450,000", "USD 1,200.50"
            - Do NOT split number and currency
            
            CARD_NUMBER vs BANK_ACCOUNT:
            - CARD_NUMBER: usually formatted with spaces or dashes between digit groups
              e.g., "4111-1111-1111-1111"
            - BANK_ACCOUNT: long continuous numeric sequences, often 10–18 digits, no card-like formatting
            - Do not classify plain long digits as CARD_NUMBER unless formatted like a card
            
            6. EXTRACTION RULES:
            - Only extract if confident and exact
            - Do NOT guess types
            - Do NOT merge spans
            - Do NOT include labels like "Email:", "Phone:", "Name:"
            - Preserve punctuation, casing, spacing
            
            7. OUTPUT:
            - MUST be valid JSON array
            - If unsure → return []
            
            Return only the JSON array.
            """;

    private final GroqLlmClient llmClient;
    private final ObjectMapper objectMapper;

    private final int maxRetries;
    private final Duration retryBackoff;

    public PiiDetectServiceImpl(GroqLlmClient llmClient,
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

        // Deterministic fallback for structured entities. This reduces reliance on the LLM
        // for high-confidence patterns (amounts, IDs, card numbers, etc.).
        // Offsets are character offsets derived from Matcher.start()/end().
        List<SensitiveDataItem> deterministic = extractStructuredEntities(source, text);

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                String raw = llmClient.chatRaw(SYSTEM_PROMPT, text).block();
                if (raw == null) {
                    // LLM failed; still return deterministic results as best-effort.
                    return new ParseResult(false, null, resolveOverlaps(dedupeBySpanKey(deterministic)));
                }

                List<SensitiveDataItem> items = parseGroqResponseToItems(raw, text, source);
                if (items == null) {
                    // Invalid JSON from LLM; still return deterministic results as best-effort.
                    return new ParseResult(false, null, resolveOverlaps(dedupeBySpanKey(deterministic)));
                }
                // Merge deterministic + LLM items, then dedupe/overlap resolution.
                List<SensitiveDataItem> merged = new ArrayList<>(deterministic.size() + items.size());
                merged.addAll(deterministic);
                merged.addAll(items);
                merged = dedupeBySpanKey(merged);
                merged = resolveOverlaps(merged);
                return new ParseResult(false, null, merged);
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

        // LLM retries exhausted; still return deterministic results as best-effort.
        return new ParseResult(false, null, resolveOverlaps(dedupeBySpanKey(deterministic)));
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
            // LLM response parsing happens here.
            // Skip malformed entities (best-effort); do not fail the whole response.
            if (!node.isObject()) {
                log.debug("pii-detect requestId=? source={} node_not_object -> skipping node", sourceLabel);
                continue;
            }
            if (!(node.has("type") && node.has("value") && node.has("start") && node.has("end"))) {
                log.debug("pii-detect requestId=? source={} missing_fields -> skipping node", sourceLabel);
                continue;
            }

            // Note: do NOT trim/normalize 'value' (contract requires exact substring match).
            // We only normalize 'type' to match AllowedTypes.
            String type = node.get("type").asText(null);
            if (type != null) type = type.trim().toUpperCase(Locale.ROOT);

            String value = node.get("value").asText(null);
            int start = node.get("start").asInt(-1);
            int end = node.get("end").asInt(-1);

            if (!AllowedTypes.isAllowed(type)) {
                log.debug("pii-detect requestId=? source={} unknown_type={} -> skipping", sourceLabel, type);
                continue;
            }
            if (value == null) {
                log.debug("pii-detect requestId=? source={} null_value -> skipping", sourceLabel);
                continue;
            }
            if (start < 0 || end <= start) {
                log.debug("pii-detect requestId=? source={} invalid_offsets start={} end={} -> skipping", sourceLabel, start, end);
                continue;
            }

            // Span validation occurs here (character offsets only).
            int[] offsets = validateCharacterOffsets(sourceText, value, start, end);

            if (offsets == null) {
                // Fallback 1: recover by searching for value literal in sourceText.
                List<int[]> recoveredSpans = recoverSpansBySearchingValue(sourceText, value);
                if (!recoveredSpans.isEmpty()) {
                    for (int[] span : recoveredSpans) {
                        items.add(new SensitiveDataItem(type, value, sourceLabel, span[0], span[1]));
                    }
                    continue;
                }

                // Fallback 2: safe narrow heuristic for common truncated amount/currency spans.
                int[] recovered = tryRecoverCommonTruncatedSpan(sourceText, value, start, end);
                if (recovered != null) {
                    items.add(new SensitiveDataItem(type,
                            sourceText.substring(recovered[0], recovered[1]),
                            sourceLabel,
                            recovered[0],
                            recovered[1]));
                    continue;
                }

                log.debug("pii-detect requestId=? source={} value_mismatch type={} start={} end={} valueLen={} -> skipping",
                        sourceLabel, type, start, end, value.length());
                continue;
            }

            items.add(new SensitiveDataItem(type, value, sourceLabel, offsets[0], offsets[1]));
        }

        // Post-processing safeguard (LLM-aligned):
        // If the LLM labels a plain digit-only long sequence as CARD_NUMBER, relabel to BANK_ACCOUNT.
        // This does not change offsets or values; it only corrects the type.
        // Rationale: card numbers are typically formatted with spaces/dashes; long unformatted digit sequences
        // are more consistent with bank account identifiers in our domain.
        items = relabelDigitOnlyCardNumbers(items);

        // Duplicate expansion happens here: LLM often returns only one occurrence even if the
        // same sensitive value appears multiple times.
        items = expandDuplicateSpans(sourceText, items);
        items = dedupeBySpanKey(items);
        return resolveOverlaps(items);
    }

    private List<SensitiveDataItem> relabelDigitOnlyCardNumbers(List<SensitiveDataItem> items) {
        if (items == null || items.isEmpty()) return items;
        List<SensitiveDataItem> out = new ArrayList<>(items.size());
        for (SensitiveDataItem i : items) {
            if (i == null) continue;
            if (!"CARD_NUMBER".equals(i.getType())) {
                out.add(i);
                continue;
            }
            String v = i.getValue();
            if (v == null) {
                out.add(i);
                continue;
            }

            // Only digits? (no spaces/dashes). Keep it lightweight and non-regex to avoid adding classification rules.
            boolean allDigits = true;
            for (int p = 0; p < v.length(); p++) {
                char c = v.charAt(p);
                if (!Character.isDigit(c)) {
                    allDigits = false;
                    break;
                }
            }

            if (allDigits) {
                // Relabel. Keep everything else the same.
                out.add(new SensitiveDataItem("BANK_ACCOUNT", v, i.getSource(), i.getStart(), i.getEnd()));
            } else {
                out.add(i);
            }
        }
        return out;
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
                // PII-safe logging: never log raw values/substrings.
                log.debug("pii-detect stage=value_mismatch start={} end={} extractedLen={} valueLen={}",
                        start, end,
                        extracted.length(),
                        value == null ? 0 : value.length());
                return null;
            }
            return new int[]{start, end};
        } catch (Exception e) {
            log.debug("pii-detect stage=validate_offsets_exception start={} end={} err={}", start, end, e.toString());
            return null;
        }
    }

    private int[] tryRecoverCommonTruncatedSpan(String sourceText, String value, int start, int end) {
        // Narrow, safe heuristic: numeric value followed by a currency code suffix.
        // Example: model value "450000" but source "450000 LKR".
        if (sourceText == null || value == null) return null;
        if (!value.matches("^\\d{2,}(?:[.,]\\d{2,})?$")) return null;

        int lookStart = Math.max(0, end);
        int lookEnd = Math.min(sourceText.length(), end + 8);
        if (lookStart >= lookEnd) return null;

        String tail = sourceText.substring(lookStart, lookEnd);
        Matcher m = Pattern.compile("^\\s+([A-Z]{2,4})\\b").matcher(tail);
        if (!m.find()) return null;

        int newEnd = end + m.end();
        if (newEnd <= end || newEnd > sourceText.length()) return null;

        String candidate = sourceText.substring(start, newEnd);
        if (candidate.startsWith(value) && candidate.length() > value.length()) {
            return new int[]{start, newEnd};
        }
        return null;
    }

    /**
     * Deterministic extraction stage for structured entities.
     * This is intentionally conservative (high precision) and uses character offsets only.
     */
    private List<SensitiveDataItem> extractStructuredEntities(String sourceLabel, String text) {
        if (text == null || text.isBlank()) return List.of();

        List<SensitiveDataItem> out = new ArrayList<>();
        out.addAll(detectAmounts(sourceLabel, text));
        out.addAll(detectNationalIds(sourceLabel, text));
        out.addAll(detectCardNumbers(sourceLabel, text));
        out.addAll(detectBankAccounts(sourceLabel, text));
        out.addAll(detectContractRefs(sourceLabel, text));

        // Keep deterministic output stable.
        out = dedupeBySpanKey(out);
        out = resolveOverlaps(out);
        return out;
    }

    /**
     * AMOUNT patterns (character offsets):
     * - "450,000 LKR"
     * - "LKR 450,000"
     * - "120000 USD"
     * - "USD 1,200.50"
     */
    private List<SensitiveDataItem> detectAmounts(String sourceLabel, String text) {
        // Currency code (ISO-ish). Keep narrow 3 letters; extendable later.
        String ccy = "(?:LKR|USD|EUR|GBP|AUD|CAD|INR|JPY|SGD|NZD)";
        // Numeric amount: 1,200 or 1200 or 1,200.50 or 1200.50
        String num = "(?:\\d{1,3}(?:,\\d{3})+|\\d+)(?:\\.\\d{2})?";

        Pattern suffix = Pattern.compile("\\b" + num + "\\s+" + ccy + "\\b");
        Pattern prefix = Pattern.compile("\\b" + ccy + "\\s+" + num + "\\b");

        List<SensitiveDataItem> out = new ArrayList<>();
        findAllMatches(out, "AMOUNT", sourceLabel, text, prefix);
        findAllMatches(out, "AMOUNT", sourceLabel, text, suffix);
        return out;
    }

    /**
     * NATIONAL_ID fallback.
     * Implement formats commonly used in typical tests:
     * - 9 digits + V/X (Sri Lanka old NIC): 123456789V / 123456789X (case-insensitive)
     * - 12 digits (Sri Lanka new NIC): 200012345678
     */
    private List<SensitiveDataItem> detectNationalIds(String sourceLabel, String text) {
        Pattern oldNic = Pattern.compile("\\b\\d{9}[VvXx]\\b");
        Pattern newNic = Pattern.compile("\\b\\d{12}\\b");

        List<SensitiveDataItem> out = new ArrayList<>();
        findAllMatches(out, "NATIONAL_ID", sourceLabel, text, oldNic);
        findAllMatches(out, "NATIONAL_ID", sourceLabel, text, newNic);
        return out;
    }

    /**
     * CARD_NUMBER fallback: detect 13-19 digit sequences allowing spaces or dashes.
     * We keep this conservative: requires at least 13 digits total.
     */
    private List<SensitiveDataItem> detectCardNumbers(String sourceLabel, String text) {
        Pattern p = Pattern.compile("\\b(?:\\d[ -]?){13,19}\\d\\b");
        List<SensitiveDataItem> out = new ArrayList<>();
        Matcher m = p.matcher(text);
        while (m.find()) {
            int s = m.start();
            int e = m.end();
            String candidate = text.substring(s, e);
            int digits = 0;
            for (int i = 0; i < candidate.length(); i++) {
                if (Character.isDigit(candidate.charAt(i))) digits++;
            }
            if (digits < 13) continue;
            out.add(new SensitiveDataItem("CARD_NUMBER", candidate, sourceLabel, s, e));
        }
        return out;
    }

    /**
     * BANK_ACCOUNT fallback: conservative IBAN-like or long digit sequences.
     * This is intentionally narrow to avoid false positives.
     */
    private List<SensitiveDataItem> detectBankAccounts(String sourceLabel, String text) {
        List<SensitiveDataItem> out = new ArrayList<>();

        // IBAN-like: 2 letters + 2 digits + 10-30 alnum
        Pattern iban = Pattern.compile("\\b[A-Z]{2}\\d{2}[A-Z0-9]{10,30}\\b", Pattern.CASE_INSENSITIVE);
        findAllMatches(out, "BANK_ACCOUNT", sourceLabel, text, iban);

        // Generic long account number: 10-18 digits (word boundaries)
        Pattern digits = Pattern.compile("\\b\\d{10,18}\\b");
        findAllMatches(out, "BANK_ACCOUNT", sourceLabel, text, digits);

        return out;
    }

    /**
     * CONTRACT_REF fallback: common "ABC-2024-1234" / "AGR-2025-0001" patterns.
     */
    private List<SensitiveDataItem> detectContractRefs(String sourceLabel, String text) {
        Pattern p = Pattern.compile("\\b[A-Z]{2,6}-\\d{4}-\\d{2,8}\\b");
        List<SensitiveDataItem> out = new ArrayList<>();
        findAllMatches(out, "CONTRACT_REF", sourceLabel, text, p);
        return out;
    }

    private void findAllMatches(List<SensitiveDataItem> out,
                                String type,
                                String sourceLabel,
                                String text,
                                Pattern pattern) {
        if (out == null || text == null || text.isEmpty() || pattern == null) return;
        Matcher m = pattern.matcher(text);
        while (m.find()) {
            int s = m.start();
            int e = m.end();
            if (s < 0 || e <= s || e > text.length()) continue;
            String value = text.substring(s, e);
            // Ensure contract: value must equal substring.
            if (validateCharacterOffsets(text, value, s, e) != null) {
                out.add(new SensitiveDataItem(type, value, sourceLabel, s, e));
            }
        }
    }

    private List<SensitiveDataItem> resolveOverlaps(List<SensitiveDataItem> items) {
        if (items.isEmpty()) return items;

        List<SensitiveDataItem> sorted = new ArrayList<>(items);
        // Overlap resolution:
        // 1) Prefer higher-confidence structured types first (policy),
        // 2) then prefer longer spans,
        // 3) then stable ordering by start/type.
        sorted.sort(Comparator
                .comparingInt((SensitiveDataItem i) -> typePriority(i.getType()))
                .thenComparing(Comparator
                        .<SensitiveDataItem>comparingInt(i -> (i.getEnd() - i.getStart())).reversed())
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

    /**
     * Lower number means higher priority when resolving overlaps.
     * Structured identifiers are preferred over broad/ambiguous spans.
     */
    private int typePriority(String type) {
        if (type == null) return 100;
        return switch (type) {
            case "NATIONAL_ID", "PASSPORT", "DRIVER_LICENSE" -> 0;
            case "CARD_NUMBER" -> 1;
            case "BANK_ACCOUNT" -> 2;
            case "AMOUNT" -> 3;
            case "CONTRACT_REF", "CASE_ID", "EVIDENCE_ID" -> 4;
            case "EMAIL", "PHONE" -> 5;
            case "PERSON", "ORGANIZATION" -> 6;
            case "ADDRESS" -> 7;
            default -> 50;
        };
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
