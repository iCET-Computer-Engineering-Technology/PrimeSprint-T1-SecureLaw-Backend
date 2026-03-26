package com.primesprint.service.impl;

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
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PiiDetectServiceImpl implements PiiDetectService {
    private static final Logger log = LoggerFactory.getLogger(PiiDetectServiceImpl.class);

    private static final String TYPE_CARD_NUMBER = "CARD_NUMBER";
    private static final String TYPE_BANK_ACCOUNT = "BANK_ACCOUNT";
    private static final String TYPE_AMOUNT = "AMOUNT";

    private static final String SYSTEM_PROMPT = """
            You are a strict PII extraction engine.
            
            Your task is to extract sensitive entities from input text with exact character positions.
            
            Return ONLY a valid JSON array. No explanations, markdown, or additional text.
            
            Output format:
            [
              { "type": "<CATEGORY>", "value": "<exact substring>", "start": <int>, "end": <int> }
            ]
            
            CRITICAL RULES:
            
            1. Allowed types ONLY (extract *all* that appear in the text):
            PERSON, ORGANIZATION, CASE_ID, ADDRESS, EMAIL, PHONE,
            NATIONAL_ID, PASSPORT, DRIVER_LICENSE, BANK_ACCOUNT,
            CARD_NUMBER, CONTRACT_REF, EVIDENCE_ID, AMOUNT.
            
            2. VALUE MUST BE EXACT:
            - Must exactly match substring from input
            - No trimming, no normalization
            - start/end must match exactly

            IMPORTANT:
            - Offsets are character offsets relative to the provided input text ONLY.
            - Do not edit the value string. The value must be exactly text[start:end].
            - If you cannot provide exact offsets/value, omit the entity.
            
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

    @Override
    public List<SensitiveDataItem> detect(PiiDetectRequest request) {
        long t0 = System.nanoTime();
        String requestId = safeRequestId(request);
        String doc = Optional.ofNullable(request.getDocumentExtractedContent()).orElse("");
        String prompt = Optional.ofNullable(request.getUserPrompt()).orElse("");

        var docRes = callParseValidateWithRetry(requestId, "document", doc);
        var promptRes = callParseValidateWithRetry(requestId, "prompt", prompt);

        log.debug("pii-detect requestId={} stage=llm_results_count docItems={} promptItems={} docFailed={} promptFailed={}",
                requestId,
                docRes.items == null ? 0 : docRes.items.size(),
                promptRes.items == null ? 0 : promptRes.items.size(),
                docRes.failed,
                promptRes.failed);

        List<SensitiveDataItem> combined = new ArrayList<>();
        if (!docRes.failed) combined.addAll(docRes.items == null ? List.of() : docRes.items);
        if (!promptRes.failed) combined.addAll(promptRes.items == null ? List.of() : promptRes.items);

        // 1) Remove duplicates where same type, source, and value (ignore start/end)
        combined = dedupeByTypeValueSource(combined);

        // 2) Resolve overlaps within each source (type-aware, deterministic)
        combined = resolveOverlapsPerSource(combined);

        // 3) Enforce strict separation rules for numeric-like categories.
        //    PHONE/CARD_NUMBER must never be represented as BANK_ACCOUNT.
        combined = enforceStrictTypeSeparationPerSource(combined);

        // IMPORTANT (masking contract): Never modify detected values in the API response.
        // We only remove exact duplicate spans; distinct positions must always survive.

        long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
        log.info("pii-detect requestId={} stage=done totalItems={} ms={}", requestId, combined.size(), ms);
        return combined;
    }

    // NOTE: Internal normalization helpers are intentionally kept for comparison/classification only.
    // They MUST NOT be used to mutate values returned in the API response.

    private String normalizePhone(String s) {
        if (s == null || s.isEmpty()) return "";
        String trimmed = s.trim();
        StringBuilder sb = new StringBuilder(trimmed.length());
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (Character.isDigit(c)) {
                sb.append(c);
            } else if (c == '+' && sb.isEmpty()) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private ParseResult callParseValidateWithRetry(String requestId, String source, String text) {
        if (text == null || text.isBlank()) {
            return ParseResult.ok(List.of());
        }

        // Deterministic fallback for structured entities. This reduces reliance on the LLM
        // for high-confidence patterns (amounts, IDs, card numbers, etc.).
        // Offsets are character offsets derived from Matcher.start()/end().
        List<SensitiveDataItem> deterministic = extractStructuredEntities(source, text);

        // Noise control: keep high-signal warnings/errors and final summary at INFO.
        // Start/parse telemetry remains available at DEBUG for troubleshooting.
        log.debug("pii-detect requestId={} source={} stage=start textLen={} deterministicCount={}",
                requestId, source, text.length(), deterministic.size());

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                String raw = llmClient.chatRaw(SYSTEM_PROMPT, text).block();
                if (raw == null) {
                    // LLM failed; still return deterministic results as best-effort.
                    log.warn("pii-detect requestId={} source={} stage=llm_null_response -> using_deterministic_only", requestId, source);
                    List<SensitiveDataItem> merged = new ArrayList<>(deterministic);
                    merged = expandDuplicateSpans(text, merged);
                    merged = dedupeByTypeValueSource(merged); // <-- Use new dedup
                    merged = resolveOverlapsPerSource(merged);
                    return ParseResult.ok(merged);
                }

                List<SensitiveDataItem> items = parseGroqResponseToItems(requestId, raw, text, source);

                // Merge deterministic + LLM items, then enforce the strict order:
                // merge -> expandDuplicateSpans -> dedupe -> resolveOverlaps.
                List<SensitiveDataItem> merged = new ArrayList<>(deterministic.size() + items.size());
                merged.addAll(deterministic);
                merged.addAll(items);
                merged = expandDuplicateSpans(text, merged);
                merged = dedupeByTypeValueSource(merged); // <-- Use new dedup
                merged = resolveOverlapsPerSource(merged);
                return ParseResult.ok(merged);
            } catch (Exception ex) {
                log.warn("pii-detect requestId={} source={} stage=exception attempt={} errType={}",
                        requestId, source, attempt, ex.getClass().getSimpleName());
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
        log.warn("pii-detect requestId={} source={} stage=retries_exhausted -> using_deterministic_only", requestId, source);
        List<SensitiveDataItem> merged = new ArrayList<>(deterministic);
        merged = expandDuplicateSpans(text, merged);
        merged = dedupeByTypeValueSource(merged); // <-- Use new dedup
        merged = resolveOverlapsPerSource(merged);
        return ParseResult.ok(merged);
    }

    // Update parseGroqResponseToItems to skip invalid nodes instead of failing the entire response
    private List<SensitiveDataItem> parseGroqResponseToItems(String requestId,
                                                             String groqResponseJson,
                                                             String sourceText,
                                                             String sourceLabel) {
        long t0 = System.nanoTime();
        String content = extractGroqContentBestEffort(groqResponseJson);
        if (content == null || content.isBlank()) {
            log.warn("pii-detect requestId={} source={} stage=parse content_empty -> llmIgnored", requestId, sourceLabel);
            return List.of();
        }

        JsonNode arrayNode = tryParseJsonArray(content);
        if (arrayNode == null) {
            log.warn("pii-detect requestId={} source={} stage=parse not_json_array -> llmIgnored", requestId, sourceLabel);
            return List.of();
        }

        List<SensitiveDataItem> items = new ArrayList<>();
        int skipped = 0;
        for (JsonNode node : arrayNode) {
            Optional<List<SensitiveDataItem>> parsed = parseSingleEntityNode(requestId, sourceLabel, sourceText, node);
            if (parsed.isEmpty()) {
                skipped++;
                continue;
            }
            items.addAll(parsed.get());
        }

        long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
        log.debug("pii-detect requestId={} source={} stage=parsed items={} skippedNodes={} ms={}",
                requestId, sourceLabel, items.size(), skipped, ms);
        return items;
    }

    private String extractGroqContentBestEffort(String groqResponseJson) {
        if (groqResponseJson == null) return null;
        try {
            JsonNode root = objectMapper.readTree(groqResponseJson);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                JsonNode message = choices.get(0).path("message");
                String c = message.path("content").asText(null);
                if (c != null) return c;
                // Some variants may use "text".
                c = choices.get(0).path("text").asText(null);
                if (c != null) return c;
            }

            // 2) Some gateways return the content directly as a JSON array/object string; fall through.
        } catch (Exception ignore) {
            // Best-effort parsing: if wrapper parsing fails, treat as raw content.
        }
        return groqResponseJson;
    }

    private JsonNode tryParseJsonArray(String content) {
        if (content == null || content.isBlank()) return null;
        String raw = stripMarkdownCodeFences(content).trim();

        // 1) Direct array
        JsonNode n = tryReadTree(raw);
        if (n != null && n.isArray()) return n;

        // 2) Some LLMs return an object with a top-level field (e.g., {"entities": [...]})
        if (n != null && n.isObject()) {
            JsonNode entities = n.path("entities");
            if (entities.isArray()) return entities;
            JsonNode data = n.path("data");
            if (data.isArray()) return data;
            JsonNode sensitive = n.path("sensitiveData");
            if (sensitive.isArray()) return sensitive;
        }

        // 3) Extract first JSON array from a noisy payload.
        String extracted = extractFirstJsonArray(raw);
        if (extracted == null) return null;
        JsonNode n2 = tryReadTree(extracted);
        return (n2 != null && n2.isArray()) ? n2 : null;
    }

    private JsonNode tryReadTree(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return objectMapper.readTree(s);
        } catch (Exception ignore) {
            return null;
        }
    }

    private Optional<List<SensitiveDataItem>> parseSingleEntityNode(String requestId,
                                                                    String sourceLabel,
                                                                    String sourceText,
                                                                    JsonNode node) {
        // Best-effort: a single malformed node must not discard other nodes.
        if (node == null || !node.isObject()) {
            log.debug("pii-detect requestId={} source={} stage=parse skip=node_not_object", requestId, sourceLabel);
            return Optional.empty();
        }
        if (!(node.has("type") && node.has("value") && node.has("start") && node.has("end"))) {
            log.debug("pii-detect requestId={} source={} stage=parse skip=missing_fields", requestId, sourceLabel);
            return Optional.empty();
        }

        // Note: do NOT trim/normalize 'value' (contract requires exact substring match).
        // We only normalize 'type' to match AllowedTypes.
        String type = AllowedTypes.canonicalize(node.get("type").asText(null));
        if (!AllowedTypes.isAllowed(type)) {
            log.debug("pii-detect requestId={} source={} stage=parse skip=unknown_type type={}", requestId, sourceLabel, type);
            return Optional.empty();
        }

        String value = node.get("value").asText(null);
        int start = node.get("start").asInt(-1);
        int end = node.get("end").asInt(-1);
        if (value == null) {
            log.debug("pii-detect requestId={} source={} stage=parse skip=null_value", requestId, sourceLabel);
            return Optional.empty();
        }
        if (start < 0 || end <= start) {
            log.debug("pii-detect requestId={} source={} stage=parse skip=invalid_offsets start={} end={}", requestId, sourceLabel, start, end);
            return Optional.empty();
        }

        // Span validation occurs here (character offsets only).
        if (isValidOffsets(sourceText, value, start, end)) {
            return Optional.of(List.of(new SensitiveDataItem(type, value, sourceLabel, start, end)));
        }

        // Fallback: recover ONLY by searching for exact literal occurrences (no value mutation).
        // This keeps the strict contract (value == substring), while tolerating LLM offset mistakes.
        List<int[]> recovered = recoverSpansBySearchingValue(sourceText, value);
        if (!recovered.isEmpty()) {
            List<SensitiveDataItem> out = new ArrayList<>(recovered.size());
            for (int[] span : recovered) {
                out.add(new SensitiveDataItem(type, value, sourceLabel, span[0], span[1]));
            }
            return Optional.of(out);
        }

        log.debug("pii-detect requestId={} source={} stage=parse skip=value_span_mismatch type={} start={} end={} valueLen={}",
                requestId, sourceLabel, type, start, end, value.length());
        return Optional.empty();
    }

    private String safeRequestId(PiiDetectRequest request) {
        // No API contract changes: best-effort, stable request id for logs.
        // Prefer explicit requestId if present in request JSON (future-proof), otherwise random.
        try {
            // Reflection to avoid DTO change requirements.
            var m = request == null ? null : request.getClass().getMethod("getRequestId");
            Object v = m == null ? null : m.invoke(request);
            if (v instanceof String s && !s.isBlank()) return s;
        } catch (Exception ignored) {
            // DTO might not expose requestId; ignore and fallback to random UUID.
        }
        return UUID.randomUUID().toString();
    }


    private int countDigits(String s) {
        if (s == null || s.isEmpty()) return 0;
        int cnt = 0;
        for (int i = 0; i < s.length(); i++) {
            if (Character.isDigit(s.charAt(i))) cnt++;
        }
        return cnt;
    }

    /**
     * Strict phone signal: requires an explicit formatting cue (e.g., +, spaces, dashes) and a realistic digit count.
     * This avoids classifying arbitrary long digit strings as PHONE.
     */
    private boolean looksLikePhoneValue(String raw) {
        if (raw == null) return false;
        String t = raw.trim();
        boolean cue = t.startsWith("+") || t.contains("(") || t.contains(")") || t.contains("-") || t.contains(" ");
        if (!cue) return false;
        String norm = normalizePhone(t);
        int digits = countDigits(norm);
        return digits >= 7 && digits <= 15;
    }

    /**
     * Strict card signal: requires card-like formatting (spaces/dashes) and 13-19 digits.
     * This prevents length-only classification of digit runs as card numbers.
     */
    private boolean looksLikeCardNumberValue(String raw) {
        if (raw == null) return false;
        String t = raw.trim();
        boolean sep = t.indexOf(' ') >= 0 || t.indexOf('-') >= 0;
        if (!sep) return false;
        int digits = countDigits(t);
        return digits >= 13 && digits <= 19;
    }

    /**
     * Enforces strict separation rules after overlap resolution but BEFORE normalization/dedup:
     * - PHONE must never be represented as BANK_ACCOUNT
     * - CARD_NUMBER must never be represented as BANK_ACCOUNT
     * - Numeric values must not be retyped by length-only heuristics
     */
    private List<SensitiveDataItem> enforceStrictTypeSeparationPerSource(List<SensitiveDataItem> items) {
        if (items == null || items.isEmpty()) return items;
        Map<String, List<SensitiveDataItem>> bySource = groupBySource(items);

        List<SensitiveDataItem> out = new ArrayList<>(items.size());
        for (var e : bySource.entrySet()) {
            out.addAll(enforceStrictTypeSeparationSingleSource(e.getValue()));
        }
        out.sort(sourceStableOrder());
        return out;
    }

    private Map<String, List<SensitiveDataItem>> groupBySource(List<SensitiveDataItem> items) {
        Map<String, List<SensitiveDataItem>> bySource = new LinkedHashMap<>();
        for (SensitiveDataItem i : items) {
            if (i == null) continue;
            bySource.computeIfAbsent(i.getSource(), k -> new ArrayList<>()).add(i);
        }
        return bySource;
    }

    private List<SensitiveDataItem> enforceStrictTypeSeparationSingleSource(List<SensitiveDataItem> srcItems) {
        if (srcItems == null || srcItems.isEmpty()) return List.of();
        List<SensitiveDataItem> src = new ArrayList<>(srcItems);
        src.sort(Comparator.comparingInt(SensitiveDataItem::getStart)
                .thenComparingInt(SensitiveDataItem::getEnd)
                .thenComparing(i -> i.getType() == null ? "" : i.getType()));

        Set<String> protectedBankConflicts = computeProtectedBankConflictSpans(src);

        List<SensitiveDataItem> out = new ArrayList<>(src.size());
        for (SensitiveDataItem i : src) {
            if (i == null) continue;
            out.addAll(filterBankAccountConflicts(i, protectedBankConflicts));
        }
        return out;
    }

    private Set<String> computeProtectedBankConflictSpans(List<SensitiveDataItem> src) {
        Set<String> spans = new HashSet<>();
        for (SensitiveDataItem i : src) {
            if (i == null) continue;
            String spanKey = i.getStart() + "|" + i.getEnd();
            if ("PHONE".equals(i.getType()) && looksLikePhoneValue(i.getValue())) {
                spans.add(spanKey);
            }
            if (TYPE_CARD_NUMBER.equals(i.getType()) && looksLikeCardNumberValue(i.getValue())) {
                spans.add(spanKey);
            }
        }
        return spans;
    }

    private List<SensitiveDataItem> filterBankAccountConflicts(SensitiveDataItem item, Set<String> protectedBankConflicts) {
        if (item == null) return List.of();
        if (!TYPE_BANK_ACCOUNT.equals(item.getType())) return List.of(item);
        String spanKey = item.getStart() + "|" + item.getEnd();
        boolean shouldDrop = protectedBankConflicts.contains(spanKey)
                || looksLikePhoneValue(item.getValue())
                || looksLikeCardNumberValue(item.getValue());
        return shouldDrop ? List.of() : List.of(item);
    }

    // NOTE: Output normalization intentionally removed.
    // Masking/redaction requires preserving exact original substrings (casing/formatting).

    private String[] splitOnLast(String s, char sep) {
        int idx = s.lastIndexOf(sep);
        if (idx < 0) return new String[]{s, ""};
        return new String[]{s.substring(0, idx), s.substring(idx + 1)};
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

    // Add heuristic to recover truncated spans for AMOUNT and IDs
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

            Pattern p;
            try {
                p = Pattern.compile(Pattern.quote(value));
            } catch (Exception ex) {
                expanded.add(item);
                continue;
            }

            Matcher m = p.matcher(sourceText);
            boolean found = false;
            while (m.find()) {
                found = true;
                int s = m.start();
                int e = m.end();
                if (isValidOffsets(sourceText, value, s, e)) {
                    expanded.add(new SensitiveDataItem(item.getType(), value, item.getSource(), s, e));
                }
            }

            if (!found) {
                expanded.add(item);
            }
        }
        return expanded;
    }

    // NEW deduplication method: keep only one entry per (type, source, value)
    private List<SensitiveDataItem> dedupeByTypeValueSource(List<SensitiveDataItem> items) {
        if (items == null || items.isEmpty()) return items;
        Set<String> seen = new HashSet<>(items.size() * 2);
        List<SensitiveDataItem> out = new ArrayList<>(items.size());
        for (SensitiveDataItem i : items) {
            String key = i.getType() + "|" + i.getSource() + "|" + i.getValue();
            if (seen.add(key)) {
                out.add(i);
            }
        }
        return out;
    }

    // Keep old span-based dedup for reference, but not used in main flow
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

    private boolean isValidOffsets(String sourceText, String value, int start, int end) {
        try {
            if (sourceText == null) return false;
            if (value == null) return false;
            if (start < 0 || end < 0 || start >= end) return false;
            if (start > sourceText.length() || end > sourceText.length()) return false;

            String extracted = sourceText.substring(start, end);
            if (!extracted.equals(value)) {
                // PII-safe logging: never log raw values/substrings.
                log.debug("pii-detect stage=value_mismatch start={} end={} extractedLen={} valueLen={}",
                        start, end,
                        extracted.length(),
                        value.length());
                return false;
            }
            return true;
        } catch (Exception e) {
            log.debug("pii-detect stage=validate_offsets_exception start={} end={} err={}", start, end, e.toString());
            return false;
        }
    }

    // NOTE: intentionally removed truncated-span widening recovery.
    // PAD-52 requires strict span validation and prohibits mutating value.

    /**
     * Deterministic extraction stage for structured entities.
     * This is intentionally conservative (high precision) and uses character offsets only.
     */
    private List<SensitiveDataItem> extractStructuredEntities(String sourceLabel, String text) {
        if (text == null || text.isBlank()) return List.of();

        List<SensitiveDataItem> out = new ArrayList<>();
        out.addAll(detectNames(sourceLabel, text));  // ADDED: PERSON detection
        out.addAll(detectEmails(sourceLabel, text)); // ADDED: EMAIL detection
        out.addAll(detectPhones(sourceLabel, text)); // ADDED: PHONE detection
        out.addAll(detectCaseIds(sourceLabel, text));
        out.addAll(detectEvidenceIds(sourceLabel, text));
        out.addAll(detectPassports(sourceLabel, text));
        out.addAll(detectAmounts(sourceLabel, text));
        out.addAll(detectNationalIds(sourceLabel, text));
        out.addAll(detectCardNumbers(sourceLabel, text));
        out.addAll(detectBankAccounts(sourceLabel, text));
        out.addAll(detectContractRefs(sourceLabel, text)); // ADDED: CONTRACT_REF detection

        // NOTE: no deterministic CONTRACT_REF fallback:
        // it was overly broad and could collide with CASE_ID/other refs. Keep this to LLM only.

        // Do not dedupe/overlap here: the main pipeline performs
        // merge -> expandDuplicateSpans -> dedupe -> resolveOverlaps.
        return out;
    }

    /**
     * PERSON detection: Extract names with capital letters and common patterns,
     * excluding titles like "Client", "Mr.", etc.
     * Uses Unicode character class to handle accented characters.
     */
    private List<SensitiveDataItem> detectNames(String sourceLabel, String text) {
        // Set of title words to exclude (all lowercase for comparison)
        Set<String> titles = Set.of(
                "client", "mr", "mrs", "ms", "dr", "prof", "hon", "miss", "master", "rev", "fr", "sir", "lady", "lord", "judge", "justice", "president", "governor", "senator", "representative", "ambassador", "secretary", "minister", "director", "officer", "chief", "head", "manager", "ceo", "cfo", "coo", "contact"
        );

        // Unicode-aware pattern for two capitalized words: Uppercase + lowercase, space, Uppercase + lowercase
        Pattern namePattern = Pattern.compile("\\b\\p{Lu}\\p{Ll}+\\s+\\p{Lu}\\p{Ll}+\\b",
                Pattern.UNICODE_CHARACTER_CLASS);

        List<SensitiveDataItem> out = new ArrayList<>();
        Matcher m = namePattern.matcher(text);

        while (m.find()) {
            int s = m.start();
            int e = m.end();
            String value = text.substring(s, e);

            // Check if the first word is a title
            String firstWord = value.split("\\s+")[0].toLowerCase();
            if (titles.contains(firstWord)) {
                continue; // skip titles
            }

            if (isValidOffsets(text, value, s, e)) {
                out.add(new SensitiveDataItem("PERSON", value, sourceLabel, s, e));
            }
        }
        return out;
    }

    /**
     * EMAIL detection: Standard email pattern
     */
    private List<SensitiveDataItem> detectEmails(String sourceLabel, String text) {
        Pattern emailPattern = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
        List<SensitiveDataItem> out = new ArrayList<>();

        Matcher m = emailPattern.matcher(text);
        while (m.find()) {
            int s = m.start();
            int e = m.end();
            String value = text.substring(s, e);
            if (isValidOffsets(text, value, s, e)) {
                out.add(new SensitiveDataItem("EMAIL", value, sourceLabel, s, e));
            }
        }
        return out;
    }

    /**
     * PHONE detection: International and local formats
     * Patterns: +94 71 555 9988, 071-555-9988, etc.
     */
    private List<SensitiveDataItem> detectPhones(String sourceLabel, String text) {
        // International format: +94 71 555 9988 or +94715559988
        Pattern intlPattern = Pattern.compile("\\+\\d{1,3}[\\s-]?\\d{2}[\\s-]?\\d{3}[\\s-]?\\d{4}");
        // Local format: 071-555-9988, 071 555 9988, (071)555-9988
        Pattern localPattern = Pattern.compile("\\b(?:\\d{3}[\\s-]?\\d{3}[\\s-]?\\d{4}|\\d{10})\\b");

        List<SensitiveDataItem> out = new ArrayList<>();

        // Check international format
        Matcher m = intlPattern.matcher(text);
        while (m.find()) {
            int s = m.start();
            int e = m.end();
            String value = text.substring(s, e);
            if (isValidOffsets(text, value, s, e)) {
                out.add(new SensitiveDataItem("PHONE", value, sourceLabel, s, e));
            }
        }

        // Check local format
        m = localPattern.matcher(text);
        while (m.find()) {
            int s = m.start();
            int e = m.end();
            String value = text.substring(s, e);
            // Skip if it's just a long number that might be an ID or bank account
            if (value.length() >= 10 && countDigits(value) >= 10 &&
                    (value.contains("-") || value.contains(" ") || value.startsWith("0"))) {
                if (isValidOffsets(text, value, s, e)) {
                    out.add(new SensitiveDataItem("PHONE", value, sourceLabel, s, e));
                }
            }
        }

        return out;
    }

    /**
     * CONTRACT_REF detection: AGR-2024-4451, CNT-2023-1122, etc.
     */
    private List<SensitiveDataItem> detectContractRefs(String sourceLabel, String text) {
        Pattern contractPattern = Pattern.compile("\\b(?:AGR|CNT|CONT)-\\d{4}-\\d{4,6}\\b");
        List<SensitiveDataItem> out = new ArrayList<>();

        Matcher m = contractPattern.matcher(text);
        while (m.find()) {
            int s = m.start();
            int e = m.end();
            String value = text.substring(s, e);
            if (isValidOffsets(text, value, s, e)) {
                out.add(new SensitiveDataItem("CONTRACT_REF", value, sourceLabel, s, e));
            }
        }
        return out;
    }

    /**
     * CASE_ID deterministic fallback (high confidence)
     * Pattern: \b(?:DC|CR|HC)-\d{4}-\d{2,6}\b
     */
    private List<SensitiveDataItem> detectCaseIds(String sourceLabel, String text) {
        Pattern p = Pattern.compile("\\b(?:DC|CR|HC|LC)-\\d{4}-\\d{2,6}\\b");
        List<SensitiveDataItem> out = new ArrayList<>();
        findAllMatches(out, "CASE_ID", sourceLabel, text, p);
        return out;
    }

    /**
     * EVIDENCE_ID deterministic fallback (high confidence)
     * Pattern: \bEV-\d{5,}\b
     */
    private List<SensitiveDataItem> detectEvidenceIds(String sourceLabel, String text) {
        Pattern p = Pattern.compile("\\bEV-\\d{5,}\\b");
        List<SensitiveDataItem> out = new ArrayList<>();
        findAllMatches(out, "EVIDENCE_ID", sourceLabel, text, p);
        return out;
    }

    /**
     * PASSPORT deterministic fallback (high confidence)
     * Pattern: \b[A-Z]\d{6,8}\b
     */
    private List<SensitiveDataItem> detectPassports(String sourceLabel, String text) {
        Pattern p = Pattern.compile("\\b[A-Z]\\d{6,8}\\b");
        List<SensitiveDataItem> out = new ArrayList<>();
        findAllMatches(out, "PASSPORT", sourceLabel, text, p);
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
        findAllMatches(out, TYPE_AMOUNT, sourceLabel, text, prefix);
        findAllMatches(out, TYPE_AMOUNT, sourceLabel, text, suffix);
        return out;
    }

    /**
     * NATIONAL_ID fallback. Implement formats commonly used in typical tests:
     * - 9 digits + V/X (Sri Lanka old NIC): 123456789V / 123456789X (case-insensitive)
     * IMPORTANT:
     * We intentionally DO NOT treat arbitrary 12-digit sequences as NATIONAL_ID.
     * A plain 12-digit number is too ambiguous (could be account numbers, references, etc.)
     * and would violate the requirement to avoid unsafe, blind classification.
     */
    private List<SensitiveDataItem> detectNationalIds(String sourceLabel, String text) {
        Pattern oldNic = Pattern.compile("\\b\\d{9}[VvXx]\\b");

        List<SensitiveDataItem> out = new ArrayList<>();
        findAllMatches(out, "NATIONAL_ID", sourceLabel, text, oldNic);
        return out;
    }

    /**
     * CARD_NUMBER fallback: detect 13-19 digit sequences allowing spaces or dashes.
     * We keep this conservative: requires at least 13 digits total.
     */
    private List<SensitiveDataItem> detectCardNumbers(String sourceLabel, String text) {
        // Require a separator to avoid misclassifying unformatted long digit sequences
        // (which are more plausibly BANK_ACCOUNT / references).
        Pattern p = Pattern.compile("\\b(?:\\d{4}[ -]){3,4}\\d{1,4}\\b");
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
            if (digits < 13 || digits > 19) continue;
            out.add(new SensitiveDataItem(TYPE_CARD_NUMBER, candidate, sourceLabel, s, e));
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
        findAllMatches(out, TYPE_BANK_ACCOUNT, sourceLabel, text, iban);

        // Numeric account numbers MUST NOT be classified by length alone.
        // Require a clear label near the number to reduce ambiguity and prevent collisions with phone/card/reference numbers.
        // Example matches: "Acct 1234567890", "Account No: 1234567890123", "A/C 12345678901".
        Pattern labeledDigits = Pattern.compile("(?i)\\b(?:acct|account|a/c|acc\\.?|account\\s*no\\.?)\\s*[:#-]?\\s*(\\d{10,18})\\b");
        Matcher m = labeledDigits.matcher(text);
        while (m.find()) {
            int s = m.start(1);
            int e = m.end(1);
            if (s < 0 || e <= s || e > text.length()) continue;
            String value = text.substring(s, e);
            if (isValidOffsets(text, value, s, e)) {
                out.add(new SensitiveDataItem(TYPE_BANK_ACCOUNT, value, sourceLabel, s, e));
            }
        }

        // Also detect unlabeled long digit sequences that don't match other patterns
        // This will catch "77889900" in the example
        Pattern unlabeledDigits = Pattern.compile("\\b\\d{8,18}\\b");
        m = unlabeledDigits.matcher(text);
        while (m.find()) {
            int s = m.start();
            int e = m.end();
            String value = text.substring(s, e);
            // Skip if it looks like a phone number (starts with 0 and has 10 digits)
            if (value.startsWith("0") && value.length() == 10) {
                continue;
            }
            // Skip if it looks like a case ID or contract ref format
            if (value.matches("\\d{4}-\\d{2,6}")) {
                continue;
            }
            if (isValidOffsets(text, value, s, e)) {
                out.add(new SensitiveDataItem(TYPE_BANK_ACCOUNT, value, sourceLabel, s, e));
            }
        }

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
            if (isValidOffsets(text, value, s, e)) {
                out.add(new SensitiveDataItem(type, value, sourceLabel, s, e));
            }
        }
    }

    private List<SensitiveDataItem> resolveOverlapsPerSource(List<SensitiveDataItem> items) {
        if (items == null || items.isEmpty()) return items;

        // Resolve overlaps within the same source only.
        Map<String, List<SensitiveDataItem>> bySource = new LinkedHashMap<>();
        for (SensitiveDataItem i : items) {
            bySource.computeIfAbsent(i.getSource(), k -> new ArrayList<>()).add(i);
        }

        List<SensitiveDataItem> out = new ArrayList<>(items.size());
        for (var e : bySource.entrySet()) {
            out.addAll(resolveOverlapsSingleSource(e.getValue()));
        }

        out.sort(sourceStableOrder());
        return out;
    }

    private Comparator<SensitiveDataItem> sourceStableOrder() {
        return Comparator.comparing(SensitiveDataItem::getSource, Comparator.nullsFirst(String::compareTo))
                .thenComparingInt(SensitiveDataItem::getStart)
                .thenComparingInt(SensitiveDataItem::getEnd);
    }

    private List<SensitiveDataItem> resolveOverlapsSingleSource(List<SensitiveDataItem> items) {
        if (items.isEmpty()) return items;

        // Overlap resolution policy (source-local, deterministic):
        // - Never dedupe identical spans: that's handled by dedupeBySpanKey.
        // - Keep different types unless this is a CARD_NUMBER vs BANK_ACCOUNT conflict.
        // - For same-type conflicts, pick a single deterministic winner.

        List<SensitiveDataItem> sorted = new ArrayList<>(items);
        sorted.sort(Comparator
                .comparingInt(SensitiveDataItem::getStart)
                .thenComparingInt(SensitiveDataItem::getEnd)
                .thenComparing((SensitiveDataItem i) -> i.getType() == null ? "" : i.getType()));

        List<SensitiveDataItem> chosen = new ArrayList<>();
        for (SensitiveDataItem cand : sorted) {
            OverlapEval eval = evaluateCandidateAgainstChosen(chosen, cand);
            if (eval.dropExisting != null) {
                chosen.remove(eval.dropExisting);
            }
            if (!eval.dropCandidate) {
                chosen.add(cand);
            }
        }

        chosen.sort(Comparator.comparingInt(SensitiveDataItem::getStart)
                .thenComparingInt(SensitiveDataItem::getEnd)
                .thenComparing((SensitiveDataItem i) -> i.getType() == null ? "" : i.getType()));
        return chosen;
    }

    private record OverlapEval(boolean dropCandidate, SensitiveDataItem dropExisting) {
        static OverlapEval keepCandidate() {
            return new OverlapEval(false, null);
        }
    }

    private OverlapEval evaluateCandidateAgainstChosen(List<SensitiveDataItem> chosen, SensitiveDataItem cand) {
        if (chosen == null || chosen.isEmpty()) return OverlapEval.keepCandidate();

        SensitiveDataItem toDrop = null;
        boolean rejectCand = false;

        int i = 0;
        int size = chosen.size();
        while (i < size) {
            SensitiveDataItem existing = chosen.get(i);
            boolean overlaps = rangesOverlap(cand.getStart(), cand.getEnd(), existing.getStart(), existing.getEnd());
            if (overlaps) {
                OverlapDecision d = decideOverlap(existing, cand);
                rejectCand = rejectCand || (d == OverlapDecision.DROP_CANDIDATE);
                boolean dropExisting = d == OverlapDecision.DROP_EXISTING;
                toDrop = dropExisting ? existing : toDrop;
            }
            i++;
        }

        return new OverlapEval(rejectCand, toDrop);
    }

    private enum OverlapDecision {
        KEEP_BOTH,
        DROP_EXISTING,
        DROP_CANDIDATE
    }

    private OverlapDecision decideOverlap(SensitiveDataItem existing, SensitiveDataItem cand) {
        String aType = existing == null ? null : existing.getType();
        String bType = cand == null ? null : cand.getType();
        if (aType == null || bType == null) {
            // Unknown types: keep both to avoid accidental loss.
            return OverlapDecision.KEEP_BOTH;
        }

        // If same type, keep the better one deterministically.
        if (aType.equals(bType)) {
            return prefer(existing, cand) == existing ? OverlapDecision.DROP_CANDIDATE : OverlapDecision.DROP_EXISTING;
        }

        // CARD_NUMBER vs BANK_ACCOUNT: if spans overlap, prefer CARD_NUMBER when formatted (contains space/dash),
        // otherwise prefer BANK_ACCOUNT. This keeps classification stable and explainable.
        if (isCardVsBank(aType, bType)) {
            SensitiveDataItem preferred = preferCardVsBank(existing, cand);
            return preferred == existing ? OverlapDecision.DROP_CANDIDATE : OverlapDecision.DROP_EXISTING;
        }

        // Otherwise, keep both: overlapping entities of different types can both be valid
        // (e.g., PERSON inside ADDRESS text, or CASE_ID near CONTRACT_REF in same segment).
        return OverlapDecision.KEEP_BOTH;
    }

    private boolean isCardVsBank(String aType, String bType) {
        return (TYPE_CARD_NUMBER.equals(aType) && TYPE_BANK_ACCOUNT.equals(bType))
                || (TYPE_BANK_ACCOUNT.equals(aType) && TYPE_CARD_NUMBER.equals(bType));
    }

    private SensitiveDataItem preferCardVsBank(SensitiveDataItem a, SensitiveDataItem b) {
        boolean aFormatted = a != null && a.getValue() != null && containsCardFormatting(a.getValue());
        boolean bFormatted = b != null && b.getValue() != null && containsCardFormatting(b.getValue());

        // formatted card wins
        if (aFormatted && !bFormatted) return TYPE_CARD_NUMBER.equals(a.getType()) ? a : b;
        if (bFormatted && !aFormatted) return TYPE_CARD_NUMBER.equals(b.getType()) ? b : a;

        // otherwise prefer BANK_ACCOUNT (unformatted digits)
        String aType = a == null ? null : a.getType();
        String bType = b == null ? null : b.getType();
        if (TYPE_BANK_ACCOUNT.equals(aType)) return a;
        if (TYPE_BANK_ACCOUNT.equals(bType)) return b;
        // defensive fallback
        return a != null ? a : b;
    }

    private boolean containsCardFormatting(String v) {
        if (v == null) return false;
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            if (c == ' ' || c == '-') return true;
        }
        return false;
    }

    private SensitiveDataItem prefer(SensitiveDataItem a, SensitiveDataItem b) {
        // lower priority number wins; if tie, longer wins; if tie, earlier start wins.
        int pa = typePriority(a == null ? null : a.getType());
        int pb = typePriority(b == null ? null : b.getType());
        if (pa != pb) return pa < pb ? a : b;

        int la = a == null ? -1 : (a.getEnd() - a.getStart());
        int lb = b == null ? -1 : (b.getEnd() - b.getStart());
        if (la != lb) return la > lb ? a : b;

        int sa = a == null ? Integer.MAX_VALUE : a.getStart();
        int sb = b == null ? Integer.MAX_VALUE : b.getStart();
        return sa <= sb ? a : b;
    }

    /**
     * Lower number means higher priority when resolving overlaps.
     * Structured identifiers are preferred over broad/ambiguous spans.
     */
    private int typePriority(String type) {
        if (type == null) return 100;
        return switch (type) {
            case "NATIONAL_ID", "PASSPORT", "DRIVER_LICENSE" -> 0;
            case TYPE_CARD_NUMBER -> 1;
            case TYPE_BANK_ACCOUNT -> 2;
            case TYPE_AMOUNT -> 3;
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
        static ParseResult ok(List<SensitiveDataItem> items) {
            return new ParseResult(false, null, items == null ? List.of() : items);
        }
    }
}