package com.primesprint.pii.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.primesprint.pii.client.GroqLlmClient;
import com.primesprint.pii.dto.PiiDetectRequest;
import com.primesprint.pii.dto.SensitiveDataItem;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

class PiiDetectServiceParsingTest {

    @Test
    void sameValueInDocAndPrompt_returnsTwoEntriesDifferentSources() {
        GroqLlmClient client = Mockito.mock(GroqLlmClient.class);
        ObjectMapper mapper = new ObjectMapper();

        // Return a minimal Groq-like response body
        // "Client John Silva." => "John Silva" starts at 7 ends at 17
        String docResp = "{\"choices\":[{\"message\":{\"content\":\"[{\\\"type\\\":\\\"PERSON\\\",\\\"value\\\":\\\"John Silva\\\",\\\"start\\\":7,\\\"end\\\":17}]\"}}]}";
        // "Hello John Silva" => "John Silva" starts at 6 ends at 16
        String promptResp = "{\"choices\":[{\"message\":{\"content\":\"[{\\\"type\\\":\\\"PERSON\\\",\\\"value\\\":\\\"John Silva\\\",\\\"start\\\":6,\\\"end\\\":16}]\"}}]}";

        Mockito.when(client.chatRaw(any(), Mockito.eq("Client John Silva."))).thenReturn(reactor.core.publisher.Mono.just(docResp));
        Mockito.when(client.chatRaw(any(), Mockito.eq("Hello John Silva"))).thenReturn(reactor.core.publisher.Mono.just(promptResp));

        PiiDetectService service = new PiiDetectService(client, mapper, false, 0, 0);

        PiiDetectRequest req = new PiiDetectRequest("r1", "Client John Silva.", "Hello John Silva");
        List<SensitiveDataItem> out = service.detect(req);

        assertEquals(2, out.size());
        assertEquals("document", out.get(0).getSource());
        assertEquals("prompt", out.get(1).getSource());
    }

    @Test
    void invalidJsonFromModel_returnsEmptyList() {
        GroqLlmClient client = Mockito.mock(GroqLlmClient.class);
        ObjectMapper mapper = new ObjectMapper();

        Mockito.when(client.chatRaw(any(), any())).thenReturn(reactor.core.publisher.Mono.just("not-json"));

        PiiDetectService service = new PiiDetectService(client, mapper, false, 0, 0);
        PiiDetectRequest req = new PiiDetectRequest("r1", "Client John", "Prompt");

        assertEquals(List.of(), service.detect(req));
    }
}


